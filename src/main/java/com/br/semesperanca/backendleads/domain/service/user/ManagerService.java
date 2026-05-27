package com.br.semesperanca.backendleads.domain.service.user;

import com.br.semesperanca.backendleads.application.model.request.CreateManagerRequest;
import com.br.semesperanca.backendleads.application.model.request.UpdateManagerRequest;
import com.br.semesperanca.backendleads.application.model.response.ManagerResponse;
import com.br.semesperanca.backendleads.domain.model.Role;
import com.br.semesperanca.backendleads.domain.model.user.Manager;
import com.br.semesperanca.backendleads.domain.repository.RoleRepository;
import com.br.semesperanca.backendleads.domain.repository.user.ManagerRepository;
import com.br.semesperanca.backendleads.domain.repository.user.UserRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ManagerService {

  private final ManagerRepository repository;
  private final RoleRepository roleRepository;
  private final PasswordEncoder passwordEncoder;
  private final UserRepository userRepository;

  @Transactional
  public ManagerResponse create(CreateManagerRequest request) {
    if (userRepository.existsByEmail(request.email())) {
      throw new RuntimeException("Email já cadastrado");
    }
    Role role =
        roleRepository
            .findByName("MANAGER")
            .orElseThrow(() -> new RuntimeException("Role não encontrada"));

    Manager manager =
        Manager.builder()
            .name(request.name())
            .email(request.email())
            .password(passwordEncoder.encode(request.password()))
            .branchOffice(request.branchOffice())
            .role(role)
            .active(true)
            .build();

    return toResponse(repository.save(manager));
  }

  public ManagerResponse findById(UUID id) {
    return toResponse(findManagerById(id));
  }

  public List<ManagerResponse> findAll() {
    return repository.findAll().stream().map(this::toResponse).toList();
  }

  @Transactional
  public ManagerResponse update(UUID id, UpdateManagerRequest request) {
    Manager manager = findManagerById(id);

    manager.setName(request.name());
    manager.setEmail(request.email());
    manager.setBranchOffice(request.branchOffice());

    return toResponse(repository.save(manager));
  }

  @Transactional
  public ManagerResponse toggleActive(UUID id) {
    Manager manager = findManagerById(id);
    manager.setActive(!manager.isActive());
    return toResponse(repository.save(manager));
  }

  private Manager findManagerById(UUID id) {
    return repository
        .findById(id)
        .orElseThrow(() -> new RuntimeException("Gerente não encontrado"));
  }

  private ManagerResponse toResponse(Manager manager) {
    return new ManagerResponse(
        manager.getId(),
        manager.getName(),
        manager.getEmail(),
        manager.getBranchOffice(),
        manager.getRole().getName(),
        manager.isActive(),
        manager.getCreatedAt());
  }
}
