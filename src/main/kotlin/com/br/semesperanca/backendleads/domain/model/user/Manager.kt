package com.br.semesperanca.backendleads.domain.model.user

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "manager")
open class Manager(
    @Column(name = "branch_office")
    var branchOffice: String? = null
) : User()