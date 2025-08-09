package com.br.semesperanca.backendleads.security.hmac;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class hmacSecretKey {

  public static String SECRET;

  @Value("${hmac.secret}")
  public void setSecret(String secret) {
    hmacSecretKey.SECRET = secret;
  }
}
