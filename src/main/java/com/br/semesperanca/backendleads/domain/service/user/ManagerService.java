package com.br.semesperanca.backendleads.domain.service.user;

import com.br.semesperanca.backendleads.application.model.request.CreateManagerRequest;
import com.br.semesperanca.backendleads.application.model.request.UpdateManagerRequest;
import com.br.semesperanca.backendleads.application.model.request.UserFilterRequest;
import com.br.semesperanca.backendleads.application.model.response.ManagerResponse;
import com.br.semesperanca.backendleads.configuration.exception.EmailAlreadyExistsException;
import com.br.semesperanca.backendleads.domain.model.Role;
import com.br.semesperanca.backendleads.domain.model.user.Manager;
import com.br.semesperanca.backendleads.domain.repository.RoleRepository;
import com.br.semesperanca.backendleads.domain.repository.user.ManagerRepository;
import com.br.semesperanca.backendleads.domain.repository.user.UserRepository;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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
      throw new EmailAlreadyExistsException(request.email());
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

  public Page<ManagerResponse> findAll(UserFilterRequest filter, Pageable pageable) {
    return repository.findAll(buildSpec(filter), pageable).map(this::toResponse);
  }

  private Specification<Manager> buildSpec(UserFilterRequest filter) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();

      if (filter.active() != null) {
        predicates.add(cb.equal(root.get("active"), filter.active()));
      }

      if (filter.search() != null && !filter.search().isBlank()) {
        String pattern = "%" + filter.search().toLowerCase() + "%";
        predicates.add(
            cb.or(
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(root.get("email")), pattern)));
      }

      return cb.and(predicates.toArray(new Predicate[0]));
    };
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
