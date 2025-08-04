package com.br.semesperanca.backendleads.application;

import jakarta.validation.constraints.*;

public record LeadDTO(
    @NotBlank(message = "Lead origin is required") String leadOrigin,
    @NotBlank(message = "Timestamp is required") String timestamp,
    @NotBlank(message = "Origin lead ID is required") String originLeadId,
    @NotBlank(message = "Origin listing ID is required") String originListingId,
    @NotBlank(message = "Client listing ID is required") String clientListingId,
    @NotBlank(message = "Name is required") String name,
    @NotBlank(message = "Email is required") @Email(message = "Email must be valid") String email,
    @NotBlank(message = "DDD is required")
        @Size(min = 2, max = 3, message = "DDD must have 2 or 3 digits")
        String ddd,
    @NotBlank(message = "Phone is required")
        @Size(min = 8, max = 15, message = "Phone must be valid")
        String phone,
    @NotBlank(message = "Message is required") String message,
    @NotBlank(message = "Temperature is required") String temperature,
    @NotBlank(message = "Transaction type is required") String transactionType) {}
