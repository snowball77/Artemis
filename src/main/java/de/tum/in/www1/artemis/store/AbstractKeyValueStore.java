package de.tum.in.www1.artemis.store;

import org.apache.kafka.common.serialization.Serde;
import org.springframework.kafka.support.serializer.JsonSerde;

public abstract class AbstractKeyValueStore<K, V> implements KeyValueStore<K, V> {

    String topic;

    Serde<K> keySerde;

    Serde<V> valueSerde;

    AbstractKeyValueStore(String topic) {
        // Default Serdes are JSON Serdes
        this(topic, new JsonSerde<K>(), new JsonSerde<V>());
    }

    AbstractKeyValueStore(String topic, Serde<K> keySerde, Serde<V> valueSerde) {
        this.topic = topic;
        this.keySerde = keySerde;
        this.valueSerde = valueSerde;
    }

    @Override
    public String getTopic() {
        return topic;
    }
}
