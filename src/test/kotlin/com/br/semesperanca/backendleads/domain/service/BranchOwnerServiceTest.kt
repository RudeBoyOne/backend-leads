package com.br.semesperanca.backendleads.domain.service

import com.br.semesperanca.backendleads.application.model.request.CreateBranchOwnerRequest
import com.br.semesperanca.backendleads.application.model.request.UpdateBranchOwnerRequest
import com.br.semesperanca.backendleads.domain.model.BranchOwner
import com.br.semesperanca.backendleads.domain.repository.BranchOwnerRepository
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
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class BranchOwnerServiceTest {
    @Mock private lateinit var repository: BranchOwnerRepository

    private lateinit var service: BranchOwnerService

    private val pageable: Pageable = PageRequest.of(0, 20, Sort.by("name"))

    @BeforeEach
    fun setUp() {
        service = BranchOwnerService(repository)
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

        @Test
        fun `responsavel nasce ativo`() {
            saveGeneratingId()

            val response = service.create(createRequest())

            assertThat(response.active).isTrue()
        }

        @Test
        fun `email e telefone sao opcionais`() {
            saveGeneratingId()

            val response =
                service.create(
                    CreateBranchOwnerRequest(TestFixtures.AGENCY_ID, "Sem contato", email = null, phone = null),
                )

            assertThat(response.email).isNull()
            assertThat(response.phone).isNull()
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
            assertThat(response.active).isTrue()
            assertThat(response.createdAt).isEqualTo(owner.createdAt)
            assertThat(response.updatedAt).isEqualTo(owner.updatedAt)
        }

        @Test
        fun `id inexistente vira RuntimeException com mensagem de dominio`() {
            val id = UUID.randomUUID()
            whenever(repository.findById(id)).thenReturn(Optional.empty())

            assertThatThrownBy { service.findById(id) }
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("Responsável de filial não encontrado")
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

        @Test
        fun `update aceita limpar o e-mail, enviando null`() {
            val owner = TestFixtures.branchOwner()
            whenever(repository.findById(owner.id!!)).thenReturn(Optional.of(owner))
            saveGeneratingId()

            val response = service.update(owner.id!!, updateRequest.copy(email = null, phone = null))

            assertThat(response.email).isNull()
            assertThat(response.phone).isNull()
        }

        @Test
        fun `update nao mexe em active nem em realEstateAgencyId`() {
            val owner = TestFixtures.branchOwner(active = false)
            whenever(repository.findById(owner.id!!)).thenReturn(Optional.of(owner))
            saveGeneratingId()

            val response = service.update(owner.id!!, updateRequest)

            assertThat(response.active).isFalse()
            assertThat(response.realEstateAgencyId).isEqualTo(TestFixtures.AGENCY_ID)
        }

        @Test
        fun `id inexistente nao salva nada`() {
            val id = UUID.randomUUID()
            whenever(repository.findById(id)).thenReturn(Optional.empty())

            assertThatThrownBy { service.update(id, updateRequest) }
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("Responsável de filial não encontrado")

            verify(repository, never()).save(any())
        }
    }

    @Nested
    inner class ToggleActive {
        @Test
        fun `desativa um responsavel ativo`() {
            val owner = TestFixtures.branchOwner(active = true)
            whenever(repository.findById(owner.id!!)).thenReturn(Optional.of(owner))
            saveGeneratingId()

            assertThat(service.toggleActive(owner.id!!).active).isFalse()
        }

        @Test
        fun `reativa um responsavel inativo`() {
            val owner = TestFixtures.branchOwner(active = false)
            whenever(repository.findById(owner.id!!)).thenReturn(Optional.of(owner))
            saveGeneratingId()

            assertThat(service.toggleActive(owner.id!!).active).isTrue()
        }
    }

    @Nested
    inner class Delete {
        @Test
        fun `remove o responsavel encontrado`() {
            val owner = TestFixtures.branchOwner()
            whenever(repository.findById(owner.id!!)).thenReturn(Optional.of(owner))

            service.delete(owner.id!!)

            verify(repository).delete(owner)
        }

        @Test
        fun `id inexistente nao deleta nada`() {
            val id = UUID.randomUUID()
            whenever(repository.findById(id)).thenReturn(Optional.empty())

            assertThatThrownBy { service.delete(id) }
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("Responsável de filial não encontrado")

            verify(repository, never()).delete(any<BranchOwner>())
        }
    }
}
