package com.project.kineai.service.notification;

import com.project.kineai.model.entity.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Canal email — désactivé par défaut (notifications.email.enabled=false).
 *
 * Pour activer en production :
 * 1. Ajouter spring-boot-starter-mail au pom.xml
 * 2. Configurer spring.mail.* dans application.properties
 * 3. Passer notifications.email.enabled=true
 * 4. Injecter JavaMailSender et implémenter l'envoi réel
 */
@Component
@Slf4j
public class EmailNotificationSender implements NotificationSender {

    @Value("${notifications.email.enabled:false}")
    private boolean emailEnabled;

    @Override
    public void send(Notification notification) {
        if (!emailEnabled) {
            log.debug("Email désactivé — notification {} non"
                    + " envoyée par email", notification.getId());
            return;
        }

        // TODO — provider SMTP à configurer :
        // String to = notification.getPatient().getUser().getEmail();
        // mailSender.send(buildMimeMessage(to, notification));
        log.info("[EMAIL] Envoi simulé à patient {} — type {}",
                notification.getPatient().getId(),
                notification.getType());
    }

    @Override
    public String channelName() {
        return "EMAIL";
    }
}