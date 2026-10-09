package com.br.semesperanca.backendleads.support

import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Path
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import org.mockito.Mockito
import org.mockito.invocation.InvocationOnMock
import org.springframework.data.jpa.domain.Specification

/**
 * Avalia um `Specification` contra um `CriteriaBuilder` instrumentado e devolve
 * os `Predicate` entregues ao `cb.and(...)` final, com os valores comparados.
 *
 * Serve para testar *quantos* filtros o servico montou e *com quais valores*,
 * sem subir um banco. O `CriteriaBuilder` tem dezenas de sobrecargas fortemente
 * tipadas, entao em vez de stubbar uma a uma o mock usa uma Answer generica:
 * `equal`/`like`/etc. registram o argumento e devolvem um no da arvore de
 * filtros, que serve tanto como `Predicate` quanto como `Expression` — por isso
 * o retorno de `cb.lower` continua aceito por `cb.like`.
 *
 * O SQL realmente gerado continua sendo verificado nos testes `@DataJpaTest`; o
 * alvo aqui e a logica de montagem do filtro.
 */
object SpecProbe {
    /**
     * @param operations registro dos operadores na ordem em que o servico os chamou,
     *   com os valores: `equal(sold)`, `like(%jardim%)`, `gte(100.00)`.
     * @param andValues o que cada chamada de `cb.and(...)` recebeu.
     */
    data class Trace(
        val operations: List<String>,
        val andValues: List<List<Any?>>,
    ) {
        /** Quantos predicados o servico combinou no `cb.and` final. */
        val predicateCount: Int get() = andValues.sumOf { it.size }
    }

    fun traceOf(spec: Specification<*>): Trace {
        val operations = mutableListOf<String>()
        val andValues = mutableListOf<List<Any?>>()
        val leaf = { node() }

        @Suppress("UNCHECKED_CAST")
        val root = Mockito.mock(Root::class.java) { leaf() } as Root<Any>

        @Suppress("UNCHECKED_CAST")
        val query = Mockito.mock(CriteriaQuery::class.java) { leaf() } as CriteriaQuery<Any>
        val cb: CriteriaBuilder =
            Mockito.mock(CriteriaBuilder::class.java) { invocation ->
                answer(invocation, operations, andValues, leaf)
            }

        @Suppress("UNCHECKED_CAST")
        (spec as Specification<Any>).toPredicate(root, query, cb)

        return Trace(operations, andValues)
    }

    private fun answer(
        invocation: InvocationOnMock,
        operations: MutableList<String>,
        andValues: MutableList<List<Any?>>,
        leaf: () -> Any,
    ): Any {
        val name = invocation.method.name

        if (name == "and") {
            andValues += varargsOf(invocation)
            operations += "and"
        } else {
            // O primeiro argumento e sempre a Expression (nos mocks, o no leaf).
            operations += "$name(${invocation.arguments.drop(1).joinToString(", ")})"
        }

        return leaf()
    }

    /**
     * Argumentos de um metodo varargs. O `getArguments()` do Mockito 5 ja vem
     * expandido (um elemento por predicado), o que esconderia o array; o
     * `getRawArguments()` preserva o `Predicate[]` como um unico elemento.
     */
    private fun varargsOf(invocation: InvocationOnMock): List<Any?> {
        val raw = invocation.rawArguments.singleOrNull()
        return if (raw is Array<*>) raw.toList() else invocation.arguments.toList()
    }

    /** Um no da arvore de filtros: serve como `Path`, `Predicate` e `Expression`. */
    private fun node(): Any = Mockito.mock(Node::class.java)

    /**
     * Interface agregada dos papeis que o codigo de producao espera:
     * `root.get(...)` devolve uma `Path`, `cb.lower(...)` devolve uma
     * `Expression` e `cb.and(...)` recebe `Predicate`s. Um unico mock atende
     * aos tres porque `Path` ja estende `Expression`.
     */
    private interface Node :
        Path<Boolean>,
        Predicate
}
