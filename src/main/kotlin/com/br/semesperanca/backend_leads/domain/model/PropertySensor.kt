ckage com.br.semesperanca.backendleads.domain.model

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "property_sensor")
class PropertySensor(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,
    @Column(name = "property_id", nullable = false)
    var propertyId: UUID? = null,
    @Column(name = "device_id", nullable = false)
    var deviceId: String? = null,
    @Column(name = "created_at", updatable = false, nullable = false)
    var createdAt: Instant? = null,
) {
    @PrePersist
    fun prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now()
        }
    }
}
