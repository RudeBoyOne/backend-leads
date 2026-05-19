package com.br.semesperanca.backendleads.domain.repository.user;

import com.br.semesperanca.backendleads.domain.model.user.RealEstateAgency;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RealEstateAgencyRepository extends JpaRepository<RealEstateAgency, Long> {
  Optional<RealEstateAgency> findByUserId(Long userId);

  boolean existsByCreci1(String creci);
}
