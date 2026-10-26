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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Resolves every {@code Specification<T>} controller parameter from an RSQL expression in the request.
 * {@link RsqlFilter} is optional; without {@code allow}, only the entity's basic attributes can be filtered.
 */
public class RsqlFilterArgumentResolver implements HandlerMethodArgumentResolver {

    static final String ALLOW_ALL = "*";

    private final RSQLParser parser = new RSQLParser(RSQLOperators.supportedOperators());

    private final String defaultParamName;

    private final Function<Class<?>, List<String>> defaultAllowed;

    /**
     * @param defaultParamName request parameter used when {@link RsqlFilter#param()} is not set
     * @param defaultAllowed   properties allowed for an entity when {@link RsqlFilter#allow()} is not set
     */
    public RsqlFilterArgumentResolver(String defaultParamName, Function<Class<?>, List<String>> defaultAllowed) {
        this.defaultParamName = defaultParamName;
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
        String paramName = filter != null && !filter.param().isBlank() ? filter.param() : defaultParamName;
        String query = webRequest.getParameter(paramName);
        if (query == null || query.isBlank()) {
            return Specification.unrestricted();
        }
        // Validates the syntax up front, to respond 400 before reaching the repository.
        parser.parse(query);
        QuerySupport.QuerySupportBuilder support = QuerySupport.builder()
                .rsqlQuery(query)
                .distinct(filter != null && filter.distinct());
        List<String> allowed = filter != null && filter.allow().length > 0
                ? List.of(filter.allow())
                : defaultAllowed.apply(entityType(parameter));
        if (!allowed.contains(ALLOW_ALL)) {
            support.propertyWhitelist(Map.of(entityType(parameter), allowed));
        }
        return RSQLJPASupport.toSpecification(support.build());
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
