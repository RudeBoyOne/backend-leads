package com.br.semesperanca.backendleads.application.controller

import com.br.semesperanca.backendleads.application.model.request.CreateRoleRequest
import com.br.semesperanca.backendleads.application.model.request.UpdateRoleRequest
import com.br.semesperanca.backendleads.application.model.response.RoleResponse
import com.br.semesperanca.backendleads.domain.service.RoleService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/roles")
@Tag(name = "Roles", description = "Gerenciamento de papéis")
class RoleController(
    private val service: RoleService,
) {
    @GetMapping
    @Operation(summary = "Listar roles")
    fun findAll(): ResponseEntity<List<RoleResponse>> = ResponseEntity.ok(service.findAll())

    @GetMapping("/{id}")
    @Operation(summary = "Buscar role por ID")
    fun findById(
        @PathVariable id: UUID,
    ): ResponseEntity<RoleResponse> = ResponseEntity.ok(service.findById(id))

    @PostMapping
    @Operation(summary = "Criar role")
    fun create(
        @RequestBody @Valid request: CreateRoleRequest,
    ): ResponseEntity<RoleResponse> = ResponseEntity.status(HttpStatus.CREATED).body(service.create(request))

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar role")
    fun update(
        @PathVariable id: UUID,
        @RequestBody @Valid request: UpdateRoleRequest,
    ): ResponseEntity<RoleResponse> = ResponseEntity.ok(service.update(id, request))

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir role")
    fun delete(
        @PathVariable id: UUID,
    ): ResponseEntity<Void> {
        service.delete(id)
        return ResponseEntity.noContent().build()
    }
}
