package com.br.semesperanca.backendleads.domain.repository

import com.br.semesperanca.backendleads.domain.model.Lead
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface LeadRepository : JpaRepository<Lead, UUID>, JpaSpecificationExecutor<Lead> {
    fun existsByOriginLeadId(originLeadId: String): Boolean

    fun findByOriginLeadId(originLeadId: String): Optional<Lead>
}