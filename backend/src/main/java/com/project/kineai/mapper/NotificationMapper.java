package com.project.kineai.mapper;

import com.project.kineai.dto.response.NotificationResponse;
import com.project.kineai.model.entity.Notification;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    NotificationResponse toResponse(Notification notification);

    List<NotificationResponse> toResponseList(
            List<Notification> notifications);
}