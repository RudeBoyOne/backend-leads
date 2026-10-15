package com.br.semesperanca.backendleads.application.model.iot

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Payload enviado pelo dispositivo IoT ao broker.
 *
 * - Temp: temperatura do DHT22
 * - Umid: umidade do DHT22
 * - Lum: luminosidade do LDR
 */
data class SensorReadingPayload(
    @param:JsonProperty("Temp")
    val temperature: Double? = null,
    @param:JsonProperty("Umid")
    val humidity: Double? = null,
    @param:JsonProperty("Lum")
    val luminosity: Double? = null,
)
