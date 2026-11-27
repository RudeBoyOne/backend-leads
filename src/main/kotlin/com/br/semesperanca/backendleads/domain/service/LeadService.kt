package com.br.semesperanca.backendleads.domain.service

import com.br.semesperanca.backendleads.application.model.LeadDTO
import com.br.semesperanca.backendleads.application.model.LeadReceivedNotificationDTO
import com.br.semesperanca.backendleads.application.model.request.CreateLeadRequest
import com.br.semesperanca.backendleads.application.model.request.UpdateLeadRequest
import com.br.semesperanca.backendleads.configuration.MailProperties
import com.br.semesperanca.backendleads.configuration.exception.NotFoundException
import com.br.semesperanca.backendleads.domain.model.Lead
import com.br.semesperanca.backendleads.domain.repository.LeadRepository
import com.br.semesperanca.backendleads.domain.repository.RealEstateAgencyRepository
import com.br.semesperanca.backendleads.domain.repository.user.RealEstateAgentRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class LeadService(
    private val leadRepository: LeadRepository,
    private val emailService: EmailService,
    private val mailProperties: MailProperties,
    private val insightClient: InsightClient,
    private val realEstateAgentRepository: RealEstateAgentRepository,
    private val realEstateAgencyRepository: RealEstateAgencyRepository,
) {
    @PreAuthorize("hasRole('FULL_ADMIN') or hasRole('ADMIN') or hasRole('MANAGER')")
    @Transactional
    fun create(request: CreateLeadRequest): LeadDTO {
        val agency =
            realEstateAgencyRepository
                .findById(request.realEstateAgencyId)
                .orElseThrow { NotFoundException("Imobiliária não encontrada") }
        val agent =
            request.realEstateAgentId?.let { agentId ->
                realEstateAgentRepository.findById(agentId).orElseThrow { NotFoundException("Corretor não encontrado") }
            }

        val lead =
            Lead(
                realEstateAgencyId = agency.id,
                realEstateAgent = agent,
                portalId = request.portalId,
                funnelStageId = request.funnelStageId,
                originLeadId = request.originLeadId,
                originListingId = request.originListingId,
                leadOrigin = request.leadOrigin,
                clientListingId = request.clientListingId,
                name = request.name,
                email = request.email,
                phoneNumber = request.phoneNumber,
                message = request.message,
                temperature = request.temperature,
                transactionType = request.transactionType,
                source = request.source,
                status = request.status ?: "new",
                active = true,
                timestamp = Instant.now(),
            )

        return LeadDTO.fromLead(leadRepository.save(lead))
    }

    fun receiveLead(receivedNotificationDTO: LeadReceivedNotificationDTO) {
        emailService.sendLeadReceived(receivedNotificationDTO, mailProperties.notifyTo ?: "")
        insightClient.createInsight(findByOriginLeadId(receivedNotificationDTO.originLeadId))
    }

    private fun findByOriginLeadId(originLeadId: String): Lead {
        val spec = Specification<Lead> { root, _, cb -> cb.equal(root.get<Any>("originLeadId"), originLeadId) }
        return leadRepository.findOne(spec).orElseThrow { NotFoundException("Lead não encontrado") }
    }

    fun findLeads(
        status: String?,
        corretorId: Long?,
        origin: String?,
        search: String?,
        pageable: Pageable,
    ): Page<Lead> {
        var spec: Specification<Lead> = Specification.unrestricted()

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
            spec =
                spec.and { root, _, cb ->
                    cb.or(
                        cb.like(cb.lower(root.get<String>("name")), pattern),
                        cb.like(cb.lower(root.get<String>("email")), pattern),
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
            leadRepository
                .findById(id)
                .orElseThrow { NotFoundException("Lead não encontrado") },
        )
    }

    @PreAuthorize("hasRole('FULL_ADMIN') or hasRole('ADMIN') or hasRole('MANAGER')")
    @Transactional
    fun delete(id: UUID) {
        val lead =
            leadRepository
                .findById(id)
                .orElseThrow { NotFoundException("Lead não encontrado") }
        leadRepository.delete(lead)
    }

    @PreAuthorize("hasRole('FULL_ADMIN') or hasRole('ADMIN') or hasRole('MANAGER') or hasRole('REAL_ESTATE_AGENT')")
    @Transactional
    fun update(
        id: UUID,
        request: UpdateLeadRequest,
    ): LeadDTO {
        val lead = leadRepository.findById(id).orElseThrow { NotFoundException("Lead não encontrado") }

        request.name?.let { lead.name = it }

        request.email?.let { lead.email = it }

        request.phoneNumber?.let { lead.phoneNumber = it.toLongOrNull() ?: lead.phoneNumber }

        request.message?.let { lead.message = it }
        request.temperature?.let { lead.temperature = it }
        request.transactionType?.let { lead.transactionType = it }
        request.source?.let { lead.source = it }
        request.status?.let { lead.status = it }
        request.realEstateAgentId?.let { agentId ->
            lead.realEstateAgent = realEstateAgentRepository.findById(agentId).orElse(null)
        }
        request.funnelStageId?.let { lead.funnelStageId = it }
        lead.updatedAt = Instant.now()

        return LeadDTO.fromLead(leadRepository.save(lead))
    }
}
