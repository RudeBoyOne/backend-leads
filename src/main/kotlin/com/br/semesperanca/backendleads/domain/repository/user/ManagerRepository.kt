package com.br.semesperanca.backendleads.domain.repository.user

import com.br.semesperanca.backendleads.domain.model.user.Manager
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface ManagerRepository :
    JpaRepository<Manager, UUID>,
    JpaSpecificationExecutor<Manager> {
    fun findByEmail(email: String): Optional<Manager>

    fun findByBranchOfficeId(branchOfficeId: UUID): List<Manager>
}
