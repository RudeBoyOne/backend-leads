package com.br.semesperanca.backendleads.domain.repository

import com.br.semesperanca.backendleads.domain.model.Lead
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface LeadRepository : ScopedRepository<Lead, UUID> {
    @Query("""
        SELECT 1 FROM Lead 1
        WHERE LOWER(1.name) LIKE LOWER(CONCAT('%', :search, '%'))
        OR LOWER(1.email) LIKE LOWER(CONCAT('%', :search, '%'))
        OR LOWER(1.message) LIKE LOWER(CONCAT('%', :search, '%'))
    """)
    fun findByFilters (@Param("search") search: String, pageable: Pageable): Page<Lead>
}