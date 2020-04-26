package de.tum.in.www1.artemis.store;

import java.util.Iterator;

import com.google.common.collect.ImmutableMap;

public class KeyValueStoreProxy<K, V> implements KeyValueStore<K, V> {

    private AbstractKeyValueStore<K, V> keyValueStore;

    KeyValueStoreProxy(AbstractKeyValueStore<K, V> keyValueStore) {
        this.keyValueStore = keyValueStore;
    }

    public V get(K key) {
        return keyValueStore.get(key);
    }

    public void put(K key, V value) {
        keyValueStore.put(key, value);
    }

    public void delete(K key) {
        keyValueStore.delete(key);
    }

    public ImmutableMap<K, V> getAll() {
        return keyValueStore.getAll();
    }

    public Iterator<K> iterator() {
        return keyValueStore.iterator();
    }

    public String getTopic() {
        return keyValueStore.getTopic();
    }

    public void registerKey(K key) {
        keyValueStore.registerKey(key);
    }
}
