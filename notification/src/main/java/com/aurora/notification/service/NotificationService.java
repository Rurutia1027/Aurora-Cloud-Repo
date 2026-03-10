package com.aurora.notification.service;

import com.aurora.clients.notification.NotificationRequest;
import com.aurora.notification.dto.Notification;
import com.aurora.notification.repo.NotificationRepository;
import com.aurora.observability.dto.NotificationFlowMonitor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final NotificationFlowMonitor notificationFlowMonitor;

    public NotificationService(NotificationRepository notificationRepository,
                               NotificationFlowMonitor notificationFlowMonitor) {
        this.notificationRepository = notificationRepository;
        this.notificationFlowMonitor = notificationFlowMonitor;
    }

    public void send(NotificationRequest notificationRequest) {
        notificationRepository.save(
                Notification.builder()
                        .toCustomerId(notificationRequest.getToCustomerId())
                        .toCustomerEmail(notificationRequest.getToCustomerEmail())
                        .sender("Emma")
                        .message(notificationRequest.getMessage())
                        .sentAt(LocalDateTime.now())
                        .build()
        );
        notificationFlowMonitor.markSent();
        log.info("Notification stored and counted toUserId={} toEmail={}",
                notificationRequest.getToCustomerId(),
                notificationRequest.getToCustomerEmail());
    }
}
