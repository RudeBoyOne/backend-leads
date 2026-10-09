package com.br.semesperanca.backendleads.domain.service.iot

import com.br.semesperanca.backendleads.configuration.MqttProperties
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client
import com.hivemq.client.mqtt.mqtt3.message.auth.Mqtt3SimpleAuth
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Factory do cliente MQTT async com transporte WebSocket.
 *
 * Seam de teste: em producao monta o client HiveMQ real; nos testes e
 * substituida por um mock que retorna um [Mqtt3AsyncClient] mockado.
 */
@Component
class MqttSensorClientFactory(
    private val mqttProperties: MqttProperties,
) {
    private val log = LoggerFactory.getLogger(MqttSensorClientFactory::class.java)

    fun create(): Mqtt3AsyncClient {
        val username = mqttProperties.username
        val password = mqttProperties.password

        val builder =
            Mqtt3Client
                .builder()
                .identifier(mqttProperties.clientId)
                .serverHost(mqttProperties.brokerHost)
                .serverPort(mqttProperties.brokerPort)
                .webSocketConfig()
                .serverPath(mqttProperties.serverPath)
                .applyWebSocketConfig()
                .automaticReconnectWithDefaultConfig()
                .addConnectedListener {
                    log.info(
                        "Conectado ao broker MQTT {}:{} (topic={})",
                        mqttProperties.brokerHost,
                        mqttProperties.brokerPort,
                        mqttProperties.topic,
                    )
                }.addDisconnectedListener {
                    log.warn("Desconectado do broker MQTT: {}", it.cause.message)
                    it.reconnector.resubscribeIfSessionPresent(true)
                }

        if (!username.isNullOrBlank()) {
            builder.simpleAuth(
                Mqtt3SimpleAuth
                    .builder()
                    .username(username)
                    .password(password?.toByteArray() ?: byteArrayOf())
                    .build(),
            )
        }

        return builder.buildAsync()
    }
}
