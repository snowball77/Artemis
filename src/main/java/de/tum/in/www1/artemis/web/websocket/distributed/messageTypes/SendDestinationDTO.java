package de.tum.in.www1.artemis.web.websocket.distributed.messageTypes;

import java.util.HashMap;
import java.util.Map;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.GenericMessage;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class SendDestinationDTO extends DistributedWebsocketMessage {

    enum MessageType {
        GENERIC, ERROR, UNDEFINED
    }

    private String destination;

    private Map<String, Object> headers;

    private MessageType messageType;

    // Needed for Jackson
    private SendDestinationDTO() {
    }

    public SendDestinationDTO(String destination, Message<?> message, String payload) {
        this.destination = destination;
        this.headers = new HashMap<>(message.getHeaders());
        this.payload = payload;

        if (message instanceof ErrorMessage) { // Check ErrorMessage first as every ErrorMessage is a GenericMessage
            messageType = MessageType.ERROR;
        }
        else if (message instanceof GenericMessage) {
            messageType = MessageType.GENERIC;
        }
        else {
            messageType = MessageType.UNDEFINED;
        }
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    @JsonIgnore
    public MessageHeaders getHeaders() {
        return new MessageHeaders(headers);
    }

    public void setHeaders(Map<String, Object> headers) {
        this.headers = headers;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    @JsonIgnore
    public Message getMessage() {
        switch (messageType) {
        case GENERIC:
            return new GenericMessage<>(payload, headers);

        default:
            throw new IllegalArgumentException("Undefined MessageType received, cannot construct");
        }
    }
}
