package com.br.semesperanca.backendleads.domain.model

import java.io.Serializable
import java.time.Instant

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
