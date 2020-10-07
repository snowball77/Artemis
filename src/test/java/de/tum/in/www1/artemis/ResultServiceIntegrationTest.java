package de.tum.in.www1.artemis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jgit.lib.ObjectId;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;

import de.tum.in.www1.artemis.config.Constants;
import de.tum.in.www1.artemis.domain.*;
import de.tum.in.www1.artemis.domain.enumeration.AssessmentType;
import de.tum.in.www1.artemis.domain.enumeration.DiagramType;
import de.tum.in.www1.artemis.domain.enumeration.SubmissionType;
import de.tum.in.www1.artemis.domain.modeling.ModelingExercise;
import de.tum.in.www1.artemis.domain.modeling.ModelingSubmission;
import de.tum.in.www1.artemis.domain.participation.ProgrammingExerciseStudentParticipation;
import de.tum.in.www1.artemis.domain.participation.SolutionProgrammingExerciseParticipation;
import de.tum.in.www1.artemis.domain.participation.StudentParticipation;
import de.tum.in.www1.artemis.domain.quiz.QuizExercise;
import de.tum.in.www1.artemis.domain.quiz.QuizSubmission;
import de.tum.in.www1.artemis.repository.*;
import de.tum.in.www1.artemis.service.FeedbackService;
import de.tum.in.www1.artemis.service.ProgrammingExerciseGradingService;
import de.tum.in.www1.artemis.service.ProgrammingExerciseTestCaseService;
import de.tum.in.www1.artemis.service.ResultService;
import de.tum.in.www1.artemis.util.DatabaseUtilService;
import de.tum.in.www1.artemis.util.ModelFactory;
import de.tum.in.www1.artemis.util.RequestUtilService;

public class ResultServiceIntegrationTest extends AbstractSpringIntegrationBambooBitbucketJiraTest {

    @Autowired
    ResultService resultService;

    @Autowired
    FeedbackService feedbackService;

    @Autowired
    ProgrammingExerciseTestCaseService programmingExerciseTestCaseService;

    @Autowired
    ProgrammingExerciseRepository programmingExerciseRepository;

    @Autowired
    SolutionProgrammingExerciseParticipationRepository solutionProgrammingExerciseRepository;

    @Autowired
    ModelingExerciseRepository modelingExerciseRepository;

    @Autowired
    QuizExerciseRepository quizExerciseRepository;

    @Autowired
    FileUploadExerciseRepository fileUploadExerciseRepository;

    @Autowired
    TextExerciseRepository textExerciseRepository;

    @Autowired
    UserRepository userRepo;

    @Autowired
    DatabaseUtilService database;

    @Autowired
    RequestUtilService request;

    @Autowired
    ResultRepository resultRepository;

    @Autowired
    SubmissionRepository submissionRepository;

    @Autowired
    ProgrammingExerciseGradingService gradingService;

    private Course course;

    private ProgrammingExercise programmingExercise;

    private ProgrammingExercise programmingExerciseWithStaticCodeAnalysis;

    private ModelingExercise modelingExercise;

    private SolutionProgrammingExerciseParticipation solutionParticipation;

    private ProgrammingExerciseStudentParticipation programmingExerciseStudentParticipation;

    private ProgrammingExerciseStudentParticipation programmingExerciseStudentParticipationStaticCodeAnalysis;

    private StudentParticipation studentParticipation;

    private Result result;

    @BeforeEach
    public void reset() {
        database.addUsers(10, 2, 2);
        course = database.addCourseWithOneProgrammingExercise();
        programmingExercise = programmingExerciseRepository.findAll().get(0);
        programmingExerciseWithStaticCodeAnalysis = database.addProgrammingExerciseToCourse(course, true);
        // This is done to avoid an unproxy issue in the processNewResult method of the ResultService.
        solutionParticipation = solutionProgrammingExerciseRepository.findWithEagerResultsAndSubmissionsByProgrammingExerciseId(programmingExercise.getId()).get();
        programmingExerciseStudentParticipation = database.addStudentParticipationForProgrammingExercise(programmingExercise, "student1");
        programmingExerciseStudentParticipationStaticCodeAnalysis = database.addStudentParticipationForProgrammingExercise(programmingExerciseWithStaticCodeAnalysis, "student1");

        database.addCourseWithOneModelingExercise();
        modelingExercise = modelingExerciseRepository.findAll().get(0);
        modelingExercise.setDueDate(ZonedDateTime.now().minusHours(1));
        modelingExerciseRepository.save(modelingExercise);
        studentParticipation = database.addParticipationForExercise(modelingExercise, "student2");

        result = ModelFactory.generateResult(true, 200).resultString("Good effort!").participation(programmingExerciseStudentParticipation);
        List<Feedback> feedbacks = ModelFactory.generateFeedback().stream().peek(feedback -> feedback.setText("Good work here")).collect(Collectors.toList());
        result.setFeedbacks(feedbacks);
        result.setAssessmentType(AssessmentType.SEMI_AUTOMATIC);

        String dummyHash = "9b3a9bd71a0d80e5bbc42204c319ed3d1d4f0d6d";
        doReturn(ObjectId.fromString(dummyHash)).when(gitService).getLastCommitHash(ArgumentMatchers.any());
    }

    @AfterEach
    public void tearDown() {
        database.resetDatabase();
    }

    @Test
    @WithMockUser(value = "student1", roles = "USER")
    public void shouldUpdateTestCasesAndResultScoreFromSolutionParticipationResult() {
        database.createProgrammingSubmission(programmingExerciseStudentParticipation, false);

        Set<ProgrammingExerciseTestCase> expectedTestCases = new HashSet<>();
        expectedTestCases
                .add(new ProgrammingExerciseTestCase().exercise(programmingExercise).testName("test1").active(true).weight(1.0).id(1L).bonusMultiplier(1D).bonusPoints(0D));
        expectedTestCases
                .add(new ProgrammingExerciseTestCase().exercise(programmingExercise).testName("test2").active(true).weight(1.0).id(2L).bonusMultiplier(1D).bonusPoints(0D));
        expectedTestCases
                .add(new ProgrammingExerciseTestCase().exercise(programmingExercise).testName("test4").active(true).weight(1.0).id(3L).bonusMultiplier(1D).bonusPoints(0D));

        final var resultNotification = ModelFactory.generateBambooBuildResult(Constants.ASSIGNMENT_REPO_NAME, List.of("test1", "test2", "test4"), List.of());
        final var optionalResult = gradingService.processNewProgrammingExerciseResult(solutionParticipation, resultNotification);

        Set<ProgrammingExerciseTestCase> testCases = programmingExerciseTestCaseService.findByExerciseId(programmingExercise.getId());
        assertThat(testCases).usingElementComparatorIgnoringFields("exercise", "id").isEqualTo(expectedTestCases);
        assertThat(optionalResult).isPresent();
        assertThat(optionalResult.get().getScore()).isEqualTo(100L);
    }

    @Test
    @WithMockUser(value = "student1", roles = "USER")
    public void shouldStoreFeedbackForResultWithStaticCodeAnalysisReport() {
        final var resultNotification = ModelFactory.generateBambooBuildResultWithStaticCodeAnalysisReport(Constants.ASSIGNMENT_REPO_NAME, List.of("test1"), List.of());
        final var staticCodeAnalysisFeedback = feedbackService
                .createFeedbackFromStaticCodeAnalysisReports(resultNotification.getBuild().getJobs().get(0).getStaticCodeAnalysisReports());
        final var optionalResult = gradingService.processNewProgrammingExerciseResult(programmingExerciseStudentParticipationStaticCodeAnalysis, resultNotification);
        final var savedResult = resultService.findOneWithEagerSubmissionAndFeedback(optionalResult.get().getId());

        // Create comparator to explicitly compare feedback attributes (equals only compares id)
        Comparator<? super Feedback> scaFeedbackComparator = (Comparator<Feedback>) (fb1, fb2) -> {
            if (Objects.equals(fb1.getDetailText(), fb2.getDetailText()) && Objects.equals(fb1.getText(), fb2.getText())
                    && Objects.equals(fb1.getReference(), fb2.getReference())) {
                return 0;
            }
            else {
                return 1;
            }
        };

        assertThat(optionalResult).isPresent();
        var result = optionalResult.get();
        assertThat(result.getFeedbacks()).usingElementComparator(scaFeedbackComparator).containsAll(savedResult.getFeedbacks());
        assertThat(savedResult.getFeedbacks()).usingElementComparator(scaFeedbackComparator).containsAll(staticCodeAnalysisFeedback);
        assertThat(result.getFeedbacks()).usingElementComparator(scaFeedbackComparator).containsAll(staticCodeAnalysisFeedback);
    }

    @Test
    @WithMockUser(value = "student1", roles = "USER")
    public void testRemoveCIDirectoriesFromPath() throws Exception {
        // 1. Test that paths not containing the Constant.STUDENT_WORKING_DIRECTORY are not shortened
        String pathWithoutWorkingDir = "Path/Without/StudentWorkingDirectory/Constant";

        var resultNotification1 = ModelFactory.generateBambooBuildResultWithStaticCodeAnalysisReport(Constants.ASSIGNMENT_REPO_NAME, List.of("test1"), List.of());
        for (var reports : resultNotification1.getBuild().getJobs().iterator().next().getStaticCodeAnalysisReports()) {
            for (var issue : reports.getIssues()) {
                issue.setFilePath(pathWithoutWorkingDir);
            }
        }
        var staticCodeAnalysisFeedback1 = feedbackService
                .createFeedbackFromStaticCodeAnalysisReports(resultNotification1.getBuild().getJobs().get(0).getStaticCodeAnalysisReports());

        for (var feedback : staticCodeAnalysisFeedback1) {
            JSONObject issueJSON = new JSONObject(feedback.getDetailText());
            assertThat(pathWithoutWorkingDir).isEqualTo(issueJSON.get("filePath"));
        }

        // 2. Test that null or empty paths default to FeedbackService.DEFAULT_FILEPATH
        var resultNotification2 = ModelFactory.generateBambooBuildResultWithStaticCodeAnalysisReport(Constants.ASSIGNMENT_REPO_NAME, List.of("test1"), List.of());
        var reports2 = resultNotification2.getBuild().getJobs().iterator().next().getStaticCodeAnalysisReports();
        for (int i = 0; i < reports2.size(); i++) {
            var report = reports2.get(i);
            // Set null or empty String to test both
            if (i % 2 == 0) {
                for (var issue : report.getIssues()) {
                    issue.setFilePath("");
                }
            }
            else {
                for (var issue : report.getIssues()) {
                    issue.setFilePath(null);
                }
            }
        }
        final var staticCodeAnalysisFeedback2 = feedbackService
                .createFeedbackFromStaticCodeAnalysisReports(resultNotification2.getBuild().getJobs().get(0).getStaticCodeAnalysisReports());

        for (var feedback : staticCodeAnalysisFeedback2) {
            JSONObject issueJSON = new JSONObject(feedback.getDetailText());
            assertThat(FeedbackService.DEFAULT_FILEPATH).isEqualTo(issueJSON.get("filePath"));
        }
    }

    @Test
    @WithMockUser(value = "student1", roles = "USER")
    public void shouldReturnTheResultDetailsForAProgrammingExerciseStudentParticipation() throws Exception {
        Result result = database.addResultToParticipation(null, null, programmingExerciseStudentParticipation);
        result = database.addSampleFeedbackToResults(result);

        List<Feedback> feedbacks = request.getList("/api/results/" + result.getId() + "/details", HttpStatus.OK, Feedback.class);

        assertThat(feedbacks).isEqualTo(result.getFeedbacks());
    }

    @Test
    @WithMockUser(value = "student1", roles = "USER")
    public void shouldReturnTheResultDetailsWithStaticCodeAnalysisFeedbackForAProgrammingExerciseStudentParticipation() throws Exception {
        Result result = database.addResultToParticipation(null, null, programmingExerciseStudentParticipation);
        result = database.addSampleStaticCodeAnalysisFeedbackToResults(result);

        List<Feedback> feedback = request.getList("/api/results/" + result.getId() + "/details", HttpStatus.OK, Feedback.class);

        assertThat(feedback).isEqualTo(result.getFeedbacks());
    }

    @Test
    @WithMockUser(value = "student2", roles = "USER")
    public void shouldReturnTheResultDetailsForAStudentParticipation() throws Exception {
        Result result = database.addResultToParticipation(null, null, studentParticipation);
        result = database.addSampleFeedbackToResults(result);

        List<Feedback> feedbacks = request.getList("/api/results/" + result.getId() + "/details", HttpStatus.OK, Feedback.class);

        assertThat(feedbacks).isEqualTo(result.getFeedbacks());
    }

    @Test
    @WithMockUser(value = "student1", roles = "USER")
    public void shouldReturnTheResultDetailsForAStudentParticipation_studentForbidden() throws Exception {
        Result result = database.addResultToParticipation(null, null, studentParticipation);
        result = database.addSampleFeedbackToResults(result);

        request.getList("/api/results/" + result.getId() + "/details", HttpStatus.FORBIDDEN, Feedback.class);
    }

    @Test
    @WithMockUser(value = "student1", roles = "USER")
    public void shouldReturnTheResultDetailsForAProgrammingExerciseStudentParticipation_studentForbidden() throws Exception {
        Result result = database.addResultToParticipation(null, null, solutionParticipation);
        result = database.addSampleFeedbackToResults(result);
        request.getList("/api/results/" + result.getId() + "/details", HttpStatus.FORBIDDEN, Feedback.class);
    }

    @Test
    @WithMockUser(value = "student1", roles = "USER")
    public void shouldReturnNotFoundForNonExistingResult() throws Exception {
        Result result = database.addResultToParticipation(null, null, solutionParticipation);
        result = database.addSampleFeedbackToResults(result);
        request.getList("/api/results/" + 11667 + "/details", HttpStatus.NOT_FOUND, Feedback.class);
    }

    @ParameterizedTest
    @MethodSource("setResultRatedPermutations")
    @WithMockUser(value = "instructor1", roles = "INSTRUCTOR")
    void setProgrammingExerciseResultRated(boolean shouldBeRated, ZonedDateTime buildAndTestAfterDueDate, SubmissionType submissionType, ZonedDateTime dueDate) {

        ProgrammingSubmission programmingSubmission = (ProgrammingSubmission) new ProgrammingSubmission().commitHash("abc").type(submissionType).submitted(true)
                .submissionDate(ZonedDateTime.now());
        database.addProgrammingSubmission(programmingExercise, programmingSubmission, "student1");
        Result result = database.addResultToParticipation(programmingExerciseStudentParticipation, programmingSubmission);
        programmingExercise.setBuildAndTestStudentSubmissionsAfterDueDate(buildAndTestAfterDueDate);
        programmingExercise.setDueDate(dueDate);
        programmingExerciseRepository.save(programmingExercise);

        result.setRatedIfNotExceeded(programmingExercise.getDueDate(), programmingSubmission);
        assertThat(result.isRated() == shouldBeRated).isTrue();
    }

    private static Stream<Arguments> setResultRatedPermutations() {
        ZonedDateTime dateInFuture = ZonedDateTime.now().plusHours(1);
        ZonedDateTime dateInPast = ZonedDateTime.now().minusHours(1);
        return Stream.of(
                // The due date has not passed, normal student submission => rated result.
                Arguments.of(true, null, SubmissionType.MANUAL, dateInFuture),
                // The due date is not set, normal student submission => rated result.
                Arguments.of(true, null, SubmissionType.MANUAL, null),
                // The due date has passed, normal student submission => unrated result.
                Arguments.of(false, null, SubmissionType.MANUAL, dateInPast),
                // The result was generated by an instructor => rated result.
                Arguments.of(true, null, SubmissionType.INSTRUCTOR, dateInPast),
                // The result was generated by test update => rated result.
                Arguments.of(true, null, SubmissionType.TEST, dateInPast),
                // The build and test date has passed, the due date has passed, the result is generated by a test update => rated result.
                Arguments.of(true, dateInPast, SubmissionType.TEST, dateInPast),
                // The build and test date has passed, the due date has passed, the result is generated by an instructor => rated result.
                Arguments.of(true, dateInPast, SubmissionType.INSTRUCTOR, dateInPast),
                // The build and test date has not passed, due date has not passed, normal student submission => rated result.
                Arguments.of(true, dateInFuture, SubmissionType.MANUAL, dateInFuture),
                // The build and test date has not passed, due date has passed, normal student submission => unrated result.
                Arguments.of(false, dateInFuture, SubmissionType.MANUAL, dateInPast));
    }

    @Test
    @WithMockUser(value = "instructor1", roles = "INSTRUCTOR")
    public void testGetResultsForProgrammingExercise() throws Exception {
        var now = ZonedDateTime.now();

        for (int i = 1; i <= 10; i++) {
            ProgrammingSubmission programmingSubmission = new ProgrammingSubmission();
            programmingSubmission.submitted(true);
            programmingSubmission.submissionDate(now.minusHours(3));
            database.addSubmission(programmingExercise, programmingSubmission, "student" + i);
            if (i % 3 == 0) {
                database.addResultToSubmission(programmingSubmission, AssessmentType.AUTOMATIC, null, 10L, true);
            }
            else if (i % 4 == 0) {
                database.addResultToSubmission(programmingSubmission, AssessmentType.AUTOMATIC, null, 20L, true);
            }
        }

        List<Result> results = request.getList("/api/exercises/" + programmingExercise.getId() + "/results", HttpStatus.OK, Result.class);
        assertThat(results).hasSize(5);
        // TODO: check additional values
    }

    @Test
    @WithMockUser(value = "instructor1", roles = "INSTRUCTOR")
    public void testGetResultsForQuizExercise() throws Exception {
        var now = ZonedDateTime.now();

        QuizExercise quizExercise = database.createQuiz(course, now.minusMinutes(5), now.minusMinutes(2));
        quizExerciseRepository.save(quizExercise);

        for (int i = 1; i <= 10; i++) {
            QuizSubmission quizSubmission = new QuizSubmission();
            quizSubmission.setScoreInPoints(2.0);
            quizSubmission.submitted(true);
            quizSubmission.submissionDate(now.minusHours(3));
            database.addSubmission(quizExercise, quizSubmission, "student" + i);
            if (i % 3 == 0) {
                database.addResultToSubmission(quizSubmission, AssessmentType.AUTOMATIC, null, 10L, true);
            }
            else if (i % 4 == 0) {
                database.addResultToSubmission(quizSubmission, AssessmentType.AUTOMATIC, null, 20L, true);
            }
        }

        List<Result> results = request.getList("/api/exercises/" + quizExercise.getId() + "/results", HttpStatus.OK, Result.class);
        assertThat(results).hasSize(5);
        // TODO: check additional values
    }

    @Test
    @WithMockUser(value = "instructor1", roles = "INSTRUCTOR")
    public void testGetResultsForModelingExercise() throws Exception {
        var now = ZonedDateTime.now();
        for (int i = 1; i <= 10; i++) {
            ModelingSubmission modelingSubmission = new ModelingSubmission();
            modelingSubmission.model("Text");
            modelingSubmission.submitted(true);
            modelingSubmission.submissionDate(now.minusHours(3));
            database.addSubmission(modelingExercise, modelingSubmission, "student" + i);
            if (i % 3 == 0) {
                database.addResultToSubmission(modelingSubmission, AssessmentType.MANUAL, database.getUserByLogin("instructor1"), 10L, true);
            }
            else if (i % 4 == 0) {
                database.addResultToSubmission(modelingSubmission, AssessmentType.SEMI_AUTOMATIC, database.getUserByLogin("instructor1"), 20L, true);
            }
        }

        List<Result> results = request.getList("/api/exercises/" + modelingExercise.getId() + "/results", HttpStatus.OK, Result.class);
        assertThat(results).hasSize(5);
        // TODO: check additional values
    }

    @Test
    @WithMockUser(value = "instructor1", roles = "INSTRUCTOR")
    public void testGetResultsForTextExercise() throws Exception {
        var now = ZonedDateTime.now();
        TextExercise textExercise = ModelFactory.generateTextExercise(now.minusDays(1), now.minusHours(2), now.minusHours(1), course);
        course.addExercises(textExercise);
        textExerciseRepository.save(textExercise);

        for (int i = 1; i <= 10; i++) {
            TextSubmission textSubmission = new TextSubmission();
            textSubmission.text("Text");
            textSubmission.submitted(true);
            textSubmission.submissionDate(now.minusHours(3));
            database.addSubmission(textExercise, textSubmission, "student" + i);
            if (i % 3 == 0) {
                database.addResultToSubmission(textSubmission, AssessmentType.MANUAL, database.getUserByLogin("instructor1"), 10L, true);
            }
            else if (i % 4 == 0) {
                database.addResultToSubmission(textSubmission, AssessmentType.SEMI_AUTOMATIC, database.getUserByLogin("instructor1"), 20L, true);
            }
        }

        List<Result> results = request.getList("/api/exercises/" + textExercise.getId() + "/results", HttpStatus.OK, Result.class);
        assertThat(results).hasSize(5);
        // TODO: check additional values
    }

    @Test
    @WithMockUser(value = "instructor1", roles = "INSTRUCTOR")
    public void testGetResultsForFileUploadExercise() throws Exception {
        var now = ZonedDateTime.now();
        FileUploadExercise fileUploadExercise = ModelFactory.generateFileUploadExercise(now.minusDays(1), now.minusHours(2), now.minusHours(1), "pdf", course);
        course.addExercises(fileUploadExercise);
        fileUploadExerciseRepository.save(fileUploadExercise);

        for (int i = 1; i <= 10; i++) {
            FileUploadSubmission fileUploadSubmission = new FileUploadSubmission();
            fileUploadSubmission.submitted(true);
            fileUploadSubmission.submissionDate(now.minusHours(3));
            database.addSubmission(fileUploadExercise, fileUploadSubmission, "student" + i);
            if (i % 3 == 0) {
                database.addResultToSubmission(fileUploadSubmission, AssessmentType.MANUAL, database.getUserByLogin("instructor1"), 10L, true);
            }
            else if (i % 4 == 0) {
                database.addResultToSubmission(fileUploadSubmission, AssessmentType.MANUAL, database.getUserByLogin("instructor1"), 20L, true);
            }
        }

        List<Result> results = request.getList("/api/exercises/" + fileUploadExercise.getId() + "/results", HttpStatus.OK, Result.class);
        assertThat(results).hasSize(5);
        // TODO: check additional values
    }

    @Test
    @WithMockUser(value = "tutor1", roles = "TA")
    public void getResult() throws Exception {
        Result result = database.addResultToParticipation(null, null, studentParticipation);
        result = database.addSampleFeedbackToResults(result);
        Result returnedResult = request.get("/api/results/" + result.getId(), HttpStatus.OK, Result.class);
        assertThat(returnedResult).isNotNull();
        assertThat(returnedResult).isEqualTo(result);
    }

    @Test
    @WithMockUser(value = "student1", roles = "USER")
    public void getResult_asStudent() throws Exception {
        Result result = database.addResultToParticipation(null, null, studentParticipation);
        request.get("/api/results/" + result.getId(), HttpStatus.FORBIDDEN, Result.class);
    }

    @Test
    @WithMockUser(value = "tutor1", roles = "TA")
    public void getLatestResultWithFeedbacks() throws Exception {
        Result result = database.addResultToParticipation(null, null, studentParticipation);
        result.setCompletionDate(ZonedDateTime.now().minusHours(10));
        Result latestResult = database.addResultToParticipation(null, null, studentParticipation);
        latestResult.setCompletionDate(ZonedDateTime.now());
        result = database.addSampleFeedbackToResults(result);
        latestResult = database.addSampleFeedbackToResults(latestResult);
        Result returnedResult = request.get("/api/participations/" + studentParticipation.getId() + "/latest-result", HttpStatus.OK, Result.class);
        assertThat(returnedResult).isNotNull();
        assertThat(returnedResult).isEqualTo(latestResult);
    }

    @Test
    @WithMockUser(value = "student1", roles = "USER")
    public void getLatestResultWithFeedbacks_asStudent() throws Exception {
        Result result = database.addResultToParticipation(null, null, studentParticipation);
        result = database.addSampleFeedbackToResults(result);
        request.get("/api/participations/" + studentParticipation.getId() + "/latest-result", HttpStatus.FORBIDDEN, Result.class);
    }

    @Test
    @WithMockUser(value = "tutor1", roles = "TA")
    public void deleteResult() throws Exception {
        Result result = database.addResultToParticipation(null, null, studentParticipation);
        result = database.addSampleFeedbackToResults(result);
        request.delete("/api/results/" + result.getId(), HttpStatus.OK);
        assertThat(resultRepository.existsById(result.getId())).isFalse();
        request.delete("api/results/" + result.getId(), HttpStatus.NOT_FOUND);
    }

    @Test
    @WithMockUser(value = "tutor1", roles = "TA")
    public void getResultForSubmission() throws Exception {
        var now = ZonedDateTime.now();
        TextExercise textExercise = ModelFactory.generateTextExercise(now.minusDays(1), now.minusHours(2), now.minusHours(1), course);
        course.addExercises(textExercise);
        textExerciseRepository.save(textExercise);
        TextSubmission textSubmission = new TextSubmission();
        database.addSubmission(textExercise, textSubmission, "student1");
        Result result = database.addResultToSubmission(textSubmission, null);
        Result returnedResult = request.get("/api/results/submission/" + textSubmission.getId(), HttpStatus.OK, Result.class);
        assertThat(returnedResult).isEqualTo(result);
    }

    @Test
    @WithMockUser(value = "instructor1", roles = "INSTRUCTOR")
    public void createExampleResult() throws Exception {
        var modelingSubmission = database.addSubmission(modelingExercise, new ModelingSubmission(), "student1");
        var exampleSubmission = ModelFactory.generateExampleSubmission(modelingSubmission, modelingExercise, false);
        exampleSubmission = database.addExampleSubmission(exampleSubmission);
        modelingSubmission.setExampleSubmission(true);
        submissionRepository.save(modelingSubmission);
        request.postWithResponseBody("/api/submissions/" + modelingSubmission.getId() + "/example-result", exampleSubmission, Result.class, HttpStatus.CREATED);
    }

    @Test
    @WithMockUser(value = "instructor1", roles = "INSTRUCTOR")
    public void createResultForExternalSubmission() throws Exception {
        Result result = new Result().rated(false);
        request.postWithResponseBody("/api/exercises/" + modelingExercise.getId() + "/external-submission-results?studentLogin=student1", result, Result.class, HttpStatus.CREATED);
    }

    @Test
    @WithMockUser(value = "instructor1", roles = "INSTRUCTOR")
    public void createResultForExternalSubmission_quizExercise() throws Exception {
        var now = ZonedDateTime.now();
        var quizExercise = ModelFactory.generateQuizExercise(now.minusDays(1), now.minusHours(2), course);
        course.addExercises(quizExercise);
        quizExerciseRepository.save(quizExercise);
        Result result = new Result().rated(false);
        request.postWithResponseBody("/api/exercises/" + quizExercise.getId() + "/external-submission-results?studentLogin=student1", result, Result.class, HttpStatus.BAD_REQUEST);
    }

    @Test
    @WithMockUser(value = "instructor1", roles = "INSTRUCTOR")
    public void createResultForExternalSubmission_studentNotInTheCourse() throws Exception {
        Result result = new Result().rated(false);
        request.postWithResponseBody("/api/exercises/" + modelingExercise.getId() + "/external-submission-results?studentLogin=student11", result, Result.class,
                HttpStatus.BAD_REQUEST);
    }

    @Test
    @WithMockUser(value = "instructor1", roles = "INSTRUCTOR")
    public void createResultForExternalSubmission_dueDateNotPassed() throws Exception {
        modelingExercise.setDueDate(ZonedDateTime.now().plusHours(1));
        modelingExerciseRepository.save(modelingExercise);
        Result result = new Result().rated(false);
        request.postWithResponseBody("/api/exercises/" + modelingExercise.getId() + "/external-submission-results?studentLogin=student1", result, Result.class,
                HttpStatus.BAD_REQUEST);
    }

    @Test
    @WithMockUser(value = "instructor1", roles = "INSTRUCTOR")
    public void createResultForExternalSubmission_resultExists() throws Exception {
        var now = ZonedDateTime.now();
        var modelingExercise = ModelFactory.generateModelingExercise(now.minusDays(1), now.minusHours(2), now.minusHours(1), DiagramType.ClassDiagram, course);
        course.addExercises(modelingExercise);
        modelingExerciseRepository.save(modelingExercise);
        var participation = database.addParticipationForExercise(modelingExercise, "student1");
        var result = database.addResultToParticipation(null, null, participation);
        request.postWithResponseBody("/api/exercises/" + modelingExercise.getId() + "/external-submission-results?studentLogin=student1", result, Result.class,
                HttpStatus.BAD_REQUEST);
    }
}
