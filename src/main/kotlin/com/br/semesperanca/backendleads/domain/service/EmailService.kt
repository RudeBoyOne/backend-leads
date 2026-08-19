package com.br.semesperanca.backendleads.domain.service

import com.br.semesperanca.backendleads.application.model.LeadReceivedNotificationDTO
import com.br.semesperanca.backendleads.configuration.MailProperties
import jakarta.mail.MessagingException
import jakarta.mail.internet.MimeMessage
import org.springframework.core.io.ClassPathResource
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

@Service
class EmailService(
    private val mailSender: JavaMailSender,
    private val mailProperties: MailProperties
) {
    fun sendLeadReceived(lead: LeadReceivedNotificationDTO, to: String) {
        try {
            val template = loadTemplate()
            val html = applyTemplate(template, lead)
            sendHtml(to, html)
        } catch (e: Exception) {
            if (e is IOException || e is MessagingException) {
                throw RuntimeException("Falha ao enviar e-mail de lead recebido", e)
            }
            throw e
        }
    }

    @Throws(MessagingException::class)
    private fun sendHtml(to: String, html: String) {
        val message: MimeMessage = mailSender.createMimeMessage()
        val helper = MimeMessageHelper(
            message,
            MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
            StandardCharsets.UTF_8.name()
        )
        helper.setFrom(mailProperties.from ?: "")
        helper.setTo(to)
        helper.setSubject("Novo lead recebido")
        helper.setText(html, true)
        mailSender.send(message)
    }

    @Throws(IOException::class)
    private fun loadTemplate(): String {
        val resource = ClassPathResource("templates/lead-received.html")
        val bytes = resource.inputStream.readAllBytes()
        return String(bytes, StandardCharsets.UTF_8)
    }

    private fun applyTemplate(tpl: String, lead: LeadReceivedNotificationDTO): String {
        val now = OffsetDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm:ss 'UTC'XXX"))
        val origin = lead.leadOrigin
        val originLeadId = lead.originLeadId

        return tpl.replace("{{date}}", now)
            .replace("{{origin}}", origin)
            .replace("{{originLeadId}}", originLeadId)
    }
}