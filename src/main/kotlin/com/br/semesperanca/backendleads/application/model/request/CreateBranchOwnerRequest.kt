package com.br.semesperanca.backendleads.application.model.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.util.UUID

data class CreateBranchOwnerRequest(
    @field:NotNull(message = "realEstateAgencyId é obrigatório")
    val realEstateAgencyId: UUID,
    @field:NotBlank(message = "name é obrigatório")
    @field:Size(max = 150, message = "name deve ter no máximo 150 caracteres")
    val name: String,
    /**
     * `users.email` é NOT NULL e UNIQUE. O responsável é um `User` e precisa
     * autenticar para gerenciar os gerentes da sua filial, então o e-mail deixa de
     * ser opcional e passa a ser a identidade dele no login.
     */
    @field:NotBlank(message = "email é obrigatório")
    @field:Email(message = "email deve ser válido")
    @field:Size(max = 150, message = "email deve ter no máximo 150 caracteres")
    val email: String,
    @field:NotBlank(message = "password é obrigatório")
    @field:Size(min = 8, max = 255, message = "password deve ter entre 8 e 255 caracteres")
    val password: String,
    @field:Size(max = 20, message = "phone deve ter no máximo 20 caracteres")
    val phone: String?,
)
