package de.tum.in.www1.artemis.service;

import java.util.Collections;

import org.apache.kafka.clients.admin.AdminClient;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

@Component
public class KafkaService {

    private AdminClient adminClient;

    public KafkaService(KafkaAdmin kafkaAdmin) {
        this.adminClient = AdminClient.create(kafkaAdmin.getConfig());
    }

    public void deleteTopic(String topic) {
        this.adminClient.deleteTopics(Collections.singletonList(topic));
    }
}
