package de.tum.in.www1.artemis.store;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteKeyValueStoreIterator<K, V> implements Iterator<K> {

    private static final Logger log = LoggerFactory.getLogger(RemoteKeyValueStoreIterator.class);

    private RemoteKeyValueStore remoteKeyValueStore;

    private KafkaConsumer<K, V> consumer;

    private ConcurrentLinkedQueue<ConsumerRecord<K, V>> queue = new ConcurrentLinkedQueue<>();

    private Thread consumerThread;

    private ConsumerRecord<K, V> lastReturnedConsumer = null; // The last item returned by next()

    RemoteKeyValueStoreIterator(RemoteKeyValueStore remoteKeyValueStore) {
        this.remoteKeyValueStore = remoteKeyValueStore;

        Runnable consumerRunnable = () -> {
            Properties props = new Properties();
            props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");    // assuming that the Kafka broker this application is talking to runs on local machine with
                                                                                     // port 9092
            props.put(ConsumerConfig.GROUP_ID_CONFIG, "group1"); // TODO: Make group id unique
            props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, remoteKeyValueStore.keySerde.deserializer().getClass());
            props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, remoteKeyValueStore.valueSerde.deserializer().getClass());
            consumer = new KafkaConsumer<>(props);
            consumer.subscribe(List.of(remoteKeyValueStore.getIteratorTopic()));

            while (true) {
                ConsumerRecords<K, V> fetchedRecords = consumer.poll(Duration.ofMillis(100));
                if (fetchedRecords.isEmpty()) {
                    break;
                }

                fetchedRecords.forEach(queue::offer);
            }
        };

        consumerThread = new Thread(consumerRunnable);
        consumerThread.start();
    }

    @Override
    public boolean hasNext() {
        // Commit last returned record
        if (lastReturnedConsumer != null) {
            TopicPartition topicPartition = new TopicPartition(remoteKeyValueStore.getIteratorTopic(), lastReturnedConsumer.partition());
            consumer.commitSync(Collections.singletonMap(topicPartition, new OffsetAndMetadata(lastReturnedConsumer.offset() + 1)));
        }

        if (consumerThread.isAlive() || !queue.isEmpty()) {
            return true;
        }

        consumer.close();
        return false;
    }

    @Override
    public K next() {
        while (consumer == null) {
            try {
                Thread.sleep(10);
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (queue.isEmpty()) {
            throw new NoSuchElementException();
        }

        lastReturnedConsumer = queue.poll();
        return lastReturnedConsumer.key();
    }
}
