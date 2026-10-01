package com.br.semesperanca.backendleads.application.controller

import com.br.semesperanca.backendleads.application.model.response.BranchOfficeResponse
import com.br.semesperanca.backendleads.domain.service.BranchOfficeService
import com.br.semesperanca.backendleads.support.AbstractControllerSlice
import com.br.semesperanca.backendleads.support.jsonEmptyBody
import com.br.semesperanca.backendleads.support.jsonEquals
import com.br.semesperanca.backendleads.support.jsonNotEmpty
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
import java.time.LocalDateTime
import java.util.UUID

@WebMvcTest(BranchOfficeController::class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class BranchOfficeControllerTest : AbstractControllerSlice() {
    @MockitoBean private lateinit var service: BranchOfficeService

    @Autowired private lateinit var mvc: MockMvcTester

    private val filialId = UUID.randomUUID()

    private val agencyId = UUID.randomUUID()

    private fun response() =
        BranchOfficeResponse(
            id = filialId,
            realEstateAgencyId = agencyId,
            name = "Filial Centro",
            address = "Av. Paulista, 1000",
            phone = "1133334444",
            active = true,
            createdAt = LocalDateTime.of(2026, 1, 15, 10, 0),
        )

    @Test
    fun `GET lista as filiais`() {
        whenever(service.findAll()).thenReturn(listOf(response()))

        mvc
            .get()
            .uri("/branch-offices")
            .exchange()
            .assertThat()
            .hasStatus(200)
            .bodyJson()
            .extractingPath("$[0].name")
            .isEqualTo("Filial Centro")
    }

    @Test
    fun `POST cria a filial e devolve 201`() {
        whenever(service.create(any())).thenReturn(response())

        mvc
            .post()
            .uri("/branch-offices")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                {
                  "name": "Filial Centro",
                  "address": "Av. Paulista, 1000",
                  "phone": "1133334444",
                  "realEstateAgencyId": "$agencyId"
                }
                """.trimIndent(),
            ).exchange()
            .assertThat()
            .hasStatus(201)
            .matches(jsonEquals("$.id", filialId.toString()))
            .matches(jsonEquals("$.active", true))
    }

    @Test
    fun `POST com nome em branco devolve 400 com o mapa de campos`() {
        mvc
            .post()
            .uri("/branch-offices")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"name": "   ", "realEstateAgencyId": "$agencyId"}""")
            .exchange()
            .assertThat()
            .hasStatus(400)
            .matches(jsonNotEmpty("$.fields.name"))

        verify(service, never()).create(any())
    }

    /**
     * `realEstateAgencyId` e um UUID nao anulavel do construtor: falta-lo quebra a
     * deserializacao, e o `HttpMessageNotReadableException` cai no handler de
     * `RuntimeException` — sem o mapa de `fields`, que e coisa do
     * `MethodArgumentNotValidException`.
     */
    @Test
    fun `POST sem agencyId devolve 400 com o erro de deserializacao`() {
        mvc
            .post()
            .uri("/branch-offices")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"name": "Filial Centro"}""")
            .exchange()
            .assertThat()
            .hasStatus(400)
            .matches(jsonNotEmpty("$.message"))
            .matches(content().string(containsString("realEstateAgencyId")))

        verify(service, never()).create(any())
    }

    @Test
    fun `GET por id devolve a filial`() {
        whenever(service.findById(filialId)).thenReturn(response())

        mvc
            .get()
            .uri("/branch-offices/$filialId")
            .exchange()
            .assertThat()
            .hasStatus(200)
            .matches(jsonEquals("$.address", "Av. Paulista, 1000"))
    }

    @Test
    fun `PUT atualiza a filial`() {
        whenever(service.update(any(), any())).thenReturn(response())

        mvc
            .put()
            .uri("/branch-offices/$filialId")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"name": "Filial Sul", "address": null, "phone": null}""")
            .exchange()
            .assertThat()
            .hasStatus(200)
            .matches(jsonEquals("$.name", "Filial Centro"))
    }

    @Test
    fun `PATCH toggle-active devolve o novo estado`() {
        whenever(service.toggleActive(filialId)).thenReturn(response().copy(active = false))

        mvc
            .patch()
            .uri("/branch-offices/$filialId/toggle-active")
            .exchange()
            .assertThat()
            .hasStatus(200)
            .matches(jsonEquals("$.active", false))
    }

    @Test
    fun `DELETE devolve 204 e corpo vazio`() {
        mvc
            .delete()
            .uri("/branch-offices/$filialId")
            .exchange()
            .assertThat()
            .hasStatus(204)
            .matches(jsonEmptyBody())

        verify(service).delete(filialId)
    }
}
