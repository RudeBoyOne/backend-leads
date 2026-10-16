package com.br.semesperanca.backendleads.configuration

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(MailProperties::class, MqttProperties::class)
class MailConfiguration
