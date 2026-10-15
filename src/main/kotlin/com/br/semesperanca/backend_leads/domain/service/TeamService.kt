package com.br.semesperanca.backend_leads.domain.service

import com.br.semesperanca.backend_leads.application.mapper.TeamMapper
import com.br.semesperanca.backend_leads.application.model.request.CreateTeamRequest
import com.br.semesperanca.backend_leads.application.model.request.UpdateTeamRequest
import com.br.semesperanca.backend_leads.application.model.response.TeamResponse
import com.br.semesperanca.backend_leads.domain.model.Team
import com.br.semesperanca.backend_leads.domain.model.TeamAgent
import com.br.semesperanca.backend_leads.domain.model.user.Manager
import com.br.semesperanca.backend_leads.domain.repository.TeamAgentRepository
import com.br.semesperanca.backend_leads.domain.repository.TeamRepository
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
class TeamService(
    private val teamRepository: TeamRepository,
    private val teamAgentRepository: TeamAgentRepository,
    private val teamMapper: TeamMapper,
) {
    @PreAuthorize("hasRole('FULL_ADMIN') or hasRole('ADMIN') or hasRole('MANAGER')")
    @Transactional
    fun create(request: CreateTeamRequest): TeamResponse {
        val manager = findManagerReference(request.managerId)
        val team = teamMapper.toEntity(request, manager)
        return teamMapper.toResponse(teamRepository.save(team))
    }

    fun findById(id: UUID): TeamResponse =
        teamMapper.toResponse(findTeamById(id))

    fun findAll(): List<TeamResponse> =
        teamRepository.findAll().map { teamMapper.toResponse(it) }

    fun findByName(name: String): List<TeamResponse> =
        teamRepository.findByNameContainingIgnoreCase(name).map { teamMapper.toResponse(it) }

    fun findByBranchOfficeId(branchOfficeId: UUID): List<TeamResponse> =
        teamRepository.findAll()
            .filter { it.manager?.branchOffice?.id == branchOfficeId }
            .map { teamMapper.toResponse(it) }

    @PreAuthorize("hasRole('FULL_ADMIN') or hasRole('ADMIN') or hasRole('MANAGER')")
    @Transactional
    fun update(id: UUID, request: UpdateTeamRequest): TeamResponse {
        val team = findTeamById(id)
        request.managerId?.let { team.manager = findManagerReference(it) }
        team.name = request.name
        team.description = request.description
        request.maxAgents?.let { team.maxAgents = it }
        request.goalAmount?.let { team.goalAmount = it }
        return teamMapper.toResponse(teamRepository.save(team))
    }

    @PreAuthorize("hasRole('FULL_ADMIN') or hasRole('ADMIN') or hasRole('MANAGER')")
    @Transactional
    fun delete(id: UUID) {
        teamRepository.delete(findTeamById(id))
    }

    @PreAuthorize("hasRole('FULL_ADMIN') or hasRole('ADMIN') or hasRole('MANAGER')")
    @Transactional
    fun addAgent(teamId: UUID, agentId: UUID) {
        val team = findTeamById(teamId)

        if (teamAgentRepository.existsByTeamIdAndRealEstateAgentId(teamId, agentId)) {
            throw IllegalStateException("Corretor já pertence a este time")
        }

        val teamAgent = TeamAgent(
            team = team,
            joinedAt = LocalDateTime.now(),
        )
        teamAgentRepository.save(teamAgent)
    }

    @PreAuthorize("hasRole('FULL_ADMIN') or hasRole('ADMIN') or hasRole('MANAGER')")
    @Transactional
    fun removeAgent(teamId: UUID, agentId: UUID) {
        val teamAgent = teamAgentRepository.findByTeamId(teamId)
            .find { it.realEstateAgent?.id == agentId }
            ?: throw RuntimeException("Corretor não encontrado neste time")
        teamAgentRepository.delete(teamAgent)
    }

    fun findAgentsByTeamId(teamId: UUID): List<UUID> =
        teamAgentRepository.findByTeamId(teamId).mapNotNull { it.realEstateAgent?.id }

    private fun findTeamById(id: UUID): Team =
        teamRepository.findById(id).orElseThrow { RuntimeException("Time não encontrado") }

    private fun findManagerReference(managerId: UUID): Manager =
        Manager().apply { id = managerId }
}
