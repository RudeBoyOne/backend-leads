package com.br.semesperanca.backend_leads.application.mapper

import com.br.semesperanca.backend_leads.application.model.request.CreateTeamRequest
import com.br.semesperanca.backend_leads.application.model.response.TeamResponse
import com.br.semesperanca.backend_leads.domain.model.Team
import com.br.semesperanca.backend_leads.domain.model.user.Manager
import org.springframework.stereotype.Component

@Component
class TeamMapper {

    fun toEntity(request: CreateTeamRequest, manager: Manager): Team =
        Team(
            manager = manager,
            name = request.name,
            description = request.description,
            active = true,
            maxAgents = request.maxAgents,
            goalAmount = request.goalAmount,
        )

    fun toResponse(team: Team): TeamResponse =
        TeamResponse(
            id = team.id,
            managerId = team.manager?.id,
            branchOfficeId = team.manager?.branchOffice?.id,
            name = team.name,
            description = team.description,
            active = team.active,
            maxAgents = team.maxAgents,
            goalAmount = team.goalAmount,
            createdAt = team.createdAt,
        )
}
