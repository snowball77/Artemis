package de.tum.in.www1.artemis.web.websocket.distributed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

import de.tum.in.www1.artemis.web.websocket.distributed.messageTypes.*;

@Service
@Profile("kafka")
@KafkaListener(id = "distributedMessagingTemplate", topics = "websocket-synchronize")
public class DistributedMessagingTemplate implements ArtemisMessagingTemplate {

    private static final Logger log = LoggerFactory.getLogger(DistributedMessagingTemplate.class);

    private static final String WEBSOCKET_SYNCHRONIZE_TOPIC = "websocket-synchronize";

    private SimpMessageSendingOperations websocketTemplate;

    private KafkaTemplate<String, DistributedWebsocketMessage> kafkaTemplate;

    DistributedMessagingTemplate(SimpMessageSendingOperations websocketTemplate, KafkaTemplate<String, DistributedWebsocketMessage> distributedWebsocketMessageKafkaTemplate) {
        this.websocketTemplate = websocketTemplate;
        this.kafkaTemplate = distributedWebsocketMessageKafkaTemplate;
    }

    public void convertAndSendToUser(String user, String destination, Object payload) throws MessagingException {
        kafkaTemplate.send(WEBSOCKET_SYNCHRONIZE_TOPIC, "sync-message", new ConvertAndSendToUserDTO(user, destination, payload));
    }

    public void send(String destination, Message<?> message) throws MessagingException {
        kafkaTemplate.send(WEBSOCKET_SYNCHRONIZE_TOPIC, "sync-message", new SendDestinationDTO(destination, message));
    }

    public void convertAndSend(String destination, Object payload) throws MessagingException {
        kafkaTemplate.send(WEBSOCKET_SYNCHRONIZE_TOPIC, "sync-message", new ConvertAndSendDestinationDTO(destination, payload));
    }

    @KafkaHandler
    public void listen(ConvertAndSendDestinationDTO convertAndSendDestinationDTO) {
        websocketTemplate.convertAndSend(convertAndSendDestinationDTO.getDestination(), convertAndSendDestinationDTO.getPayload());
    }

    @KafkaHandler
    public void listen(ConvertAndSendToUserDTO convertAndSendToUserDTO) {
        websocketTemplate.convertAndSendToUser(convertAndSendToUserDTO.getUser(), convertAndSendToUserDTO.getDestination(), convertAndSendToUserDTO.getPayload());
    }

    @KafkaHandler
    public void listen(SendDestinationDTO sendDestinationDTO) {
        websocketTemplate.send(sendDestinationDTO.getDestination(), sendDestinationDTO.getMessage());
    }

    @KafkaHandler(isDefault = true)
    public void listenDefault(Object object) {
        log.error("Received unexpected message: " + object);
    }
}
