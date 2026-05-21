package com.br.semesperanca.backendleads.application.controller.security;

import com.br.semesperanca.backendleads.application.model.request.CreateFullAdminRequest;
import com.br.semesperanca.backendleads.application.model.request.LoginRequest;
import com.br.semesperanca.backendleads.application.model.response.LoginResponse;
import com.br.semesperanca.backendleads.application.model.response.RealEstateAgentResponse;
import com.br.semesperanca.backendleads.domain.service.security.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;
  private final PasswordEncoder passwordEncoder; // 👈 adicione

  @PostMapping("/login")
  public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
    return ResponseEntity.ok(authService.login(request));
  }

  @PostMapping("/register/full-admin")
  @PreAuthorize("hasRole('FULL_ADMIN')")
  public ResponseEntity<RealEstateAgentResponse> createFullAdmin(
      @RequestBody CreateFullAdminRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(authService.createFullAdmin(request));
  }
}
