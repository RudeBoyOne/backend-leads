package com.br.semesperanca.backend_leads.application.model

data class LeadReceivedNotificationDTO(
    val leadId: String,
    val originLeadId: String,
    val leadOrigin: String,
)