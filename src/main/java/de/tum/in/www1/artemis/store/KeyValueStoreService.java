package de.tum.in.www1.artemis.store;

import org.apache.kafka.common.serialization.Serde;
import org.springframework.stereotype.Service;

/**
 * This is our Factory class for creating new KV-stores
 */
@Service
public class KeyValueStoreService<K, V> {

    private boolean kafkaConnected = false;

    public KeyValueStoreProxy<K, V> createKeyValueStore(String topic, Serde<K> keySerde, Serde<V> valueSerde) {
        if (kafkaConnected) {
            return new KeyValueStoreProxy<>(new RemoteKeyValueStore<>(topic, keySerde, valueSerde));
        }
        else {
            return new KeyValueStoreProxy<>(new LocalKeyValueStore<>(topic, keySerde, valueSerde));
        }
    }

    public KeyValueStoreProxy<K, V> createKeyValueStore(String topic) {
        if (kafkaConnected) {
            return new KeyValueStoreProxy<>(new RemoteKeyValueStore<>(topic));
        }
        else {
            return new KeyValueStoreProxy<>(new LocalKeyValueStore<>(topic));
        }

    }
}
