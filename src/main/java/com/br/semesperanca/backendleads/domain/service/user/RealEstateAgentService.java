package com.br.semesperanca.backendleads.domain.service.user;

import com.br.semesperanca.backendleads.application.model.request.CreateRealEstateAgentRequest;
import com.br.semesperanca.backendleads.application.model.request.UpdateRealEstateAgentRequest;
import com.br.semesperanca.backendleads.application.model.request.UserFilterRequest;
import com.br.semesperanca.backendleads.application.model.response.RealEstateAgentResponse;
import com.br.semesperanca.backendleads.configuration.exception.EmailAlreadyExistsException;
import com.br.semesperanca.backendleads.domain.model.Role;
import com.br.semesperanca.backendleads.domain.model.user.RealEstateAgent;
import com.br.semesperanca.backendleads.domain.repository.RoleRepository;
import com.br.semesperanca.backendleads.domain.repository.user.RealEstateAgentRepository;
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
public class RealEstateAgentService {

  private final RealEstateAgentRepository repository;
  private final RoleRepository roleRepository;
  private final PasswordEncoder passwordEncoder;
  private final UserRepository userRepository;

  @Transactional
  public RealEstateAgentResponse create(CreateRealEstateAgentRequest request) {
    if (userRepository.existsByEmail(request.email())) {
      throw new EmailAlreadyExistsException(request.email());
    }

    if (repository.existsByCreci(request.creci())) {
      throw new RuntimeException("CRECI já cadastrado");
    }

    Role role =
        roleRepository
            .findByName("REAL_ESTATE_AGENT")
            .orElseThrow(() -> new RuntimeException("Role não encontrada"));

    RealEstateAgent agent =
        RealEstateAgent.builder()
            .name(request.name())
            .email(request.email())
            .password(passwordEncoder.encode(request.password()))
            .creci(request.creci())
            .role(role)
            .active(true)
            .build();

    return toResponse(repository.save(agent));
  }

  public RealEstateAgentResponse findById(UUID id) {
    return toResponse(findAgentById(id));
  }

  @Transactional
  public RealEstateAgentResponse update(UUID id, UpdateRealEstateAgentRequest request) {
    RealEstateAgent agent = findAgentById(id);

    agent.setName(request.name());
    agent.setEmail(request.email());
    agent.setCreci(request.creci());

    return toResponse(repository.save(agent));
  }

  @Transactional
  public RealEstateAgentResponse toggleActive(UUID id) {
    RealEstateAgent agent = findAgentById(id);
    agent.setActive(!agent.isActive());
    return toResponse(repository.save(agent));
  }

  private RealEstateAgent findAgentById(UUID id) {
    return repository
        .findById(id)
        .orElseThrow(() -> new RuntimeException("Corretor não encontrado"));
  }

  private RealEstateAgentResponse toResponse(RealEstateAgent agent) {
    return new RealEstateAgentResponse(
        agent.getId(),
        agent.getName(),
        agent.getEmail(),
        agent.getCreci(),
        agent.getRole().getName(),
        agent.isActive(),
        agent.getCreatedAt());
  }

  public Page<RealEstateAgentResponse> findAll(UserFilterRequest filter, Pageable pageable) {
    return repository.findAll(buildSpec(filter), pageable).map(this::toResponse);
  }

  private Specification<RealEstateAgent> buildSpec(UserFilterRequest filter) {
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
}
