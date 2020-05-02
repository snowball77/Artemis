package de.tum.in.www1.artemis.service.distributed;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import de.tum.in.www1.artemis.service.distributed.messages.SynchronizationMessage;

@Service
@Profile("kafka")
public class DistributedSynchronizationService implements SynchronizationService {

    @Override
    public void informServers(SynchronizationMessage synchronizationMessage) {

    }
}
