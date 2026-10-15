package com.br.semesperanca.backendleads.domain.repository

import com.br.semesperanca.backendleads.domain.model.Lead
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * `Lead` é a única entidade do projeto cuja busca por identificador de origem
 * (`originLeadId`, o id do portal de leads) foi **removida** do repositório.
 *
 * A busca existia porque o `InsightClient` — `@Async`, portanto sem
 * `SecurityContext` — recebia o `originLeadId` do corpo do webhook e resolvia o
 * lead por conta própria. Método derivado não passa pela
 * [ScopedRepositoryImpl], então a saída era sem escopo: qualquer usuário
 * autenticado nomeava o `originLeadId` de outra agência e os dados de contato
 * desse lead iam para o serviço de Insight sem que nada no código recusasse.
 *
 * Hoje o caminho é [com.br.semesperanca.backendleads.domain.service.LeadService.receiveLead]:
 * a resolução acontece na thread do request, por `findOne(spec)`, que a base
 * escopada soma à subárvore da role. Não reintroduza aqui um `findBy…` derivado
 * por `originLeadId` — se a busca precisa acontecer fora do request, o
 * identificador tem de ser o `id` e a consulta tem de ser um `Specification`.
 */
@Repository
interface LeadRepository : ScopedRepository<Lead, UUID>