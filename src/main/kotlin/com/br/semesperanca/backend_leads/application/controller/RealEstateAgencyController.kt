package com.br.semesperanca.backendleads.application.controller

import com.br.semesperanca.backendleads.application.model.request.CreateRealEstateAgencyRequest
import com.br.semesperanca.backendleads.application.model.request.UpdateRealEstateAgencyRequest
import com.br.semesperanca.backendleads.application.model.response.RealEstateAgencyResponse
import com.br.semesperanca.backendleads.domain.service.RealEstateAgencyService
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
@RequestMapping("/real-estate-agencies")
@Tag(name = "Agências Imobiliárias", description = "Gerenciamento de agências imobiliárias")
class RealEstateAgencyController(
    private val service: RealEstateAgencyService,
) {
    @GetMapping
    @Operation(summary = "Listar agências")
    fun findAll(): ResponseEntity<List<RealEstateAgencyResponse>> = ResponseEntity.ok(service.findAll())

    @GetMapping("/search")
    @Operation(summary = "Buscar agências por nome (busca parcial)")
    fun searchByName(
        @RequestParam name: String,
    ): ResponseEntity<List<RealEstateAgencyResponse>> = ResponseEntity.ok(service.searchByName(name))

    @PostMapping
    @Operation(summary = "Criar agência")
    fun create(
        @RequestBody @Valid request: CreateRealEstateAgencyRequest,
    ): ResponseEntity<RealEstateAgencyResponse> = ResponseEntity.status(HttpStatus.CREATED).body(service.create(request))

    @GetMapping("/{id}")
    @Operation(summary = "Buscar agência por ID")
    fun findById(
        @PathVariable id: UUID,
    ): ResponseEntity<RealEstateAgencyResponse> = ResponseEntity.ok(service.findById(id))

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar agência")
    fun update(
        @PathVariable id: UUID,
        @RequestBody @Valid request: UpdateRealEstateAgencyRequest,
    ): ResponseEntity<RealEstateAgencyResponse> = ResponseEntity.ok(service.update(id, request))

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir agência")
    fun delete(
        @PathVariable id: UUID,
    ): ResponseEntity<Void> {
        service.delete(id)
        return ResponseEntity.noContent().build()
    }
}
