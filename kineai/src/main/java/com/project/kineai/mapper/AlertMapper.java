package com.project.kineai.mapper;

import com.project.kineai.dto.response.AlertResponse;
import com.project.kineai.model.entity.Alert;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AlertMapper {

    @Mapping(source = "patient.id",       target = "patientId")
    @Mapping(source = "patient.fullName", target = "patientName")
    AlertResponse toResponse(Alert alert);

    List<AlertResponse> toResponseList(List<Alert> alerts);
}