package io.github.matheuslealpa.apicore.rsql;

import java.util.List;
import java.util.Map;

import cz.jirutka.rsql.parser.RSQLParser;
import io.github.perplexhub.rsql.QuerySupport;
import io.github.perplexhub.rsql.RSQLJPASupport;
import io.github.perplexhub.rsql.RSQLOperators;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Resolve parâmetros {@code Specification<T>} anotados com {@link RsqlFilter}.
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
    public Specification<?> resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        RsqlFilter filter = parameter.getParameterAnnotation(RsqlFilter.class);
        String paramName = filter.param().isBlank() ? defaultParamName : filter.param();
        String query = webRequest.getParameter(paramName);
        if (query == null || query.isBlank()) {
            return Specification.unrestricted();
        }
        // Valida a sintaxe já na entrada, para responder 400 antes de chegar ao repositório.
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
            throw new IllegalStateException("@RsqlFilter com allow exige Specification tipada, ex.: Specification<Produto>: "
                    + parameter.getMethod());
        }
        return type;
    }
}
