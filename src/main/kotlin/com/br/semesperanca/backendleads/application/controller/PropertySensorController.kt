package com.br.semesperanca.backendleads.application.controller

import com.br.semesperanca.backendleads.application.model.request.BindPropertySensorRequest
import com.br.semesperanca.backendleads.application.model.request.UpdatePropertySensorRequest
import com.br.semesperanca.backendleads.application.model.response.PropertySensorResponse
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
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/property-sensors")
@Tag(name = "Vinculação de sensores", description = "Vínculo entre dispositivos IoT e imóveis")
class PropertySensorController(
    private val service: SensorMetricService,
) {
    @GetMapping
    @Operation(summary = "Listar vínculos")
    fun findAll(): ResponseEntity<List<PropertySensorResponse>> = ResponseEntity.ok(service.findAllPropertySensors())

    @GetMapping("/{id}")
    @Operation(summary = "Buscar vínculo por ID")
    fun findById(
        @PathVariable id: UUID,
    ): ResponseEntity<PropertySensorResponse> = ResponseEntity.ok(service.findPropertySensorById(id))

    @PostMapping
    @Operation(summary = "Vincular sensor a imóvel")
    fun create(
        @RequestBody @Valid request: BindPropertySensorRequest,
    ): ResponseEntity<PropertySensorResponse> = ResponseEntity.status(HttpStatus.CREATED).body(service.bind(request))

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar vínculo")
    fun update(
        @PathVariable id: UUID,
        @RequestBody @Valid request: UpdatePropertySensorRequest,
    ): ResponseEntity<PropertySensorResponse> = ResponseEntity.ok(service.updatePropertySensor(id, request))

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir vínculo")
    fun delete(
        @PathVariable id: UUID,
    ): ResponseEntity<Void> {
        service.deletePropertySensor(id)
        return ResponseEntity.noContent().build()
    }
}
