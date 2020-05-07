package de.tum.in.www1.artemis.store;

import java.util.*;
import java.util.regex.Pattern;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.errors.InvalidStateStoreException;
import org.apache.kafka.streams.processor.Processor;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.apache.kafka.streams.state.Stores;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import com.google.common.collect.ImmutableMap;
import de.tum.in.www1.artemis.service.KafkaService;

public class RemoteKeyValueStore<K, V> extends AbstractKeyValueStore<K, V> {

    private KafkaService kafkaService;

    private ReadOnlyKeyValueStore<K, V> readOnlyKeyValueStore;

    private Producer<K, V> producer;

    private Producer<K, K> registerProducer;

    private RemoteKeyValueStoreIterator<K, V> remoteKeyValueStoreIterator;

    private Class<? super K> keyClassType;

    private Class<? super V> valueClassType;

    public RemoteKeyValueStore(String topic, Class<? super K> keyClassType, Class<? super V> valueClassType, KafkaService kafkaService) {
        super(topic);
        this.kafkaService = kafkaService;

        setupGlobalStore(keyClassType, valueClassType);
        setUpProducer();
        setUpRegisterProducer();

        remoteKeyValueStoreIterator = new RemoteKeyValueStoreIterator<>(this);
    }

    public RemoteKeyValueStore(String topic, Serde<K> keySerde, Serde<V> valueSerde, KafkaService kafkaService) {
        super(topic, keySerde, valueSerde);
        this.kafkaService = kafkaService;

        setupGlobalStore(null, null);
        setUpProducer();
        setUpRegisterProducer();

        remoteKeyValueStoreIterator = new RemoteKeyValueStoreIterator<>(this);
    }

    private void setupGlobalStore(Class<? super K> keyClassType, Class<? super V> valueClassType) {
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "streams-pipe");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");    // assuming that the Kafka broker this application is talking to runs on local machine with port
                                                                                // 9092

        if (this.keySerde.deserializer() instanceof JsonDeserializer) {
            // https://stackoverflow.com/a/60583173/3802758
            JsonDeserializer jsonDeserializer = (JsonDeserializer) this.keySerde.deserializer();
            Map<String, Object> deserProps = new HashMap<>();
            deserProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
            deserProps.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, true);

            if (keyClassType != null) {
                deserProps.put(JsonDeserializer.KEY_DEFAULT_TYPE, keyClassType.getName());
            }
            jsonDeserializer.configure(deserProps, true);
        }

        if (this.valueSerde.deserializer() instanceof JsonDeserializer) {
            // https://stackoverflow.com/a/60583173/3802758
            JsonDeserializer jsonDeserializer = (JsonDeserializer) this.valueSerde.deserializer();
            Map<String, Object> deserProps = new HashMap<>();
            deserProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
            deserProps.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, true);

            if (valueClassType != null) {
                deserProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, valueClassType.getName());
            }
            jsonDeserializer.configure(deserProps, false);
        }

        final StreamsBuilder builder = new StreamsBuilder();
        builder.stream(Pattern.compile("dummy"));
        final Topology topology = builder.build();
        topology.addGlobalStore(Stores.keyValueStoreBuilder(Stores.inMemoryKeyValueStore("store-" + topic), this.keySerde, this.valueSerde).withLoggingDisabled(), topic,
                this.keySerde.deserializer(), this.valueSerde.deserializer(), topic, "store-processor", () -> new Processor() {

                    org.apache.kafka.streams.state.KeyValueStore keyValueStore;

                    @Override
                    public void init(ProcessorContext processorContext) {
                        keyValueStore = (org.apache.kafka.streams.state.KeyValueStore) processorContext.getStateStore("store-" + topic);
                    }

                    @Override
                    public void process(Object key, Object value) {
                        keyValueStore.put(key, value);
                    }

                    @Override
                    public void close() {
                    }
                });

        final KafkaStreams streams = new KafkaStreams(topology, props);
        KafkaStreams.StateListener stateListener = (newState, oldState) -> {
            if (newState.isRunning()) {
                try {
                    readOnlyKeyValueStore = streams.store("store-" + topic, QueryableStoreTypes.keyValueStore());
                }
                catch (InvalidStateStoreException ignored) {
                }
            }
        };
        streams.setStateListener(stateListener);
        streams.start();
    }

    private void setUpProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");    // assuming that the Kafka broker this application is talking to runs on local machine with port
                                                                                 // 9092
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, keySerde.serializer().getClass());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, valueSerde.serializer().getClass());
        producer = new KafkaProducer<>(props);
    }

    private void setUpRegisterProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");    // assuming that the Kafka broker this application is talking to runs on local machine with port
                                                                                 // 9092
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, keySerde.serializer().getClass());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, keySerde.serializer().getClass());
        registerProducer = new KafkaProducer<>(props);
    }

    private void writeToTopic(K key, V value) {
        ProducerRecord<K, V> producerRecord = new ProducerRecord<>(topic, key, value);
        producer.send(producerRecord);
    }

    private void writeToRegisterTopic(K key) {
        ProducerRecord<K, K> producerRecord = new ProducerRecord<>(getIteratorTopic(), key, key);
        registerProducer.send(producerRecord);
    }

    @Override
    public V get(K key) {
        return readOnlyKeyValueStore != null ? readOnlyKeyValueStore.get(key) : null;
    }

    @Override
    public void put(K key, V value) {
        writeToTopic(key, value);
    }

    @Override
    public void delete(K key) {
        writeToTopic(key, null);
    }

    @Override
    public boolean exists(K key) {
        return get(key) != null;
    }

    @Override
    public ImmutableMap<K, V> getAll() {
        Map<K, V> tempMap = new HashMap<>();
        readOnlyKeyValueStore.all().forEachRemaining(k -> tempMap.put(k.key, k.value));
        return ImmutableMap.<K, V>builder().putAll(tempMap).build();
    }

    @Override
    public Iterator<K> iterator() {
        return remoteKeyValueStoreIterator;
    }

    @Override
    public void registerKey(K key) {
        writeToRegisterTopic(key);
    }

    @Override
    public void clear() {
        kafkaService.deleteTopic(getTopic());
        kafkaService.deleteTopic(getIteratorTopic());

        setupGlobalStore(keyClassType, valueClassType);
    }

    String getIteratorTopic() {
        return topic + "-iterator";
    }
}
