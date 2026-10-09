package com.br.semesperanca.backendleads.application.controller

import com.br.semesperanca.backendleads.domain.service.TeamService
import com.br.semesperanca.backendleads.support.AbstractControllerSlice
import com.br.semesperanca.backendleads.support.jsonEmptyBody
import com.br.semesperanca.backendleads.support.jsonEquals
import com.br.semesperanca.backendleads.support.jsonNotEmpty
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.assertj.MockMvcTester
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import java.util.UUID

@WebMvcTest(TeamController::class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class TeamControllerTest : AbstractControllerSlice() {
    @MockitoBean private lateinit var service: TeamService

    @Autowired private lateinit var mvc: MockMvcTester

    private val teamId = UUID.randomUUID()

    private val managerId = UUID.randomUUID()

    private val filialId = UUID.randomUUID()

    private val agentId = UUID.randomUUID()

    private fun response() =
        com.br.semesperanca.backendleads.application.model.response.TeamResponse(
            id = teamId,
            managerId = managerId,
            branchOfficeId = filialId,
            createdAt = java.time.LocalDateTime.of(2026, 1, 15, 10, 0),
        )

    @Test
    fun `GET lista os times`() {
        whenever(service.findAll()).thenReturn(listOf(response()))

        mvc
            .get()
            .uri("/teams")
            .exchange()
            .assertThat()
            .hasStatus(200)
            .bodyJson()
            .extractingPath("$[0].id")
            .isEqualTo(teamId.toString())
    }

    @Test
    fun `POST cria o time e devolve 201`() {
        whenever(service.create(any())).thenReturn(response())

        mvc
            .post()
            .uri("/teams")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"managerId": "$managerId", "branchOfficeId": "$filialId"}""")
            .exchange()
            .assertThat()
            .hasStatus(201)
            .matches(jsonEquals("$.id", teamId.toString()))
            .matches(jsonEquals("$.managerId", managerId.toString()))
    }

    /**
     * `managerId` e um UUID nao anulavel do construtor: falta-lo quebra a
     * deserializacao, e o `HttpMessageNotReadableException` cai no handler de
     * `RuntimeException` — sem o mapa de `fields`, que e coisa do
     * `MethodArgumentNotValidException`.
     */
    @Test
    fun `POST sem managerId devolve 400 com o erro de deserializacao`() {
        mvc
            .post()
            .uri("/teams")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"branchOfficeId": "$filialId"}""")
            .exchange()
            .assertThat()
            .hasStatus(400)
            .matches(jsonNotEmpty("$.message"))
            .matches(content().string(containsString("managerId")))

        verify(service, never()).create(any())
    }

    @Test
    fun `GET por id devolve o time`() {
        whenever(service.findById(teamId)).thenReturn(response())

        mvc
            .get()
            .uri("/teams/$teamId")
            .exchange()
            .assertThat()
            .hasStatus(200)
            .matches(jsonEquals("$.branchOfficeId", filialId.toString()))
    }

    @Test
    fun `PUT atualiza o time`() {
        whenever(service.update(any(), any())).thenReturn(response())

        mvc
            .put()
            .uri("/teams/$teamId")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"branchOfficeId": "$filialId"}""")
            .exchange()
            .assertThat()
            .hasStatus(200)
            .matches(jsonEquals("$.id", teamId.toString()))
    }

    @Test
    fun `DELETE devolve 204 e corpo vazio`() {
        mvc
            .delete()
            .uri("/teams/$teamId")
            .exchange()
            .assertThat()
            .hasStatus(204)
            .matches(jsonEmptyBody())

        verify(service).delete(teamId)
    }

    @Test
    fun `GET por filial lista os times da filial`() {
        whenever(service.findByBranchOfficeId(filialId)).thenReturn(listOf(response()))

        mvc
            .get()
            .uri("/teams/branch-office/$filialId")
            .exchange()
            .assertThat()
            .hasStatus(200)
            .bodyJson()
            .extractingPath("$.length()")
            .isEqualTo(1)
    }

    @Test
    fun `POST adicionar agente devolve 204`() {
        mvc
            .post()
            .uri("/teams/$teamId/agents/$agentId")
            .exchange()
            .assertThat()
            .hasStatus(204)
            .matches(jsonEmptyBody())

        verify(service).addAgent(teamId, agentId)
    }

    @Test
    fun `DELETE remover agente devolve 204`() {
        mvc
            .delete()
            .uri("/teams/$teamId/agents/$agentId")
            .exchange()
            .assertThat()
            .hasStatus(204)

        verify(service).removeAgent(teamId, agentId)
    }

    @Test
    fun `GET lista os ids dos corretores do time`() {
        whenever(service.findAgentsByTeamId(teamId)).thenReturn(listOf(agentId))

        val result =
            mvc
                .get()
                .uri("/teams/$teamId/agents")
                .exchange()
                .assertThat()
                .hasStatus(200)

        assertThat(result).isNotNull
    }
}
