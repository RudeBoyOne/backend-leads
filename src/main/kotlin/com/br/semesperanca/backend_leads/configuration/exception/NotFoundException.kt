package com.br.semesperanca.backendleads.configuration.exception

/**
 * Recurso não existe **ou** está fora do escopo do principal autenticado.
 *
 * As duas causas produzem a mesma resposta de propósito. Devolver 403 para o id
 * alheio e 404 para o id inexistente confirmaria ao atacante que o recurso
 * existe, transformando o endpoint em oráculo de existência. O corte de escopo
 * acontece na query (ver `ScopedRepositoryImpl`), então um id fora do escopo
 * simplesmente não resolve e não há caminho que chegue aqui por outro motivo.
 */
class NotFoundException(
    message: String,
) : RuntimeException(message)
