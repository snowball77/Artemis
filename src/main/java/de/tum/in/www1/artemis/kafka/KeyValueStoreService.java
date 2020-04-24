package de.tum.in.www1.artemis.kafka;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.kafka.common.serialization.Serde;
import org.springframework.stereotype.Service;

/**
 * This is our Factory class for creating new KV-stores
 */
@Service
public class KeyValueStoreService<K, V> {

    private boolean kafkaConnected = false;

    public KeyValueStore<K, V> createKeyValueStore(String topic, Serde<K> keySerde, Serde<V> valueSerde) {
        if (kafkaConnected) {

        }
        else {
            return new LocalKeyValueStore<K, V>(topic, keySerde, valueSerde);
        }

        throw new NotImplementedException("This KV-store is not yet implemented");
    }

    public KeyValueStore createKeyValueStore(String topic) {
        if (kafkaConnected) {

        }
        else {
            return new LocalKeyValueStore<K, V>(topic);
        }

        throw new NotImplementedException("This KV-store is not yet implemented");
    }
}
