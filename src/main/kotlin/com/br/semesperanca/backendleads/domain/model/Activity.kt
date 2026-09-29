package com.br.semesperanca.backendleads.domain.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "activity")
class Activity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,
    /**
     * O tipo é anulável só para o Kotlin gerar o construtor no-arg sintético —
     * sem default em todo parâmetro do construtor primário, `Lead.activities` e
     * `Property.activities` não podem ser inicializadas, porque o
     * `CollectionLoaderSingleKey` precisa instanciar a entidade só com a chave
     * antes do `SELECT`. A obrigatoriedade é do banco: `optional = false` e
     * `@JoinColumn(nullable = false)`.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lead_id", nullable = false)
    var lead: Lead? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id")
    var property: Property? = null,
    @Column(name = "title", nullable = false)
    var title: String? = null,
    @Column(name = "description", nullable = false)
    var description: String? = null,
    @Column(name = "activity_date", nullable = false)
    var activityDate: LocalDateTime? = null,
    @Column(name = "created_at", updatable = false, nullable = false)
    var createdAt: LocalDateTime? = null,
) {
    @PrePersist
    fun prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now()
        }
    }
}
