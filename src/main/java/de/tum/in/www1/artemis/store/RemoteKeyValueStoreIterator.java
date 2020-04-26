package de.tum.in.www1.artemis.store;

import java.time.Duration;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;

public class RemoteKeyValueStoreIterator<K, V> implements Iterator<K> {

    private RemoteKeyValueStore remoteKeyValueStore;

    private KafkaConsumer<K, V> consumer;

    private ConcurrentLinkedQueue<ConsumerRecord<K, V>> queue = new ConcurrentLinkedQueue<>();

    private Thread consumerThread;

    private ConsumerRecord<K, V> lastReturnedConsumer = null; // The last item returned by next()

    private String topic;

    RemoteKeyValueStoreIterator(RemoteKeyValueStore remoteKeyValueStore) {
        this.remoteKeyValueStore = remoteKeyValueStore;

        Runnable consumerRunnable = () -> {
            consumer = new KafkaConsumer<>((Properties) null); // TODO: Simon Leiß: Fill properties

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
        if (queue.isEmpty()) {
            throw new NoSuchElementException();
        }

        lastReturnedConsumer = queue.poll();
        return lastReturnedConsumer.key();
    }
}
