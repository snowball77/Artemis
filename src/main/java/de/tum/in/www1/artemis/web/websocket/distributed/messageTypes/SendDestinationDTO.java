package de.tum.in.www1.artemis.web.websocket.distributed.messageTypes;

import org.springframework.messaging.Message;

public class SendDestinationDTO extends DistributedWebsocketMessage {

    private String destination;

    private Message<?> message;

    public SendDestinationDTO(String destination, Message<?> message) {
        this.destination = destination;
        this.message = message;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public Message<?> getMessage() {
        return message;
    }

    public void setMessage(Message<?> message) {
        this.message = message;
    }
}
