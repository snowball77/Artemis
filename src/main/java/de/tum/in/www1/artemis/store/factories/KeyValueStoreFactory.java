package de.tum.in.www1.artemis.store.factories;

import org.apache.kafka.common.serialization.Serde;

import de.tum.in.www1.artemis.store.KeyValueStore;

public interface KeyValueStoreFactory<K, V> {

    KeyValueStore<K, V> createKeyValueStore(String topic, Serde<K> keySerde, Serde<V> valueSerde);

    KeyValueStore<K, V> createKeyValueStore(String topic, Class<? super K> keyClassType, Class<? super V> valueClassType);
}
