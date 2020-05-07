package de.tum.in.www1.artemis.service.distributed;

import org.hibernate.SessionFactory;
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
import de.tum.in.www1.artemis.service.SessionFactoryService;
import de.tum.in.www1.artemis.service.distributed.messages.QuizResetMessage;
import de.tum.in.www1.artemis.service.distributed.messages.QuizUpdateMessage;
import de.tum.in.www1.artemis.service.distributed.messages.SynchronizationMessage;
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

    private SessionFactory sessionFactory;

    private QuizExerciseService quizExerciseService;

    private QuizScheduleService quizScheduleService;

    public DistributedSynchronizationService(KafkaTemplate<String, SynchronizationMessage> distributedSynchronizationKafkaTemplate, QuizExerciseService quizExerciseService,
            QuizScheduleService quizScheduleService, SessionFactoryService sessionFactoryService) {
        this.distributedSynchronizationKafkaTemplate = distributedSynchronizationKafkaTemplate;
        this.quizExerciseService = quizExerciseService;
        this.quizScheduleService = quizScheduleService;
        this.sessionFactory = sessionFactoryService.getSessionFactory();
    }

    @Override
    public void informServers(SynchronizationMessage synchronizationMessage) {
        if (synchronizationMessage.getSendingServer() == null) {
            synchronizationMessage.setSendingServer(serverId);
        }

        distributedSynchronizationKafkaTemplate.send(DISTRIBUTED_SYNCHRONIZE_TOPIC, "sync-server", synchronizationMessage);
    }

    @KafkaHandler
    public void listen(QuizUpdateMessage quizUpdateMessage) {
        log.debug("Received QuizUpdateMessage: " + quizUpdateMessage);

        if (quizUpdateMessage.getSendingServer().equals(serverId)) {
            // Ignore own messages
            return;
        }

        // Clear saved QuizExercises so changes in the database are fetched correctly
        sessionFactory.getCache().evict(QuizExercise.class);

        QuizExercise quizExercise = quizExerciseService.findOneWithQuestions(quizUpdateMessage.getQuizId());
        quizScheduleService.scheduleQuizStart(quizExercise);
    }

    @KafkaHandler
    public void listen(QuizResetMessage quizResetMessage) {
        log.debug("Received QuizResetMessage: " + quizResetMessage);

        if (quizResetMessage.getSendingServer().equals(serverId)) {
            // Ignore own messages
            return;
        }

        quizScheduleService.clearQuizData(quizResetMessage.getQuizId());
    }

    @KafkaHandler(isDefault = true)
    public void listenDefault(Object object) {
        log.error("Received unexpected message: " + object);
    }
}
