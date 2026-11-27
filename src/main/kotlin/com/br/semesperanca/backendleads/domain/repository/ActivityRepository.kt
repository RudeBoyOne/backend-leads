package com.br.semesperanca.backendleads.domain.repository

import com.br.semesperanca.backendleads.domain.model.Activity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ActivityRepository : JpaRepository<Activity, UUID>, JpaSpecificationExecutor<Activity> {

    @Query("SELECT a FROM Activity a WHERE LOWER(a.title) LIKE LOWER(CONCAT('%', :title, '%'))")
    fun findByTitleContainingIgnoreCase(@Param("title") title: String): List<Activity>

    @Query("SELECT a FROM Activity a WHERE LOWER(a.description) LIKE LOWER(CONCAT('%', :description, '%'))")
    fun findByDescriptionContainingIgnoreCase(@Param("description") description: String): List<Activity>

    fun findByLeadId(leadId: UUID): List<Activity>

    fun findByPropertyId(propertyId: UUID): List<Activity>

    fun findByCompleted(completed: Boolean): List<Activity>
}
