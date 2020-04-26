package de.tum.in.www1.artemis.service.scheduled;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import de.tum.in.www1.artemis.domain.*;
import de.tum.in.www1.artemis.domain.participation.StudentParticipation;
import de.tum.in.www1.artemis.domain.quiz.QuizExercise;
import de.tum.in.www1.artemis.domain.quiz.QuizSubmission;
import de.tum.in.www1.artemis.domain.quiz.schedule.QuizExerciseSchedule;
import de.tum.in.www1.artemis.repository.QuizSubmissionRepository;
import de.tum.in.www1.artemis.repository.ResultRepository;
import de.tum.in.www1.artemis.repository.StudentParticipationRepository;
import de.tum.in.www1.artemis.service.QuizExerciseService;
import de.tum.in.www1.artemis.service.UserService;
import de.tum.in.www1.artemis.store.KeyValueStoreService;

@Service
public class QuizScheduleService {

    private static final Logger log = LoggerFactory.getLogger(QuizScheduleService.class);

    /**
     * quizExerciseId -> ScheduledFuture
     */
    private static Map<Long, QuizExerciseSchedule> quizExerciseSchedules = new ConcurrentHashMap<>();

    private static ThreadPoolTaskScheduler threadPoolTaskScheduler;

    private final SimpMessageSendingOperations messagingTemplate;

    private final StudentParticipationRepository studentParticipationRepository;

    private final ResultRepository resultRepository;

    private final QuizSubmissionRepository quizSubmissionRepository;

    private final UserService userService;

    private final QuizExerciseService quizExerciseService;

    private final KeyValueStoreService keyValueStoreService;

    public QuizScheduleService(SimpMessageSendingOperations messagingTemplate, StudentParticipationRepository studentParticipationRepository, ResultRepository resultRepository,
            QuizSubmissionRepository quizSubmissionRepository, UserService userService, QuizExerciseService quizExerciseService, KeyValueStoreService keyValueStoreService) {
        this.messagingTemplate = messagingTemplate;
        this.studentParticipationRepository = studentParticipationRepository;
        this.resultRepository = resultRepository;
        this.quizSubmissionRepository = quizSubmissionRepository;
        this.userService = userService;
        this.quizExerciseService = quizExerciseService;
        this.keyValueStoreService = keyValueStoreService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void applicationReady() {
        // activate Quiz Schedule Service
        startSchedule(3 * 1000);                          // every 3 seconds
    }

    private QuizExerciseSchedule getOrCreateQuizExerciseSchedule(QuizExercise quizExercise) {
        if (!quizExerciseSchedules.containsKey(quizExercise.getId())) {
            quizExerciseSchedules.put(quizExercise.getId(), new QuizExerciseSchedule(quizExercise, keyValueStoreService, messagingTemplate, quizExerciseService, userService,
                    studentParticipationRepository, resultRepository, quizSubmissionRepository));
        }
        return quizExerciseSchedules.get(quizExercise.getId());
    }

    /**
     * add a quizSubmission to the submissionHashMap
     *
     * @param quizExercise   the quizExercise the submission belongs to
     * @param username       the username of the user, who submitted the submission
     * @param quizSubmission the quizSubmission, which should be added
     */
    public void updateSubmission(QuizExercise quizExercise, String username, QuizSubmission quizSubmission) {
        QuizExerciseSchedule quizExerciseSchedule = getOrCreateQuizExerciseSchedule(quizExercise);
        if (quizExerciseSchedule != null) {
            quizExerciseSchedule.updateSubmission(username, quizSubmission);
        }
    }

    /**
     * add a result to resultHashMap for a statistic-update
     * this should only be invoked once, when the quiz was submitted
     *
     * @param quizExerciseId the quizExerciseId of the quiz the result belongs to (first Key)
     * @param result the result, which should be added
     */
    public static void addResultForStatisticUpdate(Long quizExerciseId, Result result) {
        log.debug("add result for statistic update for quiz " + quizExerciseId + ": " + result);
        QuizExerciseSchedule quizExerciseSchedule = quizExerciseSchedules.get(quizExerciseId);
        if (quizExerciseSchedule != null) {
            // quizExerciseSchedule.add;
        }
        // TODO: Simon Leiß: Check if this is still needed
        /*
         * if (quizExerciseId != null && result != null) { // check if there is already a result with the same quiz if (!resultHashMap.containsKey(quizExerciseId)) {
         * resultHashMap.put(quizExerciseId, new HashSet<>()); } resultHashMap.get(quizExerciseId).add(result); }
         */
    }

    /**
     * add a participation to participationHashMap to send them back to the user when the quiz ends
     *
     * @param quizExerciseId        the quizExerciseId of the quiz the result belongs to (first Key)
     * @param participation the result, which should be added
     */
    private static void addParticipation(Long quizExerciseId, StudentParticipation participation) {
        QuizExerciseSchedule quizExerciseSchedule = quizExerciseSchedules.get(quizExerciseId);
        if (quizExerciseSchedule != null) {
            quizExerciseSchedule.addParticipation(participation);
        }
    }

    /**
     * get a quizSubmission from the submissionHashMap by quizExerciseId and username
     *
     * @param quizExerciseId   the quizExerciseId of the quiz the submission belongs to (first Key)
     * @param username the username of the user, who submitted the submission (second Key)
     * @return the quizSubmission, with the given quizExerciseId and username -> return an empty QuizSubmission if there is no quizSubmission -> return null if the quizExerciseId or if the
     *         username is null
     */
    public static QuizSubmission getQuizSubmission(Long quizExerciseId, String username) {
        QuizExerciseSchedule quizExerciseSchedule = quizExerciseSchedules.get(quizExerciseId);
        if (quizExerciseSchedule != null) {
            return quizExerciseSchedule.getQuizSubmission(username);
        }
        return new QuizSubmission().submittedAnswers(new HashSet<>());
    }

    /**
     * get a participation from the participationHashMap by quizExerciseId and username
     *
     * @param quizExerciseId   the quizExerciseId of the quiz, the participation belongs to (first Key)
     * @param username the username of the user, the participation belongs to (second Key)
     * @return the participation with the given quizExerciseId and username -> return null if there is no participation -> return null if the quizExerciseId or if the username is null
     */
    public static StudentParticipation getParticipation(Long quizExerciseId, String username) {
        QuizExerciseSchedule quizExerciseSchedule = quizExerciseSchedules.get(quizExerciseId);
        if (quizExerciseSchedule != null) {
            return quizExerciseSchedule.getParticipation(username);
        }
        return null;
    }

    /**
     * Start scheduler of quiz schedule service
     *
     * @param delayInMillis gap for which the QuizScheduleService should run repeatly
     */
    public void startSchedule(long delayInMillis) {
        if (threadPoolTaskScheduler == null) {
            threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
            threadPoolTaskScheduler.setThreadNamePrefix("QuizScheduler");
            threadPoolTaskScheduler.setPoolSize(1);
            threadPoolTaskScheduler.initialize();

            // schedule quiz start for all existing quizzes that are planned to start in the future
            List<QuizExercise> quizExercises = quizExerciseService.findAllPlannedToStartInTheFutureWithQuestions();
            for (QuizExercise quizExercise : quizExercises) {

                QuizExerciseSchedule quizExerciseSchedule = getOrCreateQuizExerciseSchedule(quizExercise);
                quizExerciseSchedule.scheduleQuizStart();
            }
        }
        else {
            log.debug("Cannot start quiz exercise schedule service, it is already RUNNING");
        }
    }

    /**
     * stop scheduler (interrupts if running)
     */
    public void stopSchedule() {
        if (threadPoolTaskScheduler != null) {
            log.info("Try to stop quiz schedule service");
            for (QuizExerciseSchedule quizExerciseSchedule : quizExerciseSchedules.values()) {
                quizExerciseSchedule.cancelScheduledQuizStart();
            }
            threadPoolTaskScheduler.shutdown();
            threadPoolTaskScheduler = null;
        }
        else {
            log.debug("Cannot stop quiz exercise schedule service, it was already STOPPED");
        }
    }

    /**
     * Start scheduler of quiz
     *
     * @param quizExercise that should be scheduled
     */
    public void scheduleQuizStart(final QuizExercise quizExercise) {
        QuizExerciseSchedule quizExerciseSchedule = getOrCreateQuizExerciseSchedule(quizExercise);
        quizExerciseSchedule.scheduleQuizStart();
        quizExerciseSchedule.scheduleQuizEnd();
    }

    public void processCachedQuizSubmissions(long quizExerciseId) {
        QuizExerciseSchedule quizExerciseSchedule = quizExerciseSchedules.get(quizExerciseId);
        if (quizExerciseSchedule != null) {
            quizExerciseSchedule.processCachedQuizSubmissions();
        }
    }

    /**
     * cancels the quiz start for the given exercise id, e.g. because the quiz was deleted or the quiz start date was changed
     *
     * @param quizExerciseId the quiz exercise for which the quiz start should be canceled
     */
    public void cancelScheduledQuizStart(Long quizExerciseId) {
        QuizExerciseSchedule quizExerciseSchedule = quizExerciseSchedules.get(quizExerciseId);
        if (quizExerciseSchedule != null) {
            quizExerciseSchedule.cancelScheduledQuizStart();
        }
    }

    public void clearAllQuizData() {
        for (QuizExerciseSchedule quizExerciseSchedule : quizExerciseSchedules.values()) {
            quizExerciseSchedule.clearQuizData();
        }
    }

    public void clearQuizData(Long quizExerciseId) {
        QuizExerciseSchedule quizExerciseSchedule = quizExerciseSchedules.get(quizExerciseId);
        if (quizExerciseSchedule != null) {
            quizExerciseSchedule.clearQuizData();
        }
    }
}
