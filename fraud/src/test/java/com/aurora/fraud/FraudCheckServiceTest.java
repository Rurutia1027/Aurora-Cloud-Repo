package com.aurora.fraud;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FraudCheckServiceTest {
    private FraudCheckHistoryRepository fraudCheckHistoryRepository;
    private FraudCheckService fraudCheckService;

    @BeforeEach
    void setUp() {
        fraudCheckHistoryRepository = mock(FraudCheckHistoryRepository.class);
        fraudCheckService = new FraudCheckService(fraudCheckHistoryRepository);

        // skip actual DB save (in ut cases, we do our best make each feature tested
        // isolated from external services/framework interactions/dependencies)
        when(fraudCheckHistoryRepository.save(any(FraudCheckHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void isFraudCustomer_ShouldReturnFalseAndSaveHistory() {
        Integer cid = UUID.randomUUID().variant();
        boolean result = fraudCheckService.isFraudulentCustomer(cid);
        assertFalse(result);
        // Verify a record is saved to db (db layer service is invoked with expected
        // parameters)
        verify(fraudCheckHistoryRepository, times(1))
                .save(argThat(history -> history.getCustomerId().equals(cid)
                        && history.getIsFraudster().equals(false)
                        && history.getCreatedAt() != null
                ));
    }
}