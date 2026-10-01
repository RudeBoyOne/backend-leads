package com.br.semesperanca.backendleads.configuration

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.iot.mqtt")
class MqttProperties {
    var enabled: Boolean = true
    var brokerHost: String = "localhost"
    var brokerPort: Int = 9001
    var serverPath: String = "/mqtt"
    var topic: String = "100esperanca/metrics/#"
    var clientId: String = "backend-leads"
    var username: String? = null
    var password: String? = null
    var qos: Int = 1

    /**
     * Prefixo do topic, derivado de [topic] removendo o wildcard final.
     * Usado pelo [com.br.semesperanca.backendleads.domain.service.iot.SensorReadingHandler]
     * para validar se a mensagem veio do topic esperado.
     */
    val topicPrefix: String
        get() = topic.removeSuffix("#").removeSuffix("/")
}
