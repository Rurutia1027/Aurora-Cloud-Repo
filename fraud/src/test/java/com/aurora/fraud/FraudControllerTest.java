package com.aurora.fraud;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FraudController.class)
class FraudControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FraudCheckService fraudCheckService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        // Mock service to always return false (skip internal logic)
        when(fraudCheckService.isFraudulentCustomer(anyInt())).thenReturn(false);
    }

    @Test
    void isFraudster_ShouldReturnFalse() throws Exception {
        Integer cid = UUID.randomUUID().variant();

        mockMvc.perform(get("/api/v1/fraud-check/{customerId}", cid)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isFraudster").value(false));
    }

}