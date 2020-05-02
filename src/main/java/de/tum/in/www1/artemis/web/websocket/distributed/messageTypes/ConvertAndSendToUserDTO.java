package de.tum.in.www1.artemis.web.websocket.distributed.messageTypes;

public class ConvertAndSendToUserDTO extends DistributedWebsocketMessage {

    // Needed for Jackson
    private ConvertAndSendToUserDTO() {
    }

    public ConvertAndSendToUserDTO(String user, String destination, String payload) {
        this.user = user;
        this.destination = destination;
        this.payload = payload;
    }

    private String user;

    private String destination;

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
}
