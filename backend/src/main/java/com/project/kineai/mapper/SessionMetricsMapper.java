// ── SessionMetricsMapper  ──
package com.project.kineai.mapper;

import com.project.kineai.dto.request.SaveMetricsRequest;
import com.project.kineai.model.entity.SessionMetrics;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface SessionMetricsMapper {

    @Mapping(target = "id",         ignore = true)
    @Mapping(target = "session",    ignore = true)
    @Mapping(target = "recordedAt", ignore = true)
    SessionMetrics toEntity(SaveMetricsRequest request);
}