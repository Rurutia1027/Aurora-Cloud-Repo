package com.aurora.fraud.integration;

import com.aurora.clients.fraud.FraudCheckResponse;
import com.aurora.fraud.FraudCheckHistory;
import com.aurora.fraud.FraudCheckHistoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class FraudServiceIT extends BaseFraudIntegrationTest {
    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private FraudCheckHistoryRepository fraudCheckHistoryRepository;

    @Test
    void shouldReturnFraudCheckAndPersistHistory() {
        Integer customerId = UUID.randomUUID().variant();

        // Call controller endpoint
        ResponseEntity<FraudCheckResponse> response = restTemplate.getForEntity(
                "/api/v1/fraud/check/{customerId}",
                FraudCheckResponse.class,
                customerId);

        // Assert controller response
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getIsFraudster()).isFalse();


        // Assert DB persistence
        List<FraudCheckHistory> histories =
                fraudCheckHistoryRepository.findAll();
        assertThat(histories.size()).isGreaterThanOrEqualTo(1);
        FraudCheckHistory history = histories.get(0);
        assertThat(history.getCreatedAt()).isNotNull();
        assertThat(history.getCustomerId()).isEqualTo(customerId);
        assertThat(history.getIsFraudster()).isEqualTo(false);
    }
}
