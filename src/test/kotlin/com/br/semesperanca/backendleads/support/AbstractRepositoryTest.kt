package com.br.semesperanca.backendleads.support

import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.test.context.ActiveProfiles
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

/**
 * Base dos testes de repositório: só JPA, sem web, sem serviços, sem segurança.
 *
 * Duas coisas obrigatórias para o teste falar com o PostgreSQL de verdade em vez
 * de um banco em memória:
 *
 * - `@AutoConfigureTestDatabase(replace = NONE)` — sem isso o Boot troca o
 *   `DataSource` por um embedded, e o schema do Flyway nem existe lá. O
 *   resultado seria um H2 vazio que não valida nada do PostgreSQL.
 * - o perfil "test" e o container de [AbstractPostgresTest], que é quem resolve
 *   os placeholders de `application.yml` e alimenta o `DataSource`.
 *
 * `@DataJpaTest` já embrulha cada teste em transação com rollback, então os
 * `save` de cada método não vazam para o próximo.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
abstract class AbstractRepositoryTest : AbstractPostgresTest() {
    @Autowired
    protected lateinit var entityManager: EntityManager

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

    private companion object {
        val cnpjSequence = AtomicInteger()
    }
}
