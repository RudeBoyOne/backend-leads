package com.br.semesperanca.backendleads.domain.service;

import com.br.semesperanca.backendleads.application.model.LeadReceivedNotificationDTO;
import com.br.semesperanca.backendleads.configuration.MailProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class EmailService {

  private final JavaMailSender mailSender;
  private final MailProperties mailProperties;

  public void sendLeadReceived(LeadReceivedNotificationDTO lead, String to) {
    try {
      String template = loadTemplate();
      String html = applyTemplate(template, lead);
      sendHtml(to, html);
    } catch (IOException | MessagingException e) {
      throw new RuntimeException("Falha ao enviar e-mail de lead recebido", e);
    }
  }

  private void sendHtml(String to, String html) throws MessagingException {
    MimeMessage message = mailSender.createMimeMessage();
    MimeMessageHelper helper =
        new MimeMessageHelper(
            message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());
    helper.setFrom(mailProperties.getFrom());
    helper.setTo(to);
    helper.setSubject("Novo lead recebido");
    helper.setText(html, true);
    mailSender.send(message);
  }

  private String loadTemplate() throws IOException {
    ClassPathResource resource = new ClassPathResource("templates/lead-received.html");
    byte[] bytes = resource.getInputStream().readAllBytes();
    return new String(bytes, StandardCharsets.UTF_8);
  }

  private String applyTemplate(String tpl, LeadReceivedNotificationDTO lead) {
    String now =
        OffsetDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm:ss 'UTC'XXX"));
    String origin = lead.leadOrigin();
    String originLeadId = lead.originLeadId();

    return tpl.replace("{{date}}", now)
        .replace("{{origin}}", origin)
        .replace("{{originLeadId}}", originLeadId);
  }
}
