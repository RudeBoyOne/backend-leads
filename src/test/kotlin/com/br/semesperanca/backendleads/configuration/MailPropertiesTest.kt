package com.br.semesperanca.backendleads.configuration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource

class MailPropertiesTest {
    private fun bind(vararg entries: Pair<String, Any>) =
        Binder(MapConfigurationPropertySource(entries.toMap()))
            .bind("app.mail", MailProperties::class.java)
            .orElseGet(::MailProperties)

    @Test
    fun `liga as propriedades app_mail_from e app_mail_notify-to`() {
        val properties = bind("app.mail.from" to "nao-responda@imobiliaria.com.br", "app.mail.notify-to" to "corretores@imobiliaria.com.br")

        assertThat(properties.from).isEqualTo("nao-responda@imobiliaria.com.br")
        assertThat(properties.notifyTo).isEqualTo("corretores@imobiliaria.com.br")
    }

    @Test
    fun `propriedades ausentes ficam nulas, nao lancam erro`() {
        val properties = bind()

        assertThat(properties.from).isNull()
        assertThat(properties.notifyTo).isNull()
    }

    @Test
    fun `propriedades sao mutaveis para accommodate reload de configuracao`() {
        val properties = bind("app.mail.from" to "a@b.com")

        properties.from = "c@d.com"
        properties.notifyTo = "e@f.com"

        assertThat(properties.from).isEqualTo("c@d.com")
        assertThat(properties.notifyTo).isEqualTo("e@f.com")
    }
}
