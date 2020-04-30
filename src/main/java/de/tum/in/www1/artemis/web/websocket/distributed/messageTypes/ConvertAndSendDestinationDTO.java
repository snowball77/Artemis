package de.tum.in.www1.artemis.web.websocket.distributed.messageTypes;

public class ConvertAndSendDestinationDTO extends DistributedWebsocketMessage {

    private String destination;

    private Object payload;

    public ConvertAndSendDestinationDTO(String destination, Object payload) {
        this.destination = destination;
        this.payload = payload;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public Object getPayload() {
        return payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }
}
