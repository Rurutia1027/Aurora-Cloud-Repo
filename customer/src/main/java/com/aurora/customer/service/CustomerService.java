package com.aurora.customer.service;

import com.aurora.amqp.RabbitMQMessageProducer;
import com.aurora.clients.fraud.FraudCheckResponse;
import com.aurora.clients.fraud.FraudClient;
import com.aurora.clients.notification.NotificationRequest;
import com.aurora.customer.dto.Customer;
import com.aurora.customer.dto.CustomerRegistrationRequest;
import com.aurora.customer.repo.CustomerRepository;
import com.aurora.observability.dto.CustomerFlowMonitor;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@AllArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final FraudClient fraudClient;
    private final RabbitMQMessageProducer rabbitMQMessageProducer;
    private final CustomerFlowMonitor customerFlowMonitor;

    public void registerCustomer(CustomerRegistrationRequest request) {
        Customer customer = Customer.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .id(UUID.randomUUID().variant())
                .build();
        // todo: check if email valid
        // todo: check if email not taken
        customerRepository.saveAndFlush(customer);
        log.info("Customer persisted customerId={} email={}", customer.getId(), customer.getEmail());

        FraudCheckResponse fraudCheckResponse =
                fraudClient.isFraudster(customer.getId());

        if (fraudCheckResponse.getIsFraudster()) {
            log.warn("Fraud detected for customerId={}", customer.getId());
            customerFlowMonitor.markError();
            throw new IllegalStateException("fraudster");
        }

        NotificationRequest notificationRequest = new NotificationRequest(
                customer.getId(),
                customer.getEmail(),
                String.format("Hi %s, welcome to Bank System...",
                        customer.getFirstName())
        );

        rabbitMQMessageProducer.publish(
                notificationRequest,
                "internal.exchange",
                "internal.notification.routing-key"
        );
        log.info("Notification request published to message broker customerId={} email={}",
                customer.getId(), customer.getEmail());
        customerFlowMonitor.markSuccess();
    }
}
