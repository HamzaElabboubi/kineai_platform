package com.project.kineai.mapper;

import com.project.kineai.dto.response.BadgeResponse;
import com.project.kineai.model.entity.Badge;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface BadgeMapper {

    BadgeResponse toResponse(Badge badge);

    List<BadgeResponse> toResponseList(List<Badge> badges);
}