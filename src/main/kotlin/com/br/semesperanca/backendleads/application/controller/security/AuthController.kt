package com.br.semesperanca.backendleads.application.controller.security

import com.br.semesperanca.backendleads.application.model.request.ChangePasswordRequest
import com.br.semesperanca.backendleads.application.model.request.CreateFullAdminRequest
import com.br.semesperanca.backendleads.application.model.request.LoginRequest
import com.br.semesperanca.backendleads.application.model.response.LoginResponse
import com.br.semesperanca.backendleads.application.model.response.RealEstateAgentResponse
import com.br.semesperanca.backendleads.domain.service.security.AuthService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticação", description = "Endpoints de autenticação local e OAuth2")
class AuthController(
    private val authService: AuthService
) {
    @PostMapping("/login")
    @Operation(summary = "Login local", description = "Autentica com email e senha, retorna JWT")
    fun login(@RequestBody request: LoginRequest): ResponseEntity<LoginResponse> {
        return ResponseEntity.ok(authService.login(request))
    }

    @PostMapping("/register/full-admin")
    @Operation(
        summary = "Criar FULL_ADMIN",
        description = "Cria um novo super administrador (requer FULL_ADMIN)"
    )
    fun createFullAdmin(
        @RequestBody request: CreateFullAdminRequest
    ): ResponseEntity<RealEstateAgentResponse> {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.createFullAdmin(request))
    }

    @PatchMapping("/change-password")
    @Operation(summary = "Trocar senha", description = "Troca a senha do usuário autenticado")
    fun changePassword(
        @RequestBody @Valid request: ChangePasswordRequest,
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<Void> {
        val email: String? = jwt.getClaimAsString("email")
        authService.changePassword(request, email ?: "")
        return ResponseEntity.noContent().build()
    }
}