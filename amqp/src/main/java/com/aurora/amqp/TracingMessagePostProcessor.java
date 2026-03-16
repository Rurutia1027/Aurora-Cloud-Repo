package com.aurora.amqp;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapSetter;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;

/**
 * Injects the current OpenTelemetry Context (trace_id, span_id, baggage) into RabbitMQ
 * message headers so the consumer can extract and continue the same trace.
 */
public class TracingMessagePostProcessor implements MessagePostProcessor {
    private static final TextMapSetter<MessageProperties> SETTER = (carrier, key, value) -> {
        if (carrier != null && key != null && value != null) {
            carrier.setHeader(key, value);
        }
    };

    @Override
    public Message postProcessMessage(Message message) throws AmqpException {
        if (message == null || message.getMessageProperties() == null) {

        }
        Context current = Context.current();
        if (current == null || current == Context.root()) {
            return message;
        }

        try {
            GlobalOpenTelemetry.getPropagators().getTextMapPropagator()
                    .inject(current, message.getMessageProperties(), SETTER);
        } catch (Exception e) {
            // no-op if OTel not initialized (e.g., agent not attached)
        }
        return message;
    }
}
