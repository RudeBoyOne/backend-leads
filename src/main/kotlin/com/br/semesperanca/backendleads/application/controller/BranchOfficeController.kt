package com.br.semesperanca.backendleads.application.controller

import com.br.semesperanca.backendleads.application.model.request.CreateBranchOfficeRequest
import com.br.semesperanca.backendleads.application.model.request.UpdateBranchOfficeRequest
import com.br.semesperanca.backendleads.application.model.response.BranchOfficeResponse
import com.br.semesperanca.backendleads.domain.service.BranchOfficeService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
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
@RequestMapping("/branch-offices")
@Tag(name = "Filiais", description = "Gerenciamento de filiais")
class BranchOfficeController(
    private val service: BranchOfficeService,
) {
    @GetMapping
    @Operation(summary = "Listar filiais")
    fun findAll(): ResponseEntity<List<BranchOfficeResponse>> = ResponseEntity.ok(service.findAll())

    @GetMapping("/search")
    @Operation(summary = "Buscar filiais por nome (busca parcial)")
    fun searchByName(
        @RequestParam name: String,
    ): ResponseEntity<List<BranchOfficeResponse>> = ResponseEntity.ok(service.searchByName(name))

    @PostMapping
    @Operation(summary = "Criar filial")
    fun create(
        @RequestBody @Valid request: CreateBranchOfficeRequest,
    ): ResponseEntity<BranchOfficeResponse> = ResponseEntity.status(HttpStatus.CREATED).body(service.create(request))

    @GetMapping("/{id}")
    @Operation(summary = "Buscar filial por ID")
    fun findById(
        @PathVariable id: UUID,
    ): ResponseEntity<BranchOfficeResponse> = ResponseEntity.ok(service.findById(id))

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar filial")
    fun update(
        @PathVariable id: UUID,
        @RequestBody @Valid request: UpdateBranchOfficeRequest,
    ): ResponseEntity<BranchOfficeResponse> = ResponseEntity.ok(service.update(id, request))

    @PatchMapping("/{id}/toggle-active")
    @Operation(summary = "Ativar/Desativar filial")
    fun toggleActive(
        @PathVariable id: UUID,
    ): ResponseEntity<BranchOfficeResponse> = ResponseEntity.ok(service.toggleActive(id))

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir filial")
    fun delete(
        @PathVariable id: UUID,
    ): ResponseEntity<Void> {
        service.delete(id)
        return ResponseEntity.noContent().build()
    }
}
