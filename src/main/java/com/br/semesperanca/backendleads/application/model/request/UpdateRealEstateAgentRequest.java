package com.br.semesperanca.backendleads.application.model.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateRealEstateAgentRequest(
    @NotBlank(message = "Nome é obrigatório")
        @Size(min = 2, max = 150, message = "Nome deve ter entre 2 e 150 caracteres")
        String name,
    @NotBlank(message = "Email é obrigatório") @Email(message = "Email inválido") String email,
    @NotBlank(message = "CRECI é obrigatório") String creci) {}
