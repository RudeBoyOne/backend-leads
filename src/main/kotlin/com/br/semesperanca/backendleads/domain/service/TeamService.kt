package com.br.semesperanca.backendleads.domain.service

import com.br.semesperanca.backendleads.application.model.request.CreateTeamRequest
import com.br.semesperanca.backendleads.application.model.request.UpdateTeamRequest
import com.br.semesperanca.backendleads.application.model.response.TeamResponse
import com.br.semesperanca.backendleads.configuration.exception.NotFoundException
import com.br.semesperanca.backendleads.domain.model.BranchOffice
import com.br.semesperanca.backendleads.domain.model.Team
import com.br.semesperanca.backendleads.domain.model.TeamAgent
import com.br.semesperanca.backendleads.domain.model.user.Manager
import com.br.semesperanca.backendleads.domain.repository.BranchOfficeRepository
import com.br.semesperanca.backendleads.domain.repository.TeamAgentRepository
import com.br.semesperanca.backendleads.domain.repository.TeamRepository
import com.br.semesperanca.backendleads.domain.repository.user.ManagerRepository
import com.br.semesperanca.backendleads.domain.repository.user.RealEstateAgentRepository
import org.springframework.security.access.prepost.PreAuthorize
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
    @PreAuthorize("hasRole('FULL_ADMIN') or hasRole('ADMIN') or hasRole('MANAGER')")
    @Transactional
    fun create(request: CreateTeamRequest): TeamResponse {
        val manager = findManagerById(request.managerId)
        return toResponse(teamRepository.save(Team(manager = manager)))
    }

    fun findById(id: UUID): TeamResponse = toResponse(findTeamById(id))

    fun findAll(): List<TeamResponse> = teamRepository.findAll().map { toResponse(it) }

    /**
     * Times de uma filial. A filial do time é derivada do gerente, não
     * armazenada: `team.branch_office_id` saiu do schema.
     *
     * A filial é validada pelo [BranchOfficeRepository] escopado primeiro, então
     * pedir `/teams/branch-office/{id}` com a filial de outra agência não vaza
     * nada: o `findById` não resolve e a chamada sai em 404.
     */
    fun findByBranchOfficeId(branchOfficeId: UUID): List<TeamResponse> {
        findBranchOfficeById(branchOfficeId)
        val managerIds = managerRepository.findByBranchOfficeId(branchOfficeId).mapNotNull { it.id }
        if (managerIds.isEmpty()) return emptyList()
        return teamRepository.findByManagerIdIn(managerIds).map { toResponse(it) }
    }

    @PreAuthorize("hasRole('FULL_ADMIN') or hasRole('ADMIN') or hasRole('MANAGER')")
    @Transactional
    fun update(
        id: UUID,
        request: UpdateTeamRequest,
    ): TeamResponse {
        val team = findTeamById(id)
        request.managerId?.let { team.manager = findManagerById(it) }
        return toResponse(teamRepository.save(team))
    }

    @PreAuthorize("hasRole('FULL_ADMIN') or hasRole('ADMIN') or hasRole('MANAGER')")
    @Transactional
    fun delete(id: UUID) {
        val team = findTeamById(id)
        teamRepository.delete(team)
    }

    @PreAuthorize("hasRole('FULL_ADMIN') or hasRole('ADMIN') or hasRole('MANAGER')")
    @Transactional
    fun addAgent(
        teamId: UUID,
        agentId: UUID,
    ) {
        val team = findTeamById(teamId)
        val agent = findAgentById(agentId)

        if (teamAgentRepository.existsByTeamIdAndRealEstateAgentId(teamId, agentId)) {
            throw IllegalStateException("Corretor já pertence a este time")
        }

        val teamAgent =
            TeamAgent(
                team = team,
                realEstateAgent = agent,
                joinedAt = LocalDateTime.now(),
            )
        teamAgentRepository.save(teamAgent)
    }

    @PreAuthorize("hasRole('FULL_ADMIN') or hasRole('ADMIN') or hasRole('MANAGER')")
    @Transactional
    fun removeAgent(
        teamId: UUID,
        agentId: UUID,
    ) {
        val teamAgent =
            teamAgentRepository
                .findByTeamId(teamId)
                .find { it.realEstateAgent?.id == agentId }
                ?: throw NotFoundException("Corretor não encontrado neste time")
        teamAgentRepository.delete(teamAgent)
    }

    fun findAgentsByTeamId(teamId: UUID): List<UUID> = teamAgentRepository.findByTeamId(teamId).mapNotNull { it.realEstateAgent?.id }

    private fun findTeamById(id: UUID): Team = teamRepository.findById(id).orElseThrow { NotFoundException("Time não encontrado") }

    private fun findManagerById(id: UUID): Manager =
        managerRepository.findById(id).orElseThrow { NotFoundException("Gerente não encontrado") }

    private fun findBranchOfficeById(id: UUID): BranchOffice =
        branchOfficeRepository.findById(id).orElseThrow { NotFoundException("Filial não encontrada") }

    private fun findAgentById(id: UUID) =
        realEstateAgentRepository.findById(id).orElseThrow { NotFoundException("Corretor não encontrado") }

    private fun toResponse(team: Team): TeamResponse =
        TeamResponse(
            id = team.id,
            managerId = team.manager?.id,
            branchOfficeId = team.manager?.branchOffice?.id,
            createdAt = team.createdAt,
        )
}
