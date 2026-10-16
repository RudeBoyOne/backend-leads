package com.br.semesperanca.backendleads.application.mapper

import com.br.semesperanca.backendleads.application.model.request.CreateActivityRequest
import com.br.semesperanca.backendleads.application.model.response.ActivityResponse
import com.br.semesperanca.backendleads.domain.model.Activity
import com.br.semesperanca.backendleads.domain.model.Lead
import com.br.semesperanca.backendleads.domain.model.Property
import org.springframework.stereotype.Component

@Component
class ActivityMapper {

    fun toEntity(request: CreateActivityRequest, lead: Lead, property: Property?): Activity =
        Activity(
            lead = lead,
            property = property,
            title = request.title,
            description = request.description,
            activityDate = request.activityDate,
            completed = false,
        )

    fun toResponse(activity: Activity): ActivityResponse =
        ActivityResponse(
            id = activity.id!!,
            leadId = activity.lead?.id!!,
            propertyId = activity.property?.id,
            title = activity.title!!,
            description = activity.description!!,
            activityDate = activity.activityDate!!,
            completed = activity.completed,
            createdAt = activity.createdAt,
        )
}
