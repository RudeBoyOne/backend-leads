package com.br.semesperanca.backendleads.application.controller

import com.br.semesperanca.backendleads.application.model.request.CreateUserRequest
import com.br.semesperanca.backendleads.application.model.request.UpdateUserRequest
import com.br.semesperanca.backendleads.application.model.response.UserResponse
import com.br.semesperanca.backendleads.domain.service.user.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/users")
@Tag(name = "Usuários", description = "Gerenciamento da raiz de usuários")
class UserController(
    private val service: UserService,
) {
    @GetMapping
    @Operation(summary = "Listar usuários")
    fun findAll(): ResponseEntity<List<UserResponse>> = ResponseEntity.ok(service.findAll())

    @GetMapping("/{id}")
    @Operation(summary = "Buscar usuário por ID")
    fun findById(
        @PathVariable id: UUID,
    ): ResponseEntity<UserResponse> = ResponseEntity.ok(service.findById(id))

    @PostMapping
    @Operation(summary = "Criar usuário")
    fun create(
        @RequestBody @Valid request: CreateUserRequest,
    ): ResponseEntity<UserResponse> = ResponseEntity.status(HttpStatus.CREATED).body(service.create(request))

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar usuário")
    fun update(
        @PathVariable id: UUID,
        @RequestBody @Valid request: UpdateUserRequest,
    ): ResponseEntity<UserResponse> = ResponseEntity.ok(service.update(id, request))

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir usuário")
    fun delete(
        @PathVariable id: UUID,
    ): ResponseEntity<Void> {
        service.delete(id)
        return ResponseEntity.noContent().build()
    }
}
