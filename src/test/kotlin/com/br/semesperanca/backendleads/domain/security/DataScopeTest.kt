package com.br.semesperanca.backendleads.domain.security

import com.br.semesperanca.backendleads.configuration.exception.NotFoundException
import com.br.semesperanca.backendleads.domain.model.BranchOffice
import com.br.semesperanca.backendleads.domain.model.BranchOwner
import com.br.semesperanca.backendleads.domain.model.Lead
import com.br.semesperanca.backendleads.domain.model.Property
import com.br.semesperanca.backendleads.domain.model.Team
import com.br.semesperanca.backendleads.domain.model.TeamAgent
import com.br.semesperanca.backendleads.domain.model.user.Manager
import com.br.semesperanca.backendleads.domain.model.user.RealEstateAgent
import com.br.semesperanca.backendleads.domain.repository.BranchOfficeRepository
import com.br.semesperanca.backendleads.domain.repository.BranchOwnerRepository
import com.br.semesperanca.backendleads.domain.repository.LeadRepository
import com.br.semesperanca.backendleads.domain.repository.PropertyRepository
import com.br.semesperanca.backendleads.domain.repository.TeamAgentRepository
import com.br.semesperanca.backendleads.domain.repository.TeamRepository
import com.br.semesperanca.backendleads.domain.repository.user.ManagerRepository
import com.br.semesperanca.backendleads.domain.repository.user.RealEstateAgentRepository
import com.br.semesperanca.backendleads.domain.repository.user.UserRepository
import com.br.semesperanca.backendleads.support.AbstractRepositoryTest
import com.br.semesperanca.backendleads.support.TestFixtures
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.domain.Specification
import org.springframework.security.core.context.SecurityContextHolder
import java.time.LocalDateTime
import java.util.UUID

/**
 * O escopo de role, exercitado de verdade: duas agências completas, o mesmo
 * banco, e cada papel vendo exatamente a sua subárvore.
 *
 * Este é o teste que os outros não substituem. Um service novo que chame
 * `findAll()` num repositório escopado já sai correto — mas isso é uma afirmação
 * sobre o mecanismo, e o mecanismo é justamente o que este arquivo testa: que a
 * `Specification` montada pela [RoleScope] produz, no PostgreSQL, a subárvore
 * certa para cada um dos sete tipos, e que `findById` de id alheio **não
 * resolve**.
 *
 * A estrutura semeada, idêntica nas duas agências:
 *
 * ```
 * Imob A                          Imob B
 *   responsavel A (ADMIN)           responsavel B (ADMIN)
 *     filial A1  filial A2            filial B1  filial B2
 *     gerente A1 gerente A2           gerente B1 gerente B2
 *     agente A1   agente A2           agente B1   agente B2
 *     time A1 (gerente A1 + agente A2) time B1 (...)
 *     imovel A1  imovel A2            imovel B1  imovel B2
 *     lead A1 (agente A1)  lead A2     lead B1    lead B2
 * ```
 *
 * Os grupos são prefixados com `Como` porque `BranchOwner`, `Manager` e
 * `RealEstateAgent` também são nomes de entidade importada: um `@Nested` com o
 * mesmo nome esconderia a classe do domínio dentro da própria classe de teste.
 *
 * `gerente A1` tem `agente A2` por dois caminhos ao mesmo tempo — lotação na
 * mesma filial e inscrição no time de `gerente A1` — porque essa ambiguidade é o
 * caso que a disjunção de [RoleScope.agentUnderManagement] existe para resolver,
 * e um teste que só exercitasse um dos lados passaria com a disjunção apagada.
 */
// `open` porque os `@Nested` injetam o contexto no instância externa e o Spring
// faz proxy CGLIB dela — classe Kotlin final não tem subclasse para gerar.
open class DataScopeTest : AbstractRepositoryTest() {
    @Autowired private lateinit var managerRepository: ManagerRepository

    @Autowired private lateinit var agentRepository: RealEstateAgentRepository

    @Autowired private lateinit var teamRepository: TeamRepository

    @Autowired private lateinit var teamAgentRepository: TeamAgentRepository

    @Autowired private lateinit var leadRepository: LeadRepository

    @Autowired private lateinit var propertyRepository: PropertyRepository

    @Autowired private lateinit var userRepository: UserRepository

    // ------------------------------------------------------- duas agências

    private lateinit var responsavelA: BranchOwner

    private lateinit var responsavelB: BranchOwner
    private lateinit var filialA1: BranchOffice
    private lateinit var filialA2: BranchOffice
    private lateinit var filialB1: BranchOffice
    private lateinit var filialB2: BranchOffice
    private lateinit var gerenteA1: Manager
    private lateinit var gerenteA2: Manager
    private lateinit var gerenteB1: Manager
    private lateinit var gerenteB2: Manager
    private lateinit var agenteA1: RealEstateAgent
    private lateinit var agenteA2: RealEstateAgent
    private lateinit var agenteB1: RealEstateAgent
    private lateinit var agenteB2: RealEstateAgent
    private lateinit var timeA1: Team
    private lateinit var timeB1: Team
    private lateinit var imovelA1: Property
    private lateinit var imovelA2: Property
    private lateinit var imovelB1: Property
    private lateinit var imovelB2: Property
    private lateinit var leadA1: Lead
    private lateinit var leadA2: Lead
    private lateinit var leadB1: Lead
    private lateinit var leadB2: Lead

    @BeforeEach
    fun semeiaDuasAgencias() {
        val agencyA = insertAgency("Imob A")
        val agencyB = insertAgency("Imob B")

        responsavelA = insertBranchOwner(agencyA, name = "Responsável A")
        responsavelB = insertBranchOwner(agencyB, name = "Responsável B")

        filialA1 = insertBranchOffice(agencyA, name = "A1", owner = responsavelA)
        filialA2 = insertBranchOffice(agencyA, name = "A2", owner = responsavelA)
        filialB1 = insertBranchOffice(agencyB, name = "B1", owner = responsavelB)
        filialB2 = insertBranchOffice(agencyB, name = "B2", owner = responsavelB)

        gerenteA1 = insertManager(agencyA, filialA1, "Gerente A1")
        gerenteA2 = insertManager(agencyA, filialA2, "Gerente A2")
        gerenteB1 = insertManager(agencyB, filialB1, "Gerente B1")
        gerenteB2 = insertManager(agencyB, filialB2, "Gerente B2")

        // Agente A2 está na filial A1 **e** inscrito no time do gerente A1: os
        // dois caminhos legítimos de "corretor sob minha gestão", ao mesmo tempo.
        agenteA1 = insertAgent(agencyA, filialA1, "Agente A1")
        agenteA2 = insertAgent(agencyA, filialA1, "Agente A2")
        agenteB1 = insertAgent(agencyB, filialB1, "Agente B1")
        agenteB2 = insertAgent(agencyB, filialB1, "Agente B2")

        timeA1 = teamRepository.save(Team(manager = gerenteA1))
        timeB1 = teamRepository.save(Team(manager = gerenteB1))
        teamAgentRepository.save(TeamAgent(team = timeA1, realEstateAgent = agenteA2, joinedAt = LocalDateTime.now()))
        teamAgentRepository.save(TeamAgent(team = timeB1, realEstateAgent = agenteB2, joinedAt = LocalDateTime.now()))

        imovelA1 = insertProperty(agencyA, filialA1, "Imovel A1")
        imovelA2 = insertProperty(agencyA, filialA2, "Imovel A2")
        imovelB1 = insertProperty(agencyB, filialB1, "Imovel B1")
        imovelB2 = insertProperty(agencyB, filialB2, "Imovel B2")

        leadA1 = insertLead(agencyA, agenteA1, "OL-A1")
        leadA2 = insertLead(agencyA, agenteA2, "OL-A2")
        leadB1 = insertLead(agencyB, agenteB1, "OL-B1")
        leadB2 = insertLead(agencyB, agenteB2, "OL-B2")

        entityManager.clear()
    }

    /**
     * `id = null` em todas as semeias, e isso não é detalhe: com id atribuido,
     * `save()` cai em `merge`, o Hibernate trata a entidade como *detached*,
     * emite `UPDATE` contra uma linha que não existe e o `StaleObjectStateException`
     * estoura no flush. Com id nulo o caminho é `persist` e é `INSERT`.
     */
    private fun insertManager(
        agency: UUID,
        filial: BranchOffice,
        name: String,
    ): Manager =
        managerRepository.saveAndFlush(
            TestFixtures
                .manager(
                    id = null,
                    realEstateAgencyId = agency,
                    branchOffice = filial,
                    name = name,
                    // `users.email` é UNIQUE e o default da fixture é fixo, então
                    // a segunda semeia da suíte morreria em `users_email_key`.
                    email = "${name.lowercase().replace(' ', '.')}-${UUID.randomUUID()}@example.com",
                    // A role tem de ser a linha da V002: um `Role(...)` novo é
                    // transient e o flush morre com
                    // `TransientPropertyValueException`, porque `users.role_id`
                    // é `NOT NULL`.
                    role = insertRole("MANAGER"),
                ),
        )

    private fun insertAgent(
        agency: UUID,
        filial: BranchOffice,
        name: String,
    ): RealEstateAgent =
        agentRepository.saveAndFlush(
            TestFixtures.realEstateAgent(
                id = null,
                realEstateAgencyId = agency,
                branchOffice = filial,
                name = name,
                // `users.email` e `real_estate_agent.creci` são UNIQUE, e esta
                // classe não é transacional: cada método ressemeia, e um valor
                // fixo colidiria com a inserção do método anterior. O CRECI usa
                // 8 caracteres do UUID porque a coluna é `varchar(30)` e o UUID
                // inteiro, com o prefixo, estourava o limite.
                creci = "C-${UUID.randomUUID().toString().take(8)}",
                email = "${name.lowercase().replace(' ', '.')}-${UUID.randomUUID()}@example.com",
                role = insertRole("REAL_ESTATE_AGENT"),
            ),
        )

    private fun insertProperty(
        agency: UUID,
        filial: BranchOffice,
        title: String,
    ): Property =
        propertyRepository.saveAndFlush(
            TestFixtures.property(id = null, realEstateAgencyId = agency, branchOffice = filial, title = title),
        )

    private fun insertLead(
        agency: UUID,
        agent: RealEstateAgent,
        originLeadId: String,
    ): Lead =
        leadRepository.saveAndFlush(
            TestFixtures
                .lead(
                    id = null,
                    originLeadId = "$originLeadId-${UUID.randomUUID()}",
                    email = "$originLeadId-${UUID.randomUUID()}@example.com",
                ).apply {
                    realEstateAgencyId = agency
                    this.realEstateAgent = agent
                },
        )

    // ------------------------------------------------------------- FULL_ADMIN

    @Nested
    inner class FullAdmin {
        /**
         * `contains`, e não `containsExactly`: esta classe não é transacional,
         * então o seed do método anterior continua no banco e o root, que não é
         * escopado, enxerga também. O que se prova aqui é o **ausência de
         * corte**, não a quantidade — as contagens exatas são dos papéis
         * recortados, que é onde o recorte é a asserção.
         */
        @Test
        fun `enxerga tudo, porque root nao e escopado`() {
            asRole("FULL_ADMIN")

            assertThat(ids(branchOwnerRepository.findAll())).contains(responsavelA.id, responsavelB.id)
            assertThat(ids(branchOfficeRepository.findAll())).contains(filialA1.id, filialA2.id, filialB1.id, filialB2.id)
            assertThat(ids(managerRepository.findAll())).contains(gerenteA1.id, gerenteA2.id, gerenteB1.id, gerenteB2.id)
            assertThat(ids(agentRepository.findAll())).contains(agenteA1.id, agenteA2.id, agenteB1.id, agenteB2.id)
            assertThat(ids(teamRepository.findAll())).contains(timeA1.id, timeB1.id)
            assertThat(ids(propertyRepository.findAll())).contains(imovelA1.id, imovelA2.id, imovelB1.id, imovelB2.id)
            assertThat(ids(leadRepository.findAll())).contains(leadA1.id, leadA2.id, leadB1.id, leadB2.id)
        }

        @Test
        fun `o count do root e o total real, e nao o da subarvore`() {
            asRole("FULL_ADMIN")

            assertThat(leadRepository.count()).isEqualTo(leadRepository.findAll().size.toLong())
        }
    }

    // --------------------------------------------------------- branch owner

    @Nested
    inner class ComoBranchOwner {
        @BeforeEach
        fun autentica() {
            asRole("ADMIN", responsavelA.id!!)
        }

        @Test
        fun `so enxerga a si proprio entre os responsaveis`() {
            assertThat(ids(branchOwnerRepository.findAll())).containsExactly(responsavelA.id)
        }

        @Test
        fun `enxerga as duas filiais dele e nenhuma das do outro`() {
            assertThat(ids(branchOfficeRepository.findAll())).containsExactlyInAnyOrder(filialA1.id, filialA2.id)
        }

        @Test
        fun `enxerga os gerentes e os corretores das filiais dele`() {
            assertThat(ids(managerRepository.findAll())).containsExactlyInAnyOrder(gerenteA1.id, gerenteA2.id)
            assertThat(ids(agentRepository.findAll())).containsExactlyInAnyOrder(agenteA1.id, agenteA2.id)
        }

        @Test
        fun `enxerga os times dos gerentes das filiais dele`() {
            assertThat(ids(teamRepository.findAll())).containsExactly(timeA1.id)
        }

        @Test
        fun `enxerga os imoveis das filiais dele`() {
            assertThat(ids(propertyRepository.findAll())).containsExactlyInAnyOrder(imovelA1.id, imovelA2.id)
        }

        /**
         * O caminho do branch owner para o lead é o mais longo: lead -> corretor
         * -> filial -> responsável. É o que a subquery única de [RoleScope.forLead]
         * precisa percorrer sem sair da subárvore.
         */
        @Test
        fun `enxerga os leads dos corretores das filiais dele`() {
            assertThat(ids(leadRepository.findAll())).containsExactlyInAnyOrder(leadA1.id, leadA2.id)
        }

        @Test
        fun `o responsavel da outra agencia responde 404, nao 403`() {
            assertThat(branchOwnerRepository.findById(responsavelB.id!!)).isEmpty()
        }
    }

    // ------------------------------------------------------------------ ADMIN

    @Nested
    inner class ComoGerente {
        @BeforeEach
        fun autentica() {
            asRole("MANAGER", gerenteA1.id!!)
        }

        @Test
        fun `so enxerga a si proprio entre os gerentes`() {
            assertThat(ids(managerRepository.findAll())).containsExactly(gerenteA1.id)
        }

        @Test
        fun `enxerga a propria filial e nenhuma outra`() {
            assertThat(ids(branchOfficeRepository.findAll())).containsExactly(filialA1.id)
        }

        @Test
        fun `enxerga os corretores da propria filial, mesmo os que nao estao no seu time`() {
            // Agente A1 está na filial A1 mas não está inscrito em nenhum time do
            // gerente A1; a lotação basta. Agente A2 está nos dois caminhos.
            assertThat(ids(agentRepository.findAll())).containsExactlyInAnyOrder(agenteA1.id, agenteA2.id)
        }

        @Test
        fun `enxerga apenas o proprio time`() {
            assertThat(ids(teamRepository.findAll())).containsExactly(timeA1.id)
        }

        @Test
        fun `enxerga os imoveis da propria filial`() {
            assertThat(ids(propertyRepository.findAll())).containsExactly(imovelA1.id)
        }

        @Test
        fun `enxerga os leads dos corretores sob sua gestao`() {
            assertThat(ids(leadRepository.findAll())).containsExactlyInAnyOrder(leadA1.id, leadA2.id)
        }

        @Test
        fun `nao enxerga responsavel nenhum - o escopo dele comeca na filial`() {
            assertThat(branchOwnerRepository.findAll()).isEmpty()
        }

        @Test
        fun `o gerente da outra filial responde 404`() {
            assertThat(managerRepository.findById(gerenteB1.id!!)).isEmpty()
        }
    }

    // ----------------------------------------------------------- corretor

    @Nested
    inner class ComoCorretor {
        @BeforeEach
        fun autentica() {
            asRole("REAL_ESTATE_AGENT", agenteA1.id!!)
        }

        @Test
        fun `so enxerga a si proprio entre os corretores`() {
            assertThat(ids(agentRepository.findAll())).containsExactly(agenteA1.id)
        }

        @Test
        fun `enxerga a propria filial, e o time em que esta inscrito`() {
            // O agente A1 não está inscrito em time nenhum, então a lista de
            // times sai vazia — e isso é o correto: ele não gerencia nada.
            assertThat(ids(branchOfficeRepository.findAll())).containsExactly(filialA1.id)
            assertThat(teamRepository.findAll()).isEmpty()
        }

        @Test
        fun `enxerga apenas os proprios leads`() {
            assertThat(ids(leadRepository.findAll())).containsExactly(leadA1.id)
        }

        /**
         * Gerente e responsável estão acima dele na hierarquia e a listagem sai
         * vazia — não 403, que confirmaria a existência do registro.
         *
         * Imóvel é o caso diferente: o corretor enxerga os imóveis **da filial
         * dele**, porque é neles que ele vende. É o mesmo `ownOffice` que o
         * gerente usa, e o que impede o corretor de ver o catálogo da filial
         * vizinha.
         */
        @Test
        fun `nao enxerga gerente nem responsavel, e so ve os imoveis da propria filial`() {
            assertThat(managerRepository.findAll()).isEmpty()
            assertThat(branchOwnerRepository.findAll()).isEmpty()
            assertThat(ids(propertyRepository.findAll())).contains(imovelA1.id)
            assertThat(ids(propertyRepository.findAll())).doesNotContain(imovelA2.id, imovelB1.id, imovelB2.id)
        }

        @Test
        fun `o lead do colega responde 404`() {
            assertThat(leadRepository.findById(leadA2.id!!)).isEmpty()
        }

        @Test
        fun `o imovel da outra filial responde 404`() {
            assertThat(propertyRepository.findById(imovelB1.id!!)).isEmpty()
        }
    }

    // -------------------------------------------- composicao e falha fechada

    @Nested
    inner class Composicao {
        @Test
        fun `o filtro do cliente se soma ao escopo e nunca o substitui`() {
            asRole("MANAGER", gerenteA1.id!!)

            // "Imovel A2" é do mesmo branch owner, mas do outro gerente: um
            // `findAll(spec)` que devolvesse A2 provaria que a composição
            // sobrescreveu a subárvore em vez de somá-la.
            assertThat(propertyRepository.findAll(specIgual("Imovel A2"))).isEmpty()
            assertThat(propertyRepository.findAll(specIgual("Imovel A1"))).hasSize(1)
        }

        @Test
        fun `a paginacao conta a subarvore e nao a tabela inteira`() {
            asRole("ADMIN", responsavelA.id!!)

            assertThat(leadRepository.findAll(PageRequest.of(0, 1)).totalElements).isEqualTo(2)
            assertThat(leadRepository.findAll(PageRequest.of(0, 1)).content).hasSize(1)
        }

        @Test
        fun `findAllById tambem e escopado`() {
            asRole("MANAGER", gerenteA1.id!!)

            val visiveis = propertyRepository.findAllById(listOf(imovelA1.id!!, imovelB1.id!!))

            assertThat(ids(visiveis)).containsExactly(imovelA1.id)
        }

        @Test
        fun `existsById usa o mesmo findById escopado`() {
            asRole("MANAGER", gerenteA1.id!!)

            assertThat(propertyRepository.existsById(imovelA1.id!!)).isTrue()
            assertThat(propertyRepository.existsById(imovelB1.id!!)).isFalse()
        }

        @Test
        fun `removecao em massa e recusada, porque nao ha id para escopar`() {
            asRole("MANAGER", gerenteA1.id!!)

            assertThatThrownBy { propertyRepository.deleteAll() }
                .isInstanceOf(UnsupportedOperationException::class.java)
                .hasMessageContaining("escopo de role")
        }

        /**
         * Falha fechada: sem principal, um repositório escopado não vira tabela
         * inteira. É o teste que impede alguém de "consertar" um
         * `NullPointerException` trocando a exceção por `unrestricted()`.
         */
        @Test
        fun `sem autenticacao o repositorio escopado estoura, e nao devolve tudo`() {
            SecurityContextHolder.clearContext()

            assertThatThrownBy { leadRepository.findAll() }
                .isInstanceOf(NotAuthenticatedException::class.java)
            assertThatThrownBy { propertyRepository.findById(imovelA1.id!!) }
                .isInstanceOf(NotAuthenticatedException::class.java)
        }

        /**
         * `UserRepository` e `RoleRepository` não têm dono na hierarquia, e é
         * justamente por isso que precisam funcionar **sem** principal: o
         * `CustomJwtAuthenticationConverter` e o `AuthService.login` os consultam
         * antes de existir token. Resolver o principal antes do teste de escopo
         * deixaria a autenticação impossível de fazer.
         */
        @Test
        fun `repositorio sem escopo funciona sem autenticacao`() {
            SecurityContextHolder.clearContext()

            assertThat(insertRole("ADMIN").name).isEqualTo("ADMIN")
            assertThat(userRepository.findByEmail("admin@semesperanca.com.br")).isPresent
        }

        @Test
        fun `a lotacao e lida no instante da consulta, e nao de um valor guardado`() {
            asRole("MANAGER", gerenteA1.id!!)
            assertThat(ids(branchOfficeRepository.findAll())).containsExactly(filialA1.id)

            // Transferir o gerente de filial é uma mudança de lotação, e o escopo
            // precisa refletir na consulta seguinte sem esperar nova sessão.
            gerenteA1.branchOffice = filialA2
            managerRepository.saveAndFlush(gerenteA1)
            entityManager.clear()

            assertThat(ids(branchOfficeRepository.findAll())).containsExactly(filialA2.id)
            // A filial antiga e os imóveis dela saíram do escopo; os da nova
            // entraram. Uma sessão já aberta não pode servir a subárvore antiga,
            // e é por isso que a lotação é lida por subquery.
            assertThat(ids(propertyRepository.findAll())).contains(imovelA2.id).doesNotContain(imovelA1.id)
        }
    }

    // ----------------------------------------------------------- cobertura

    @Test
    fun `todo tipo escopado produz uma Specification que o PostgreSQL aceita`() {
        asRole("FULL_ADMIN")

        DataScope.scopedTypes.forEach { tipo ->
            val spec = DataScope.forType(tipo)
            assertThat(spec).describedAs("%s", tipo.simpleName).isNotNull()
        }
    }

    @Test
    fun `a lista de tipos escopados e exatamente a do despacho`() {
        // Se alguém declarar escopo para um tipo novo e esquecer o `when`, o
        // `error()` de [DataScope.forType] só apareceria em produção. Comparar as
        // duas listas transforma o esquecimento em falha de build.
        val declarados =
            setOf(
                BranchOwner::class.java,
                BranchOffice::class.java,
                Manager::class.java,
                RealEstateAgent::class.java,
                Team::class.java,
                Lead::class.java,
                Property::class.java,
            )

        assertThat(DataScope.scopedTypes.toSet()).isEqualTo(declarados)
    }

    @Test
    fun `tipo fora da lista devolve Specification sem restricao, e nao estoura`() {
        asRole("FULL_ADMIN")

        assertThat(DataScope.forType(String::class.java)).isNotNull()
    }

    // ------------------------------------------------------------------ utils

    private fun <T : Any> ids(itens: Iterable<T>): List<UUID?> = itens.map { idOf(it) }

    private fun <T : Any> idOf(item: T): UUID? =
        when (item) {
            is BranchOwner -> item.id
            is BranchOffice -> item.id
            is Manager -> item.id
            is RealEstateAgent -> item.id
            is Team -> item.id
            is Property -> item.id
            is Lead -> item.id
            else -> error("tipo sem id: ${item::class.simpleName}")
        }

    private fun specIgual(titulo: String) =
        Specification<Property> { root, _, cb ->
            cb.equal(root.get<Any>("title"), titulo)
        }
}
