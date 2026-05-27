package com.br.semesperanca.backendleads.application.controller.security;

import com.br.semesperanca.backendleads.application.model.request.CreateFullAdminRequest;
import com.br.semesperanca.backendleads.application.model.request.LoginRequest;
import com.br.semesperanca.backendleads.application.model.response.LoginResponse;
import com.br.semesperanca.backendleads.application.model.response.RealEstateAgentResponse;
import com.br.semesperanca.backendleads.domain.service.security.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Endpoints de autenticação local e OAuth2")
public class AuthController {

  private final AuthService authService;

  @PostMapping("/login")
  @Operation(summary = "Login local", description = "Autentica com email e senha, retorna JWT")
  public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
    return ResponseEntity.ok(authService.login(request));
  }

  @PostMapping("/register/full-admin")
  @Operation(
      summary = "Criar FULL_ADMIN",
      description = "Cria um novo super administrador (requer FULL_ADMIN)")
  public ResponseEntity<RealEstateAgentResponse> createFullAdmin(
      @RequestBody CreateFullAdminRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(authService.createFullAdmin(request));
  }
}
