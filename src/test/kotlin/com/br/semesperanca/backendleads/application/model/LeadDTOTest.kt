package com.br.semesperanca.backendleads.application.model

import com.br.semesperanca.backendleads.support.TestFixtures
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.format.DateTimeParseException
import java.util.UUID

/**
 * `LeadDTO` e a fronteira entre o JSON do portal e a entidade `Lead`.
 *
 * A assimetria que mais importa: `fromLead` tem `!!` nos campos obrigatorios
 * (o `Lead` vem do banco, onde ja sao `NOT NULL`), enquanto `toLead` so converte
 * o que o DTO traz e deixa `insightId`, `createdAt` e `updatedAt` de fora — a
 * entidade criada por `toLead` ainda nao passou por `receiveLead`.
 */
class LeadDTOTest {
    @Test
    fun `toLead mapeia campo a campo e parseia o timestamp`() {
        val dto = TestFixtures.leadDTO()

        val lead = LeadDTO.toLead(dto)

        assertThat(lead.leadOrigin).isEqualTo(dto.leadOrigin)
        assertThat(lead.timestamp).isEqualTo(Instant.parse("2026-01-15T10:00:00Z"))
        assertThat(lead.originLeadId).isEqualTo(dto.originLeadId)
        assertThat(lead.originListingId).isEqualTo(dto.originListingId)
        assertThat(lead.clientListingId).isEqualTo(dto.clientListingId)
        assertThat(lead.name).isEqualTo(dto.name)
        assertThat(lead.email).isEqualTo(dto.email)
        assertThat(lead.phoneNumber).isEqualTo(dto.phone)
        assertThat(lead.message).isEqualTo(dto.message)
        assertThat(lead.temperature).isEqualTo(dto.temperature)
        assertThat(lead.transactionType).isEqualTo(dto.transactionType)
        assertThat(lead.realEstateAgencyId).isEqualTo(TestFixtures.AGENCY_ID)
        assertThat(lead.status).isEqualTo(dto.status)
    }

    @Test
    fun `toLead devolve id nulo porque a entidade ainda nao foi persistida`() {
        val lead = LeadDTO.toLead(TestFixtures.leadDTO())

        assertThat(lead.id).isNull()
    }

    @Test
    fun `toLead propaga os ids opcionais e o source`() {
        val agentId = UUID.randomUUID()
        val portalId = UUID.randomUUID()
        val funnelStageId = UUID.randomUUID()
        val dto =
            TestFixtures.leadDTO().copy(
                realEstateAgentId = agentId,
                portalId = portalId,
                funnelStageId = funnelStageId,
                source = "site",
            )

        val lead = LeadDTO.toLead(dto)

        assertThat(lead.realEstateAgentId).isEqualTo(agentId)
        assertThat(lead.portalId).isEqualTo(portalId)
        assertThat(lead.funnelStageId).isEqualTo(funnelStageId)
        assertThat(lead.source).isEqualTo("site")
    }

    /**
     * `timestamp` e `String` no DTO e `Instant` na entidade, entao um valor fora
     * do ISO-8601 estoura `DateTimeParseException` — o `GlobalExceptionHandler`
     * a transforma em 400.
     */
    @Test
    fun `toLead com timestamp invalido estoura DateTimeParseException`() {
        assertThatThrownBy { LeadDTO.toLead(TestFixtures.leadDTO().copy(timestamp = "15/01/2026")) }
            .isInstanceOf(DateTimeParseException::class.java)
    }

    @Test
    fun `fromLead espelha todos os campos`() {
        val lead = TestFixtures.lead(status = "converted")

        val dto = LeadDTO.fromLead(lead)

        assertThat(dto.originLeadId).isEqualTo(lead.originLeadId)
        assertThat(dto.status).isEqualTo("converted")
        assertThat(dto.phone).isEqualTo(lead.phoneNumber)
        assertThat(dto.timestamp).isEqualTo("2026-01-15T10:00:00Z")
        assertThat(dto.createdAt).isEqualTo("2026-01-15T10:00:00Z")
        assertThat(dto.updatedAt).isNull()
        assertThat(dto.realEstateAgentId).isNull()
    }

    /**
     * O DTO tem `@NotBlank` nos campos que a entidade declara `!!`; a atribuicao
     * forcar bypassa a validacao e o `!!` estoura. Fica registrado porque e o
     * motivo de `fromLead` nao aceitar `Lead` incompleta.
     */
    @Test
    fun `fromLead com campo obrigatorio nulo estoura NullPointerException`() {
        val lead = TestFixtures.lead().apply { name = null }

        assertThatThrownBy { LeadDTO.fromLead(lead) }
            .isInstanceOf(NullPointerException::class.java)
    }

    @Test
    fun `ida e volta preserva os campos de negocio do Lead`() {
        val original =
            TestFixtures.lead().apply {
                realEstateAgentId = UUID.randomUUID()
                source = "portal"
                insightId = "INS-9"
            }

        val volta = LeadDTO.toLead(LeadDTO.fromLead(original))

        assertThat(volta.name).isEqualTo(original.name)
        assertThat(volta.email).isEqualTo(original.email)
        assertThat(volta.realEstateAgencyId).isEqualTo(original.realEstateAgencyId)
        assertThat(volta.realEstateAgentId).isEqualTo(original.realEstateAgentId)
        assertThat(volta.status).isEqualTo(original.status)
        assertThat(volta.source).isEqualTo(original.source)
        assertThat(volta.insightId).isNull()
    }
}
