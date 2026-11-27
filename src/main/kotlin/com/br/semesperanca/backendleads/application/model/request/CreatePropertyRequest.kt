package com.br.semesperanca.backendleads.application.model.request

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.util.UUID

data class CreatePropertyRequest(
    @field:NotBlank(message = "title é obrigatório")
    @field:Size(max = 150, message = "title deve ter no máximo 150 caracteres")
    val title: String,

    val description: String?,

    @field:DecimalMin(value = "0.0", inclusive = true, message = "price não pode ser negativo")
    val price: BigDecimal?,

    val propertyType: String?,

    @field:Min(value = 0, message = "bedrooms não pode ser negativo")
    @field:Max(value = 100, message = "bedrooms inválido")
    val bedrooms: Int?,

    @field:Min(value = 0, message = "bathrooms não pode ser negativo")
    @field:Max(value = 100, message = "bathrooms inválido")
    val bathrooms: Int?,

    @field:DecimalMin(value = "0.0", inclusive = true, message = "areaSqm não pode ser negativo")
    val areaSqm: BigDecimal?,

    @field:Size(max = 255, message = "address deve ter no máximo 255 caracteres")
    val address: String?,

    @field:Size(max = 100, message = "city deve ter no máximo 100 caracteres")
    val city: String?,

    @field:Size(min = 2, max = 2, message = "state deve ter 2 caracteres")
    val state: String?,

    @field:Size(max = 10, message = "zipCode deve ter no máximo 10 caracteres")
    val zipCode: String?,

    @field:NotNull(message = "branchOfficeId é obrigatório")
    val branchOfficeId: UUID,
    @field:Pattern(
        regexp = "available|reserved|sold|rented|inactive",
        message = "status deve ser um dos seguintes valores: available, reserved, sold, rented, inactive",
    )
    val status: String? = "available",
)