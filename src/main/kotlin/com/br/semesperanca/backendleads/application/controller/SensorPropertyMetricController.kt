package com.br.semesperanca.backendleads.application.controller

import com.br.semesperanca.backendleads.application.model.request.BindPropertySensorRequest
import com.br.semesperanca.backendleads.application.model.response.PropertySensorResponse
import com.br.semesperanca.backendleads.application.model.response.SensorPropertyMetricDetailResponse
import com.br.semesperanca.backendleads.application.model.response.SensorPropertyMetricResponse
import com.br.semesperanca.backendleads.domain.service.iot.SensorMetricService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/sensor-property-metrics")
@Tag(name = "Sensores", description = "Metricas dos sensores IoT vinculados aos imoveis")
class SensorPropertyMetricController(
    private val service: SensorMetricService,
) {
    @GetMapping
    @Operation(
        summary = "Listar metricas dos sensores",
        description = "Ultima leitura de cada dispositivo vinculado a um imovel. Filtro opcional por propertyId.",
    )
    fun findAll(
        @RequestParam(required = false) propertyId: UUID?,
    ): ResponseEntity<List<SensorPropertyMetricResponse>> = ResponseEntity.ok(service.findAll(propertyId))

    @GetMapping("/{propertyId}")
    @Operation(
        summary = "Detalhar metricas de um imovel",
        description = "Ultima leitura + historico horario (media/min/max) do sensor vinculado ao imovel.",
    )
    fun findByProperty(
        @PathVariable propertyId: UUID,
        @RequestParam(defaultValue = "24") hours: Int,
    ): ResponseEntity<SensorPropertyMetricDetailResponse> = ResponseEntity.ok(service.findByProperty(propertyId, hours))

    @PostMapping
    @Operation(
        summary = "Vincular sensor a imovel",
        description = "Cria ou atualiza o vinculo entre um dispositivo IoT e um imovel.",
    )
    fun bind(
        @RequestBody @Valid request: BindPropertySensorRequest,
    ): ResponseEntity<PropertySensorResponse> = ResponseEntity.status(HttpStatus.CREATED).body(service.bind(request))

    @DeleteMapping("/{propertyId}")
    @Operation(
        summary = "Desvincular sensor de imovel",
        description = "Remove o vinculo. As leituras do dispositivo permanecem no banco.",
    )
    fun unbind(
        @PathVariable propertyId: UUID,
    ): ResponseEntity<Void> {
        service.unbind(propertyId)
        return ResponseEntity.noContent().build()
    }
}
