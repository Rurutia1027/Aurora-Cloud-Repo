package com.aurora.observability.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Snapshot-style metrics monitor for fraud checks.
 */
@Component
public class FraudFlowMonitor {

    private final AtomicLong totalChecks = new AtomicLong();

    public FraudFlowMonitor(MeterRegistry meterRegistry) {
        Gauge.builder("fraud_checks_total", totalChecks, AtomicLong::get)
                .tag("service", "fraud")
                .register(meterRegistry);
    }

    public void markCheck() {
        totalChecks.incrementAndGet();
    }
}

