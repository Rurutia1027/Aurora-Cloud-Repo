package com.aurora.customer.integration;

import com.aurora.clients.fraud.FraudCheckResponse;
import com.aurora.clients.fraud.FraudClient;
import com.aurora.clients.notification.NotificationRequest;
import com.aurora.customer.dto.Customer;
import com.aurora.customer.dto.CustomerRegistrationRequest;
import com.aurora.customer.repo.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class CustomerRegistrationIT extends BaseIntegrationTest {
    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CustomerRepository customerRepository;

    /**
     * Stub external dependencies
     */
    @MockBean
    private FraudClient fraudClient;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.queues.notification}")
    private String notificationQueue;

    @Autowired
    private AmqpAdmin amqpAdmin;

    @BeforeEach
    void setupRabbit() {
        Queue queue = new Queue(notificationQueue, true);
        DirectExchange exchange = new DirectExchange("internal.exchange");
        Binding binding = BindingBuilder.bind(queue)
                .to(exchange)
                .with("internal.notification.routing-key");

        amqpAdmin.declareQueue(queue);
        amqpAdmin.declareExchange(exchange);
        amqpAdmin.declareBinding(binding);
    }

    @Test
    void registerCustomer_ShouldPersistCustomer() {
        // Arrange
        when(fraudClient.isFraudster(any()))
                .thenReturn(FraudCheckResponse.builder().isFraudster(false).build());

        String firstName = UUID.randomUUID().toString();
        String lastName = UUID.randomUUID().toString();
        String email = UUID.randomUUID().toString();

        CustomerRegistrationRequest registrationRequest = CustomerRegistrationRequest.builder()
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .build();

        // Act
        ResponseEntity<Void> response = restTemplate.postForEntity("/api/v1/customers",
                registrationRequest, Void.class);

        // Assert: HTTP
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Assert: DB
        Customer savedCustomer =
                customerRepository.findAll().stream().findFirst().orElse(null);
        assertThat(savedCustomer).isNotNull();
        assertThat(savedCustomer.getEmail()).isEqualTo(email);
        assertThat(savedCustomer.getFirstName()).isEqualTo(firstName);
        assertThat(savedCustomer.getLastName()).isEqualTo(lastName);
        assertThat(savedCustomer.getId()).isNotNull();

        // Assert RabbitMQ Data & Metadata info
        NotificationRequest msg = (NotificationRequest) rabbitTemplate
                .receiveAndConvert(notificationQueue);
        assertThat(msg).isNotNull();
        assertThat(msg.getMessage().contains("Hi"));
        QueueInformation qInfo = amqpAdmin.getQueueInfo(notificationQueue);
        assertThat(qInfo.getName().equalsIgnoreCase(notificationQueue));
    }
}
