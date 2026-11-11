package com.br.semesperanca.backendleads.domain.service

import com.br.semesperanca.backendleads.application.model.LeadReceivedNotificationDTO
import com.br.semesperanca.backendleads.configuration.MailProperties
import com.br.semesperanca.backendleads.configuration.exception.NotFoundException
import com.br.semesperanca.backendleads.domain.model.Lead
import com.br.semesperanca.backendleads.domain.repository.LeadRepository
import com.br.semesperanca.backendleads.domain.repository.user.RealEstateAgentRepository
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import org.springframework.dao.IncorrectResultSizeDataAccessException
import org.springframework.data.jpa.domain.Specification
import java.util.Optional

/**
 * Teste de unidade (caixa-branca) de [LeadService.receiveLead],
 * caso de uso "Receber lead de portal".
 *
 * Recursos de teste usados (o JUnit cria uma instancia nova da classe por @Test,
 * entao cada teste comeca com dubles limpos):
 *  - MOCK : leadRepository, emailService, insightClient, realEstateAgentRepository.
 *           Substituem colaboradores externos (banco, SMTP, servico de insights) e
 *           permitem verificar interacoes (verify / inOrder / never).
 *  - STUB : `whenever(leadRepository.findOne(...)).thenReturn(...)` define a resposta
 *           pronta do repositorio; MailProperties e uma instancia real com valor fixo.
 *  - DRIVER: cada metodo @Test e o driver que chama receiveLead.
 */
class LeadServiceTest {
    // MOCKs
    private val leadRepository = mock<LeadRepository>()
    private val emailService = mock<EmailService>()
    private val insightClient = mock<InsightClient>()
    private val realEstateAgentRepository = mock<RealEstateAgentRepository>() // nao usado em receiveLead

    // STUB (objeto real, valor fixo)
    private val mailProperties =
        MailProperties().apply {
            from = "no-reply@test"
            notifyTo = "equipe@imobiliaria.test"
        }

    private val service =
        LeadService(leadRepository, emailService, mailProperties, insightClient, realEstateAgentRepository)

    private val dto =
        LeadReceivedNotificationDTO(leadIdDb = "db-1", originLeadId = "ORIGIN-1", leadOrigin = "ZAP")

    private fun leadEncontrado() = Lead(originLeadId = "ORIGIN-1", leadOrigin = "ZAP")

    @Test
    fun `RN02 - envia o e-mail antes de solicitar o insight com o lead resolvido`() {
        val lead = leadEncontrado()
        whenever(leadRepository.findOne(any<Specification<Lead>>())).thenReturn(Optional.of(lead))

        service.receiveLead(dto)

        inOrder(emailService, insightClient) {
            verify(emailService).sendLeadReceived(dto, "equipe@imobiliaria.test")
            verify(insightClient).createInsight(lead)
        }
    }

    @Test
    fun `RN05 - destinatario e sempre o configurado em app mail notify-to`() {
        whenever(leadRepository.findOne(any<Specification<Lead>>())).thenReturn(Optional.of(leadEncontrado()))
        val outro = LeadReceivedNotificationDTO("db-2", "ORIGIN-2", "VIVAREAL")

        service.receiveLead(dto)
        service.receiveLead(outro)

        verify(emailService).sendLeadReceived(dto, "equipe@imobiliaria.test")
        verify(emailService).sendLeadReceived(outro, "equipe@imobiliaria.test")
    }

    @Test
    fun `notify-to nulo - envia com destinatario vazio (predicado do elvis)`() {
        mailProperties.notifyTo = null
        whenever(leadRepository.findOne(any<Specification<Lead>>())).thenReturn(Optional.of(leadEncontrado()))

        service.receiveLead(dto)

        verify(emailService).sendLeadReceived(dto, "")
    }

    @Test
    fun `RN01 - resolve o lead apenas via findOne com Specification (repositorio escopado)`() {
        whenever(leadRepository.findOne(any<Specification<Lead>>())).thenReturn(Optional.of(leadEncontrado()))

        service.receiveLead(dto)

        verify(leadRepository).findOne(any<Specification<Lead>>())
        // nenhum outro metodo do repositorio (ex.: derivado, que ignora o escopo) pode ser usado
        verifyNoMoreInteractions(leadRepository)
    }

    @Test
    fun `RN01 - lead inexistente ou fora do escopo lanca NotFoundException e nao envia dados ao insight`() {
        whenever(leadRepository.findOne(any<Specification<Lead>>())).thenReturn(Optional.empty())

        assertThrows(NotFoundException::class.java) { service.receiveLead(dto) }

        // pos-condicao do caso de uso: o e-mail (passo 3) ja foi enviado
        verify(emailService).sendLeadReceived(dto, "equipe@imobiliaria.test")
        verify(insightClient, never()).createInsight(any())
    }

    @Test
    fun `falha no envio do e-mail interrompe o fluxo (sem busca de lead e sem insight)`() {
        doThrow(RuntimeException("smtp indisponivel"))
            .whenever(emailService)
            .sendLeadReceived(any<LeadReceivedNotificationDTO>(), any<String>())

        assertThrows(RuntimeException::class.java) { service.receiveLead(dto) }

        verifyNoInteractions(leadRepository)
        verifyNoInteractions(insightClient)
    }

    @Test
    fun `originLeadId duplicado no escopo propaga erro e nao solicita insight`() {
        whenever(leadRepository.findOne(any<Specification<Lead>>()))
            .thenThrow(IncorrectResultSizeDataAccessException(1, 2))

        assertThrows(IncorrectResultSizeDataAccessException::class.java) { service.receiveLead(dto) }

        verify(emailService).sendLeadReceived(eq(dto), any<String>())
        verify(insightClient, never()).createInsight(any())
    }
}
