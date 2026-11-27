package com.br.semesperanca.backendleads.application.model.iot

import com.fasterxml.jackson.annotation.JsonProperty

data class SensorReadingPayload(
    @param:JsonProperty("Temp")
    val temperature: Double? = null,
    @param:JsonProperty("Umid")
    val humidity: Double? = null,
    @param:JsonProperty("Lum")
    val luminosity: Double? = null,
)
