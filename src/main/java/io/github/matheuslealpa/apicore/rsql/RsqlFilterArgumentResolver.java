package io.github.matheuslealpa.apicore.rsql;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import cz.jirutka.rsql.parser.RSQLParser;
import io.github.perplexhub.rsql.QuerySupport;
import io.github.perplexhub.rsql.RSQLJPASupport;
import io.github.perplexhub.rsql.RSQLOperators;
import org.jspecify.annotations.Nullable;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Resolves every {@code Specification<T>} controller parameter from an RSQL expression in the request.
 * {@link RsqlFilter} is optional; without {@code allow}, only the entity's basic attributes can be filtered.
 * The same whitelist applies to the {@code ?sort=} properties of the request, so a listing cannot be ordered
 * by a field (or join) it cannot be filtered by.
 */
public class RsqlFilterArgumentResolver implements HandlerMethodArgumentResolver {

    static final String ALLOW_ALL = "*";

    private final RSQLParser parser = new RSQLParser(RSQLOperators.supportedOperators());

    private final String defaultParamName;

    private final String sortParamName;

    private final Function<Class<?>, List<String>> defaultAllowed;

    /**
     * @param defaultParamName request parameter used when {@link RsqlFilter#param()} is not set
     * @param defaultAllowed   properties allowed for an entity when {@link RsqlFilter#allow()} is not set
     */
    public RsqlFilterArgumentResolver(String defaultParamName, Function<Class<?>, List<String>> defaultAllowed) {
        this(defaultParamName, "sort", defaultAllowed);
    }

    /**
     * @param defaultParamName request parameter used when {@link RsqlFilter#param()} is not set
     * @param sortParamName    request parameter holding the sort, validated against the same whitelist
     * @param defaultAllowed   properties allowed for an entity when {@link RsqlFilter#allow()} is not set
     */
    public RsqlFilterArgumentResolver(String defaultParamName, String sortParamName,
            Function<Class<?>, List<String>> defaultAllowed) {
        this.defaultParamName = defaultParamName;
        this.sortParamName = sortParamName;
        this.defaultAllowed = defaultAllowed;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return Specification.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Specification<?> resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) {
        RsqlFilter filter = effectiveFilter(parameter);
        List<String> allowed = allowedProperties(parameter);
        boolean allowAll = allowed.contains(ALLOW_ALL);
        if (!allowAll) {
            checkSort(webRequest, allowed);
        }
        String query = webRequest.getParameter(paramName(parameter));
        if (query == null || query.isBlank()) {
            return Specification.unrestricted();
        }
        // Validates the syntax up front, to respond 400 before reaching the repository.
        parser.parse(query);
        QuerySupport.QuerySupportBuilder support = QuerySupport.builder()
                .rsqlQuery(query)
                .distinct(filter != null && filter.distinct());
        if (!allowAll) {
            support.propertyWhitelist(Map.of(entityType(parameter), allowed));
        }
        return RSQLJPASupport.toSpecification(support.build());
    }

    /**
     * Request parameter holding the RSQL expression for the given {@code Specification<T>} parameter.
     */
    public String paramName(MethodParameter parameter) {
        RsqlFilter filter = effectiveFilter(parameter);
        return filter != null && !filter.param().isBlank() ? filter.param() : defaultParamName;
    }

    /**
     * Properties that can be filtered and sorted for the given {@code Specification<T>} parameter;
     * {@code "*"} means all of them.
     */
    public List<String> allowedProperties(MethodParameter parameter) {
        RsqlFilter filter = effectiveFilter(parameter);
        return filter != null && filter.allow().length > 0
                ? List.of(filter.allow())
                : defaultAllowed.apply(entityType(parameter));
    }

    // Same rule as the RSQL whitelist: the first segment of the path must be allowed, so a listed
    // association also allows its fields (category.name). Values follow Spring Data: "a,b,desc".
    private void checkSort(NativeWebRequest webRequest, List<String> allowed) {
        String[] values = webRequest.getParameterValues(sortParamName);
        if (values == null) {
            return;
        }
        for (String value : values) {
            for (String part : value.split(",")) {
                String property = part.trim();
                if (property.isEmpty() || "ignorecase".equalsIgnoreCase(property)
                        || Sort.Direction.fromOptionalString(property).isPresent()) {
                    continue;
                }
                if (!allowed.contains(property.split("\\.", 2)[0])) {
                    throw new InvalidSortException(property);
                }
            }
        }
    }

    // A parameter annotation without attributes defers to the controller class, e.g. CrudController subclasses.
    private static @Nullable RsqlFilter effectiveFilter(MethodParameter parameter) {
        RsqlFilter filter = parameter.getParameterAnnotation(RsqlFilter.class);
        if (filter != null && (filter.allow().length > 0 || !filter.param().isBlank() || filter.distinct())) {
            return filter;
        }
        RsqlFilter classFilter = AnnotatedElementUtils.findMergedAnnotation(parameter.getContainingClass(), RsqlFilter.class);
        return classFilter != null ? classFilter : filter;
    }

    private static Class<?> entityType(MethodParameter parameter) {
        Class<?> type = ResolvableType.forMethodParameter(parameter).as(Specification.class).resolveGeneric(0);
        if (type == null) {
            throw new IllegalStateException("RSQL filtering requires a typed Specification, e.g. Specification<Product>: "
                    + parameter.getMethod());
        }
        return type;
    }
}
