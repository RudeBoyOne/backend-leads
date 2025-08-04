package com.br.semesperanca.backendleads.service;

import com.br.semesperanca.backendleads.application.LeadDTO;
import com.br.semesperanca.backendleads.model.Lead;
import com.br.semesperanca.backendleads.repository.LeadRepository;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LeadService {
  @Autowired private LeadRepository leadRepository;

  public void ConvertDTO(LeadDTO dto) {
    if (leadRepository.existsByOriginLeadId(dto.originLeadId())) {
      return;
    }
    Lead lead = new Lead();
    lead.setOriginLeadId(dto.originLeadId());
    lead.setClientListingId(dto.clientListingId());
    lead.setName(dto.name());
    lead.setEmail(dto.email());
    lead.setDdd(dto.ddd());
    lead.setLeadOrigin(dto.leadOrigin());
    lead.setOriginListingId(dto.originListingId());
    lead.setPhone(dto.phone());
    lead.setMessage(dto.message());
    lead.setTemperature(dto.temperature());
    lead.setTransactionType(dto.transactionType());
    lead.setTimestamp(Instant.parse(dto.timestamp()));
    leadRepository.save(lead);
  }
}
