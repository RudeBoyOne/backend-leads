package com.br.semesperanca.backendleads.application.model.response;

public record LoginResponse(String token, String name, String email, String role) {}
