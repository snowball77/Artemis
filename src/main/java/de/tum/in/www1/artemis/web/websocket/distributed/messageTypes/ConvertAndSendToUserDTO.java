package de.tum.in.www1.artemis.web.websocket.distributed.messageTypes;

public class ConvertAndSendToUserDTO extends DistributedWebsocketMessage {

    public ConvertAndSendToUserDTO(String user, String destination, Object payload) {
        this.user = user;
        this.destination = destination;
        this.payload = payload;
    }

    private String user;

    private String destination;

    private Object payload;

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
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
