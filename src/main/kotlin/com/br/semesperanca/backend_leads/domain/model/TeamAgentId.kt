package com.br.semesperanca.backendleads.domain.model

import java.io.Serializable
import java.util.UUID

class TeamAgentId(
    var team: UUID? = null,
    var realEstateAgent: UUID? = null,
) : Serializable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TeamAgentId) return false
        return team == other.team && realEstateAgent == other.realEstateAgent
    }

    override fun hashCode(): Int = 31 * (team?.hashCode() ?: 0) + (realEstateAgent?.hashCode() ?: 0)
}
