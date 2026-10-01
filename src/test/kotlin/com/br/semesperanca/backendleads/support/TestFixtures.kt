package com.br.semesperanca.backendleads.support

import com.br.semesperanca.backendleads.application.model.LeadDTO
import com.br.semesperanca.backendleads.application.model.response.ActivityResponse
import com.br.semesperanca.backendleads.application.model.response.BranchOwnerResponse
import com.br.semesperanca.backendleads.application.model.response.LoginResponse
import com.br.semesperanca.backendleads.application.model.response.ManagerResponse
import com.br.semesperanca.backendleads.application.model.response.PropertyResponse
import com.br.semesperanca.backendleads.application.model.response.RealEstateAgentResponse
import com.br.semesperanca.backendleads.domain.model.Activity
import com.br.semesperanca.backendleads.domain.model.BranchOffice
import com.br.semesperanca.backendleads.domain.model.BranchOwner
import com.br.semesperanca.backendleads.domain.model.Lead
import com.br.semesperanca.backendleads.domain.model.Property
import com.br.semesperanca.backendleads.domain.model.Role
import com.br.semesperanca.backendleads.domain.model.user.Manager
import com.br.semesperanca.backendleads.domain.model.user.RealEstateAgent
import com.br.semesperanca.backendleads.domain.model.user.User
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

/**
 * Fabricas de entidades. As entidades sao classes de Kotlin com construtor
 * primario e campos mutaveis, entao um helper por tipo evita repetir a mesma
 * 10 linhas em 30 testes.
 *
 * Nenhuma aqui e persistida: sao para testes unitarios com repositorio mockado.
 * Os testes de integracao gravam direto pelos repositories.
 */
object TestFixtures {
    val AGENCY_ID: UUID = UUID.fromString("11111111-1111-1111-1111-111111111111")

    /**
     * `id` nasce nulo de propósito. `Role` é `@GeneratedValue`, e um id
     * preenchido à mão faz o Hibernate tratar o objeto como *detached*: o
     * `save` vira `merge` e estoura `ObjectOptimisticLockingFailureException`
     * porque a linha não existe. Nos testes com repositório mockado o id é
     * irrelevante; nos de `@DataJpaTest` ele precisa ser nulo.
     */
    fun role(
        name: String = "MANAGER",
        id: UUID? = null,
    ): Role = Role(id = id, name = name)

    fun lead(
        id: UUID? = UUID.randomUUID(),
        originLeadId: String = "origin-1",
        leadOrigin: String = "Zapimoveis",
        name: String = "Maria Silva",
        email: String = "maria@example.com",
        phoneNumber: String? = "11987654321",
        message: String = "Quero comprar um apartamento",
        temperature: String = "hot",
        transactionType: String = "venda",
        status: String = "new",
        insightId: String? = null,
        clientListingId: String? = "LST-1",
        originListingId: String? = "OL-1",
        timestamp: Instant? = Instant.parse("2026-01-15T10:00:00Z"),
        createdAt: Instant? = Instant.parse("2026-01-15T10:00:00Z"),
    ): Lead =
        Lead(
            id = id,
            realEstateAgencyId = AGENCY_ID,
            originLeadId = originLeadId,
            originListingId = originListingId,
            leadOrigin = leadOrigin,
            clientListingId = clientListingId,
            name = name,
            email = email,
            phoneNumber = phoneNumber,
            message = message,
            temperature = temperature,
            transactionType = transactionType,
            status = status,
            insightId = insightId,
            timestamp = timestamp,
            createdAt = createdAt,
        )

    fun property(
        id: UUID? = UUID.randomUUID(),
        realEstateAgencyId: UUID? = AGENCY_ID,
        title: String? = "Apartamento no centro",
        description: String? = "Sala, cozinha e dois quartos",
        price: BigDecimal? = BigDecimal("350000.00"),
        propertyType: String? = "apartamento",
        bedrooms: Int? = 2,
        bathrooms: Int? = 1,
        areaSqm: BigDecimal? = BigDecimal("65.00"),
        address: String? = "Rua das Flores, 100",
        city: String? = "Sao Paulo",
        state: String? = "SP",
        zipCode: String? = "01001000",
        status: String? = "available",
        active: Boolean = true,
        createdAt: LocalDateTime? = LocalDateTime.of(2026, 1, 15, 10, 0),
        updatedAt: LocalDateTime? = LocalDateTime.of(2026, 1, 15, 10, 0),
    ): Property =
        Property(
            id = id,
            realEstateAgencyId = realEstateAgencyId,
            title = title,
            description = description,
            price = price,
            propertyType = propertyType,
            bedrooms = bedrooms,
            bathrooms = bathrooms,
            areaSqm = areaSqm,
            address = address,
            city = city,
            state = state,
            zipCode = zipCode,
            status = status,
            active = active,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

    fun branchOwner(
        id: UUID? = UUID.randomUUID(),
        realEstateAgencyId: UUID? = AGENCY_ID,
        name: String? = "Carlos Souza",
        email: String? = "carlos@example.com",
        phone: String? = "11912345678",
        active: Boolean = true,
        createdAt: LocalDateTime? = LocalDateTime.of(2026, 1, 15, 10, 0),
        updatedAt: LocalDateTime? = LocalDateTime.of(2026, 1, 15, 10, 0),
    ): BranchOwner =
        BranchOwner(
            id = id,
            realEstateAgencyId = realEstateAgencyId,
            name = name,
            email = email,
            phone = phone,
            active = active,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

    fun activity(
        id: UUID? = UUID.randomUUID(),
        lead: Lead = lead(),
        property: Property? = null,
        title: String? = "Visita ao imovel",
        description: String? = "Agendada para sexta as 14h",
        activityDate: LocalDateTime? = LocalDateTime.of(2026, 1, 20, 14, 0),
        createdAt: LocalDateTime? = LocalDateTime.of(2026, 1, 15, 10, 0),
    ): Activity =
        Activity(
            id = id,
            lead = lead,
            property = property,
            title = title,
            description = description,
            activityDate = activityDate,
            createdAt = createdAt,
        )

    fun realEstateAgent(
        id: UUID? = UUID.randomUUID(),
        creci: String = "CRECI-123",
        name: String? = "Ana Paula",
        email: String? = "ana@example.com",
        role: Role? = role("REAL_ESTATE_AGENT"),
        isActive: Boolean = true,
        password: String = "{noop}secret",
        realEstateAgencyId: UUID? = AGENCY_ID,
        branchOffice: BranchOffice? = null,
        createdAt: LocalDateTime? = LocalDateTime.of(2026, 1, 15, 10, 0),
    ): RealEstateAgent =
        RealEstateAgent(creci = creci).apply {
            this.id = id
            this.name = name
            this.email = email
            this.role = role
            this.isActive = isActive
            setPassword(password)
            this.realEstateAgencyId = realEstateAgencyId
            this.branchOffice = branchOffice
            this.createdAt = createdAt
        }

    fun manager(
        id: UUID? = UUID.randomUUID(),
        branchOffice: BranchOffice? = null,
        name: String? = "Bruno Lima",
        email: String? = "bruno@example.com",
        role: Role? = role("MANAGER"),
        isActive: Boolean = true,
        password: String = "{noop}secret",
        realEstateAgencyId: UUID? = AGENCY_ID,
        createdAt: LocalDateTime? = LocalDateTime.of(2026, 1, 15, 10, 0),
    ): Manager =
        Manager().apply {
            this.id = id
            this.name = name
            this.email = email
            this.role = role
            this.isActive = isActive
            setPassword(password)
            this.realEstateAgencyId = realEstateAgencyId
            this.createdAt = createdAt
            this.branchOffice = branchOffice
        }

    /**
     * `User` e abstrata; os testes de `getAuthorities`/`isEnabled` que nao
     * precisam de uma tabela filha especifica usam um agente vazio com papel nulo.
     */
    fun bareUser(
        role: Role? = null,
        isActive: Boolean = true,
        email: String? = "user@example.com",
    ): User =
        RealEstateAgent(creci = "CRECI-BARE").apply {
            this.role = role
            this.isActive = isActive
            this.name = "Usuario"
            this.email = email
        }

    /**
     * Fabricas de *response*. Nos slices `@WebMvcTest` o servico e mockado, e
     * por isso preciso de um JSON de saida pronto — converter entidade em
     * response aqui dentro esconderia justamente o mapeamento que quero testar.
     */
    fun propertyResponse(
        id: UUID = UUID.randomUUID(),
        title: String = "Apartamento no centro",
        active: Boolean = true,
        status: String? = "available",
    ): PropertyResponse =
        PropertyResponse(
            id = id,
            realEstateAgencyId = AGENCY_ID,
            title = title,
            description = "Sala, cozinha e dois quartos",
            price = BigDecimal("350000.00"),
            propertyType = "apartamento",
            bedrooms = 2,
            bathrooms = 1,
            areaSqm = BigDecimal("65.00"),
            address = "Rua das Flores, 100",
            city = "Sao Paulo",
            state = "SP",
            zipCode = "01001000",
            status = status,
            active = active,
            createdAt = LocalDateTime.of(2026, 1, 15, 10, 0),
            updatedAt = LocalDateTime.of(2026, 1, 15, 10, 0),
        )

    fun activityResponse(
        id: UUID = UUID.randomUUID(),
        leadId: UUID = UUID.randomUUID(),
        propertyId: UUID? = null,
        title: String = "Visita ao imovel",
    ): ActivityResponse =
        ActivityResponse(
            id = id,
            leadId = leadId,
            propertyId = propertyId,
            title = title,
            description = "Agendada para sexta as 14h",
            activityDate = LocalDateTime.of(2026, 1, 20, 14, 0),
            createdAt = LocalDateTime.of(2026, 1, 15, 10, 0),
        )

    fun branchOwnerResponse(
        id: UUID = UUID.randomUUID(),
        name: String = "Carlos Souza",
        active: Boolean = true,
    ): BranchOwnerResponse =
        BranchOwnerResponse(
            id = id,
            realEstateAgencyId = AGENCY_ID,
            name = name,
            email = "carlos@example.com",
            phone = "11912345678",
            active = active,
            createdAt = LocalDateTime.of(2026, 1, 15, 10, 0),
            updatedAt = LocalDateTime.of(2026, 1, 15, 10, 0),
        )

    fun managerResponse(
        id: UUID = UUID.randomUUID(),
        name: String = "Bruno Lima",
        active: Boolean = true,
    ): ManagerResponse =
        ManagerResponse(
            id = id,
            name = name,
            email = "bruno@example.com",
            branchOfficeId = null,
            role = "MANAGER",
            active = active,
            createdAt = LocalDateTime.of(2026, 1, 15, 10, 0),
        )

    fun realEstateAgentResponse(
        id: UUID = UUID.randomUUID(),
        name: String = "Ana Paula",
        active: Boolean = true,
    ): RealEstateAgentResponse =
        RealEstateAgentResponse(
            id = id,
            name = name,
            email = "ana@example.com",
            creci = "CRECI-123",
            branchOfficeId = null,
            role = "REAL_ESTATE_AGENT",
            active = active,
            createdAt = LocalDateTime.of(2026, 1, 15, 10, 0),
        )

    fun loginResponse(token: String = "jwt-abc.123.def"): LoginResponse = LoginResponse(token = token)

    fun leadDTO(
        originLeadId: String = "OL-9",
        status: String = "new",
    ): LeadDTO =
        LeadDTO(
            leadOrigin = "zap",
            timestamp = "2026-01-15T10:00:00Z",
            originLeadId = originLeadId,
            originListingId = "OL-1",
            clientListingId = "CL-1",
            name = "Joana Ribeiro",
            email = "joana@example.com",
            phone = "11988887777",
            message = "Quero agendar uma visita",
            temperature = "quente",
            transactionType = "venda",
            realEstateAgencyId = AGENCY_ID,
            realEstateAgentId = null,
            portalId = null,
            funnelStageId = null,
            source = "site",
            status = status,
            insightId = null,
            createdAt = "2026-01-15T10:00:00Z",
            updatedAt = null,
        )
}
