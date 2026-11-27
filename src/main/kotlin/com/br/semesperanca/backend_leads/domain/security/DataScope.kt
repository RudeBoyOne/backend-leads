package com.br.semesperanca.backendleads.domain.security

import com.br.semesperanca.backendleads.domain.model.BranchOffice
import com.br.semesperanca.backendleads.domain.model.BranchOwner
import com.br.semesperanca.backendleads.domain.model.Lead
import com.br.semesperanca.backendleads.domain.model.Property
import com.br.semesperanca.backendleads.domain.model.RealEstateAgency
import com.br.semesperanca.backendleads.domain.model.Team
import com.br.semesperanca.backendleads.domain.model.user.Manager
import com.br.semesperanca.backendleads.domain.model.user.RealEstateAgent
import org.springframework.data.jpa.domain.Specification

object DataScope {
    fun <T : Any> forType(type: Class<T>): Specification<T> {
        if (type !in scopedTypes) return unrestricted()

        val scope = RoleScope(PrincipalResolver.current())
        @Suppress("UNCHECKED_CAST")
        return when (type) {
            BranchOwner::class.java -> scope.forBranchOwner() as Specification<T>
            BranchOffice::class.java -> scope.forBranchOffice() as Specification<T>
            RealEstateAgency::class.java -> scope.forRealEstateAgency() as Specification<T>
            Manager::class.java -> scope.forManager() as Specification<T>
            RealEstateAgent::class.java -> scope.forRealEstateAgent() as Specification<T>
            Team::class.java -> scope.forTeam() as Specification<T>
            Lead::class.java -> scope.forLead() as Specification<T>
            Property::class.java -> scope.forProperty() as Specification<T>
            else -> error("Escopo declarado para $type mas sem regra implementada")
        }
    }

    private fun <T> unrestricted(): Specification<T> = Specification.unrestricted()

    val scopedTypes: List<Class<*>> =
        listOf(
            BranchOwner::class.java,
            BranchOffice::class.java,
            RealEstateAgency::class.java,
            Manager::class.java,
            RealEstateAgent::class.java,
            Team::class.java,
            Lead::class.java,
            Property::class.java,
        )
}
