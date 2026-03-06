package com.aurora.observability;

import io.opentelemetry.api.trace.Span;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Writes request headers (X-User-Id, X-Request-Id, X-Tenant-Id) onto the current
 * OpenTelemetry span.
 * Use this filter in:
 * <ul>
 * <li>apigw (gateway) - unified entry: set/forward these headers and set span attributes so
 * the whole trace has context.</li>
 * <li>customer, fraud, notification - so each backend's span has user/request/tenant
 * attributes (whether traffic comes via gateway or direct).</li>
 * </ul>
 * <p>
 * All apps are peers; the gateway is the single place to originate headers; every service
 * that can receive a request should use this filter so its span is enriched. See
 * docs/probes-tracing-context.md
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


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Span span = Span.current();
        if (span.getSpanContext().isValid()) {
            setIfPresent(span, ATTR_USER_ID, request.getHeader(HEADER_USER_ID));
            setIfPresent(span, ATTR_REQUEST_ID, request.getHeader(HEADER_REQUEST_ID));
            setIfPresent(span, ATTR_TENANT_ID, request.getHeader(HEADER_TENANT_ID));
        }
        filterChain.doFilter(request, response);
    }

    private static void setIfPresent(Span span, String key, String value) {
        if (value != null && !value.isBlank()) {
            span.setAttribute(key, value);
        }
    }
}