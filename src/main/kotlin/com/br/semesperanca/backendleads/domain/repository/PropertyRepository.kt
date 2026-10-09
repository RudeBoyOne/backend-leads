package com.br.semesperanca.backendleads.domain.repository

import com.br.semesperanca.backendleads.domain.model.Property
import org.springframework.stereotype.Repository
import java.util.List
import java.util.Optional
import java.util.UUID

@Repository
interface PropertyRepository : ScopedRepository<Property, UUID> {
    fun findByRealEstateAgencyId(realEstateAgencyId: UUID): List<Property>

    fun findByTitleContainingIgnoreCase(title: String): Optional<Property>
}
