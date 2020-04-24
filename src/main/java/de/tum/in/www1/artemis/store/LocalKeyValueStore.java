package de.tum.in.www1.artemis.store;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.kafka.common.serialization.Serde;

public class LocalKeyValueStore<K, V> extends KeyValueStore<K, V> {

    private Map<K, V> localStorage = new ConcurrentHashMap<>();

    LocalKeyValueStore(String topic) {
        super(topic);
    }

    LocalKeyValueStore(String topic, Serde<K> keySerde, Serde<V> valueSerde) {
        super(topic, keySerde, valueSerde);
    }

    @Override
    public V get(K key) {
        return localStorage.get(key);
    }

    @Override
    public void put(K key, V value) {
        localStorage.put(key, value);
    }

    @Override
    public void delete(K key) {
        localStorage.remove(key);
    }
}
