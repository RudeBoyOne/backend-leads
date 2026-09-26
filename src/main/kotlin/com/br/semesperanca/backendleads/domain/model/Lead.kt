package com.br.semesperanca.backendleads.domain.model

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "lead")
open class Lead(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: UUID? = null,

    open var realEstateAgencyId: UUID? = null,

    open var realEstateAgentId: UUID? = null,

    open var portalId: UUID? = null,

    open var funnelStageId: UUID? = null,

    open var originLeadId: String? = null,

    open var originListingId: String? = null,

    open var leadOrigin: String? = null,

    open var clientListingId: String? = null,

    open var name: String? = null,

    open var email: String? = null,

    open var phoneNumber: String? = null,

    open var message: String? = null,

    open var temperature: String? = null,

    open var transactionType: String? = null,

    open var source: String? = null,

    open var status: String? = null,

    open var insightId: String? = null,

    open var timestamp: Instant? = null,

    open var createdAt: Instant? = null,

    open var updatedAt: Instant? = null
) {
    @OneToMany(mappedBy = "lead", fetch = FetchType.LAZY)
    open var activities: MutableList<Activity> = mutableListOf()
}
