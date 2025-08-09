package com.br.semesperanca.backendleads.security.hmac;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import org.jboss.logging.Logger;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

@Component
public class hmacAuthFilter extends OncePerRequestFilter {

  private static final Logger log = Logger.getLogger(hmacAuthFilter.class);

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {

    ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
    String ip = wrappedRequest.getRemoteAddr();
    String timestamp = Instant.now().toString();
    String userAgent = request.getHeader("User-Agent");

    String receivedSignature = wrappedRequest.getHeader("X-SIGNATURE");

    if (receivedSignature == null) {
      log.warnf(
          "Webhook rejected | Timestamp: %s | IP: %s | User-Agent: %s | Status: 401 | Reason: Missing signature",
          timestamp, ip, userAgent);
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing signature");
      return;
    }

    wrappedRequest.getParameterMap();
    String body =
        new String(
            wrappedRequest.getContentAsByteArray(),
            wrappedRequest.getCharacterEncoding() != null
                ? Charset.forName(wrappedRequest.getCharacterEncoding())
                : StandardCharsets.UTF_8);

    try {

      String expectedSignature = hmacUtil.generateHmac(body, hmacSecretKey.SECRET);

      System.out.println("BODY RECEBIDO: [" + body + "]");
      System.out.println("SIGNATURE RECEBIDA: [" + receivedSignature + "]");
      System.out.println("SIGNATURE ESPERADA: [" + expectedSignature + "]");

      if (!MessageDigest.isEqual(expectedSignature.getBytes(), receivedSignature.getBytes())) {
        log.warnf(
            "Webhook rejected | Timestamp: %s | IP: %s | User-Agent: %s | Status: 401 | Reason: Invalid signature",
            timestamp, ip, userAgent);
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid signature");
        return;
      }

      log.infof(
          "Webhook received | Timestamp: %s | IP: %s | User-Agent: %s | Status: 200",
          timestamp, ip, userAgent);
      filterChain.doFilter(wrappedRequest, response);

    } catch (Exception e) {
      log.errorf(
          "Webhook error | Timestamp: %s | IP: %s | User-Agent: %s | Status: 500 | Reason: %s",
          timestamp, ip, e.getMessage());
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Signature error");
    }
  }
}
