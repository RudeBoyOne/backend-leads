package com.br.semesperanca.backendleads.domain.repository

import com.br.semesperanca.backendleads.domain.model.Activity
import com.br.semesperanca.backendleads.domain.model.Lead
import com.br.semesperanca.backendleads.support.AbstractRepositoryTest
import com.br.semesperanca.backendleads.support.TestFixtures
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.hibernate.InstantiationException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import java.time.Instant
import java.util.UUID

/**
 * `Activity` é a entidade mais relacional do projeto: `@ManyToOne` para `lead`
 * (obrigatório) e para `property` (opcional, adicionada no `V011`), e o
 * `activity_date`/`created_at` foram rebaixados de `TIMESTAMPTZ` para
 * `TIMESTAMP` no `V012` — o que casa com `LocalDateTime` e não casaria com
 * `OffsetDateTime`.
 *
 * `lead.activities` é `LAZY`, e `open-in-view: false`, então a coleção só
 * resolve dentro da transação do teste. O teste do cascade prova que o
 * `ON DELETE CASCADE` do banco está de pé.
 */
class ActivityRepositoryTest : AbstractRepositoryTest() {
    @Autowired
    private lateinit var repository: ActivityRepository

    @Autowired
    private lateinit var leadRepository: LeadRepository

    @Autowired
    private lateinit var propertyRepository: PropertyRepository

    private lateinit var lead: Lead

    private lateinit var agencyId: UUID

    @BeforeEach
    fun setUp() {
        agencyId = insertAgency("Imob Atividades")
        lead =
            leadRepository.save(
                TestFixtures.lead(id = null, originLeadId = "OL-1").apply {
                    realEstateAgencyId = agencyId
                    updatedAt = Instant.parse("2026-01-15T10:00:00Z")
                },
            )
    }

    private fun nova(
        lead: Lead = this.lead,
        property: com.br.semesperanca.backendleads.domain.model.Property? = null,
        title: String? = "Visita ao imovel",
        createdAt: java.time.LocalDateTime? = null,
    ): Activity = TestFixtures.activity(id = null, lead = lead, property = property, title = title, createdAt = createdAt)

    @Test
    fun `save preenche o id e o prePersist do createdAt`() {
        val saved = repository.save(nova())

        assertThat(saved.id).isNotNull()
        assertThat(saved.createdAt).isNotNull()
    }

    @Test
    fun `a atividade guarda o id do lead e do imóvel`() {
        val imovel = propertyRepository.save(TestFixtures.property(id = null, realEstateAgencyId = agencyId))

        val saved = repository.save(nova(property = imovel))

        val relida = repository.findById(saved.id!!).get()
        assertThat(relida.lead.id).isEqualTo(lead.id)
        assertThat(relida.property?.id).isEqualTo(imovel.id)
    }

    @Test
    fun `o prePersist preserva um createdAt que já veio preenchido`() {
        val jaDatado = java.time.LocalDateTime.of(2020, 5, 1, 9, 0)

        val saved = repository.save(nova(createdAt = jaDatado))

        assertThat(saved.createdAt).isEqualTo(jaDatado)
    }

    /**
     * Bug latente, e o motivo de `Lead.activities` não poder ser usada.
     *
     * `Activity` não tem construtor no-arg: o parâmetro `lead: Lead` não tem
     * valor default, então o Kotlin não gera o no-arg sintético que o `Lead`
     * ganha (todos os parâmetros dele têm default). Inicializar a coleção
     * `activities` passa pelo `CollectionLoaderSingleKey`, que precisa
     * instanciar uma `Activity` só com a chave antes do `SELECT` — e sem no-arg
     * isso estoura `InstantiationException`.
     *
     * Detalhe que confunde na leitura do código: ler o campo `activities` não
     * estoura nada, devolve o `PersistentBag` vazio. Só a primeira operação que
     * toca os elementos dispara o `SELECT` — por isso o `.size` dentro do
     * `assertThatThrownBy` e não a leitura crua.
     *
     * Nada no serviço usa essa coleção: `ActivityService` filtra por
     * `Specification`, nunca por `lead.activities`. O conserto seria o
     * `kotlin("plugin.jpa")`, que o `build.gradle.kts` proíbe de propósito
     * (linha 26) porque ele também força `allOpen` em tudo que tem `@Entity`.
     * Fica registrado, não corrigido.
     */
    @Test
    fun `a coleção activities do lead não pode ser carregada porque Activity não tem construtor no-arg`() {
        repository.save(nova(title = "Minha 1"))
        repository.save(nova(title = "Minha 2"))
        entityManager.flush()
        entityManager.clear()

        val relido = entityManager.find(Lead::class.java, lead.id!!)

        assertThatThrownBy { relido.activities.size }
            .isInstanceOf(InstantiationException::class.java)
            .hasMessageContaining("No default constructor")
    }

    /**
     * O caminho que o serviço usa: filtrar por `Specification`, o que resolve a
     * chave da atividade a partir da própria linha e não passa pelo
     * `CollectionLoaderSingleKey` — então funciona.
     */
    @Test
    fun `filtrar por Specification traz só as atividades do lead`() {
        val outroLead =
            leadRepository.save(
                TestFixtures.lead(id = null, originLeadId = "OL-2").apply {
                    realEstateAgencyId = agencyId
                    updatedAt = Instant.parse("2026-01-15T10:00:00Z")
                },
            )
        repository.save(nova(title = "Minha 1"))
        repository.save(nova(title = "Minha 2"))
        repository.save(nova(lead = outroLead, title = "Dele"))

        val leadId = lead.id
        val spec = Specification<Activity> { root, _, cb -> cb.equal(root.get<Any>("lead").get<Any>("id"), leadId) }

        assertThat(repository.findAll(spec).map { it.title }).containsExactlyInAnyOrder("Minha 1", "Minha 2")
    }

    @Test
    fun `excluir o lead leva as atividades junto por causa do ON DELETE CASCADE`() {
        repository.save(nova())
        entityManager.flush()
        entityManager.clear()

        leadRepository.deleteById(lead.id!!)
        entityManager.flush()
        entityManager.clear()

        assertThat(repository.count()).isZero()
    }

    @Test
    fun `Specification por título e por lead compõe os dois predicados`() {
        repository.save(nova(title = "Visita tecnica"))
        repository.save(nova(title = "Ligacao de retorno"))
        repository.save(nova(title = "Visita remota"))

        val spec =
            Specification<Activity> { root, _, cb -> cb.like(root.get<String>("title"), "Visita%") }
                .and { root, _, cb -> cb.equal(root.get<Any>("lead"), lead) }

        assertThat(repository.findAll(spec).map { it.title }).containsExactlyInAnyOrder("Visita tecnica", "Visita remota")
    }

    @Test
    fun `ordenar por activityDate do mais novo para o mais antigo`() {
        repository.save(nova(title = "antiga").apply { activityDate = java.time.LocalDateTime.of(2026, 1, 1, 9, 0) })
        repository.save(nova(title = "recente").apply { activityDate = java.time.LocalDateTime.of(2026, 3, 1, 9, 0) })
        repository.save(nova(title = "media").apply { activityDate = java.time.LocalDateTime.of(2026, 2, 1, 9, 0) })

        val spec = Specification<Activity> { _, _, cb -> cb.conjunction() }
        val pagina =
            repository.findAll(
                spec,
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "activityDate")),
            )

        assertThat(pagina.content.map { it.title }).containsExactly("recente", "media", "antiga")
    }

    @Test
    fun `excluir a atividade não mexe no lead`() {
        val saved = repository.save(nova())

        repository.deleteById(saved.id!!)

        assertThat(repository.count()).isZero()
        assertThat(leadRepository.existsById(lead.id!!)).isTrue()
    }
}
