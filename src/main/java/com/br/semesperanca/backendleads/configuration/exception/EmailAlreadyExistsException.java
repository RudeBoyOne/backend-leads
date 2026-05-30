package com.br.semesperanca.backendleads.configuration.exception;

public class EmailAlreadyExistsException extends RuntimeException {
  public EmailAlreadyExistsException(String email) {
    super("Email já cadastrado: " + email);
  }
}
