package com.aurora.notification.rabbitmq;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import org.springframework.amqp.core.Message;

import java.util.Collections;
import java.util.Map;

/**
 * Extracts OpenTelemetry Context from RabbitMQ message headers (set by producer via
 * TracingMessagePostProcessor).
 */
public final class RabbitMQTracingContext {
    private RabbitMQTracingContext() {
    }

    private static final TextMapGetter<Map<String, Object>> GETTER =
            new TextMapGetter<Map<String, Object>>() {
                @Override
                public Iterable<String> keys(Map<String, Object> carrier) {
                    return carrier == null ? Collections.emptyList() : carrier.keySet();
                }

                @Override
                public String get(Map<String, Object> carrier, String key) {
                    if (carrier == null || key == null) return null;
                    Object value = carrier.get(key);
                    return value == null ? null : value.toString();
                }
            };

    /**
     * Extract Context from message headers. Returns current context unchanged if extraction
     * fails or OTel is not set up.
     */
    public static Context extract(Message message) {
        if (message == null || message.getMessageProperties() == null) {
            return Context.current();
        }

        Map<String, Object> headers = message.getMessageProperties().getHeaders();
        if (headers == null || headers.isEmpty()) {
            return Context.current();
        }

        try {
            return GlobalOpenTelemetry.getPropagators().getTextMapPropagator()
                    .extract(Context.current(), headers, GETTER);
        } catch (Exception e) {
            return Context.current();
        }
    }
}
