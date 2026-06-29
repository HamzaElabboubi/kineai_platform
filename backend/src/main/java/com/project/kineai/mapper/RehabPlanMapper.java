package com.project.kineai.mapper;

import com.project.kineai.dto.response.RehabPlanResponse;
import com.project.kineai.model.entity.RehabPlan;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RehabPlanMapper {

    @Mapping(source = "patient.id", target = "patientId")
    RehabPlanResponse toResponse(RehabPlan plan);

    List<RehabPlanResponse> toResponseList(List<RehabPlan> plans);
}