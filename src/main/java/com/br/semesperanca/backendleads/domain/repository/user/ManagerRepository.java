package com.br.semesperanca.backendleads.domain.repository.user;

import com.br.semesperanca.backendleads.domain.model.user.Manager;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ManagerRepository extends JpaRepository<Manager, UUID> {
  Optional<Manager> findByEmail(String email);
}
