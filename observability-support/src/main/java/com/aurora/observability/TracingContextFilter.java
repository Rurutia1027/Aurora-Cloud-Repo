package com.aurora.observability;

import io.opentelemetry.api.trace.Span;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Writes request headers (X-User-Id, X-Request-Id, X-Tenant-Id) onto the current OpenTelemetry span
 * and into MDC so logs carry trace_id, span_id, and optional business context. Clears MDC in finally
 * so the same thread can be reused without leaking previous request context (TTL safety). See docs/observability-mdc-context.md.
 */
@Component
@Order(1)
public class TracingContextFilter extends OncePerRequestFilter {

    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_REQUEST_ID = "X-Request-Id";
    public static final String HEADER_TENANT_ID = "X-Tenant-Id";
    private static final String ATTR_USER_ID = "user.id";
    private static final String ATTR_REQUEST_ID = "request.id";
    private static final String ATTR_TENANT_ID = "tenant.id";

    /**
     * MDC keys for logging; match common OTel / Loki conventions.
     */
    public static final String MDC_TRACE_ID = "trace_id";
    public static final String MDC_SPAN_ID = "span_id";
    public static final String MDC_USER_ID = "user_id";
    public static final String MDC_REQUEST_ID = "request_id";
    public static final String MDC_TENANT_ID = "tenant_id";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        Span span = Span.current();
        if (span.getSpanContext().isValid()) {
            setIfPresent(span, ATTR_USER_ID, request.getHeader(HEADER_USER_ID));
            setIfPresent(span, ATTR_REQUEST_ID, request.getHeader(HEADER_REQUEST_ID));
            setIfPresent(span, ATTR_TENANT_ID, request.getHeader(HEADER_TENANT_ID));
            putMdc(span, request);
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }

    private void putMdc(Span span, HttpServletRequest request) {
        String traceId = span.getSpanContext().getTraceId();
        String spanId = span.getSpanContext().getSpanId();
        if (traceId != null && !traceId.isEmpty()) MDC.put(MDC_TRACE_ID, traceId);
        if (spanId != null && !spanId.isEmpty()) MDC.put(MDC_SPAN_ID, spanId);
        putMdcIfPresent(MDC_USER_ID, request.getHeader(HEADER_USER_ID));
        putMdcIfPresent(MDC_REQUEST_ID, request.getHeader(HEADER_REQUEST_ID));
        putMdcIfPresent(MDC_TENANT_ID, request.getHeader(HEADER_TENANT_ID));
    }

    private static void putMdcIfPresent(String key, String value) {
        if (value != null && !value.isBlank()) MDC.put(key, value);
    }

    private static void setIfPresent(Span span, String key, String value) {
        if (value != null && !value.isBlank()) {
            span.setAttribute(key, value);
        }
    }
}
