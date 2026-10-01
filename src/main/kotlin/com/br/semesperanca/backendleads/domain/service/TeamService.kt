package com.br.semesperanca.backendleads.domain.service

import com.br.semesperanca.backendleads.application.model.request.CreateTeamRequest
import com.br.semesperanca.backendleads.application.model.request.UpdateTeamRequest
import com.br.semesperanca.backendleads.application.model.response.TeamResponse
import com.br.semesperanca.backendleads.domain.model.BranchOffice
import com.br.semesperanca.backendleads.domain.model.Team
import com.br.semesperanca.backendleads.domain.model.TeamAgent
import com.br.semesperanca.backendleads.domain.model.user.Manager
import com.br.semesperanca.backendleads.domain.repository.BranchOfficeRepository
import com.br.semesperanca.backendleads.domain.repository.TeamAgentRepository
import com.br.semesperanca.backendleads.domain.repository.TeamRepository
import com.br.semesperanca.backendleads.domain.repository.user.ManagerRepository
import com.br.semesperanca.backendleads.domain.repository.user.RealEstateAgentRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
class TeamService(
    private val teamRepository: TeamRepository,
    private val teamAgentRepository: TeamAgentRepository,
    private val managerRepository: ManagerRepository,
    private val branchOfficeRepository: BranchOfficeRepository,
    private val realEstateAgentRepository: RealEstateAgentRepository,
) {
    @Transactional
    fun create(request: CreateTeamRequest): TeamResponse {
        val manager = findManagerById(request.managerId)
        val branchOffice = request.branchOfficeId?.let { findBranchOfficeById(it) }

        val team =
            Team(
                manager = manager,
                branchOffice = branchOffice,
            )

        return toResponse(teamRepository.save(team))
    }

    fun findById(id: UUID): TeamResponse = toResponse(findTeamById(id))

    fun findAll(): List<TeamResponse> = teamRepository.findAll().map { toResponse(it) }

    fun findByBranchOfficeId(branchOfficeId: UUID): List<TeamResponse> =
        teamRepository.findByBranchOfficeId(branchOfficeId).map {
            toResponse(it)
        }

    @Transactional
    fun update(
        id: UUID,
        request: UpdateTeamRequest,
    ): TeamResponse {
        val team = findTeamById(id)
        team.branchOffice = request.branchOfficeId?.let { findBranchOfficeById(it) }
        return toResponse(teamRepository.save(team))
    }

    @Transactional
    fun delete(id: UUID) {
        val team = findTeamById(id)
        teamRepository.delete(team)
    }

    @Transactional
    fun addAgent(
        teamId: UUID,
        agentId: UUID,
    ) {
        val team = findTeamById(teamId)
        val agent = findAgentById(agentId)

        if (teamAgentRepository.existsByTeamIdAndRealEstateAgentId(teamId, agentId)) {
            throw RuntimeException("Corretor já pertence a este time")
        }

        val teamAgent =
            TeamAgent(
                team = team,
                realEstateAgent = agent,
                joinedAt = LocalDateTime.now(),
            )
        teamAgentRepository.save(teamAgent)
    }

    @Transactional
    fun removeAgent(
        teamId: UUID,
        agentId: UUID,
    ) {
        val teamAgent =
            teamAgentRepository
                .findByTeamId(teamId)
                .find { it.realEstateAgent?.id == agentId }
                ?: throw RuntimeException("Corretor não encontrado neste time")
        teamAgentRepository.delete(teamAgent)
    }

    fun findAgentsByTeamId(teamId: UUID): List<UUID> = teamAgentRepository.findByTeamId(teamId).mapNotNull { it.realEstateAgent?.id }

    private fun findTeamById(id: UUID): Team = teamRepository.findById(id).orElseThrow { RuntimeException("Time não encontrado") }

    private fun findManagerById(id: UUID): Manager =
        managerRepository.findById(id).orElseThrow { RuntimeException("Gerente não encontrado") }

    private fun findBranchOfficeById(id: UUID): BranchOffice =
        branchOfficeRepository.findById(id).orElseThrow {
            RuntimeException("Filial não encontrada")
        }

    private fun findAgentById(id: UUID) = realEstateAgentRepository.findById(id).orElseThrow { RuntimeException("Corretor não encontrado") }

    private fun toResponse(team: Team): TeamResponse =
        TeamResponse(
            id = team.id,
            managerId = team.manager?.id,
            branchOfficeId = team.branchOffice?.id,
            createdAt = team.createdAt,
        )
}
