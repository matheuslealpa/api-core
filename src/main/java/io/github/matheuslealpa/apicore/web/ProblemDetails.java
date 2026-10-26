package io.github.matheuslealpa.apicore.web;

import org.slf4j.MDC;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/**
 * Cria {@link ProblemDetail} já com o {@code traceId} do tracing atual.
 */
public final class ProblemDetails {

    private ProblemDetails() {
    }

    public static ProblemDetail of(HttpStatus status, String detail) {
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
