package io.github.matheuslealpa.apicore.web;

import io.github.matheuslealpa.apicore.exception.PreconditionFailedException;
import io.github.matheuslealpa.apicore.jpa.PageResponse;
import io.github.matheuslealpa.apicore.rsql.RsqlFilter;
import io.github.matheuslealpa.apicore.service.CrudService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ETag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

/**
 * Base REST controller exposing CRUD endpoints on top of a {@link CrudService}:
 * {@code GET /}, {@code GET /{id}}, {@code POST /}, {@code PUT /{id}} and {@code DELETE /{id}}.
 * The list endpoint accepts RSQL filters; declare {@code @RsqlFilter(allow = ...)} on the subclass
 * to restrict the filterable and sortable properties.
 *
 * <p>Versioned entities ({@code AuditableEntity}) are returned with an {@code ETag} holding their version.
 * {@code PUT} and {@code PATCH} accept it back in {@code If-Match} and answer 412 if the entity changed in the
 * meantime. {@code PATCH /{id}} answers 405 unless the subclass overrides {@link #applyPatch}.
 *
 * <pre>{@code
 * @RestController
 * @RequestMapping("/users")
 * @RsqlFilter(allow = {"name", "email"})
 * public class UserController extends CrudController<User, UUID, UserRequest, UserResponse> {
 *
 *     public UserController(UserService service) {
 *         super(service);
 *     }
 *
 *     protected UserResponse toResponse(User user) { ... }
 *     protected User toEntity(UserRequest request) { ... }
 *     protected void applyChanges(User user, UserRequest request) { ... }
 * }
 * }</pre>
 *
 * @param <T>   entity type
 * @param <ID>  id type
 * @param <REQ> request body type, validated with {@code @Valid}
 * @param <RES> response type
 */
public abstract class CrudController<T, ID, REQ, RES> {

    protected final CrudService<T, ID, ?> service;

    protected CrudController(CrudService<T, ID, ?> service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<RES> list(@RsqlFilter Specification<T> filter, Pageable pageable) {
        return PageResponse.of(service.findAll(filter, pageable), this::toResponse);
    }

    @GetMapping("/{id}")
    public RES get(@PathVariable ID id) {
        return respond(service.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RES create(@Valid @RequestBody REQ request) {
        return respond(service.create(toEntity(request)));
    }

    @PutMapping("/{id}")
    public RES update(@PathVariable ID id, @Valid @RequestBody REQ request) {
        return respond(service.update(id, checkingIfMatch(changed -> applyChanges(changed, request))));
    }

    /**
     * Partial update. Only fields present in the body are changed; it is up to {@link #applyPatch} to validate them.
     */
    @PatchMapping("/{id}")
    public RES patch(@PathVariable ID id, @RequestBody Map<String, @Nullable Object> patch) {
        if (!supportsPatch()) {
            throw new ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED, "PATCH is not supported by this resource.");
        }
        return respond(service.update(id, checkingIfMatch(changed -> applyPatch(changed, patch))));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable ID id) {
        service.delete(id);
    }

    /**
     * Maps an entity to the response body.
     */
    protected abstract RES toResponse(T entity);

    /**
     * Creates a new entity from the request body.
     */
    protected abstract T toEntity(REQ request);

    /**
     * Applies the request body to an existing entity.
     */
    protected abstract void applyChanges(T entity, REQ request);

    /**
     * Applies a partial update, where absent keys mean "keep" and {@code null} values mean "clear".
     * Not overridden, {@code PATCH} answers 405. Throw {@code BusinessException} (422) for invalid values.
     *
     * <pre>{@code
     * protected void applyPatch(User user, Map<String, Object> patch) {
     *     if (patch.containsKey("name")) {
     *         user.setName((String) patch.get("name"));
     *     }
     * }
     * }</pre>
     */
    protected void applyPatch(T entity, Map<String, @Nullable Object> patch) {
        throw new ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED, "PATCH is not supported by this resource.");
    }

    // ETag and If-Match go through the current request, so the endpoint signatures stay free of HTTP plumbing
    // and subclasses overriding them keep compiling.
    private RES respond(T entity) {
        Long version = service.versionOf(entity);
        HttpServletResponse response = currentRequest().getResponse();
        if (version != null && response != null) {
            response.setHeader(HttpHeaders.ETAG, "\"" + version + "\"");
        }
        return toResponse(entity);
    }

    // Checks If-Match inside the update transaction, before the changes are applied. Goes through
    // CrudService.update(id, changes), so services overriding it keep being called.
    private Consumer<T> checkingIfMatch(Consumer<T> changes) {
        Long expected = expectedVersion(currentRequest().getRequest().getHeader(HttpHeaders.IF_MATCH));
        return entity -> {
            if (expected != null && !expected.equals(service.versionOf(entity))) {
                throw new PreconditionFailedException("The resource has changed since it was read.");
            }
            changes.accept(entity);
        };
    }

    private static ServletRequestAttributes currentRequest() {
        return (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
    }

    // Absent or "*" skips the check. A weak or non-numeric tag never matches the strong version ETag.
    private static @Nullable Long expectedVersion(@Nullable String ifMatch) {
        if (ifMatch == null || ifMatch.isBlank()) {
            return null;
        }
        List<ETag> tags = ETag.parse(ifMatch);
        if (tags.stream().anyMatch(ETag::isWildcard)) {
            return null;
        }
        if (tags.size() != 1 || tags.getFirst().weak()) {
            throw new PreconditionFailedException("If-Match must hold a single strong ETag.");
        }
        try {
            return Long.valueOf(tags.getFirst().tag());
        }
        catch (NumberFormatException ex) {
            throw new PreconditionFailedException("The resource has changed since it was read.");
        }
    }

    // Unwraps CGLIB proxies (e.g. method security) to look at the subclass written by the user.
    private boolean supportsPatch() {
        Method method = ReflectionUtils.findMethod(ClassUtils.getUserClass(this), "applyPatch", Object.class, Map.class);
        return method != null && method.getDeclaringClass() != CrudController.class;
    }
}
