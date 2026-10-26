package io.github.matheuslealpa.apicore.rsql;

import cz.jirutka.rsql.parser.RSQLParserException;
import io.github.matheuslealpa.apicore.web.ProblemDetails;
import io.github.perplexhub.rsql.PropertyException;
import io.github.perplexhub.rsql.RSQLException;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Responds 400 to invalid RSQL filters. Runs before the global handler, which catches {@code Exception}.
 */
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE - 1)
public class RsqlExceptionHandler {

    @ExceptionHandler(RSQLParserException.class)
    public ProblemDetail handleSyntax(RSQLParserException exception) {
        return ProblemDetails.of(HttpStatus.BAD_REQUEST, "Invalid filter syntax.");
    }

    @ExceptionHandler(RSQLException.class)
    public ProblemDetail handleFilter(RSQLException exception) {
        ProblemDetail problem = ProblemDetails.of(HttpStatus.BAD_REQUEST, "Invalid filter.");
        if (exception instanceof PropertyException property) {
            problem.setDetail("Invalid filter for property '" + property.getName() + "'.");
            problem.setProperty("property", property.getName());
        }
        return problem;
    }
}
