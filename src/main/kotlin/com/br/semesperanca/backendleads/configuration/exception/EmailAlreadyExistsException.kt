package com.br.semesperanca.backendleads.configuration.exception

class EmailAlreadyExistsException(email: String) : RuntimeException("Email já cadastrado: $email")