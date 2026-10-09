package com.br.semesperanca.backendleads.domain.service

import com.br.semesperanca.backendleads.application.model.request.CreateBranchOfficeRequest
import com.br.semesperanca.backendleads.application.model.request.UpdateBranchOfficeRequest
import com.br.semesperanca.backendleads.configuration.exception.NotFoundException
import com.br.semesperanca.backendleads.domain.model.BranchOffice
import com.br.semesperanca.backendleads.domain.repository.BranchOfficeRepository
import com.br.semesperanca.backendleads.domain.repository.BranchOwnerRepository
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
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class BranchOfficeServiceTest {
    @Mock private lateinit var repository: BranchOfficeRepository

    @Mock private lateinit var branchOwnerRepository: BranchOwnerRepository

    private lateinit var service: BranchOfficeService

    private val agencyId = UUID.randomUUID()

    private val filialId = UUID.randomUUID()

    private val owner = TestFixtures.branchOwner(realEstateAgencyId = agencyId)

    @BeforeEach
    fun setUp() {
        service = BranchOfficeService(repository, branchOwnerRepository)
    }

    private fun filial(active: Boolean = true): BranchOffice =
        BranchOffice(
            id = filialId,
            realEstateAgencyId = agencyId,
            branchOwner = owner,
            name = "Filial Centro",
            address = "Av. Paulista, 1000",
            phone = "1133334444",
            active = active,
        )

    private fun saveGeneratingId() =
        whenever(repository.save(any<BranchOffice>())).doAnswer { invocation ->
            @Suppress("UNCHECKED_CAST")
            (invocation.getArgument<BranchOffice>(0)).apply { if (id == null) id = filialId }
        }

    private fun request(
        name: String = "Filial Centro",
        address: String? = "Av. Paulista, 1000",
        phone: String? = "1133334444",
    ) = CreateBranchOfficeRequest(name, address, phone, owner.id!!)

    @Nested
    inner class Create {
        @Test
        fun `cria a filial ativa com os dados do request`() {
            whenever(branchOwnerRepository.findById(owner.id!!)).thenReturn(Optional.of(owner))
            whenever(repository.existsByRealEstateAgencyIdAndName(agencyId, "Filial Centro")).thenReturn(false)
            saveGeneratingId()

            val response = service.create(request())

            assertThat(response.id).isEqualTo(filialId)
            assertThat(response.realEstateAgencyId).isEqualTo(agencyId)
            assertThat(response.name).isEqualTo("Filial Centro")
            assertThat(response.active).isTrue()
        }

        /**
         * `branch_office.branch_owner_id` e `NOT NULL` com `UNIQUE`. Sem gravar o
         * dono, a filial nao teria quem a administrasse e o escopo de branch owner
         * — que percorre exatamente `filial -> responsavel` — nao acharia ela.
         */
        @Test
        fun `a filial nasce com o responsavel resolvido pelo request`() {
            whenever(branchOwnerRepository.findById(owner.id!!)).thenReturn(Optional.of(owner))
            whenever(repository.existsByRealEstateAgencyIdAndName(agencyId, "Filial Centro")).thenReturn(false)
            saveGeneratingId()

            val response = service.create(request())

            val captor = argumentCaptor<BranchOffice>()
            verify(repository).save(captor.capture())
            assertThat(captor.firstValue.branchOwner?.id).isEqualTo(owner.id)
            assertThat(response.branchOwnerId).isEqualTo(owner.id)
        }

        /**
         * Filial e responsavel pertencem a mesma agencia. Derivar a agencia do dono
         * elimina o par de ids que divergiria — e a divergencia apareceria como
         * uma filial de uma agencia que o escopo do dono nao alcanca.
         */
        @Test
        fun `a agencia vem do responsavel, e nao do corpo do request`() {
            whenever(branchOwnerRepository.findById(owner.id!!)).thenReturn(Optional.of(owner))
            whenever(repository.existsByRealEstateAgencyIdAndName(agencyId, "Filial Centro")).thenReturn(false)
            saveGeneratingId()

            service.create(request())

            val captor = argumentCaptor<BranchOffice>()
            verify(repository).save(captor.capture())
            assertThat(captor.firstValue.realEstateAgencyId).isEqualTo(owner.realEstateAgencyId)
        }

        /**
         * O `findById` do responsavel e escopado: um branch owner que se atribua a
         * um id de outro nao encontra e a criacao morre em 404. A autorizacao sai
         * da consulta, sem nenhum `if` de papel no servico.
         */
        @Test
        fun `responsavel fora do escopo do branch owner aborta com 404`() {
            whenever(branchOwnerRepository.findById(owner.id!!)).thenReturn(Optional.empty())

            assertThatThrownBy { service.create(request()) }
                .isInstanceOf(NotFoundException::class.java)
                .hasMessage("Responsável não encontrado")

            verify(repository, never()).save(any<BranchOffice>())
        }

        @Test
        fun `nome repetido na mesma agencia aborta antes do save`() {
            whenever(branchOwnerRepository.findById(owner.id!!)).thenReturn(Optional.of(owner))
            whenever(repository.existsByRealEstateAgencyIdAndName(agencyId, "Filial Centro")).thenReturn(true)

            assertThatThrownBy { service.create(request()) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("Já existe uma filial com este nome nesta agência")

            verify(repository, never()).save(any())
        }

        @Test
        fun `nao consulta o nome repetido quando a agencia e outra`() {
            whenever(branchOwnerRepository.findById(owner.id!!)).thenReturn(Optional.of(owner))
            whenever(repository.existsByRealEstateAgencyIdAndName(eq(agencyId), any())).thenReturn(false)
            saveGeneratingId()

            service.create(request(name = "Filial Norte", address = null, phone = null))

            verify(repository).existsByRealEstateAgencyIdAndName(agencyId, "Filial Norte")
        }
    }

    @Nested
    inner class Read {
        @Test
        fun `findById mapeia a filial`() {
            whenever(repository.findById(filialId)).thenReturn(Optional.of(filial()))

            val response = service.findById(filialId)

            assertThat(response.id).isEqualTo(filialId)
            assertThat(response.address).isEqualTo("Av. Paulista, 1000")
            assertThat(response.phone).isEqualTo("1133334444")
            assertThat(response.branchOwnerId).isEqualTo(owner.id)
        }

        @Test
        fun `filial inexistente aborta com NotFoundException`() {
            whenever(repository.findById(filialId)).thenReturn(Optional.empty())

            assertThatThrownBy { service.findById(filialId) }
                .isInstanceOf(NotFoundException::class.java)
                .hasMessage("Filial não encontrada")
        }

        @Test
        fun `findAll mapeia todas as filiais`() {
            whenever(repository.findAll()).thenReturn(listOf(filial(), filial()))

            assertThat(service.findAll()).hasSize(2)
        }
    }

    @Nested
    inner class Update {
        @Test
        fun `sobrescreve nome, endereco e telefone`() {
            whenever(repository.findById(filialId)).thenReturn(Optional.of(filial()))
            saveGeneratingId()

            val response =
                service.update(
                    filialId,
                    UpdateBranchOfficeRequest("Filial Sul", "Rua Augusta, 500", "1144445555"),
                )

            assertThat(response.name).isEqualTo("Filial Sul")
            assertThat(response.address).isEqualTo("Rua Augusta, 500")
            assertThat(response.phone).isEqualTo("1144445555")
        }

        /**
         * O `update` nao aceita `branchOwnerId`: trocar o responsavel de uma filial
         * reescreve a subarvore de quem responde por ela, e isso e uma operacao do
         * root, nao um detalhe do cadastro.
         */
        @Test
        fun `update nao mexe em agency, em responsavel nem em active`() {
            whenever(repository.findById(filialId)).thenReturn(Optional.of(filial(active = false)))
            saveGeneratingId()

            val response = service.update(filialId, UpdateBranchOfficeRequest("Filial Sul", null, null))

            assertThat(response.realEstateAgencyId).isEqualTo(agencyId)
            assertThat(response.branchOwnerId).isEqualTo(owner.id)
            assertThat(response.active).isFalse()
        }

        @Test
        fun `filial inexistente nao salva nada`() {
            whenever(repository.findById(filialId)).thenReturn(Optional.empty())

            assertThatThrownBy { service.update(filialId, UpdateBranchOfficeRequest("Filial Sul", null, null)) }
                .isInstanceOf(NotFoundException::class.java)
                .hasMessage("Filial não encontrada")

            verify(repository, never()).save(any())
        }

        @Test
        fun `toggleActive inverte o flag e devolve o novo estado`() {
            whenever(repository.findById(filialId)).thenReturn(Optional.of(filial(active = true)))
            saveGeneratingId()

            assertThat(service.toggleActive(filialId).active).isFalse()
            assertThat(service.toggleActive(filialId).active).isTrue()
        }

        @Test
        fun `toggleActive de filial inexistente nao salva nada`() {
            whenever(repository.findById(filialId)).thenReturn(Optional.empty())

            assertThatThrownBy { service.toggleActive(filialId) }
                .isInstanceOf(NotFoundException::class.java)
                .hasMessage("Filial não encontrada")

            verify(repository, never()).save(any())
        }
    }

    @Nested
    inner class Delete {
        @Test
        fun `deleta a filial encontrada`() {
            whenever(repository.findById(filialId)).thenReturn(Optional.of(filial()))

            service.delete(filialId)

            verify(repository).delete(any<BranchOffice>())
        }

        @Test
        fun `filial inexistente nao deleta nada`() {
            whenever(repository.findById(filialId)).thenReturn(Optional.empty())

            assertThatThrownBy { service.delete(filialId) }
                .isInstanceOf(NotFoundException::class.java)
                .hasMessage("Filial não encontrada")

            verify(repository, never()).delete(any<BranchOffice>())
        }
    }

    @Nested
    inner class BranchOfficeLifecycle {
        @Test
        fun `prePersist e preUpdate do BranchOffice preenchem os timestamps`() {
            val nova = BranchOffice(realEstateAgencyId = agencyId, branchOwner = owner, name = "Filial Nova")

            nova.prePersist()
            assertThat(nova.createdAt).isNotNull()
            assertThat(nova.updatedAt).isNotNull()

            val criado = nova.createdAt
            nova.preUpdate()
            assertThat(nova.createdAt).isEqualTo(criado)
        }

        @Test
        fun `prePersist nao sobrescreve um createdAt que ja veio preenchido`() {
            val anterior = java.time.LocalDateTime.of(2020, 1, 1, 8, 0)
            val comData = BranchOffice(realEstateAgencyId = agencyId, branchOwner = owner, name = "Filial Antiga", createdAt = anterior)

            comData.prePersist()

            assertThat(comData.createdAt).isEqualTo(anterior)
        }

        @Test
        fun `save persiste os campos vindos do request`() {
            whenever(branchOwnerRepository.findById(owner.id!!)).thenReturn(Optional.of(owner))
            whenever(repository.existsByRealEstateAgencyIdAndName(any(), any())).thenReturn(false)
            saveGeneratingId()

            service.create(request(name = "Filial Oeste", address = "Rua Bela Vista, 9", phone = "1155556666"))

            val captor = argumentCaptor<BranchOffice>()
            verify(repository).save(captor.capture())
            assertThat(captor.firstValue.name).isEqualTo("Filial Oeste")
            assertThat(captor.firstValue.realEstateAgencyId).isEqualTo(agencyId)
            assertThat(captor.firstValue.active).isTrue()
        }
    }
}
