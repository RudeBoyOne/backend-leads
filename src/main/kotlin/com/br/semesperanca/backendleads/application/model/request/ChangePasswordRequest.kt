package com.br.semesperanca.backendleads.application.model.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class ChangePasswordRequest(
    @field:NotBlank(message = "Senha atual é obrigatória")
    val currentPassword: String,
    @field:NotBlank(message = "Nova senha é obrigatória")
    @field:Size(min = 6, message = "Nova senha deve ter no mínimo 6 caracteres")
    val newPassword: String,
    @field:NotBlank(message = "Confirmação de senha é obrigatória")
    val confirmPassword: String
)