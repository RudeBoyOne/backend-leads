package com.br.semesperanca.backendleads.application.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record InsightRequest(
    @JsonProperty("lead_origin") String leadOrigin,
    @JsonProperty("origin_lead_id") String originLeadId,
    @JsonProperty("transaction_type") String transactionType,
    Boolean converted,
    @JsonProperty("origin_listing_id") String originListingId,
    @JsonProperty("listing_external_code") String listingExternalCode,
    @JsonProperty("listing_neighborhood") String listingNeighborhood,
    @JsonProperty("listing_address") String listingAddress,
    @JsonProperty("lead_name") String leadName,
    @JsonProperty("lead_email") String leadEmail,
    String ddd,
    String phone,
    String message,
    String temperature,
    @JsonProperty("listing_price") Double listingPrice,
    String timestamp) {}
