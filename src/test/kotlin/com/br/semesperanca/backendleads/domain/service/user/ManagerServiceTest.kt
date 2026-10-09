package com.br.semesperanca.backendleads.domain.service.user

import com.br.semesperanca.backendleads.application.model.request.CreateManagerRequest
import com.br.semesperanca.backendleads.application.model.request.UpdateManagerRequest
import com.br.semesperanca.backendleads.application.model.request.UserFilterRequest
import com.br.semesperanca.backendleads.configuration.exception.EmailAlreadyExistsException
import com.br.semesperanca.backendleads.configuration.exception.NotFoundException
import com.br.semesperanca.backendleads.domain.model.BranchOffice
import com.br.semesperanca.backendleads.domain.model.user.Manager
import com.br.semesperanca.backendleads.domain.repository.BranchOfficeRepository
import com.br.semesperanca.backendleads.domain.repository.RoleRepository
import com.br.semesperanca.backendleads.domain.repository.user.ManagerRepository
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
class ManagerServiceTest {
    @Mock private lateinit var repository: ManagerRepository

    @Mock private lateinit var roleRepository: RoleRepository

    @Mock private lateinit var userRepository: UserRepository

    @Mock private lateinit var branchOfficeRepository: BranchOfficeRepository

    private val passwordEncoder: PasswordEncoder = BCryptPasswordEncoder()
    private lateinit var service: ManagerService

    private val pageable: Pageable = PageRequest.of(0, 20, Sort.by("name"))

    private val branchOfficeId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        service = ManagerService(repository, roleRepository, passwordEncoder, userRepository, branchOfficeRepository)
    }

    private fun hash(password: String): String = requireNotNull(passwordEncoder.encode(password))

    private fun saveGeneratingId() =
        whenever(repository.save(any<Manager>())).doAnswer { invocation ->
            @Suppress("UNCHECKED_CAST")
            (invocation.getArgument<Manager>(0)).apply { if (id == null) id = UUID.randomUUID() }
        }

    private fun stubRole() {
        whenever(roleRepository.findByName("MANAGER"))
            .thenReturn(Optional.of(TestFixtures.role("MANAGER")))
    }

    /**
     * A filial precisa de agência e de responsável: `branch_office` tem
     * `real_estate_agency_id` e `branch_owner_id` como `NOT NULL`, e é a agência
     * dela que o gerente herda.
     */
    private val filial = TestFixtures.branchOffice(id = branchOfficeId)

    private fun stubBranchOffice() {
        whenever(branchOfficeRepository.findById(branchOfficeId)).thenReturn(Optional.of(filial))
    }

    @Nested
    inner class Create {
        private val request = CreateManagerRequest("Bruno Lima", "bruno@example.com", "s3cret", branchOfficeId)

        @Test
        fun `cria o gerente com a role MANAGER`() {
            whenever(userRepository.existsByEmail("bruno@example.com")).thenReturn(false)
            stubRole()
            stubBranchOffice()
            saveGeneratingId()

            val response = service.create(request)

            assertThat(response.name).isEqualTo("Bruno Lima")
            assertThat(response.email).isEqualTo("bruno@example.com")
            assertThat(response.branchOfficeId).isEqualTo(branchOfficeId)
            assertThat(response.role).isEqualTo("MANAGER")
            assertThat(response.active).isTrue()
        }

        @Test
        fun `grava a senha com hash BCrypt, nunca em texto puro`() {
            whenever(userRepository.existsByEmail(any())).thenReturn(false)
            stubRole()
            stubBranchOffice()
            saveGeneratingId()

            service.create(request)

            val captor = argumentCaptor<Manager>()
            verify(repository).save(captor.capture())
            assertThat(captor.firstValue.getPassword()).isNotEqualTo("s3cret")
            assertThat(passwordEncoder.matches("s3cret", captor.firstValue.getPassword())).isTrue()
        }

        @Test
        fun `gerente nasce ativo`() {
            whenever(userRepository.existsByEmail(any())).thenReturn(false)
            stubRole()
            stubBranchOffice()
            saveGeneratingId()

            assertThat(service.create(request).active).isTrue()
        }

        /**
         * Filial de outra agencia nao resolve no `BranchOfficeRepository` escopado,
         * entao nem chega a ser gravado. E o `realEstateAgencyId` sai da filial,
         * nunca do corpo.
         */
        @Test
        fun `filial fora do escopo aborta antes do save`() {
            whenever(userRepository.existsByEmail(any())).thenReturn(false)
            stubRole()
            whenever(branchOfficeRepository.findById(branchOfficeId)).thenReturn(Optional.empty())

            assertThatThrownBy { service.create(request) }
                .isInstanceOf(NotFoundException::class.java)
                .hasMessage("Filial não encontrada")

            verify(repository, never()).save(any<Manager>())
        }

        @Test
        fun `a agencia do gerente e derivada da filial`() {
            whenever(userRepository.existsByEmail(any())).thenReturn(false)
            stubRole()
            stubBranchOffice()
            saveGeneratingId()

            service.create(request)

            val captor = argumentCaptor<Manager>()
            verify(repository).save(captor.capture())
            assertThat(captor.firstValue.realEstateAgencyId).isEqualTo(filial.realEstateAgencyId)
        }

        @Test
        fun `e-mail duplicado vira EmailAlreadyExistsException, traduzida em 409`() {
            whenever(userRepository.existsByEmail("bruno@example.com")).thenReturn(true)

            assertThatThrownBy { service.create(request) }
                .isInstanceOf(EmailAlreadyExistsException::class.java)
                .hasMessageContaining("bruno@example.com")

            verify(repository, never()).save(any())
            verify(roleRepository, never()).findByName(any())
        }

        @Test
        fun `role MANAGER ausente no banco aborta a criacao`() {
            whenever(userRepository.existsByEmail(any())).thenReturn(false)
            whenever(roleRepository.findByName("MANAGER")).thenReturn(Optional.empty())

            assertThatThrownBy { service.create(request) }
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("Role MANAGER não encontrada — a V002 semeia a tabela role")

            verify(repository, never()).save(any())
        }
    }

    @Nested
    inner class FindById {
        @Test
        fun `mapeia a entidade para o response`() {
            val manager = TestFixtures.manager()
            whenever(repository.findById(manager.id!!)).thenReturn(Optional.of(manager))

            val response = service.findById(manager.id!!)

            assertThat(response.id).isEqualTo(manager.id)
            assertThat(response.name).isEqualTo("Bruno Lima")
            assertThat(response.email).isEqualTo("bruno@example.com")
            assertThat(response.branchOfficeId).isEqualTo(manager.branchOffice?.id)
            assertThat(response.role).isEqualTo("MANAGER")
            assertThat(response.active).isTrue()
            assertThat(response.createdAt).isEqualTo(manager.createdAt)
        }

        @Test
        fun `gerente sem role devolve role nula em vez de estourar`() {
            val manager = TestFixtures.manager(role = null)
            whenever(repository.findById(manager.id!!)).thenReturn(Optional.of(manager))

            assertThat(service.findById(manager.id!!).role).isNull()
        }

        @Test
        fun `id inexistente vira RuntimeException com mensagem de dominio`() {
            val id = UUID.randomUUID()
            whenever(repository.findById(id)).thenReturn(Optional.empty())

            assertThatThrownBy { service.findById(id) }
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("Gerente não encontrado")
        }
    }

    @Nested
    inner class FindAll {
        private fun traceOf(filter: UserFilterRequest): SpecProbe.Trace {
            whenever(repository.findAll(any<Specification<Manager>>(), eq(pageable)))
                .thenReturn(PageImpl(emptyList()))

            service.findAll(filter, pageable)

            val specCaptor = argumentCaptor<Specification<Manager>>()
            verify(repository, atLeastOnce()).findAll(specCaptor.capture(), eq(pageable))
            return SpecProbe.traceOf(specCaptor.allValues.last())
        }

        @Test
        fun `devolve a pagina ja mapeada para response`() {
            whenever(repository.findAll(any<Specification<Manager>>(), eq(pageable)))
                .thenReturn(PageImpl(listOf(TestFixtures.manager())))

            val page = service.findAll(UserFilterRequest(null, null), pageable)

            assertThat(page.content).hasSize(1)
            assertThat(page.content.first().name).isEqualTo("Bruno Lima")
        }

        @Test
        fun `repassa o Pageable recebido sem reordenar`() {
            whenever(repository.findAll(any<Specification<Manager>>(), eq(pageable)))
                .thenReturn(PageImpl(emptyList()))

            service.findAll(UserFilterRequest(null, null), pageable)

            verify(repository).findAll(any<Specification<Manager>>(), eq(pageable))
        }

        @Test
        fun `filtro vazio nao restringe nada`() {
            assertThat(traceOf(UserFilterRequest(null, null)).andValues.flatten()).isEmpty()
        }

        @Test
        fun `search vazio ou em branco nao vira filtro de like`() {
            assertThat(traceOf(UserFilterRequest(null, "")).andValues.flatten()).isEmpty()
            assertThat(traceOf(UserFilterRequest(null, "   ")).andValues.flatten()).isEmpty()
        }

        @Test
        fun `filtra por active, incluindo o false explicito`() {
            assertThat(traceOf(UserFilterRequest(true, null)).operations).contains("equal(true)")
            assertThat(traceOf(UserFilterRequest(false, null)).operations).contains("equal(false)")
        }

        @Test
        fun `search vira um like com curinga sobre nome e e-mail`() {
            val trace = traceOf(UserFilterRequest(null, "BRUNO"))

            assertThat(trace.operations.filter { it.startsWith("like(") })
                .containsExactly("like(%bruno%)", "like(%bruno%)")
        }

        @Test
        fun `active e search juntos viram duas restricoes`() {
            val trace = traceOf(UserFilterRequest(true, "bruno"))

            assertThat(trace.andValues.flatten()).hasSize(2)
        }
    }

    @Nested
    inner class Update {
        private val request = UpdateManagerRequest("Bruno A. Lima", "bruno.novo@example.com", branchOfficeId)

        @Test
        fun `sobrescreve nome, e-mail e filial`() {
            val manager = TestFixtures.manager()
            whenever(repository.findById(manager.id!!)).thenReturn(Optional.of(manager))
            stubBranchOffice()
            saveGeneratingId()

            val response = service.update(manager.id!!, request)

            assertThat(response.name).isEqualTo("Bruno A. Lima")
            assertThat(response.email).isEqualTo("bruno.novo@example.com")
            assertThat(response.branchOfficeId).isEqualTo(branchOfficeId)
        }

        @Test
        fun `update nao mexe em active, role nem senha`() {
            val manager = TestFixtures.manager(isActive = false, password = hash("original"))
            whenever(repository.findById(manager.id!!)).thenReturn(Optional.of(manager))
            stubBranchOffice()
            saveGeneratingId()

            val response = service.update(manager.id!!, request)

            assertThat(response.active).isFalse()
            assertThat(response.role).isEqualTo("MANAGER")
            val captor = argumentCaptor<Manager>()
            verify(repository).save(captor.capture())
            assertThat(passwordEncoder.matches("original", captor.firstValue.getPassword())).isTrue()
        }

        @Test
        fun `id inexistente nao salva nada`() {
            val id = UUID.randomUUID()
            whenever(repository.findById(id)).thenReturn(Optional.empty())

            assertThatThrownBy { service.update(id, request) }
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("Gerente não encontrado")

            verify(repository, never()).save(any())
        }
    }

    @Nested
    inner class ToggleActive {
        @Test
        fun `desativa um gerente ativo`() {
            val manager = TestFixtures.manager(isActive = true)
            whenever(repository.findById(manager.id!!)).thenReturn(Optional.of(manager))
            saveGeneratingId()

            assertThat(service.toggleActive(manager.id!!).active).isFalse()
        }

        @Test
        fun `reativa um gerente inativo`() {
            val manager = TestFixtures.manager(isActive = false)
            whenever(repository.findById(manager.id!!)).thenReturn(Optional.of(manager))
            saveGeneratingId()

            assertThat(service.toggleActive(manager.id!!).active).isTrue()
        }

        @Test
        fun `id inexistente nao salva nada`() {
            val id = UUID.randomUUID()
            whenever(repository.findById(id)).thenReturn(Optional.empty())

            assertThatThrownBy { service.toggleActive(id) }
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("Gerente não encontrado")

            verify(repository, never()).save(any())
        }
    }

    @Nested
    inner class Delete {
        @Test
        fun `remove o gerente encontrado`() {
            val manager = TestFixtures.manager()
            whenever(repository.findById(manager.id!!)).thenReturn(Optional.of(manager))

            service.delete(manager.id!!)

            verify(repository).delete(manager)
        }

        @Test
        fun `id inexistente nao deleta nada`() {
            val id = UUID.randomUUID()
            whenever(repository.findById(id)).thenReturn(Optional.empty())

            assertThatThrownBy { service.delete(id) }
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("Gerente não encontrado")

            verify(repository, never()).delete(any<Manager>())
        }
    }
}
