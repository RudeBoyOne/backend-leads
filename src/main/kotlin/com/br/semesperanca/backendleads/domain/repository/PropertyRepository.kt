package com.br.semesperanca.backendleads.domain.repository

import com.br.semesperanca.backendleads.domain.model.Property
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository
import java.util.List
import java.util.Optional
import java.util.UUID

@Repository
interface PropertyRepository : JpaRepository<Property, UUID>, JpaSpecificationExecutor<Property> {
    fun findByRealEstateAgencyId(realEstateAgencyId: UUID): List<Property>
    fun findByTitleContainingIgnoreCase(title: String): Optional<Property>
}