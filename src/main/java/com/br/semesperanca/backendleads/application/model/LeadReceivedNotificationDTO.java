package com.br.semesperanca.backendleads.application.model;

public record LeadReceivedNotificationDTO(
        String leadIdDb, String originLeadId, String leadOrigin
) {
}
