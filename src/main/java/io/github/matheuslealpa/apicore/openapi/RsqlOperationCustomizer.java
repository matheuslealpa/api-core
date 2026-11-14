package io.github.matheuslealpa.apicore.openapi;

import io.github.matheuslealpa.apicore.rsql.RsqlFilterArgumentResolver;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.springdoc.core.customizers.OperationCustomizer;

import java.util.List;

import org.springframework.core.MethodParameter;
import org.springframework.web.method.HandlerMethod;

/**
 * Documents the RSQL parameter of every endpoint with a {@code Specification<T>} parameter, listing the properties
 * that can be filtered and sorted. The {@code Specification} itself is hidden: it is not part of the request.
 */
public class RsqlOperationCustomizer implements OperationCustomizer {

    private final RsqlFilterArgumentResolver resolver;

    public RsqlOperationCustomizer(RsqlFilterArgumentResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public Operation customize(Operation operation, HandlerMethod handlerMethod) {
        for (MethodParameter parameter : handlerMethod.getMethodParameters()) {
            if (resolver.supportsParameter(parameter)) {
                operation.addParametersItem(new Parameter()
                        .in("query")
                        .name(resolver.paramName(parameter))
                        .required(false)
                        .description(description(resolver.allowedProperties(parameter)))
                        .example("name==abc*;price=gt=10")
                        .schema(new StringSchema()));
            }
        }
        return operation;
    }

    private static String description(List<String> allowed) {
        String properties = allowed.contains("*") ? "all, including associations" : String.join(", ", allowed);
        return "RSQL filter: ';' = and, ',' = or; operators ==, !=, =gt=, =ge=, =lt=, =le=, =in=, =out=, =bt=, "
                + "=like=, =isnull=, =notnull=. Filterable and sortable properties: " + properties + ".";
    }
}
