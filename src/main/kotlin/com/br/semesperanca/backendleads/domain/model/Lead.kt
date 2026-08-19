package com.br.semesperanca.backendleads.domain.model

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "lead")
open class Lead(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: UUID? = null,

    var realEstateAgencyId: UUID? = null,

    var realEstateAgentId: UUID? = null,

    var portalId: UUID? = null,

    var funnelStageId: UUID? = null,

    var originLeadId: String? = null,

    var originListingId: String? = null,

    var leadOrigin: String? = null,

    var clientListingId: String? = null,

    var name: String? = null,

    var email: String? = null,

    var phoneNumber: String? = null,

    var message: String? = null,

    var temperature: String? = null,

    var transactionType: String? = null,

    var source: String? = null,

    var status: String? = null,

    var insightId: String? = null,

    var timestamp: Instant? = null,

    var createdAt: Instant? = null,

    var updatedAt: Instant? = null
)
