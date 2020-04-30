package de.tum.in.www1.artemis.store.factories;

import org.apache.kafka.common.serialization.Serde;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import de.tum.in.www1.artemis.store.KeyValueStore;
import de.tum.in.www1.artemis.store.KeyValueStoreProxy;
import de.tum.in.www1.artemis.store.LocalKeyValueStore;

@Service
@Profile("!kafka")
public class LocalKeyValueStoreFactory<K, V> implements KeyValueStoreFactory<K, V> {

    @Override
    public KeyValueStore<K, V> createKeyValueStore(String topic, Serde<K> keySerde, Serde<V> valueSerde) {
        return new KeyValueStoreProxy<>(new LocalKeyValueStore<>(topic, keySerde, valueSerde));
    }

    @Override
    public KeyValueStore<K, V> createKeyValueStore(String topic, Class<? super K> keyClassType, Class<? super V> valueClassType) {
        return new KeyValueStoreProxy<>(new LocalKeyValueStore<>(topic));
    }
}
