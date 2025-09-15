package com.br.semesperanca.backendleads.application.controller;

import com.br.semesperanca.backendleads.application.model.LeadReceivedNotificationDTO;
import com.br.semesperanca.backendleads.domain.model.Lead;
import com.br.semesperanca.backendleads.domain.service.LeadService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping("/leads")
public class LeadController {

  private final LeadService leadService;

  @PostMapping("/receive")
  public ResponseEntity<Void> receive(
      @Valid @RequestBody LeadReceivedNotificationDTO receivedNotificationDTO,
      HttpServletRequest request) {

    leadService.receiveLead(receivedNotificationDTO);

    String endpoint = request.getRequestURI();
    log.info(
        "Received | Endpoint: {} | LeadId: {}", endpoint, receivedNotificationDTO.originLeadId());

    return ResponseEntity.ok().build();
  }

  @GetMapping("/search")
  public ResponseEntity<?> getLeads(
      @RequestParam(required = false) String status,
      @RequestParam(required = false) Long corretorId,
      @RequestParam(required = false) String origin,
      @RequestParam(required = false) String search,
      Pageable pageable) {
    Page<Lead> leads = leadService.findLeads(status, corretorId, origin, search, pageable);
    return ResponseEntity.ok(leads);
  }
}
