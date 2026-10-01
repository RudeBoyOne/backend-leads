package com.br.semesperanca.backendleads.domain.service

import com.br.semesperanca.backendleads.application.model.request.CreateTeamRequest
import com.br.semesperanca.backendleads.application.model.request.UpdateTeamRequest
import com.br.semesperanca.backendleads.domain.model.BranchOffice
import com.br.semesperanca.backendleads.domain.model.Team
import com.br.semesperanca.backendleads.domain.model.TeamAgent
import com.br.semesperanca.backendleads.domain.model.TeamAgentId
import com.br.semesperanca.backendleads.domain.model.user.Manager
import com.br.semesperanca.backendleads.domain.model.user.RealEstateAgent
import com.br.semesperanca.backendleads.domain.repository.BranchOfficeRepository
import com.br.semesperanca.backendleads.domain.repository.TeamAgentRepository
import com.br.semesperanca.backendleads.domain.repository.TeamRepository
import com.br.semesperanca.backendleads.domain.repository.user.ManagerRepository
import com.br.semesperanca.backendleads.domain.repository.user.RealEstateAgentRepository
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
class TeamServiceTest {
    @Mock private lateinit var teamRepository: TeamRepository

    @Mock private lateinit var teamAgentRepository: TeamAgentRepository

    @Mock private lateinit var managerRepository: ManagerRepository

    @Mock private lateinit var branchOfficeRepository: BranchOfficeRepository

    @Mock private lateinit var agentRepository: RealEstateAgentRepository

    private lateinit var service: TeamService

    private val managerId = UUID.randomUUID()

    private val filialId = UUID.randomUUID()

    private val agentId = UUID.randomUUID()

    private val teamId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        service = TeamService(teamRepository, teamAgentRepository, managerRepository, branchOfficeRepository, agentRepository)
    }

    /** O `Team.id` e `@GeneratedValue`: sem id o `save` nao devolve nada usavel. */
    private fun saveGeneratingId() =
        whenever(teamRepository.save(any<Team>())).doAnswer { invocation ->
            @Suppress("UNCHECKED_CAST")
            (invocation.getArgument<Team>(0)).apply { if (id == null) id = teamId }
        }

    private fun team(branchOffice: BranchOffice? = null): Team =
        Team(
            id = teamId,
            manager = Manager().apply { id = managerId },
            branchOffice = branchOffice,
        )

    private fun filial() = BranchOffice(id = filialId, name = "Filial Centro")

    @Nested
    inner class Create {
        @Test
        fun `cria o time com gerente e filial`() {
            whenever(managerRepository.findById(managerId)).thenReturn(Optional.of(Manager().apply { id = managerId }))
            whenever(branchOfficeRepository.findById(filialId)).thenReturn(Optional.of(filial()))
            saveGeneratingId()

            val response = service.create(CreateTeamRequest(managerId, filialId))

            assertThat(response.id).isEqualTo(teamId)
            assertThat(response.managerId).isEqualTo(managerId)
            assertThat(response.branchOfficeId).isEqualTo(filialId)
        }

        @Test
        fun `filial ausente no request deixa o time sem filial`() {
            whenever(managerRepository.findById(managerId)).thenReturn(Optional.of(Manager().apply { id = managerId }))
            saveGeneratingId()

            val response = service.create(CreateTeamRequest(managerId, null))

            assertThat(response.branchOfficeId).isNull()
            verify(branchOfficeRepository, never()).findById(any())
        }

        @Test
        fun `gerente inexistente aborta com mensagem de dominio`() {
            whenever(managerRepository.findById(managerId)).thenReturn(Optional.empty())

            assertThatThrownBy { service.create(CreateTeamRequest(managerId, null)) }
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("Gerente não encontrado")

            verify(teamRepository, never()).save(any())
        }

        @Test
        fun `filial inexistente aborta com mensagem de dominio`() {
            whenever(managerRepository.findById(managerId)).thenReturn(Optional.of(Manager().apply { id = managerId }))
            whenever(branchOfficeRepository.findById(filialId)).thenReturn(Optional.empty())

            assertThatThrownBy { service.create(CreateTeamRequest(managerId, filialId)) }
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("Filial não encontrada")
        }
    }

    @Nested
    inner class Read {
        @Test
        fun `findById mapeia gerente e filial`() {
            whenever(teamRepository.findById(teamId)).thenReturn(Optional.of(team(filial())))

            val response = service.findById(teamId)

            assertThat(response.id).isEqualTo(teamId)
            assertThat(response.branchOfficeId).isEqualTo(filialId)
        }

        @Test
        fun `findById de time inexistente aborta`() {
            whenever(teamRepository.findById(teamId)).thenReturn(Optional.empty())

            assertThatThrownBy { service.findById(teamId) }
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("Time não encontrado")
        }

        @Test
        fun `findAll mapeia todos os times`() {
            whenever(teamRepository.findAll()).thenReturn(listOf(team(), team()))

            assertThat(service.findAll()).hasSize(2)
        }

        @Test
        fun `findByBranchOfficeId delega no filtro do repositorio`() {
            whenever(teamRepository.findByBranchOfficeId(filialId)).thenReturn(listOf(team(filial())))

            val response = service.findByBranchOfficeId(filialId)

            assertThat(response).hasSize(1)
            assertThat(response.first().branchOfficeId).isEqualTo(filialId)
        }
    }

    @Nested
    inner class Update {
        @Test
        fun `troca a filial do time`() {
            whenever(teamRepository.findById(teamId)).thenReturn(Optional.of(team()))
            whenever(branchOfficeRepository.findById(filialId)).thenReturn(Optional.of(filial()))
            saveGeneratingId()

            val response = service.update(teamId, UpdateTeamRequest(filialId))

            assertThat(response.branchOfficeId).isEqualTo(filialId)
        }

        @Test
        fun `branchOfficeId nulo desvincula a filial`() {
            whenever(teamRepository.findById(teamId)).thenReturn(Optional.of(team(filial())))
            saveGeneratingId()

            val response = service.update(teamId, UpdateTeamRequest(null))

            assertThat(response.branchOfficeId).isNull()
            verify(branchOfficeRepository, never()).findById(any())
        }

        @Test
        fun `time inexistente nao salva nada`() {
            whenever(teamRepository.findById(teamId)).thenReturn(Optional.empty())

            assertThatThrownBy { service.update(teamId, UpdateTeamRequest(null)) }
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("Time não encontrado")

            verify(teamRepository, never()).save(any())
        }
    }

    @Nested
    inner class Delete {
        @Test
        fun `deleta o time encontrado`() {
            val alvo = team()
            whenever(teamRepository.findById(teamId)).thenReturn(Optional.of(alvo))

            service.delete(teamId)

            verify(teamRepository).delete(alvo)
        }

        @Test
        fun `time inexistente nao deleta nada`() {
            whenever(teamRepository.findById(teamId)).thenReturn(Optional.empty())

            assertThatThrownBy { service.delete(teamId) }
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("Time não encontrado")

            verify(teamRepository, never()).delete(any())
        }
    }

    @Nested
    inner class Agents {
        @Test
        fun `addAgent grava o vinculo com joinedAt`() {
            whenever(teamRepository.findById(teamId)).thenReturn(Optional.of(team()))
            whenever(agentRepository.findById(agentId))
                .thenReturn(Optional.of(TestFixtures.realEstateAgent(id = agentId)))
            whenever(teamAgentRepository.existsByTeamIdAndRealEstateAgentId(teamId, agentId)).thenReturn(false)

            service.addAgent(teamId, agentId)

            val captor = argumentCaptor<TeamAgent>()
            verify(teamAgentRepository).save(captor.capture())
            assertThat(captor.firstValue.team?.id).isEqualTo(teamId)
            assertThat(captor.firstValue.realEstateAgent?.id).isEqualTo(agentId)
            assertThat(captor.firstValue.joinedAt).isNotNull()
        }

        @Test
        fun `corretor ja vinculado aborta antes do save`() {
            whenever(teamRepository.findById(teamId)).thenReturn(Optional.of(team()))
            whenever(agentRepository.findById(agentId))
                .thenReturn(Optional.of(TestFixtures.realEstateAgent(id = agentId)))
            whenever(teamAgentRepository.existsByTeamIdAndRealEstateAgentId(teamId, agentId)).thenReturn(true)

            assertThatThrownBy { service.addAgent(teamId, agentId) }
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("Corretor já pertence a este time")

            verify(teamAgentRepository, never()).save(any())
        }

        @Test
        fun `corretor inexistente aborta`() {
            whenever(teamRepository.findById(teamId)).thenReturn(Optional.of(team()))
            whenever(agentRepository.findById(agentId)).thenReturn(Optional.empty())

            assertThatThrownBy { service.addAgent(teamId, agentId) }
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("Corretor não encontrado")
        }

        @Test
        fun `removeAgent apaga o vinculo correto entre varios`() {
            val alvo =
                TeamAgent(
                    team = team(),
                    realEstateAgent = TestFixtures.realEstateAgent(id = agentId),
                )
            whenever(teamAgentRepository.findByTeamId(teamId)).thenReturn(listOf(alvo))

            service.removeAgent(teamId, agentId)

            verify(teamAgentRepository).delete(alvo)
        }

        @Test
        fun `removeAgent de corretor fora do time aborta`() {
            whenever(teamAgentRepository.findByTeamId(teamId)).thenReturn(emptyList())

            assertThatThrownBy { service.removeAgent(teamId, agentId) }
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("Corretor não encontrado neste time")

            verify(teamAgentRepository, never()).delete(any())
        }

        @Test
        fun `findAgentsByTeamId devolve so os ids e descarta vinculo sem corretor`() {
            val comAgente = TeamAgent(team = team(), realEstateAgent = TestFixtures.realEstateAgent(id = agentId))
            val semAgente = TeamAgent(team = team(), realEstateAgent = null)
            whenever(teamAgentRepository.findByTeamId(teamId)).thenReturn(listOf(comAgente, semAgente))

            assertThat(service.findAgentsByTeamId(teamId)).containsExactly(agentId)
        }
    }

    @Nested
    inner class TeamAgentIdentity {
        @Test
        fun `equals e hashCode do TeamAgent comparam o par time-corretor`() {
            val a = TeamAgent(team = team(), realEstateAgent = TestFixtures.realEstateAgent(id = agentId))
            val b = TeamAgent(team = team(), realEstateAgent = TestFixtures.realEstateAgent(id = agentId))
            val c = TeamAgent(team = team(), realEstateAgent = TestFixtures.realEstateAgent(id = UUID.randomUUID()))

            assertThat(a).isEqualTo(b).hasSameHashCodeAs(b)
            assertThat(a).isNotEqualTo(c)
            assertThat(a).isEqualTo(a)
            assertThat(a).isNotEqualTo("outro tipo")
        }

        @Test
        fun `TeamAgentId compara e hasheia os dois ids`() {
            val id = TeamAgentId(teamId, agentId)

            assertThat(id).isEqualTo(TeamAgentId(teamId, agentId)).hasSameHashCodeAs(TeamAgentId(teamId, agentId))
            assertThat(id).isNotEqualTo(TeamAgentId(teamId, UUID.randomUUID()))
            assertThat(id).isEqualTo(id)
            assertThat(id).isNotEqualTo("outro tipo")
            assertThat(id.hashCode()).isEqualTo(31 * teamId.hashCode() + agentId.hashCode())
        }
    }
}
