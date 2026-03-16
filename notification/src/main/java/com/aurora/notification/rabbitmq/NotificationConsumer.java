package com.aurora.notification.rabbitmq;

import com.aurora.clients.notification.NotificationRequest;
import com.aurora.notification.service.NotificationService;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@Slf4j
public class NotificationConsumer {

    private final NotificationService notificationService;

    @RabbitListener(queues = "${rabbitmq.queues.notification}")
    public void consumer(Message message, @Payload NotificationRequest notificationRequest) {
        Context extracted = RabbitMQTracingContext.extract(message);
        try (Scope scope = extracted.makeCurrent()) {
            log.info("Consumed {} from queue", notificationRequest);
            notificationService.send(notificationRequest);
        }
    }
}
