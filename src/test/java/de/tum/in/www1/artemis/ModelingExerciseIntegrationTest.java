package de.tum.in.www1.artemis;

import static de.tum.in.www1.artemis.domain.enumeration.DiagramType.CommunicationDiagram;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.time.ZonedDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.util.LinkedMultiValueMap;

import de.tum.in.www1.artemis.domain.Course;
import de.tum.in.www1.artemis.domain.GradingCriterion;
import de.tum.in.www1.artemis.domain.GradingInstruction;
import de.tum.in.www1.artemis.domain.enumeration.DiagramType;
import de.tum.in.www1.artemis.domain.enumeration.DifficultyLevel;
import de.tum.in.www1.artemis.domain.exam.ExerciseGroup;
import de.tum.in.www1.artemis.domain.modeling.ModelingExercise;
import de.tum.in.www1.artemis.repository.ExerciseRepository;
import de.tum.in.www1.artemis.repository.ModelingExerciseRepository;
import de.tum.in.www1.artemis.repository.UserRepository;
import de.tum.in.www1.artemis.util.DatabaseUtilService;
import de.tum.in.www1.artemis.util.ModelFactory;
import de.tum.in.www1.artemis.util.ModelingExerciseUtilService;
import de.tum.in.www1.artemis.util.RequestUtilService;
import de.tum.in.www1.artemis.web.rest.dto.SearchResultPageDTO;

public class ModelingExerciseIntegrationTest extends AbstractSpringIntegrationBambooBitbucketJiraTest {

    @Autowired
    ExerciseRepository exerciseRepo;

    @Autowired
    RequestUtilService request;

    @Autowired
    DatabaseUtilService database;

    @Autowired
    ModelingExerciseUtilService modelingExerciseUtilService;

    @Autowired
    ModelingExerciseRepository modelingExerciseRepository;

    @Autowired
    UserRepository userRepo;

    private ModelingExercise classExercise;

    private List<GradingCriterion> gradingCriteria;

    @BeforeEach
    public void initTestCase() throws Exception {
        database.addUsers(1, 1, 1);
        Course course = database.addCourseWithOneModelingExercise();
        classExercise = (ModelingExercise) course.getExercises().iterator().next();

        // Add users that are not in course
        userRepo.save(ModelFactory.generateActivatedUser("instructor2"));
        userRepo.save(ModelFactory.generateActivatedUser("tutor2"));
    }

    @AfterEach
    public void tearDown() {
        database.resetDatabase();
    }

    @Test
    @WithMockUser(roles = "USER")
    public void getCompassStatistic_asStudent_Forbidden() throws Exception {
        request.get("/api/modeling-exercises/" + classExercise.getId() + "/print-statistic", HttpStatus.FORBIDDEN, Void.class);
    }

    @Test
    @WithMockUser(roles = "TA")
    public void getCompassStatistic_asTutor_Forbidden() throws Exception {
        request.get("/api/modeling-exercises/" + classExercise.getId() + "/print-statistic", HttpStatus.FORBIDDEN, Void.class);
    }

    @Test
    @WithMockUser(roles = "INSTRUCTOR")
    public void getCompassStatistic_asInstructor_Forbidden() throws Exception {
        request.get("/api/modeling-exercises/" + classExercise.getId() + "/print-statistic", HttpStatus.FORBIDDEN, Void.class);
    }

    @Test
    @WithMockUser(username = "user1", roles = "USER")
    public void testGetModelingExercise_asStudent_Forbidden() throws Exception {
        request.get("/api/modeling-exercises/" + classExercise.getId(), HttpStatus.FORBIDDEN, ModelingExercise.class);
    }

    @Test
    @WithMockUser(username = "tutor1", roles = "TA")
    public void testGetModelingExercise_asTA() throws Exception {
        ModelingExercise receivedModelingExercise = request.get("/api/modeling-exercises/" + classExercise.getId(), HttpStatus.OK, ModelingExercise.class);
        gradingCriteria = database.addGradingInstructionsToExercise(receivedModelingExercise);
        assertThat(receivedModelingExercise.getGradingCriteria().get(0).getTitle()).isEqualTo(null);
        assertThat(receivedModelingExercise.getGradingCriteria().get(1).getTitle()).isEqualTo("test title");

        assertThat(gradingCriteria.get(0).getStructuredGradingInstructions().size()).isEqualTo(1);
        assertThat(gradingCriteria.get(1).getStructuredGradingInstructions().size()).isEqualTo(3);
        assertThat(gradingCriteria.get(0).getStructuredGradingInstructions().get(0).getInstructionDescription())
                .isEqualTo("created first instruction with empty criteria for testing");
    }

    @Test
    @WithMockUser(username = "tutor2", roles = "TA")
    public void testGetModelingExercise_tutorNotInCourse() throws Exception {
        request.get("/api/modeling-exercises/" + classExercise.getId(), HttpStatus.FORBIDDEN, ModelingExercise.class);
    }

    @Test
    @WithMockUser(username = "tutor1", roles = "TA")
    public void testGetModelingExerciseForCourse_asTA() throws Exception {
        request.get("/api/courses/" + classExercise.getCourseViaExerciseGroupOrCourseMember().getId() + "/modeling-exercises", HttpStatus.OK, List.class);
    }

    @Test
    @WithMockUser(username = "tutor2", roles = "TA")
    public void testGetModelingExerciseForCourse_tutorNotInCourse() throws Exception {
        request.get("/api/courses/" + classExercise.getCourseViaExerciseGroupOrCourseMember().getId() + "/modeling-exercises", HttpStatus.FORBIDDEN, List.class);
    }

    @Test
    @WithMockUser(username = "tutor1", roles = "TA")
    public void testGetModelingExerciseStatistics_asTA() throws Exception {
        request.get("/api/modeling-exercises/" + classExercise.getId() + "/statistics", HttpStatus.OK, String.class);
        request.get("/api/modeling-exercises/" + classExercise.getId() + 1 + "/statistics", HttpStatus.NOT_FOUND, String.class);

        classExercise.setDiagramType(CommunicationDiagram);
        exerciseRepo.save(classExercise);
        request.get("/api/modeling-exercises/" + classExercise.getId() + "/statistics", HttpStatus.NOT_FOUND, String.class);
    }

    @Test
    @WithMockUser(username = "tutor2", roles = "TA")
    public void testGetModelingExerciseStatistics_tutorNotInCourse() throws Exception {
        request.get("/api/modeling-exercises/" + classExercise.getId() + "/statistics", HttpStatus.FORBIDDEN, String.class);
    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    public void testCreateModelingExercise_asInstructor() throws Exception {
        ModelingExercise modelingExercise = modelingExerciseUtilService.createModelingExercise(classExercise.getCourseViaExerciseGroupOrCourseMember().getId());
        gradingCriteria = database.addGradingInstructionsToExercise(modelingExercise);
        ModelingExercise receivedModelingExercise = request.postWithResponseBody("/api/modeling-exercises", modelingExercise, ModelingExercise.class, HttpStatus.CREATED);
        assertThat(receivedModelingExercise.getGradingCriteria().get(0).getStructuredGradingInstructions().size()).isEqualTo(1);
        assertThat(receivedModelingExercise.getGradingCriteria().get(1).getStructuredGradingInstructions().size()).isEqualTo(3);

        modelingExercise = modelingExerciseUtilService.createModelingExercise(classExercise.getCourseViaExerciseGroupOrCourseMember().getId(), 1L);
        request.post("/api/modeling-exercises", modelingExercise, HttpStatus.BAD_REQUEST);

        modelingExercise = modelingExerciseUtilService.createModelingExercise(2L);
        request.post("/api/modeling-exercises", modelingExercise, HttpStatus.NOT_FOUND);
    }

    @Test
    @WithMockUser(username = "instructor2", roles = "INSTRUCTOR")
    public void testCreateModelingExercise_instructorNotInCourse() throws Exception {
        ModelingExercise modelingExercise = modelingExerciseUtilService.createModelingExercise(classExercise.getCourseViaExerciseGroupOrCourseMember().getId());
        request.post("/api/modeling-exercises", modelingExercise, HttpStatus.FORBIDDEN);
    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    public void testUpdateModelingExercise_asInstructor() throws Exception {
        ModelingExercise modelingExercise = modelingExerciseUtilService.createModelingExercise(classExercise.getCourseViaExerciseGroupOrCourseMember().getId());
        ModelingExercise createdModelingExercise = request.postWithResponseBody("/api/modeling-exercises", modelingExercise, ModelingExercise.class, HttpStatus.CREATED);
        gradingCriteria = database.addGradingInstructionsToExercise(modelingExercise);

        ModelingExercise modelingExerciseWithSubmission = modelingExerciseUtilService.addExampleSubmission(createdModelingExercise);
        var params = new LinkedMultiValueMap<String, String>();
        var notificationText = "notified!";
        params.add("notificationText", notificationText);
        ModelingExercise returnedModelingExercise = request.putWithResponseBodyAndParams("/api/modeling-exercises", modelingExerciseWithSubmission, ModelingExercise.class,
                HttpStatus.OK, params);
        assertThat(returnedModelingExercise.getExampleSubmissions().size()).isEqualTo(1);
        verify(groupNotificationService).notifyStudentGroupAboutExerciseUpdate(returnedModelingExercise, notificationText);

        // use an arbitrary course id that was not yet stored on the server to get a bad request in the PUT call
        modelingExercise = modelingExerciseUtilService.createModelingExercise(100L, classExercise.getId());
        request.put("/api/modeling-exercises", modelingExercise, HttpStatus.NOT_FOUND);
    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    public void testUpdateModelingExerciseCriteria_asInstructor() throws Exception {
        ModelingExercise modelingExercise = modelingExerciseUtilService.createModelingExercise(classExercise.getCourseViaExerciseGroupOrCourseMember().getId());
        gradingCriteria = database.addGradingInstructionsToExercise(modelingExercise);
        var currentCriteriaSize = modelingExercise.getGradingCriteria().size();
        var newCriteria = new GradingCriterion();
        newCriteria.setTitle("new");
        modelingExercise.addGradingCriteria(newCriteria);
        ModelingExercise createdModelingExercise = request.postWithResponseBody("/api/modeling-exercises", modelingExercise, ModelingExercise.class, HttpStatus.CREATED);
        assertThat(createdModelingExercise.getGradingCriteria().size()).isEqualTo(currentCriteriaSize + 1);

        modelingExercise.getGradingCriteria().get(1).setTitle("UPDATE");
        createdModelingExercise = request.postWithResponseBody("/api/modeling-exercises", modelingExercise, ModelingExercise.class, HttpStatus.CREATED);
        assertThat(createdModelingExercise.getGradingCriteria().get(1).getTitle()).isEqualTo("UPDATE");

        // If the grading criteria are deleted then their instructions should also be deleted
        modelingExercise.setGradingCriteria(null);
        createdModelingExercise = request.postWithResponseBody("/api/modeling-exercises", modelingExercise, ModelingExercise.class, HttpStatus.CREATED);
        assertThat(createdModelingExercise.getGradingCriteria().size()).isEqualTo(0);
    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    public void testUpdateModelingExerciseInstructions_asInstructor() throws Exception {
        ModelingExercise modelingExercise = modelingExerciseUtilService.createModelingExercise(classExercise.getCourseViaExerciseGroupOrCourseMember().getId());
        gradingCriteria = database.addGradingInstructionsToExercise(modelingExercise);

        var currentInstructionsSize = modelingExercise.getGradingCriteria().get(1).getStructuredGradingInstructions().size();
        var newInstruction = new GradingInstruction();
        newInstruction.setInstructionDescription("New Instruction");

        modelingExercise.getGradingCriteria().get(1).addStructuredGradingInstructions(newInstruction);
        ModelingExercise createdModelingExercise = request.postWithResponseBody("/api/modeling-exercises", modelingExercise, ModelingExercise.class, HttpStatus.CREATED);
        assertThat(createdModelingExercise.getGradingCriteria().get(1).getStructuredGradingInstructions().size()).isEqualTo(currentInstructionsSize + 1);

        modelingExercise.getGradingCriteria().get(1).getStructuredGradingInstructions().get(0).setInstructionDescription("UPDATE");
        createdModelingExercise = request.postWithResponseBody("/api/modeling-exercises", modelingExercise, ModelingExercise.class, HttpStatus.CREATED);
        assertThat(createdModelingExercise.getGradingCriteria().get(1).getStructuredGradingInstructions().get(0).getInstructionDescription()).isEqualTo("UPDATE");

        modelingExercise.getGradingCriteria().get(1).setStructuredGradingInstructions(null);
        createdModelingExercise = request.postWithResponseBody("/api/modeling-exercises", modelingExercise, ModelingExercise.class, HttpStatus.CREATED);
        assertThat(createdModelingExercise.getGradingCriteria().size()).isGreaterThan(0);
        assertThat(createdModelingExercise.getGradingCriteria().get(1).getStructuredGradingInstructions()).isEqualTo(null);
    }

    @Test
    @WithMockUser(username = "instructor2", roles = "INSTRUCTOR")
    public void testUpdateModelingExercise_instructorNotInCourse() throws Exception {
        request.put("/api/modeling-exercises", classExercise, HttpStatus.FORBIDDEN);
    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    public void testDeleteModelingExercise_asInstructor() throws Exception {
        request.delete("/api/modeling-exercises/" + classExercise.getId(), HttpStatus.OK);
        request.delete("/api/modeling-exercises/" + classExercise.getId(), HttpStatus.NOT_FOUND);
    }

    @Test
    @WithMockUser(username = "tutor1", roles = "TA")
    public void testDeleteModelingExercise_asTutor_Forbidden() throws Exception {
        request.delete("/api/modeling-exercises/" + classExercise.getId(), HttpStatus.FORBIDDEN);
    }

    @Test
    @WithMockUser(username = "instructor2", roles = "INSTRUCTOR")
    public void testDeleteModelingExercise_notInstructorInCourse() throws Exception {
        request.delete("/api/modeling-exercises/" + classExercise.getId(), HttpStatus.FORBIDDEN);
    }

    @Test
    @WithMockUser(value = "instructor1", roles = "INSTRUCTOR")
    public void importModelingExerciseFromCourseToCourse() throws Exception {
        var now = ZonedDateTime.now();
        Course course1 = database.addEmptyCourse();
        Course course2 = database.addEmptyCourse();
        ModelingExercise modelingExercise = ModelFactory.generateModelingExercise(now.minusDays(1), now.minusHours(2), now.minusHours(1), DiagramType.ClassDiagram, course1);
        modelingExerciseRepository.save(modelingExercise);
        modelingExercise.setCourse(course2);

        request.postWithResponseBody("/api/modeling-exercises/import/" + modelingExercise.getId(), modelingExercise, ModelingExercise.class, HttpStatus.CREATED);
    }

    @Test
    @WithMockUser(value = "instructor1", roles = "INSTRUCTOR")
    public void importModelingExerciseFromCourseToExam() throws Exception {
        var now = ZonedDateTime.now();
        Course course1 = database.addEmptyCourse();
        ExerciseGroup exerciseGroup1 = database.addExerciseGroupWithExamAndCourse(true);
        ModelingExercise modelingExercise = ModelFactory.generateModelingExercise(now.minusDays(1), now.minusHours(2), now.minusHours(1), DiagramType.ClassDiagram, course1);
        modelingExerciseRepository.save(modelingExercise);
        modelingExercise.setCourse(null);
        modelingExercise.setExerciseGroup(exerciseGroup1);

        request.postWithResponseBody("/api/modeling-exercises/import/" + modelingExercise.getId(), modelingExercise, ModelingExercise.class, HttpStatus.CREATED);
    }

    @Test
    @WithMockUser(value = "instructor1", roles = "TA")
    public void importModelingExerciseFromCourseToExam_forbidden() throws Exception {
        var now = ZonedDateTime.now();
        Course course1 = database.addEmptyCourse();
        ExerciseGroup exerciseGroup1 = database.addExerciseGroupWithExamAndCourse(true);
        ModelingExercise modelingExercise = ModelFactory.generateModelingExercise(now.minusDays(1), now.minusHours(2), now.minusHours(1), DiagramType.ClassDiagram, course1);
        modelingExerciseRepository.save(modelingExercise);
        modelingExercise.setCourse(null);
        modelingExercise.setExerciseGroup(exerciseGroup1);

        request.postWithResponseBody("/api/modeling-exercises/import/" + modelingExercise.getId(), modelingExercise, ModelingExercise.class, HttpStatus.FORBIDDEN);
    }

    @Test
    @WithMockUser(value = "instructor1", roles = "INSTRUCTOR")
    public void importModelingExerciseFromExamToCourse() throws Exception {
        ExerciseGroup exerciseGroup1 = database.addExerciseGroupWithExamAndCourse(true);
        ModelingExercise modelingExercise = ModelFactory.generateModelingExerciseForExam(DiagramType.ClassDiagram, exerciseGroup1);
        Course course1 = database.addEmptyCourse();
        modelingExerciseRepository.save(modelingExercise);
        modelingExercise.setCourse(course1);
        modelingExercise.setExerciseGroup(null);

        request.postWithResponseBody("/api/modeling-exercises/import/" + modelingExercise.getId(), modelingExercise, ModelingExercise.class, HttpStatus.CREATED);
    }

    @Test
    @WithMockUser(value = "instructor1", roles = "TA")
    public void importModelingExerciseFromExamToCourse_forbidden() throws Exception {
        ExerciseGroup exerciseGroup1 = database.addExerciseGroupWithExamAndCourse(true);
        ModelingExercise modelingExercise = ModelFactory.generateModelingExerciseForExam(DiagramType.ClassDiagram, exerciseGroup1);
        Course course1 = database.addEmptyCourse();
        modelingExerciseRepository.save(modelingExercise);
        modelingExercise.setCourse(course1);
        modelingExercise.setExerciseGroup(null);

        request.postWithResponseBody("/api/modeling-exercises/import/" + modelingExercise.getId(), modelingExercise, ModelingExercise.class, HttpStatus.FORBIDDEN);
    }

    @Test
    @WithMockUser(value = "instructor1", roles = "INSTRUCTOR")
    public void importModelingExerciseFromExamToExam() throws Exception {
        ExerciseGroup exerciseGroup1 = database.addExerciseGroupWithExamAndCourse(true);
        ExerciseGroup exerciseGroup2 = database.addExerciseGroupWithExamAndCourse(true);
        ModelingExercise modelingExercise = ModelFactory.generateModelingExerciseForExam(DiagramType.ClassDiagram, exerciseGroup1);
        modelingExerciseRepository.save(modelingExercise);
        modelingExercise.setExerciseGroup(exerciseGroup2);

        request.postWithResponseBody("/api/modeling-exercises/import/" + modelingExercise.getId(), modelingExercise, ModelingExercise.class, HttpStatus.CREATED);
    }

    @Test
    @WithMockUser(value = "instructor1", roles = "INSTRUCTOR")
    public void importModelingExerciseFromCourseToCourse_badRequest() throws Exception {
        var now = ZonedDateTime.now();
        Course course1 = database.addEmptyCourse();
        ModelingExercise modelingExercise = ModelFactory.generateModelingExercise(now.minusDays(1), now.minusHours(2), now.minusHours(1), DiagramType.ClassDiagram, course1);
        modelingExerciseRepository.save(modelingExercise);
        modelingExercise.setCourse(null);

        request.postWithResponseBody("/api/modeling-exercises/import/" + modelingExercise.getId(), modelingExercise, ModelingExercise.class, HttpStatus.BAD_REQUEST);
    }

    @Test
    @WithMockUser(value = "instructor1", roles = "INSTRUCTOR")
    public void createModelingExerciseForExam() throws Exception {
        ExerciseGroup exerciseGroup = database.addExerciseGroupWithExamAndCourse(true);
        ModelingExercise modelingExercise = ModelFactory.generateModelingExerciseForExam(DiagramType.ClassDiagram, exerciseGroup);

        String title = "New Exam Modeling Exercise";
        DifficultyLevel difficulty = DifficultyLevel.HARD;
        modelingExercise.setTitle(title);
        modelingExercise.setDifficulty(difficulty);

        ModelingExercise newModelingExercise = request.postWithResponseBody("/api/modeling-exercises/", modelingExercise, ModelingExercise.class, HttpStatus.CREATED);

        assertThat(newModelingExercise.getTitle()).as("modeling exercise title was correctly set").isEqualTo(title);
        assertThat(newModelingExercise.getDifficulty()).as("modeling exercise difficulty was correctly set").isEqualTo(difficulty);
        assertThat(!newModelingExercise.hasCourse()).as("course was not set for exam exercise");
        assertThat(newModelingExercise.getExerciseGroup()).as("exerciseGroup was set for exam exercise").isNotNull();
        assertThat(newModelingExercise.getExerciseGroup().getId()).as("exerciseGroupId was set correctly").isEqualTo(exerciseGroup.getId());
    }

    @Test
    @WithMockUser(value = "instructor1", roles = "INSTRUCTOR")
    public void createModelingExercise_setCourseAndExerciseGroup_badRequest() throws Exception {
        ExerciseGroup exerciseGroup = database.addExerciseGroupWithExamAndCourse(true);
        ModelingExercise modelingExercise = ModelFactory.generateModelingExerciseForExam(DiagramType.ClassDiagram, exerciseGroup);
        modelingExercise.setCourse(exerciseGroup.getExam().getCourse());
        request.postWithResponseBody("/api/modeling-exercises/", modelingExercise, ModelingExercise.class, HttpStatus.BAD_REQUEST);
    }

    @Test
    @WithMockUser(value = "instructor1", roles = "INSTRUCTOR")
    public void createModelingExercise_setNeitherCourseAndExerciseGroup_badRequest() throws Exception {
        ModelingExercise modelingExercise = ModelFactory.generateModelingExerciseForExam(DiagramType.ClassDiagram, null);
        request.postWithResponseBody("/api/modeling-exercises/", modelingExercise, ModelingExercise.class, HttpStatus.BAD_REQUEST);
    }

    @Test
    @WithMockUser(value = "instructor2", roles = "INSTRUCTOR")
    public void testInstructorGetsOnlyResultsFromOwningCourses() throws Exception {
        final var search = database.configureSearch("");
        final var result = request.get("/api/modeling-exercises/", HttpStatus.OK, SearchResultPageDTO.class, database.exerciseSearchMapping(search));

        assertThat(result.getResultsOnPage()).isEmpty();
    }

    @Test
    @WithMockUser(value = "instructor1", roles = "INSTRUCTOR")
    public void testInstructorGetsResultsFromOwningCoursesNotEmpty() throws Exception {
        database.addCourseWithOneModelingExercise();
        database.addCourseWithOneModelingExercise("Activity Diagram");
        final var searchClassDiagram = database.configureSearch("ClassDiagram");
        final var resultClassDiagram = request.get("/api/modeling-exercises/", HttpStatus.OK, SearchResultPageDTO.class, database.exerciseSearchMapping(searchClassDiagram));
        assertThat(resultClassDiagram.getResultsOnPage().size()).isEqualTo(2);

        final var searchActivityDiagram = database.configureSearch("Activity Diagram");
        final var resultActivityDiagram = request.get("/api/modeling-exercises/", HttpStatus.OK, SearchResultPageDTO.class, database.exerciseSearchMapping(searchActivityDiagram));
        assertThat(resultActivityDiagram.getResultsOnPage().size()).isEqualTo(1);

    }

    @Test
    @WithMockUser(value = "admin", roles = "ADMIN")
    public void testAdminGetsResultsFromAllCourses() throws Exception {
        database.addCourseInOtherInstructionGroupAndExercise("ClassDiagram");

        final var search = database.configureSearch("ClassDiagram");
        final var result = request.get("/api/modeling-exercises/", HttpStatus.OK, SearchResultPageDTO.class, database.exerciseSearchMapping(search));
        assertThat(result.getResultsOnPage().size()).isEqualTo(2);
    }

}
