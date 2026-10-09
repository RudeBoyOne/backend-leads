package com.br.semesperanca.backendleads.domain.service.user

import com.br.semesperanca.backendleads.application.model.request.CreateRealEstateAgentRequest
import com.br.semesperanca.backendleads.application.model.request.UpdateRealEstateAgentRequest
import com.br.semesperanca.backendleads.application.model.request.UserFilterRequest
import com.br.semesperanca.backendleads.configuration.exception.EmailAlreadyExistsException
import com.br.semesperanca.backendleads.domain.model.BranchOffice
import com.br.semesperanca.backendleads.domain.model.user.RealEstateAgent
import com.br.semesperanca.backendleads.domain.repository.BranchOfficeRepository
import com.br.semesperanca.backendleads.domain.repository.RoleRepository
import com.br.semesperanca.backendleads.domain.repository.user.RealEstateAgentRepository
import com.br.semesperanca.backendleads.domain.repository.user.UserRepository
import com.br.semesperanca.backendleads.support.SpecProbe
import com.br.semesperanca.backendleads.support.TestFixtures
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class RealEstateAgentServiceTest {
    @Mock private lateinit var repository: RealEstateAgentRepository

    @Mock private lateinit var roleRepository: RoleRepository

    @Mock private lateinit var userRepository: UserRepository

    @Mock private lateinit var branchOfficeRepository: BranchOfficeRepository

    private val passwordEncoder: PasswordEncoder = BCryptPasswordEncoder()
    private lateinit var service: RealEstateAgentService

    private val pageable: Pageable = PageRequest.of(0, 20, Sort.by("name"))

    private val branchOfficeId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        service = RealEstateAgentService(repository, roleRepository, passwordEncoder, userRepository, branchOfficeRepository)
    }

    private fun hash(password: String): String = requireNotNull(passwordEncoder.encode(password))

    private fun saveGeneratingId() =
        whenever(repository.save(any<RealEstateAgent>())).doAnswer { invocation ->
            @Suppress("UNCHECKED_CAST")
            (invocation.getArgument<RealEstateAgent>(0)).apply { if (id == null) id = UUID.randomUUID() }
        }

    private fun stubRole() {
        whenever(roleRepository.findByName("REAL_ESTATE_AGENT"))
            .thenReturn(Optional.of(TestFixtures.role("REAL_ESTATE_AGENT")))
    }

    /** `create`/`update` resolvem a filial informada no request antes de salvar. */
    private fun stubBranchOffice(id: UUID = branchOfficeId) {
        whenever(branchOfficeRepository.findById(id))
            .thenReturn(Optional.of(BranchOffice(name = "Filial Centro")))
    }

    @Nested
    inner class Create {
        private val request =
            CreateRealEstateAgentRequest("Ana Paula", "ana@example.com", "s3cret", "CRECI-123", branchOfficeId)

        @Test
        fun `cria o corretor com a role REAL_ESTATE_AGENT`() {
            whenever(userRepository.existsByEmail("ana@example.com")).thenReturn(false)
            whenever(repository.existsByCreci("CRECI-123")).thenReturn(false)
            stubRole()
            stubBranchOffice()
            saveGeneratingId()

            val response = service.create(request)

            assertThat(response.name).isEqualTo("Ana Paula")
            assertThat(response.email).isEqualTo("ana@example.com")
            assertThat(response.creci).isEqualTo("CRECI-123")
            assertThat(response.role).isEqualTo("REAL_ESTATE_AGENT")
            assertThat(response.active).isTrue()
        }

        @Test
        fun `grava a senha com hash BCrypt, nunca em texto puro`() {
            whenever(userRepository.existsByEmail(any())).thenReturn(false)
            whenever(repository.existsByCreci(any())).thenReturn(false)
            stubRole()
            stubBranchOffice()
            saveGeneratingId()

            service.create(request)

            val captor = argumentCaptor<RealEstateAgent>()
            verify(repository).save(captor.capture())
            assertThat(captor.firstValue.getPassword()).isNotEqualTo("s3cret")
            assertThat(passwordEncoder.matches("s3cret", captor.firstValue.getPassword())).isTrue()
        }

        @Test
        fun `e-mail duplicado aborta antes de checar o CRECI`() {
            whenever(userRepository.existsByEmail("ana@example.com")).thenReturn(true)

            assertThatThrownBy { service.create(request) }
                .isInstanceOf(EmailAlreadyExistsException::class.java)
                .hasMessageContaining("ana@example.com")

            verify(repository, never()).existsByCreci(any())
            verify(repository, never()).save(any())
        }

        @Test
        fun `CRECI duplicado aborta com mensagem propria`() {
            whenever(userRepository.existsByEmail(any())).thenReturn(false)
            whenever(repository.existsByCreci("CRECI-123")).thenReturn(true)

            assertThatThrownBy { service.create(request) }
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("CRECI já cadastrado")

            verify(repository, never()).save(any())
            verify(roleRepository, never()).findByName(any())
        }

        @Test
        fun `role REAL_ESTATE_AGENT ausente no banco aborta a criacao`() {
            whenever(userRepository.existsByEmail(any())).thenReturn(false)
            whenever(repository.existsByCreci(any())).thenReturn(false)
            whenever(roleRepository.findByName("REAL_ESTATE_AGENT")).thenReturn(Optional.empty())

            assertThatThrownBy { service.create(request) }
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("Role REAL_ESTATE_AGENT não encontrada — a V002 semeia a tabela role")

            verify(repository, never()).save(any())
        }
    }

    @Nested
    inner class FindById {
        @Test
        fun `mapeia a entidade para o response`() {
            val agent = TestFixtures.realEstateAgent()
            whenever(repository.findById(agent.id!!)).thenReturn(Optional.of(agent))

            val response = service.findById(agent.id!!)

            assertThat(response.id).isEqualTo(agent.id)
            assertThat(response.name).isEqualTo("Ana Paula")
            assertThat(response.email).isEqualTo("ana@example.com")
            assertThat(response.creci).isEqualTo("CRECI-123")
            assertThat(response.role).isEqualTo("REAL_ESTATE_AGENT")
            assertThat(response.active).isTrue()
            assertThat(response.createdAt).isEqualTo(agent.createdAt)
        }

        @Test
        fun `corretor sem role devolve role nula em vez de estourar`() {
            val agent = TestFixtures.realEstateAgent(role = null)
            whenever(repository.findById(agent.id!!)).thenReturn(Optional.of(agent))

            assertThat(service.findById(agent.id!!).role).isNull()
        }

        @Test
        fun `id inexistente vira RuntimeException com mensagem de dominio`() {
            val id = UUID.randomUUID()
            whenever(repository.findById(id)).thenReturn(Optional.empty())

            assertThatThrownBy { service.findById(id) }
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("Corretor não encontrado")
        }
    }

    @Nested
    inner class FindAll {
        private fun traceOf(filter: UserFilterRequest): SpecProbe.Trace {
            whenever(repository.findAll(any<Specification<RealEstateAgent>>(), eq(pageable)))
                .thenReturn(PageImpl(emptyList()))

            service.findAll(filter, pageable)

            val specCaptor = argumentCaptor<Specification<RealEstateAgent>>()
            verify(repository, atLeastOnce()).findAll(specCaptor.capture(), eq(pageable))
            return SpecProbe.traceOf(specCaptor.allValues.last())
        }

        @Test
        fun `devolve a pagina ja mapeada para response`() {
            whenever(repository.findAll(any<Specification<RealEstateAgent>>(), eq(pageable)))
                .thenReturn(PageImpl(listOf(TestFixtures.realEstateAgent())))

            val page = service.findAll(UserFilterRequest(null, null), pageable)

            assertThat(page.content).hasSize(1)
            assertThat(page.content.first().creci).isEqualTo("CRECI-123")
        }

        @Test
        fun `repassa o Pageable recebido sem reordenar`() {
            whenever(repository.findAll(any<Specification<RealEstateAgent>>(), eq(pageable)))
                .thenReturn(PageImpl(emptyList()))

            service.findAll(UserFilterRequest(null, null), pageable)

            verify(repository).findAll(any<Specification<RealEstateAgent>>(), eq(pageable))
        }

        @Test
        fun `filtro vazio nao restringe nada`() {
            assertThat(traceOf(UserFilterRequest(null, null)).andValues.flatten()).isEmpty()
        }

        @Test
        fun `search vazio ou em branco nao vira filtro de like`() {
            assertThat(traceOf(UserFilterRequest(null, "")).andValues.flatten()).isEmpty()
            assertThat(traceOf(UserFilterRequest(null, "  ")).andValues.flatten()).isEmpty()
        }

        @Test
        fun `filtra por active, incluindo o false explicito`() {
            assertThat(traceOf(UserFilterRequest(true, null)).operations).contains("equal(true)")
            assertThat(traceOf(UserFilterRequest(false, null)).operations).contains("equal(false)")
        }

        @Test
        fun `search vira um like com curinga sobre nome e e-mail`() {
            val trace = traceOf(UserFilterRequest(null, "ANA"))

            assertThat(trace.operations.filter { it.startsWith("like(") })
                .containsExactly("like(%ana%)", "like(%ana%)")
        }

        @Test
        fun `active e search juntos viram duas restricoes`() {
            val trace = traceOf(UserFilterRequest(true, "ana"))

            assertThat(trace.andValues.flatten()).hasSize(2)
        }
    }

    @Nested
    inner class Update {
        private val request = UpdateRealEstateAgentRequest("Ana P. Souza", "ana.nova@example.com", "CRECI-999", branchOfficeId)

        @Test
        fun `sobrescreve nome, e-mail e CRECI`() {
            val agent = TestFixtures.realEstateAgent()
            whenever(repository.findById(agent.id!!)).thenReturn(Optional.of(agent))
            stubBranchOffice()
            saveGeneratingId()

            val response = service.update(agent.id!!, request)

            assertThat(response.name).isEqualTo("Ana P. Souza")
            assertThat(response.email).isEqualTo("ana.nova@example.com")
            assertThat(response.creci).isEqualTo("CRECI-999")
        }

        @Test
        fun `update nao mexe em active, role nem senha`() {
            val agent = TestFixtures.realEstateAgent(isActive = false, password = hash("original"))
            whenever(repository.findById(agent.id!!)).thenReturn(Optional.of(agent))
            stubBranchOffice()
            saveGeneratingId()

            val response = service.update(agent.id!!, request)

            assertThat(response.active).isFalse()
            assertThat(response.role).isEqualTo("REAL_ESTATE_AGENT")
            val captor = argumentCaptor<RealEstateAgent>()
            verify(repository).save(captor.capture())
            assertThat(passwordEncoder.matches("original", captor.firstValue.getPassword())).isTrue()
        }

        @Test
        fun `id inexistente nao salva nada`() {
            val id = UUID.randomUUID()
            whenever(repository.findById(id)).thenReturn(Optional.empty())

            assertThatThrownBy { service.update(id, request) }
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("Corretor não encontrado")

            verify(repository, never()).save(any())
        }
    }

    @Nested
    inner class ToggleActive {
        @Test
        fun `desativa um corretor ativo`() {
            val agent = TestFixtures.realEstateAgent(isActive = true)
            whenever(repository.findById(agent.id!!)).thenReturn(Optional.of(agent))
            saveGeneratingId()

            assertThat(service.toggleActive(agent.id!!).active).isFalse()
        }

        @Test
        fun `reativa um corretor inativo`() {
            val agent = TestFixtures.realEstateAgent(isActive = false)
            whenever(repository.findById(agent.id!!)).thenReturn(Optional.of(agent))
            saveGeneratingId()

            assertThat(service.toggleActive(agent.id!!).active).isTrue()
        }

        @Test
        fun `id inexistente nao salva nada`() {
            val id = UUID.randomUUID()
            whenever(repository.findById(id)).thenReturn(Optional.empty())

            assertThatThrownBy { service.toggleActive(id) }
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("Corretor não encontrado")

            verify(repository, never()).save(any())
        }
    }

    @Nested
    inner class Delete {
        @Test
        fun `remove o corretor encontrado`() {
            val agent = TestFixtures.realEstateAgent()
            whenever(repository.findById(agent.id!!)).thenReturn(Optional.of(agent))

            service.delete(agent.id!!)

            verify(repository).delete(agent)
        }

        @Test
        fun `id inexistente nao deleta nada`() {
            val id = UUID.randomUUID()
            whenever(repository.findById(id)).thenReturn(Optional.empty())

            assertThatThrownBy { service.delete(id) }
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("Corretor não encontrado")

            verify(repository, never()).delete(any<RealEstateAgent>())
        }
    }
}
