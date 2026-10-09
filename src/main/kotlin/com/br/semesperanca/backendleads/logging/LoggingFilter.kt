package com.br.semesperanca.backendleads.logging

import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.MDC
import org.springframework.stereotype.Component

@Component
class LoggingFilter : Filter {

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        try {
            if (request is HttpServletRequest) {
                val ip = request.remoteAddr
                val userAgent = request.getHeader("User-Agent")
                MDC.put("ip", ip)
                MDC.put("user-agent", userAgent)
            }
            chain.doFilter(request, response)
        } finally {
            MDC.clear()
        }
    }
}