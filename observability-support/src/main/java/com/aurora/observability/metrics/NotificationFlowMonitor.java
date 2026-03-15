package com.aurora.observability.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Snapshot-style metrics monitor for notifications sent.
 */
@Component
public class NotificationFlowMonitor {

    private final AtomicLong notificationsSent = new AtomicLong();

    public NotificationFlowMonitor(MeterRegistry meterRegistry) {
        Gauge.builder("notifications_sent_total", notificationsSent, AtomicLong::get)
                .tag("service", "notification")
                .register(meterRegistry);
    }

    public void markSent() {
        notificationsSent.incrementAndGet();
    }
}

