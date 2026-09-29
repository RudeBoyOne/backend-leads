package com.br.semesperanca.backendleads.configuration.exception

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.core.MethodParameter
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException

class GlobalExceptionHandlerTest {
    private val handler = GlobalExceptionHandler()

    private fun assertEnvelope(
        response: ResponseEntity<Map<String, Any>>,
        status: HttpStatus,
        message: String,
    ) {
        assertThat(response.statusCode).isEqualTo(status)
        val body = response.body!!
        assertThat(body["status"]).isEqualTo(status.value())
        assertThat(body["error"]).isEqualTo(status.reasonPhrase)
        assertThat(body["message"]).isEqualTo(message)
        assertThat(body["timestamp"]).isNotNull()
    }

    /**
     * `MethodArgumentNotValidException` exige um `MethodParameter` real. No Spring 7
     * o constrator `(Method, Class)` sumiu, restando `(Method, paramIndex)` — o
     * indice -1 significa "o proprio Method ja e o parametro".
     */
    private fun validationException(vararg errors: FieldError): MethodArgumentNotValidException {
        val target = Any()
        val bindingResult = BeanPropertyBindingResult(target, "request").apply { errors.forEach(::addError) }
        val method = ValidationTarget::class.java.getDeclaredMethod("accept", Any::class.java)
        return MethodArgumentNotValidException(MethodParameter(method, -1), bindingResult)
    }

    @Test
    fun `RuntimeException vira 400 e preserva a mensagem original`() {
        val response = handler.handleRuntimeException(RuntimeException("Lead não encontrado"))

        assertEnvelope(response, HttpStatus.BAD_REQUEST, "Lead não encontrado")
    }

    @Test
    fun `RuntimeException sem mensagem vira 400 com mensagem vazia, sem NPE`() {
        val response = handler.handleRuntimeException(RuntimeException())

        assertEnvelope(response, HttpStatus.BAD_REQUEST, "")
    }

    @Test
    fun `AccessDeniedException vira 403 com a mensagem de acesso negado`() {
        val response = handler.handleAccessDeniedException(AccessDeniedException("detalhe interno"))

        assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
        assertThat(response.body!!["message"])
            .isEqualTo("Acesso negado — você não tem permissão para acessar este recurso")
    }

    @Test
    fun `AccessDeniedException nao vaza o detalhe da excecao`() {
        val response = handler.handleAccessDeniedException(AccessDeniedException("coluna role.id"))

        assertThat(response.body!!).doesNotContainValue("coluna role.id")
    }

    @Test
    fun `AuthenticationException vira 401`() {
        val response = handler.handleAuthenticationException(BadCredentialsException("token expirado"))

        assertEnvelope(response, HttpStatus.UNAUTHORIZED, "Não autenticado — token inválido ou expirado")
    }

    @Test
    fun `EmailAlreadyExistsException vira 409, nao 400`() {
        val response = handler.handleEmailAlreadyExists(EmailAlreadyExistsException("dup@example.com"))

        assertEnvelope(response, HttpStatus.CONFLICT, "Email já cadastrado: dup@example.com")
    }

    @Test
    fun `Exception generica vira 500 sem expor a mensagem original`() {
        val response = handler.handleGenericException(IllegalStateException("conexao com o banco caiu"))

        assertEnvelope(response, HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno do servidor")
        assertThat(response.body!!).doesNotContainValue("conexao com o banco caiu")
    }

    @Test
    fun `MethodArgumentNotValidException vira 400 com o mapa de campos`() {
        val exception =
            validationException(
                FieldError("request", "email", "Email inválido"),
                FieldError("request", "password", "Senha deve ter no mínimo 6 caracteres"),
            )

        val response = handler.handleValidationException(exception)

        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        val body = response.body!!
        assertThat(body["error"]).isEqualTo("Bad Request")
        assertThat(body["message"]).isEqualTo("Erro de validação")
        @Suppress("UNCHECKED_CAST")
        val fields = body["fields"] as Map<String, String>
        assertThat(fields)
            .containsEntry("email", "Email inválido")
            .containsEntry("password", "Senha deve ter no mínimo 6 caracteres")
    }

    @Test
    fun `campo de validacao sem mensagem padrao vira string vazia`() {
        val exception = validationException(FieldError("request", "email", "", false, emptyArray(), null, null))

        val response = handler.handleValidationException(exception)

        @Suppress("UNCHECKED_CAST")
        val fields = response.body!!["fields"] as Map<String, String>
        assertThat(fields).containsEntry("email", "")
    }

    @Test
    fun `o envelope de erro tem sempre as mesmas quatro chaves`() {
        val response = handler.handleRuntimeException(RuntimeException("x"))

        assertThat(response.body!!.keys)
            .containsExactlyInAnyOrder("timestamp", "status", "error", "message")
    }

    @Test
    fun `o envelope de validacao acrescenta fields às quatro chaves`() {
        val response = handler.handleValidationException(validationException())

        assertThat(response.body!!.keys)
            .containsExactlyInAnyOrder("timestamp", "status", "error", "message", "fields")
    }

    private class ValidationTarget {
        @Suppress("UNUSED_PARAMETER")
        fun accept(arg: Any) = Unit
    }
}
