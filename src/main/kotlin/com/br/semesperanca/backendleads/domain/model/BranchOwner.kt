package com.br.semesperanca.backendleads.domain.model

import com.br.semesperanca.backendleads.domain.model.user.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

/**
 * Responsável por uma ou mais filiais da agência, e a raiz da hierarquia
 * operacional:
 *
 * ```
 * FULL_ADMIN          root do software, externo ao negócio
 *   └─ ADMIN          branch owner  -> suas filiais
 *        └─ MANAGER   -> seus times -> seus corretores
 *             └─ REAL_ESTATE_AGENT  -> seus leads
 * ```
 *
 * É subclasse JOINED de [User], como [com.br.semesperanca.backendleads.domain.model.user.Manager]
 * e [com.br.semesperanca.backendleads.domain.model.user.RealEstateAgent]: quem é a pessoa
 * mora em `users` e a tabela filha guarda só o que `users` não tem, que aqui é só o
 * telefone. Por isso a role é ADMIN e é o único detentor dela.
 *
 * A filial não é uma coluna em `users`: um responsável supervisiona N filiais, então
 * [branchOffices] é a fonte da verdade e `users.branch_office_id` fica nulo.
 */
@Entity
@Table(name = "branch_owner")
class BranchOwner(
    @Column(name = "phone")
    var phone: String? = null,
) : User() {
    @OneToMany(mappedBy = "branchOwner", fetch = FetchType.LAZY)
    var branchOffices: MutableList<BranchOffice> = mutableListOf()
}
