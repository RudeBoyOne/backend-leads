package com.br.semesperanca.backend_leads.domain.model

import java.io.Serializable
import java.util.UUID

/**
 * Identificador composto de [TeamAgent]. O Hibernate exige uma classe de
 * identificador sempre que a entidade tem mais de um atributo @Id; os nomes
 * das propriedades aqui precisam bater exatamente com os atributos @Id da
 * entidade (team e realEstateAgent), guardando o UUID e não a association.
 */
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
