package de.tum.in.www1.artemis.store;

import org.apache.kafka.common.serialization.Serde;
import org.springframework.kafka.support.serializer.JsonSerde;

public abstract class KeyValueStore<K, V> {

    protected String topic;

    protected Serde<K> keySerde;

    protected Serde<V> valueSerde;

    public KeyValueStore(String topic) {
        // Default Serdes are JSON Serdes
        this(topic, new JsonSerde<K>(), new JsonSerde<V>());
    }

    public KeyValueStore(String topic, Serde<K> keySerde, Serde<V> valueSerde) {
        this.keySerde = keySerde;
        this.valueSerde = valueSerde;
    }

    /**
     * Get the stored value for the given key
     *
     * @param key the key for which the value should be returned
     * @return the value for the given key, or null if not present
     */
    public abstract V get(K key);

    /**
     * Insert the given value to the given key
     *
     * @param key the key for which the value should be inserted
     * @param value the value that should be inserted
     */
    public abstract void put(K key, V value);

    /**
     * Delete the value of the given key
     *
     * @param key the key for which the value should be deleted
     */
    public abstract void delete(K key);

    private byte[] serializeKey(K key) {
        return keySerde.serializer().serialize(topic, key);
    }

    private K deserializeKey(byte[] key) {
        return keySerde.deserializer().deserialize(topic, key);
    }

    private byte[] serializeValue(V value) {
        return valueSerde.serializer().serialize(topic, value);
    }

    private V deserializeValue(byte[] value) {
        return valueSerde.deserializer().deserialize(topic, value);
    }
}
