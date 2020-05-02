package de.tum.in.www1.artemis.service.distributed.messages;

public abstract class SynchronizationMessage {

    private String sendingServer;

    public SynchronizationMessage(String sendingServer) {
        this.sendingServer = sendingServer;
    }

    public String getSendingServer() {
        return sendingServer;
    }

    public void setSendingServer(String sendingServer) {
        this.sendingServer = sendingServer;
    }
}
