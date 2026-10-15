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

/**
 * Raiz da herança `JOINED` das três pessoas do sistema: [Manager],
 * [RealEstateAgent] e [BranchOwner]. A tabela filha guarda só o que `users` não
 * tem — CRECI, ou telefone.
 *
 * A classe é **concreta**, e essa é a parte que importa: `FULL_ADMIN` é pessoa
 * externa ao negócio, não é corretor e não tem CRECI, então a linha dele em
 * `users` não tem par em nenhuma tabela filha. Se a raiz fosse abstrata, o
 * Hibernate não teria o que instanciar ao ler essa linha, e o `login` do root
 * quebraria com `AssertionError` dentro do inicializador de entidade. Ser
 * concreta é o que faz "não é nenhum dos três subtipos" ser um estado
 * representável em vez de uma linha que o ORM não sabe ler.
 */
@Entity
@Table(name = "\"users\"")
@Inheritance(strategy = InheritanceType.JOINED)
open class User(
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
    /**
     * `createdAt` só é preenchido quando vem nulo, como em `Property` e
     * `BranchOffice`. Sobrescrever incondicionalmente descartaria uma data
     * fornecida de propósito — e `created_at` é `updatable = false`, então a
     * intenção de preservar é justamente a que o mapeamento expressa.
     * `updatedAt` é sempre reescrito, porque é o que ele significa.
     */
    @PrePersist
    fun prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now()
        }
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
