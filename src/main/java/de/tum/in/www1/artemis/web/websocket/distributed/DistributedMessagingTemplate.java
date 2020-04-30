package de.tum.in.www1.artemis.web.websocket.distributed;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

import de.tum.in.www1.artemis.web.websocket.distributed.messageTypes.ConvertAndSendDestinationDTO;
import de.tum.in.www1.artemis.web.websocket.distributed.messageTypes.ConvertAndSendToUserDTO;
import de.tum.in.www1.artemis.web.websocket.distributed.messageTypes.DistributedWebsocketMessage;
import de.tum.in.www1.artemis.web.websocket.distributed.messageTypes.SendDestinationDTO;

@Service
@Profile("kafka")
public class DistributedMessagingTemplate implements ArtemisMessagingTemplate {

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

    @KafkaListener(topics = WEBSOCKET_SYNCHRONIZE_TOPIC)
    public void listenConvertAndSendToUser(ConsumerRecord<String, DistributedWebsocketMessage> consumerRecord) {
        DistributedWebsocketMessage distributedWebsocketMessage = consumerRecord.value();
        if (distributedWebsocketMessage instanceof ConvertAndSendDestinationDTO) {
            ConvertAndSendDestinationDTO convertAndSendDestinationDTO = (ConvertAndSendDestinationDTO) distributedWebsocketMessage;
            websocketTemplate.convertAndSend(convertAndSendDestinationDTO.getDestination(), convertAndSendDestinationDTO.getPayload());

        }
        else if (distributedWebsocketMessage instanceof ConvertAndSendToUserDTO) {
            ConvertAndSendToUserDTO convertAndSendToUserDTO = (ConvertAndSendToUserDTO) distributedWebsocketMessage;
            websocketTemplate.convertAndSendToUser(convertAndSendToUserDTO.getUser(), convertAndSendToUserDTO.getDestination(), convertAndSendToUserDTO.getPayload());

        }
        else if (distributedWebsocketMessage instanceof SendDestinationDTO) {
            SendDestinationDTO sendDestinationDTO = (SendDestinationDTO) distributedWebsocketMessage;
            websocketTemplate.send(sendDestinationDTO.getDestination(), sendDestinationDTO.getMessage());
        }
    }
}
