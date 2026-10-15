package com.br.semesperanca.backendleads.domain.service.iot

import com.br.semesperanca.backendleads.application.model.iot.SensorReadingPayload
import com.br.semesperanca.backendleads.configuration.MqttProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

/**
 * Processa mensagens MQTT recebidas do broker.
 *
 * Responsabilidade unica: extrair o deviceId do topic, deserializar e validar
 * o payload, e delegar para o [SensorMetricHandler]. Nenhuma excecao escapa
 * daqui — uma mensagem ruim nao pode derrubar o contexto nem a conexao MQTT.
 */
@Component
class SensorReadingHandler(
    private val objectMapper: ObjectMapper,
    private val sensorMetricHandler: SensorMetricHandler,
    private val mqttProperties: MqttProperties,
) {
    private val log = LoggerFactory.getLogger(SensorReadingHandler::class.java)

    fun handle(
        topic: String,
        payload: String,
    ) {
        val deviceId = extractDeviceId(topic)
        if (deviceId == null) {
            log.warn("Topic fora do prefixo esperado, ignorando: {}", topic)
            return
        }

        val reading = parsePayload(payload)
        if (reading == null) {
            log.warn("Payload invalido do dispositivo {}: {}", deviceId, payload)
            return
        }

        sensorMetricHandler.record(deviceId, reading)
    }
    private fun extractDeviceId(topic: String): String? {
        val prefix = mqttProperties.topicPrefix
        if (!topic.startsWith(prefix)) return null

        val segments = topic.removePrefix(prefix).split("/")
        if (segments.size < 2) return null

        val last = segments.last().trim().uppercase()
        return if (last.isNotEmpty()) last else null
    }

    private fun parsePayload(payload: String): SensorReadingPayload? =
        try {
            val reading = objectMapper.readValue(payload, SensorReadingPayload::class.java)
            if (reading.temperature == null || reading.humidity == null || reading.luminosity == null) {
                null
            } else if (!reading.temperature.isFinite() || !reading.humidity.isFinite() || !reading.luminosity.isFinite()) {
                null
            } else {
                reading
            }
        } catch (e: Exception) {
            log.warn("Falha ao deserializar payload: {}", e.message)
            null
        }
}
