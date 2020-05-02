package de.tum.in.www1.artemis.web.websocket.distributed.messageTypes;

public class ConvertAndSendDestinationDTO extends DistributedWebsocketMessage {

    private String destination;

    // Needed for Jackson
    private ConvertAndSendDestinationDTO() {
    }

    public ConvertAndSendDestinationDTO(String destination, String payload) {
        this.destination = destination;
        this.payload = payload;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }
}
