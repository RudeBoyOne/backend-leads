package com.br.semesperanca.backendleads.domain.repository

import com.br.semesperanca.backendleads.domain.model.Team
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface TeamRepository : JpaRepository<Team, UUID> {

    fun findByManagerId(managerId: UUID): List<Team>

    fun findByManagerIdIn(managerIds: Collection<UUID>): List<Team>

    @Query("SELECT t FROM Team t WHERE LOWER(t.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    fun findByNameContainingIgnoreCase(@Param("name") name: String): List<Team>

    fun findByActive(active: Boolean): List<Team>
}
