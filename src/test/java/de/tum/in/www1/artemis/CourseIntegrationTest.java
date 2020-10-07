package de.tum.in.www1.artemis;

import static de.tum.in.www1.artemis.config.Constants.ARTEMIS_GROUP_DEFAULT_PREFIX;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;

import de.tum.in.www1.artemis.config.Constants;
import de.tum.in.www1.artemis.connector.jira.JiraRequestMockProvider;
import de.tum.in.www1.artemis.domain.Course;
import de.tum.in.www1.artemis.domain.Exercise;
import de.tum.in.www1.artemis.domain.FileUploadExercise;
import de.tum.in.www1.artemis.domain.ProgrammingExercise;
import de.tum.in.www1.artemis.domain.Submission;
import de.tum.in.www1.artemis.domain.TextExercise;
import de.tum.in.www1.artemis.domain.TextSubmission;
import de.tum.in.www1.artemis.domain.User;
import de.tum.in.www1.artemis.domain.enumeration.RepositoryType;
import de.tum.in.www1.artemis.domain.enumeration.TutorParticipationStatus;
import de.tum.in.www1.artemis.domain.leaderboard.tutor.LeaderboardId;
import de.tum.in.www1.artemis.domain.leaderboard.tutor.TutorLeaderboardAnsweredMoreFeedbackRequestsView;
import de.tum.in.www1.artemis.domain.leaderboard.tutor.TutorLeaderboardAssessmentView;
import de.tum.in.www1.artemis.domain.leaderboard.tutor.TutorLeaderboardComplaintResponsesView;
import de.tum.in.www1.artemis.domain.leaderboard.tutor.TutorLeaderboardComplaintsView;
import de.tum.in.www1.artemis.domain.leaderboard.tutor.TutorLeaderboardMoreFeedbackRequestsView;
import de.tum.in.www1.artemis.domain.modeling.ModelingExercise;
import de.tum.in.www1.artemis.domain.modeling.ModelingSubmission;
import de.tum.in.www1.artemis.domain.participation.Participation;
import de.tum.in.www1.artemis.domain.participation.TutorParticipation;
import de.tum.in.www1.artemis.repository.CourseRepository;
import de.tum.in.www1.artemis.repository.CustomAuditEventRepository;
import de.tum.in.www1.artemis.repository.ExamRepository;
import de.tum.in.www1.artemis.repository.ExerciseRepository;
import de.tum.in.www1.artemis.repository.LectureRepository;
import de.tum.in.www1.artemis.repository.NotificationRepository;
import de.tum.in.www1.artemis.repository.ParticipationRepository;
import de.tum.in.www1.artemis.repository.ResultRepository;
import de.tum.in.www1.artemis.repository.SubmissionRepository;
import de.tum.in.www1.artemis.repository.TutorLeaderboardAnsweredMoreFeedbackRequestsViewRepository;
import de.tum.in.www1.artemis.repository.TutorLeaderboardAssessmentViewRepository;
import de.tum.in.www1.artemis.repository.TutorLeaderboardComplaintResponsesViewRepository;
import de.tum.in.www1.artemis.repository.TutorLeaderboardComplaintsViewRepository;
import de.tum.in.www1.artemis.repository.TutorLeaderboardMoreFeedbackRequestsViewRepository;
import de.tum.in.www1.artemis.repository.UserRepository;
import de.tum.in.www1.artemis.service.UserService;
import de.tum.in.www1.artemis.util.DatabaseUtilService;
import de.tum.in.www1.artemis.util.ModelFactory;
import de.tum.in.www1.artemis.util.RequestUtilService;
import de.tum.in.www1.artemis.web.rest.dto.StatsForInstructorDashboardDTO;

public class CourseIntegrationTest extends AbstractSpringIntegrationBambooBitbucketJiraTest {

    @Autowired
    DatabaseUtilService database;

    @Autowired
    RequestUtilService request;

    @Autowired
    CourseRepository courseRepo;

    @Autowired
    ExerciseRepository exerciseRepo;

    @Autowired
    LectureRepository lectureRepo;

    @Autowired
    ParticipationRepository participationRepo;

    @Autowired
    SubmissionRepository submissionRepo;

    @Autowired
    ResultRepository resultRepo;

    @Autowired
    CustomAuditEventRepository auditEventRepo;

    @Autowired
    JiraRequestMockProvider jiraRequestMockProvider;

    @Autowired
    UserRepository userRepo;

    @Autowired
    UserService userService;

    @Autowired
    NotificationRepository notificationRepo;

    @Autowired
    TutorLeaderboardAssessmentViewRepository tutorLeaderboardAssessmentViewRepo;

    @Autowired
    TutorLeaderboardComplaintsViewRepository tutorLeaderboardComplaintsViewRepo;

    @Autowired
    TutorLeaderboardComplaintResponsesViewRepository tutorLeaderboardComplaintResponsesViewRepo;

    @Autowired
    TutorLeaderboardMoreFeedbackRequestsViewRepository tutorLeaderboardMoreFeedbackRequestsViewRepo;

    @Autowired
    TutorLeaderboardAnsweredMoreFeedbackRequestsViewRepository tutorLeaderboardAnsweredMoreFeedbackRequestsViewRepo;

    @Autowired
    ExamRepository examRepo;

    private final int numberOfStudents = 4;

    private final int numberOfTutors = 5;

    private final int numberOfInstructors = 1;

    @BeforeEach
    public void initTestCase() {
        database.addUsers(numberOfStudents, numberOfTutors, numberOfInstructors);

        // Add users that are not in the course
        userRepo.save(ModelFactory.generateActivatedUser("tutor6"));
        userRepo.save(ModelFactory.generateActivatedUser("instructor2"));
    }

    @AfterEach
    public void resetDatabase() {
        database.resetDatabase();
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    public void testCreateCourseWithPermission() throws Exception {
        Course course = ModelFactory.generateCourse(null, null, null, new HashSet<>());
        jiraRequestMockProvider.enableMockingOfRequests();
        jiraRequestMockProvider.mockCreateGroup(course.getDefaultStudentGroupName());
        jiraRequestMockProvider.mockCreateGroup(course.getDefaultTeachingAssistantGroupName());
        jiraRequestMockProvider.mockCreateGroup(course.getDefaultInstructorGroupName());
        request.post("/api/courses", course, HttpStatus.CREATED);
        List<Course> repoContent = courseRepo.findAll();
        assertThat(repoContent.size()).as("Course got stored").isEqualTo(1);

        course = ModelFactory.generateCourse(1L, null, null, new HashSet<>());
        request.post("/api/courses", course, HttpStatus.BAD_REQUEST);
        assertThat(courseRepo.findAll()).as("Course has not been stored").contains(repoContent.toArray(new Course[0]));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    public void testCreateCourseWithSameShortName() throws Exception {
        Course course1 = ModelFactory.generateCourse(null, null, null, new HashSet<>());
        course1.setShortName("shortName");
        jiraRequestMockProvider.enableMockingOfRequests();
        jiraRequestMockProvider.mockCreateGroup(course1.getDefaultStudentGroupName());
        jiraRequestMockProvider.mockCreateGroup(course1.getDefaultTeachingAssistantGroupName());
        jiraRequestMockProvider.mockCreateGroup(course1.getDefaultInstructorGroupName());
        request.post("/api/courses", course1, HttpStatus.CREATED);
        assertThat(courseRepo.findAll().size()).as("Course got stored").isEqualTo(1);

        Course course2 = ModelFactory.generateCourse(null, null, null, new HashSet<>(), "tumuser", "tutor", "instructor");
        course2.setShortName("shortName");
        request.post("/api/courses", course2, HttpStatus.BAD_REQUEST);
        assertThat(courseRepo.findAll().size()).as("Course has not been stored").isEqualTo(1);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    public void testCreateCourseWithNegativeMaxComplainNumber() throws Exception {
        Course course = ModelFactory.generateCourse(null, null, null, new HashSet<>());
        jiraRequestMockProvider.enableMockingOfRequests();
        jiraRequestMockProvider.mockCreateGroup(course.getDefaultStudentGroupName());
        jiraRequestMockProvider.mockCreateGroup(course.getDefaultTeachingAssistantGroupName());
        jiraRequestMockProvider.mockCreateGroup(course.getDefaultInstructorGroupName());
        course.setMaxComplaints(-1);
        request.post("/api/courses", course, HttpStatus.BAD_REQUEST);
        List<Course> repoContent = courseRepo.findAll();
        assertThat(repoContent.size()).as("Course has not been stored").isEqualTo(0);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    public void testCreateCourseWithNegativeMaxComplainTimeDays() throws Exception {
        Course course = ModelFactory.generateCourse(null, null, null, new HashSet<>());
        jiraRequestMockProvider.enableMockingOfRequests();
        jiraRequestMockProvider.mockCreateGroup(course.getDefaultStudentGroupName());
        jiraRequestMockProvider.mockCreateGroup(course.getDefaultTeachingAssistantGroupName());
        jiraRequestMockProvider.mockCreateGroup(course.getDefaultInstructorGroupName());
        course.setMaxComplaintTimeDays(-1);
        request.post("/api/courses", course, HttpStatus.BAD_REQUEST);
        List<Course> repoContent = courseRepo.findAll();
        assertThat(repoContent.size()).as("Course has not been stored").isEqualTo(0);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    public void testCreateCourseWithNegativeMaxTeamComplainNumber() throws Exception {
        Course course = ModelFactory.generateCourse(null, null, null, new HashSet<>());
        jiraRequestMockProvider.enableMockingOfRequests();
        jiraRequestMockProvider.mockCreateGroup(course.getDefaultStudentGroupName());
        jiraRequestMockProvider.mockCreateGroup(course.getDefaultTeachingAssistantGroupName());
        jiraRequestMockProvider.mockCreateGroup(course.getDefaultInstructorGroupName());
        course.setMaxTeamComplaints(-1);
        request.post("/api/courses", course, HttpStatus.BAD_REQUEST);
        List<Course> repoContent = courseRepo.findAll();
        assertThat(repoContent.size()).as("Course has not been stored").isEqualTo(0);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    public void testCreateCourseWithModifiedMaxComplainTimeDaysAndMaxComplains() throws Exception {
        Course course = ModelFactory.generateCourse(null, null, null, new HashSet<>());
        jiraRequestMockProvider.enableMockingOfRequests();
        jiraRequestMockProvider.mockCreateGroup(course.getDefaultStudentGroupName());
        jiraRequestMockProvider.mockCreateGroup(course.getDefaultTeachingAssistantGroupName());
        jiraRequestMockProvider.mockCreateGroup(course.getDefaultInstructorGroupName());
        course.setMaxComplaintTimeDays(0);
        course.setMaxComplaints(1);
        request.post("/api/courses", course, HttpStatus.BAD_REQUEST);
        List<Course> repoContent = courseRepo.findAll();
        assertThat(repoContent.size()).as("Course has not been stored").isEqualTo(0);

        // change configuration
        course.setMaxComplaintTimeDays(1);
        course.setMaxComplaints(0);
        course.setMaxTeamComplaints(0);
        request.post("/api/courses", course, HttpStatus.BAD_REQUEST);
        repoContent = courseRepo.findAll();
        assertThat(repoContent.size()).as("Course has not been stored").isEqualTo(0);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    public void testCreateCourseWithCustomNonExistingGroupNames() throws Exception {
        Course course = ModelFactory.generateCourse(null, null, null, new HashSet<>());
        course.setStudentGroupName("StudentGroupName");
        course.setTeachingAssistantGroupName("TeachingAssistantGroupName");
        course.setInstructorGroupName("InstructorGroupName");
        request.post("/api/courses", course, HttpStatus.CREATED);
        List<Course> repoContent = courseRepo.findAll();
        assertThat(repoContent.size()).as("Course got stored").isEqualTo(1);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    public void testCreateCourseWithOptions() throws Exception {
        // Generate POST Request Body with maxComplaints = 5, maxComplaintTimeDays = 14, studentQuestionsEnabled = false
        Course course = ModelFactory.generateCourse(null, null, null, new HashSet<>(), null, null, null, 5, 5, 14, false);
        jiraRequestMockProvider.enableMockingOfRequests();
        jiraRequestMockProvider.mockCreateGroup(course.getDefaultStudentGroupName());
        jiraRequestMockProvider.mockCreateGroup(course.getDefaultTeachingAssistantGroupName());
        jiraRequestMockProvider.mockCreateGroup(course.getDefaultInstructorGroupName());
        course = request.postWithResponseBody("/api/courses", course, Course.class, HttpStatus.CREATED);
        // Because the courseId is automatically generated we cannot use the findById method to retrieve the saved course.
        Course getFromRepo = courseRepo.findAll().get(0);
        assertThat(getFromRepo.getMaxComplaints()).as("Course has right maxComplaints Value").isEqualTo(5);
        assertThat(getFromRepo.getMaxComplaintTimeDays()).as("Course has right maxComplaintTimeDays Value").isEqualTo(14);
        assertThat(getFromRepo.getStudentQuestionsEnabled()).as("Course has right studentQuestionsEnabled Value").isFalse();

        // Test edit course
        course.setId(getFromRepo.getId());
        course.setMaxComplaints(1);
        course.setMaxComplaintTimeDays(7);
        course.setStudentQuestionsEnabled(true);
        Course updatedCourse = request.putWithResponseBody("/api/courses", course, Course.class, HttpStatus.OK);
        assertThat(updatedCourse.getMaxComplaints()).as("maxComplaints Value updated successfully").isEqualTo(course.getMaxComplaints());
        assertThat(updatedCourse.getMaxComplaintTimeDays()).as("maxComplaintTimeDays Value updated successfully").isEqualTo(course.getMaxComplaintTimeDays());
        assertThat(updatedCourse.getStudentQuestionsEnabled()).as("studentQuestionsEnabled Value updated successfully").isTrue();
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    public void testDeleteCourseWithPermission() throws Exception {
        jiraRequestMockProvider.enableMockingOfRequests();
        bambooRequestMockProvider.enableMockingOfRequests(true);
        bitbucketRequestMockProvider.enableMockingOfRequests(true);
        // add to new list so that we can add another course with ARTEMIS_GROUP_DEFAULT_PREFIX so that delete group will be tested properly
        List<Course> courses = new ArrayList<>(database.createCoursesWithExercisesAndLectures(true));
        Course course3 = ModelFactory.generateCourse(null, ZonedDateTime.now().minusDays(8), ZonedDateTime.now().minusDays(4), new HashSet<>(), null, null, null);
        course3.setStudentGroupName(course3.getDefaultStudentGroupName());
        course3.setTeachingAssistantGroupName(course3.getDefaultTeachingAssistantGroupName());
        course3.setInstructorGroupName(course3.getDefaultInstructorGroupName());
        course3 = courseRepo.save(course3);
        courses.add(course3);
        database.addExamWithExerciseGroup(courses.get(0), true);
        // mock certain requests to JIRA Bitbucket and Bamboo
        for (Course course : courses) {
            if (course.getStudentGroupName().startsWith(ARTEMIS_GROUP_DEFAULT_PREFIX)) {
                jiraRequestMockProvider.mockDeleteGroup(course.getStudentGroupName());
            }
            if (course.getTeachingAssistantGroupName().startsWith(ARTEMIS_GROUP_DEFAULT_PREFIX)) {
                jiraRequestMockProvider.mockDeleteGroup(course.getTeachingAssistantGroupName());
            }
            if (course.getInstructorGroupName().startsWith(ARTEMIS_GROUP_DEFAULT_PREFIX)) {
                jiraRequestMockProvider.mockDeleteGroup(course.getInstructorGroupName());
            }
            for (Exercise exercise : course.getExercises()) {
                if (exercise instanceof ProgrammingExercise) {
                    final String projectKey = ((ProgrammingExercise) exercise).getProjectKey();
                    bambooRequestMockProvider.mockDeleteProject(projectKey);
                    bitbucketRequestMockProvider.mockDeleteRepository(projectKey, (projectKey + "-" + RepositoryType.TEMPLATE.getName()).toLowerCase());
                    bitbucketRequestMockProvider.mockDeleteRepository(projectKey, (projectKey + "-" + RepositoryType.SOLUTION.getName()).toLowerCase());
                    bitbucketRequestMockProvider.mockDeleteRepository(projectKey, (projectKey + "-" + RepositoryType.TESTS.getName()).toLowerCase());
                    bitbucketRequestMockProvider.mockDeleteProject(projectKey);
                }
            }
        }

        for (Course course : courses) {
            if (!course.getExercises().isEmpty()) {
                groupNotificationService.notifyStudentGroupAboutExerciseUpdate(course.getExercises().iterator().next(), "notify");
            }
            request.delete("/api/courses/" + course.getId(), HttpStatus.OK);
        }
        assertThat(courseRepo.findAll()).as("All courses deleted").hasSize(0);
        assertThat(notificationRepo.findAll()).as("All notifications are deleted").isEmpty();
        assertThat(examRepo.findAll()).as("All exams are deleted").isEmpty();
        assertThat(exerciseRepo.findAll()).as("All Exercises are deleted").isEmpty();
        assertThat(lectureRepo.findAll()).as("All Lectures are deleted").isEmpty();
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    public void testDeleteNotExistingCourse() throws Exception {
        request.delete("/api/courses/1", HttpStatus.NOT_FOUND);
    }

    @Test
    @WithMockUser(username = "student1", roles = "USER")
    public void testCreateCourseWithoutPermission() throws Exception {
        Course course = ModelFactory.generateCourse(null, null, null, new HashSet<>());
        request.post("/api/courses", course, HttpStatus.FORBIDDEN);
        assertThat(courseRepo.findAll().size()).as("Course got stored").isEqualTo(0);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    public void testCreateCourseWithWrongShortName() throws Exception {
        Course course = ModelFactory.generateCourse(null, null, null, new HashSet<>());
        course.setShortName("`badName~");
        request.post("/api/courses", course, HttpStatus.BAD_REQUEST);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    public void testUpdateCourseWithWrongShortName() throws Exception {
        Course course = ModelFactory.generateCourse(null, null, null, new HashSet<>());
        course.setShortName("`badName~");
        courseRepo.save(course);
        request.put("/api/courses", course, HttpStatus.BAD_REQUEST);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    public void testUpdateCourseWithoutId() throws Exception {
        Course course = ModelFactory.generateCourse(null, null, null, new HashSet<>());
        jiraRequestMockProvider.enableMockingOfRequests();
        jiraRequestMockProvider.mockCreateGroup(course.getDefaultStudentGroupName());
        jiraRequestMockProvider.mockCreateGroup(course.getDefaultTeachingAssistantGroupName());
        jiraRequestMockProvider.mockCreateGroup(course.getDefaultInstructorGroupName());
        request.put("/api/courses", course, HttpStatus.CREATED);
        List<Course> repoContent = courseRepo.findAll();
        assertThat(repoContent.size()).as("Course got stored").isEqualTo(1);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    public void testUpdateCourseIsEmpty() throws Exception {
        Course course = ModelFactory.generateCourse(1L, null, null, new HashSet<>());
        request.put("/api/courses", course, HttpStatus.NOT_FOUND);
    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    public void testEditCourseWithPermission() throws Exception {

        Course course = ModelFactory.generateCourse(1L, null, null, new HashSet<>(), "tumuser", "tutor", "instructor");
        course = courseRepo.save(course);

        course.setTitle("Test Course");
        course.setStartDate(ZonedDateTime.now().minusDays(5));
        course.setEndDate(ZonedDateTime.now().plusDays(5));
        Course updatedCourse = request.putWithResponseBody("/api/courses", course, Course.class, HttpStatus.OK);
        assertThat(updatedCourse.getShortName()).as("short name was changed correctly").isEqualTo(course.getShortName());
        assertThat(updatedCourse.getTitle()).as("title was changed correctly").isEqualTo(course.getTitle());
        assertThat(updatedCourse.getStartDate()).as("start date was changed correctly").isEqualTo(course.getStartDate());
        assertThat(updatedCourse.getEndDate()).as("end date was changed correctly").isEqualTo(course.getEndDate());
    }

    @Test
    @WithMockUser(username = "student1", roles = "USER")
    public void testGetCourseWithoutPermission() throws Exception {
        request.getList("/api/courses", HttpStatus.FORBIDDEN, Course.class);
    }

    @Test
    @WithMockUser(username = "tutor6", roles = "TA")
    public void testGetCourse_tutorNotInCourse() throws Exception {
        var courses = database.createCoursesWithExercisesAndLectures(true);
        request.getList("/api/courses/" + courses.get(0).getId(), HttpStatus.FORBIDDEN, Course.class);
        request.get("/api/courses/" + courses.get(0).getId() + "/with-exercises", HttpStatus.FORBIDDEN, Course.class);
    }

    @Test
    @WithMockUser(username = "instructor2", roles = "INSTRUCTOR")
    public void testGetCourseWithExercisesAndRelevantParticipationsWithoutPermissions() throws Exception {
        var courses = database.createCoursesWithExercisesAndLectures(true);
        request.get("/api/courses/" + courses.get(0).getId() + "/with-exercises-and-relevant-participations", HttpStatus.FORBIDDEN, Course.class);
    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    public void testGetCoursesWithPermission() throws Exception {
        database.createCoursesWithExercisesAndLectures(true);
        List<Course> courses = request.getList("/api/courses", HttpStatus.OK, Course.class);
        assertThat(courses.size()).as("All courses are available").isEqualTo(2);
        for (Exercise exercise : courses.get(0).getExercises()) {
            assertThat(exercise.getGradingInstructions()).as("Grading instructions are not filtered out").isNotNull();
            assertThat(exercise.getProblemStatement()).as("Problem statements are not filtered out").isNotNull();
        }
    }

    @Test
    @WithMockUser(username = "student1", roles = "USER")
    public void testGetCourseForDashboard() throws Exception {
        List<Course> courses = database.createCoursesWithExercisesAndLectures(true);
        Course receivedCourse = request.get("/api/courses/" + courses.get(0).getId() + "/for-dashboard", HttpStatus.OK, Course.class);

        // Test that the received course has five exercises
        assertThat(receivedCourse.getExercises().size()).as("Five exercises are returned").isEqualTo(5);
        // Test that the received course has two lectures
        assertThat(receivedCourse.getLectures().size()).as("Two lectures are returned").isEqualTo(2);

        // Iterate over all exercises of the remaining course
        for (Exercise exercise : courses.get(0).getExercises()) {
            // Test that the exercise does not have more than one participation.
            assertThat(exercise.getStudentParticipations().size()).as("At most one participation for exercise").isLessThanOrEqualTo(1);
            if (exercise.getStudentParticipations().size() > 0) {
                // Buffer participation so that null checking is easier.
                Participation participation = exercise.getStudentParticipations().iterator().next();
                if (participation.getSubmissions().size() > 0) {
                    // The call filters participations by submissions and their result. After the call each participation shouldn't have more than one submission.
                    assertThat(participation.getSubmissions().size()).as("At most one submission for participation").isLessThanOrEqualTo(1);
                    Submission submission = participation.getSubmissions().iterator().next();
                    if (submission != null) {
                        // Test that the correct text submission was filtered.
                        if (submission instanceof TextSubmission) {
                            TextSubmission textSubmission = (TextSubmission) submission;
                            assertThat(textSubmission.getText()).as("Correct text submission").isEqualTo("text");
                        }
                        // Test that the correct modeling submission was filtered.
                        if (submission instanceof ModelingSubmission) {
                            ModelingSubmission modelingSubmission = (ModelingSubmission) submission;
                            assertThat(modelingSubmission.getModel()).as("Correct modeling submission").isEqualTo("model1");
                        }
                    }
                }
            }
        }
    }

    @Test
    @WithMockUser(username = "student1", roles = "USER")
    public void testGetAllCoursesForDashboard() throws Exception {
        database.createCoursesWithExercisesAndLectures(true);

        // Perform the request that is being tested here
        List<Course> courses = request.getList("/api/courses/for-dashboard", HttpStatus.OK, Course.class);

        // Test that the prepared inactive course was filtered out
        assertThat(courses.size()).as("Inactive course was filtered out").isEqualTo(1);

        // Test that the remaining course has five exercises
        assertThat(courses.get(0).getExercises().size()).as("Five exercises are returned").isEqualTo(5);

        // Iterate over all exercises of the remaining course
        for (Exercise exercise : courses.get(0).getExercises()) {
            // Test that the exercise does not have more than one participation.
            assertThat(exercise.getStudentParticipations().size()).as("At most one participation for exercise").isLessThanOrEqualTo(1);
            if (exercise.getStudentParticipations().size() > 0) {
                // Buffer participation so that null checking is easier.
                Participation participation = exercise.getStudentParticipations().iterator().next();
                if (participation.getSubmissions().size() > 0) {
                    // The call filters participations by submissions and their result. After the call each participation shouldn't have more than one submission.
                    assertThat(participation.getSubmissions().size()).as("At most one submission for participation").isLessThanOrEqualTo(1);
                    Submission submission = participation.getSubmissions().iterator().next();
                    if (submission != null) {
                        // Test that the correct text submission was filtered.
                        if (submission instanceof TextSubmission) {
                            TextSubmission textSubmission = (TextSubmission) submission;
                            assertThat(textSubmission.getText()).as("Correct text submission").isEqualTo("text");
                        }
                        // Test that the correct modeling submission was filtered.
                        if (submission instanceof ModelingSubmission) {
                            ModelingSubmission modelingSubmission = (ModelingSubmission) submission;
                            assertThat(modelingSubmission.getModel()).as("Correct modeling submission").isEqualTo("model1");
                        }
                    }
                }
            }
        }
    }

    @Test
    @WithMockUser(username = "tutor1", roles = "TA")
    public void testGetCoursesWithoutActiveExercises() throws Exception {
        Course course = ModelFactory.generateCourse(1L, null, null, new HashSet<>(), "tumuser", "tutor", "instructor");
        courseRepo.save(course);
        List<Course> courses = request.getList("/api/courses/for-dashboard", HttpStatus.OK, Course.class);
        assertThat(courses.size()).as("Only one course is returned").isEqualTo(1);
        assertThat(courses.stream().findFirst().get().getExercises().size()).as("Course doesn't have any exercises").isEqualTo(0);
    }

    @Test
    @WithMockUser(username = "tutor1", roles = "TA")
    public void testGetCoursesAccurateTimezoneEvaluation() throws Exception {
        Course courseActive = ModelFactory.generateCourse(1L, ZonedDateTime.now().minusMinutes(25), ZonedDateTime.now().plusMinutes(25), new HashSet<>(), "tumuser", "tutor",
                "instructor");
        Course courseNotActivePast = ModelFactory.generateCourse(2L, ZonedDateTime.now().minusDays(5), ZonedDateTime.now().minusMinutes(25), new HashSet<>(), "tumuser", "tutor",
                "instructor");
        Course courseNotActiveFuture = ModelFactory.generateCourse(3L, ZonedDateTime.now().plusMinutes(25), ZonedDateTime.now().plusDays(5), new HashSet<>(), "tumuser", "tutor",
                "instructor");
        courseRepo.save(courseActive);
        courseRepo.save(courseNotActivePast);
        courseRepo.save(courseNotActiveFuture);
        List<Course> courses = request.getList("/api/courses/for-dashboard", HttpStatus.OK, Course.class);
        assertThat(courses.size()).as("Exactly one course is returned").isEqualTo(1);
        courses.get(0).setId(courseActive.getId());
        assertThat(courses.get(0)).as("Active course is returned").isEqualTo(courseActive);
    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    public void testGetAllCoursesWithUserStats() throws Exception {
        List<Course> testCourses = database.createCoursesWithExercisesAndLectures(true);
        List<Course> receivedCourse = request.getList("/api/courses/with-user-stats", HttpStatus.OK, Course.class);
        assertThat(testCourses).isEqualTo(receivedCourse);
        assertThat(receivedCourse.get(0).getNumberOfStudents()).isEqualTo(numberOfStudents);
        assertThat(receivedCourse.get(0).getNumberOfTeachingAssistants()).isEqualTo(numberOfTutors);
        assertThat(receivedCourse.get(0).getNumberOfInstructors()).isEqualTo(numberOfInstructors);
    }

    @Test
    @WithMockUser(username = "student1")
    public void testGetCoursesToRegisterAndAccurateTimeZoneEvaluation() throws Exception {
        Course courseActiveRegistrationEnabled = ModelFactory.generateCourse(1L, ZonedDateTime.now().minusMinutes(25), ZonedDateTime.now().plusMinutes(25), new HashSet<>(),
                "tumuser", "tutor", "instructor");
        courseActiveRegistrationEnabled.setRegistrationEnabled(true);
        Course courseActiveRegistrationDisabled = ModelFactory.generateCourse(2L, ZonedDateTime.now().minusMinutes(25), ZonedDateTime.now().plusMinutes(25), new HashSet<>(),
                "tumuser", "tutor", "instructor");
        courseActiveRegistrationDisabled.setRegistrationEnabled(false);
        Course courseNotActivePast = ModelFactory.generateCourse(3L, ZonedDateTime.now().minusDays(5), ZonedDateTime.now().minusMinutes(25), new HashSet<>(), "tumuser", "tutor",
                "instructor");
        Course courseNotActiveFuture = ModelFactory.generateCourse(4L, ZonedDateTime.now().plusMinutes(25), ZonedDateTime.now().plusDays(5), new HashSet<>(), "tumuser", "tutor",
                "instructor");
        courseRepo.save(courseActiveRegistrationEnabled);
        courseRepo.save(courseActiveRegistrationDisabled);
        courseRepo.save(courseNotActivePast);
        courseRepo.save(courseNotActiveFuture);

        List<Course> courses = request.getList("/api/courses/to-register", HttpStatus.OK, Course.class);
        assertThat(courses.size()).as("Exactly one course is available to register").isEqualTo(1);
        courses.get(0).setId(courseActiveRegistrationEnabled.getId());
        assertThat(courses.get(0)).as("Only active course is returned").isEqualTo(courseActiveRegistrationEnabled);
    }

    private void getCourseForDashboardWithStats(boolean isInstructor) throws Exception {
        List<Course> testCourses = database.createCoursesWithExercisesAndLectures(true);
        for (Course testCourse : testCourses) {
            Course course = request.get("/api/courses/" + testCourse.getId() + "/for-tutor-dashboard", HttpStatus.OK, Course.class);
            for (Exercise exercise : course.getExercises()) {
                assertThat(exercise.getNumberOfAssessments().getInTime()).as("Number of in-time assessments is correct").isZero();
                assertThat(exercise.getNumberOfAssessments().getLate()).as("Number of late assessments is correct").isZero();
                assertThat(exercise.getTutorParticipations().size()).as("Tutor participation was created").isEqualTo(1);
                // Mock data contains exactly two submissions for the modeling exercise
                if (exercise instanceof ModelingExercise) {
                    assertThat(exercise.getNumberOfSubmissions().getInTime()).as("Number of in-time submissions is correct").isEqualTo(2);
                }
                // Mock data contains exactly one submission for the text exercise
                if (exercise instanceof TextExercise) {
                    assertThat(exercise.getNumberOfSubmissions().getInTime()).as("Number of in-time submissions is correct").isEqualTo(1);
                }
                // Mock data contains no submissions for the file upload and programming exercise
                if (exercise instanceof FileUploadExercise || exercise instanceof ProgrammingExercise) {
                    assertThat(exercise.getNumberOfSubmissions().getInTime()).as("Number of in-time submissions is correct").isEqualTo(0);
                }

                assertThat(exercise.getNumberOfSubmissions().getLate()).as("Number of late submissions is correct").isEqualTo(0);

                // Check tutor participation
                if (exercise.getTutorParticipations().size() > 0) {
                    TutorParticipation tutorParticipation = exercise.getTutorParticipations().iterator().next();
                    assertThat(tutorParticipation.getStatus()).as("Tutor participation status is correctly initialized").isEqualTo(TutorParticipationStatus.NOT_PARTICIPATED);
                }
            }

            StatsForInstructorDashboardDTO stats = request.get("/api/courses/" + testCourse.getId() + "/stats-for-tutor-dashboard", HttpStatus.OK,
                    StatsForInstructorDashboardDTO.class);
            long numberOfInTimeSubmissions = course.getId().equals(testCourses.get(0).getId()) ? 3 : 0; // course 1 has 3 submissions, course 2 has 0 submissions
            assertThat(stats.getNumberOfSubmissions().getInTime()).as("Number of in-time submissions is correct").isEqualTo(numberOfInTimeSubmissions);
            assertThat(stats.getNumberOfSubmissions().getLate()).as("Number of latte submissions is correct").isEqualTo(0);
            assertThat(stats.getNumberOfAssessments().getInTime()).as("Number of in-time assessments is correct").isEqualTo(0);
            assertThat(stats.getNumberOfAssessments().getLate()).as("Number of late assessments is correct").isEqualTo(0);
            assertThat(stats.getTutorLeaderboardEntries().size()).as("Number of tutor leaderboard entries is correct").isEqualTo(5);

            StatsForInstructorDashboardDTO stats2 = request.get("/api/courses/" + testCourse.getId() + "/stats-for-instructor-dashboard",
                    isInstructor ? HttpStatus.OK : HttpStatus.FORBIDDEN, StatsForInstructorDashboardDTO.class);

            if (!isInstructor) {
                assertThat(stats2).as("Stats for instructor are not available to tutor").isNull();
            }
            else {
                assertThat(stats2).as("Stats are available for instructor").isNotNull();
                assertThat(stats2.getNumberOfSubmissions()).as("Submission stats for instructor are correct.").usingRecursiveComparison().isEqualTo(stats.getNumberOfSubmissions());
                assertThat(stats2.getNumberOfAssessments()).as("Assessment stats for instructor are correct.").usingRecursiveComparison().isEqualTo(stats.getNumberOfAssessments());
            }
        }
    }

    @Test
    @WithMockUser(username = "tutor1", roles = "TA")
    public void testGetCourseForTutorDashboardWithStats() throws Exception {
        getCourseForDashboardWithStats(false);
    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    public void testGetCourseForInstructorDashboardWithStats() throws Exception {
        getCourseForDashboardWithStats(true);
    }

    @Test
    @WithMockUser(username = "instructor2", roles = "INSTRUCTOR")
    public void testGetCourseForInstructorDashboardWithStats_instructorNotInCourse() throws Exception {
        List<Course> testCourses = database.createCoursesWithExercisesAndLectures(true);
        request.get("/api/courses/" + testCourses.get(0).getId() + "/for-tutor-dashboard", HttpStatus.FORBIDDEN, Course.class);
        request.get("/api/courses/" + testCourses.get(0).getId() + "/stats-for-instructor-dashboard", HttpStatus.FORBIDDEN, StatsForInstructorDashboardDTO.class);
    }

    @Test
    @WithMockUser(username = "tutor6", roles = "TA")
    public void testGetCourseForTutorDashboardWithStats_tutorNotInCourse() throws Exception {
        List<Course> testCourses = database.createCoursesWithExercisesAndLectures(true);
        request.get("/api/courses/" + testCourses.get(0).getId() + "/for-tutor-dashboard", HttpStatus.FORBIDDEN, Course.class);
        request.get("/api/courses/" + testCourses.get(0).getId() + "/stats-for-tutor-dashboard", HttpStatus.FORBIDDEN, StatsForInstructorDashboardDTO.class);
    }

    @Test
    @WithMockUser(username = "tutor1", roles = "TA")
    public void testGetTutorDashboardStats_withComplaints() throws Exception {
        getTutorDashboardsStatsWithComplaints(true);
    }

    @Test
    @WithMockUser(username = "tutor1", roles = "TA")
    public void testGetTutorDashboardStats_withComplaints_withoutPoints() throws Exception {
        getTutorDashboardsStatsWithComplaints(false);
    }

    private void getTutorDashboardsStatsWithComplaints(boolean withPoints) throws Exception {
        Course testCourse = database.addCourseWithOneReleasedTextExercise();
        var points = withPoints ? 15L : null;
        var leaderboardId = new LeaderboardId(database.getUserByLogin("tutor1").getId(), testCourse.getExercises().iterator().next().getId());
        tutorLeaderboardComplaintsViewRepo.save(new TutorLeaderboardComplaintsView(leaderboardId, 3L, 1L, points, testCourse.getId(), ""));
        tutorLeaderboardComplaintResponsesViewRepo.save(new TutorLeaderboardComplaintResponsesView(leaderboardId, 1L, points, testCourse.getId(), ""));
        tutorLeaderboardAnsweredMoreFeedbackRequestsViewRepo.save(new TutorLeaderboardAnsweredMoreFeedbackRequestsView(leaderboardId, 1L, points, testCourse.getId(), ""));
        tutorLeaderboardMoreFeedbackRequestsViewRepo.save(new TutorLeaderboardMoreFeedbackRequestsView(leaderboardId, 3L, 1L, points, testCourse.getId(), ""));
        tutorLeaderboardAssessmentViewRepo.save(new TutorLeaderboardAssessmentView(leaderboardId, 2L, points, testCourse.getId(), ""));

        StatsForInstructorDashboardDTO stats = request.get("/api/courses/" + testCourse.getId() + "/stats-for-tutor-dashboard", HttpStatus.OK,
                StatsForInstructorDashboardDTO.class);
        var currentTutorLeaderboard = stats.getTutorLeaderboardEntries().get(0);
        assertThat(currentTutorLeaderboard.getNumberOfTutorComplaints()).isEqualTo(3);
        assertThat(currentTutorLeaderboard.getNumberOfAcceptedComplaints()).isEqualTo(1);
        assertThat(currentTutorLeaderboard.getNumberOfComplaintResponses()).isEqualTo(1);
        assertThat(currentTutorLeaderboard.getNumberOfAnsweredMoreFeedbackRequests()).isEqualTo(1);
        assertThat(currentTutorLeaderboard.getNumberOfNotAnsweredMoreFeedbackRequests()).isEqualTo(1);
        assertThat(currentTutorLeaderboard.getNumberOfTutorMoreFeedbackRequests()).isEqualTo(3);
        assertThat(currentTutorLeaderboard.getNumberOfAssessments()).isEqualTo(2);
        if (withPoints) {
            assertThat(currentTutorLeaderboard.getPoints()).isEqualTo(0);
        }
        else {
            assertThat(currentTutorLeaderboard.getPoints()).isEqualTo(1);
        }
    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    public void testGetCourse() throws Exception {
        List<Course> testCourses = database.createCoursesWithExercisesAndLectures(true);
        for (Course testCourse : testCourses) {
            Course courseWithExercisesAndRelevantParticipations = request.get("/api/courses/" + testCourse.getId() + "/with-exercises-and-relevant-participations", HttpStatus.OK,
                    Course.class);
            Course courseWithExercises = request.get("/api/courses/" + testCourse.getId() + "/with-exercises", HttpStatus.OK, Course.class);
            Course courseOnly = request.get("/api/courses/" + testCourse.getId(), HttpStatus.OK, Course.class);

            // Check course properties on courseOnly
            assertThat(courseOnly.getStudentGroupName()).as("Student group name is correct").isEqualTo("tumuser");
            assertThat(courseOnly.getTeachingAssistantGroupName()).as("Teaching assistant group name is correct").isEqualTo("tutor");
            assertThat(courseOnly.getInstructorGroupName()).as("Instructor group name is correct").isEqualTo("instructor");
            assertThat(courseOnly.getEndDate()).as("End date is after start date").isAfter(courseOnly.getStartDate());
            assertThat(courseOnly.getMaxComplaints()).as("Max complaints is correct").isEqualTo(3);
            assertThat(courseOnly.getPresentationScore()).as("Presentation score is correct").isEqualTo(2);
            assertThat(courseOnly.getExercises().size()).as("Course without exercises contains no exercises").isZero();

            // Assert that course properties on courseWithExercises and courseWithExercisesAndRelevantParticipations match those of courseOnly
            String[] ignoringFields = { "exercises", "tutorGroups", "lectures", "exams", "fileService" };
            assertThat(courseWithExercises).as("courseWithExercises same as courseOnly").usingRecursiveComparison().ignoringFields(ignoringFields).isEqualTo(courseOnly);
            assertThat(courseWithExercisesAndRelevantParticipations).as("courseWithExercisesAndRelevantParticipations same as courseOnly").usingRecursiveComparison()
                    .ignoringFields(ignoringFields).isEqualTo(courseOnly);

            // Verify presence of exercises in mock courses
            // - Course 1 has 5 exercises in total, 4 exercises with relevant participations
            // - Course 2 has 0 exercises in total, 0 exercises with relevant participations
            boolean isFirstCourse = courseOnly.getId().equals(testCourses.get(0).getId());
            long numberOfExercises = isFirstCourse ? 5 : 0;
            long numberOfInterestingExercises = isFirstCourse ? 4 : 0;
            assertThat(courseWithExercises.getExercises().size()).as("Course contains correct number of exercises").isEqualTo(numberOfExercises);
            assertThat(courseWithExercisesAndRelevantParticipations.getExercises().size()).as("Course contains correct number of exercises")
                    .isEqualTo(numberOfInterestingExercises);
        }
    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    public void testGetCategoriesInCourse() throws Exception {
        List<Course> testCourses = database.createCoursesWithExercisesAndLectures(true);
        Course course1 = testCourses.get(0);
        Course course2 = testCourses.get(1);
        Set<String> categories1 = request.get("/api/courses/" + course1.getId() + "/categories", HttpStatus.OK, Set.class);
        assertThat(categories1).as("Correct categories in course1").containsExactlyInAnyOrder("Category", "Modeling", "Quiz", "File", "Text", "Programming");
        Set<String> categories2 = request.get("/api/courses/" + course2.getId() + "/categories", HttpStatus.OK, Set.class);
        assertThat(categories2).as("No categories in course2").isEmpty();
    }

    @Test
    @WithMockUser(username = "instructor2", roles = "INSTRUCTOR")
    public void testGetCategoriesInCourse_instructorNotInCourse() throws Exception {
        List<Course> testCourses = database.createCoursesWithExercisesAndLectures(true);
        request.get("/api/courses/" + testCourses.get(0).getId() + "/categories", HttpStatus.FORBIDDEN, Set.class);
    }

    @Test
    @WithMockUser(username = "ab123cd")
    public void testRegisterForCourse() throws Exception {
        jiraRequestMockProvider.enableMockingOfRequests();
        User student = ModelFactory.generateActivatedUser("ab123cd");
        userRepo.save(student);

        ZonedDateTime pastTimestamp = ZonedDateTime.now().minusDays(5);
        ZonedDateTime futureTimestamp = ZonedDateTime.now().plusDays(5);
        Course course1 = ModelFactory.generateCourse(null, pastTimestamp, futureTimestamp, new HashSet<>(), "testcourse1", "tutor", "instructor");
        Course course2 = ModelFactory.generateCourse(null, pastTimestamp, futureTimestamp, new HashSet<>(), "testcourse2", "tutor", "instructor");
        course1.setRegistrationEnabled(true);

        course1 = courseRepo.save(course1);
        course2 = courseRepo.save(course2);
        jiraRequestMockProvider.mockAddUserToGroupForMultipleGroups(Set.of(course1.getStudentGroupName()));
        jiraRequestMockProvider.mockAddUserToGroupForMultipleGroups(Set.of(course2.getStudentGroupName()));

        User updatedStudent = request.postWithResponseBody("/api/courses/" + course1.getId() + "/register", null, User.class, HttpStatus.OK);
        assertThat(updatedStudent.getGroups()).as("User is registered for course").contains(course1.getStudentGroupName());

        List<AuditEvent> auditEvents = auditEventRepo.find("ab123cd", Instant.now().minusSeconds(20), Constants.REGISTER_FOR_COURSE);
        assertThat(auditEvents).as("Audit Event for course registration added").hasSize(1);
        AuditEvent auditEvent = auditEvents.get(0);
        assertThat(auditEvent.getData().get("course")).as("Correct Event Data").isEqualTo(course1.getTitle());

        request.postWithResponseBody("/api/courses/" + course2.getId() + "/register", null, User.class, HttpStatus.BAD_REQUEST);
    }

    @Test
    @WithMockUser(username = "ab123cd")
    public void testRegisterForCourse_notMeetsDate() throws Exception {
        jiraRequestMockProvider.enableMockingOfRequests();
        User student = ModelFactory.generateActivatedUser("ab123cd");
        userRepo.save(student);

        ZonedDateTime pastTimestamp = ZonedDateTime.now().minusDays(5);
        ZonedDateTime futureTimestamp = ZonedDateTime.now().plusDays(5);
        Course notYetStartedCourse = ModelFactory.generateCourse(null, futureTimestamp, futureTimestamp, new HashSet<>(), "testcourse1", "tutor", "instructor");
        Course finishedCourse = ModelFactory.generateCourse(null, pastTimestamp, pastTimestamp, new HashSet<>(), "testcourse2", "tutor", "instructor");
        notYetStartedCourse.setRegistrationEnabled(true);

        notYetStartedCourse = courseRepo.save(notYetStartedCourse);
        finishedCourse = courseRepo.save(finishedCourse);
        jiraRequestMockProvider.mockAddUserToGroupForMultipleGroups(Set.of(notYetStartedCourse.getStudentGroupName()));
        jiraRequestMockProvider.mockAddUserToGroupForMultipleGroups(Set.of(finishedCourse.getStudentGroupName()));

        request.post("/api/courses/" + notYetStartedCourse.getId() + "/register", User.class, HttpStatus.BAD_REQUEST);
        request.post("/api/courses/" + finishedCourse.getId() + "/register", User.class, HttpStatus.BAD_REQUEST);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    public void testUpdateCourse_withExternalUserManagement_vcsUserManagementHasNotBeenCalled() throws Exception {
        var course = ModelFactory.generateCourse(1L, null, null, new HashSet<>(), "tumuser", "tutor", "instructor");
        course = courseRepo.save(course);

        request.put("/api/courses", course, HttpStatus.OK);

        verifyNoInteractions(versionControlService);
    }

    @Test
    @WithMockUser(username = "instructor2", roles = "INSTRUCTOR")
    public void testUpdateCourse_instructorNotInCourse() throws Exception {
        var course = ModelFactory.generateCourse(1L, null, null, new HashSet<>(), "tumuser", "tutor", "instructor");
        course = courseRepo.save(course);

        request.put("/api/courses", course, HttpStatus.FORBIDDEN);
    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    public void testGetAllStudentsOrTutorsOrInstructorsInCourse() throws Exception {
        Course course = ModelFactory.generateCourse(null, null, null, new HashSet<>(), "tumuser", "tutor", "instructor");
        course = courseRepo.save(course);

        // Get all students for course
        List<User> students = request.getList("/api/courses/" + course.getId() + "/students", HttpStatus.OK, User.class);
        assertThat(students).as("All students in course found").hasSize(numberOfStudents);

        // Get all tutors for course
        List<User> tutors = request.getList("/api/courses/" + course.getId() + "/tutors", HttpStatus.OK, User.class);
        assertThat(tutors).as("All tutors in course found").hasSize(numberOfTutors);

        // Get all instructors for course
        List<User> instructors = request.getList("/api/courses/" + course.getId() + "/instructors", HttpStatus.OK, User.class);
        assertThat(instructors).as("All instructors in course found").hasSize(numberOfInstructors);
    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    public void testGetAllStudentsOrTutorsOrInstructorsInCourse_AsInstructorOfOtherCourse_forbidden() throws Exception {
        Course course = ModelFactory.generateCourse(null, null, null, new HashSet<>(), "other-tumuser", "other-tutor", "other-instructor");
        course = courseRepo.save(course);
        testGetAllStudentsOrTutorsOrInstructorsInCourse__forbidden(course);
    }

    @Test
    @WithMockUser(username = "tutor1", roles = "TA")
    public void testGetAllStudentsOrTutorsOrInstructorsInCourse_AsTutor_forbidden() throws Exception {
        Course course = ModelFactory.generateCourse(null, null, null, new HashSet<>(), "tumuser", "tutor", "instructor");
        course = courseRepo.save(course);
        testGetAllStudentsOrTutorsOrInstructorsInCourse__forbidden(course);
    }

    private void testGetAllStudentsOrTutorsOrInstructorsInCourse__forbidden(Course course) throws Exception {
        request.getList("/api/courses/" + course.getId() + "/students", HttpStatus.FORBIDDEN, User.class);
        request.getList("/api/courses/" + course.getId() + "/tutors", HttpStatus.FORBIDDEN, User.class);
        request.getList("/api/courses/" + course.getId() + "/instructors", HttpStatus.FORBIDDEN, User.class);
    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    public void testAddStudentOrTutorOrInstructorToCourse() throws Exception {
        Course course = ModelFactory.generateCourse(null, null, null, new HashSet<>(), "tumuser", "tutor", "instructor");
        course = courseRepo.save(course);
        testAddStudentOrTutorOrInstructorToCourse(course, HttpStatus.OK);

        // TODO check that the roles have changed accordingly
    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    public void testAddStudentOrTutorOrInstructorToCourse_AsInstructorOfOtherCourse_forbidden() throws Exception {
        Course course = ModelFactory.generateCourse(null, null, null, new HashSet<>(), "other-tumuser", "other-tutor", "other-instructor");
        course = courseRepo.save(course);
        testAddStudentOrTutorOrInstructorToCourse(course, HttpStatus.FORBIDDEN);
    }

    @Test
    @WithMockUser(username = "tutor1", roles = "TA")
    public void testAddStudentOrTutorOrInstructorToCourse_AsTutor_forbidden() throws Exception {
        Course course = ModelFactory.generateCourse(null, null, null, new HashSet<>(), "tumuser", "tutor", "instructor");
        course = courseRepo.save(course);
        testAddStudentOrTutorOrInstructorToCourse(course, HttpStatus.FORBIDDEN);
    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    public void testAddStudentOrTutorOrInstructorToCourse_WithNonExistingUser() throws Exception {
        Course course = ModelFactory.generateCourse(null, null, null, new HashSet<>(), "tumuser", "tutor", "instructor");
        course = courseRepo.save(course);
        jiraRequestMockProvider.enableMockingOfRequests();
        jiraRequestMockProvider.mockAddUserToGroupForMultipleGroups(Set.of(course.getStudentGroupName()));
        jiraRequestMockProvider.mockAddUserToGroupForMultipleGroups(Set.of(course.getTeachingAssistantGroupName()));
        jiraRequestMockProvider.mockAddUserToGroupForMultipleGroups(Set.of(course.getInstructorGroupName()));

        request.postWithoutLocation("/api/courses/" + course.getId() + "/students/maxMustermann", null, HttpStatus.NOT_FOUND, null);
        request.postWithoutLocation("/api/courses/" + course.getId() + "/tutors/maxMustermann", null, HttpStatus.NOT_FOUND, null);
        request.postWithoutLocation("/api/courses/" + course.getId() + "/instructors/maxMustermann", null, HttpStatus.NOT_FOUND, null);
    }

    private void testAddStudentOrTutorOrInstructorToCourse(Course course, HttpStatus httpStatus) throws Exception {
        jiraRequestMockProvider.enableMockingOfRequests();
        jiraRequestMockProvider.mockAddUserToGroupForMultipleGroups(Set.of(course.getStudentGroupName()));
        jiraRequestMockProvider.mockAddUserToGroupForMultipleGroups(Set.of(course.getTeachingAssistantGroupName()));
        jiraRequestMockProvider.mockAddUserToGroupForMultipleGroups(Set.of(course.getInstructorGroupName()));

        request.postWithoutLocation("/api/courses/" + course.getId() + "/students/student1", null, httpStatus, null);
        request.postWithoutLocation("/api/courses/" + course.getId() + "/tutors/tutor1", null, httpStatus, null);
        request.postWithoutLocation("/api/courses/" + course.getId() + "/instructors/instructor1", null, httpStatus, null);
    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    public void testRemoveStudentOrTutorOrInstructorFromCourse() throws Exception {
        Course course = ModelFactory.generateCourse(null, null, null, new HashSet<>(), "tumuser", "tutor", "instructor");
        course = courseRepo.save(course);
        testRemoveStudentOrTutorOrInstructorFromCourse_forbidden(course, HttpStatus.OK);
        // TODO check that the roles have changed accordingly
    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    public void testRemoveStudentOrTutorOrInstructorFromCourse_WithNonExistingUser() throws Exception {
        Course course = ModelFactory.generateCourse(null, null, null, new HashSet<>(), "tumuser", "tutor", "instructor");
        course = courseRepo.save(course);
        request.delete("/api/courses/" + course.getId() + "/students/maxMustermann", HttpStatus.NOT_FOUND);
        request.delete("/api/courses/" + course.getId() + "/tutors/maxMustermann", HttpStatus.NOT_FOUND);
        request.delete("/api/courses/" + course.getId() + "/instructors/maxMustermann", HttpStatus.NOT_FOUND);
    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    public void testRemoveStudentOrTutorOrInstructorFromCourse_AsInstructorOfOtherCourse_forbidden() throws Exception {
        Course course = ModelFactory.generateCourse(null, null, null, new HashSet<>(), "other-tumuser", "other-tutor", "other-instructor");
        course = courseRepo.save(course);
        testRemoveStudentOrTutorOrInstructorFromCourse_forbidden(course, HttpStatus.FORBIDDEN);
    }

    @Test
    @WithMockUser(username = "tutor1", roles = "TA")
    public void testRemoveStudentOrTutorOrInstructorFromCourse_AsTutor_forbidden() throws Exception {
        Course course = ModelFactory.generateCourse(null, null, null, new HashSet<>(), "tumuser", "tutor", "instructor");
        course = courseRepo.save(course);
        testRemoveStudentOrTutorOrInstructorFromCourse_forbidden(course, HttpStatus.FORBIDDEN);
    }

    private void testRemoveStudentOrTutorOrInstructorFromCourse_forbidden(Course course, HttpStatus httpStatus) throws Exception {
        // Retrieve users from whom to remove groups
        User student = userRepo.findOneWithGroupsByLogin("student1").get();
        User tutor = userRepo.findOneWithGroupsByLogin("tutor1").get();
        User instructor = userRepo.findOneWithGroupsByLogin("instructor1").get();
        // Mock remove requests
        jiraRequestMockProvider.enableMockingOfRequests();
        jiraRequestMockProvider.mockRemoveUserFromGroup(Set.of(course.getStudentGroupName()), student.getLogin());
        jiraRequestMockProvider.mockRemoveUserFromGroup(Set.of(course.getTeachingAssistantGroupName()), tutor.getLogin());
        jiraRequestMockProvider.mockRemoveUserFromGroup(Set.of(course.getInstructorGroupName()), instructor.getLogin());
        // Remove users from their group
        request.delete("/api/courses/" + course.getId() + "/students/" + student.getLogin(), httpStatus);
        request.delete("/api/courses/" + course.getId() + "/tutors/" + tutor.getLogin(), httpStatus);
        request.delete("/api/courses/" + course.getId() + "/instructors/" + instructor.getLogin(), httpStatus);
    }

    @Test
    @WithMockUser(username = "tutor1", roles = "TA")
    public void testGetLockedSubmissionsForCourseAsTutor() throws Exception {
        Course course = database.addCourseWithDifferentModelingExercises();
        ModelingExercise classExercise = database.findModelingExerciseWithTitle(course.getExercises(), "ClassDiagram");

        List<Submission> lockedSubmissions = request.get("/api/courses/" + course.getId() + "/lockedSubmissions", HttpStatus.OK, List.class);
        assertThat(lockedSubmissions).as("Locked Submissions is not null").isNotNull();
        assertThat(lockedSubmissions).as("Locked Submissions length is 0").hasSize(0);

        String validModel = database.loadFileFromResources("test-data/model-submission/model.54727.json");

        ModelingSubmission submission = ModelFactory.generateModelingSubmission(validModel, true);
        database.addModelingSubmissionWithResultAndAssessor(classExercise, submission, "student1", "tutor1");

        submission = ModelFactory.generateModelingSubmission(validModel, true);
        database.addModelingSubmissionWithResultAndAssessor(classExercise, submission, "student2", "tutor1");

        submission = ModelFactory.generateModelingSubmission(validModel, true);
        database.addModelingSubmissionWithResultAndAssessor(classExercise, submission, "student3", "tutor1");

        lockedSubmissions = request.get("/api/courses/" + course.getId() + "/lockedSubmissions", HttpStatus.OK, List.class);
        assertThat(lockedSubmissions).as("Locked Submissions is not null").isNotNull();
        assertThat(lockedSubmissions).as("Locked Submissions length is 3").hasSize(3);
    }

    @Test
    @WithMockUser(username = "student1", roles = "USER")
    public void testGetLockedSubmissionsForCourseAsStudent() throws Exception {
        List<Submission> lockedSubmissions = request.get("/api/courses/1/lockedSubmissions", HttpStatus.FORBIDDEN, List.class);
        assertThat(lockedSubmissions).as("Locked Submissions is null").isNull();
    }
}
