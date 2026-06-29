package com.project.kineai.mapper;

import com.project.kineai.dto.response.AuthResponse;
import com.project.kineai.model.entity.Kinesitherapeute;
import com.project.kineai.model.entity.Patient;
import com.project.kineai.model.entity.User;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface UserMapper {

    // ── Login Patient ─────────────────────────
    @Mapping(source = "user.id",          target = "userId")
    @Mapping(source = "user.email",       target = "email")
    @Mapping(source = "user.role",        target = "role")
    @Mapping(source = "patient.fullName", target = "fullName")
    @Mapping(target = "accessToken",      ignore = true)
    @Mapping(target = "refreshToken",     ignore = true)
    @Mapping(target = "tokenType",        ignore = true)
    AuthResponse toAuthResponse(User user, Patient patient);

    // ── Login Kiné ────────────────────────────
    @Mapping(source = "user.id",      target = "userId")
    @Mapping(source = "user.email",   target = "email")
    @Mapping(source = "user.role",    target = "role")
    @Mapping(source = "kine.fullName",target = "fullName")
    @Mapping(target = "accessToken",  ignore = true)
    @Mapping(target = "refreshToken", ignore = true)
    @Mapping(target = "tokenType",    ignore = true)
    AuthResponse toAuthResponse(User user, Kinesitherapeute kine);
}