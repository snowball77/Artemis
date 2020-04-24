package de.tum.in.www1.artemis.store;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.kafka.common.serialization.Serde;

import com.google.common.collect.ImmutableMap;

class LocalKeyValueStore<K, V> extends KeyValueStore<K, V> {

    private Map<K, V> localStorage = new ConcurrentHashMap<>();

    LocalKeyValueStore(String topic) {
        super(topic);
    }

    LocalKeyValueStore(String topic, Serde<K> keySerde, Serde<V> valueSerde) {
        super(topic, keySerde, valueSerde);
    }

    @Override
    V get(K key) {
        return localStorage.get(key);
    }

    @Override
    void put(K key, V value) {
        localStorage.put(key, value);
    }

    @Override
    void delete(K key) {
        localStorage.remove(key);
    }

    @Override
    public ImmutableMap<K, V> getAll() {
        return ImmutableMap.<K, V>builder().putAll(localStorage).build();
    }

    @Override
    public Set<K> getResponsibleKeys() {
        return localStorage.keySet();
    }

    @Override
    public ImmutableMap<K, V> getResponsibleKeyValues() {
        // In the local key value store, the server is always responsible for the whole range
        return getAll();
    }
}
