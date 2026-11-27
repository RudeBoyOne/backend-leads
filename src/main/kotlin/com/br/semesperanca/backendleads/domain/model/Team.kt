package com.br.semesperanca.backendleads.domain.model

import com.br.semesperanca.backendleads.domain.model.user.Manager
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "team")
class Team(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id", nullable = false)
    var manager: Manager? = null,

    @Column(name = "name", nullable = false, length = 100)
    var name: String? = null,

    @Column(name = "description", length = 255)
    var description: String? = null,

    @Column(name = "active", nullable = false)
    var active: Boolean = true,

    @Column(name = "max_agents")
    var maxAgents: Int? = null,

    @Column(name = "goal_amount")
    var goalAmount: Double? = null,

    @Column(name = "created_at", updatable = false)
    var createdAt: LocalDateTime? = null,

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null,
) {
    @OneToMany(mappedBy = "team", fetch = FetchType.LAZY)
    var agents: MutableList<TeamAgent> = mutableListOf()

    @PrePersist
    fun prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now()
        updatedAt = LocalDateTime.now()
    }

    @PreUpdate
    fun preUpdate() {
        updatedAt = LocalDateTime.now()
    }
}
