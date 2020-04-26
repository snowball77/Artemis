package de.tum.in.www1.artemis.store;

import java.util.Iterator;

import com.google.common.collect.ImmutableMap;

public interface KeyValueStore<K, V> {

    /**
     * Get the stored value for the given key
     *
     * @param key the key for which the value should be returned
     * @return the value for the given key, or null if not present
     */
    V get(K key);

    /**
     * Insert the given value to the given key
     *
     * @param key the key for which the value should be inserted
     * @param value the value that should be inserted
     */
    void put(K key, V value);

    /**
     * Delete the value of the given key
     * This only deletes the value but does not guarantee to remove the key from the iterator
     *
     * @param key the key for which the value should be deleted
     */
    void delete(K key);

    /**
     * Get all KeyValuePairs in the given storage
     * This map is immutable and can not be changed
     *
     * @return the stored pairs
     */
    ImmutableMap<K, V> getAll();

    /**
     * Iterate over all keys for which this store is responsible.
     * Can only be called once.
     */
    Iterator<K> iterator();

    /**
     * Return the topic this store uses to create the store.
     *
     * @return the topic of the store
     */
    String getTopic();

    /**
     * Register a key to be returned in the {@link #iterator() iterator}.
     *
     * @param key the key that should be registered
     */
    void registerKey(K key);
}
