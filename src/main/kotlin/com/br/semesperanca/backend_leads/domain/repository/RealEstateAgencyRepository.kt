package com.br.semesperanca.backendleads.domain.repository

import com.br.semesperanca.backendleads.domain.model.RealEstateAgency
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface RealEstateAgencyRepository : ScopedRepository<RealEstateAgency, UUID> {
    fun existsByEmail(email: String): Boolean

    fun existsByCnpj(cnpj: String): Boolean

    @Query(
        """
        SELECT a
        FROM RealEstateAgency a
        WHERE LOWER(a.name) LIKE LOWER(CONCAT('%', :name, '%'))
        """,
    )
    fun searchByName(
        @Param("name") name: String,
    ): List<RealEstateAgency>
}
