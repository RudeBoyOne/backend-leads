package com.br.semesperanca.backendleads.configuration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.context.properties.bind.Bindable
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.boot.context.properties.source.ConfigurationPropertySources
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.StandardEnvironment
import java.util.Collections

class MqttPropertiesTest {
    @Test
    fun `deve aplicar defaults quando nao ha configuracao`() {
        val props = bind(emptyMap())

        assertThat(props.enabled).isTrue()
        assertThat(props.brokerHost).isEqualTo("localhost")
        assertThat(props.brokerPort).isEqualTo(9001)
        assertThat(props.serverPath).isEqualTo("/mqtt")
        assertThat(props.topic).isEqualTo("100esperanca/metrics/#")
        assertThat(props.clientId).isEqualTo("backend-leads")
        assertThat(props.username).isNull()
        assertThat(props.password).isNull()
        assertThat(props.qos).isEqualTo(1)
    }

    @Test
    fun `deve sobrescrever defaults com configuracao`() {
        val source =
            mapOf(
                "app.iot.mqtt.enabled" to "false",
                "app.iot.mqtt.broker-host" to "broker.exemplo.com",
                "app.iot.mqtt.broker-port" to "8080",
                "app.iot.mqtt.server-path" to "/custom",
                "app.iot.mqtt.topic" to "outro/topic/#",
                "app.iot.mqtt.client-id" to "meu-client",
                "app.iot.mqtt.username" to "user",
                "app.iot.mqtt.password" to "pass",
                "app.iot.mqtt.qos" to "2",
            )
        val props = bind(source)

        assertThat(props.enabled).isFalse()
        assertThat(props.brokerHost).isEqualTo("broker.exemplo.com")
        assertThat(props.brokerPort).isEqualTo(8080)
        assertThat(props.serverPath).isEqualTo("/custom")
        assertThat(props.topic).isEqualTo("outro/topic/#")
        assertThat(props.clientId).isEqualTo("meu-client")
        assertThat(props.username).isEqualTo("user")
        assertThat(props.password).isEqualTo("pass")
        assertThat(props.qos).isEqualTo(2)
    }

    private fun bind(source: Map<String, String>): MqttProperties {
        val environment = StandardEnvironment()
        environment.propertySources.addFirst(MapPropertySource("test", source as Map<String, Any>))
        val binder = Binder(ConfigurationPropertySources.get(environment))
        return binder
            .bind("app.iot.mqtt", Bindable.of(MqttProperties::class.java))
            .orElseGet { MqttProperties() }
    }
}
