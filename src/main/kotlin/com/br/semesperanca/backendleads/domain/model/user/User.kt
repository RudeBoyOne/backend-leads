package com.br.semesperanca.backendleads.domain.model.user

import com.br.semesperanca.backendleads.domain.model.BranchOffice
import com.br.semesperanca.backendleads.domain.model.Role
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Inheritance
import jakarta.persistence.InheritanceType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "\"users\"")
@Inheritance(strategy = InheritanceType.JOINED)
abstract class User(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,
    @Column(name = "real_estate_agency_id", nullable = false)
    var realEstateAgencyId: UUID? = null,
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    var role: Role? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_office_id")
    var branchOffice: BranchOffice? = null,
    @Column(name = "name", nullable = false)
    var name: String? = null,
    @Column(name = "email", nullable = false, unique = true)
    var email: String? = null,
    @Column(name = "active", nullable = false)
    var isActive: Boolean = true,
    @Column(name = "created_at", updatable = false)
    var createdAt: LocalDateTime? = null,
    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null,
) : UserDetails {
    @PrePersist
    fun prePersist() {
        createdAt = LocalDateTime.now()
        updatedAt = LocalDateTime.now()
    }

    @PreUpdate
    fun preUpdate() {
        updatedAt = LocalDateTime.now()
    }

    override fun getAuthorities(): Collection<GrantedAuthority> =
        role?.name?.let { listOf(SimpleGrantedAuthority("ROLE_$it")) } ?: emptyList()

    @Column(name = "password", nullable = false)
    private var pw: String = ""

    override fun getPassword(): String = pw

    fun setPassword(value: String) {
        pw = value
    }

    override fun getUsername(): String = email ?: ""

    override fun isEnabled(): Boolean = isActive

    override fun isAccountNonExpired(): Boolean = true

    override fun isAccountNonLocked(): Boolean = true

    override fun isCredentialsNonExpired(): Boolean = true
}
