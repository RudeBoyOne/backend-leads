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

    val topicPrefix: String
        get() = topic.removeSuffix("#").removeSuffix("/")
}
