package de.tum.in.www1.artemis.store;

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
    public V get(K key) {
        return null;
    }

    @Override
    public void put(K key, V value) {

    }

    @Override
    public void delete(K key) {

    }
}
