package io.github.matheuslealpa.apicore.web;

import io.github.matheuslealpa.apicore.exception.BusinessException;
import io.github.matheuslealpa.apicore.exception.PreconditionFailedException;
import io.github.matheuslealpa.apicore.exception.ResourceNotFoundException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.*;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;
import java.util.Map;

/**
 * Converts exceptions into {@code application/problem+json}, including the {@code traceId} for correlation with traces.
 */
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({ResourceNotFoundException.class, EntityNotFoundException.class})
    public ProblemDetail handleNotFound(RuntimeException exception) {
        return problem(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusiness(BusinessException exception) {
        return problem(HttpStatus.UNPROCESSABLE_CONTENT, exception.getMessage());
    }

    @ExceptionHandler(PreconditionFailedException.class)
    public ProblemDetail handlePreconditionFailed(PreconditionFailedException exception) {
        return problem(HttpStatus.PRECONDITION_FAILED, exception.getMessage());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException exception) {
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Invalid request.");
        problem.setProperty("errors", exception.getConstraintViolations().stream()
                .map(violation -> Map.of("field", violation.getPropertyPath().toString(),
                        "message", violation.getMessage()))
                .toList());
        return problem;
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLock(ObjectOptimisticLockingFailureException exception) {
        return problem(HttpStatus.CONFLICT, "The resource was modified by another request.");
    }

    // Unique, foreign key or not-null violations. The database message is logged, never returned to the client.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException exception) {
        log.debug("Data integrity violation", exception);
        return problem(HttpStatus.CONFLICT, "The request conflicts with existing data.");
    }

    // Unknown property in ?sort=, raised by Spring Data when building the query.
    @ExceptionHandler(PropertyReferenceException.class)
    public ProblemDetail handlePropertyReference(PropertyReferenceException exception) {
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Invalid property '" + exception.getPropertyName() + "'.");
        problem.setProperty("property", exception.getPropertyName());
        return problem;
    }

    // Thrown by @PreAuthorize inside the controller: must stay 401/403 instead of becoming 500.
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException exception) {
        return problem(HttpStatus.FORBIDDEN, "Access denied.");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthentication(AuthenticationException exception) {
        return problem(HttpStatus.UNAUTHORIZED, "Authentication required.");
    }

    /**
     * Fallback for anything else. Exceptions annotated with {@link ResponseStatus} keep their status, since this
     * handler runs before Spring's {@code ResponseStatusExceptionResolver}.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception exception) {
        ResponseStatus status = AnnotatedElementUtils.findMergedAnnotation(exception.getClass(), ResponseStatus.class);
        if (status != null) {
            if (status.code().is5xxServerError()) {
                log.error("Request failed with status {}", status.code().value(), exception);
            }
            String detail = status.reason().isBlank() ? exception.getMessage() : status.reason();
            return ResponseEntity.status(status.code()).body(problem(status.code(), detail));
        }
        log.error("Unexpected error", exception);
        return ResponseEntity.internalServerError()
                .body(problem(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error."));
    }

    @Override
    protected @Nullable ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
                                                                  HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = exception.getBody();
        List<Map<String, String>> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of("field", error.getField(),
                        "message", String.valueOf(error.getDefaultMessage())))
                .toList();
        problem.setProperty("errors", errors);
        return handleExceptionInternal(exception, problem, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> createResponseEntity(@Nullable Object body, HttpHeaders headers, HttpStatusCode status,
                                                          WebRequest request) {
        if (body instanceof ProblemDetail problem) {
            ProblemDetails.withTraceId(problem);
        }
        return super.createResponseEntity(body, headers, status, request);
    }

    private static ProblemDetail problem(HttpStatus status, @Nullable String detail) {
        return ProblemDetails.of(status, detail);
    }
}
