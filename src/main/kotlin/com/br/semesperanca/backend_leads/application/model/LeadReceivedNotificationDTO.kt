package com.br.semesperanca.backendleads.application.model

data class LeadReceivedNotificationDTO(
    val leadId: String,
    val originLeadId: String,
    val leadOrigin: String,
)