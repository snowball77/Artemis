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

    private ConcurrentLinkedQueue<ConsumerRecord<K, V>> commitQueue = new ConcurrentLinkedQueue<>();

    private Thread consumerThread;

    private boolean hasReadStarted = false;

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
                if (fetchedRecords.isEmpty() && hasReadStarted) {
                    log.info("Fetched no records, ending polling");
                    break;
                }

                fetchedRecords.forEach(this::addIfNotPresent);

                while (!commitQueue.isEmpty()) {
                    ConsumerRecord<K, V> commitRecord = commitQueue.poll();
                    TopicPartition topicPartition = new TopicPartition(remoteKeyValueStore.getIteratorTopic(), commitRecord.partition());
                    consumer.commitSync(Collections.singletonMap(topicPartition, new OffsetAndMetadata(commitRecord.offset() + 1)));
                }
            }

            while (!commitQueue.isEmpty()) {
                ConsumerRecord<K, V> commitRecord = commitQueue.poll();
                TopicPartition topicPartition = new TopicPartition(remoteKeyValueStore.getIteratorTopic(), commitRecord.partition());
                consumer.commitSync(Collections.singletonMap(topicPartition, new OffsetAndMetadata(commitRecord.offset() + 1)));
            }

            consumer.close();
        };

        consumerThread = new Thread(consumerRunnable);
        consumerThread.start();
    }

    private void addIfNotPresent(ConsumerRecord<K, V> consumerRecord) {
        if (!queue.contains(consumerRecord)) {
            queue.offer(consumerRecord);
            log.info("Added fetched record " + consumerRecord + " to queue");
        }
    }

    @Override
    public boolean hasNext() {
        hasReadStarted = true;

        // Commit last returned record
        if (lastReturnedConsumer != null) {
            commitQueue.offer(lastReturnedConsumer);
        }

        if (!queue.isEmpty()) {
            return true;
        }

        return false;
    }

    @Override
    public K next() {
        // TODO: Simon Leiß: evaluate if there is a better approach
        if (!queue.isEmpty()) {
            lastReturnedConsumer = queue.poll();
            return lastReturnedConsumer.key();
        }

        while (consumerThread.isAlive()) {
            if (!queue.isEmpty()) {
                lastReturnedConsumer = queue.poll();
                return lastReturnedConsumer.key();
            }
            log.info("consumerThread is alive, sleeping");
            try {
                Thread.sleep(10);
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        throw new NoSuchElementException();
    }
}
