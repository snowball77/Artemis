package de.tum.in.www1.artemis.config.kafka;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import de.tum.in.www1.artemis.web.websocket.distributed.messageTypes.DistributedWebsocketMessage;

@EnableKafka
@Configuration
@Profile("kafka")
public class KafkaConsumerConfig {

    private KafkaProperties kafkaProperties;

    private ObjectMapper objectMapper;

    @Value("${artemis.kafka.group-id}")
    private String groupId;

    public KafkaConsumerConfig(KafkaProperties kafkaProperties, MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter) {
        this.kafkaProperties = kafkaProperties;
        this.objectMapper = mappingJackson2HttpMessageConverter.getObjectMapper().copy();

        this.objectMapper.activateDefaultTyping(BasicPolymorphicTypeValidator.builder().allowIfBaseType(Object.class).build());
    }

    @Bean
    public ConsumerFactory<String, DistributedWebsocketMessage> distributedWebsocketMessageConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootStrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);

        Map<String, Object> deserProps = new HashMap<>();
        deserProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        JsonDeserializer<DistributedWebsocketMessage> jsonDeserializer = new JsonDeserializer<>(objectMapper);
        jsonDeserializer.configure(deserProps, false);

        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), jsonDeserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, DistributedWebsocketMessage> kafkaListenerContainerFactory() {

        ConcurrentKafkaListenerContainerFactory<String, DistributedWebsocketMessage> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(distributedWebsocketMessageConsumerFactory());
        return factory;
    }
}
