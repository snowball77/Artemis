package de.tum.in.www1.artemis.web.websocket.distributed;

import org.springframework.context.annotation.Profile;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

@Service
@Profile("!kafka") // Only load if kafka profile is not active
public class LocalMessagingTemplate implements ArtemisMessagingTemplate {

    private SimpMessageSendingOperations simpMessageSendingOperations;

    public LocalMessagingTemplate(SimpMessageSendingOperations simpMessageSendingOperations) {
        this.simpMessageSendingOperations = simpMessageSendingOperations;
    }

    @Override
    public void convertAndSendToUser(String user, String destination, Object payload) throws MessagingException {
        simpMessageSendingOperations.convertAndSendToUser(user, destination, payload);
    }

    @Override
    public void send(String destination, Message<?> message) throws MessagingException {
        simpMessageSendingOperations.send(destination, message);
    }

    @Override
    public void convertAndSend(String destination, Object payload) throws MessagingException {
        simpMessageSendingOperations.convertAndSend(destination, payload);
    }
}
