package com.br.semesperanca.backendleads.application.controller.user;

import com.br.semesperanca.backendleads.application.model.request.CreateRealEstateAgentRequest;
import com.br.semesperanca.backendleads.application.model.request.UpdateRealEstateAgentRequest;
import com.br.semesperanca.backendleads.application.model.response.RealEstateAgentResponse;
import com.br.semesperanca.backendleads.domain.service.user.RealEstateAgentService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/real-estate-agents")
@RequiredArgsConstructor
public class RealEstateAgentController {

  private final RealEstateAgentService service;

  @PostMapping
  public ResponseEntity<RealEstateAgentResponse> create(
      @RequestBody CreateRealEstateAgentRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
  }

  @GetMapping("/{id}")
  public ResponseEntity<RealEstateAgentResponse> findById(@PathVariable UUID id) {
    return ResponseEntity.ok(service.findById(id));
  }

  @GetMapping
  public ResponseEntity<List<RealEstateAgentResponse>> findAll() {
    return ResponseEntity.ok(service.findAll());
  }

  @PutMapping("/{id}")
  public ResponseEntity<RealEstateAgentResponse> update(
      @PathVariable UUID id, @RequestBody UpdateRealEstateAgentRequest request) {
    return ResponseEntity.ok(service.update(id, request));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable UUID id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }

  @PatchMapping("/{id}/toggle-active")
  public ResponseEntity<RealEstateAgentResponse> toggleActive(@PathVariable UUID id) {
    return ResponseEntity.ok(service.toggleActive(id));
  }
}
