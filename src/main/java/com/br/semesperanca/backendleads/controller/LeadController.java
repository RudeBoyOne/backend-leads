package com.br.semesperanca.backendleads.controller;

import com.br.semesperanca.backendleads.application.LeadDTO;
import com.br.semesperanca.backendleads.model.Lead;
import com.br.semesperanca.backendleads.service.LeadService;
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
  public ResponseEntity<Void> receiveLead(
      @Valid @RequestBody LeadDTO dto, HttpServletRequest request) {

    String endpoint = request.getRequestURI();
    Lead lead = leadService.ConvertDTO(dto);
    Long leadId = (lead != null) ? lead.getId() : null;
    log.info("Webhook received | Endpoint: %s | LeadId: %s", endpoint, leadId);

    return ResponseEntity.ok().build();
  }

  @GetMapping("/list")
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
