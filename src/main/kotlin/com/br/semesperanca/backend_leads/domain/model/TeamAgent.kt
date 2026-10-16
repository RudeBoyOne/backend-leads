package com.br.semesperanca.backendleads.domain.model

import com.br.semesperanca.backendleads.domain.model.user.RealEstateAgent
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "team_agent")
@IdClass(TeamAgentId::class)
class TeamAgent(
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    var team: Team? = null,

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "real_estate_agent_id", nullable = false)
    var realEstateAgent: RealEstateAgent? = null,

    @Column(name = "joined_at", nullable = false)
    var joinedAt: LocalDateTime? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TeamAgent) return false
        return team?.id == other.team?.id && realEstateAgent?.id == other.realEstateAgent?.id
    }

    override fun hashCode(): Int {
        var result = team?.id?.hashCode() ?: 0
        result = 31 * result + (realEstateAgent?.id?.hashCode() ?: 0)
        return result
    }
}
