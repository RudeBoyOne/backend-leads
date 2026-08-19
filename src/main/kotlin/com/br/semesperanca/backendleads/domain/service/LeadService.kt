package com.br.semesperanca.backendleads.domain.service

import com.br.semesperanca.backendleads.application.model.LeadDTO
import com.br.semesperanca.backendleads.application.model.LeadReceivedNotificationDTO
import com.br.semesperanca.backendleads.configuration.MailProperties
import com.br.semesperanca.backendleads.domain.model.Lead
import com.br.semesperanca.backendleads.domain.repository.LeadRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class LeadService(
    private val leadRepository: LeadRepository,
    private val emailService: EmailService,
    private val mailProperties: MailProperties,
    private val insightClient: InsightClient
) {
    fun receiveLead(receivedNotificationDTO: LeadReceivedNotificationDTO) {
        emailService.sendLeadReceived(receivedNotificationDTO, mailProperties.notifyTo ?: "")
        insightClient.createInsight(receivedNotificationDTO.originLeadId)
    }

    fun findLeads(
        status: String?,
        corretorId: Long?,
        origin: String?,
        search: String?,
        pageable: Pageable
    ): Page<Lead> {
        var spec: Specification<Lead> = Specification { _, _, cb -> cb.conjunction() }

        if (status != null) {
            spec = spec.and { root, _, cb -> cb.equal(root.get<Any>("status"), status) }
        }
        if (corretorId != null) {
            spec = spec.and { root, _, cb -> cb.equal(root.get<Any>("clientListingId"), corretorId.toString()) }
        }
        if (origin != null) {
            spec = spec.and { root, _, cb -> cb.equal(root.get<Any>("leadOrigin"), origin) }
        }
        if (search != null) {
            val pattern = "%" + search.lowercase() + "%"
            spec = spec.and { root, _, cb ->
                cb.or(
                    cb.like(cb.lower(root.get<String>("name")), pattern),
                    cb.like(cb.lower(root.get<String>("email")), pattern)
                )
            }
        }

        return leadRepository.findAll(spec, pageable)
    }

    fun getById(id: UUID): LeadDTO {
        if (id == null) {
            throw IllegalArgumentException("the id cant be none.")
        }

        return LeadDTO.fromLead(
            leadRepository.findById(id)
                .orElseThrow { IllegalArgumentException("this id dont exist") }
        )
    }
}