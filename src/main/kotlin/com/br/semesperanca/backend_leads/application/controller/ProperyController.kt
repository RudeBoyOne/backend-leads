package com.br.semesperanca.backendleads.application.controller

import com.br.semesperanca.backendleads.application.model.request.CreatePropertyRequest
import com.br.semesperanca.backendleads.application.model.request.UpdatePropertyRequest
import com.br.semesperanca.backendleads.application.model.response.PropertyResponse
import com.br.semesperanca.backendleads.domain.service.PropertyService
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
import java.math.BigDecimal
import java.util.UUID

@RestController
@RequestMapping("/properties")
@Tag(name = "Imóveis", description = "Gerenciamento de imóveis")
class PropertyController(
    private val service: PropertyService,
) {
    @PostMapping
    @Operation(summary = "Criar imóvel")
    fun create(
        @RequestBody @Valid request: CreatePropertyRequest,
    ): ResponseEntity<PropertyResponse> = ResponseEntity.status(HttpStatus.CREATED).body(service.create(request))

    @GetMapping
    @Operation(
        summary = "Listar imóveis",
        description = "Filtros: realEstateAgencyId, active, status, propertyType, search, minPrice, maxPrice",
    )
    fun findAll(
        @RequestParam(required = false) realEstateAgencyId: UUID?,
        @RequestParam(required = false) active: Boolean?,
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) propertyType: String?,
        @RequestParam(required = false) search: String?,
        @RequestParam(required = false) minPrice: BigDecimal?,
        @RequestParam(required = false) maxPrice: BigDecimal?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<Page<PropertyResponse>> {
        val pageable: Pageable = PageRequest.of(page, size)
        return ResponseEntity.ok(
            service.findAll(realEstateAgencyId, active, status, propertyType, search, minPrice, maxPrice, pageable),
        )
    }

    @PatchMapping("/{id}/toggle-active")
    @Operation(summary = "Ativar/Desativar imóvel")
    fun toggleActive(
        @PathVariable id: UUID,
    ): ResponseEntity<PropertyResponse> = ResponseEntity.ok(service.toggleActive(id))

    @GetMapping("/{id}")
    @Operation(summary = "Buscar imóvel por ID")
    fun findById(
        @PathVariable id: UUID,
    ): ResponseEntity<PropertyResponse> = ResponseEntity.ok(service.findById(id))

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar imóvel")
    fun update(
        @PathVariable id: UUID,
        @RequestBody @Valid request: UpdatePropertyRequest,
    ): ResponseEntity<PropertyResponse> = ResponseEntity.ok(service.update(id, request))

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir imóvel")
    fun delete(
        @PathVariable id: UUID,
    ): ResponseEntity<Void> {
        service.delete(id)
        return ResponseEntity.noContent().build()
    }
}