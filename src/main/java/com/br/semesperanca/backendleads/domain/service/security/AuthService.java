package com.br.semesperanca.backendleads.domain.service.security;

import com.br.semesperanca.backendleads.application.model.request.CreateFullAdminRequest;
import com.br.semesperanca.backendleads.application.model.request.LoginRequest;
import com.br.semesperanca.backendleads.application.model.response.LoginResponse;
import com.br.semesperanca.backendleads.application.model.response.RealEstateAgentResponse;
import com.br.semesperanca.backendleads.domain.model.Role;
import com.br.semesperanca.backendleads.domain.model.user.RealEstateAgent;
import com.br.semesperanca.backendleads.domain.repository.RoleRepository;
import com.br.semesperanca.backendleads.domain.repository.user.RealEstateAgentRepository;
import com.br.semesperanca.backendleads.domain.repository.user.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final RealEstateAgentRepository repository;
  private final RoleRepository roleRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final UserRepository userRepository;

  public LoginResponse login(LoginRequest request) {
    RealEstateAgent agent =
        repository
            .findByEmail(request.email())
            .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

    if (!agent.isActive()) {
      throw new RuntimeException("Usuário inativo");
    }

    if (!passwordEncoder.matches(request.password(), agent.getPassword())) {
      throw new RuntimeException("Senha inválida");
    }

    String token =
        jwtService.generateToken(agent.getId(), agent.getEmail(), agent.getRole().getName());

    return new LoginResponse(token);
  }

  @Transactional
  public RealEstateAgentResponse createFullAdmin(CreateFullAdminRequest request) {
    if (userRepository.existsByEmail(request.email())) {

      throw new RuntimeException("Email já cadastrado");
    }

    Role role =
        roleRepository
            .findByName("FULL_ADMIN")
            .orElseThrow(() -> new RuntimeException("Role não encontrada"));

    RealEstateAgent agent =
        RealEstateAgent.builder()
            .name(request.name())
            .email(request.email())
            .password(passwordEncoder.encode(request.password()))
            .creci("FULL-ADMIN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
            .role(role)
            .active(true)
            .build();

    return toResponse(repository.save(agent));
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
