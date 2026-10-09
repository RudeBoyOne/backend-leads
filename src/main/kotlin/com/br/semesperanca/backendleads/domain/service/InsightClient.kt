package com.br.semesperanca.backendleads.domain.service

import com.br.semesperanca.backendleads.application.model.InsightRequest
import com.br.semesperanca.backendleads.application.model.InsightResponse
import com.br.semesperanca.backendleads.domain.model.Lead
import com.br.semesperanca.backendleads.domain.repository.LeadRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

/**
 * Envia o lead ao serviço de Insight.
 *
 * [createInsight] recebe o `Lead` **já carregado** e não busca por
 * `originLeadId`, e isso não é preferência de assinatura: o método é `@Async`, e a
 * thread do executor não tem `SecurityContext`. Uma busca aqui rodaria sem
 * principal nenhum, e como `findByOriginLeadId` é método derivado — que não passa
 * pela [com.br.semesperanca.backendleads.domain.repository.ScopedRepositoryImpl] —
 * ela sairia sem escopo. Bastava um `POST /leads/receive` com o
 * `originLeadId` de outra agência para que os dados de contato desse lead fossem
 * enviados a um terceiro, e nada no código denunciaria a falha.
 *
 * Por isso a resolução do lead fica em
 * [com.br.semesperanca.backendleads.domain.service.LeadService.receiveLead], na
 * thread do request, onde o principal existe e o repositório escopado responde.
 * Aqui só resta a escrita do `insightId`, que é `save` de uma entidade já
 * carregada e por isso não precisa de principal.
 */
@Component
class InsightClient(
    private val leadRepository: LeadRepository,
    restClientBuilder: RestClient.Builder,
    @Value("\${app.insight.url}") insightUrl: String,
) {
    private val restClient: RestClient = restClientBuilder.baseUrl(insightUrl).build()
    private val logger = LoggerFactory.getLogger(InsightClient::class.java)

    @Async
    fun createInsight(lead: Lead) {
        val originLeadId = lead.originLeadId

        val request = mapToInsightRequest(lead)

        try {
            val response =
                restClient
                    .post()
                    .uri("/insight")
                    .body(request)
                    .retrieve()
                    .toEntity(InsightResponse::class.java)

            if (response.statusCode == HttpStatus.ACCEPTED &&
                response.body != null &&
                response.body!!.id != null
            ) {
                lead.insightId = response.body!!.id
                leadRepository.save(lead)
                logger.info("Insight created | LeadId: {} | InsightId: {}", originLeadId, response.body!!.id)
            }
        } catch (e: Exception) {
            logger.error("Failed to create insight for lead: {} | Error: {}", originLeadId, e.message)
        }
    }

    private fun mapToInsightRequest(lead: Lead): InsightRequest {
        var ddd: String? = null
        var phone: String? = null
        val phoneNumber = lead.phoneNumber
        if (phoneNumber != null && phoneNumber.length >= 2) {
            ddd = phoneNumber.substring(0, 2)
            phone = phoneNumber.substring(2)
        }

        val converted = "converted".equals(lead.status, ignoreCase = true)

        val timestamp =
            lead.timestamp?.toString()
                ?: OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

        return InsightRequest(
            leadOrigin = lead.leadOrigin,
            originLeadId = lead.originLeadId,
            transactionType = lead.transactionType,
            converted = converted,
            originListingId = lead.originListingId,
            listingExternalCode = lead.clientListingId,
            listingNeighborhood = null,
            listingAddress = null,
            leadName = lead.name,
            leadEmail = lead.email,
            ddd = ddd,
            phone = phone,
            message = lead.message,
            temperature = lead.temperature,
            listingPrice = null,
            timestamp = timestamp,
        )
    }
}
