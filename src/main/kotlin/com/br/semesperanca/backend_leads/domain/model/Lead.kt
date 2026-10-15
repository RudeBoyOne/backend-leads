package com.br.semesperanca.backendleads.domain.model

import com.br.semesperanca.backendleads.domain.model.user.RealEstateAgent
import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "lead")
class Lead(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: UUID? = null,
    var realEstateAgencyId: UUID? = null,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "real_estate_agent_id")
    var realEstateAgent: RealEstateAgent? = null,
    
    @Column(name = "portal_id", nullable = true)
    var portalId: UUID? = null,

    @Column(name = "funnel_stage_id", nullable = true, length = 50)
    var funnelStageId: UUID? = null,
    
    @Column(name = "origin_lead_id", nullable = true, length = 100)
    var originLeadId: String? = null,
    
    @Column(name = "origin_listing_id", nullable = true, length = 100)
    var originListingId: String? = null,

    @Column(name = "lead_origin", nullable = true, length = 50)
    var leadOrigin: String? = null,
    
    @Column(name = "client_listing_id", nullable = true, length = 50)
    var clientListingId: String? = null,

    @Column(name = "name", nullable = true, length = 150)
    var name: String? = null,

    @Column(name = "email", nullable = true, length = 100)
    var email: String? = null,

    @Column(name = "phone_number")
    var phoneNumber: Long? = null,
    
    @Column(name = "message", nullable = true, length = 500)
    var message: String? = null,
    
    @Column(name = "temperature", nullable = true, length = 100)
    var temperature: String? = null,
    
    @Column(name = "transaction_type", nullable = true, length = 100)
    var transactionType: String? = null,
    
    @Column(name = "source", nullable = true, length = 100)
    var source: String? = null,
    
    @Column(name = "status", nullable = true, length = 30)
    var status: String? = null,
    
    @Column(name = "insight_id", nullable = true, length = 100)
    var insightId: String? = null,
    
    @Column(name = "timestamp", nullable = true)
    var timestamp: Instant? = null,
    
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant? = null, 

    @Column(name = "updated_at", nullable = false, updatable = true)
    var updatedAt: Instant? = null,
) {
    @OneToMany(mappedBy = "lead", fetch = FetchType.LAZY)
    var activities: MutableList<Activity> = mutableListOf()

    @PrePersist
    fun prePersist() {
        val agora = Instant.now()
        if (createdAt == null) {
            createdAt = agora
        }
        if (updatedAt == null) {
            updatedAt = agora
        }
    }

    @PreUpdate
    fun preUpdate() {
        updatedAt = Instant.now()
    }
}