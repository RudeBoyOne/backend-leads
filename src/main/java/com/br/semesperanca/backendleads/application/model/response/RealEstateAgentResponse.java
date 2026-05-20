package com.br.semesperanca.backendleads.application.model.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record RealEstateAgentResponse(
    UUID id,
    String name,
    String email,
    String creci,
    String role,
    boolean active,
    LocalDateTime createdAt) {}
