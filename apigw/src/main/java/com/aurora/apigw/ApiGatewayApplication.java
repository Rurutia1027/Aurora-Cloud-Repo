package com.aurora.apigw;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
        "com.aurora.apigw",
        "com.aurora.observability.webflux"
})
@Slf4j
public class ApiGatewayApplication {

    public static void main(String[] args) {
        log.info("Starting API Gateway application");
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}

