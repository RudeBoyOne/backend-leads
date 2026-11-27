package com.br.semesperanca.backendleads.application.controller

import com.br.semesperanca.backendleads.application.model.request.CreateActivityRequest
import com.br.semesperanca.backendleads.application.model.request.UpdateActivityRequest
import com.br.semesperanca.backendleads.application.model.response.ActivityResponse
import com.br.semesperanca.backendleads.domain.service.ActivityService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
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
@RequestMapping("/activities")
@Tag(name = "Atividades", description = "Gerenciamento de atividades de leads")
class ActivityController(
    private val service: ActivityService,
) {
    @PostMapping
    @Operation(summary = "Criar atividade")
    fun create(
        @RequestBody @Valid request: CreateActivityRequest,
    ): ResponseEntity<ActivityResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(service.create(request))

    @GetMapping
    @Operation(summary = "Listar atividades com filtros opcionais")
    fun findAll(
        @RequestParam(required = false) leadId: UUID?,
        @RequestParam(required = false) propertyId: UUID?,
        @RequestParam(required = false) search: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<Page<ActivityResponse>> =
        ResponseEntity.ok(service.findAll(leadId, propertyId, search, PageRequest.of(page, size)))

    @GetMapping("/{id}")
    @Operation(summary = "Buscar atividade por ID")
    fun findById(
        @PathVariable id: UUID,
    ): ResponseEntity<ActivityResponse> =
        ResponseEntity.ok(service.findById(id))

    @GetMapping("/search")
    @Operation(summary = "Buscar atividades por título (parcial)")
    fun findByTitle(
        @RequestParam title: String,
    ): ResponseEntity<List<ActivityResponse>> =
        ResponseEntity.ok(service.findByTitle(title))

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar atividade")
    fun update(
        @PathVariable id: UUID,
        @RequestBody @Valid request: UpdateActivityRequest,
    ): ResponseEntity<ActivityResponse> =
        ResponseEntity.ok(service.update(id, request))

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir atividade")
    fun delete(
        @PathVariable id: UUID,
    ): ResponseEntity<Void> {
        service.delete(id)
        return ResponseEntity.noContent().build()
    }
}
