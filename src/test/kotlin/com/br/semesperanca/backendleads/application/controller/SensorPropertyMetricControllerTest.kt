package com.br.semesperanca.backendleads.application.controller

import com.br.semesperanca.backendleads.application.model.request.BindPropertySensorRequest
import com.br.semesperanca.backendleads.application.model.response.HourlySensorMetricResponse
import com.br.semesperanca.backendleads.application.model.response.PropertySensorResponse
import com.br.semesperanca.backendleads.application.model.response.SensorPropertyMetricDetailResponse
import com.br.semesperanca.backendleads.application.model.response.SensorPropertyMetricResponse
import com.br.semesperanca.backendleads.application.model.response.SensorReadingResponse
import com.br.semesperanca.backendleads.domain.service.iot.SensorMetricService
import com.br.semesperanca.backendleads.support.AbstractControllerSlice
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper
import java.time.Instant
import java.util.UUID

@WebMvcTest(SensorPropertyMetricController::class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class SensorPropertyMetricControllerTest : AbstractControllerSlice() {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var service: SensorMetricService

    private val propertyId = UUID.randomUUID()
    private val deviceId = "3DE62F3C"

    @Test
    fun `GET deve listar metricas`() {
        val response =
            SensorPropertyMetricResponse(
                propertyId = propertyId,
                propertyTitle = "Apartamento no centro",
                deviceId = deviceId,
                temperature = 23.9,
                humidity = 72.3,
                luminosity = 4095.0,
                recordedAt = Instant.now(),
            )
        whenever(service.findAll(anyOrNull())).thenReturn(listOf(response))

        mockMvc
            .perform(get("/sensor-property-metrics"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].deviceId").value(deviceId))
            .andExpect(jsonPath("$[0].propertyId").value(propertyId.toString()))
            .andExpect(jsonPath("$[0].temperature").value(23.9))
    }

    @Test
    fun `GET deve filtrar por propertyId quando informado`() {
        whenever(service.findAll(eq(propertyId))).thenReturn(emptyList())

        mockMvc
            .perform(get("/sensor-property-metrics").param("propertyId", propertyId.toString()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isEmpty)
    }

    @Test
    fun `GET por propertyId deve retornar detalhe`() {
        val detail =
            SensorPropertyMetricDetailResponse(
                propertyId = propertyId,
                propertyTitle = "Apartamento no centro",
                deviceId = deviceId,
                latest =
                    SensorReadingResponse(
                        deviceId = deviceId,
                        temperature = 23.9,
                        humidity = 72.3,
                        luminosity = 4095.0,
                        recordedAt = Instant.now(),
                    ),
                history =
                    listOf(
                        HourlySensorMetricResponse(
                            bucketStart = Instant.now(),
                            sampleCount = 10,
                            temperatureAvg = 23.7,
                            temperatureMin = 23.0,
                            temperatureMax = 24.5,
                            humidityAvg = 72.0,
                            humidityMin = 70.0,
                            humidityMax = 75.0,
                            luminosityAvg = 4000.0,
                            luminosityMin = 3900.0,
                            luminosityMax = 4095.0,
                            partial = true,
                        ),
                    ),
            )
        whenever(service.findByProperty(eq(propertyId), any())).thenReturn(detail)

        mockMvc
            .perform(get("/sensor-property-metrics/$propertyId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.deviceId").value(deviceId))
            .andExpect(jsonPath("$.latest.temperature").value(23.9))
            .andExpect(jsonPath("$.history[0].sampleCount").value(10))
            .andExpect(jsonPath("$.history[0].partial").value(true))
    }

    @Test
    fun `GET por propertyId deve aceitar hours customizado`() {
        whenever(service.findByProperty(eq(propertyId), eq(48))).thenReturn(
            SensorPropertyMetricDetailResponse(
                propertyId = propertyId,
                propertyTitle = "Imovel",
                deviceId = deviceId,
                latest = null,
                history = emptyList(),
            ),
        )

        mockMvc
            .perform(get("/sensor-property-metrics/$propertyId").param("hours", "48"))
            .andExpect(status().isOk)
    }

    @Test
    fun `POST deve vincular sensor`() {
        val request = BindPropertySensorRequest(propertyId = propertyId, deviceId = deviceId)
        val response =
            PropertySensorResponse(
                propertyId = propertyId,
                deviceId = deviceId,
                createdAt = Instant.now(),
            )
        whenever(service.bind(any())).thenReturn(response)

        mockMvc
            .perform(
                post("/sensor-property-metrics")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.deviceId").value(deviceId))
            .andExpect(jsonPath("$.propertyId").value(propertyId.toString()))
    }

    @Test
    fun `POST deve retornar 400 quando request invalido`() {
        val request = mapOf("propertyId" to propertyId.toString())

        mockMvc
            .perform(
                post("/sensor-property-metrics")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `POST deve retornar 400 quando service lanca excecao`() {
        val request = BindPropertySensorRequest(propertyId = propertyId, deviceId = deviceId)
        whenever(service.bind(any())).thenThrow(RuntimeException("Imovel nao encontrado"))

        mockMvc
            .perform(
                post("/sensor-property-metrics")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `DELETE deve desvincular sensor`() {
        whenever(service.unbind(propertyId)).then { }

        mockMvc
            .perform(delete("/sensor-property-metrics/$propertyId"))
            .andExpect(status().isNoContent)
    }

    @Test
    fun `DELETE deve retornar 400 quando imovel nao tem sensor`() {
        whenever(service.unbind(propertyId)).thenThrow(RuntimeException("Nenhum sensor vinculado a este imovel"))

        mockMvc
            .perform(delete("/sensor-property-metrics/$propertyId"))
            .andExpect(status().isBadRequest)
    }
}
