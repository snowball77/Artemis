package de.tum.in.www1.artemis.config.kafka;

import java.util.ArrayList;
import java.util.List;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class KafkaHashMapService {

    private List<KafkaHashMap> kafkaHashMaps = new ArrayList<>();

    /**
     * Registers a new HashMap
     * @param map the HashMap that should be supported
     */
    public void registerHashMap(KafkaHashMap map) {
        kafkaHashMaps.add(map);
    }

    @KafkaListener(topics = "hashmap", groupId = "${kafka.groupid}")
    public void listen(String message) {

    }

}
