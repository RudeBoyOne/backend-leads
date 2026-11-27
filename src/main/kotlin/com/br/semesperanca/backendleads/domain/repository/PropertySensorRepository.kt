package com.br.semesperanca.backendleads.domain.repository

import com.br.semesperanca.backendleads.domain.model.PropertySensor
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface PropertySensorRepository : JpaRepository<PropertySensor, UUID> {
    fun findByPropertyId(propertyId: UUID): Optional<PropertySensor>

    fun findByDeviceId(deviceId: String): Optional<PropertySensor>

    fun findAllByOrderByPropertyIdAsc(): List<PropertySensor>
}
