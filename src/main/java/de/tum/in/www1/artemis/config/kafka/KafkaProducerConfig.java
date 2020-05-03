package de.tum.in.www1.artemis.config.kafka;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.tum.in.www1.artemis.service.distributed.messages.SynchronizationMessage;
import de.tum.in.www1.artemis.web.websocket.distributed.messageTypes.DistributedWebsocketMessage;

@Configuration
@EnableKafka
@Profile("kafka")
public class KafkaProducerConfig {

    private KafkaProperties kafkaProperties;

    private ObjectMapper objectMapper;

    public KafkaProducerConfig(@Autowired KafkaProperties kafkaProperties, MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter) {
        this.kafkaProperties = kafkaProperties;
        this.objectMapper = mappingJackson2HttpMessageConverter.getObjectMapper().copy();
    }

    @Bean
    public ProducerFactory<String, DistributedWebsocketMessage> distributedWebsocketMessageProducerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootStrapServers());

        return new DefaultKafkaProducerFactory<>(configProps, new StringSerializer(), new JsonSerializer<>(objectMapper));
    }

    @Bean
    public ProducerFactory<String, SynchronizationMessage> distributedSynchronizationMessageProducerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootStrapServers());

        return new DefaultKafkaProducerFactory<>(configProps, new StringSerializer(), new JsonSerializer<>(objectMapper));
    }

    @Bean
    public KafkaTemplate<String, DistributedWebsocketMessage> distributedWebsocketMessageKafkaTemplate() {
        return new KafkaTemplate<>(distributedWebsocketMessageProducerFactory());
    }

    @Bean
    public KafkaTemplate<String, SynchronizationMessage> distributedSynchronizationMessageKafkaTemplate() {
        return new KafkaTemplate<>(distributedSynchronizationMessageProducerFactory());
    }
}
