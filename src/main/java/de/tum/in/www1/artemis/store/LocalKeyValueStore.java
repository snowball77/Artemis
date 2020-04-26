package de.tum.in.www1.artemis.store;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.kafka.common.serialization.Serde;

import com.google.common.collect.ImmutableMap;

class LocalKeyValueStore<K, V> extends AbstractKeyValueStore<K, V> {

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

    @Override
    public ImmutableMap<K, V> getAll() {
        return ImmutableMap.<K, V>builder().putAll(localStorage).build();
    }

    @Override
    public Iterator<K> iterator() {
        return localStorage.keySet().iterator();
    }

    @Override
    public void registerKey(K key) {
        // Nothing to do here as all used keys are registered by default in the HashMap
    }
}
