package com.br.semesperanca.backendleads.domain.repository

import com.br.semesperanca.backendleads.domain.model.Lead
import com.br.semesperanca.backendleads.support.AbstractRepositoryTest
import com.br.semesperanca.backendleads.support.TestFixtures
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import java.time.Instant
import java.util.Optional
import java.util.UUID

/**
 * `LeadRepository` carrega a única chave de negócio do domínio:
 * `origin_lead_id` é o identificador do portal e a idempotência do
 * A busca por `originLeadId` usada pelo `LeadService` é uma `Specification`, e
 * não um método derivado: só a primeira passa pela `ScopedRepositoryImpl`.
 *
 * `lead.status` tem `CHECK IN ('new','in_progress','converted','lost','archived')`
 * — um teste que passe um status fora da lista pega na hora se essa constraint
 * sumir de uma migration.
 */
class LeadRepositoryTest : AbstractRepositoryTest() {
    @Autowired
    private lateinit var repository: LeadRepository

    @Autowired
    private lateinit var activityRepository: ActivityRepository

    private lateinit var agencyId: UUID

    @BeforeEach
    fun setUp() {
        agencyId = insertAgency("Imob Leads")
    }

    /**
     * `lead.created_at` e `lead.updated_at` são `NOT NULL DEFAULT NOW()`, e o
     * `DEFAULT` só vale quando a coluna é omitida do `INSERT` — o Hibernate
     * sempre manda toda coluna mapeada, então o default só não é anulado
     * porque o `Lead` tem `@PrePersist` que preenche os dois.
     *
     * Além disso, `V006` instala trigger de `updated_at` em `users`,
     * `real_estate_agent` e `manager`, **não** em `lead` — o `@PreUpdate` é o
     * que existe por isso.
     */
    /**
     * Busca por `originLeadId` na forma que a aplicação usa: `Specification` via
     * `findOne`, que a [ScopedRepositoryImpl] soma à subárvore da role. Um
     * `findByOriginLeadId` derivado ignoraria o escopo e não apareceria aqui.
     */
    private fun buscaPorOrigin(originLeadId: String): Optional<Lead> =
        repository.findOne(Specification<Lead> { root, _, cb -> cb.equal(root.get<Any>("originLeadId"), originLeadId) })

    private fun porOrigin(originLeadId: String): Lead = buscaPorOrigin(originLeadId).get()

    private fun novo(
        originLeadId: String = "OL-1",
        status: String = "new",
        name: String = "Maria Silva",
        leadOrigin: String = "Zapimoveis",
        temperature: String = "hot",
        source: String? = "portal",
    ): Lead =
        TestFixtures
            .lead(
                id = null,
                originLeadId = originLeadId,
                status = status,
                name = name,
                leadOrigin = leadOrigin,
                temperature = temperature,
            ).apply {
                realEstateAgencyId = agencyId
                this.source = source
                updatedAt = Instant.parse("2026-01-15T10:00:00Z")
            }

    @Test
    fun `save preenche o id e persiste a agência como FK`() {
        val saved = repository.save(novo())

        assertThat(saved.id).isNotNull()
        assertThat(saved.realEstateAgencyId).isEqualTo(agencyId)
    }

    /**
     * Armadilha do schema: a coluna tem `DEFAULT NOW()`, então o `INSERT` parece
     * ter tudo certo, mas o Hibernate sobrescreve o default com o `NULL` do
     * campo. Sem `@PrePersist` em `Lead`, o `save` estourava
     * `DataIntegrityViolationException` — o `@PrePersist` é o que segura isso.
     */
    @Test
    fun `save com updatedAt nulo preenche o timestamp no prePersist`() {
        val semTimestamp = novo(originLeadId = "OL-sem-data").apply { updatedAt = null }

        val saved = repository.saveAndFlush(semTimestamp)

        assertThat(saved.id).isNotNull()
        assertThat(saved.updatedAt).isNotNull()
    }

    @Test
    fun `prePersist preserva um createdAt que já veio preenchido`() {
        val ditado = Instant.parse("2020-01-01T00:00:00Z")
        val lead = novo(originLeadId = "OL-ditado").apply { createdAt = ditado }

        val saved = repository.saveAndFlush(lead)

        assertThat(saved.createdAt).isEqualTo(ditado)
    }

    /**
     * `lead` não tem trigger de `updated_at` (o `V006` só cobre `users`,
     * `real_estate_agent` e `manager`), então o `@PreUpdate` é a única coisa que
     * atualiza a coluna — sem ele, `updated_at` ficaria para sempre no valor
     * do `INSERT`.
     */
    @Test
    fun `preUpdate mexe no updatedAt e deixa o createdAt intacto`() {
        val lead = repository.saveAndFlush(novo(originLeadId = "OL-update"))
        val criado = lead.createdAt!!
        lead.updatedAt = Instant.parse("2020-01-01T00:00:00Z")
        lead.name = "Maria Souza"
        entityManager.flush()
        entityManager.clear()

        val relido = porOrigin("OL-update")
        assertThat(relido.createdAt).isEqualTo(criado)
        assertThat(relido.updatedAt).isAfter(Instant.parse("2020-01-01T00:00:00Z"))
    }

    @Test
    fun `a coleção activities do lead carrega as atividades dele`() {
        val lead = repository.saveAndFlush(novo(originLeadId = "OL-com-atividades"))
        activityRepository.save(TestFixtures.activity(id = null, lead = lead, title = "Visita tecnica", createdAt = null))
        activityRepository.flush()
        entityManager.clear()

        val relido = entityManager.find(Lead::class.java, lead.id!!)

        assertThat(relido.activities.map { it.title }).containsExactly("Visita tecnica")
    }

    /**
     * A busca por `originLeadId` é `Specification`, e não método derivado, e a
     * diferença é o escopo: `findByOriginLeadId` era exatamente o que permitia ao
     * `InsightClient` — que roda `@Async`, sem `SecurityContext` — despachar o
     * lead de qualquer agência para o serviço de Insight só por causa do
     * `originLeadId` que vinha no corpo do webhook. `findOne(spec)` passa pela
     * [ScopedRepositoryImpl], que soma a subárvore da role.
     */
    @Test
    fun `busca por originLeadId acha o lead`() {
        repository.save(novo(originLeadId = "OL-7", name = "Joana"))

        assertThat(porOrigin("OL-7").name).isEqualTo("Joana")
    }

    @Test
    fun `busca por originLeadId de lead inexistente devolve vazio`() {
        assertThat(buscaPorOrigin("OL-inexistente")).isEmpty()
    }

    /**
     * `lead.status` tem `CHECK IN ('new','in_progress','converted','lost','archived')`.
     * A validação do `UpdateLeadRequest` no controller cobre metade do problema;
     * isto cobre a outra metade — nada entra no banco com status fora da lista,
     * nem por SQL direto.
     */
    @Test
    fun `status fora do CHECK é recusado pelo próprio banco`() {
        assertThatThrownBy { repository.saveAndFlush(novo(originLeadId = "OL-ruim", status = "nao-existe")) }
            .isInstanceOf(DataIntegrityViolationException::class.java)
    }

    @Test
    fun `status dentro do CHECK é aceito`() {
        repository.saveAndFlush(novo(originLeadId = "OL-ok", status = "converted"))

        assertThat(porOrigin("OL-ok").status).isEqualTo("converted")
    }

    @Test
    fun `Specification por status e origem compõe os dois predicados`() {
        repository.save(novo(originLeadId = "A", status = "new", leadOrigin = "Zapimoveis"))
        repository.save(novo(originLeadId = "B", status = "new", leadOrigin = "Site"))
        repository.save(novo(originLeadId = "C", status = "converted", leadOrigin = "Zapimoveis"))

        val spec = statusSpec("new").and(originSpec("Zapimoveis"))

        assertThat(repository.findAll(spec).map { it.originLeadId }).containsExactly("A")
    }

    @Test
    fun `Specification por temperatura com paginação e ordenação`() {
        repository.save(novo(originLeadId = "A", temperature = "hot"))
        repository.save(novo(originLeadId = "B", temperature = "hot"))
        repository.save(novo(originLeadId = "C", temperature = "cold"))

        val spec = temperatureSpec("hot")
        val pagina = repository.findAll(spec, PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "originLeadId")))

        assertThat(pagina.content.map { it.originLeadId }).containsExactly("A", "B")
        assertThat(pagina.totalElements).isEqualTo(2)
    }

    @Test
    fun `busca textual por nome ignora maiúsculas e minúsculas`() {
        repository.save(novo(originLeadId = "A", name = "Maria Aparecida"))
        repository.save(novo(originLeadId = "B", name = "João"))

        val spec = Specification<Lead> { root, _, cb -> cb.like(cb.lower(root.get<String>("name")), "%maria%") }

        assertThat(repository.findAll(spec).map { it.originLeadId }).containsExactly("A")
    }

    /**
     * `LeadService.findLeads` sempre começa de `cb.conjunction()`, nunca de null
     * — e o Spring Data 4 rejeita null na borda do repositório. Passar null
     * "para pegar tudo" não funciona mais; `findAll(pageable)` é o caminho.
     */
    @Test
    fun `spec nulo é rejeitado pelo JpaSpecificationExecutor`() {
        repository.save(novo(originLeadId = "A"))
        repository.save(novo(originLeadId = "B"))

        assertThatThrownBy { repository.findAll(null as Specification<Lead>?, PageRequest.of(0, 10)) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("must not be null")
    }

    @Test
    fun `findAll sem spec devolve a lead inteira`() {
        repository.save(novo(originLeadId = "A"))
        repository.save(novo(originLeadId = "B"))

        assertThat(repository.findAll(PageRequest.of(0, 10)).totalElements).isEqualTo(2)
    }

    @Test
    fun `deleteById remove o lead`() {
        val saved = repository.save(novo())

        repository.deleteById(saved.id!!)

        assertThat(repository.existsById(saved.id!!)).isFalse()
    }

    private fun statusSpec(status: String): Specification<Lead> =
        Specification { root, _, cb -> cb.equal(root.get<String>("status"), status) }

    private fun originSpec(leadOrigin: String): Specification<Lead> =
        Specification { root, _, cb -> cb.equal(root.get<String>("leadOrigin"), leadOrigin) }

    private fun temperatureSpec(temperature: String): Specification<Lead> =
        Specification { root, _, cb -> cb.equal(root.get<String>("temperature"), temperature) }
}
