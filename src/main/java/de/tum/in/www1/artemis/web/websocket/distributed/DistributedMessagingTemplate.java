package de.tum.in.www1.artemis.web.websocket.distributed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.tum.in.www1.artemis.web.websocket.distributed.messageTypes.*;

@Service
@Profile("kafka")
@KafkaListener(groupId = "${artemis.kafka.group-id}", topics = "websocket-synchronize")
public class DistributedMessagingTemplate implements ArtemisMessagingTemplate {

    private static final Logger log = LoggerFactory.getLogger(DistributedMessagingTemplate.class);

    private static final String WEBSOCKET_SYNCHRONIZE_TOPIC = "websocket-synchronize";

    private SimpMessageSendingOperations websocketTemplate;

    private KafkaTemplate<String, DistributedWebsocketMessage> kafkaTemplate;

    private ObjectMapper objectMapper;

    DistributedMessagingTemplate(SimpMessageSendingOperations websocketTemplate, KafkaTemplate<String, DistributedWebsocketMessage> distributedWebsocketMessageKafkaTemplate,
            MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter) {
        this.websocketTemplate = websocketTemplate;
        this.kafkaTemplate = distributedWebsocketMessageKafkaTemplate;
        this.objectMapper = mappingJackson2HttpMessageConverter.getObjectMapper();
    }

    public void convertAndSendToUser(String user, String destination, Object payload) throws MessagingException {
        if (payload instanceof String) {
            kafkaTemplate.send(WEBSOCKET_SYNCHRONIZE_TOPIC, "sync-message", new ConvertAndSendToUserDTO(user, destination, (String) payload));
        }
        else {
            try {
                kafkaTemplate.send(WEBSOCKET_SYNCHRONIZE_TOPIC, "sync-message", new ConvertAndSendToUserDTO(user, destination, objectMapper.writeValueAsString(payload)));
            }
            catch (JsonProcessingException e) {
                log.error("Error while sending WebSocket message to Kafka", e);
            }
        }
    }

    public void send(String destination, Message<?> message) throws MessagingException {
        if (message.getPayload() instanceof String) {
            kafkaTemplate.send(WEBSOCKET_SYNCHRONIZE_TOPIC, "sync-message", new SendDestinationDTO(destination, message, (String) message.getPayload()));
        }
        else {
            try {
                kafkaTemplate.send(WEBSOCKET_SYNCHRONIZE_TOPIC, "sync-message",
                        new SendDestinationDTO(destination, message, objectMapper.writeValueAsString(message.getPayload())));
            }
            catch (JsonProcessingException e) {
                log.error("Error while sending WebSocket message to Kafka", e);
            }
        }
    }

    public void convertAndSend(String destination, Object payload) throws MessagingException {
        if (payload instanceof String) {
            kafkaTemplate.send(WEBSOCKET_SYNCHRONIZE_TOPIC, "sync-message", new ConvertAndSendDestinationDTO(destination, (String) payload));
        }
        else {
            try {
                kafkaTemplate.send(WEBSOCKET_SYNCHRONIZE_TOPIC, "sync-message", new ConvertAndSendDestinationDTO(destination, objectMapper.writeValueAsString(payload)));
            }
            catch (JsonProcessingException e) {
                log.error("Error while sending WebSocket message to Kafka", e);
            }
        }
    }

    @KafkaHandler
    public void listen(ConvertAndSendDestinationDTO convertAndSendDestinationDTO) {
        log.debug("Received ConvertAndSendDestinationDTO: " + convertAndSendDestinationDTO);
        websocketTemplate.convertAndSend(convertAndSendDestinationDTO.getDestination(), convertAndSendDestinationDTO.getPayload());
    }

    @KafkaHandler
    public void listen(ConvertAndSendToUserDTO convertAndSendToUserDTO) {
        log.debug("Received ConvertAndSendToUserDTO: " + convertAndSendToUserDTO);
        websocketTemplate.convertAndSendToUser(convertAndSendToUserDTO.getUser(), convertAndSendToUserDTO.getDestination(), convertAndSendToUserDTO.getPayload());
    }

    @KafkaHandler
    public void listen(SendDestinationDTO sendDestinationDTO) {
        log.debug("SendDestinationDTO: " + sendDestinationDTO);
        websocketTemplate.convertAndSend(sendDestinationDTO.getDestination(), sendDestinationDTO.getMessage());
    }

    @KafkaHandler(isDefault = true)
    public void listenDefault(Object object) {
        log.error("Received unexpected message: " + object);
    }
}
