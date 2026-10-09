package com.br.semesperanca.backendleads.domain.service.iot

import com.br.semesperanca.backendleads.configuration.MqttProperties
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient.Mqtt3SubscribeAndCallbackBuilder
import com.hivemq.client.mqtt.mqtt3.message.subscribe.suback.Mqtt3SubAck
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

class MqttSensorSubscriberTest {
    private val props =
        MqttProperties().apply {
            enabled = true
            brokerHost = "localhost"
            brokerPort = 9001
            serverPath = "/mqtt"
            topic = "100esperanca/metrics/#"
            clientId = "test-client"
            qos = 1
        }

    private val factory = org.mockito.kotlin.mock<MqttSensorClientFactory>()
    private val handler = org.mockito.kotlin.mock<SensorReadingHandler>()
    private val executor = Executor { it.run() }

    /**
     * O `start()` sobe `running` no `whenComplete` da cadeia
     * connect -> subscribeWith -> send. Sem-stubar essa cadeia, o `thenCompose`
     * estoura e `running` continua falso — dai o "idempotente" criar dois
     * clientes e o `stop` não desconectar nada.
     */
    private fun mockClient(): Mqtt3AsyncClient {
        val client = mock<Mqtt3AsyncClient>()
        val start = mock<Mqtt3SubscribeAndCallbackBuilder.Start>()
        val startComplete = mock<Mqtt3SubscribeAndCallbackBuilder.Start.Complete>()
        val callEx = mock<Mqtt3SubscribeAndCallbackBuilder.Call.Ex>()

        whenever(client.subscribeWith()).thenReturn(start)
        whenever(start.topicFilter(any<String>())).thenReturn(startComplete)
        whenever(startComplete.qos(any())).thenReturn(startComplete)
        whenever(startComplete.callback(any())).thenReturn(callEx)
        whenever(callEx.executor(any())).thenReturn(callEx)
        whenever(callEx.send()).thenReturn(CompletableFuture.completedFuture(mock<Mqtt3SubAck>()))
        whenever(client.connect())
            .thenReturn(CompletableFuture.completedFuture(mock<com.hivemq.client.mqtt.mqtt3.message.connect.connack.Mqtt3ConnAck>()))

        return client
    }

    @Test
    fun `start deve ser no-op quando desabilitado`() {
        props.enabled = false
        val subscriber = MqttSensorSubscriber(props, factory, handler, executor)

        subscriber.start()

        verify(factory, never()).create()
        assertThat(subscriber.isRunning).isFalse()
    }

    @Test
    fun `start deve criar cliente quando habilitado`() {
        val client = mockClient()
        whenever(factory.create()).thenReturn(client)

        val subscriber = MqttSensorSubscriber(props, factory, handler, executor)
        subscriber.start()

        verify(factory).create()
        verify(client).connect()
    }

    @Test
    fun `start deve ser idempotente`() {
        val client = mockClient()
        whenever(factory.create()).thenReturn(client)

        val subscriber = MqttSensorSubscriber(props, factory, handler, executor)
        subscriber.start()
        subscriber.start()

        verify(factory, org.mockito.kotlin.times(1)).create()
    }

    @Test
    fun `stop deve ser no-op quando nao running`() {
        val subscriber = MqttSensorSubscriber(props, factory, handler, executor)

        subscriber.stop()

        verify(factory, never()).create()
    }

    @Test
    fun `stop deve desconectar quando running`() {
        val client = mockClient()
        val disconnectFuture = CompletableFuture.completedFuture(mock<Void>())

        whenever(factory.create()).thenReturn(client)
        whenever(client.disconnect()).thenReturn(disconnectFuture)

        val subscriber = MqttSensorSubscriber(props, factory, handler, executor)
        subscriber.start()
        subscriber.stop()

        verify(client).disconnect()
        assertThat(subscriber.isRunning).isFalse()
    }

    @Test
    fun `deve reportar isAutoStartup como true`() {
        val subscriber = MqttSensorSubscriber(props, factory, handler, executor)

        assertThat(subscriber.isAutoStartup).isTrue()
    }

    @Test
    fun `deve reportar phase como 0`() {
        val subscriber = MqttSensorSubscriber(props, factory, handler, executor)

        assertThat(subscriber.phase).isEqualTo(0)
    }
}
