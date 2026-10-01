package com.br.semesperanca.backendleads.domain.service

import com.br.semesperanca.backendleads.application.model.request.CreateBranchOfficeRequest
import com.br.semesperanca.backendleads.application.model.request.UpdateBranchOfficeRequest
import com.br.semesperanca.backendleads.domain.model.BranchOffice
import com.br.semesperanca.backendleads.domain.repository.BranchOfficeRepository
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

    private lateinit var service: BranchOfficeService

    private val agencyId = UUID.randomUUID()

    private val filialId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        service = BranchOfficeService(repository)
    }

    private fun filial(active: Boolean = true): BranchOffice =
        BranchOffice(
            id = filialId,
            realEstateAgencyId = agencyId,
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

    @Nested
    inner class Create {
        @Test
        fun `cria a filial ativa com os dados do request`() {
            whenever(repository.existsByRealEstateAgencyIdAndName(agencyId, "Filial Centro")).thenReturn(false)
            saveGeneratingId()

            val response =
                service.create(
                    CreateBranchOfficeRequest("Filial Centro", "Av. Paulista, 1000", "1133334444", agencyId),
                )

            assertThat(response.id).isEqualTo(filialId)
            assertThat(response.realEstateAgencyId).isEqualTo(agencyId)
            assertThat(response.name).isEqualTo("Filial Centro")
            assertThat(response.active).isTrue()
        }

        @Test
        fun `nome repetido na mesma agencia aborta antes do save`() {
            whenever(repository.existsByRealEstateAgencyIdAndName(agencyId, "Filial Centro")).thenReturn(true)

            assertThatThrownBy {
                service.create(CreateBranchOfficeRequest("Filial Centro", null, null, agencyId))
            }.isInstanceOf(RuntimeException::class.java).hasMessage("Filial já cadastrada nesta agência")

            verify(repository, never()).save(any())
        }

        @Test
        fun `nao consulta o nome repetido quando a agencia e outra`() {
            whenever(repository.existsByRealEstateAgencyIdAndName(eq(agencyId), any())).thenReturn(false)
            saveGeneratingId()

            service.create(CreateBranchOfficeRequest("Filial Norte", null, null, agencyId))

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
        }

        @Test
        fun `filial inexistente aborta com mensagem de dominio`() {
            whenever(repository.findById(filialId)).thenReturn(Optional.empty())

            assertThatThrownBy { service.findById(filialId) }
                .isInstanceOf(RuntimeException::class.java)
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

        @Test
        fun `update nao mexe em agency nem em active`() {
            whenever(repository.findById(filialId)).thenReturn(Optional.of(filial(active = false)))
            saveGeneratingId()

            val response = service.update(filialId, UpdateBranchOfficeRequest("Filial Sul", null, null))

            assertThat(response.realEstateAgencyId).isEqualTo(agencyId)
            assertThat(response.active).isFalse()
        }

        @Test
        fun `filial inexistente nao salva nada`() {
            whenever(repository.findById(filialId)).thenReturn(Optional.empty())

            assertThatThrownBy { service.update(filialId, UpdateBranchOfficeRequest("Filial Sul", null, null)) }
                .isInstanceOf(RuntimeException::class.java)
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
                .isInstanceOf(RuntimeException::class.java)
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
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("Filial não encontrada")

            verify(repository, never()).delete(any())
        }
    }

    @Nested
    inner class BranchOfficeLifecycle {
        @Test
        fun `prePersist e preUpdate do BranchOffice preenchem os timestamps`() {
            val nova = BranchOffice(realEstateAgencyId = agencyId, name = "Filial Nova")

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
            val comData = BranchOffice(realEstateAgencyId = agencyId, name = "Filial Antiga", createdAt = anterior)

            comData.prePersist()

            assertThat(comData.createdAt).isEqualTo(anterior)
        }

        @Test
        fun `save persiste os campos vindos do request`() {
            whenever(repository.existsByRealEstateAgencyIdAndName(any(), any())).thenReturn(false)
            saveGeneratingId()

            service.create(CreateBranchOfficeRequest("Filial Oeste", "Rua Bela Vista, 9", "1155556666", agencyId))

            val captor = argumentCaptor<BranchOffice>()
            verify(repository).save(captor.capture())
            assertThat(captor.firstValue.name).isEqualTo("Filial Oeste")
            assertThat(captor.firstValue.realEstateAgencyId).isEqualTo(agencyId)
            assertThat(captor.firstValue.active).isTrue()
        }
    }
}
