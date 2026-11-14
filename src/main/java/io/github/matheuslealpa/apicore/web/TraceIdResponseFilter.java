package io.github.matheuslealpa.apicore.web;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;

import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Writes the current trace id to a response header, so clients can report it and support can find the trace.
 * Must run inside the observation filter, which opens the trace and fills the {@code traceId} of the MDC.
 */
public class TraceIdResponseFilter extends OncePerRequestFilter {

    private final String headerName;

    public TraceIdResponseFilter(String headerName) {
        this.headerName = headerName;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String traceId = MDC.get("traceId");
        if (traceId != null && !traceId.isBlank()) {
            response.setHeader(headerName, traceId);
        }
        chain.doFilter(request, response);
    }
}
