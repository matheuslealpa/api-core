package io.github.matheuslealpa.apicore.web;

import io.github.matheuslealpa.apicore.jpa.PageResponse;
import io.github.matheuslealpa.apicore.rsql.RsqlFilter;
import io.github.matheuslealpa.apicore.service.CrudService;
import jakarta.validation.Valid;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Base REST controller exposing CRUD endpoints on top of a {@link CrudService}:
 * {@code GET /}, {@code GET /{id}}, {@code POST /}, {@code PUT /{id}} and {@code DELETE /{id}}.
 * The list endpoint accepts RSQL filters; declare {@code @RsqlFilter(allow = ...)} on the subclass
 * to restrict the filterable properties.
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
        return toResponse(service.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RES create(@Valid @RequestBody REQ request) {
        return toResponse(service.create(toEntity(request)));
    }

    @PutMapping("/{id}")
    public RES update(@PathVariable ID id, @Valid @RequestBody REQ request) {
        return toResponse(service.update(id, entity -> applyChanges(entity, request)));
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
}
