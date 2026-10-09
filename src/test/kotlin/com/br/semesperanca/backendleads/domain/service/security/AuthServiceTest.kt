package com.br.semesperanca.backendleads.domain.service.security

import com.br.semesperanca.backendleads.application.model.request.ChangePasswordRequest
import com.br.semesperanca.backendleads.application.model.request.CreateFullAdminRequest
import com.br.semesperanca.backendleads.application.model.request.LoginRequest
import com.br.semesperanca.backendleads.application.model.request.ResetPasswordRequest
import com.br.semesperanca.backendleads.domain.model.user.RealEstateAgent
import com.br.semesperanca.backendleads.domain.repository.RoleRepository
import com.br.semesperanca.backendleads.domain.repository.user.RealEstateAgentRepository
import com.br.semesperanca.backendleads.domain.repository.user.UserRepository
import com.br.semesperanca.backendleads.support.TestFixtures
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTest {
    @Mock private lateinit var agentRepository: RealEstateAgentRepository

    @Mock private lateinit var roleRepository: RoleRepository

    @Mock private lateinit var userRepository: UserRepository

    @Mock private lateinit var jwtService: JwtService

    private val passwordEncoder: PasswordEncoder = BCryptPasswordEncoder()

    /**
     * `PasswordEncoder.encode` e @Nullable no Spring Security 7, mas BCrypt
     * nunca devolve null. O helper afasta o aviso de nullability sem espalhar
     * `!!` pelo arquivo.
     */
    private fun hash(password: String): String = requireNotNull(passwordEncoder.encode(password))

    private lateinit var service: AuthService

    private val userId: UUID = UUID.fromString("33333333-3333-3333-3333-333333333333")

    @BeforeEach
    fun setUp() {
        service = AuthService(agentRepository, roleRepository, passwordEncoder, jwtService, userRepository)
    }

    @Nested
    inner class Login {
        @Test
        fun `devolve o token do usuario quando a senha confere`() {
            val user = TestFixtures.realEstateAgent(id = userId, email = "ana@example.com")
            user.setPassword(hash("s3cret"))
            whenever(userRepository.findByEmail("ana@example.com")).thenReturn(Optional.of(user))
            whenever(jwtService.generateToken(any(), any(), anyOrNull())).thenReturn("token-jwt")

            val response = service.login(LoginRequest("ana@example.com", "s3cret"))

            assertThat(response.token).isEqualTo("token-jwt")
        }

        @Test
        fun `passa o papel do usuario para o gerador de token`() {
            val user =
                TestFixtures.realEstateAgent(
                    id = userId,
                    email = "ana@example.com",
                    role = TestFixtures.role("MANAGER"),
                )
            val userPassword = hash("s3cret")
            user.setPassword(userPassword)
            whenever(userRepository.findByEmail("ana@example.com")).thenReturn(Optional.of(user))
            whenever(jwtService.generateToken(any(), any(), any())).thenReturn("t")

            service.login(LoginRequest("ana@example.com", "s3cret"))

            verify(jwtService).generateToken(userId, "ana@example.com", "MANAGER")
        }

        @Test
        fun `rejeita e-mail inexistente`() {
            whenever(userRepository.findByEmail("ninguem@example.com")).thenReturn(Optional.empty())

            assertThatThrownBy { service.login(LoginRequest("ninguem@example.com", "s3cret")) }
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("Usuário não encontrado")
        }

        @Test
        fun `rejeita usuario inativo antes de conferir a senha`() {
            val user = TestFixtures.realEstateAgent(isActive = false)
            whenever(userRepository.findByEmail("inativo@example.com")).thenReturn(Optional.of(user))

            assertThatThrownBy { service.login(LoginRequest("inativo@example.com", "s3cret")) }
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("Usuário inativo")

            verify(jwtService, never()).generateToken(any(), any(), any())
        }

        @Test
        fun `rejeita senha incorreta`() {
            val user = TestFixtures.realEstateAgent()
            user.setPassword(hash("senha-correta"))
            whenever(userRepository.findByEmail("ana@example.com")).thenReturn(Optional.of(user))

            assertThatThrownBy { service.login(LoginRequest("ana@example.com", "senha-errada")) }
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("Senha inválida")
        }
    }

    @Nested
    inner class CreateFullAdmin {
        private val request = CreateFullAdminRequest("Administrador", "root@example.com", "s3cret")

        @Test
        fun `cria o agente com role FULL_ADMIN e CRECI gerado`() {
            whenever(userRepository.existsByEmail("root@example.com")).thenReturn(false)
            whenever(roleRepository.findByName("FULL_ADMIN"))
                .thenReturn(Optional.of(TestFixtures.role("FULL_ADMIN")))
            whenever(agentRepository.save(any())).thenAnswer { it.getArgument(0) }

            val response = service.createFullAdmin(request)

            assertThat(response.name).isEqualTo("Administrador")
            assertThat(response.email).isEqualTo("root@example.com")
            assertThat(response.role).isEqualTo("FULL_ADMIN")
            assertThat(response.active).isTrue()
            assertThat(response.creci).startsWith("FULL-ADMIN-").hasSize(19)
        }

        @Test
        fun `gera um CRECI diferente a cada chamada`() {
            whenever(userRepository.existsByEmail(any())).thenReturn(false)
            whenever(roleRepository.findByName("FULL_ADMIN"))
                .thenReturn(Optional.of(TestFixtures.role("FULL_ADMIN")))
            whenever(agentRepository.save(any())).thenAnswer { it.getArgument(0) }

            val first = service.createFullAdmin(request).creci
            val second = service.createFullAdmin(request).creci

            assertThat(first).isNotEqualTo(second)
        }

        @Test
        fun `grava a senha com hash BCrypt, nunca em texto puro`() {
            whenever(userRepository.existsByEmail(any())).thenReturn(false)
            whenever(roleRepository.findByName(any()))
                .thenReturn(Optional.of(TestFixtures.role("FULL_ADMIN")))
            whenever(agentRepository.save(any())).thenAnswer { it.getArgument(0) }

            service.createFullAdmin(request)

            val captor = ArgumentCaptor.forClass(RealEstateAgent::class.java)
            verify(agentRepository).save(captor.capture())
            val saved = captor.value
            assertThat(saved.getPassword()).isNotEqualTo("s3cret")
            assertThat(passwordEncoder.matches("s3cret", saved.getPassword())).isTrue()
        }

        @Test
        fun `rejeita e-mail ja cadastrado`() {
            whenever(userRepository.existsByEmail("root@example.com")).thenReturn(true)

            assertThatThrownBy { service.createFullAdmin(request) }
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("Email já cadastrado")

            verify(agentRepository, never()).save(any())
        }

        @Test
        fun `rejeita quando a role FULL_ADMIN nao existe no banco`() {
            whenever(userRepository.existsByEmail(any())).thenReturn(false)
            whenever(roleRepository.findByName("FULL_ADMIN")).thenReturn(Optional.empty())

            assertThatThrownBy { service.createFullAdmin(request) }
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("Role não encontrada")
        }
    }

    @Nested
    inner class ChangePassword {
        private val request = ChangePasswordRequest("atual123", "nova123", "nova123")

        @Test
        fun `troca a senha do usuario autenticado`() {
            val user = TestFixtures.realEstateAgent()
            user.setPassword(hash("atual123"))
            whenever(userRepository.findByEmail("ana@example.com")).thenReturn(Optional.of(user))
            whenever(userRepository.save(any())).thenAnswer { it.getArgument(0) }

            service.changePassword(request, "ana@example.com")

            val captor = ArgumentCaptor.forClass(com.br.semesperanca.backendleads.domain.model.user.User::class.java)
            verify(userRepository).save(captor.capture())
            assertThat(passwordEncoder.matches("nova123", captor.value.getPassword())).isTrue()
        }

        @Test
        fun `rejeita confirmacao divergente sem tocar no banco`() {
            val request = ChangePasswordRequest("atual123", "nova123", "outra123")

            assertThatThrownBy { service.changePassword(request, "ana@example.com") }
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("Nova senha e confirmação não coincidem")

            verify(userRepository, never()).findByEmail(any())
            verify(userRepository, never()).save(any())
        }

        @Test
        fun `rejeita usuario inexistente`() {
            val request = ChangePasswordRequest("atual123", "nova123", "nova123")
            whenever(userRepository.findByEmail(any())).thenReturn(Optional.empty())

            assertThatThrownBy { service.changePassword(request, "fantasma@example.com") }
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("Usuário não encontrado")
        }

        @Test
        fun `rejeita senha atual incorreta`() {
            val user = TestFixtures.realEstateAgent()
            user.setPassword(hash("outra-senha"))
            whenever(userRepository.findByEmail(any())).thenReturn(Optional.of(user))

            assertThatThrownBy { service.changePassword(request, "ana@example.com") }
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("Senha atual inválida")

            verify(userRepository, never()).save(any())
        }
    }

    @Nested
    inner class ResetPassword {
        private val request = ResetPasswordRequest("nova123", "nova123")

        @Test
        fun `redefine a senha pelo id sem conferir a senha atual`() {
            val user = TestFixtures.realEstateAgent()
            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
            whenever(userRepository.save(any())).thenAnswer { it.getArgument(0) }

            service.resetPassword(userId, request)

            val captor = ArgumentCaptor.forClass(com.br.semesperanca.backendleads.domain.model.user.User::class.java)
            verify(userRepository).save(captor.capture())
            assertThat(passwordEncoder.matches("nova123", captor.value.getPassword())).isTrue()
        }

        @Test
        fun `rejeita confirmacao divergente`() {
            val request = ResetPasswordRequest("nova123", "diferente")

            assertThatThrownBy { service.resetPassword(userId, request) }
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("Nova senha e confirmação não coincidem")

            verify(userRepository, never()).save(any())
        }

        @Test
        fun `rejeita usuario inexistente`() {
            whenever(userRepository.findById(userId)).thenReturn(Optional.empty())

            assertThatThrownBy { service.resetPassword(userId, request) }
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("Usuário não encontrado")
        }

        @Test
        fun `funciona tambem para um usuario ja inativo, que nao consegue mais fazer login`() {
            val user = TestFixtures.realEstateAgent(isActive = false)
            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
            whenever(userRepository.save(any())).doAnswer { it.getArgument(0) }

            service.resetPassword(userId, request)

            verify(userRepository).save(any())
        }
    }
}
