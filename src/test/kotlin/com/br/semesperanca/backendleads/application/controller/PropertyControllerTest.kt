package com.br.semesperanca.backendleads.application.controller

import com.br.semesperanca.backendleads.domain.service.PropertyService
import com.br.semesperanca.backendleads.support.AbstractControllerSlice
import com.br.semesperanca.backendleads.support.TestFixtures
import com.br.semesperanca.backendleads.support.jsonEmptyBody
import com.br.semesperanca.backendleads.support.jsonEquals
import com.br.semesperanca.backendleads.support.jsonNotEmpty
import org.hamcrest.Matchers.containsString
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import java.math.BigDecimal
import java.util.UUID

@WebMvcTest(PropertyController::class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class PropertyControllerTest : AbstractControllerSlice() {
    @MockitoBean private lateinit var service: PropertyService

    @Autowired private lateinit var mvc: MockMvcTester

    /**
     * A filial é `@NotNull` no create e sai da `Property` por ela. O id não é
     * validado aqui: o slice não tem repositório, e resolver a filial é trabalho
     * do `BranchOfficeRepository` escopado, não do binding do JSON.
     */
    private val branchOfficeId = UUID.randomUUID()

    @Test
    fun `POST cria o imovel e devolve 201 com o JSON do servico`() {
        val created = TestFixtures.propertyResponse(title = "Cobertura duplex")
        whenever(service.create(any())).thenReturn(created)

        mvc
            .post()
            .uri("/properties")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                {
                  "branchOfficeId": "$branchOfficeId",
                  "title": "Cobertura duplex",
                  "description": "Vista para o parque",
                  "price": 1250000.00,
                  "propertyType": "cobertura",
                  "bedrooms": 4,
                  "bathrooms": 3,
                  "areaSqm": 220.00,
                  "address": "Av. Paulista, 1000",
                  "city": "Sao Paulo",
                  "state": "SP",
                  "zipCode": "01310-100",
                  "status": "reserved"
                }
                """.trimIndent(),
            ).exchange()
            .assertThat()
            .hasStatus(201)
            .matches(jsonEquals("$.id", created.id.toString()))
            .matches(jsonEquals("$.title", "Cobertura duplex"))
            .matches(jsonEquals("$.active", true))
    }

    @Test
    fun `POST sem titulo devolve 400 com o mapa de campos, sem chamar o servico`() {
        mvc
            .post()
            .uri("/properties")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"branchOfficeId": "$branchOfficeId", "title": "  "}""")
            .exchange()
            .assertThat()
            .hasStatus(400)
            .matches(jsonNotEmpty("$.fields.title"))

        verify(service, never()).create(any())
    }

    @Test
    fun `POST com status fora do enum devolve 400 com a mensagem do campo`() {
        mvc
            .post()
            .uri("/properties")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """{"branchOfficeId": "$branchOfficeId", "title": "Casa", "status": "nao-existe"}""",
            ).exchange()
            .assertThat()
            .hasStatus(400)
            .matches(jsonNotEmpty("$.fields.status"))
    }

    /**
     * `branchOfficeId` e `UUID` nao-nulo no construtor, entao a falha e de
     * *binding* e nao de validacao: o Jackson aborta antes do `@Valid`, e a
     * `HttpMessageNotReadableException` cai no handler de `RuntimeException`.
     * O `@NotNull` do DTO e a segunda rede, e nao e alcancavel por JSON: o
     * Jackson aborta antes, porque `null` para um parametro Kotlin nao anulavel
     * tambem e falha de binding.
     */
    @Test
    fun `POST sem branchOfficeId devolve 400 com o erro de deserializacao, sem chamar o servico`() {
        mvc
            .post()
            .uri("/properties")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"title": "Casa"}""")
            .exchange()
            .assertThat()
            .hasStatus(400)
            .matches(jsonNotEmpty("$.message"))
            .matches(content().string(containsString("branchOfficeId")))

        verify(service, never()).create(any())
    }


    @Test
    fun `GET lista pagina por padrao, sem filtros`() {
        val item = TestFixtures.propertyResponse()
        whenever(service.findAll(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), any()))
            .thenReturn(PageImpl(listOf(item)))

        mvc
            .get()
            .uri("/properties")
            .exchange()
            .assertThat()
            .hasStatus(200)
            .matches(jsonEquals("$.content[0].id", item.id.toString()))
            .matches(jsonEquals("$.totalElements", 1))
    }

    @Test
    fun `GET converte os 7 query params e a pagina no filtro do servico`() {
        whenever(service.findAll(any(), any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(PageImpl(emptyList()))

        mvc
            .get()
            .uri(
                "/properties" +
                    "?realEstateAgencyId=${TestFixtures.AGENCY_ID}&active=false&status=sold" +
                    "&propertyType=casa&search=jardim&minPrice=100000.00&maxPrice=500000.00" +
                    "&page=2&size=5",
            ).exchange()
            .assertThat()
            .hasStatus(200)

        verify(service).findAll(
            eq(TestFixtures.AGENCY_ID),
            eq(false),
            eq("sold"),
            eq("casa"),
            eq("jardim"),
            eq(BigDecimal("100000.00")),
            eq(BigDecimal("500000.00")),
            eq(PageRequest.of(2, 5)),
        )
    }

    @Test
    fun `GET usa page 0 e size 20 quando o cliente nao informa paginacao`() {
        whenever(service.findAll(any(), any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(PageImpl(emptyList()))

        mvc
            .get()
            .uri("/properties")
            .exchange()
            .assertThat()
            .hasStatus(200)

        verify(service).findAll(
            eq(null),
            eq(null),
            eq(null),
            eq(null),
            eq(null),
            eq(null),
            eq(null),
            eq(PageRequest.of(0, 20)),
        )
    }

    @Test
    fun `GET por id devolve o imovel`() {
        val id = UUID.randomUUID()
        whenever(service.findById(id)).thenReturn(TestFixtures.propertyResponse(id = id, title = "Casa de campo"))

        mvc
            .get()
            .uri("/properties/$id")
            .exchange()
            .assertThat()
            .hasStatus(200)
            .matches(jsonEquals("$.title", "Casa de campo"))
    }

    @Test
    fun `GET por id invalido devolve 400 antes de chegar no servico`() {
        mvc
            .get()
            .uri("/properties/nao-e-uuid")
            .exchange()
            .assertThat()
            .hasStatus(400)

        verify(service, never()).findById(any())
    }

    @Test
    fun `GET por id inexistente transforma o erro do dominio em 400`() {
        val id = UUID.randomUUID()
        whenever(service.findById(id)).thenThrow(RuntimeException("Imóvel não encontrado"))

        mvc
            .get()
            .uri("/properties/$id")
            .exchange()
            .assertThat()
            .hasStatus(400)
            .matches(jsonEquals("$.message", "Imóvel não encontrado"))
    }

    @Test
    fun `PATCH toggle-active devolve o imovel com o estado invertido`() {
        val id = UUID.randomUUID()
        whenever(service.toggleActive(id)).thenReturn(TestFixtures.propertyResponse(id = id, active = false))

        mvc
            .patch()
            .uri("/properties/$id/toggle-active")
            .exchange()
            .assertThat()
            .hasStatus(200)
            .matches(jsonEquals("$.active", false))
    }

    @Test
    fun `PUT atualiza e devolve 200`() {
        val id = UUID.randomUUID()
        whenever(service.update(eq(id), any())).thenReturn(TestFixtures.propertyResponse(id = id, title = "Reformado"))

        mvc
            .put()
            .uri("/properties/$id")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                {
                  "title": "Reformado",
                  "description": "Novo acabamento",
                  "price": 500000.00,
                  "propertyType": "apartamento",
                  "bedrooms": 3,
                  "bathrooms": 2,
                  "areaSqm": 80.00,
                  "address": "Rua Nova, 20",
                  "city": "Campinas",
                  "state": "SP",
                  "zipCode": "13000-000",
                  "status": "sold"
                }
                """.trimIndent(),
            ).exchange()
            .assertThat()
            .hasStatus(200)
            .matches(jsonEquals("$.title", "Reformado"))
    }

    @Test
    fun `PUT sem titulo devolve 400 e nao chama o servico`() {
        mvc
            .put()
            .uri("/properties/${UUID.randomUUID()}")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"title": ""}""")
            .exchange()
            .assertThat()
            .hasStatus(400)

        verify(service, never()).update(any(), any())
    }

    @Test
    fun `DELETE devolve 204 sem corpo e delega ao servico`() {
        val id = UUID.randomUUID()

        mvc
            .delete()
            .uri("/properties/$id")
            .exchange()
            .assertThat()
            .hasStatus(204)
            .matches(jsonEmptyBody())

        verify(service).delete(id)
    }

    @Test
    fun `DELETE de id inexistente devolve 400`() {
        val id = UUID.randomUUID()
        whenever(service.delete(id)).thenThrow(RuntimeException("Imóvel não encontrado"))

        mvc
            .delete()
            .uri("/properties/$id")
            .exchange()
            .assertThat()
            .hasStatus(400)
            .matches(jsonEquals("$.message", "Imóvel não encontrado"))
    }
}
