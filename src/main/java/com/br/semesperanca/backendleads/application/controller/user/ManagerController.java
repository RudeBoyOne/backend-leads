package com.br.semesperanca.backendleads.application.controller.user;

import com.br.semesperanca.backendleads.application.model.request.CreateManagerRequest;
import com.br.semesperanca.backendleads.application.model.request.ResetPasswordRequest;
import com.br.semesperanca.backendleads.application.model.request.UpdateManagerRequest;
import com.br.semesperanca.backendleads.application.model.request.UserFilterRequest;
import com.br.semesperanca.backendleads.application.model.response.ManagerResponse;
import com.br.semesperanca.backendleads.domain.service.security.AuthService;
import com.br.semesperanca.backendleads.domain.service.user.ManagerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/managers")
@RequiredArgsConstructor
@Tag(name = "Gerentes", description = "Gerenciamento de gerentes")
public class ManagerController {

  private final ManagerService service;
  private final AuthService authService;

  @GetMapping
  @Operation(summary = "Listar gerentes", description = "Requer FULL_ADMIN ou ADMIN")
  public ResponseEntity<Page<ManagerResponse>> findAll(
      @RequestParam(required = false) Boolean active,
      @RequestParam(required = false) String search,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {

    Pageable pageable = PageRequest.of(page, size);
    UserFilterRequest filter = new UserFilterRequest(active, search);
    return ResponseEntity.ok(service.findAll(filter, pageable));
  }

  @PostMapping
  @Operation(summary = "Criar gerente", description = "Requer FULL_ADMIN ou ADMIN")
  public ResponseEntity<ManagerResponse> create(@RequestBody @Valid CreateManagerRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Buscar gerente por ID")
  public ResponseEntity<ManagerResponse> findById(@PathVariable UUID id) {
    return ResponseEntity.ok(service.findById(id));
  }

  @PutMapping("/{id}")
  @Operation(summary = "Atualizar gerente")
  public ResponseEntity<ManagerResponse> update(
      @PathVariable UUID id, @RequestBody @Valid UpdateManagerRequest request) {
    return ResponseEntity.ok(service.update(id, request));
  }

  @PatchMapping("/{id}/toggle-active")
  @Operation(summary = "Ativar/Desativar gerente", description = "Requer FULL_ADMIN ou ADMIN")
  public ResponseEntity<ManagerResponse> toggleActive(@PathVariable UUID id) {
    return ResponseEntity.ok(service.toggleActive(id));
  }

  @PatchMapping("/{id}/reset-password")
  @Operation(summary = "Resetar senha do gerente", description = "Requer FULL_ADMIN ou ADMIN")
  public ResponseEntity<Void> resetPassword(
      @PathVariable UUID id, @RequestBody @Valid ResetPasswordRequest request) {
    authService.resetPassword(id, request);
    return ResponseEntity.noContent().build();
  }
}
