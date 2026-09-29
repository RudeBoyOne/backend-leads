package com.br.semesperanca.backendleads.domain.service

import com.br.semesperanca.backendleads.domain.model.Lead
import com.br.semesperanca.backendleads.domain.repository.LeadRepository
import com.br.semesperanca.backendleads.support.TestFixtures
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.hamcrest.Matchers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.content
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.web.client.RestClient
import java.util.Optional

/**
 * O `InsightClient` monta o proprio `RestClient` a partir do `RestClient.Builder`,
 * entao o teste injeta um builder com `MockRestServiceServer` por tras. Nao ha
 * socket: toda chamada e respondida na memoria.
 */
@ExtendWith(MockitoExtension::class)
class InsightClientTest {
    @Mock private lateinit var leadRepository: LeadRepository

    private val baseUrl = "http://insight.test"
    private val insightUrl = "$baseUrl/insight"
    private lateinit var server: MockRestServiceServer
    private lateinit var client: InsightClient

    @BeforeEach
    fun setUp() {
        val builder = RestClient.builder()
        server = MockRestServiceServer.bindTo(builder).build()
        client = InsightClient(leadRepository, builder, baseUrl)
    }

    private fun stubLead(lead: Lead) {
        whenever(leadRepository.findByOriginLeadId(lead.originLeadId!!)).thenReturn(Optional.of(lead))
    }

    private fun accepted(body: String = """{"id":"insight-777"}""") =
        withStatus(HttpStatusCode.valueOf(202)).contentType(MediaType.APPLICATION_JSON).body(body)

    private fun expectPost() = server.expect(requestTo(insightUrl)).andExpect(method(HttpMethod.POST))

    @Test
    fun `persiste o insightId quando a API responde 202 com id`() {
        val lead = TestFixtures.lead()
        stubLead(lead)
        whenever(leadRepository.save(any<Lead>())).doAnswer { it.getArgument(0) }
        expectPost().andRespond(accepted())

        client.createInsight(lead.originLeadId!!)

        server.verify()
        verify(leadRepository).save(lead)
        assertThat(lead.insightId).isEqualTo("insight-777")
    }

    @Test
    fun `manda o lead mapeado com ddd e telefone separados`() {
        val lead = TestFixtures.lead(phoneNumber = "11987654321")
        stubLead(lead)
        expectPost()
            .andExpect(content().string(Matchers.containsString(""""ddd":"11"""")))
            .andExpect(content().string(Matchers.containsString(""""phone":"987654321"""")))
            .andRespond(accepted())

        client.createInsight(lead.originLeadId!!)

        server.verify()
    }

    @Test
    fun `usa os nomes de campo em snake_case do contrato`() {
        val lead = TestFixtures.lead()
        stubLead(lead)
        expectPost()
            .andExpect(content().string(Matchers.containsString(""""lead_origin":"Zapimoveis"""")))
            .andExpect(content().string(Matchers.containsString(""""origin_lead_id":"origin-1"""")))
            .andExpect(content().string(Matchers.containsString(""""origin_listing_id":"OL-1"""")))
            .andExpect(content().string(Matchers.containsString(""""listing_external_code":"LST-1"""")))
            .andRespond(accepted())

        client.createInsight(lead.originLeadId!!)

        server.verify()
    }

    @Test
    fun `status converted, ignorando caixa, vira converted true`() {
        val lead = TestFixtures.lead(status = "Converted")
        stubLead(lead)
        expectPost()
            .andExpect(content().string(Matchers.containsString(""""converted":true""")))
            .andRespond(accepted())

        client.createInsight(lead.originLeadId!!)

        server.verify()
    }

    @Test
    fun `lead sem timestamp manda um instante gerado no momento`() {
        val lead = TestFixtures.lead(timestamp = null)
        stubLead(lead)
        expectPost()
            .andExpect(content().string(Matchers.containsString(""""timestamp":"""")))
            .andRespond(accepted())

        client.createInsight(lead.originLeadId!!)

        server.verify()
    }

    @Test
    fun `telefone de um digito fica sem ddd, e nao quebra o mapeamento`() {
        val lead = TestFixtures.lead(phoneNumber = "9")
        stubLead(lead)
        expectPost()
            .andExpect(content().string(Matchers.containsString(""""ddd":null""")))
            .andExpect(content().string(Matchers.containsString(""""phone":null""")))
            .andRespond(accepted())

        client.createInsight(lead.originLeadId!!)

        server.verify()
    }

    @Test
    fun `resposta sem id nao grava o insightId`() {
        val lead = TestFixtures.lead()
        stubLead(lead)
        expectPost().andRespond(accepted("""{"id":null}"""))

        client.createInsight(lead.originLeadId!!)

        server.verify()
        assertThat(lead.insightId).isNull()
        verify(leadRepository, never()).save(any())
    }

    @Test
    fun `status diferente de 202 nao grava nada`() {
        val lead = TestFixtures.lead()
        stubLead(lead)
        expectPost()
            .andRespond(withStatus(HttpStatusCode.valueOf(200)).contentType(MediaType.APPLICATION_JSON).body("""{"id":"x"}"""))

        client.createInsight(lead.originLeadId!!)

        server.verify()
        assertThat(lead.insightId).isNull()
        verify(leadRepository, never()).save(any())
    }

    @Test
    fun `erro da API e engolido, sem propagar para quem chamou`() {
        val lead = TestFixtures.lead()
        stubLead(lead)
        expectPost().andRespond(withStatus(HttpStatusCode.valueOf(500)))

        assertThatCode { client.createInsight(lead.originLeadId!!) }.doesNotThrowAnyException()

        server.verify()
        verify(leadRepository, never()).save(any())
    }

    @Test
    fun `lead inexistente aborta com IllegalArgumentException antes de chamar a API`() {
        whenever(leadRepository.findByOriginLeadId("nao-existe")).thenReturn(Optional.empty())

        assertThatThrownBy { client.createInsight("nao-existe") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Lead not found: nao-existe")

        server.verify()
        verify(leadRepository, never()).save(any())
    }
}
