package de.tum.in.www1.artemis.store;

import java.util.Set;
import java.util.stream.Collectors;

import org.apache.kafka.common.serialization.Serde;
import org.springframework.kafka.support.serializer.JsonSerde;

import com.google.common.collect.ImmutableMap;

abstract class KeyValueStore<K, V> {

    protected String topic;

    protected Serde<K> keySerde;

    protected Serde<V> valueSerde;

    KeyValueStore(String topic) {
        // Default Serdes are JSON Serdes
        this(topic, new JsonSerde<K>(), new JsonSerde<V>());
    }

    KeyValueStore(String topic, Serde<K> keySerde, Serde<V> valueSerde) {
        this.keySerde = keySerde;
        this.valueSerde = valueSerde;
    }

    /**
     * Get the stored value for the given key
     *
     * @param key the key for which the value should be returned
     * @return the value for the given key, or null if not present
     */
    abstract V get(K key);

    /**
     * Insert the given value to the given key
     *
     * @param key the key for which the value should be inserted
     * @param value the value that should be inserted
     */
    abstract void put(K key, V value);

    /**
     * Delete the value of the given key
     *
     * @param key the key for which the value should be deleted
     */
    abstract void delete(K key);

    /**
     * Get all KeyValuePairs in the given storage
     * This map is immutable and can not be changed
     *
     * @return the stored pairs
     */
    public abstract ImmutableMap<K, V> getAll();

    /**
     * Return the keys for which this server is responsible
     *
     * @return the keys for which this server is responsible
     */
    public abstract Set<K> getResponsibleKeys();

    /**
     * Return the keys and values for which this server is responsible
     *
     * @return the keys and values for which this server is responsible
     */
    public ImmutableMap<K, V> getResponsibleKeyValues() {
        return ImmutableMap.<K, V>builder().putAll(getResponsibleKeys().parallelStream().collect(Collectors.toMap(k -> k, this::get))).build();
    }

    protected byte[] serializeKey(K key) {
        return keySerde.serializer().serialize(topic, key);
    }

    protected K deserializeKey(byte[] key) {
        return keySerde.deserializer().deserialize(topic, key);
    }

    protected byte[] serializeValue(V value) {
        return valueSerde.serializer().serialize(topic, value);
    }

    protected V deserializeValue(byte[] value) {
        return valueSerde.deserializer().deserialize(topic, value);
    }
}
