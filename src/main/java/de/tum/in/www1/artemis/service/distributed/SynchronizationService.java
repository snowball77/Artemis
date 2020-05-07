package de.tum.in.www1.artemis.service.distributed;

import de.tum.in.www1.artemis.service.distributed.messages.SynchronizationMessage;

public interface SynchronizationService {

    /**
     * Inform other instances that something happened on this instance of Artemis
     * @param synchronizationMessage the message that should be sent to the other servers
     */
    void informServers(SynchronizationMessage synchronizationMessage);
}
