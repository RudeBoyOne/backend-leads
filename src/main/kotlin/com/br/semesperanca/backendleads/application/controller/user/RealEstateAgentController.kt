package com.br.semesperanca.backendleads.application.controller.user

import com.br.semesperanca.backendleads.application.model.request.CreateRealEstateAgentRequest
import com.br.semesperanca.backendleads.application.model.request.ResetPasswordRequest
import com.br.semesperanca.backendleads.application.model.request.UpdateRealEstateAgentRequest
import com.br.semesperanca.backendleads.application.model.request.UserFilterRequest
import com.br.semesperanca.backendleads.application.model.response.RealEstateAgentResponse
import com.br.semesperanca.backendleads.domain.service.security.AuthService
import com.br.semesperanca.backendleads.domain.service.user.RealEstateAgentService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/real-estate-agents")
@Tag(name = "Corretores", description = "Gerenciamento de corretores imobiliários")
class RealEstateAgentController(
    private val service: RealEstateAgentService,
    private val authService: AuthService,
) {
    @GetMapping
    @Operation(summary = "Listar corretores", description = "Requer FULL_ADMIN ou ADMIN")
    fun findAll(
        @RequestParam(required = false) active: Boolean?,
        @RequestParam(required = false) search: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
    ): ResponseEntity<Page<RealEstateAgentResponse>> {
        val pageable: Pageable = PageRequest.of(page, size)
        val filter = UserFilterRequest(active, search)
        return ResponseEntity.ok(service.findAll(filter, pageable))
    }

    @PostMapping
    @Operation(summary = "Criar corretor", description = "Requer FULL_ADMIN ou ADMIN")
    fun create(
        @RequestBody @Valid request: CreateRealEstateAgentRequest,
    ): ResponseEntity<RealEstateAgentResponse> = ResponseEntity.status(HttpStatus.CREATED).body(service.create(request))

    @GetMapping("/{id}")
    @Operation(
        summary = "Buscar corretor por ID",
        description = "Requer FULL_ADMIN, ADMIN ou REAL_ESTATE_AGENT",
    )
    fun findById(
        @PathVariable id: UUID,
    ): ResponseEntity<RealEstateAgentResponse> = ResponseEntity.ok(service.findById(id))

    @PutMapping("/{id}")
    @Operation(
        summary = "Atualizar corretor",
        description = "Requer FULL_ADMIN, ADMIN ou REAL_ESTATE_AGENT",
    )
    fun update(
        @PathVariable id: UUID,
        @RequestBody @Valid request: UpdateRealEstateAgentRequest,
    ): ResponseEntity<RealEstateAgentResponse> = ResponseEntity.ok(service.update(id, request))

    @PatchMapping("/{id}/toggle-active")
    @Operation(summary = "Ativar/Desativar corretor", description = "Requer FULL_ADMIN ou ADMIN")
    fun toggleActive(
        @PathVariable id: UUID,
    ): ResponseEntity<RealEstateAgentResponse> = ResponseEntity.ok(service.toggleActive(id))

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir corretor", description = "Requer FULL_ADMIN ou ADMIN")
    fun delete(
        @PathVariable id: UUID,
    ): ResponseEntity<Void> {
        service.delete(id)
        return ResponseEntity.noContent().build()
    }

    @PatchMapping("/{id}/reset-password")
    @Operation(summary = "Resetar senha do corretor", description = "Requer FULL_ADMIN ou ADMIN")
    fun resetPassword(
        @PathVariable id: UUID,
        @RequestBody @Valid request: ResetPasswordRequest,
    ): ResponseEntity<Void> {
        authService.resetPassword(id, request)
        return ResponseEntity.noContent().build()
    }
}
