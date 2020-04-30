package de.tum.in.www1.artemis.web.websocket.distributed;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;

public interface ArtemisMessagingTemplate {

    void convertAndSendToUser(String user, String destination, Object payload) throws MessagingException;

    void send(String destination, Message<?> message) throws MessagingException;

    void convertAndSend(String destination, Object payload) throws MessagingException;
}
