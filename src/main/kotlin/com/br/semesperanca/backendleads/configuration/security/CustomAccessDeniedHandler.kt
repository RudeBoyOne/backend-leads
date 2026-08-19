package com.br.semesperanca.backendleads.configuration.security

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class CustomAccessDeniedHandler : AccessDeniedHandler {

    private val objectMapper = ObjectMapper()

    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        accessDeniedException: AccessDeniedException
    ) {
        response.status = HttpServletResponse.SC_FORBIDDEN
        response.contentType = MediaType.APPLICATION_JSON_VALUE

        objectMapper.writeValue(
            response.outputStream,
            mapOf(
                "timestamp" to LocalDateTime.now().toString(),
                "status" to 403,
                "error" to "Forbidden",
                "message" to "Acesso negado — você não tem permissão para acessar este recurso"
            )
        )
    }
}