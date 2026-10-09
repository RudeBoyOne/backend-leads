package com.br.semesperanca.backendleads.domain.service.iot

import com.br.semesperanca.backendleads.application.model.iot.SensorReadingPayload
import com.br.semesperanca.backendleads.configuration.MqttProperties
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import tools.jackson.module.kotlin.jacksonObjectMapper

class SensorReadingHandlerTest {
    private val objectMapper = jacksonObjectMapper()
    private val sensorMetricHandler = org.mockito.kotlin.mock<SensorMetricHandler>()
    private val mqttProperties = MqttProperties()
    private val handler = SensorReadingHandler(objectMapper, sensorMetricHandler, mqttProperties)

    @Test
    fun `deve processar mensagem valida`() {
        val topic = "100esperanca/metrics/3DE62F3C"
        val payload = """{"Temp":23.9,"Umid":72.3,"Lum":4095.00}"""

        handler.handle(topic, payload)

        verify(sensorMetricHandler).record(
            eq("3DE62F3C"),
            eq(SensorReadingPayload(temperature = 23.9, humidity = 72.3, luminosity = 4095.00)),
        )
    }

    @Test
    fun `deve normalizar deviceId para maiusculas`() {
        val topic = "100esperanca/metrics/3de62f3c"
        val payload = """{"Temp":21.2,"Umid":75.3,"Lum":4057.00}"""

        handler.handle(topic, payload)

        verify(sensorMetricHandler).record(eq("3DE62F3C"), any())
    }

    @Test
    fun `deve ignorar topic fora do prefixo`() {
        val topic = "outro/topic/3DE62F3C"
        val payload = """{"Temp":23.9,"Umid":72.3,"Lum":4095.00}"""

        handler.handle(topic, payload)

        verify(sensorMetricHandler, never()).record(any(), any())
    }

    @Test
    fun `deve ignorar topic sem segmento de device`() {
        val topic = "100esperanca/metrics/"
        val payload = """{"Temp":23.9,"Umid":72.3,"Lum":4095.00}"""

        handler.handle(topic, payload)

        verify(sensorMetricHandler, never()).record(any(), any())
    }

    @Test
    fun `deve ignorar payload invalido`() {
        val topic = "100esperanca/metrics/3DE62F3C"
        val payload = "nao e json"

        handler.handle(topic, payload)

        verify(sensorMetricHandler, never()).record(any(), any())
    }

    @Test
    fun `deve ignorar payload com campo ausente`() {
        val topic = "100esperanca/metrics/3DE62F3C"
        val payload = """{"Temp":23.9,"Umid":72.3}"""

        handler.handle(topic, payload)

        verify(sensorMetricHandler, never()).record(any(), any())
    }

    @Test
    fun `deve ignorar payload com valor nao numerico`() {
        val topic = "100esperanca/metrics/3DE62F3C"
        val payload = """{"Temp":"quente","Umid":72.3,"Lum":4095.00}"""

        handler.handle(topic, payload)

        verify(sensorMetricHandler, never()).record(any(), any())
    }

    @Test
    fun `deve ignorar payload com NaN`() {
        val topic = "100esperanca/metrics/3DE62F3C"
        val payload = """{"Temp":NaN,"Umid":72.3,"Lum":4095.00}"""

        handler.handle(topic, payload)

        verify(sensorMetricHandler, never()).record(any(), any())
    }

    @Test
    fun `deve ignorar payload com infinito`() {
        val topic = "100esperanca/metrics/3DE62F3C"
        val payload = """{"Temp":Infinity,"Umid":72.3,"Lum":4095.00}"""

        handler.handle(topic, payload)

        verify(sensorMetricHandler, never()).record(any(), any())
    }

    @Test
    fun `deve aceitar payload com campos extras`() {
        val topic = "100esperanca/metrics/3DE62F3C"
        val payload = """{"Temp":23.9,"Umid":72.3,"Lum":4095.00,"extra":"valor"}"""

        handler.handle(topic, payload)

        verify(sensorMetricHandler).record(
            eq("3DE62F3C"),
            eq(SensorReadingPayload(temperature = 23.9, humidity = 72.3, luminosity = 4095.00)),
        )
    }
}
