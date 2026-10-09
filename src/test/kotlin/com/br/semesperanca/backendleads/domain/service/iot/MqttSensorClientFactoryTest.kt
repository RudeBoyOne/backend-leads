package com.br.semesperanca.backendleads.domain.service.iot

import com.br.semesperanca.backendleads.configuration.MqttProperties
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MqttSensorClientFactoryTest {
    @Test
    fun `deve criar cliente sem conectar`() {
        val props =
            MqttProperties().apply {
                enabled = true
                brokerHost = "localhost"
                brokerPort = 9001
                serverPath = "/mqtt"
                clientId = "test-client"
            }
        val factory = MqttSensorClientFactory(props)

        val client = factory.create()

        assertThat(client).isNotNull
        assertThat(client).isInstanceOf(Mqtt3AsyncClient::class.java)
    }

    @Test
    fun `deve criar cliente com credenciais quando configuradas`() {
        val props =
            MqttProperties().apply {
                enabled = true
                brokerHost = "broker.exemplo.com"
                brokerPort = 9001
                serverPath = "/mqtt"
                clientId = "test-client"
                username = "user"
                password = "pass"
            }
        val factory = MqttSensorClientFactory(props)

        val client = factory.create()

        assertThat(client).isNotNull
    }

    @Test
    fun `deve criar cliente sem credenciais quando nao configuradas`() {
        val props =
            MqttProperties().apply {
                enabled = true
                brokerHost = "localhost"
                brokerPort = 9001
                serverPath = "/mqtt"
                clientId = "test-client"
                username = null
                password = null
            }
        val factory = MqttSensorClientFactory(props)

        val client = factory.create()

        assertThat(client).isNotNull
    }

    @Test
    fun `deve criar cliente com username vazio sem credenciais`() {
        val props =
            MqttProperties().apply {
                enabled = true
                brokerHost = "localhost"
                brokerPort = 9001
                serverPath = "/mqtt"
                clientId = "test-client"
                username = ""
                password = ""
            }
        val factory = MqttSensorClientFactory(props)

        val client = factory.create()

        assertThat(client).isNotNull
    }
}
