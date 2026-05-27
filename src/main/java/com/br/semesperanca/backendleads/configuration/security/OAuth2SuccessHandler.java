package com.br.semesperanca.backendleads.configuration.security;

import com.br.semesperanca.backendleads.domain.model.Role;
import com.br.semesperanca.backendleads.domain.model.user.RealEstateAgent;
import com.br.semesperanca.backendleads.domain.repository.RoleRepository;
import com.br.semesperanca.backendleads.domain.repository.user.RealEstateAgentRepository;
import com.br.semesperanca.backendleads.domain.repository.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

  private final OAuth2AuthorizedClientService authorizedClientService;
  private final RealEstateAgentRepository realEstateAgentRepository;
  private final RoleRepository roleRepository;
  private final UserRepository userRepository;

  @Override
  public void onAuthenticationSuccess(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication)
      throws IOException {

    OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;

    OAuth2AuthorizedClient client =
        authorizedClientService.loadAuthorizedClient(
            oauthToken.getAuthorizedClientRegistrationId(), oauthToken.getName());

    String email = oauthToken.getPrincipal().getAttribute("email");
    String name = oauthToken.getPrincipal().getAttribute("name");

    String tokenValue;
    if (oauthToken.getPrincipal() instanceof OidcUser oidcUser) {
      tokenValue = oidcUser.getIdToken().getTokenValue();
    } else {
      tokenValue = client.getAccessToken().getTokenValue();
    }

    if (!userRepository.existsByEmail(email)) {
      Role role =
          roleRepository
              .findByName("REAL_ESTATE_AGENT")
              .orElseThrow(() -> new RuntimeException("Role não encontrada"));

      RealEstateAgent agent =
          RealEstateAgent.builder()
              .name(name)
              .email(email)
              .password("")
              .creci("GOOGLE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
              .role(role)
              .active(true)
              .build();

      realEstateAgentRepository.save(agent);
      System.out.println("Novo usuário criado via Google: " + email);
    }

    System.out.println("Login Google com sucesso! Email: " + email);
    System.out.println(
        "Token type: "
            + (oauthToken.getPrincipal() instanceof OidcUser ? "ID Token" : "Access Token"));

    String redirectUrl = "http://localhost:3000/auth/callback?token=" + tokenValue;
    getRedirectStrategy().sendRedirect(request, response, redirectUrl);
  }
}
