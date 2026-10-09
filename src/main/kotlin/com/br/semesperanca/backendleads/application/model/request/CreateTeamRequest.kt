package com.br.semesperanca.backendleads.application.model.request

import jakarta.validation.constraints.NotNull
import java.util.UUID

data class CreateTeamRequest(
    /**
     * A filial do time não é enviada. `team.branch_office_id` saiu do schema: a
     * filial é `manager.branch_office_id`, e mandar as duas deixaria dois
     * endereços para o mesmo fato, able to divergirem no primeiro update parcial.
     */
    @field:NotNull(message = "Manager é obrigatório")
    val managerId: UUID,
)
