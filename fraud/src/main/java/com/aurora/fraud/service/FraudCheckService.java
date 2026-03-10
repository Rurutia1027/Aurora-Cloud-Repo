package com.aurora.fraud.service;

import com.aurora.fraud.dto.FraudCheckHistory;
import com.aurora.fraud.repo.FraudCheckHistoryRepository;
import com.aurora.observability.dto.FraudFlowMonitor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
public class FraudCheckService {

    private final FraudCheckHistoryRepository fraudCheckHistoryRepository;
    private final FraudFlowMonitor fraudFlowMonitor;

    public FraudCheckService(FraudCheckHistoryRepository fraudCheckHistoryRepository,
                             FraudFlowMonitor fraudFlowMonitor) {
        this.fraudCheckHistoryRepository = fraudCheckHistoryRepository;
        this.fraudFlowMonitor = fraudFlowMonitor;
    }

    public boolean isFraudulentCustomer(Integer customerId) {
        fraudFlowMonitor.markCheck();
        fraudCheckHistoryRepository.save(
                FraudCheckHistory.builder()
                        .customerId(customerId)
                        .isFraudster(false)
                        .createdAt(LocalDateTime.now())
                        .build()
        );
        log.info("Fraud check recorded for customerId={}", customerId);
        return false;
    }

}
