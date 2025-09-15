package com.br.semesperanca.backendleads.domain.service;

import com.br.semesperanca.backendleads.application.model.LeadReceivedNotificationDTO;
import com.br.semesperanca.backendleads.domain.model.Lead;
import com.br.semesperanca.backendleads.domain.repository.LeadRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
public class LeadService {
    private final LeadRepository leadRepository;

    public LeadService(LeadRepository leadRepository) {
        this.leadRepository = leadRepository;
    }

    public void receiveLead(LeadReceivedNotificationDTO receivedNotificationDTO) {
        System.out.println("Received Lead Notification DTO: " + receivedNotificationDTO);
    }

    public Page<Lead> findLeads(String status, Long corretorId, String origin, String search, Pageable pageable) {
        Specification<Lead> spec = (root, query, cb) -> cb.conjunction();

        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("temperature"), status));
        }
        if (corretorId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("clientListingId"), corretorId.toString()));
        }
        if (origin != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("leadOrigin"), origin));
        }
        if (search != null) {
            spec = spec.and((root, query, cb) -> cb.or(cb.like(cb.lower(root.get("name")), "%" + search.toLowerCase() + "%"), cb.like(cb.lower(root.get("email")), "%" + search.toLowerCase() + "%")));
        }

        return leadRepository.findAll(spec, pageable);
    }

}
