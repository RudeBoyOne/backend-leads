package com.br.semesperanca.backendleads.application.model

data class LeadReceivedNotificationDTO(
    val leadIdDb: String,
    val originLeadId: String,
    val leadOrigin: String,
)
