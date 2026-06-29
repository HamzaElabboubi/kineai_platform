package com.project.kineai.mapper;

import com.project.kineai.dto.request.SaveMetricsRequest;
import com.project.kineai.dto.response.SessionResponse;
import com.project.kineai.model.entity.Session;
import com.project.kineai.model.entity.SessionMetrics;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SessionMapper {

    @Mapping(source = "patient.id",    target = "patientId")
    @Mapping(source = "exercise.id",   target = "exerciseId")
    @Mapping(source = "exercise.name", target = "exerciseName")
    @Mapping(source = "sessionStatus", target = "status")
    SessionResponse toResponse(Session session);

    List<SessionResponse> toResponseList(List<Session> sessions);



}