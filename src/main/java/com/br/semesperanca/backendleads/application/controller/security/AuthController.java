package com.br.semesperanca.backendleads.application.controller.security;

import com.br.semesperanca.backendleads.application.model.request.LoginRequest;
import com.br.semesperanca.backendleads.application.model.response.LoginResponse;
import com.br.semesperanca.backendleads.domain.service.security.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;

  @PostMapping("/login")
  public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
    return ResponseEntity.ok(authService.login(request));
  }
}
