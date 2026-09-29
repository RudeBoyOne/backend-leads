package com.br.semesperanca.backendleads.domain.service

import com.br.semesperanca.backendleads.application.model.LeadReceivedNotificationDTO
import com.br.semesperanca.backendleads.configuration.MailProperties
import jakarta.mail.MessagingException
import jakarta.mail.Part
import jakarta.mail.Session
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeMultipart
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.mail.javamail.JavaMailSender
import java.time.OffsetDateTime
import java.util.Properties

@ExtendWith(MockitoExtension::class)
class EmailServiceTest {
    @Mock private lateinit var mailSender: JavaMailSender

    private val mailProperties = MailProperties().apply { from = "nao-responda@imobiliaria.com.br" }
    private lateinit var service: EmailService

    private val lead = LeadReceivedNotificationDTO("db-id-1", "origin-4711", "Zapimoveis")
    private val destinatario = "corretores@imobiliaria.com.br"

    @BeforeEach
    fun setUp() {
        service = EmailService(mailSender, mailProperties)
    }

    /**
     * MimeMessage real sobre uma Session sem servidor: assim da para inspecionar
     * destinatario, assunto e corpo sem depender de um SMTP de verdade.
     */
    private fun realMimeMessage(): MimeMessage =
        MimeMessage(
            Session.getInstance(
                Properties().apply {
                    setProperty("mail.smtp.host", "localhost")
                    setProperty("mail.mime.charset", "UTF-8")
                },
            ),
        )

    private fun captureSent(): MimeMessage {
        val captor = argumentCaptor<MimeMessage>()
        verify(mailSender).send(captor.capture())
        return captor.firstValue.apply { saveChanges() }
    }

    /** O helper monta a mensagem como multipart/mixed, entao o HTML vive numa parte interna. */
    private fun textOf(part: Part): String =
        when (val content = part.content) {
            is String -> content
            is MimeMultipart -> (0 until content.count).joinToString("\n") { textOf(content.getBodyPart(it)) }
            else -> ""
        }

    /**
     * O `MULTIPART_MODE_MIXED_RELATED` aninha duas camadas: mixed > related > html,
     * entao a busca precisa ser recursiva.
     */
    private fun htmlParts(part: Part): List<Part> {
        val content = part.content
        return when (content) {
            is MimeMultipart -> (0 until content.count).flatMap { htmlParts(content.getBodyPart(it)) }
            is String -> if (part.contentType.startsWith("text/html")) listOf(part) else emptyList()
            else -> emptyList()
        }
    }

    @Test
    fun `envia com remetente, destinatario e assunto corretos`() {
        whenever(mailSender.createMimeMessage()).thenReturn(realMimeMessage())

        service.sendLeadReceived(lead, destinatario)

        val message = captureSent()
        assertThat(message.allRecipients).hasSize(1)
        assertThat(message.allRecipients.first().toString()).isEqualTo(destinatario)
        assertThat(message.from.first().toString()).isEqualTo("nao-responda@imobiliaria.com.br")
        assertThat(message.subject).isEqualTo("Novo lead recebido")
    }

    @Test
    fun `o corpo vai como HTML, dentro de um multipart`() {
        whenever(mailSender.createMimeMessage()).thenReturn(realMimeMessage())

        service.sendLeadReceived(lead, destinatario)

        val message = captureSent()
        assertThat(message.contentType).startsWith("multipart/mixed")
        assertThat(htmlParts(message)).isNotEmpty()
        assertThat(htmlParts(message).first().contentType).contains("UTF-8")
    }

    @Test
    fun `o corpo recebe data, origem e id do lead de origem, sem placeholders sobrando`() {
        whenever(mailSender.createMimeMessage()).thenReturn(realMimeMessage())

        service.sendLeadReceived(lead, destinatario)

        val body = textOf(captureSent())
        assertThat(body).contains("Zapimoveis")
        assertThat(body).contains("origin-4711")
        assertThat(body).doesNotContain("{{date}}")
        assertThat(body).doesNotContain("{{origin}}")
        assertThat(body).doesNotContain("{{originLeadId}}")
    }

    @Test
    fun `a data do corpo segue o padrao dd MM aaaa HH mm ss UTC`() {
        whenever(mailSender.createMimeMessage()).thenReturn(realMimeMessage())

        service.sendLeadReceived(lead, destinatario)

        val body = textOf(captureSent())
        val anoAtual = OffsetDateTime.now().year
        assertThat(body).containsPattern("\\d{2}/\\d{2}/$anoAtual \\d{2}:\\d{2}:\\d{2} UTC")
    }

    @Test
    fun `o template vem do classpath com UTF-8 e acentuacao preservada`() {
        whenever(mailSender.createMimeMessage()).thenReturn(realMimeMessage())

        service.sendLeadReceived(lead, destinatario)

        val body = textOf(captureSent())
        assertThat(body).contains("Não responda")
        assertThat(body).contains("Novo Lead Recebido")
        assertThat(body).contains("Este é um e-mail automático").doesNotContain("{{")
    }

    @Test
    fun `destinatario malformado e barrado pelo Jakarta Mail, nao entregue a origem`() {
        // `InternetAddress.parse` aceita ate um texto sem arroba, mas recusa
        // qualquer endereco com espaco ou caractere de controle.
        whenever(mailSender.createMimeMessage()).thenReturn(realMimeMessage())

        assertThatThrownBy { service.sendLeadReceived(lead, "a b@c.com") }
            .isInstanceOf(RuntimeException::class.java)
            .hasMessage("Falha ao enviar e-mail de lead recebido")
            .hasCauseInstanceOf(MessagingException::class.java)

        verify(mailSender, never()).send(any<MimeMessage>())
    }

    @Test
    fun `remetente ausente tambem aborta, porque o endereco vazio e invalido para o Jakarta Mail`() {
        mailProperties.from = null
        whenever(mailSender.createMimeMessage()).thenReturn(realMimeMessage())

        assertThatThrownBy { service.sendLeadReceived(lead, destinatario) }
            .isInstanceOf(RuntimeException::class.java)
            .hasMessage("Falha ao enviar e-mail de lead recebido")
            .hasCauseInstanceOf(MessagingException::class.java)
    }

    @Test
    fun `falha do servidor SMTP sobe como RuntimeException nao verificada`() {
        whenever(mailSender.createMimeMessage()).thenReturn(realMimeMessage())
        whenever(mailSender.send(any<MimeMessage>()))
            .thenThrow(org.springframework.mail.MailSendException("SMTP recusou a conexao"))

        assertThatThrownBy { service.sendLeadReceived(lead, destinatario) }
            .isInstanceOf(org.springframework.mail.MailSendException::class.java)
    }

    @Test
    fun `erro que nao e de IO nem de e-mail sobe sem reembalar`() {
        whenever(mailSender.createMimeMessage()).thenThrow(IllegalStateException("configuracao ausente"))

        assertThatThrownBy { service.sendLeadReceived(lead, destinatario) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessage("configuracao ausente")

        verify(mailSender, never()).send(any<MimeMessage>())
    }

    @Test
    fun `cria uma MimeMessage nova a cada envio, sem reaproveitar estado`() {
        whenever(mailSender.createMimeMessage()).thenReturn(realMimeMessage(), realMimeMessage())

        assertThatCode {
            service.sendLeadReceived(lead, destinatario)
            service.sendLeadReceived(lead, "outro@example.com")
        }.doesNotThrowAnyException()

        val captor = argumentCaptor<MimeMessage>()
        verify(mailSender, org.mockito.kotlin.times(2)).send(captor.capture())
        assertThat(
            captor.allValues[0]
                .allRecipients
                .first()
                .toString(),
        ).isEqualTo(destinatario)
        assertThat(
            captor.allValues[1]
                .allRecipients
                .first()
                .toString(),
        ).isEqualTo("outro@example.com")
    }
}
