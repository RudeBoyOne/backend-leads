package com.br.semesperanca.backendleads.controller;

import com.br.semesperanca.backendleads.application.LeadDTO;
import com.br.semesperanca.backendleads.model.Lead;
import com.br.semesperanca.backendleads.service.LeadService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.jboss.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhook")
public class LeadController {

  private static final Logger logger = Logger.getLogger(LeadController.class);

  @Autowired private LeadService leadService;

  private static final Logger log = Logger.getLogger(LeadController.class);

  @PostMapping("/lead")
  public ResponseEntity<Void> receiveLead(
      @Valid @RequestBody LeadDTO dto, HttpServletRequest request) {

    String endpoint = request.getRequestURI();
    Lead lead = leadService.ConvertDTO(dto);
    Long leadId = (lead != null) ? lead.getId() : null;
    log.infof("Webhook received | Endpoint: %s | LeadId: %s", endpoint, leadId);

    return ResponseEntity.ok().build();
  }

  @GetMapping("/leads")
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
