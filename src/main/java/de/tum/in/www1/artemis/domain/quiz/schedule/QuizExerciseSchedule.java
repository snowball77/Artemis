package de.tum.in.www1.artemis.domain.quiz.schedule;

import java.util.HashSet;
import java.util.concurrent.ScheduledFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import de.tum.in.www1.artemis.domain.participation.StudentParticipation;
import de.tum.in.www1.artemis.domain.quiz.QuizExercise;
import de.tum.in.www1.artemis.domain.quiz.QuizSubmission;
import de.tum.in.www1.artemis.kafka.KeyValueStore;

/**
 * This class is responsible for scheduling a specific quiz.
 * It is present on every server running the quiz
 */
public class QuizExerciseSchedule {

    private static final Logger log = LoggerFactory.getLogger(QuizExerciseSchedule.class);

    private QuizExercise quizExercise;

    private KeyValueStore<String, QuizSubmission> submissionKeyValueStore;

    private KeyValueStore<String, StudentParticipation> participationKeyValueStore;

    private ScheduledFuture quizStartSchedule;

    private static ThreadPoolTaskScheduler threadPoolTaskScheduler;

    static {
        threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.setThreadNamePrefix("QuizScheduler");
        threadPoolTaskScheduler.setPoolSize(1);
        threadPoolTaskScheduler.initialize();
        // scheduledProcessQuizSubmissions = threadPoolTaskScheduler.scheduleWithFixedDelay(this::processCachedQuizSubmissions, delayInMillis); // TODO Simon Leiß: Implement this
        // part
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
}
