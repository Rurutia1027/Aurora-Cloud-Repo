package com.aurora.notification.service;

import com.aurora.clients.notification.NotificationRequest;
import com.aurora.notification.dto.Notification;
import com.aurora.notification.repo.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationServiceTest {
    private NotificationRepository notificationRepository;
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationRepository = mock(NotificationRepository.class);
        notificationService = new NotificationService(notificationRepository);

        // skip actual DB save
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
    }

    @Test
    void send_ShouldSaveNotification() {
        int cid = UUID.randomUUID().variant();
        String customerEmail = "rr@333.com";
        String message = UUID.randomUUID().toString();

        NotificationRequest request = NotificationRequest.builder()
                .toCustomerId(cid)
                .toCustomerEmail(customerEmail)
                .message(message)
                .build();

        notificationService.send(request);

        verify(notificationRepository, times(1))
                .save(argThat(notification ->
                        notification.getToCustomerEmail().equals(customerEmail)
                                && notification.getToCustomerId().equals(cid)
                                && notification.getMessage().equals(message)
                                && notification.getSender().equals("Emma")
                                && notification.getSentAt() != null));
    }

}