package com.br.semesperanca.backendleads.domain.service

import com.br.semesperanca.backendleads.application.model.request.CreateBranchOwnerRequest
import com.br.semesperanca.backendleads.application.model.request.UpdateBranchOwnerRequest
import com.br.semesperanca.backendleads.configuration.exception.EmailAlreadyExistsException
import com.br.semesperanca.backendleads.configuration.exception.NotFoundException
import com.br.semesperanca.backendleads.domain.model.BranchOwner
import com.br.semesperanca.backendleads.domain.model.Role
import com.br.semesperanca.backendleads.domain.repository.BranchOfficeRepository
import com.br.semesperanca.backendleads.domain.repository.BranchOwnerRepository
import com.br.semesperanca.backendleads.domain.repository.RoleRepository
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
import org.mockito.Mockito.lenient
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
class BranchOwnerServiceTest {
    @Mock private lateinit var repository: BranchOwnerRepository

    @Mock private lateinit var userRepository: UserRepository

    @Mock private lateinit var roleRepository: RoleRepository

    @Mock private lateinit var branchOfficeRepository: BranchOfficeRepository

    private val passwordEncoder: PasswordEncoder = BCryptPasswordEncoder()

    private lateinit var service: BranchOwnerService

    private val pageable: Pageable = PageRequest.of(0, 20, Sort.by("name"))

    private val adminRole = TestFixtures.role(Role.ADMIN)

    @BeforeEach
    fun setUp() {
        service = BranchOwnerService(repository, userRepository, roleRepository, branchOfficeRepository, passwordEncoder)
        // `lenient` porque o setup abaixo e compartilhado por testes que nao
        // criam responsavel; o `MockitoExtension`so reclamaria de stub sem uso.
        lenient().`when`(roleRepository.findByName(Role.ADMIN)).thenReturn(Optional.of(adminRole))
        lenient().`when`(userRepository.existsByEmail(any())).thenReturn(false)
    }

    private fun saveGeneratingId() =
        whenever(repository.save(any<BranchOwner>())).doAnswer { invocation ->
            @Suppress("UNCHECKED_CAST")
            (invocation.getArgument<BranchOwner>(0)).apply { if (id == null) id = UUID.randomUUID() }
        }

    private fun createRequest() =
        CreateBranchOwnerRequest(
            realEstateAgencyId = TestFixtures.AGENCY_ID,
            name = "Carlos Souza",
            email = "carlos@example.com",
            password = "senha-secreta",
            phone = "11912345678",
        )

    @Nested
    inner class Create {
        @Test
        fun `persiste o responsavel com todos os campos do request`() {
            saveGeneratingId()

            val response = service.create(createRequest())

            val captor = argumentCaptor<BranchOwner>()
            verify(repository).save(captor.capture())
            assertThat(captor.firstValue.realEstateAgencyId).isEqualTo(TestFixtures.AGENCY_ID)
            assertThat(captor.firstValue.name).isEqualTo("Carlos Souza")
            assertThat(captor.firstValue.email).isEqualTo("carlos@example.com")
            assertThat(captor.firstValue.phone).isEqualTo("11912345678")
            assertThat(response.id).isNotNull()
        }

        /**
         * O responsavel e um `User` que autentica, entao a senha entra hasheada
         * com o encoder da aplicacao. Guardar em claro — ou com o encoder padrao
         * do `User` — deixaria o login do branch owner quebrado ou a credencial em
         * texto no banco.
         */
        @Test
        fun `a senha e persistida hasheada e nunca em claro`() {
            saveGeneratingId()

            service.create(createRequest())

            val captor = argumentCaptor<BranchOwner>()
            verify(repository).save(captor.capture())
            assertThat(captor.firstValue.password).isNotEqualTo("senha-secreta")
            assertThat(passwordEncoder.matches("senha-secreta", captor.firstValue.password)).isTrue()
        }

        @Test
        fun `responsavel nasce com a role ADMIN, que e o que significa branch owner`() {
            saveGeneratingId()

            service.create(createRequest())

            val captor = argumentCaptor<BranchOwner>()
            verify(repository).save(captor.capture())
            assertThat(captor.firstValue.role?.name).isEqualTo(Role.ADMIN)
        }

        /**
         * O responsavel supervisiona N filiais, entao nao tem sede. Deixar
         * `branchOffice` nulo e o que mantem a arvore livre de ambiguidade: se ele
         * tivesse uma, passaria a aparecer em dois lugares do escopo.
         */
        @Test
        fun `responsavel nasce sem lotacao, porque administra varias filiais`() {
            saveGeneratingId()

            service.create(createRequest())

            val captor = argumentCaptor<BranchOwner>()
            verify(repository).save(captor.capture())
            assertThat(captor.firstValue.branchOffice).isNull()
        }

        @Test
        fun `responsavel nasce ativo`() {
            saveGeneratingId()

            val response = service.create(createRequest())

            assertThat(response.active).isTrue()
        }

        @Test
        fun `telefone e opcional`() {
            saveGeneratingId()

            val response = service.create(createRequest().copy(phone = null))

            assertThat(response.phone).isNull()
        }

        @Test
        fun `email ja cadastrado e recusado antes de tocar no repositorio`() {
            whenever(userRepository.existsByEmail("carlos@example.com")).thenReturn(true)

            assertThatThrownBy { service.create(createRequest()) }
                .isInstanceOf(EmailAlreadyExistsException::class.java)

            verify(repository, never()).save(any<BranchOwner>())
        }

        @Test
        fun `sem a role ADMIN no banco a falha e explicita`() {
            whenever(roleRepository.findByName(Role.ADMIN)).thenReturn(Optional.empty())

            assertThatThrownBy { service.create(createRequest()) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("ADMIN")
        }
    }

    @Nested
    inner class FindById {
        @Test
        fun `mapeia a entidade para o response`() {
            val owner = TestFixtures.branchOwner()
            whenever(repository.findById(owner.id!!)).thenReturn(Optional.of(owner))

            val response = service.findById(owner.id!!)

            assertThat(response.id).isEqualTo(owner.id)
            assertThat(response.realEstateAgencyId).isEqualTo(TestFixtures.AGENCY_ID)
            assertThat(response.name).isEqualTo("Carlos Souza")
            assertThat(response.email).isEqualTo("carlos@example.com")
            assertThat(response.phone).isEqualTo("11912345678")
            assertThat(response.role).isEqualTo(Role.ADMIN)
            assertThat(response.active).isTrue()
            assertThat(response.createdAt).isEqualTo(owner.createdAt)
            assertThat(response.updatedAt).isEqualTo(owner.updatedAt)
        }

        /**
         * `NotFoundException` vira 404. O `findById` escopado devolve vazio tanto
         * para id inexistente quanto para id de outro branch owner, e os dois casos
         * precisam responder igual — 403 confirmaria a existencia do registro.
         */
        @Test
        fun `id fora do escopo vira NotFoundException`() {
            val id = UUID.randomUUID()
            whenever(repository.findById(id)).thenReturn(Optional.empty())

            assertThatThrownBy { service.findById(id) }
                .isInstanceOf(NotFoundException::class.java)
                .hasMessage("Responsável não encontrado")
        }
    }

    @Nested
    inner class FindAll {
        private fun traceOf(
            realEstateAgencyId: UUID? = null,
            active: Boolean? = null,
            search: String? = null,
        ): SpecProbe.Trace {
            whenever(repository.findAll(any<Specification<BranchOwner>>(), eq(pageable)))
                .thenReturn(PageImpl(emptyList()))

            service.findAll(realEstateAgencyId, active, search, pageable)

            val specCaptor = argumentCaptor<Specification<BranchOwner>>()
            verify(repository, atLeastOnce()).findAll(specCaptor.capture(), eq(pageable))
            return SpecProbe.traceOf(specCaptor.allValues.last())
        }

        @Test
        fun `devolve a pagina ja mapeada para response`() {
            whenever(repository.findAll(any<Specification<BranchOwner>>(), eq(pageable)))
                .thenReturn(PageImpl(listOf(TestFixtures.branchOwner())))

            val page = service.findAll(null, null, null, pageable)

            assertThat(page.content).hasSize(1)
            assertThat(page.content.first().name).isEqualTo("Carlos Souza")
        }

        @Test
        fun `repassa o Pageable recebido sem reordenar`() {
            whenever(repository.findAll(any<Specification<BranchOwner>>(), eq(pageable))).thenReturn(PageImpl(emptyList()))

            service.findAll(null, null, null, pageable)

            verify(repository).findAll(any<Specification<BranchOwner>>(), eq(pageable))
        }

        @Test
        fun `sem nenhum filtro a Specification nao restringe nada`() {
            assertThat(traceOf().andValues.flatten()).isEmpty()
        }

        @Test
        fun `search vazio ou em branco nao vira filtro de like`() {
            assertThat(traceOf(search = "").andValues.flatten()).isEmpty()
            assertThat(traceOf(search = "  ").andValues.flatten()).isEmpty()
        }

        @Test
        fun `filtra por agencia e por active`() {
            val trace = traceOf(realEstateAgencyId = TestFixtures.AGENCY_ID, active = false)

            assertThat(trace.operations)
                .contains("equal(${TestFixtures.AGENCY_ID})", "equal(false)")
        }

        @Test
        fun `search vira um like com curinga sobre nome e e-mail`() {
            val trace = traceOf(search = "CARLOS")

            assertThat(trace.operations.filter { it.startsWith("like(") })
                .containsExactly("like(%carlos%)", "like(%carlos%)")
        }

        @Test
        fun `todos os filtros juntos viram tres restricoes`() {
            val trace = traceOf(realEstateAgencyId = TestFixtures.AGENCY_ID, active = true, search = "carlos")

            assertThat(trace.andValues.flatten()).hasSize(3)
        }
    }

    @Nested
    inner class Update {
        private val updateRequest = UpdateBranchOwnerRequest("Carlos Alberto Souza", "carlos.novo@example.com", "11999998888")

        @Test
        fun `sobrescreve nome, e-mail e telefone`() {
            val owner = TestFixtures.branchOwner()
            whenever(repository.findById(owner.id!!)).thenReturn(Optional.of(owner))
            saveGeneratingId()

            val response = service.update(owner.id!!, updateRequest)

            assertThat(response.name).isEqualTo("Carlos Alberto Souza")
            assertThat(response.email).isEqualTo("carlos.novo@example.com")
            assertThat(response.phone).isEqualTo("11999998888")
        }

        /**
         * `email` ausente é "não mexe", não "apaga": `users.email` é `NOT NULL` e
         * é a identidade do login do responsável. Só o telefone, que é nullable,
         * aceita ser limpo.
         */
        @Test
        fun `email ausente ephone aceitam ser limpos, e o e-mail e preservado`() {
            val owner = TestFixtures.branchOwner()
            whenever(repository.findById(owner.id!!)).thenReturn(Optional.of(owner))
            saveGeneratingId()

            val response = service.update(owner.id!!, updateRequest.copy(email = null, phone = null))

            assertThat(response.email).isEqualTo("carlos@example.com")
            assertThat(response.phone).isNull()
        }

        @Test
        fun `update nao mexe em active nem em realEstateAgencyId`() {
            val owner = TestFixtures.branchOwner(isActive = false)
            whenever(repository.findById(owner.id!!)).thenReturn(Optional.of(owner))
            saveGeneratingId()

            val response = service.update(owner.id!!, updateRequest)

            assertThat(response.active).isFalse()
            assertThat(response.realEstateAgencyId).isEqualTo(TestFixtures.AGENCY_ID)
        }

        /**
         * Trocar o e-mail para um ja usado quebraria o login de dois usuarios ao
         * mesmo tempo, entao a unicidade e conferida antes de gravar.
         */
        @Test
        fun `update recusa e-mail que ja pertence a outro usuario`() {
            val owner = TestFixtures.branchOwner()
            whenever(repository.findById(owner.id!!)).thenReturn(Optional.of(owner))
            whenever(userRepository.existsByEmail("carlos.novo@example.com")).thenReturn(true)

            assertThatThrownBy { service.update(owner.id!!, updateRequest) }
                .isInstanceOf(EmailAlreadyExistsException::class.java)

            verify(repository, never()).save(any<BranchOwner>())
        }

        @Test
        fun `update com o proprio e-mail nao acusa conflito consigo mesmo`() {
            val owner = TestFixtures.branchOwner(email = "carlos@example.com")
            whenever(repository.findById(owner.id!!)).thenReturn(Optional.of(owner))
            saveGeneratingId()

            val response = service.update(owner.id!!, updateRequest.copy(email = "carlos@example.com"))

            assertThat(response.email).isEqualTo("carlos@example.com")
        }

        @Test
        fun `id inexistente nao salva nada`() {
            val id = UUID.randomUUID()
            whenever(repository.findById(id)).thenReturn(Optional.empty())

            assertThatThrownBy { service.update(id, updateRequest) }
                .isInstanceOf(NotFoundException::class.java)
                .hasMessage("Responsável não encontrado")

            verify(repository, never()).save(any())
        }
    }

    @Nested
    inner class ToggleActive {
        @Test
        fun `desativa um responsavel ativo`() {
            val owner = TestFixtures.branchOwner(isActive = true)
            whenever(repository.findById(owner.id!!)).thenReturn(Optional.of(owner))
            saveGeneratingId()

            assertThat(service.toggleActive(owner.id!!).active).isFalse()
        }

        @Test
        fun `reativa um responsavel inativo`() {
            val owner = TestFixtures.branchOwner(isActive = false)
            whenever(repository.findById(owner.id!!)).thenReturn(Optional.of(owner))
            saveGeneratingId()

            assertThat(service.toggleActive(owner.id!!).active).isTrue()
        }

        @Test
        fun `id inexistente nao salva nada`() {
            val id = UUID.randomUUID()
            whenever(repository.findById(id)).thenReturn(Optional.empty())

            assertThatThrownBy { service.toggleActive(id) }.isInstanceOf(NotFoundException::class.java)

            verify(repository, never()).save(any<BranchOwner>())
        }
    }

    @Nested
    inner class Delete {
        @Test
        fun `remove o responsavel encontrado`() {
            val owner = TestFixtures.branchOwner()
            whenever(repository.findById(owner.id!!)).thenReturn(Optional.of(owner))
            whenever(branchOfficeRepository.existsByBranchOwnerId(owner.id!!)).thenReturn(false)

            service.delete(owner.id!!)

            verify(repository).delete(owner)
        }

        /**
         * `branch_office.branch_owner_id` e `NOT NULL` com `ON DELETE RESTRICT`,
         * entao o banco ja recusaria. A checagem existe para devolver o motivo em
         * portugues em vez de uma violacao de FK generica.
         */
        @Test
        fun `responsavel com filial nao e removivel`() {
            val owner = TestFixtures.branchOwner()
            whenever(repository.findById(owner.id!!)).thenReturn(Optional.of(owner))
            whenever(branchOfficeRepository.existsByBranchOwnerId(owner.id!!)).thenReturn(true)

            assertThatThrownBy { service.delete(owner.id!!) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("filiais")

            verify(repository, never()).delete(any<BranchOwner>())
        }

        @Test
        fun `id inexistente nao deleta nada`() {
            val id = UUID.randomUUID()
            whenever(repository.findById(id)).thenReturn(Optional.empty())

            assertThatThrownBy { service.delete(id) }
                .isInstanceOf(NotFoundException::class.java)
                .hasMessage("Responsável não encontrado")

            verify(repository, never()).delete(any<BranchOwner>())
        }
    }
}
