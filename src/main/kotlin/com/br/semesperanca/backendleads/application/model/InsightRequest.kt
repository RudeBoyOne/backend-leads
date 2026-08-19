package com.br.semesperanca.backendleads.application.model

import com.fasterxml.jackson.annotation.JsonProperty

data class InsightRequest(
    @JsonProperty("lead_origin")
    val leadOrigin: String?,

    @JsonProperty("origin_lead_id")
    val originLeadId: String?,

    @JsonProperty("transaction_type")
    val transactionType: String?,

    val converted: Boolean?,

    @JsonProperty("origin_listing_id")
    val originListingId: String?,

    @JsonProperty("listing_external_code")
    val listingExternalCode: String?,

    @JsonProperty("listing_neighborhood")
    val listingNeighborhood: String?,

    @JsonProperty("listing_address")
    val listingAddress: String?,

    @JsonProperty("lead_name")
    val leadName: String?,

    @JsonProperty("lead_email")
    val leadEmail: String?,

    val ddd: String?,

    val phone: String?,

    val message: String?,

    val temperature: String?,

    @JsonProperty("listing_price")
    val listingPrice: Double?,

    val timestamp: String?
)