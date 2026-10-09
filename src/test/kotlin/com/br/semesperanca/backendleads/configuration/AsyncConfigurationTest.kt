package com.br.semesperanca.backendleads.configuration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class AsyncConfigurationTest {
    @Test
    fun `o executor ja nasce inicializado, pronto para receber tarefas`() {
        val executor = AsyncConfiguration().taskExecutor()

        assertThat(executor).isNotNull()
        assertThat(executor.corePoolSize).isEqualTo(2)
        assertThat(executor.maxPoolSize).isEqualTo(4)
        assertThat(executor.threadPoolExecutor.queue.remainingCapacity()).isEqualTo(50)
    }

    @Test
    fun `as threads do executor usam o prefixo insight-`() {
        val executor = AsyncConfiguration().taskExecutor()

        val name = executor.threadPoolExecutor.submit<String> { Thread.currentThread().name }.get(5, TimeUnit.SECONDS)

        assertThat(name).startsWith("insight-")
    }

    @Test
    fun `tarefas acima do corePoolSize esperam na fila em vez de criar thread nova`() {
        val executor = AsyncConfiguration().taskExecutor()
        val running = CountDownLatch(2)
        val release = CountDownLatch(1)
        val inFlight = AtomicInteger()
        val peak = AtomicInteger()

        val futures =
            (1..4).map {
                executor.submit {
                    running.countDown()
                    release.await(5, TimeUnit.SECONDS)
                    peak.accumulateAndGet(inFlight.incrementAndGet(), ::maxOf)
                    inFlight.decrementAndGet()
                }
            }

        // ThreadPoolExecutor so cria thread acima do corePoolSize quando a
        // fila esta cheia. Aqui a fila comporta 50, entao as 2 tarefas extras
        // param nela e a concorrencia real fica em 2, nunca 4.
        assertThat(running.await(5, TimeUnit.SECONDS)).isTrue()
        assertThat(executor.activeCount).isEqualTo(2)
        assertThat(executor.threadPoolExecutor.queue.size).isEqualTo(2)

        release.countDown()
        futures.forEach { it.get(5, TimeUnit.SECONDS) }
        assertThat(peak.get()).isEqualTo(2)
    }

    @Test
    fun `o executor aceita um volume de tarefas bem maior que a fila`() {
        val executor = AsyncConfiguration().taskExecutor()

        repeat(500) { i -> executor.submit<Int> { i }.get(5, TimeUnit.SECONDS) }

        assertThat(executor.threadPoolExecutor.completedTaskCount).isEqualTo(500)
    }

    @Test
    fun `cada chamada cria um executor distinto`() {
        val config = AsyncConfiguration()

        assertThat(config.taskExecutor()).isNotSameAs(config.taskExecutor())
    }
}
