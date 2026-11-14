package io.github.matheuslealpa.apicore.openapi;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.springdoc.core.customizers.OperationCustomizer;

import java.math.BigDecimal;

import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Pageable;
import org.springframework.web.method.HandlerMethod;

/**
 * Documents a {@code Pageable} parameter as the {@code page}, {@code size} and {@code sort} query parameters
 * Spring Data actually reads, instead of the single required object springdoc infers without {@code @ParameterObject}.
 */
public class PageableOperationCustomizer implements OperationCustomizer {

    private final String page;

    private final String size;

    private final String sort;

    public PageableOperationCustomizer(String prefix, String page, String size, String sort) {
        this.page = prefix + page;
        this.size = prefix + size;
        this.sort = sort;
    }

    @Override
    public Operation customize(Operation operation, HandlerMethod handlerMethod) {
        for (MethodParameter parameter : handlerMethod.getMethodParameters()) {
            if (!Pageable.class.isAssignableFrom(parameter.getParameterType()) || operation.getParameters() == null) {
                continue;
            }
            String name = parameter.getParameterName();
            boolean removed = operation.getParameters()
                    .removeIf(existing -> "query".equals(existing.getIn()) && existing.getName().equals(name));
            if (removed) {
                operation.addParametersItem(query(page, "Page number, starting at 0.", new IntegerSchema()._default(0)
                        .minimum(BigDecimal.ZERO)));
                operation.addParametersItem(query(size, "Page size.", new IntegerSchema()._default(20)
                        .minimum(BigDecimal.ONE)));
                operation.addParametersItem(query(sort, "Sorting as property(,asc|desc); repeat for several properties.",
                        new ArraySchema().items(new StringSchema())));
            }
        }
        return operation;
    }

    private static Parameter query(String name, String description, io.swagger.v3.oas.models.media.Schema<?> schema) {
        return new Parameter().in("query").name(name).required(false).description(description).schema(schema);
    }
}
