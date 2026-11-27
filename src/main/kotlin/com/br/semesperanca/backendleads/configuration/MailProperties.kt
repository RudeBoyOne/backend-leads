package com.br.semesperanca.backendleads.configuration

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.mail")
class MailProperties {
    var from: String? = null
    var notifyTo: String? = null
}