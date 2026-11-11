package com.br.semesperanca.backendleads.domain.service

import com.br.semesperanca.backendleads.domain.model.Lead
import com.br.semesperanca.backendleads.domain.repository.LeadRepository
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.mock.http.client.MockClientHttpRequest
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.ResponseCreator
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withServerError
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import java.io.IOException
import java.time.Instant
import java.time.OffsetDateTime

/**
 * Teste de unidade (caixa-branca) de [InsightClient.createInsight] e do mapeamento
 * privado mapToInsightRequest (exercitado via createInsight).
 *
 * Recursos de teste usados:
 *  - MOCK  : leadRepository (Mockito) - verifica se o `save` do insightId ocorre.
 *  - STUB  : MockRestServiceServer faz o papel do servico externo de insights e devolve
 *            respostas pre-definidas (202 com id, 200, 500, timeout...). Nao sobe Spring
 *            nem rede.
 *  - DRIVER: cada @Test chama createInsight diretamente. Como o contexto Spring nao
 *            existe aqui, o @Async nao atua e a chamada roda de forma sincrona,
 *            o que torna os testes deterministicos.
 *
 * Os caminhos C1..C5 correspondem aos caminhos independentes do GFC de createInsight (Ex. 8).
 */
class InsightClientTest {
    private val leadRepository = mock<LeadRepository>() // MOCK
    private lateinit var server: MockRestServiceServer // STUB do servico de insights
    private lateinit var client: InsightClient

    @BeforeEach
    fun setUp() {
        val builder = RestClient.builder()
        server = MockRestServiceServer.bindTo(builder).build()
        client = InsightClient(leadRepository, builder, "http://insight.test")
    }

    private fun lead(
        status: String? = "new",
        phoneNumber: String? = "11999998888",
        timestamp: Instant? = Instant.parse("2026-01-10T12:00:00Z"),
    ) = Lead(
        originLeadId = "ORIGIN-1",
        leadOrigin = "ZAP",
        name = "Maria",
        email = "maria@teste.com",
        status = status,
        phoneNumber = phoneNumber,
        timestamp = timestamp,
    )

    /** Responde 202 com id e devolve o corpo JSON que o InsightClient enviou. */
    private fun enviarECapturarCorpo(lead: Lead): String {
        var corpo = ""
        server
            .expect(requestTo("http://insight.test/insight"))
            .andExpect(method(HttpMethod.POST))
            .andExpect { request -> corpo = (request as MockClientHttpRequest).bodyAsString }
            .andRespond(withStatus(HttpStatus.ACCEPTED).contentType(MediaType.APPLICATION_JSON).body("""{"id":"ins-1"}"""))
        client.createInsight(lead)
        server.verify()
        return corpo
    }

    // ---------------------------------------------------------------- RN03 (caminhos do GFC)

    @Test
    fun `C5 RN03 - 202 com id persiste o insightId no lead`() {
        val lead = lead()
        server
            .expect(requestTo("http://insight.test/insight"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withStatus(HttpStatus.ACCEPTED).contentType(MediaType.APPLICATION_JSON).body("""{"id":"ins-123"}"""))

        client.createInsight(lead)

        assertEquals("ins-123", lead.insightId)
        verify(leadRepository).save(lead)
        server.verify()
    }

    @Test
    fun `C4 RN03 - 202 com id nulo nao persiste insightId`() {
        val lead = lead()
        server.expect(requestTo("http://insight.test/insight"))
            .andRespond(withStatus(HttpStatus.ACCEPTED).contentType(MediaType.APPLICATION_JSON).body("""{"id":null}"""))

        client.createInsight(lead)

        assertNull(lead.insightId)
        verify(leadRepository, never()).save(any<Lead>())
    }

    @Test
    fun `C3 RN03 - 202 sem corpo nao persiste insightId`() {
        val lead = lead()
        server.expect(requestTo("http://insight.test/insight"))
            .andRespond(withStatus(HttpStatus.ACCEPTED))

        client.createInsight(lead)

        assertNull(lead.insightId)
        verify(leadRepository, never()).save(any<Lead>())
    }

    @Test
    fun `C2 RN03 - status diferente de 202 mesmo com id nao persiste insightId`() {
        val lead = lead()
        server.expect(requestTo("http://insight.test/insight"))
            .andRespond(withSuccess("""{"id":"ins-9"}""", MediaType.APPLICATION_JSON))

        client.createInsight(lead)

        assertNull(lead.insightId)
        verify(leadRepository, never()).save(any<Lead>())
    }

    @Test
    fun `C1 RN03 - erro 500 do servico e capturado e nao propaga`() {
        val lead = lead()
        server.expect(requestTo("http://insight.test/insight")).andRespond(withServerError())

        assertDoesNotThrow { client.createInsight(lead) }

        assertNull(lead.insightId)
        verify(leadRepository, never()).save(any<Lead>())
    }

    @Test
    fun `C1 RN03 - falha de rede ou timeout e capturada e nao propaga`() {
        val lead = lead()
        server.expect(requestTo("http://insight.test/insight"))
            .andRespond(ResponseCreator { throw IOException("timeout simulado") })

        assertDoesNotThrow { client.createInsight(lead) }

        assertNull(lead.insightId)
        verify(leadRepository, never()).save(any<Lead>())
    }

    // ---------------------------------------------------------------- RN04: converted

    @ParameterizedTest(name = "status \"{0}\" => converted={1}")
    @CsvSource("converted,true", "CONVERTED,true", "Converted,true", "new,false", "contacted,false")
    fun `RN04 - converted so e true para status converted sem diferenciar maiusculas`(
        status: String,
        esperado: Boolean,
    ) {
        val corpo = enviarECapturarCorpo(lead(status = status))

        assertTrue(corpo.contains("\"converted\":$esperado"), corpo)
    }

    @Test
    fun `RN04 - status nulo resulta em converted false`() {
        val corpo = enviarECapturarCorpo(lead(status = null))

        assertTrue(corpo.contains("\"converted\":false"), corpo)
    }

    // ---------------------------------------------------------------- RN04: telefone (valor limite = 2 caracteres)

    @Test
    fun `RN04 - telefone com 11 caracteres e dividido em ddd e phone`() {
        val corpo = enviarECapturarCorpo(lead(phoneNumber = "11999998888"))

        assertTrue(corpo.contains("\"ddd\":\"11\""), corpo)
        assertTrue(corpo.contains("\"phone\":\"999998888\""), corpo)
    }

    @Test
    fun `RN04 - telefone com exatamente 2 caracteres (limite) gera ddd e phone vazio`() {
        val corpo = enviarECapturarCorpo(lead(phoneNumber = "11"))

        assertTrue(corpo.contains("\"ddd\":\"11\""), corpo)
        assertTrue(corpo.contains("\"phone\":\"\""), corpo)
    }

    @ParameterizedTest(name = "phoneNumber \"{0}\" nao e dividido")
    @CsvSource("1", "''")
    fun `RN04 - telefone com menos de 2 caracteres nao gera ddd nem phone`(phoneNumber: String) {
        val corpo = enviarECapturarCorpo(lead(phoneNumber = phoneNumber))

        assertFalse(corpo.contains("\"ddd\":\""), corpo)
        assertFalse(corpo.contains("\"phone\":\""), corpo)
    }

    @Test
    fun `RN04 - telefone nulo nao gera ddd nem phone`() {
        val corpo = enviarECapturarCorpo(lead(phoneNumber = null))

        assertFalse(corpo.contains("\"ddd\":\""), corpo)
        assertFalse(corpo.contains("\"phone\":\""), corpo)
    }

    // ---------------------------------------------------------------- RN04: timestamp

    @Test
    fun `RN04 - usa o timestamp do lead quando informado`() {
        val corpo = enviarECapturarCorpo(lead(timestamp = Instant.parse("2026-01-10T12:00:00Z")))

        assertTrue(corpo.contains("\"timestamp\":\"2026-01-10T12:00:00Z\""), corpo)
    }

    @Test
    fun `RN04 - sem timestamp no lead usa o instante atual em ISO-8601`() {
        val antes = OffsetDateTime.now().minusMinutes(1)

        val corpo = enviarECapturarCorpo(lead(timestamp = null))

        val valor = Regex("\"timestamp\":\"([^\"]+)\"").find(corpo)?.groupValues?.get(1)
        assertNotNull(valor, corpo)
        val instante = OffsetDateTime.parse(valor) // lanca excecao se nao for ISO-8601 com offset
        assertTrue(instante.isAfter(antes), "timestamp deveria ser o instante atual: $valor")
    }
}
