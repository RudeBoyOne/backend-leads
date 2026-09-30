package com.br.semesperanca.backendleads.application.controller

import com.br.semesperanca.backendleads.application.model.LeadDTO
import com.br.semesperanca.backendleads.application.model.LeadReceivedNotificationDTO
import com.br.semesperanca.backendleads.application.model.request.UpdateLeadRequest
import com.br.semesperanca.backendleads.domain.model.Lead
import com.br.semesperanca.backendleads.domain.service.LeadService
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/leads")
class LeadController(
    private val leadService: LeadService,
) {
    private val logger = LoggerFactory.getLogger(LeadController::class.java)

    @PostMapping("/receive")
    @Operation(
        summary = "Webhook",
        description = "Endpoint que recebe os leads dos postais",
    )
    fun receive(
        @Valid @RequestBody receivedNotificationDTO: LeadReceivedNotificationDTO,
        request: HttpServletRequest,
    ): ResponseEntity<Void> {
        leadService.receiveLead(receivedNotificationDTO)

        val endpoint = request.requestURI
        logger.info("Received | Endpoint: {} | LeadId: {}", endpoint, receivedNotificationDTO.originLeadId)

        return ResponseEntity.ok().build()
    }

    @GetMapping("/search")
    @Operation(
        summary = "Lista leads filtros",
        description = "Listagem de leads com filtros dinâmicos",
    )
    fun getLeads(
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) corretorId: Long?,
        @RequestParam(required = false) origin: String?,
        @RequestParam(required = false) search: String?,
        pageable: Pageable,
    ): ResponseEntity<Page<Lead>> {
        val leads: Page<Lead> = leadService.findLeads(status, corretorId, origin, search, pageable)

        logger.info("Fetched Leads | Count: {}", leads.totalElements)

        return ResponseEntity.ok(leads)
    }

    @GetMapping("/search/{id}")
    @Operation(summary = "Buscar lead por ID")
    fun getLeadById(
        @PathVariable("id") id: UUID,
    ): ResponseEntity<LeadDTO> = ResponseEntity.ok(leadService.getById(id))

    @PutMapping("/{id}")
    @Operation(
        summary = "Atualizar lead",
        description = "Atualização de leads os campos ausentes permanecem com o dados antigos",
    )
    fun update(
        @PathVariable("id") id: UUID,
        @Valid @RequestBody updateLeadRequest: UpdateLeadRequest,
        request: HttpServletRequest,
    ): ResponseEntity<LeadDTO> {
        val updatedLead: LeadDTO = leadService.update(id, updateLeadRequest)

        val endpoint = request.requestURI
        logger.info("Updated | Endpoint: {} | LeadId: {}", endpoint, id)

        return ResponseEntity.ok(updatedLead)
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir lead")
    fun delete(
        @PathVariable id: UUID,
        request: HttpServletRequest,
    ): ResponseEntity<Void> {
        leadService.delete(id)

        val endpoint = request.requestURI
        logger.info("Deleted | Endpoint: {} | LeadId: {}", endpoint, id)

        return ResponseEntity.noContent().build()
    }
}
