package com.br.semesperanca.backendleads.repository;

import com.br.semesperanca.backendleads.model.Lead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LeadRepository extends JpaRepository<Lead, Long> {
  boolean existsByOriginLeadId(String originLeadId);
}
