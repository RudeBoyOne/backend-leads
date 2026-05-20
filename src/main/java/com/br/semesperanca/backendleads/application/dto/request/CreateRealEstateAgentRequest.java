package com.br.semesperanca.backendleads.application.dto.request;

public record CreateRealEstateAgentRequest(
    String name, String email, String password, String creci) {}
