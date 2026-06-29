package com.project.kineai.mapper;

import com.project.kineai.dto.response.KineResponse;
import com.project.kineai.model.entity.Kinesitherapeute;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface KineMapper {
    @Mapping(target = "patientCount",
            expression = "java(kine.getPatients() != null ? kine.getPatients().size() : 0)")
    @Mapping(source = "user.email", target = "email")
    @Mapping(source = "user.active", target = "active")
    KineResponse toResponse(Kinesitherapeute kine);

    List<KineResponse> toResponseList(List<Kinesitherapeute> kines);
}