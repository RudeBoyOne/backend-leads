package com.br.semesperanca.backendleads.domain.service;

import com.br.semesperanca.backendleads.application.model.InsightRequest;
import com.br.semesperanca.backendleads.application.model.InsightResponse;
import com.br.semesperanca.backendleads.domain.model.Lead;
import com.br.semesperanca.backendleads.domain.repository.LeadRepository;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class InsightClient {

  private final LeadRepository leadRepository;
  private final RestClient restClient;

  public InsightClient(
      LeadRepository leadRepository,
      RestClient.Builder restClientBuilder,
      @Value("${app.insight.url}") String insightUrl) {
    this.leadRepository = leadRepository;
    this.restClient = restClientBuilder.baseUrl(insightUrl).build();
  }

  @Async
  public void createInsight(String originLeadId) {
    Lead lead =
        leadRepository
            .findByOriginLeadId(originLeadId)
            .orElseThrow(() -> new IllegalArgumentException("Lead not found: " + originLeadId));

    InsightRequest request = mapToInsightRequest(lead);

    try {
      ResponseEntity<InsightResponse> response =
          restClient
              .post()
              .uri("/insight")
              .body(request)
              .retrieve()
              .toEntity(InsightResponse.class);

      if (response.getStatusCode() == HttpStatus.ACCEPTED
          && response.getBody() != null
          && response.getBody().id() != null) {
        lead.setInsightId(response.getBody().id());
        leadRepository.save(lead);
        log.info(
            "Insight created | LeadId: {} | InsightId: {}", originLeadId, response.getBody().id());
      }
    } catch (Exception e) {
      log.error("Failed to create insight for lead: {} | Error: {}", originLeadId, e.getMessage());
    }
  }

  private InsightRequest mapToInsightRequest(Lead lead) {
    String ddd = null;
    String phone = null;
    if (lead.getPhoneNumber() != null && lead.getPhoneNumber().length() >= 2) {
      ddd = lead.getPhoneNumber().substring(0, 2);
      phone = lead.getPhoneNumber().substring(2);
    }

    boolean converted = "converted".equalsIgnoreCase(lead.getStatus());

    String timestamp =
        lead.getTimestamp() != null
            ? lead.getTimestamp().toString()
            : OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

    return new InsightRequest(
        lead.getLeadOrigin(),
        lead.getOriginLeadId(),
        lead.getTransactionType(),
        converted,
        lead.getOriginListingId(),
        lead.getClientListingId(),
        null,
        null,
        lead.getName(),
        lead.getEmail(),
        ddd,
        phone,
        lead.getMessage(),
        lead.getTemperature(),
        null,
        timestamp);
  }
}
