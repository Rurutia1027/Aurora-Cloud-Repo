package com.aurora.notification.integration;

import com.aurora.clients.notification.NotificationRequest;
import com.aurora.notification.dto.Notification;
import com.aurora.notification.repo.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class NotificationControllerIT extends BaseNotificationIntegrationTest {
    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private NotificationRepository notificationRepository;


    @Test
    void shouldSendNotificationAndPersistIt() {
        NotificationRequest request = new NotificationRequest();
        request.setToCustomerId(1001);
        request.setToCustomerEmail("test@example.com");
        request.setMessage("Hello Integration Test!");

        // Send POST request to controller
        ResponseEntity<Void> response = restTemplate.postForEntity(
                "/api/v1/notification",
                new HttpEntity<>(request),
                Void.class
        );

        // Assert HTTP response
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

        // Assert that the notification was persisted
        List<Notification> notifications = notificationRepository.findAll();
        assertThat(notifications).hasSize(1);
        Notification saved = notifications.get(0);
        assertThat(saved.getToCustomerEmail()).isEqualTo("test@example.com");
        assertThat(saved.getMessage()).isEqualTo("Hello Integration Test!");
        assertThat(saved.getSender()).isEqualTo("Emma");
        assertThat(saved.getSentAt()).isNotNull();
    }
}
