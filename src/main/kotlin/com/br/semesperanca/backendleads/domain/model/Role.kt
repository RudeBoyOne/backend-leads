package com.br.semesperanca.backendleads.domain.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "role")
class Role(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,
    @Column(name = "name", nullable = false, unique = true)
    var name: String? = null,
) {
    companion object {
        /**
         * Nomes semeados pela V002. `ADMIN` não é um admin de agência qualquer: é
         * exclusivamente o branch owner, único detentor dessa role. `FULL_ADMIN` é o
         * root do software, externo ao negócio, e não é escopado.
         */
        const val FULL_ADMIN = "FULL_ADMIN"
        const val ADMIN = "ADMIN"
        const val MANAGER = "MANAGER"
        const val REAL_ESTATE_AGENT = "REAL_ESTATE_AGENT"
    }
}
