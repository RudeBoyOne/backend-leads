package com.br.semesperanca.backendleads.security.hmac;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class hmacUtil {

  public static String generateHmac(String data, String secret) throws Exception {
    SecretKeySpec keySpec =
        new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(keySpec);
    byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    return Base64.getEncoder().encodeToString(rawHmac);
  }

  public static boolean verifyHmac(String data, String secret, String signature) {
    try {
      String expectedSignature = generateHmac(data, secret);
      return MessageDigest.isEqual(
          expectedSignature.getBytes(StandardCharsets.UTF_8),
          signature.getBytes(StandardCharsets.UTF_8));
    } catch (Exception e) {
      return false;
    }
  }
}
