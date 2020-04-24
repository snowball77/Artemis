package de.tum.in.www1.artemis.store;

import java.util.Set;

import com.google.common.collect.ImmutableMap;

public class KeyValueStoreProxy<K, V> {

    private KeyValueStore<K, V> keyValueStore;

    public KeyValueStoreProxy(KeyValueStore<K, V> keyValueStore) {
        this.keyValueStore = keyValueStore;
    }

    /**
     * Get the stored value for the given key
     *
     * @param key the key for which the value should be returned
     * @return the value for the given key, or null if not present
     */
    public V get(K key) {
        return keyValueStore.get(key);
    }

    /**
     * Insert the given value to the given key
     *
     * @param key the key for which the value should be inserted
     * @param value the value that should be inserted
     */
    public void put(K key, V value) {
        keyValueStore.put(key, value);
    }

    /**
     * Delete the value of the given key
     *
     * @param key the key for which the value should be deleted
     */
    public void delete(K key) {
        keyValueStore.delete(key);
    }

    /**
     * Get all KeyValuePairs in the given storage
     * This map is immutable and can not be changed
     *
     * @return the stored pairs
     */
    public ImmutableMap<K, V> getAll() {
        return keyValueStore.getAll();
    }

    /**
     * Return the keys for which this server is responsible
     *
     * @return the keys for which this server is responsible
     */
    public Set<K> getResponsibleKeys() {
        return keyValueStore.getResponsibleKeys();
    }

    /**
     * Return the keys and values for which this server is responsible
     *
     * @return the keys and values for which this server is responsible
     */
    public ImmutableMap<K, V> getResponsibleKeyValues() {
        return keyValueStore.getResponsibleKeyValues();
    }
}
