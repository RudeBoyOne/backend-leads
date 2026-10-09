package com.br.semesperanca.backendleads.support

import org.springframework.test.web.servlet.ResultMatcher
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath

/**
 * Atalhos para ler o JSON de resposta. O `MockMvcTester` expoe
 * `bodyJson().extractingPath(...)`, mas cada encadeamento devolve um assert
 * diferente e o tipo se perde no Kotlin; um `ResultMatcher` por linha fica
 * mais legivel e tipado.
 */
fun jsonEquals(
    path: String,
    expected: Any?,
): ResultMatcher = jsonPath(path).value(expected)

fun jsonNotEmpty(path: String): ResultMatcher = jsonPath(path).isNotEmpty()

fun jsonEmptyBody(): ResultMatcher = content().string("")
