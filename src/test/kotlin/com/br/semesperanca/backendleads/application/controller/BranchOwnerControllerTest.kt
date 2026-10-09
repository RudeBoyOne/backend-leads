package com.br.semesperanca.backendleads.application.controller

import com.br.semesperanca.backendleads.configuration.exception.NotFoundException
import com.br.semesperanca.backendleads.domain.service.BranchOwnerService
import com.br.semesperanca.backendleads.support.AbstractControllerSlice
import com.br.semesperanca.backendleads.support.TestFixtures
import com.br.semesperanca.backendleads.support.jsonEmptyBody
import com.br.semesperanca.backendleads.support.jsonEquals
import com.br.semesperanca.backendleads.support.jsonNotEmpty
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.assertj.MockMvcTester
import java.util.UUID

@WebMvcTest(BranchOwnerController::class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class BranchOwnerControllerTest : AbstractControllerSlice() {
    @MockitoBean private lateinit var service: BranchOwnerService

    @Autowired private lateinit var mvc: MockMvcTester

    private val createJson =
        """
        {
          "realEstateAgencyId": "${TestFixtures.AGENCY_ID}",
          "name": "Carlos Souza",
          "email": "carlos@example.com",
          "password": "senha-secreta",
          "phone": "11912345678"
        }
        """.trimIndent()

    @Test
    fun `POST cria o responsavel e devolve 201`() {
        val created = TestFixtures.branchOwnerResponse(name = "Carlos Souza")
        whenever(service.create(any())).thenReturn(created)

        mvc
            .post()
            .uri("/branch-owners")
            .contentType(MediaType.APPLICATION_JSON)
            .content(createJson)
            .exchange()
            .assertThat()
            .hasStatus(201)
            .matches(jsonEquals("$.id", created.id.toString()))
            .matches(jsonEquals("$.name", "Carlos Souza"))
            .matches(jsonEquals("$.active", true))
    }

    @Test
    fun `POST com email invalido devolve 400 sem chamar o servico`() {
        mvc
            .post()
            .uri("/branch-owners")
            .contentType(MediaType.APPLICATION_JSON)
            .content(createJson.replace("carlos@example.com", "nao-e-email"))
            .exchange()
            .assertThat()
            .hasStatus(400)
            .matches(jsonNotEmpty("$.fields.email"))

        verify(service, never()).create(any())
    }

    @Test
    fun `POST sem nome devolve 400`() {
        mvc
            .post()
            .uri("/branch-owners")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """{"realEstateAgencyId": "${TestFixtures.AGENCY_ID}", "name": "", "email": "carlos@example.com", "password": "senha-secreta"}""",
            )
            .exchange()
            .assertThat()
            .hasStatus(400)
            .matches(jsonNotEmpty("$.fields.name"))
    }

    @Test
    fun `GET converte os 3 filtros e a pagina`() {
        val item = TestFixtures.branchOwnerResponse()
        whenever(service.findAll(anyOrNull(), anyOrNull(), anyOrNull(), any()))
            .thenReturn(PageImpl(listOf(item)))

        mvc
            .get()
            .uri("/branch-owners?realEstateAgencyId=${TestFixtures.AGENCY_ID}&active=false&search=carlos&page=3&size=2")
            .exchange()
            .assertThat()
            .hasStatus(200)
            .matches(jsonEquals("$.content[0].id", item.id.toString()))

        verify(service).findAll(
            eq(TestFixtures.AGENCY_ID),
            eq(false),
            eq("carlos"),
            eq(PageRequest.of(3, 2)),
        )
    }

    @Test
    fun `GET sem filtros usa page 0 e size 20`() {
        whenever(service.findAll(anyOrNull(), anyOrNull(), anyOrNull(), any()))
            .thenReturn(PageImpl(emptyList()))

        mvc
            .get()
            .uri("/branch-owners")
            .exchange()
            .assertThat()
            .hasStatus(200)

        verify(service).findAll(eq(null), eq(null), eq(null), eq(PageRequest.of(0, 20)))
    }

    @Test
    fun `GET por id devolve o responsavel`() {
        val id = UUID.randomUUID()
        whenever(service.findById(id)).thenReturn(TestFixtures.branchOwnerResponse(id = id, name = "Marina Reis"))

        mvc
            .get()
            .uri("/branch-owners/$id")
            .exchange()
            .assertThat()
            .hasStatus(200)
            .matches(jsonEquals("$.name", "Marina Reis"))
    }

    @Test
    fun `GET por id invalido devolve 400 antes de chegar no servico`() {
        mvc
            .get()
            .uri("/branch-owners/42")
            .exchange()
            .assertThat()
            .hasStatus(400)

        verify(service, never()).findById(any())
    }

    @Test
    fun `PATCH toggle-active devolve o responsavel com o estado invertido`() {
        val id = UUID.randomUUID()
        whenever(service.toggleActive(id)).thenReturn(TestFixtures.branchOwnerResponse(id = id, active = false))

        mvc
            .patch()
            .uri("/branch-owners/$id/toggle-active")
            .exchange()
            .assertThat()
            .hasStatus(200)
            .matches(jsonEquals("$.active", false))
    }

    @Test
    fun `PUT atualiza e devolve 200`() {
        val id = UUID.randomUUID()
        whenever(service.update(eq(id), any()))
            .thenReturn(TestFixtures.branchOwnerResponse(id = id, name = "Carlos Alberto"))

        mvc
            .put()
            .uri("/branch-owners/$id")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"name": "Carlos Alberto", "email": "carlos@example.com", "phone": "11999998888"}""")
            .exchange()
            .assertThat()
            .hasStatus(200)
            .matches(jsonEquals("$.name", "Carlos Alberto"))
    }

    @Test
    fun `PUT com telefone longo demais devolve 400 e nao chama o servico`() {
        mvc
            .put()
            .uri("/branch-owners/${UUID.randomUUID()}")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"name": "Carlos", "phone": "1234567890123456789012"}""")
            .exchange()
            .assertThat()
            .hasStatus(400)
            .matches(jsonNotEmpty("$.fields.phone"))

        verify(service, never()).update(any(), any())
    }

    @Test
    fun `DELETE devolve 204 sem corpo e delega ao servico`() {
        val id = UUID.randomUUID()

        mvc
            .delete()
            .uri("/branch-owners/$id")
            .exchange()
            .assertThat()
            .hasStatus(204)
            .matches(jsonEmptyBody())

        verify(service).delete(id)
    }

    /**
     * 404 e não 400: `findById` escopado devolve vazio tanto para id inexistente
     * quanto para id de outro responsável, e os dois casos precisam responder
     * igual — 403 confirmaria ao cliente que o registro existe.
     */
    @Test
    fun `DELETE de id fora do escopo devolve 404`() {
        val id = UUID.randomUUID()
        whenever(service.delete(id)).thenThrow(NotFoundException("Responsável não encontrado"))

        mvc
            .delete()
            .uri("/branch-owners/$id")
            .exchange()
            .assertThat()
            .hasStatus(404)
            .matches(jsonEquals("$.message", "Responsável não encontrado"))
    }
}
