package com.br.semesperanca.backendleads.application.controller.user

import com.br.semesperanca.backendleads.application.model.request.CreateManagerRequest
import com.br.semesperanca.backendleads.application.model.request.ResetPasswordRequest
import com.br.semesperanca.backendleads.application.model.request.UpdateManagerRequest
import com.br.semesperanca.backendleads.application.model.request.UserFilterRequest
import com.br.semesperanca.backendleads.application.model.response.ManagerResponse
import com.br.semesperanca.backendleads.domain.service.security.AuthService
import com.br.semesperanca.backendleads.domain.service.user.ManagerService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
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
@RequestMapping("/api/managers")
@Tag(name = "Gerentes", description = "Gerenciamento de gerentes")
class ManagerController(
    private val service: ManagerService,
    private val authService: AuthService
) {
    @GetMapping
    @Operation(summary = "Listar gerentes", description = "Requer FULL_ADMIN ou ADMIN")
    fun findAll(
        @RequestParam(required = false) active: Boolean?,
        @RequestParam(required = false) search: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<Page<ManagerResponse>> {
        val pageable: Pageable = PageRequest.of(page, size)
        val filter = UserFilterRequest(active, search)
        return ResponseEntity.ok(service.findAll(filter, pageable))
    }

    @PostMapping
    @Operation(summary = "Criar gerente", description = "Requer FULL_ADMIN ou ADMIN")
    fun create(@RequestBody @Valid request: CreateManagerRequest): ResponseEntity<ManagerResponse> {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request))
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar gerente por ID")
    fun findById(@PathVariable id: UUID): ResponseEntity<ManagerResponse> {
        return ResponseEntity.ok(service.findById(id))
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar gerente")
    fun update(
        @PathVariable id: UUID,
        @RequestBody @Valid request: UpdateManagerRequest
    ): ResponseEntity<ManagerResponse> {
        return ResponseEntity.ok(service.update(id, request))
    }

    @PatchMapping("/{id}/toggle-active")
    @Operation(summary = "Ativar/Desativar gerente", description = "Requer FULL_ADMIN ou ADMIN")
    fun toggleActive(@PathVariable id: UUID): ResponseEntity<ManagerResponse> {
        return ResponseEntity.ok(service.toggleActive(id))
    }

    @PatchMapping("/{id}/reset-password")
    @Operation(summary = "Resetar senha do gerente", description = "Requer FULL_ADMIN ou ADMIN")
    fun resetPassword(
        @PathVariable id: UUID,
        @RequestBody @Valid request: ResetPasswordRequest
    ): ResponseEntity<Void> {
        authService.resetPassword(id, request)
        return ResponseEntity.noContent().build()
    }
}