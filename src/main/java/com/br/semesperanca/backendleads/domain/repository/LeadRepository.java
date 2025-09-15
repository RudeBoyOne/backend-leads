package com.br.semesperanca.backendleads.domain.repository;

import com.br.semesperanca.backendleads.domain.model.Lead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface LeadRepository extends JpaRepository<Lead, Long>, JpaSpecificationExecutor<Lead> {
  boolean existsByOriginLeadId(String originLeadId);
}
