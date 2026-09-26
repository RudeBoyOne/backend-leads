package com.br.semesperanca.backendleads.application.controller

import com.br.semesperanca.backendleads.application.model.request.CreateBranchOwnerRequest
import com.br.semesperanca.backendleads.application.model.request.UpdateBranchOwnerRequest
import com.br.semesperanca.backendleads.application.model.response.BranchOwnerResponse
import com.br.semesperanca.backendleads.domain.service.BranchOwnerService
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
@RequestMapping("/branch-owners")
@Tag(name = "Responsáveis de filial", description = "Gerenciamento de responsáveis de filial")
class BranchOwnerController(
    private val service: BranchOwnerService
) {
    @PostMapping
    @Operation(summary = "Criar responsável de filial")
    fun create(@RequestBody @Valid request: CreateBranchOwnerRequest): ResponseEntity<BranchOwnerResponse> {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request))
    }

    @GetMapping
    @Operation(
        summary = "Listar responsáveis de filial",
        description = "Filtros: realEstateAgencyId, active e search (name/email)"
    )
    fun findAll(
        @RequestParam(required = false) realEstateAgencyId: UUID?,
        @RequestParam(required = false) active: Boolean?,
        @RequestParam(required = false) search: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<Page<BranchOwnerResponse>> {
        val pageable: Pageable = PageRequest.of(page, size)
        return ResponseEntity.ok(service.findAll(realEstateAgencyId, active, search, pageable))
    }

    @PatchMapping("/{id}/toggle-active")
    @Operation(summary = "Ativar/Desativar responsável de filial")
    fun toggleActive(@PathVariable id: UUID): ResponseEntity<BranchOwnerResponse> {
        return ResponseEntity.ok(service.toggleActive(id))
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar responsável de filial por ID")
    fun findById(@PathVariable id: UUID): ResponseEntity<BranchOwnerResponse> {
        return ResponseEntity.ok(service.findById(id))
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar responsável de filial")
    fun update(
        @PathVariable id: UUID,
        @RequestBody @Valid request: UpdateBranchOwnerRequest
    ): ResponseEntity<BranchOwnerResponse> {
        return ResponseEntity.ok(service.update(id, request))
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir responsável de filial")
    fun delete(@PathVariable id: UUID): ResponseEntity<Void> {
        service.delete(id)
        return ResponseEntity.noContent().build()
    }
}