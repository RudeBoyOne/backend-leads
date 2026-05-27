package com.br.semesperanca.backendleads.domain.repository.user;

import com.br.semesperanca.backendleads.domain.model.user.RealEstateAgent;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RealEstateAgentRepository extends JpaRepository<RealEstateAgent, UUID> {
  Optional<RealEstateAgent> findByEmail(String email);

  boolean existsByEmail(String email);

  boolean existsByCreci(String creci);
}
