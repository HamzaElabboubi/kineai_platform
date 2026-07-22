package com.project.kineai.service.notification;

import com.project.kineai.model.entity.Notification;

/**
 * Abstraction d'envoi de notification vers un canal externe
 * (email, SMS, push...). Le stockage "in-app" est géré par
 * NotificationService directement — seuls les canaux EXTERNES
 * implémentent cette interface.
 *
 * Ajouter un nouveau canal (SMS, push mobile...) = créer une
 * classe @Component qui implémente NotificationSender, rien
 * d'autre à modifier (Spring l'injecte automatiquement dans
 * NotificationService via List<NotificationSender>).
 */
public interface NotificationSender {

    void send(Notification notification);

    String channelName();
}