package de.tum.in.www1.artemis.domain.quiz.schedule;

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import de.tum.in.www1.artemis.domain.Result;
import de.tum.in.www1.artemis.domain.User;
import de.tum.in.www1.artemis.domain.enumeration.AssessmentType;
import de.tum.in.www1.artemis.domain.enumeration.InitializationState;
import de.tum.in.www1.artemis.domain.enumeration.SubmissionType;
import de.tum.in.www1.artemis.domain.participation.StudentParticipation;
import de.tum.in.www1.artemis.domain.quiz.QuizExercise;
import de.tum.in.www1.artemis.domain.quiz.QuizSubmission;
import de.tum.in.www1.artemis.repository.QuizSubmissionRepository;
import de.tum.in.www1.artemis.repository.ResultRepository;
import de.tum.in.www1.artemis.repository.StudentParticipationRepository;
import de.tum.in.www1.artemis.service.QuizExerciseService;
import de.tum.in.www1.artemis.service.UserService;
import de.tum.in.www1.artemis.store.KeyValueStoreProxy;
import de.tum.in.www1.artemis.store.KeyValueStoreService;

/**
 * This class is responsible for scheduling a specific quiz.
 * It is present on every server running the quiz
 */
public class QuizExerciseSchedule {

    private static final Logger log = LoggerFactory.getLogger(QuizExerciseSchedule.class);

    private static ThreadPoolTaskScheduler threadPoolTaskScheduler;

    static {
        threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.setThreadNamePrefix("QuizScheduler");
        threadPoolTaskScheduler.setPoolSize(1);
        threadPoolTaskScheduler.initialize();
        // scheduledProcessQuizSubmissions = threadPoolTaskScheduler.scheduleWithFixedDelay(this::processCachedQuizSubmissions, delayInMillis); // TODO Simon Leiß: Implement this
        // part
    }

    private final QuizExercise quizExercise;

    private KeyValueStoreProxy<String, QuizSubmission> submissionKeyValueStore;

    private KeyValueStoreProxy<String, StudentParticipation> participationKeyValueStore;

    private ScheduledFuture quizStartSchedule;

    private QuizExerciseService quizExerciseService;

    private UserService userService;

    private StudentParticipationRepository studentParticipationRepository;

    private ResultRepository resultRepository;

    private QuizSubmissionRepository quizSubmissionRepository;

    public QuizExerciseSchedule(QuizExercise quizExercise, KeyValueStoreService keyValueStoreService, QuizExerciseService quizExerciseService, UserService userService,
            StudentParticipationRepository studentParticipationRepository, ResultRepository resultRepository, QuizSubmissionRepository quizSubmissionRepository) {
        this.quizExercise = quizExercise;
        this.userService = userService;

        submissionKeyValueStore = keyValueStoreService.createKeyValueStore("quiz-" + quizExercise.getId());
        participationKeyValueStore = keyValueStoreService.createKeyValueStore("participation-" + quizExercise.getId());
    }

    /**
     * Save a QuizSubmission in the KV-store
     *
     * @param username the username of whom the submission should be saved
     * @param quizSubmission the quizSubmission which should be saved
     */
    public void updateSubmission(String username, QuizSubmission quizSubmission) {
        if (username != null && quizSubmission != null) {
            submissionKeyValueStore.put(username, quizSubmission);
        }
    }

    /**
     * Save a participation in the KV-store
     *
     * @param participation the participation that should be saved
     */
    public void addParticipation(StudentParticipation participation) {
        if (participation != null) {
            participationKeyValueStore.put(participation.getParticipantIdentifier(), participation);
        }
    }

    /**
     * Get the quiz submission from the KV-store for the given user.
     *
     * @param username the user for whom the submission should be loaded
     * @return the saved submission or a new submission if no saved submission was found
     */
    public QuizSubmission getQuizSubmission(String username) {
        if (username == null) {
            return null;
        }

        QuizSubmission quizSubmission = submissionKeyValueStore.get(username);
        if (quizSubmission != null) {
            return quizSubmission;
        }

        // return an empty quizSubmission if the KV-store contains no entry for the key
        return new QuizSubmission().submittedAnswers(new HashSet<>());
    }

    /**
     * Get the participation for a given username
     *
     * @param username the user for whom the participation should be loaded
     * @return the loaded participation, or null if no participation was found
     */
    public StudentParticipation getParticipation(String username) {
        if (username == null) {
            return null;
        }

        return participationKeyValueStore.get(username);
    }

    private void startQuiz() {
        quizExerciseService.sendQuizExerciseToSubscribedClients(quizExercise);
    }

    public void scheduleQuizStart(final QuizExercise quizExercise) {
        // first remove and cancel old scheduledFuture if it exists
        cancelScheduledQuizStart(quizExercise.getId());

        if (quizExercise.isIsPlannedToStart() && quizExercise.getReleaseDate().isAfter(ZonedDateTime.now())) {
            // schedule sending out filtered quiz over websocket
            this.quizStartSchedule = threadPoolTaskScheduler.schedule(this::startQuiz, Date.from(quizExercise.getReleaseDate().toInstant()));
        }
    }

    private void cancelScheduledQuizStart(Long quizExerciseId) {
        if (quizStartSchedule != null) {
            boolean cancelSuccess = quizStartSchedule.cancel(true);
            log.info("Stop scheduled quiz start for quiz " + quizExerciseId + " was successful: " + cancelSuccess);
        }
    }

    public void clearQuizData(Long quizExerciseId) {
        // TODO: Check if needed
    }

    public void processCachedQuizSubmissions() {
        log.debug("Process cached quiz submissions for quiz " + quizExercise.getId());
        try {
            long start = System.currentTimeMillis();

            // TODO: Check if this makes sense/works
            QuizExercise loadedQuizExercise = quizExerciseService.findOneWithQuestions(quizExercise.getId());
            // check if quiz has been deleted
            if (loadedQuizExercise == null) {
                // TODO: Delete submissions
                // continue
            }

            if (quizExercise.isEnded()) {
                int num = createParticipations();

                if (num > 0) {
                    log.info("Processed {} submissions after {} ms in quiz {}", num, System.currentTimeMillis() - start, quizExercise.getTitle());
                }
            }

        }
        catch (Exception e) {
            log.error("Exception in Quiz Schedule for quiz {}:\n{}", quizExercise.getId(), e.getMessage());

        }
    }

    private int createParticipations() {
        int counter = 0;

        // This quiz has just ended
        for (Map.Entry<String, QuizSubmission> pair : submissionKeyValueStore.getResponsibleKeyValues().entrySet()) {
            String username = pair.getKey();
            QuizSubmission quizSubmission = pair.getValue();
            try {
                quizSubmission.setSubmitted(true);
                quizSubmission.setType(SubmissionType.TIMEOUT);
                quizSubmission.setSubmissionDate(ZonedDateTime.now());

                // Create Participation and Result and save to Database (DB Write)
                // Remove processed Submissions from SubmissionHashMap and write Participations with Result into ParticipationHashMap and Results into ResultHashMap
                createParticipationWithResultAndWriteItInHashMaps(username, quizSubmission);
                counter++;
            }
            catch (Exception e) {
                log.error("Exception in createParticipations() for {} in quiz {}: \n{}", username, quizExercise.getId(), e.getMessage());
            }
        }

        return counter;
    }

    /**
     * create Participation and Result if the submission was submitted or if the quiz has ended and save them to Database (DB Write)
     *
     * @param username       the user, who submitted the quizSubmission
     * @param quizSubmission the quizSubmission, which is used to calculate the Result
     */
    private void createParticipationWithResultAndWriteItInHashMaps(String username, QuizSubmission quizSubmission) {

        if (quizExercise != null && username != null && quizSubmission != null) {

            // create and save new participation
            StudentParticipation participation = new StudentParticipation();
            // TODO: when this is set earlier for the individual quiz start of a student, we don't need to set this here anymore
            participation.setInitializationDate(quizSubmission.getSubmissionDate());
            Optional<User> user = userService.getUserByLogin(username);
            user.ifPresent(participation::setParticipant);
            // add the quizExercise to the participation
            participation.setExercise(quizExercise);

            // create new result
            Result result = new Result().participation(participation).submission(quizSubmission);
            result.setRated(true);
            result.setAssessmentType(AssessmentType.AUTOMATIC);
            result.setCompletionDate(quizSubmission.getSubmissionDate());
            result.setSubmission(quizSubmission);

            // calculate scores and update result and submission accordingly
            quizSubmission.calculateAndUpdateScores(quizExercise);
            result.evaluateSubmission();

            // add result to participation
            participation.addResult(result);

            // add submission to participation
            participation.addSubmissions(quizSubmission);
            participation.setInitializationState(InitializationState.FINISHED);
            participation.setExercise(quizExercise);

            // save participation, result and quizSubmission
            participation = studentParticipationRepository.save(participation);
            quizSubmissionRepository.save(quizSubmission);
            result = resultRepository.save(result);

            // add the participation to the participationHashMap for the send out at the end of the quiz
            addParticipation(participation);
            // add the result of the participation resultHashMap for the statistic-Update
            // addResultForStatisticUpdate(quizExercise.getId(), result); // TODO: Check if this is still needed
        }
    }

}
