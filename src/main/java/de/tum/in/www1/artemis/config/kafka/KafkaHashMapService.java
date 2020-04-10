package de.tum.in.www1.artemis.config.kafka;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaHashMapService {

    private List<KafkaHashMap> kafkaHashMaps = new ArrayList<>();

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    private final Logger log = LoggerFactory.getLogger(KafkaHashMapService.class);

    /**
     * Registers a new HashMap
     * @param map the HashMap that should be supported
     */
    public void registerHashMap(KafkaHashMap map) {
        log.info("Registered HashMap " + map.toString());
        kafkaHashMaps.add(map);
    }

    public void putUpdate(KafkaHashMap map, Object key, Object value) {
        kafkaTemplate.send("hashmap", String.format("New value %s for key %s", value.toString(), key, toString()));
    }

    @KafkaListener(topics = "hashmap", groupId = "${kafka.groupid}")
    public void listen(String message) {
        log.info("Received HashMap update: " + message);
    }

}
