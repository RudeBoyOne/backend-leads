package com.br.semesperanca.backendleads.application.model.request;

public record CreateRealEstateAgentRequest(
    String name, String email, String password, String creci) {}
