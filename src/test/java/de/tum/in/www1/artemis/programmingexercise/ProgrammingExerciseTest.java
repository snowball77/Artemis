package de.tum.in.www1.artemis.programmingexercise;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import de.tum.in.www1.artemis.AbstractSpringIntegrationBambooBitbucketJiraTest;
import de.tum.in.www1.artemis.domain.ProgrammingExercise;
import de.tum.in.www1.artemis.repository.ProgrammingExerciseRepository;
import de.tum.in.www1.artemis.util.DatabaseUtilService;
import de.tum.in.www1.artemis.util.RequestUtilService;
import de.tum.in.www1.artemis.web.rest.ProgrammingExerciseResource;

class ProgrammingExerciseTest extends AbstractSpringIntegrationBambooBitbucketJiraTest {

    @Autowired
    DatabaseUtilService database;

    @Autowired
    RequestUtilService request;

    @Autowired
    ProgrammingExerciseRepository programmingExerciseRepository;

    Long programmingExerciseId;

    @BeforeEach
    void init() {
        database.addUsers(2, 2, 2);
        database.addCourseWithOneProgrammingExercise();
        programmingExerciseId = programmingExerciseRepository.findAll().get(0).getId();
    }

    @AfterEach
    void tearDown() {
        database.resetDatabase();
    }

    void updateProgrammingExercise(ProgrammingExercise programmingExercise, String newProblem, String newTitle) throws Exception {
        bambooRequestMockProvider.enableMockingOfRequests();
        bitbucketRequestMockProvider.enableMockingOfRequests();
        programmingExercise.setProblemStatement(newProblem);
        programmingExercise.setTitle(newTitle);

        bambooRequestMockProvider.mockBuildPlanIsValid(programmingExercise.getTemplateBuildPlanId(), true);
        bambooRequestMockProvider.mockBuildPlanIsValid(programmingExercise.getSolutionBuildPlanId(), true);
        bitbucketRequestMockProvider.mockRepositoryUrlIsValid(programmingExercise.getTemplateRepositoryUrlAsUrl(), programmingExercise.getProjectKey(), true);
        bitbucketRequestMockProvider.mockRepositoryUrlIsValid(programmingExercise.getSolutionRepositoryUrlAsUrl(), programmingExercise.getProjectKey(), true);

        ProgrammingExercise updatedProgrammingExercise = request.putWithResponseBody("/api/programming-exercises", programmingExercise, ProgrammingExercise.class, HttpStatus.OK);

        // The result from the put response should be updated with the new data.
        assertThat(updatedProgrammingExercise.getProblemStatement()).isEqualTo(newProblem);
        assertThat(updatedProgrammingExercise.getTitle()).isEqualTo(newTitle);

        // There should still be only 1 programming exercise.
        assertThat(programmingExerciseRepository.count()).isEqualTo(1);
        // The programming exercise in the db should also be updated.
        ProgrammingExercise programmingExerciseFromDb = programmingExerciseRepository.findWithTemplateParticipationAndSolutionParticipationById(programmingExercise.getId()).get();
        assertThat(programmingExerciseFromDb.getProblemStatement()).isEqualTo(newProblem);
        assertThat(programmingExerciseFromDb.getTitle()).isEqualTo(newTitle);
    }

    @Test
    @WithMockUser(value = "instructor1", roles = "INSTRUCTOR")
    void updateProgrammingExerciseOnce() throws Exception {
        ProgrammingExercise programmingExercise = programmingExerciseRepository.findWithTemplateParticipationAndSolutionParticipationById(programmingExerciseId).get();
        updateProgrammingExercise(programmingExercise, "new problem 1", "new title 1");
    }

    @Test
    @WithMockUser(value = "instructor1", roles = "INSTRUCTOR")
    void updateProgrammingExerciseTwice() throws Exception {
        ProgrammingExercise programmingExercise = programmingExerciseRepository.findWithTemplateParticipationAndSolutionParticipationById(programmingExerciseId).get();
        updateProgrammingExercise(programmingExercise, "new problem 1", "new title 1");
        updateProgrammingExercise(programmingExercise, "new problem 2", "new title 2");
    }

    @Test
    @WithMockUser(value = "instructor1", roles = "INSTRUCTOR")
    void updateProblemStatement() throws Exception {
        final var newProblem = "a new problem statement";
        final var endpoint = "/api" + ProgrammingExerciseResource.Endpoints.PROBLEM.replace("{exerciseId}", String.valueOf(programmingExerciseId));
        ProgrammingExercise updatedProgrammingExercise = request.patchWithResponseBody(endpoint, newProblem, ProgrammingExercise.class, HttpStatus.OK, MediaType.TEXT_PLAIN);

        assertThat(updatedProgrammingExercise.getProblemStatement()).isEqualTo(newProblem);

        ProgrammingExercise fromDb = programmingExerciseRepository.findWithTemplateParticipationAndSolutionParticipationById(programmingExerciseId).get();
        assertThat(fromDb.getProblemStatement()).isEqualTo(newProblem);
    }

}
