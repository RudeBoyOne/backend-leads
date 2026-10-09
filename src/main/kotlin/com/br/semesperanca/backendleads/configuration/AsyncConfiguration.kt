package com.br.semesperanca.backendleads.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

@Configuration
@EnableAsync
class AsyncConfiguration {
    @Bean
    fun taskExecutor(): ThreadPoolTaskExecutor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 2
        executor.maxPoolSize = 4
        executor.queueCapacity = 50
        executor.setThreadNamePrefix("insight-")
        executor.initialize()
        return executor
    }

    /**
     * Executor dedicado para o callback MQTT. O DB nao pode rodar na thread
     * de I/O do cliente HiveMQ, entao o subscribe usa este executor.
     */
    @Bean
    fun mqttIngestExecutor(): ThreadPoolTaskExecutor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 1
        executor.maxPoolSize = 2
        executor.queueCapacity = 500
        executor.setThreadNamePrefix("mqtt-")
        executor.initialize()
        return executor
    }
}
