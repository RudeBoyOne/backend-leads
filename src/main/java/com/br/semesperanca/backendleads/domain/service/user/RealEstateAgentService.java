package com.br.semesperanca.backendleads.domain.service.user;

import com.br.semesperanca.backendleads.application.dto.request.CreateRealEstateAgentRequest;
import com.br.semesperanca.backendleads.application.dto.request.UpdateRealEstateAgentRequest;
import com.br.semesperanca.backendleads.application.dto.response.RealEstateAgentResponse;
import com.br.semesperanca.backendleads.domain.model.Role;
import com.br.semesperanca.backendleads.domain.model.user.RealEstateAgent;
import com.br.semesperanca.backendleads.domain.repository.RoleRepository;
import com.br.semesperanca.backendleads.domain.repository.user.RealEstateAgentRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RealEstateAgentService {

  private final RealEstateAgentRepository repository;
  private final RoleRepository roleRepository;
  private final PasswordEncoder passwordEncoder;

  @Transactional
  public RealEstateAgentResponse create(CreateRealEstateAgentRequest request) {
    if (repository.existsByEmail(request.email())) {
      throw new RuntimeException("Email já cadastrado");
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

  public List<RealEstateAgentResponse> findAll() {
    return repository.findAll().stream().map(this::toResponse).toList();
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
  public void delete(UUID id) {
    repository.delete(findAgentById(id));
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
}
