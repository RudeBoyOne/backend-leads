package com.br.semesperanca.backendleads.domain.service

import com.br.semesperanca.backendleads.application.model.request.CreateRoleRequest
import com.br.semesperanca.backendleads.application.model.request.UpdateRoleRequest
import com.br.semesperanca.backendleads.application.model.response.RoleResponse
import com.br.semesperanca.backendleads.configuration.exception.NotFoundException
import com.br.semesperanca.backendleads.domain.model.Role
import com.br.semesperanca.backendleads.domain.repository.RoleRepository
import com.br.semesperanca.backendleads.domain.repository.user.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Service
class RoleService(
    private val roleRepository: RoleRepository,
    private val userRepository: UserRepository,
) {
    fun findAll(): List<RoleResponse> = roleRepository.findAll().map(::toResponse)

    fun findById(id: UUID): RoleResponse = toResponse(findRole(id))

    @PreAuthorize("hasRole('FULL_ADMIN')")
    @Transactional
    fun create(request: CreateRoleRequest): RoleResponse {
        if (roleRepository.findByName(request.name).isPresent) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Role já cadastrada")
        }

        val role =
            Role(
                name = request.name,
                description = request.description,
                active = true,
                hierarchyLevel = request.hierarchyLevel,
            )
        return toResponse(roleRepository.save(role))
    }

    @PreAuthorize("hasRole('FULL_ADMIN')")
    @Transactional
    fun update(
        id: UUID,
        request: UpdateRoleRequest,
    ): RoleResponse {
        val role = findRole(id)

        request.name?.let { name ->
            if (name != role.name && roleRepository.findByName(name).isPresent) {
                throw ResponseStatusException(HttpStatus.CONFLICT, "Role já cadastrada")
            }
            role.name = name
        }
        request.description?.let { role.description = it }
        request.active?.let { role.active = it }
        request.hierarchyLevel?.let { role.hierarchyLevel = it }

        return toResponse(roleRepository.save(role))
    }

    @PreAuthorize("hasRole('FULL_ADMIN')")
    @Transactional
    fun delete(id: UUID) {
        val role = findRole(id)

        if (role.name in SEEDED_ROLES) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Roles do sistema não podem ser removidas")
        }
        if (userRepository.existsByRoleId(id)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Remova os usuários vinculados antes de excluir a role")
        }

        roleRepository.delete(role)
    }

    private fun findRole(id: UUID): Role = roleRepository.findById(id).orElseThrow { NotFoundException("Role não encontrada") }

    private fun toResponse(role: Role): RoleResponse =
        RoleResponse(
            id = role.id!!,
            name = role.name!!,
            description = role.description,
            active = role.active,
            hierarchyLevel = role.hierarchyLevel,
            createdAt = role.createdAt,
            updatedAt = role.updatedAt,
        )

    companion object {
        private val SEEDED_ROLES =
            setOf(
                Role.FULL_ADMIN,
                Role.ADMIN,
                Role.MANAGER,
                Role.REAL_ESTATE_AGENT,
                "TEAM_AGENT",
                "USER",
            )
    }
}
