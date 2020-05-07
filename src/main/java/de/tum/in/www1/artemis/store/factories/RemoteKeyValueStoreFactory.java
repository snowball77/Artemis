package de.tum.in.www1.artemis.store.factories;

import org.apache.kafka.common.serialization.Serde;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import de.tum.in.www1.artemis.service.KafkaService;
import de.tum.in.www1.artemis.store.KeyValueStore;
import de.tum.in.www1.artemis.store.KeyValueStoreProxy;
import de.tum.in.www1.artemis.store.RemoteKeyValueStore;

@Service
@Profile("kafka")
public class RemoteKeyValueStoreFactory<K, V> implements KeyValueStoreFactory<K, V> {

    private KafkaService kafkaService;

    public RemoteKeyValueStoreFactory(KafkaService kafkaService) {
        this.kafkaService = kafkaService;
    }

    @Override
    public KeyValueStore<K, V> createKeyValueStore(String topic, Serde<K> keySerde, Serde<V> valueSerde) {
        return new KeyValueStoreProxy<>(new RemoteKeyValueStore<>(topic, keySerde, valueSerde, kafkaService));
    }

    @Override
    public KeyValueStore<K, V> createKeyValueStore(String topic, Class<? super K> keyClassType, Class<? super V> valueClassType) {
        return new KeyValueStoreProxy<>(new RemoteKeyValueStore<>(topic, keyClassType, valueClassType, kafkaService));
    }
}
