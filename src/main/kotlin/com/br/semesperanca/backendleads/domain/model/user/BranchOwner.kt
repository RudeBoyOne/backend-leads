package com.br.semesperanca.backendleads.domain.model

import com.br.semesperanca.backendleads.domain.model.user.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity
@Table(name = "branch_owner")
class BranchOwner(
    @Column(name = "phone")
    var phone: String? = null,
) : User() {
    @OneToMany(mappedBy = "branchOwner", fetch = FetchType.LAZY)
    var branchOffices: MutableList<BranchOffice> = mutableListOf()
}
