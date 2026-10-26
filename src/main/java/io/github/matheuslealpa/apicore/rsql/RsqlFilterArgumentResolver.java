package io.github.matheuslealpa.apicore.rsql;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import cz.jirutka.rsql.parser.RSQLParser;
import io.github.perplexhub.rsql.QuerySupport;
import io.github.perplexhub.rsql.RSQLJPASupport;
import io.github.perplexhub.rsql.RSQLOperators;
import org.jspecify.annotations.Nullable;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Resolves {@code Specification<T>} parameters annotated with {@link RsqlFilter}.
 */
public class RsqlFilterArgumentResolver implements HandlerMethodArgumentResolver {

    private final RSQLParser parser = new RSQLParser(RSQLOperators.supportedOperators());

    private final String defaultParamName;

    public RsqlFilterArgumentResolver(String defaultParamName) {
        this.defaultParamName = defaultParamName;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(RsqlFilter.class)
                && Specification.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Specification<?> resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) {
        // supportsParameter already guarantees the annotation.
        RsqlFilter filter = Objects.requireNonNull(parameter.getParameterAnnotation(RsqlFilter.class));
        String paramName = filter.param().isBlank() ? defaultParamName : filter.param();
        String query = webRequest.getParameter(paramName);
        if (query == null || query.isBlank()) {
            return Specification.unrestricted();
        }
        // Validates the syntax up front, to respond 400 before reaching the repository.
        parser.parse(query);
        QuerySupport.QuerySupportBuilder support = QuerySupport.builder()
                .rsqlQuery(query)
                .distinct(filter.distinct());
        if (filter.allow().length > 0) {
            support.propertyWhitelist(Map.of(entityType(parameter), List.of(filter.allow())));
        }
        return RSQLJPASupport.toSpecification(support.build());
    }

    private static Class<?> entityType(MethodParameter parameter) {
        Class<?> type = ResolvableType.forMethodParameter(parameter).as(Specification.class).resolveGeneric(0);
        if (type == null) {
            throw new IllegalStateException("@RsqlFilter with allow requires a typed Specification, e.g. Specification<Product>: "
                    + parameter.getMethod());
        }
        return type;
    }
}
