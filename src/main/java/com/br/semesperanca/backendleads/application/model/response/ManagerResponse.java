package com.br.semesperanca.backendleads.application.model.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record ManagerResponse(
    UUID id,
    String name,
    String email,
    String branchOffice,
    String role,
    boolean active,
    LocalDateTime createdAt) {}
