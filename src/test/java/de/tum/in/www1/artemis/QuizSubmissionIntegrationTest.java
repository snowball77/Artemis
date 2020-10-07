package de.tum.in.www1.artemis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.security.Principal;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;

import de.tum.in.www1.artemis.config.Constants;
import de.tum.in.www1.artemis.domain.Course;
import de.tum.in.www1.artemis.domain.Result;
import de.tum.in.www1.artemis.domain.quiz.*;
import de.tum.in.www1.artemis.repository.*;
import de.tum.in.www1.artemis.service.QuizExerciseService;
import de.tum.in.www1.artemis.service.scheduled.quiz.QuizScheduleService;
import de.tum.in.www1.artemis.util.DatabaseUtilService;
import de.tum.in.www1.artemis.util.RequestUtilService;
import de.tum.in.www1.artemis.web.websocket.QuizSubmissionWebsocketService;

public class QuizSubmissionIntegrationTest extends AbstractSpringIntegrationBambooBitbucketJiraTest {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    DatabaseUtilService database;

    @Autowired
    RequestUtilService request;

    @Autowired
    CourseRepository courseRepository;

    @Autowired
    ExerciseRepository exerciseRepository;

    @Autowired
    QuizExerciseService quizExerciseService;

    @Autowired
    QuizScheduleService quizScheduleService;

    @Autowired
    QuizSubmissionWebsocketService quizSubmissionWebsocketService;

    @Autowired
    QuizSubmissionRepository quizSubmissionRepository;

    @Autowired
    ParticipationRepository participationRepository;

    @Autowired
    SubmissionRepository submissionRepository;

    @Autowired
    ResultRepository resultRepository;

    int multiplier = 100;

    @BeforeEach
    public void init() {
        quizScheduleService.stopSchedule();
        database.addUsers(10 * multiplier, 5, 1);
        // do not use the schedule service based on a time interval in the tests, because this would result in flaky tests that run much slower
    }

    @AfterEach
    public void tearDown() {
        quizScheduleService.clearAllQuizData();
        database.resetDatabase();
    }

    @Test
    @WithMockUser(value = "student1", roles = "USER")
    public void testQuizSubmit() throws Exception {
        // change config to make test faster
        List<Course> courses = database.createCoursesWithExercisesAndLectures(true);
        Course course = courses.get(0);
        QuizExercise quizExercise = database.createQuiz(course, ZonedDateTime.now(), null);
        quizExercise.duration(60);
        quizExercise.setIsPlannedToStart(true);
        quizExercise.setIsVisibleBeforeStart(true);
        quizExercise = quizExerciseService.save(quizExercise);

        int numberOfParticipants = 10 * multiplier;
        QuizSubmission quizSubmission;

        for (int i = 1; i <= numberOfParticipants; i++) {
            quizSubmission = database.generateSubmissionForThreeQuestions(quizExercise, i, false, null);
            final var username = "student" + i;
            final Principal principal = () -> username;
            // save
            quizSubmissionWebsocketService.saveSubmission(quizExercise.getId(), quizSubmission, principal);
            // NOTE: the communication back to the client is currently deactivated
            // verify(messagingTemplate, times(1)).convertAndSendToUser(username, "/topic/quizExercise/" + quizExercise.getId() + "/submission", quizSubmission);
        }

        // only half of the students submit manually
        for (int i = 1; i <= numberOfParticipants / 2; i++) {
            quizSubmission = database.generateSubmissionForThreeQuestions(quizExercise, i, true, null);
            final var username = "student" + i;
            final Principal principal = () -> username;
            // submit
            quizSubmissionWebsocketService.saveSubmission(quizExercise.getId(), quizSubmission, principal);
            // NOTE: the communication back to the client is currently deactivated
            // verify(messagingTemplate, times(1)).convertAndSendToUser(username, "/topic/quizExercise/" + quizExercise.getId() + "/submission", quizSubmission);
        }

        // before the quiz submissions are processed, none of them ends up in the database
        assertThat(quizSubmissionRepository.count()).isEqualTo(0);

        // process first half of the submissions
        quizScheduleService.processCachedQuizSubmissions();
        assertThat(quizSubmissionRepository.count()).isEqualTo(numberOfParticipants / 2);

        // End the quiz right now so that results can be processed
        quizExercise = quizExerciseService.findOneWithQuestionsAndStatistics(quizExercise.getId());
        quizExercise.setDuration((int) Duration.between(quizExercise.getReleaseDate(), ZonedDateTime.now()).getSeconds() - Constants.QUIZ_GRACE_PERIOD_IN_SECONDS);
        exerciseRepository.saveAndFlush(quizExercise);

        quizScheduleService.processCachedQuizSubmissions();

        // after the quiz submissions have been processed, all submission are saved to the database
        assertThat(quizSubmissionRepository.count()).isEqualTo(numberOfParticipants);

        // Test the statistics directly from the database
        QuizExercise quizExerciseWithStatistic = quizExerciseService.findOneWithQuestionsAndStatistics(quizExercise.getId());
        assertThat(quizExerciseWithStatistic.getQuizPointStatistic().getParticipantsUnrated()).isEqualTo(0);
        assertThat(quizExerciseWithStatistic.getQuizPointStatistic().getParticipantsRated()).isEqualTo(numberOfParticipants);
        int questionScore = quizExerciseWithStatistic.getQuizQuestions().stream().map(QuizQuestion::getScore).reduce(0, Integer::sum);
        assertThat(quizExerciseWithStatistic.getMaxScore()).isEqualTo(questionScore);
        assertThat(quizExerciseWithStatistic.getQuizPointStatistic().getPointCounters().size()).isEqualTo(questionScore + 1);
        // check general statistics
        for (var pointCounter : quizExerciseWithStatistic.getQuizPointStatistic().getPointCounters()) {
            if (pointCounter.getPoints() == 0.0) {
                assertThat(pointCounter.getRatedCounter()).isEqualTo(Math.round(numberOfParticipants / 3.0));
                assertThat(pointCounter.getUnRatedCounter()).isEqualTo(0);
            }
            else if (pointCounter.getPoints() == 3.0 || pointCounter.getPoints() == 4.0 || pointCounter.getPoints() == 6.0) {
                assertThat(pointCounter.getRatedCounter()).isEqualTo(Math.round(numberOfParticipants / 6.0));
                assertThat(pointCounter.getUnRatedCounter()).isEqualTo(0);
            }
            else if (pointCounter.getPoints() == 7.0 || pointCounter.getPoints() == 9.0) {
                assertThat(pointCounter.getRatedCounter()).isEqualTo(Math.round(numberOfParticipants / 12.0));
                assertThat(pointCounter.getUnRatedCounter()).isEqualTo(0);
            }
            else {
                assertThat(pointCounter.getRatedCounter()).isEqualTo(0);
                assertThat(pointCounter.getUnRatedCounter()).isEqualTo(0);
            }
        }
        // check statistic for each question
        for (var question : quizExerciseWithStatistic.getQuizQuestions()) {
            if (question instanceof MultipleChoiceQuestion) {
                assertThat(question.getQuizQuestionStatistic().getRatedCorrectCounter()).isEqualTo(Math.round(numberOfParticipants / 2.0));
            }
            else if (question instanceof DragAndDropQuestion) {
                assertThat(question.getQuizQuestionStatistic().getRatedCorrectCounter()).isEqualTo(Math.round(numberOfParticipants / 3.0));
            }
            else {
                assertThat(question.getQuizQuestionStatistic().getRatedCorrectCounter()).isEqualTo(Math.round(numberOfParticipants / 4.0));
            }
            assertThat(question.getQuizQuestionStatistic().getUnRatedCorrectCounter()).isEqualTo(0);
            assertThat(question.getQuizQuestionStatistic().getParticipantsRated()).isEqualTo(numberOfParticipants);
            assertThat(question.getQuizQuestionStatistic().getParticipantsUnrated()).isEqualTo(0);
        }

        // execute the scheduler again, this should remove the quiz exercise from the cache
        quizScheduleService.processCachedQuizSubmissions();
        // but of course keep all submissions
        assertThat(quizSubmissionRepository.count()).isEqualTo(numberOfParticipants);
    }

    @Test
    @WithMockUser(value = "student1", roles = "USER")
    public void testQuizSubmitPractice() throws Exception {
        List<Course> courses = database.createCoursesWithExercisesAndLectures(false);
        Course course = courses.get(0);
        QuizExercise quizExercise = database.createQuiz(course, ZonedDateTime.now().minusSeconds(10), null);
        quizExercise.setDueDate(ZonedDateTime.now().minusSeconds(8));
        quizExercise.setDuration(2);
        quizExercise.setIsPlannedToStart(true);
        quizExercise.setIsVisibleBeforeStart(true);
        quizExercise.setIsOpenForPractice(true);
        quizExerciseService.save(quizExercise);

        assertThat(quizSubmissionRepository.findAll().size()).isEqualTo(0);
        assertThat(participationRepository.findAll().size()).isEqualTo(0);

        var numberOfParticipants = 10;

        // submit 10 times for 10 different students
        for (int i = 1; i <= numberOfParticipants; i++) {
            QuizSubmission quizSubmission = database.generateSubmissionForThreeQuestions(quizExercise, i, true, null);
            database.changeUser("student" + i);
            Result receivedResult = request.postWithResponseBody("/api/exercises/" + quizExercise.getId() + "/submissions/practice", quizSubmission, Result.class, HttpStatus.OK);
            assertThat(((QuizSubmission) receivedResult.getSubmission()).getSubmittedAnswers().size()).isEqualTo(quizSubmission.getSubmittedAnswers().size());
        }

        // after the quiz has ended, all submission are saved to the database
        assertThat(quizSubmissionRepository.findAll().size()).isEqualTo(numberOfParticipants);
        assertThat(participationRepository.findAll().size()).isEqualTo(numberOfParticipants);

        // processing the quiz submissions will update the statistics
        quizScheduleService.processCachedQuizSubmissions();

        // Test the statistics directly from the database
        QuizExercise quizExerciseWithStatistic = quizExerciseService.findOneWithQuestionsAndStatistics(quizExercise.getId());
        assertThat(quizExerciseWithStatistic.getQuizPointStatistic().getParticipantsRated()).isEqualTo(0);
        assertThat(quizExerciseWithStatistic.getQuizPointStatistic().getParticipantsUnrated()).isEqualTo(numberOfParticipants);
        int questionScore = quizExerciseWithStatistic.getQuizQuestions().stream().map(QuizQuestion::getScore).reduce(0, Integer::sum);
        assertThat(quizExerciseWithStatistic.getMaxScore()).isEqualTo(questionScore);
        assertThat(quizExerciseWithStatistic.getQuizPointStatistic().getPointCounters().size()).isEqualTo(questionScore + 1);
        // check general statistics
        for (var pointCounter : quizExerciseWithStatistic.getQuizPointStatistic().getPointCounters()) {
            if (pointCounter.getPoints() == 0.0) {
                assertThat(pointCounter.getRatedCounter()).isEqualTo(0);
                assertThat(pointCounter.getUnRatedCounter()).isEqualTo(3);
            }
            else if (pointCounter.getPoints() == 3.0 || pointCounter.getPoints() == 4.0 || pointCounter.getPoints() == 6.0) {
                assertThat(pointCounter.getRatedCounter()).isEqualTo(0);
                assertThat(pointCounter.getUnRatedCounter()).isEqualTo(2);
            }
            else if (pointCounter.getPoints() == 7.0) {
                assertThat(pointCounter.getRatedCounter()).isEqualTo(0);
                assertThat(pointCounter.getUnRatedCounter()).isEqualTo(1);
            }
            else {
                assertThat(pointCounter.getRatedCounter()).isEqualTo(0);
                assertThat(pointCounter.getUnRatedCounter()).isEqualTo(0);
            }
        }
        // check statistic for each question
        for (var question : quizExerciseWithStatistic.getQuizQuestions()) {
            if (question instanceof MultipleChoiceQuestion) {
                assertThat(question.getQuizQuestionStatistic().getUnRatedCorrectCounter()).isEqualTo(5);
            }
            else if (question instanceof DragAndDropQuestion) {
                assertThat(question.getQuizQuestionStatistic().getUnRatedCorrectCounter()).isEqualTo(3);
            }
            else {
                assertThat(question.getQuizQuestionStatistic().getUnRatedCorrectCounter()).isEqualTo(2);
            }
            assertThat(question.getQuizQuestionStatistic().getRatedCorrectCounter()).isEqualTo(0);
            assertThat(question.getQuizQuestionStatistic().getParticipantsUnrated()).isEqualTo(numberOfParticipants);
            assertThat(question.getQuizQuestionStatistic().getParticipantsRated()).isEqualTo(0);
        }
    }

    @Test
    @WithMockUser(value = "student1", roles = "USER")
    public void testQuizSubmitPractice_badRequest() throws Exception {
        List<Course> courses = database.createCoursesWithExercisesAndLectures(true);
        Course course = courses.get(0);
        QuizExercise quizExerciseServer = database.createQuiz(course, ZonedDateTime.now().minusSeconds(4), null);
        quizExerciseServer.setDueDate(ZonedDateTime.now().minusSeconds(2));
        quizExerciseServer.setDuration(2);
        quizExerciseServer.setIsPlannedToStart(true);
        quizExerciseServer.setIsVisibleBeforeStart(true);
        quizExerciseServer.setIsOpenForPractice(false);
        quizExerciseService.save(quizExerciseServer);

        assertThat(quizSubmissionRepository.findAll().size()).isEqualTo(0);

        QuizSubmission quizSubmission = new QuizSubmission();
        for (var question : quizExerciseServer.getQuizQuestions()) {
            for (int i = 1; i <= 10; i++) {
                var answer = database.generateSubmittedAnswerFor(question, i % 2 == 0);
                quizSubmission.addSubmittedAnswers(answer);
                // also remove once
                quizSubmission.removeSubmittedAnswers(answer);
                quizSubmission.addSubmittedAnswers(answer);
            }
        }
        quizSubmission.setSubmitted(true);
        // quiz not open for practice --> bad request expected
        Result result = request.postWithResponseBody("/api/exercises/" + quizExerciseServer.getId() + "/submissions/practice", quizSubmission, Result.class,
                HttpStatus.BAD_REQUEST);
        assertThat(result).isNull();
    }

    @Test
    @WithMockUser(value = "student1", roles = "USER")
    public void testQuizSubmitPreview_forbidden() throws Exception {
        List<Course> courses = database.createCoursesWithExercisesAndLectures(true);
        Course course = courses.get(0);
        QuizExercise quizExercise = database.createQuiz(course, ZonedDateTime.now().minusSeconds(4), null);
        quizExerciseService.save(quizExercise);
        request.postWithResponseBody("/api/exercises/" + quizExercise.getId() + "/submissions/preview", new QuizSubmission(), Result.class, HttpStatus.FORBIDDEN);
    }

    @Test
    @WithMockUser(value = "student1", roles = "USER")
    public void testQuizSubmitPractice_forbidden() throws Exception {
        List<Course> courses = database.createCoursesWithExercisesAndLectures(true);
        Course course = courses.get(0);
        course.setStudentGroupName("abc");
        courseRepository.save(course);
        QuizExercise quizExercise = database.createQuiz(course, ZonedDateTime.now().minusSeconds(4), null);
        quizExerciseService.save(quizExercise);
        request.postWithResponseBody("/api/exercises/" + quizExercise.getId() + "/submissions/practice", new QuizSubmission(), Result.class, HttpStatus.FORBIDDEN);
    }

    @Test
    @WithMockUser(value = "tutor1", roles = "TA")
    public void testQuizSubmitPreview_forbidden_otherTa() throws Exception {
        List<Course> courses = database.createCoursesWithExercisesAndLectures(true);
        Course course = courses.get(0);
        course.setTeachingAssistantGroupName("tutor2");
        courseRepository.save(course);
        QuizExercise quizExercise = database.createQuiz(course, ZonedDateTime.now().minusSeconds(4), null);
        quizExerciseService.save(quizExercise);
        request.postWithResponseBody("/api/exercises/" + quizExercise.getId() + "/submissions/preview", new QuizSubmission(), Result.class, HttpStatus.FORBIDDEN);
    }

    @Test
    @WithMockUser(value = "instructor1", roles = "INSTRUCTOR")
    public void testQuizSubmitPreview_badRequest_noQuiz() throws Exception {
        request.postWithResponseBody("/api/exercises/" + 11 + "/submissions/preview", new QuizSubmission(), Result.class, HttpStatus.BAD_REQUEST);
    }

    @Test
    @WithMockUser(value = "instructor1", roles = "INSTRUCTOR")
    public void testQuizSubmitPractice_badRequest_noQuiz() throws Exception {
        request.postWithResponseBody("/api/exercises/" + 11 + "/submissions/practice", new QuizSubmission(), Result.class, HttpStatus.BAD_REQUEST);
    }

    @Test
    @WithMockUser(value = "instructor1", roles = "INSTRUCTOR")
    public void testQuizSubmitPreview_badRequest_submissionId() throws Exception {
        List<Course> courses = database.createCoursesWithExercisesAndLectures(true);
        Course course = courses.get(0);
        QuizExercise quizExercise = database.createQuiz(course, ZonedDateTime.now().minusSeconds(4), null);
        quizExerciseService.save(quizExercise);
        var quizSubmission = new QuizSubmission();
        quizSubmission.setId(1L);
        request.postWithResponseBody("/api/exercises/" + quizExercise.getId() + "/submissions/preview", quizSubmission, Result.class, HttpStatus.BAD_REQUEST);
    }

    @Test
    @WithMockUser(value = "instructor1", roles = "INSTRUCTOR")
    public void testQuizSubmitPractice_badRequest_submissionId() throws Exception {
        List<Course> courses = database.createCoursesWithExercisesAndLectures(true);
        Course course = courses.get(0);
        QuizExercise quizExercise = database.createQuiz(course, ZonedDateTime.now().minusSeconds(4), null);
        quizExerciseService.save(quizExercise);
        var quizSubmission = new QuizSubmission();
        quizSubmission.setId(1L);
        request.postWithResponseBody("/api/exercises/" + quizExercise.getId() + "/submissions/practice", quizSubmission, Result.class, HttpStatus.BAD_REQUEST);
    }

    @Test
    @WithMockUser(value = "instructor1", roles = "INSTRUCTOR")
    public void testQuizSubmitPreview() throws Exception {
        List<Course> courses = database.createCoursesWithExercisesAndLectures(true);
        Course course = courses.get(0);
        QuizExercise quizExercise = database.createQuiz(course, ZonedDateTime.now().minusSeconds(4), null);
        quizExerciseService.save(quizExercise);

        int numberOfParticipants = 10;
        QuizSubmission quizSubmission = new QuizSubmission();
        for (var question : quizExercise.getQuizQuestions()) {
            for (int i = 1; i <= numberOfParticipants; i++) {
                quizSubmission.addSubmittedAnswers(database.generateSubmittedAnswerFor(question, i % 2 == 0));
            }
        }

        Result receivedResult = request.postWithResponseBody("/api/exercises/" + quizExercise.getId() + "/submissions/preview", quizSubmission, Result.class, HttpStatus.OK);
        assertThat(((QuizSubmission) receivedResult.getSubmission()).getSubmittedAnswers().size()).isEqualTo(quizSubmission.getSubmittedAnswers().size());

        // in the preview the submission will not be saved to the database
        assertThat(quizSubmissionRepository.findAll().size()).isEqualTo(0);

        quizScheduleService.processCachedQuizSubmissions();

        // all stats must be 0 because we have a preview here
        // Test the statistics directly from the database
        QuizExercise quizExerciseWithStatistic = quizExerciseService.findOneWithQuestionsAndStatistics(quizExercise.getId());
        assertThat(quizExerciseWithStatistic.getQuizPointStatistic().getParticipantsRated()).isEqualTo(0);
        assertThat(quizExerciseWithStatistic.getQuizPointStatistic().getParticipantsUnrated()).isEqualTo(0);
        int questionScore = quizExerciseWithStatistic.getQuizQuestions().stream().map(QuizQuestion::getScore).reduce(0, Integer::sum);
        assertThat(quizExerciseWithStatistic.getMaxScore()).isEqualTo(questionScore);
        assertThat(quizExerciseWithStatistic.getQuizPointStatistic().getPointCounters().size()).isEqualTo(questionScore + 1);
        for (var pointCounter : quizExerciseWithStatistic.getQuizPointStatistic().getPointCounters()) {
            if (pointCounter.getPoints() == 0.0f) {
                // all participants have 0 points (and are unrated)
                assertThat(pointCounter.getRatedCounter()).isEqualTo(0);
                assertThat(pointCounter.getUnRatedCounter()).isEqualTo(0);
            }
            else {
                assertThat(pointCounter.getRatedCounter()).isEqualTo(0);
                assertThat(pointCounter.getUnRatedCounter()).isEqualTo(0);
            }
        }
        // check statistic for each question
        for (var question : quizExerciseWithStatistic.getQuizQuestions()) {
            assertThat(question.getQuizQuestionStatistic().getUnRatedCorrectCounter()).isEqualTo(0);
            assertThat(question.getQuizQuestionStatistic().getUnRatedCorrectCounter()).isEqualTo(0);
            assertThat(question.getQuizQuestionStatistic().getRatedCorrectCounter()).isEqualTo(0);
            assertThat(question.getQuizQuestionStatistic().getParticipantsUnrated()).isEqualTo(0);
            assertThat(question.getQuizQuestionStatistic().getParticipantsRated()).isEqualTo(0);
        }
    }

    @Test
    @WithMockUser(value = "student1", roles = "USER")
    public void testQuizSubmitScheduledAndDeleted() throws Exception {
        /*
         * The time we wait in between needs to be relatively high to make sure the concurrent tasks are finished in time, especially sending out the exercise can easily take up to
         * 100 ms, so we should leave about 200 ms for that, similar for the completion of all saving/updating/scheduling operations.
         */
        List<Course> courses = database.createCoursesWithExercisesAndLectures(true);
        Course course = courses.get(0);
        String publishQuizPath = "/topic/courses/" + course.getId() + "/quizExercises";
        long time = System.currentTimeMillis();
        log.debug("// Creating the quiz exercise");
        QuizExercise quizExercise = database.createQuiz(course, ZonedDateTime.now().plus(600, ChronoUnit.MILLIS), null);
        quizExercise.duration(60);
        quizExercise.setIsPlannedToStart(true);
        quizExercise.setIsVisibleBeforeStart(true);

        // also schedules the quiz
        log.debug("// Saving the quiz initially");
        quizExercise = quizExerciseService.save(quizExercise);

        // check that quiz has not started now
        verify(messagingTemplate, never()).send(eq(publishQuizPath), any());

        // wait a bit
        TimeUnit.MILLISECONDS.sleep(400 - (System.currentTimeMillis() - time));
        time = System.currentTimeMillis();

        // reschedule
        log.debug("// Rescheduling the quiz");
        quizExercise.releaseDate(ZonedDateTime.now().plus(700, ChronoUnit.MILLIS));
        quizExercise = quizExerciseService.save(quizExercise);

        // wait for the old release date to pass
        TimeUnit.MILLISECONDS.sleep(400 - (System.currentTimeMillis() - time));
        time = System.currentTimeMillis();

        // check that quiz has still not started now
        log.debug("// Check that the quiz has not started and submissions are not allowed");
        verify(messagingTemplate, never()).send(eq(publishQuizPath), any());

        // check that submission fails
        QuizSubmission quizSubmission = database.generateSubmissionForThreeQuestions(quizExercise, 1, true, null);
        quizSubmissionWebsocketService.saveSubmission(quizExercise.getId(), quizSubmission, () -> "student1");

        quizScheduleService.processCachedQuizSubmissions();
        assertThat(quizSubmissionRepository.count()).isZero();

        // wait for the new release date to pass
        TimeUnit.MILLISECONDS.sleep(600 - (System.currentTimeMillis() - time));

        // check that quiz has started
        log.debug("// Check that the quiz has started");
        verify(messagingTemplate, times(1)).send(eq(publishQuizPath), any());

        // process cached submissions
        quizScheduleService.processCachedQuizSubmissions();

        // save submissions
        int numberOfParticipants = 10;
        for (int i = 1; i <= numberOfParticipants; i++) {
            quizSubmission = database.generateSubmissionForThreeQuestions(quizExercise, i, false, null);
            final var username = "student" + i;
            final Principal principal = () -> username;
            // save
            quizSubmissionWebsocketService.saveSubmission(quizExercise.getId(), quizSubmission, principal);
        }

        // process the saved but not submitted quiz submissions
        quizScheduleService.processCachedQuizSubmissions();

        // before the quiz submissions are submitted, none of them ends up in the database
        assertThat(quizSubmissionRepository.count()).isZero();

        // set the quiz end to now and ...
        log.debug("// End the quiz and delete it");
        quizExercise = quizExerciseService.findOneWithQuestionsAndStatistics(quizExercise.getId());
        quizExercise.setDuration((int) Duration.between(quizExercise.getReleaseDate(), ZonedDateTime.now()).getSeconds() - Constants.QUIZ_GRACE_PERIOD_IN_SECONDS);
        quizExercise = exerciseRepository.saveAndFlush(quizExercise);
        quizScheduleService.updateQuizExercise(quizExercise);
        // ... directly delete the quiz
        exerciseRepository.delete(quizExercise);

        // the deleted quiz should get removed, no submissions should be saved
        quizScheduleService.processCachedQuizSubmissions();

        // quiz is not cached anymore
        assertThat(quizScheduleService.getQuizExercise(quizExercise.getId())).isNull();
        // no submissions were marked as submitted and saved
        assertThat(quizSubmissionRepository.count()).isZero();
    }
}
