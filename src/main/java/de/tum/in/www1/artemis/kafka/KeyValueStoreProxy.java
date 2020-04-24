package de.tum.in.www1.artemis.kafka;

import org.apache.kafka.common.serialization.Serde;

public class KeyValueStoreProxy<K, V> extends KeyValueStore<K, V> {

    private KeyValueStore keyValueStore;

    public KeyValueStoreProxy(String topic) {
        super(topic);
    }

    public KeyValueStoreProxy(String topic, Serde<K> keySerde, Serde<V> valueSerde) {
        super(topic, keySerde, valueSerde);
    }

    @Override
    V get(K key) {
        return null;
    }

    @Override
    void put(K key, V value) {

    }

    @Override
    void delete(K key) {

    }
}
