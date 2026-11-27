package com.br.semesperanca.backendleads.domain.repository

import com.br.semesperanca.backendleads.domain.model.Property
import org.springframework.stereotype.Repository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import kotlin.collections.List
import java.util.Optional
import java.util.UUID

@Repository
interface PropertyRepository : ScopedRepository<Property, UUID> {
    fun findByRealEstateAgencyId(realEstateAgencyId: UUID): List<Property>

    @Query(
        """
        SELECT p
        FROM Property p
        WHERE LOWER(p.title) LIKE LOWER(CONCAT('%', :title, '%'))
    """
    )

    fun searchByTitle(@Param("title") title: String): List<Property>
}