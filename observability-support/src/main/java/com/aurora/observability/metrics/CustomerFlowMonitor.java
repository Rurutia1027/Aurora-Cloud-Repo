package com.aurora.observability.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Snapshot-style business metrics monitor for customer registrations.
 * Business code only calls markSuccess/markError; this class owns Micrometer metrics.
 */
@Component
public class CustomerFlowMonitor {
    private final AtomicLong success = new AtomicLong();
    private final AtomicLong error = new AtomicLong();

    public CustomerFlowMonitor(MeterRegistry meterRegistry) {
        Gauge.builder("customer_registrations_total", success, AtomicLong::get)
                .tag("service", "customer")
                .tags("outcome", "success")
                .register(meterRegistry);

        Gauge.builder("customer_registrations_total", error, AtomicLong::get)
                .tags("service", "customer")
                .tag("outcome", "error")
                .register(meterRegistry);
    }

    public void markSuccess() {
        success.incrementAndGet();
    }

    public void markError() {
        error.incrementAndGet();
    }
}
