package com.br.semesperanca.backendleads.controller;

import com.br.semesperanca.backendleads.application.LeadDTO;
import com.br.semesperanca.backendleads.service.LeadService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhook")
public class LeadController {

  @Autowired private LeadService leadService;

  @PostMapping("/lead")
  public ResponseEntity<Void> receiveLead(@Valid @RequestBody LeadDTO dto) {
    leadService.ConvertDTO(dto);
    return ResponseEntity.ok().build();
  }
}
