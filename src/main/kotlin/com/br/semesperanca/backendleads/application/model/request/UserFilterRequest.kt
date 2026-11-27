package com.br.semesperanca.backendleads.application.model.request

data class UserFilterRequest(
    val active: Boolean?,
    val search: String?
)