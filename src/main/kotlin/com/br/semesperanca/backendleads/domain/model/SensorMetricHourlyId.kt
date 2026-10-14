package com.br.semesperanca.backendleads.domain.model

import java.io.Serializable
import java.time.Instant

/**
 * Identificador composto de [SensorMetricHourly]: o bucket horario e
 * dispositivo. Sem classe de identificador o Hibernate rejeita a entidade por
 * ter dois atributos `@Id`; os nomes das propriedades precisam bater com os
 * atributos marcados na entidade.
 */
class SensorMetricHourlyId(
    var deviceId: String? = null,
    var bucketStart: Instant? = null,
) : Serializable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SensorMetricHourlyId) return false
        return deviceId == other.deviceId && bucketStart == other.bucketStart
    }

    override fun hashCode(): Int = 31 * (deviceId?.hashCode() ?: 0) + (bucketStart?.hashCode() ?: 0)
}
