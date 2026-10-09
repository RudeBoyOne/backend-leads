package com.br.semesperanca.backendleads.domain.service

import com.br.semesperanca.backendleads.application.model.request.CreateActivityRequest
import com.br.semesperanca.backendleads.application.model.request.UpdateActivityRequest
import com.br.semesperanca.backendleads.application.model.response.ActivityResponse
import com.br.semesperanca.backendleads.domain.model.Activity
import com.br.semesperanca.backendleads.domain.model.Lead
import com.br.semesperanca.backendleads.domain.repository.ActivityRepository
import com.br.semesperanca.backendleads.domain.repository.LeadRepository
import com.br.semesperanca.backendleads.domain.repository.PropertyRepository
import jakarta.persistence.criteria.Predicate
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ActivityService(
    private val repository: ActivityRepository,
    private val leadRepository: LeadRepository,
    private val propertyRepository: PropertyRepository,
) {
    @Transactional
    fun create(request: CreateActivityRequest): ActivityResponse {
        val lead =
            leadRepository
                .findById(request.leadId)
                .orElseThrow { RuntimeException("Lead não encontrado") }
        val property =
            request.propertyId?.let {
                propertyRepository.findById(it).orElseThrow { RuntimeException("Imóvel não encontrado") }
            }

        val activity =
            Activity(
                lead = lead,
                property = property,
                title = request.title,
                description = request.description,
                activityDate = request.activityDate,
            )

        return toResponse(repository.save(activity))
    }

    fun findById(id: UUID): ActivityResponse = toResponse(findActivityById(id))

    fun findAll(
        leadId: UUID?,
        propertyId: UUID?,
        search: String?,
        pageable: Pageable,
    ): Page<ActivityResponse> = repository.findAll(buildSpec(leadId, propertyId, search), pageable).map { toResponse(it) }

    @Transactional
    fun update(
        id: UUID,
        request: UpdateActivityRequest,
    ): ActivityResponse {
        val activity = findActivityById(id)
        val property =
            request.propertyId?.let {
                propertyRepository.findById(it).orElseThrow { RuntimeException("Imóvel não encontrado") }
            }

        activity.property = property
        activity.title = request.title
        activity.description = request.description
        activity.activityDate = request.activityDate

        return toResponse(repository.save(activity))
    }

    @Transactional
    fun delete(id: UUID) {
        repository.delete(findActivityById(id))
    }

    private fun findActivityById(id: UUID): Activity = repository.findById(id).orElseThrow { RuntimeException("Atividade não encontrada") }

    private fun buildSpec(
        leadId: UUID?,
        propertyId: UUID?,
        search: String?,
    ): Specification<Activity> =
        Specification { root, _, cb ->
            val predicates = mutableListOf<Predicate>()
            if (leadId != null) {
                predicates.add(cb.equal(root.get<Lead>("lead").get<Any>("id"), leadId))
            }
            if (propertyId != null) {
                predicates.add(cb.equal(root.get<Any>("property").get<Any>("id"), propertyId))
            }
            if (!search.isNullOrBlank()) {
                val pattern = "%" + search.lowercase() + "%"
                predicates.add(
                    cb.or(
                        cb.like(cb.lower(root.get<String>("title")), pattern),
                        cb.like(cb.lower(root.get<String>("description")), pattern),
                    ),
                )
            }
            cb.and(*predicates.toTypedArray())
        }

    private fun toResponse(activity: Activity): ActivityResponse =
        ActivityResponse(
            id = activity.id!!,
            leadId = activity.lead?.id!!,
            propertyId = activity.property?.id,
            title = activity.title!!,
            description = activity.description!!,
            activityDate = activity.activityDate!!,
            createdAt = activity.createdAt,
        )
}
