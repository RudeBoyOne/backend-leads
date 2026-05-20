package com.br.semesperanca.backendleads.domain.service.security;

import com.br.semesperanca.backendleads.application.model.request.LoginRequest;
import com.br.semesperanca.backendleads.application.model.response.LoginResponse;
import com.br.semesperanca.backendleads.domain.model.user.RealEstateAgent;
import com.br.semesperanca.backendleads.domain.repository.user.RealEstateAgentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final RealEstateAgentRepository repository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;

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
        jwtService.generateToken(agent.getEmail(), agent.getRole().getName(), agent.getName());

    return new LoginResponse(token, agent.getName(), agent.getEmail(), agent.getRole().getName());
  }
}
