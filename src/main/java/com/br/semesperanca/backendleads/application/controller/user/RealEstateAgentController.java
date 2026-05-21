package com.br.semesperanca.backendleads.application.controller.user;

import com.br.semesperanca.backendleads.application.model.request.CreateRealEstateAgentRequest;
import com.br.semesperanca.backendleads.application.model.request.UpdateRealEstateAgentRequest;
import com.br.semesperanca.backendleads.application.model.response.RealEstateAgentResponse;
import com.br.semesperanca.backendleads.domain.service.user.RealEstateAgentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/real-estate-agents")
@RequiredArgsConstructor
@Tag(name = "Corretores", description = "Gerenciamento de corretores imobiliários")
public class RealEstateAgentController {

  private final RealEstateAgentService service;

  @GetMapping
  @PreAuthorize("hasAnyRole('FULL_ADMIN', 'ADMIN')")
  @Operation(summary = "Listar corretores", description = "Requer FULL_ADMIN ou ADMIN")
  public ResponseEntity<List<RealEstateAgentResponse>> findAll() {
    return ResponseEntity.ok(service.findAll());
  }

  @PostMapping
  @PreAuthorize("hasAnyRole('FULL_ADMIN', 'ADMIN')")
  @Operation(summary = "Criar corretor", description = "Requer FULL_ADMIN ou ADMIN")
  public ResponseEntity<RealEstateAgentResponse> create(
      @RequestBody CreateRealEstateAgentRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('FULL_ADMIN', 'ADMIN', 'REAL_ESTATE_AGENT')")
  @Operation(
      summary = "Buscar corretor por ID",
      description = "Requer FULL_ADMIN, ADMIN ou REAL_ESTATE_AGENT")
  public ResponseEntity<RealEstateAgentResponse> findById(@PathVariable UUID id) {
    return ResponseEntity.ok(service.findById(id));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('FULL_ADMIN', 'ADMIN', 'REAL_ESTATE_AGENT')")
  @Operation(
      summary = "Atualizar corretor",
      description = "Requer FULL_ADMIN, ADMIN ou REAL_ESTATE_AGENT")
  public ResponseEntity<RealEstateAgentResponse> update(
      @PathVariable UUID id, @RequestBody UpdateRealEstateAgentRequest request) {
    return ResponseEntity.ok(service.update(id, request));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAnyRole('FULL_ADMIN', 'ADMIN')")
  @Operation(summary = "Deletar corretor", description = "Requer FULL_ADMIN ou ADMIN")
  public ResponseEntity<Void> delete(@PathVariable UUID id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }

  @PatchMapping("/{id}/toggle-active")
  @PreAuthorize("hasAnyRole('FULL_ADMIN', 'ADMIN')")
  @Operation(summary = "Ativar/Desativar corretor", description = "Requer FULL_ADMIN ou ADMIN")
  public ResponseEntity<RealEstateAgentResponse> toggleActive(@PathVariable UUID id) {
    return ResponseEntity.ok(service.toggleActive(id));
  }
}
