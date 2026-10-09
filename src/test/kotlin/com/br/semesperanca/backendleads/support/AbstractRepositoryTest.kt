package com.br.semesperanca.backendleads.support

import com.br.semesperanca.backendleads.configuration.security.RepositorySecurityConfig
import com.br.semesperanca.backendleads.domain.model.BranchOffice
import com.br.semesperanca.backendleads.domain.model.BranchOwner
import com.br.semesperanca.backendleads.domain.model.Role
import com.br.semesperanca.backendleads.domain.repository.BranchOfficeRepository
import com.br.semesperanca.backendleads.domain.repository.BranchOwnerRepository
import com.br.semesperanca.backendleads.domain.repository.RoleRepository
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ActiveProfiles
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

/**
 * Base dos testes de repositório: só JPA, sem web, sem serviços, sem segurança.
 *
 * Quatro coisas obrigatórias para o teste falar com o PostgreSQL de verdade em
 * vez de um banco em memória:
 *
 * - `@AutoConfigureTestDatabase(replace = NONE)` — sem isso o Boot troca o
 *   `DataSource` por um embedded, e o schema do Flyway nem existe lá. O
 *   resultado seria um H2 vazio que não valida nada do PostgreSQL.
 * - o perfil "test" e o container de [AbstractPostgresTest], que é quem resolve
 *   os placeholders de `application.yml` e alimenta o `DataSource`.
 * - `@Import(RepositorySecurityConfig)` — o slice do `@DataJpaTest` não carrega
 *   `@Configuration` da aplicação, e sem o `@EnableJpaRepositories` o escopo de
 *   role fica **ausente**: os repositórios são proxies comuns e todos os testes
 *   deste arquivo passariam sem exercitar nada do escopo. Importar a configuração
 *   aqui é o que torna estes testes uma verificação de verdade.
 * - um principal no `SecurityContext` — o repositório escopado falha fechado sem
 *   autenticação, então [asRole] dá a cada teste o root por padrão.
 *
 * `@DataJpaTest` já embrulha cada teste em transação com rollback, então os
 * `save` de cada método não vazam para o próximo.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(RepositorySecurityConfig::class)
abstract class AbstractRepositoryTest : AbstractPostgresTest() {
    @Autowired
    protected lateinit var entityManager: EntityManager

    @Autowired
    protected lateinit var roleRepository: RoleRepository

    @Autowired
    protected lateinit var branchOwnerRepository: BranchOwnerRepository

    @Autowired
    protected lateinit var branchOfficeRepository: BranchOfficeRepository

    /**
     * `FULL_ADMIN` por padrão porque é o root: não escopado, e por isso todo teste
     * histórico desta suíte continua válido sem reescrita. Os testes de escopo
     * estreitam com [asRole] dentro do próprio caso.
     */
    @BeforeEach
    fun assumeFullAdmin() {
        asRole(Role.FULL_ADMIN)
    }

    /**
     * Autentica o teste como a role pedida.
     *
     * O id precisa existir em `users` quando o escopo o percorre em subquery —
     * `RoleScope.ownedOffices` casa `branch_owner.id = :id`, e um id que não existe
     * devolve escopo vazio em vez de erro. É por isso que o `UUID` é parâmetro e
     * não um `random()` escondido dentro do helper.
     */
    protected fun asRole(
        role: String,
        userId: UUID = UUID.randomUUID(),
    ) {
        val authentication =
            TestingAuthenticationToken(
                "test",
                "test",
                listOf(
                    SimpleGrantedAuthority("ROLE_$role"),
                    SimpleGrantedAuthority("UID_$userId"),
                ),
            )
        SecurityContextHolder.getContext().authentication = authentication
    }

    /**
     * `real_estate_agency` é uma tabela sem entidade JPA no projeto — o
     * `Property`, o `BranchOwner` e o `Lead` apontam para ela por UUID puro.
     * Inserir a linha por SQL nativo é a única forma de ter uma agência real
     * para satisfazer o `NOT NULL REFERENCES`.
     *
     * `cnpj` e `email` são `UNIQUE` e o `cnpj` tem exatamente 18 colunas, então
     * saem de um contador sequencial em vez de serem derivados do UUID.
     */
    protected fun insertAgency(name: String = "Imobiliária Teste"): UUID {
        val id = UUID.randomUUID()
        val sequence = cnpjSequence.incrementAndGet()
        val digits = "%014d".format(sequence)
        val cnpj = "${digits.take(2)}.${digits.substring(2, 8)}/${digits.substring(8, 12)}-${digits.takeLast(2)}"
        entityManager
            .createNativeQuery(
                """
                INSERT INTO real_estate_agency (id, name, cnpj, email, phone, active)
                VALUES (:id, :name, :cnpj, :email, :phone, TRUE)
                """.trimIndent(),
            ).setParameter("id", id)
            .setParameter("name", name)
            .setParameter("cnpj", cnpj)
            .setParameter("email", "agencia-$sequence@example.com")
            .setParameter("phone", "11900000000")
            .executeUpdate()
        entityManager.clear()
        return id
    }

    /**
     * `users.role_id` é `NOT NULL`, então nenhuma entidade de `User` — gerente,
     * corretor ou responsável — sobrevive sem uma role já persistida. A `V002`
     * semeia `FULL_ADMIN`, `ADMIN`, `MANAGER`, `REAL_ESTATE_AGENT` e
     * `TEAM_AGENT`, e o repositório é quem devolve a linha.
     */
    protected fun insertRole(name: String): Role =
        roleRepository.findByName(name).orElseThrow { IllegalStateException("Role $name não semeada pela V002") }

    /**
     * Responsável persistido, com agência e role. É o piso da hierarquia: uma
     * filial exige um (`branch_office.branch_owner_id` é `NOT NULL`).
     */
    protected fun insertBranchOwner(
        realEstateAgencyId: UUID,
        name: String = "Responsável",
        email: String = "responsavel-${UUID.randomUUID()}@example.com",
        role: String = Role.ADMIN,
    ): BranchOwner =
        branchOwnerRepository.save(
            BranchOwner(phone = "11900000000").apply {
                this.realEstateAgencyId = realEstateAgencyId
                this.name = name
                this.email = email
                this.role = insertRole(role)
                isActive = true
                setPassword("{noop}senha")
            },
        )

    /**
     * Filial persistida.
     *
     * O responsável é novo a cada chamada **por convenção**, não por imposição do
     * banco: `branch_office.branch_owner_id` é `NOT NULL`, mas o índice é comum,
     * então um mesmo responsável pode ter N filiais. Quem tem N filiais passa
     * `owner = inspetor` explicitamente — é assim que o [DataScopeTest] monta as
     * duas filiais de cada agência.
     *
     * O que o banco exige é o nome ser único dentro da agência; os `name`
     * padrão já são distintos por chamada.
     */
    protected fun insertBranchOffice(
        realEstateAgencyId: UUID,
        name: String = "Filial",
        owner: BranchOwner = insertBranchOwner(realEstateAgencyId),
    ): BranchOffice =
        branchOfficeRepository.save(
            BranchOffice(
                realEstateAgencyId = realEstateAgencyId,
                branchOwner = owner,
                name = name,
                address = "Rua das Flores, 100",
                phone = "1133334444",
                active = true,
            ),
        )

    private companion object {
        val cnpjSequence = AtomicInteger()
    }
}
