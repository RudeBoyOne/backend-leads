package com.br.semesperanca.backendleads.domain.service

import com.br.semesperanca.backendleads.application.model.request.CreatePropertyRequest
import com.br.semesperanca.backendleads.application.model.request.UpdatePropertyRequest
import com.br.semesperanca.backendleads.application.model.response.PropertyResponse
import com.br.semesperanca.backendleads.domain.model.Property
import com.br.semesperanca.backendleads.domain.repository.PropertyRepository
import jakarta.persistence.criteria.Predicate
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.util.UUID

@Service
class PropertyService(
    private val repository: PropertyRepository
) {
    @Transactional
    fun create(request: CreatePropertyRequest): PropertyResponse {
        val property = Property(
            realEstateAgencyId = request.realEstateAgencyId,
            title = request.title,
            description = request.description,
            price = request.price,
            propertyType = request.propertyType,
            bedrooms = request.bedrooms,
            bathrooms = request.bathrooms,
            areaSqm = request.areaSqm,
            address = request.address,
            city = request.city,
            state = request.state,
            zipCode = request.zipCode,
            status = request.status ?: "available",
            active = true
        )

        return toResponse(repository.save(property))
    }

    fun findById(id: UUID): PropertyResponse = toResponse(findPropertyById(id))

    fun findAll(
        realEstateAgencyId: UUID?,
        active: Boolean?,
        status: String?,
        propertyType: String?,
        search: String?,
        minPrice: BigDecimal?,
        maxPrice: BigDecimal?,
        pageable: Pageable
    ): Page<PropertyResponse> {
        return repository
            .findAll(
                buildSpec(realEstateAgencyId, active, status, propertyType, search, minPrice, maxPrice),
                pageable
            )
            .map { toResponse(it) }
    }

    @Transactional
    fun update(id: UUID, request: UpdatePropertyRequest): PropertyResponse {
        val property = findPropertyById(id)
        property.title = request.title
        property.description = request.description
        property.price = request.price
        property.propertyType = request.propertyType
        property.bedrooms = request.bedrooms
        property.bathrooms = request.bathrooms
        property.areaSqm = request.areaSqm
        property.address = request.address
        property.city = request.city
        property.state = request.state
        property.zipCode = request.zipCode
        property.status = request.status ?: "available"

        return toResponse(repository.save(property))
    }

    @Transactional
    fun toggleActive(id: UUID): PropertyResponse {
        val property = findPropertyById(id)
        property.active = !property.active
        return toResponse(repository.save(property))
    }

    @Transactional
    fun delete(id: UUID) {
        repository.delete(findPropertyById(id))
    }

    private fun findPropertyById(id: UUID): Property {
        return repository.findById(id).orElseThrow { RuntimeException("Imóvel não encontrado") }
    }

    private fun buildSpec(
        realEstateAgencyId: UUID?,
        active: Boolean?,
        status: String?,
        propertyType: String?,
        search: String?,
        minPrice: BigDecimal?,
        maxPrice: BigDecimal?
    ): Specification<Property> {
        return Specification { root, _, cb ->
            val predicates = mutableListOf<Predicate>()
            if (realEstateAgencyId != null) {
                predicates.add(cb.equal(root.get<Any>("realEstateAgencyId"), realEstateAgencyId))
            }
            if (active != null) {
                predicates.add(cb.equal(root.get<Boolean>("active"), active))
            }
            if (status != null) {
                predicates.add(cb.equal(root.get<String>("status"), status))
            }
            if (propertyType != null) {
                predicates.add(cb.equal(root.get<String>("propertyType"), propertyType))
            }
            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get<BigDecimal>("price"), minPrice))
            }
            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get<BigDecimal>("price"), maxPrice))
            }
            if (!search.isNullOrBlank()) {
                val pattern = "%" + search.lowercase() + "%"
                predicates.add(
                    cb.or(
                        cb.like(cb.lower(root.get<String>("title")), pattern),
                        cb.like(cb.lower(root.get<String>("address")), pattern),
                        cb.like(cb.lower(root.get<String>("city")), pattern)
                    )
                )
            }
            cb.and(*predicates.toTypedArray())
        }
    }

    private fun toResponse(property: Property): PropertyResponse {
        return PropertyResponse(
            id = property.id!!,
            realEstateAgencyId = property.realEstateAgencyId!!,
            title = property.title!!,
            description = property.description,
            price = property.price,
            propertyType = property.propertyType,
            bedrooms = property.bedrooms,
            bathrooms = property.bathrooms,
            areaSqm = property.areaSqm,
            address = property.address,
            city = property.city,
            state = property.state,
            zipCode = property.zipCode,
            status = property.status,
            active = property.active,
            createdAt = property.createdAt,
            updatedAt = property.updatedAt
        )
    }
}