package com.br.semesperanca.backendleads.domain.repository.user

import com.br.semesperanca.backendleads.domain.model.user.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface UserRepository : JpaRepository<User, UUID> {
    fun findByEmail(email: String): Optional<User>

    fun existsByEmail(email: String): Boolean
}