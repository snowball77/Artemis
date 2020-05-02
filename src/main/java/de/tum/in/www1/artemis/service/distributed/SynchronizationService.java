package de.tum.in.www1.artemis.service.distributed;

import de.tum.in.www1.artemis.service.distributed.messages.SynchronizationMessage;

public interface SynchronizationService {

    void informServers(SynchronizationMessage synchronizationMessage);
}
