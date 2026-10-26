package io.github.matheuslealpa.apicore.web;

import org.jspecify.annotations.Nullable;
import org.slf4j.MDC;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/**
 * Creates a {@link ProblemDetail} carrying the {@code traceId} of the current trace.
 */
public final class ProblemDetails {

    private ProblemDetails() {
    }

    public static ProblemDetail of(HttpStatus status, @Nullable String detail) {
        return withTraceId(ProblemDetail.forStatusAndDetail(status, detail));
    }

    public static ProblemDetail withTraceId(ProblemDetail problem) {
        String traceId = MDC.get("traceId");
        if (traceId != null) {
            problem.setProperty("traceId", traceId);
        }
        return problem;
    }
}
