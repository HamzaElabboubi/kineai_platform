package com.project.kineai.dto.response;

import com.project.kineai.model.enums.BadgeType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class BadgeResponse {
    private UUID id;
    private BadgeType badgeType;
    private LocalDateTime unlockedAt;
    private Boolean displayed;
}
