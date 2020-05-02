package de.tum.in.www1.artemis.web.websocket.distributed.messageTypes;

public abstract class DistributedWebsocketMessage {

    protected String payload;

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }
}
