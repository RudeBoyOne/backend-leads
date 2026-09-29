package com.br.semesperanca.backendleads.domain.repository.user

import com.br.semesperanca.backendleads.domain.model.Role
import com.br.semesperanca.backendleads.domain.model.user.Manager
import com.br.semesperanca.backendleads.domain.model.user.RealEstateAgent
import com.br.semesperanca.backendleads.domain.repository.RoleRepository
import com.br.semesperanca.backendleads.support.AbstractRepositoryTest
import com.br.semesperanca.backendleads.support.TestFixtures
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.util.UUID

/**
 * `User` é a única entidade com herança `JOINED`: o `save` de um `RealEstateAgent`
 * escreve em `"users"` **e** em `real_estate_agent`, e o `findByEmail` no
 * `UserRepository` devolve qualquer um dos subtipos.
 *
 * Os dois `findByEmail` (`ManagerRepository` e `RealEstateAgentRepository`) só
 *BATEM no subtipo correto, porque o JPQL derivado inclui o `TYPE(...)` da
 * classe raiz — e é por isso que os dois e-mails abaixo são propositalmente
 * iguais: se a herança estivesse mal configurada, um `findByEmail` devolveria o
 * usuário errado em vez de Optional vazio.
 *
 * `users.role_id` é `NOT NULL`, então todo agente/gerente precisa de um `Role`
 * de verdade — os papéis do `V001` servem.
 */
class UserRepositoryTest : AbstractRepositoryTest() {
    @Autowired
    private lateinit var repository: UserRepository

    @Autowired
    private lateinit var roleRepository: RoleRepository

    @Autowired
    private lateinit var managerRepository: ManagerRepository

    @Autowired
    private lateinit var agentRepository: RealEstateAgentRepository

    private lateinit var roleAgente: Role

    private lateinit var roleGerente: Role

    @BeforeEach
    fun setUp() {
        roleAgente = roleRepository.findByName("REAL_ESTATE_AGENT").get()
        roleGerente = roleRepository.findByName("MANAGER").get()
    }

    private fun agente(
        email: String = "ana-${UUID.randomUUID().toString().take(8)}@example.com",
        creci: String = "CRECI-${UUID.randomUUID().toString().take(8)}",
        isActive: Boolean = true,
    ): RealEstateAgent =
        TestFixtures
            .realEstateAgent(
                id = null,
                creci = creci,
                email = email,
                role = roleAgente,
                isActive = isActive,
                password = BCryptPasswordEncoder().encode("segredo"),
                createdAt = null,
            )

    private fun gerente(email: String = "bruno-${UUID.randomUUID().toString().take(8)}@example.com"): Manager =
        TestFixtures
            .manager(
                id = null,
                email = email,
                role = roleGerente,
                password = BCryptPasswordEncoder().encode("segredo"),
                createdAt = null,
            )

    @Test
    fun `save escreve nas duas tabelas da herança JOINED`() {
        val salvo = repository.save(agente())

        assertThat(salvo.id).isNotNull()
        assertThat(salvo.createdAt).isNotNull()
        assertThat(salvo.updatedAt).isNotNull()
        assertThat(entidadeNaTabelaFilha("real_estate_agent", salvo.id!!)).isTrue()
        assertThat(entidadeNaTabelaFilha("manager", salvo.id!!)).isFalse()
    }

    @Test
    fun `o prePersist do User ignora o createdAt que veio preenchido`() {
        val velho = java.time.LocalDateTime.of(2020, 1, 1, 8, 0)

        val salvo = repository.save(agente().apply { createdAt = velho })

        assertThat(salvo.createdAt).isNotEqualTo(velho)
    }

    @Test
    fun `findByEmail acha qualquer subtipo de User`() {
        val email = "comum-${UUID.randomUUID().toString().take(8)}@example.com"
        repository.save(gerente(email))

        val encontrado = repository.findByEmail(email)

        assertThat(encontrado).isPresent
        assertThat(encontrado.get()).isInstanceOf(Manager::class.java)
        assertThat(encontrado.get().id).isNotNull()
    }

    @Test
    fun `findByEmail e case sensitive porque a coluna é unica e não tem índice por lower`() {
        val email = "CaSe-${UUID.randomUUID().toString().take(8)}@example.com"
        repository.save(agente(email = email))

        assertThat(repository.findByEmail(email)).isPresent
        assertThat(repository.findByEmail(email.lowercase())).isEmpty
    }

    @Test
    fun `existsByEmail distingue cadastro novo de e-mail já usado`() {
        val email = "existe-${UUID.randomUUID().toString().take(8)}@example.com"

        assertThat(repository.existsByEmail(email)).isFalse()
        repository.save(agente(email = email))
        assertThat(repository.existsByEmail(email)).isTrue()
    }

    @Test
    fun `email duplicado é recusado pela constraint UNIQUE`() {
        val email = "dup-${UUID.randomUUID().toString().take(8)}@example.com"
        repository.saveAndFlush(agente(email = email))

        org.assertj.core.api.Assertions
            .assertThatThrownBy { repository.saveAndFlush(agente(email = email, creci = "OUTRO-CRECI")) }
            .isInstanceOf(org.springframework.dao.DataIntegrityViolationException::class.java)
    }

    @Test
    fun `o findByEmail do RealEstateAgentRepository só devolve corretores`() {
        val email = "misto-${UUID.randomUUID().toString().take(8)}@example.com"
        repository.save(gerente(email))

        assertThat(agentRepository.findByEmail(email)).isEmpty()
        assertThat(managerRepository.findByEmail(email)).isPresent
    }

    @Test
    fun `o findByEmail do ManagerRepository só devolve gerentes`() {
        val email = "so-agente-${UUID.randomUUID().toString().take(8)}@example.com"
        repository.save(agente(email = email))

        assertThat(managerRepository.findByEmail(email)).isEmpty()
        assertThat(agentRepository.findByEmail(email)).isPresent
    }

    @Test
    fun `existsByCreci detecta corretor já cadastrado`() {
        val creci = "CRECI-${UUID.randomUUID().toString().take(8)}"

        assertThat(agentRepository.existsByCreci(creci)).isFalse()
        repository.save(agente(creci = creci))
        assertThat(agentRepository.existsByCreci(creci)).isTrue()
    }

    /**
     * Bug de schema, não de teste. A entidade declara
     * `@Column(creci, nullable = false, unique = true)`, mas o `V002` cria a
     * coluna como `VARCHAR(30)` **sem** `UNIQUE` — e `ddl-auto: validate` só
     * confere existência e tipo de coluna, nunca constraint. Então dois
     * corretores podem acabar com o mesmo CREVI no banco.
     *
     * A única barreira é o `existsByCreci` do `RealEstateAgentService.create`,
     * que é uma checagem de aplicação: duas requisições simultâneas passam
     * juntas. Fica registrado aqui, não corrigido, porque mexer em migration
     * num schema já em produção é decisão de quem mantém o banco.
     */
    @Test
    fun `creci repetido é aceito porque o V002 não criou a constraint UNIQUE`() {
        val creci = "CRECI-${UUID.randomUUID().toString().take(8)}"
        repository.saveAndFlush(agente(creci = creci))

        val segundo = repository.saveAndFlush(agente(creci = creci))

        assertThat(agentRepository.findAll().count { it.creci == creci }).isEqualTo(2)
        assertThat(segundo.id).isNotNull()
    }

    @Test
    fun `o papel do corretor sobrevive ao round trip pelo banco`() {
        val salvo = repository.save(agente())

        val relido = repository.findByEmail(salvo.email!!).get()

        assertThat(relido.role?.name).isEqualTo("REAL_ESTATE_AGENT")
        assertThat(relido.authorities.map { it.authority }).containsExactly("ROLE_REAL_ESTATE_AGENT")
    }

    @Test
    fun `inativar o corretor reflete em isEnabled e na tabela`() {
        val salvo = repository.save(agente())

        salvo.isActive = false
        repository.saveAndFlush(salvo)
        entityManager.clear()

        assertThat(repository.findById(salvo.id!!).get().isEnabled).isFalse()
    }

    @Test
    fun `o branchOffice do gerente fica só na tabela manager`() {
        val salvo = repository.save(gerente())
        entityManager.flush()
        entityManager.clear()

        val relido = managerRepository.findByEmail(salvo.email!!).get()

        assertThat(relido.branchOffice).isEqualTo("Filial Centro")
        assertThat(entidadeNaTabelaFilha("manager", salvo.id!!)).isTrue()
        assertThat(entidadeNaTabelaFilha("real_estate_agent", salvo.id!!)).isFalse()
    }

    /**
     * O `V007` semeia dois `FULL_ADMIN` reais, então a tabela `users` já começa
     * com 2 linhas — `count` nunca é zero aqui. Este teste é a prova de que o
     * seed sobreviveu às 13 migrations: sem ele, um `authService.login` com o
     * e-mail do admin cairia no `Optional.empty`.
     */
    @Test
    fun `o seed do V007 criou os dois admins com papel FULL_ADMIN`() {
        val admin = repository.findByEmail("admin@semesperanca.com.br")

        assertThat(admin).isPresent
        assertThat(admin.get().role?.name).isEqualTo("FULL_ADMIN")
        assertThat(admin.get().isActive).isTrue()
        assertThat(admin.get().password).startsWith("$2a$")
        assertThat(repository.findByEmail("mrRobo@gmail.com")).isPresent
    }

    @Test
    fun `findAll devolve os dois subtipos juntos porque a raiz é a tabela users`() {
        val antes = repository.count()
        repository.save(agente())
        repository.save(gerente())

        assertThat(repository.count()).isEqualTo(antes + 2)
        assertThat(repository.findAll().map { it::class.java.simpleName })
            .contains("RealEstateAgent", "Manager")
    }

    @Test
    fun `excluir o usuário apaga também a linha da tabela filha`() {
        val salvo = repository.save(agente())

        repository.deleteById(salvo.id!!)
        entityManager.flush()
        entityManager.clear()

        assertThat(entidadeNaTabelaFilha("real_estate_agent", salvo.id!!)).isFalse()
        assertThat(repository.existsById(salvo.id!!)).isFalse()
    }

    private fun entidadeNaTabelaFilha(
        tabela: String,
        id: UUID,
    ): Boolean =
        entityManager
            .createNativeQuery("SELECT COUNT(*) FROM $tabela WHERE id = :id")
            .setParameter("id", id)
            .getSingleResult() as Long > 0
}
