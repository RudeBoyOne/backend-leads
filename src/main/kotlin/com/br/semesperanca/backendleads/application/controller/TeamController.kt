package com.br.semesperanca.backendleads.application.controller

import com.br.semesperanca.backendleads.application.model.request.CreateTeamRequest
import com.br.semesperanca.backendleads.application.model.request.UpdateTeamRequest
import com.br.semesperanca.backendleads.application.model.response.TeamResponse
import com.br.semesperanca.backendleads.domain.service.TeamService
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
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/teams")
@Tag(name = "Times", description = "Gerenciamento de times de corretores")
class TeamController(
    private val service: TeamService,
) {
    @GetMapping
    @Operation(summary = "Listar times")
    fun findAll(): ResponseEntity<List<TeamResponse>> = ResponseEntity.ok(service.findAll())

    @PostMapping
    @Operation(summary = "Criar time")
    fun create(
        @RequestBody @Valid request: CreateTeamRequest,
    ): ResponseEntity<TeamResponse> = ResponseEntity.status(HttpStatus.CREATED).body(service.create(request))

    @GetMapping("/{id}")
    @Operation(summary = "Buscar time por ID")
    fun findById(
        @PathVariable id: UUID,
    ): ResponseEntity<TeamResponse> = ResponseEntity.ok(service.findById(id))

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar time")
    fun update(
        @PathVariable id: UUID,
        @RequestBody @Valid request: UpdateTeamRequest,
    ): ResponseEntity<TeamResponse> = ResponseEntity.ok(service.update(id, request))

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir time")
    fun delete(
        @PathVariable id: UUID,
    ): ResponseEntity<Void> {
        service.delete(id)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/branch-office/{branchOfficeId}")
    @Operation(summary = "Listar times por filial")
    fun findByBranchOfficeId(
        @PathVariable branchOfficeId: UUID,
    ): ResponseEntity<List<TeamResponse>> = ResponseEntity.ok(service.findByBranchOfficeId(branchOfficeId))

    @PostMapping("/{teamId}/agents/{agentId}")
    @Operation(summary = "Adicionar corretor ao time")
    fun addAgent(
        @PathVariable teamId: UUID,
        @PathVariable agentId: UUID,
    ): ResponseEntity<Void> {
        service.addAgent(teamId, agentId)
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/{teamId}/agents/{agentId}")
    @Operation(summary = "Remover corretor do time")
    fun removeAgent(
        @PathVariable teamId: UUID,
        @PathVariable agentId: UUID,
    ): ResponseEntity<Void> {
        service.removeAgent(teamId, agentId)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/{teamId}/agents")
    @Operation(summary = "Listar corretores do time")
    fun findAgentsByTeamId(
        @PathVariable teamId: UUID,
    ): ResponseEntity<List<UUID>> = ResponseEntity.ok(service.findAgentsByTeamId(teamId))
}
