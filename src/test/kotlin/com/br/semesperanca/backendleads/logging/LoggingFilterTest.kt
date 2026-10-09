package com.br.semesperanca.backendleads.logging

import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.http.HttpServletRequest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.slf4j.MDC
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

class LoggingFilterTest {
    private val filter = LoggingFilter()

    @AfterEach
    fun clearMdc() = MDC.clear()

    @Test
    fun `poe ip e user-agent no MDC durante a requisicao`() {
        val request =
            MockHttpServletRequest().apply {
                remoteAddr = "203.0.113.7"
                addHeader("User-Agent", "JUnit/1.0")
            }
        var ipInsideChain: String? = null
        var userAgentInsideChain: String? = null
        val chain =
            FilterChain { _, _ ->
                ipInsideChain = MDC.get("ip")
                userAgentInsideChain = MDC.get("user-agent")
            }

        filter.doFilter(request, MockHttpServletResponse(), chain)

        assertThat(ipInsideChain).isEqualTo("203.0.113.7")
        assertThat(userAgentInsideChain).isEqualTo("JUnit/1.0")
    }

    @Test
    fun `limpa o MDC ao final, depois de atender a requisicao`() {
        filter.doFilter(MockHttpServletRequest(), MockHttpServletResponse(), FilterChain { _, _ -> })

        assertThat(MDC.getCopyOfContextMap()).isNull()
    }

    @Test
    fun `limpa o MDC quando o filtro lancou excecao`() {
        val boom = IllegalStateException("controller explodiu")
        val chain = FilterChain { _, _ -> throw boom }
        val request: HttpServletRequest = MockHttpServletRequest().apply { remoteAddr = "10.0.0.1" }

        assertThatThrownBy { filter.doFilter(request, MockHttpServletResponse(), chain) }
            .isSameAs(boom)

        assertThat(MDC.getCopyOfContextMap()).isNull()
    }

    @Test
    fun `MDC de requisicoes anteriores e sobrescrito, nao vaza para a seguinte`() {
        MDC.put("ip", "192.168.0.1")
        var ipInsideChain: String? = null
        val chain = FilterChain { _, _ -> ipInsideChain = MDC.get("ip") }

        // MockHttpServletRequest usa 127.0.0.1 como remoteAddr padrao.
        filter.doFilter(MockHttpServletRequest(), MockHttpServletResponse(), chain)

        assertThat(ipInsideChain).isEqualTo("127.0.0.1")
    }

    @Test
    fun `requisicao que nao e HttpServletRequest segue a cadeia sem tocar no MDC`() {
        val request: ServletRequest = mock()
        var invoked = false

        filter.doFilter(request, MockHttpServletResponse(), FilterChain { _, _ -> invoked = true })

        assertThat(invoked).isTrue()
    }
}
