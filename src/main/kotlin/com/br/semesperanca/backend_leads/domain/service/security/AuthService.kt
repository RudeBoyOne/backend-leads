package com.br.semesperanca.backendleads.domain.service.security

import com.br.semesperanca.backendleads.application.model.request.ChangePasswordRequest
import com.br.semesperanca.backendleads.application.model.request.CreateFullAdminRequest
import com.br.semesperanca.backendleads.application.model.request.LoginRequest
import com.br.semesperanca.backendleads.application.model.request.ResetPasswordRequest
import com.br.semesperanca.backendleads.application.model.response.LoginResponse
import com.br.semesperanca.backendleads.application.model.response.RealEstateAgentResponse
import com.br.semesperanca.backendleads.domain.model.Role
import com.br.semesperanca.backendleads.domain.model.user.RealEstateAgent
import com.br.semesperanca.backendleads.domain.model.user.User
import com.br.semesperanca.backendleads.domain.repository.RoleRepository
import com.br.semesperanca.backendleads.domain.repository.user.RealEstateAgentRepository
import com.br.semesperanca.backendleads.domain.repository.user.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class AuthService(
    private val repository: RealEstateAgentRepository,
    private val roleRepository: RoleRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
    private val userRepository: UserRepository,
) {
    fun login(request: LoginRequest): LoginResponse {
        val user: User =
            userRepository
                .findByEmail(request.email)
                .orElseThrow { RuntimeException("Usuário não encontrado") }

        if (!user.isActive) {
            throw RuntimeException("Usuário inativo")
        }

        if (!passwordEncoder.matches(request.password, user.getPassword())) {
            throw RuntimeException("Senha inválida")
        }

        val token = jwtService.generateToken(user.id!!, user.email, user.role?.name)
        return LoginResponse(token)
    }

    @Transactional
    fun createFullAdmin(request: CreateFullAdminRequest): RealEstateAgentResponse {
        if (userRepository.existsByEmail(request.email)) {
            throw RuntimeException("Email já cadastrado")
        }

        val role: Role =
            roleRepository
                .findByName("FULL_ADMIN")
                .orElseThrow { RuntimeException("Role não encontrada") }

        val agent =
            RealEstateAgent(
                creci =
                    "FULL-ADMIN-" +
                        UUID
                            .randomUUID()
                            .toString()
                            .substring(0, 8)
                            .uppercase(),
            ).apply {
                name = request.name
                email = request.email
                setPassword(passwordEncoder.encode(request.password))
                this.role = role
                isActive = true
            }

        return toResponse(repository.save(agent))
    }

    private fun toResponse(agent: RealEstateAgent): RealEstateAgentResponse =
        RealEstateAgentResponse(
            id = agent.id,
            name = agent.name,
            email = agent.email,
            creci = agent.creci,
            branchOfficeId = agent.branchOffice?.id,
            role = agent.role?.name,
            active = agent.isActive,
            createdAt = agent.createdAt,
        )

    @Transactional
    fun changePassword(
        request: ChangePasswordRequest,
        email: String,
    ) {
        if (request.newPassword != request.confirmPassword) {
            throw RuntimeException("Nova senha e confirmação não coincidem")
        }

        val user: User =
            userRepository
                .findByEmail(email)
                .orElseThrow { RuntimeException("Usuário não encontrado") }

        if (!passwordEncoder.matches(request.currentPassword, user.getPassword())) {
            throw RuntimeException("Senha atual inválida")
        }

        user.setPassword(passwordEncoder.encode(request.newPassword))
        userRepository.save(user)
    }

    @Transactional
    fun resetPassword(
        userId: UUID,
        request: ResetPasswordRequest,
    ) {
        if (request.newPassword != request.confirmPassword) {
            throw RuntimeException("Nova senha e confirmação não coincidem")
        }

        val user: User =
            userRepository
                .findById(userId)
                .orElseThrow { RuntimeException("Usuário não encontrado") }

        user.setPassword(passwordEncoder.encode(request.newPassword))
        userRepository.save(user)
    }
}
