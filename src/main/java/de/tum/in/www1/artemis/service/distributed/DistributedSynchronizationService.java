package de.tum.in.www1.artemis.service.distributed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import de.tum.in.www1.artemis.domain.quiz.QuizExercise;
import de.tum.in.www1.artemis.service.QuizExerciseService;
import de.tum.in.www1.artemis.service.distributed.messages.SynchronizationMessage;
import de.tum.in.www1.artemis.service.distributed.messages.UpdateQuizMessage;
import de.tum.in.www1.artemis.service.scheduled.QuizScheduleService;

@Service
@Profile("kafka")
@KafkaListener(groupId = "${artemis.kafka.group-id}", topics = "distributed-synchronize", containerFactory = "synchronizationMessageListenerContainerFactory")
public class DistributedSynchronizationService implements SynchronizationService {

    private static final Logger log = LoggerFactory.getLogger(DistributedSynchronizationService.class);

    private static final String DISTRIBUTED_SYNCHRONIZE_TOPIC = "distributed-synchronize";

    @Value("${artemis.kafka.group-id}")
    private String serverId;

    KafkaTemplate<String, SynchronizationMessage> distributedSynchronizationKafkaTemplate;

    private QuizExerciseService quizExerciseService;

    private QuizScheduleService quizScheduleService;

    public DistributedSynchronizationService(KafkaTemplate<String, SynchronizationMessage> distributedSynchronizationKafkaTemplate, QuizExerciseService quizExerciseService,
            QuizScheduleService quizScheduleService) {
        this.distributedSynchronizationKafkaTemplate = distributedSynchronizationKafkaTemplate;
        this.quizExerciseService = quizExerciseService;
        this.quizScheduleService = quizScheduleService;
    }

    @Override
    public void informServers(SynchronizationMessage synchronizationMessage) {
        if (synchronizationMessage.getSendingServer() == null) {
            synchronizationMessage.setSendingServer(serverId);
        }

        distributedSynchronizationKafkaTemplate.send(DISTRIBUTED_SYNCHRONIZE_TOPIC, "sync-server", synchronizationMessage);
    }

    @KafkaHandler
    public void listen(UpdateQuizMessage updateQuizMessage) {
        log.debug("Received UpdateQuizMessage: " + updateQuizMessage);

        if (updateQuizMessage.getSendingServer().equals(serverId)) {
            // Ignore own messages
            return;
        }

        QuizExercise quizExercise = quizExerciseService.findOneWithQuestions(updateQuizMessage.getQuizId());
        quizScheduleService.scheduleQuizStart(quizExercise);
    }

    @KafkaHandler(isDefault = true)
    public void listenDefault(Object object) {
        log.error("Received unexpected message: " + object);
    }
}
