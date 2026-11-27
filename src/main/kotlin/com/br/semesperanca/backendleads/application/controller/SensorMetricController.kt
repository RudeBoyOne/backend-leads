package com.br.semesperanca.backendleads.application.controller

import com.br.semesperanca.backendleads.application.model.request.CreateSensorRequest
import com.br.semesperanca.backendleads.application.model.request.UpdateSensorRequest
import com.br.semesperanca.backendleads.application.model.response.SensorMetricResponse
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

@RestController
@RequestMapping("/sensors")
@Tag(name = "Sensores", description = "Leituras dos dispositivos IoT")
class SensorMetricController(
    private val service: SensorMetricService,
) {
    @GetMapping
    @Operation(summary = "Listar leituras")
    fun findAll(): ResponseEntity<List<SensorMetricResponse>> = ResponseEntity.ok(service.findAllSensors())

    @GetMapping("/{deviceId}")
    @Operation(summary = "Buscar leitura por deviceId")
    fun findById(
        @PathVariable deviceId: String,
    ): ResponseEntity<SensorMetricResponse> = ResponseEntity.ok(service.findSensorById(deviceId))

    @PostMapping
    @Operation(summary = "Criar leitura")
    fun create(
        @RequestBody @Valid request: CreateSensorRequest,
    ): ResponseEntity<SensorMetricResponse> = ResponseEntity.status(HttpStatus.CREATED).body(service.createSensor(request))

    @PutMapping("/{deviceId}")
    @Operation(summary = "Atualizar leitura")
    fun update(
        @PathVariable deviceId: String,
        @RequestBody @Valid request: UpdateSensorRequest,
    ): ResponseEntity<SensorMetricResponse> = ResponseEntity.ok(service.updateSensor(deviceId, request))

    @DeleteMapping("/{deviceId}")
    @Operation(summary = "Excluir leitura")
    fun delete(
        @PathVariable deviceId: String,
    ): ResponseEntity<Void> {
        service.deleteSensor(deviceId)
        return ResponseEntity.noContent().build()
    }
}
