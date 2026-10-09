package com.br.semesperanca.backendleads.domain.model.user

import com.br.semesperanca.backendleads.support.TestFixtures
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * `User` implementa `UserDetails` e e a entidade que o Spring Security consulta
 * no login. As regras sao fixas — papel vira `ROLE_`, conta bloqueada nunca
 * acontece — e sao exatamente o que a `CustomJwtAuthenticationConverter` assume
 * quando monta as autoridades.
 */
class UserDetailsTest {
    @Test
    fun `autoridade vem do papel com o prefixo ROLE`() {
        val agent = TestFixtures.realEstateAgent(role = TestFixtures.role(name = "MANAGER"))

        assertThat(agent.authorities.map { it.authority }).containsExactly("ROLE_MANAGER")
    }

    /**
     * Sem papel a lista sai vazia, e não com a autoridade literal `"ROLE_null"`,
     * que nunca casaria com nenhuma regra de `hasRole` e ainda vaza no log de
     * auditoria como se fosse um papel real. É o mesmo comportamento da
     * `CustomJwtAuthenticationConverter`, o outro caminho de autenticação.
     */
    @Test
    fun `usuario sem papel nao ganha nenhuma autoridade`() {
        val agent = TestFixtures.bareUser(role = null)

        assertThat(agent.authorities).isEmpty()
    }

    @Test
    fun `username e o email`() {
        assertThat(TestFixtures.bareUser(email = "ana@example.com").username).isEqualTo("ana@example.com")
    }

    @Test
    fun `username de usuario sem email e string vazia em vez de null`() {
        assertThat(TestFixtures.bareUser(email = null).username).isEmpty()
    }

    @Test
    fun `isEnabled espelha o campo isActive`() {
        assertThat(TestFixtures.bareUser(isActive = true).isEnabled).isTrue()
        assertThat(TestFixtures.bareUser(isActive = false).isEnabled).isFalse()
    }

    @Test
    fun `conta nunca expira, nunca e bloqueada e a senha nunca expira`() {
        val user = TestFixtures.bareUser(isActive = false)

        assertThat(user.isAccountNonExpired).isTrue()
        assertThat(user.isAccountNonLocked).isTrue()
        assertThat(user.isCredentialsNonExpired).isTrue()
    }

    @Test
    fun `senha vai e vem pelo par set e get`() {
        val agent = TestFixtures.realEstateAgent(password = "hash-bcrypt")

        assertThat(agent.password).isEqualTo("hash-bcrypt")

        agent.setPassword("outro-hash")

        assertThat(agent.password).isEqualTo("outro-hash")
    }

    @Test
    fun `gerente herda o mesmo contrato de UserDetails`() {
        val manager = TestFixtures.manager(role = TestFixtures.role(name = "BRANCH_OWNER"))

        assertThat(manager.authorities.map { it.authority }).containsExactly("ROLE_BRANCH_OWNER")
        assertThat(manager.username).isEqualTo("bruno@example.com")
    }
}
