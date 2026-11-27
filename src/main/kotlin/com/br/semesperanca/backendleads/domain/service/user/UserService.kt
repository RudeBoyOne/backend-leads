package com.br.semesperanca.backendleads.domain.service.user

import com.br.semesperanca.backendleads.application.model.request.CreateUserRequest
import com.br.semesperanca.backendleads.application.model.request.UpdateUserRequest
import com.br.semesperanca.backendleads.application.model.response.UserResponse
import com.br.semesperanca.backendleads.configuration.exception.EmailAlreadyExistsException
import com.br.semesperanca.backendleads.configuration.exception.NotFoundException
import com.br.semesperanca.backendleads.domain.model.user.User
import com.br.semesperanca.backendleads.domain.repository.BranchOfficeRepository
import com.br.semesperanca.backendleads.domain.repository.RealEstateAgencyRepository
import com.br.semesperanca.backendleads.domain.repository.RoleRepository
import com.br.semesperanca.backendleads.domain.repository.user.UserRepository
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class UserService(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val realEstateAgencyRepository: RealEstateAgencyRepository,
    private val branchOfficeRepository: BranchOfficeRepository,
    private val passwordEncoder: PasswordEncoder,
) {
    @PreAuthorize("hasRole('FULL_ADMIN')")
    fun findAll(): List<UserResponse> = userRepository.findAll().map(::toResponse)

    @PreAuthorize("hasRole('FULL_ADMIN')")
    fun findById(id: UUID): UserResponse = toResponse(findUser(id))

    @PreAuthorize("hasRole('FULL_ADMIN')")
    @Transactional
    fun create(request: CreateUserRequest): UserResponse {
        if (userRepository.existsByEmail(request.email)) {
            throw EmailAlreadyExistsException(request.email)
        }

        val role =
            roleRepository
                .findById(request.roleId)
                .orElseThrow { NotFoundException("Role não encontrada") }
        val agency =
            realEstateAgencyRepository
                .findById(request.realEstateAgencyId)
                .orElseThrow { NotFoundException("Imobiliária não encontrada") }
        val branchOffice =
            request.branchOfficeId?.let {
                branchOfficeRepository.findById(it).orElseThrow { NotFoundException("Filial não encontrada") }
            }

        val user =
            User(
                realEstateAgencyId = agency.id,
                role = role,
                branchOffice = branchOffice,
                name = request.name,
                email = request.email,
                isActive = true,
            ).apply { setPassword(passwordEncoder.encode(request.password)) }

        return toResponse(userRepository.save(user))
    }

    @PreAuthorize("hasRole('FULL_ADMIN')")
    @Transactional
    fun update(
        id: UUID,
        request: UpdateUserRequest,
    ): UserResponse {
        val user = findUser(id)

        request.name?.let { user.name = it }
        request.email?.let {
            if (it != user.email && userRepository.existsByEmail(it)) {
                throw EmailAlreadyExistsException(it)
            }
            user.email = it
        }
        request.roleId?.let { roleId ->
            user.role = roleRepository.findById(roleId).orElseThrow { NotFoundException("Role não encontrada") }
        }
        request.realEstateAgencyId?.let { user.realEstateAgencyId = it }
        request.branchOfficeId?.let { branchOfficeId ->
            user.branchOffice =
                branchOfficeRepository.findById(branchOfficeId).orElseThrow { NotFoundException("Filial não encontrada") }
        }
        request.active?.let { user.isActive = it }

        return toResponse(userRepository.save(user))
    }

    @PreAuthorize("hasRole('FULL_ADMIN')")
    @Transactional
    fun delete(id: UUID) {
        userRepository.delete(findUser(id))
    }

    private fun findUser(id: UUID): User = userRepository.findById(id).orElseThrow { NotFoundException("Usuário não encontrado") }

    private fun toResponse(user: User): UserResponse =
        UserResponse(
            id = user.id!!,
            name = user.name!!,
            email = user.email!!,
            role = user.role?.name,
            realEstateAgencyId = user.realEstateAgencyId!!,
            branchOfficeId = user.branchOffice?.id,
            active = user.isActive,
            createdAt = user.createdAt,
            updatedAt = user.updatedAt,
        )
}
