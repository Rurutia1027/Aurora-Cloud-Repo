package com.aurora.observability.webflux;

import com.aurora.observability.TracingContextFilter;
import io.opentelemetry.api.trace.Span;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Reactive equivalent of {@link com.aurora.observability.TracingContextFilter}: writes
 * request headers (X-User-Id, X-Request-Id, X-Tenant-Id) onto the current OpenTelemetry span.
 * Use in <b>apigw</b> (Spring Cloud Gateway).
 * See docs/tracing-design-microservices.md for more details.
 */

@Component
@Order(1)
public class TracingContextWebFilter implements WebFilter {
    private static final String ATTR_USER_ID = "user.id";
    private static final String ATTR_REQUEST_ID = "request.id";
    private static final String ATTR_TENANT_ID = "tenant.id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        Span span = Span.current();
        if (span.getSpanContext().isValid()) {
            setIfPresent(span, ATTR_USER_ID, exchange.getRequest()
                    .getHeaders().getFirst(TracingContextFilter.HEADER_USER_ID));
            setIfPresent(span, ATTR_REQUEST_ID, exchange.getRequest()
                    .getHeaders().getFirst(TracingContextFilter.HEADER_REQUEST_ID));
            setIfPresent(span, ATTR_TENANT_ID, exchange.getRequest()
                    .getHeaders().getFirst(TracingContextFilter.HEADER_TENANT_ID));
        }
        return chain.filter(exchange);
    }

    private static void setIfPresent(Span span, String key, String value) {
        if (value != null && !value.isBlank()) {
            span.setAttribute(key, value);
        }
    }
}