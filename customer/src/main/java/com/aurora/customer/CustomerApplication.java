package com.aurora.customer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = {
        "com.aurora.customer",
        "com.aurora.amqp"
})
@EnableFeignClients(basePackages = {
        "com.aurora.clients.fraud",
        "com.aurora.clients.notification"
})
public class CustomerApplication {
    public static void main(String[] args) {
        SpringApplication.run(CustomerApplication.class, args);
    }
}