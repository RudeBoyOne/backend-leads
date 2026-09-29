package com.br.semesperanca.backendleads.configuration.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.resource.NoResourceFoundException
import java.time.LocalDateTime
import java.util.LinkedHashMap

@RestControllerAdvice
class GlobalExceptionHandler {
    private val log = org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(RuntimeException::class)
    fun handleRuntimeException(ex: RuntimeException): ResponseEntity<Map<String, Any>> = buildResponse(HttpStatus.BAD_REQUEST, ex.message)

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDeniedException(ex: AccessDeniedException): ResponseEntity<Map<String, Any>> =
        buildResponse(HttpStatus.FORBIDDEN, "Acesso negado — você não tem permissão para acessar este recurso")

    @ExceptionHandler(AuthenticationException::class)
    fun handleAuthenticationException(ex: AuthenticationException): ResponseEntity<Map<String, Any>> =
        buildResponse(HttpStatus.UNAUTHORIZED, "Não autenticado — token inválido ou expirado")

    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNoResourceFound(ex: NoResourceFoundException): ResponseEntity<Map<String, Any>> {
        log.warn("Recurso nao encontrado: {}", ex.resourcePath)
        return buildResponse(HttpStatus.NOT_FOUND, "Recurso não encontrado")
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception): ResponseEntity<Map<String, Any>> {
        log.error("Excecao nao tratada", ex)
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno do servidor")
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(ex: MethodArgumentNotValidException): ResponseEntity<Map<String, Any>> {
        val errors = LinkedHashMap<String, String>()
        ex.bindingResult.fieldErrors.forEach { error ->
            errors[error.field] = error.defaultMessage ?: ""
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            mapOf(
                "timestamp" to LocalDateTime.now().toString(),
                "status" to 400,
                "error" to "Bad Request",
                "message" to "Erro de validação",
                "fields" to errors,
            ),
        )
    }

    @ExceptionHandler(EmailAlreadyExistsException::class)
    fun handleEmailAlreadyExists(ex: EmailAlreadyExistsException): ResponseEntity<Map<String, Any>> =
        buildResponse(HttpStatus.CONFLICT, ex.message)

    private fun buildResponse(
        status: HttpStatus,
        message: String?,
    ): ResponseEntity<Map<String, Any>> =
        ResponseEntity.status(status).body(
            mapOf(
                "timestamp" to LocalDateTime.now().toString(),
                "status" to status.value(),
                "error" to status.reasonPhrase,
                "message" to (message ?: ""),
            ),
        )
}
