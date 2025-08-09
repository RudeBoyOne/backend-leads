package com.br.semesperanca.backendleads.controller;

import com.br.semesperanca.backendleads.application.LeadDTO;
import com.br.semesperanca.backendleads.service.LeadService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Instant;
import org.jboss.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhook")
public class LeadController {

  private static final Logger logger = Logger.getLogger(LeadController.class);

  @Autowired private LeadService leadService;

  @PostMapping("/lead")
  public ResponseEntity<Void> receiveLead(
      @Valid @RequestBody LeadDTO dto, HttpServletRequest request) {

    String ip = request.getHeader("X-Forwarded-For");
    if (ip == null) {
      ip = request.getRemoteAddr();
    }

    String userAgent = request.getHeader("User-Agent");
    String timestamp = Instant.now().toString();

    logger.infof(
        "Webhook received | Timestamp: %s | IP: %s | Origin: %s | User-Agent: %s",
        timestamp, ip, dto.leadOrigin(), userAgent);

    leadService.ConvertDTO(dto);
    return ResponseEntity.ok().build();
  }
}
