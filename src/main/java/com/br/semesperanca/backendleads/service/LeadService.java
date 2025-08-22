package com.br.semesperanca.backendleads.service;

import com.br.semesperanca.backendleads.application.LeadDTO;
import com.br.semesperanca.backendleads.model.Lead;
import com.br.semesperanca.backendleads.repository.LeadRepository;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
public class LeadService {
  @Autowired private LeadRepository leadRepository;

  public Lead ConvertDTO(LeadDTO dto) {
    if (leadRepository.existsByOriginLeadId(dto.originLeadId())) {
      return null;
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

    return lead;
  }

  public Page<Lead> findLeads(
      String status, Long corretorId, String origin, String search, Pageable pageable) {
    Specification<Lead> spec = Specification.where(null);

    if (status != null) {
      spec = spec.and((root, query, cb) -> cb.equal(root.get("temperature"), status));
    }
    if (corretorId != null) {
      spec =
          spec.and(
              (root, query, cb) -> cb.equal(root.get("clientListingId"), corretorId.toString()));
    }
    if (origin != null) {
      spec = spec.and((root, query, cb) -> cb.equal(root.get("leadOrigin"), origin));
    }
    if (search != null) {
      spec =
          spec.and(
              (root, query, cb) ->
                  cb.or(
                      cb.like(cb.lower(root.get("name")), "%" + search.toLowerCase() + "%"),
                      cb.like(cb.lower(root.get("email")), "%" + search.toLowerCase() + "%")));
    }

    return leadRepository.findAll(spec, pageable);
  }
}
