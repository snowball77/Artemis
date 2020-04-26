package de.tum.in.www1.artemis.store;

import java.util.*;
import java.util.regex.Pattern;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.processor.Processor;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.apache.kafka.streams.state.Stores;

import com.google.common.collect.ImmutableMap;

class RemoteKeyValueStore<K, V> extends AbstractKeyValueStore<K, V> {

    private ReadOnlyKeyValueStore<K, V> readOnlyKeyValueStore;

    private Producer<K, V> producer;

    private Producer<K, K> registerProducer;

    RemoteKeyValueStore(String topic) {
        super(topic);
        setupGlobalStore();
        setUpProducer();
        setUpRegisterProducer();
    }

    RemoteKeyValueStore(String topic, Serde<K> keySerde, Serde<V> valueSerde) {
        super(topic, keySerde, valueSerde);
        setupGlobalStore();
        setUpProducer();
        setUpRegisterProducer();
    }

    private void setupGlobalStore() {
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "streams-pipe");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");    // assuming that the Kafka broker this application is talking to runs on local machine with port
                                                                                // 9092
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

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
                        System.out.printf("Old value: %s%n", keyValueStore.get(key));
                        System.out.printf("Storing %s: %s%n", key, value);
                        keyValueStore.put(key, value);
                    }

                    @Override
                    public void close() {
                    }
                });

        topology.addSource("Source", topic).addProcessor("store-" + topic + "-local-processor", () -> new Processor() {

            org.apache.kafka.streams.state.KeyValueStore keyValueStore;

            @Override
            public void init(ProcessorContext processorContext) {
                keyValueStore = (org.apache.kafka.streams.state.KeyValueStore) processorContext.getStateStore("store-" + topic + "-local");
            }

            @Override
            public void process(Object key, Object value) {
                System.out.printf("Old value: %s%n", keyValueStore.get(key));
                System.out.printf("Storing %s: %s%n", key, value);
                keyValueStore.put(key, value);
            }

            @Override
            public void close() {
            }
        }).addStateStore(Stores.keyValueStoreBuilder(Stores.inMemoryKeyValueStore("store-" + topic + "-local"), this.keySerde, this.valueSerde).withLoggingDisabled());

        final KafkaStreams streams = new KafkaStreams(topology, props);
        KafkaStreams.StateListener stateListener = (newState, oldState) -> {
            if (newState.isRunningOrRebalancing()) {
                StoreQueryParameters storeQueryParameters = StoreQueryParameters.fromNameAndType("store-" + topic, QueryableStoreTypes.keyValueStore());
                readOnlyKeyValueStore = (ReadOnlyKeyValueStore<K, V>) streams.store(storeQueryParameters);
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
        ProducerRecord<K, K> producerRecord = new ProducerRecord<>(topic, key, key);
        registerProducer.send(producerRecord);
    }

    @Override
    public V get(K key) {
        return readOnlyKeyValueStore.get(key);
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
    public ImmutableMap<K, V> getAll() {
        Map<K, V> tempMap = new HashMap<>();
        readOnlyKeyValueStore.all().forEachRemaining(k -> tempMap.put(k.key, k.value));
        return ImmutableMap.<K, V>builder().putAll(tempMap).build();
    }

    @Override
    public Iterator<K> iterator() {
        return new RemoteKeyValueStoreIterator<>(this);
    }

    @Override
    public void registerKey(K key) {
        writeToRegisterTopic(key);
    }

    String getIteratorTopic() {
        return topic + "-iterator";
    }
}
