package com.br.semesperanca.backendleads.domain.service.iot

import com.br.semesperanca.backendleads.configuration.MqttProperties
import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient
import org.slf4j.LoggerFactory
import org.springframework.context.SmartLifecycle
import org.springframework.stereotype.Component
import java.util.concurrent.Executor

/**
 * Subscriber MQTT que conecta ao broker via WebSocket e assina o topic
 * de metricas dos sensores IoT.
 *
 * Implementa [SmartLifecycle] para que o start() rode depois do contexto
 * estar pronto (Flyway, DataSource, etc.) e o stop() no shutdown.
 *
 * Se [MqttProperties.enabled] for false, start() e no-op — util em testes
 * e em ambientes sem broker.
 */
@Component
class MqttSensorSubscriber(
    private val mqttProperties: MqttProperties,
    private val mqttSensorClientFactory: MqttSensorClientFactory,
    private val sensorReadingHandler: SensorReadingHandler,
    private val mqttIngestExecutor: Executor,
) : SmartLifecycle {
    private val log = LoggerFactory.getLogger(MqttSensorSubscriber::class.java)

    @Volatile
    private var running = false

    @Volatile
    private var client: Mqtt3AsyncClient? = null

    override fun isAutoStartup(): Boolean = true

    override fun getPhase(): Int = 0

    override fun isRunning(): Boolean = running

    override fun start() {
        if (!mqttProperties.enabled) {
            log.info("MQTT subscriber desabilitado (app.iot.mqtt.enabled=false)")
            return
        }

        if (running) {
            log.warn("MQTT subscriber ja esta rodando")
            return
        }

        log.info(
            "Conectando ao broker MQTT {}:{} (topic={})",
            mqttProperties.brokerHost,
            mqttProperties.brokerPort,
            mqttProperties.topic,
        )

        val mqttClient = mqttSensorClientFactory.create()
        client = mqttClient

        mqttClient
            .connect()
            .thenCompose {
                mqttClient
                    .subscribeWith()
                    .topicFilter(mqttProperties.topic)
                    .qos(MqttQos.AT_LEAST_ONCE)
                    .callback { publish ->
                        val topic = publish.topic.toString()
                        val payload = String(publish.payloadAsBytes)
                        log.info("MQTT recebido topic={} payload={}", topic, payload)
                        sensorReadingHandler.handle(topic, payload)
                    }.executor(mqttIngestExecutor)
                    .send()
            }.whenComplete { _, throwable ->
                if (throwable != null) {
                    log.error("Falha ao conectar/assinar broker MQTT: {}", throwable.message)
                } else {
                    running = true
                    log.info("MQTT subscriber ativo no topic {}", mqttProperties.topic)
                }
            }
    }

    override fun stop() {
        if (!running) return
        running = false
        client?.let {
            it.disconnect()
            log.info("MQTT subscriber desconectado")
        }
        client = null
    }
}
