package com.br.semesperanca.backendleads.domain.security

import com.br.semesperanca.backendleads.domain.model.BranchOffice
import com.br.semesperanca.backendleads.domain.model.BranchOwner
import com.br.semesperanca.backendleads.domain.model.Lead
import com.br.semesperanca.backendleads.domain.model.Property
import com.br.semesperanca.backendleads.domain.model.Team
import com.br.semesperanca.backendleads.domain.model.user.Manager
import com.br.semesperanca.backendleads.domain.model.user.RealEstateAgent
import org.springframework.data.jpa.domain.Specification

/**
 * Entrega a `Specification` de escopo de cada entidade escopada.
 *
 * A [com.br.semesperanca.backendleads.domain.repository.ScopedRepositoryImpl] é
 * genérica e não conhece o domínio, então quem faz o despacho de tipo para tipo
 * é este objeto. A consequência útil é que a lista de entidades escopadas é
 * auditável num lugar só: um repositório novo precisa aparecer aqui para ser
 * escopado, e o `ScopeCoverageTest` trava essa cobertura.
 *
 * Um tipo ausente não falha em produção — devolve "sem restrição" — porque
 * `Role`, `Plan`, `Portal` e as tabelas de IoT realmente não têm dono na
 * hierarquia. A lista fechada abaixo é o contrato: ela é exaustiva para o
 * negócio, e o teste cobre exatamente esses sete tipos.
 */
object DataScope {
    /**
     * O principal é resolvido **depois** do teste de escopo, e nunca antes.
     *
     * Resolvê-lo primeiro seria um deadlock lógico: [CustomJwtAuthenticationConverter]
     * lê `UserRepository` para descobrir a role de quem está entrando, e
     * [AuthService] lê o mesmo repositório no login — nenhum dos dois tem
     * principal nenhum ainda, porque o token ainda não existe. Se a resolução
     * fosse eager, `UserRepository` (que não tem dono na hierarquia) exigiria
     * autenticação e a autenticação ficaria impossível de fazer.
     *
     * Para os sete tipos escopados a falha continua fechada: sem principal,
     * [PrincipalResolver.current] estoura e a consulta não sai.
     */
    fun <T : Any> forType(type: Class<T>): Specification<T> {
        if (type !in scopedTypes) return unrestricted()

        val scope = RoleScope(PrincipalResolver.current())
        @Suppress("UNCHECKED_CAST")
        return when (type) {
            BranchOwner::class.java -> scope.forBranchOwner() as Specification<T>
            BranchOffice::class.java -> scope.forBranchOffice() as Specification<T>
            Manager::class.java -> scope.forManager() as Specification<T>
            RealEstateAgent::class.java -> scope.forRealEstateAgent() as Specification<T>
            Team::class.java -> scope.forTeam() as Specification<T>
            Lead::class.java -> scope.forLead() as Specification<T>
            Property::class.java -> scope.forProperty() as Specification<T>
            else -> error("Escopo declarado para $type mas sem regra implementada")
        }
    }

    /**
     * A `Specification` é reconstruída a cada chamada, nunca guardada em campo: o
     * repositório é um singleton e o principal muda a cada requisição. Cachear
     * aqui serviria o escopo do usuário anterior para o próximo.
     */
    private fun <T> unrestricted(): Specification<T> = Specification.unrestricted()

    /** Tipos com regra de escopo. Existe para o teste de cobertura iterate sobre ele. */
    val scopedTypes: List<Class<*>> =
        listOf(
            BranchOwner::class.java,
            BranchOffice::class.java,
            Manager::class.java,
            RealEstateAgent::class.java,
            Team::class.java,
            Lead::class.java,
            Property::class.java,
        )
}
