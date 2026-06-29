package com.project.kineai.mapper;

import com.project.kineai.dto.request.UpdatePatientRequest;
import com.project.kineai.dto.response.PatientResponse;
import com.project.kineai.model.entity.Patient;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import java.util.List;

@Mapper(componentModel = "spring")
public interface PatientMapper {

    // ✅ toResponse — source = Patient, target = PatientResponse
    // Ignorer les champs de Patient qui n'existent pas dans PatientResponse
    @Mapping(source = "kine.id",       target = "kineId")
    @Mapping(source = "kine.fullName", target = "kineName")
    @Mapping(source = "user.active",   target = "isActive")
    PatientResponse toResponse(Patient patient);

    // ✅ toResponseList — MapStruct génère automatiquement
    List<PatientResponse> toResponseList(List<Patient> patients);

    // ✅ updateEntity — source = UpdatePatientRequest, target = Patient
    // Ignorer les champs de Patient non présents dans UpdatePatientRequest
    @Mapping(target = "id",          ignore = true)
    @Mapping(target = "user",        ignore = true)
    @Mapping(target = "kine",        ignore = true)
    @Mapping(target = "pathology",   ignore = true)
    @Mapping(target = "level",       ignore = true)
    @Mapping(target = "streakCount", ignore = true)
    @Mapping(target = "totalXp",     ignore = true)
    void updateEntity(UpdatePatientRequest request,
                      @MappingTarget Patient patient);
}