package de.tum.in.www1.artemis.config.kafka;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaHashMapService {

    private Map<String, KafkaHashMap> kafkaHashMaps = new HashMap<>();

    private KafkaTemplate<String, String> kafkaTemplate;

    private final Logger log = LoggerFactory.getLogger(KafkaHashMapService.class);

    public KafkaHashMapService(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Registers a new HashMap
     * @param hashMapId the id of the HashMap
     * @param map the HashMap that should be supported
     */
    private void registerHashMap(String hashMapId, KafkaHashMap map) {
        log.info("Registered HashMap " + map.toString());
        kafkaHashMaps.put(hashMapId, map);
    }

    public <K, V> KafkaHashMap<K, V> createKafkaHashMap(String hashMapId) {
        KafkaHashMap<K, V> kafkaHashMap = new KafkaHashMap<>(hashMapId, this);
        registerHashMap(hashMapId, kafkaHashMap);
        return kafkaHashMap;
    }

    public void putUpdate(String hashMapId, Object key, Object value) {
        log.info(String.format("New value %s for key %s in HashMap %s", value.toString(), key.toString(), hashMapId));
        kafkaTemplate.send("hashmap", String.format("New value %s for key %s in HashMap %s", value.toString(), key.toString(), hashMapId));
    }

    @KafkaListener(topics = "hashmap", groupId = "${kafka.groupid}")
    public void listen(String message) {
        log.info("Received HashMap update: " + message);
    }

}
