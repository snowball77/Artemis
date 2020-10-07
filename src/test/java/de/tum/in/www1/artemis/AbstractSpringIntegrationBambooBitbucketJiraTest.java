package de.tum.in.www1.artemis;

import static de.tum.in.www1.artemis.config.Constants.*;
import static de.tum.in.www1.artemis.domain.enumeration.BuildPlanType.SOLUTION;
import static de.tum.in.www1.artemis.domain.enumeration.BuildPlanType.TEMPLATE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.test.context.ActiveProfiles;

import com.appfire.common.cli.CliClient;
import com.atlassian.bamboo.specs.util.BambooServer;

import de.tum.in.www1.artemis.connector.bamboo.BambooRequestMockProvider;
import de.tum.in.www1.artemis.connector.bitbucket.BitbucketRequestMockProvider;
import de.tum.in.www1.artemis.domain.ProgrammingExercise;
import de.tum.in.www1.artemis.domain.User;
import de.tum.in.www1.artemis.domain.enumeration.RepositoryType;
import de.tum.in.www1.artemis.service.*;
import de.tum.in.www1.artemis.service.connectors.GitService;
import de.tum.in.www1.artemis.service.connectors.LtiService;
import de.tum.in.www1.artemis.service.connectors.bamboo.BambooService;
import de.tum.in.www1.artemis.service.connectors.bitbucket.BitbucketService;
import de.tum.in.www1.artemis.service.ldap.LdapUserService;
import de.tum.in.www1.artemis.service.messaging.InstanceMessageSendService;
import de.tum.in.www1.artemis.service.scheduled.ProgrammingExerciseScheduleService;
import de.tum.in.www1.artemis.util.GitUtilService;
import de.tum.in.www1.artemis.util.LocalRepository;
import de.tum.in.www1.artemis.util.Verifiable;

@SpringBootTest(properties = { "artemis.athene.token-validity-in-seconds=10800",
        "artemis.athene.base64-secret=YWVuaXF1YWRpNWNlaXJpNmFlbTZkb283dXphaVF1b29oM3J1MWNoYWlyNHRoZWUzb2huZ2FpM211bGVlM0VpcAo=" })
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
// NOTE: we use a common set of active profiles to reduce the number of application launches during testing. This significantly saves time and memory!
@ActiveProfiles({ "artemis", "bamboo", "bitbucket", "jira", "automaticText", "ldap", "scheduling", "athene" })
public abstract class AbstractSpringIntegrationBambooBitbucketJiraTest {

    @Value("${server.url}")
    protected String ARTEMIS_SERVER_URL;

    @SpyBean
    protected LdapUserService ldapUserService;

    // NOTE: we prefer SpyBean over MockBean, because it is more lightweight, we can mock method, but we can also invoke actual methods during testing
    @SpyBean
    protected LtiService ltiService;

    // please only use this to verify method calls using Mockito. Do not mock methods, instead mock the communication with Bamboo using the corresponding RestTemplate.
    @SpyBean
    protected BambooService continuousIntegrationService;

    // please only use this to verify method calls using Mockito. Do not mock methods, instead mock the communication with Bitbucket using the corresponding RestTemplate.
    @SpyBean
    protected BitbucketService versionControlService;

    @SpyBean
    protected BambooServer bambooServer;

    @SpyBean
    protected GitService gitService;

    @SpyBean
    protected GroupNotificationService groupNotificationService;

    @SpyBean
    protected WebsocketMessagingService websocketMessagingService;

    @SpyBean
    protected PlantUmlService plantUmlService;

    @SpyBean
    protected SimpMessageSendingOperations messagingTemplate;

    @SpyBean
    protected ProgrammingSubmissionService programmingSubmissionService;

    @SpyBean
    protected ExamAccessService examAccessService;

    @SpyBean
    protected InstanceMessageSendService instanceMessageSendService;

    @SpyBean
    protected ProgrammingExerciseScheduleService programmingExerciseScheduleService;

    @SpyBean
    protected ProgrammingExerciseParticipationService programmingExerciseParticipationServiceSpy;

    @Autowired
    protected BambooRequestMockProvider bambooRequestMockProvider;

    @Autowired
    protected BitbucketRequestMockProvider bitbucketRequestMockProvider;

    @AfterEach
    public void resetSpyBeans() {
        Mockito.reset(ltiService, continuousIntegrationService, versionControlService, bambooServer, gitService, groupNotificationService, websocketMessagingService,
                plantUmlService, messagingTemplate, programmingSubmissionService, examAccessService, instanceMessageSendService, programmingExerciseScheduleService,
                programmingExerciseParticipationServiceSpy);
    }

    protected List<Verifiable> mockConnectorRequestsForStartParticipation(ProgrammingExercise exercise, String username, Set<User> users) throws Exception {
        final var verifications = new LinkedList<Verifiable>();
        bitbucketRequestMockProvider.mockCopyRepositoryForParticipation(exercise, username);
        bitbucketRequestMockProvider.mockConfigureRepository(exercise, username, users);
        verifications.addAll(bambooRequestMockProvider.mockCopyBuildPlanForParticipation(exercise, username));
        verifications.addAll(bambooRequestMockProvider.mockUpdatePlanRepositoryForParticipation(exercise, username));
        bitbucketRequestMockProvider.mockAddWebHooks(exercise);
        return verifications;
    }

    protected void mockConnectorRequestsForSetup(ProgrammingExercise exercise) throws IOException, URISyntaxException {
        final var projectKey = exercise.getProjectKey();
        String exerciseRepoName = projectKey.toLowerCase() + "-" + RepositoryType.TEMPLATE.getName();
        String testRepoName = projectKey.toLowerCase() + "-" + RepositoryType.TESTS.getName();
        String solutionRepoName = projectKey.toLowerCase() + "-" + RepositoryType.SOLUTION.getName();
        bambooRequestMockProvider.mockCheckIfProjectExists(exercise, false);
        bitbucketRequestMockProvider.mockCheckIfProjectExists(exercise, false);
        bitbucketRequestMockProvider.mockCreateProjectForExercise(exercise);
        bitbucketRequestMockProvider.mockCreateRepository(exercise, exerciseRepoName);
        bitbucketRequestMockProvider.mockCreateRepository(exercise, testRepoName);
        bitbucketRequestMockProvider.mockCreateRepository(exercise, solutionRepoName);
        bitbucketRequestMockProvider.mockAddWebHooks(exercise);
        bambooRequestMockProvider.mockRemoveAllDefaultProjectPermissions(exercise);
        bambooRequestMockProvider.mockGiveProjectPermissions(exercise);

        // TODO: check the actual plan and plan permissions that get passed here
        doReturn(null).when(bambooServer).publish(any());
    }

    protected List<Verifiable> mockConnectorRequestsForImport(ProgrammingExercise sourceExercise, ProgrammingExercise exerciseToBeImported)
            throws IOException, URISyntaxException, CliClient.RemoteRestException, CliClient.ClientException {
        final var verifications = new ArrayList<Verifiable>();
        final var projectKey = exerciseToBeImported.getProjectKey();
        final var sourceProjectKey = sourceExercise.getProjectKey();
        final var templateRepoName = (projectKey + "-" + RepositoryType.TEMPLATE.getName()).toLowerCase();
        final var solutionRepoName = (projectKey + "-" + RepositoryType.SOLUTION.getName()).toLowerCase();
        final var testsRepoName = (projectKey + "-" + RepositoryType.TESTS.getName()).toLowerCase();
        var nextParticipationId = sourceExercise.getTemplateParticipation().getId() + 1;
        final var artemisSolutionHookPath = ARTEMIS_SERVER_URL + PROGRAMMING_SUBMISSION_RESOURCE_API_PATH + nextParticipationId++;
        final var artemisTemplateHookPath = ARTEMIS_SERVER_URL + PROGRAMMING_SUBMISSION_RESOURCE_API_PATH + nextParticipationId++;
        final var artemisTestsHookPath = ARTEMIS_SERVER_URL + TEST_CASE_CHANGED_API_PATH + (sourceExercise.getId() + 1);

        verifications.add(bambooRequestMockProvider.mockCopyBuildPlan(sourceExercise.getProjectKey(), TEMPLATE.getName(), projectKey, TEMPLATE.getName()));
        verifications.add(bambooRequestMockProvider.mockCopyBuildPlan(sourceExercise.getProjectKey(), SOLUTION.getName(), projectKey, SOLUTION.getName()));
        doReturn(null).when(bambooServer).publish(any());
        verifications.add(bambooRequestMockProvider.mockEnablePlan(projectKey, TEMPLATE.getName()));
        verifications.add(bambooRequestMockProvider.mockEnablePlan(projectKey, SOLUTION.getName()));
        bitbucketRequestMockProvider.mockCheckIfProjectExists(exerciseToBeImported, false);
        bitbucketRequestMockProvider.mockCreateProjectForExercise(exerciseToBeImported);
        bitbucketRequestMockProvider.mockCopyRepository(sourceProjectKey, projectKey, sourceExercise.getTemplateRepositoryName(), templateRepoName);
        bitbucketRequestMockProvider.mockCopyRepository(sourceProjectKey, projectKey, sourceExercise.getSolutionRepositoryName(), solutionRepoName);
        bitbucketRequestMockProvider.mockCopyRepository(sourceProjectKey, projectKey, sourceExercise.getTestRepositoryName(), testsRepoName);
        bitbucketRequestMockProvider.mockGetExistingWebhooks(projectKey, templateRepoName);
        bitbucketRequestMockProvider.mockAddWebhook(projectKey, templateRepoName, artemisTemplateHookPath);
        bitbucketRequestMockProvider.mockGetExistingWebhooks(projectKey, solutionRepoName);
        bitbucketRequestMockProvider.mockAddWebhook(projectKey, solutionRepoName, artemisSolutionHookPath);
        bitbucketRequestMockProvider.mockGetExistingWebhooks(projectKey, testsRepoName);
        bitbucketRequestMockProvider.mockAddWebhook(projectKey, testsRepoName, artemisTestsHookPath);
        bambooRequestMockProvider.mockCheckIfProjectExists(exerciseToBeImported, false);
        bambooRequestMockProvider.mockGiveProjectPermissions(exerciseToBeImported);
        bambooRequestMockProvider.mockUpdatePlanRepository(exerciseToBeImported, TEMPLATE.getName(), ASSIGNMENT_REPO_NAME, templateRepoName, List.of(ASSIGNMENT_REPO_NAME));
        bambooRequestMockProvider.mockUpdatePlanRepository(exerciseToBeImported, TEMPLATE.getName(), TEST_REPO_NAME, testsRepoName, List.of());
        bambooRequestMockProvider.mockUpdatePlanRepository(exerciseToBeImported, SOLUTION.getName(), ASSIGNMENT_REPO_NAME, solutionRepoName, List.of());
        bambooRequestMockProvider.mockUpdatePlanRepository(exerciseToBeImported, SOLUTION.getName(), TEST_REPO_NAME, testsRepoName, List.of());
        bambooRequestMockProvider.mockTriggerBuild(exerciseToBeImported.getProjectKey() + "-" + TEMPLATE.getName());
        bambooRequestMockProvider.mockTriggerBuild(exerciseToBeImported.getProjectKey() + "-" + SOLUTION.getName());
        return verifications;
    }

    /**
     * can be invoked for teams and students
     */
    protected void setupRepositoryMocksParticipant(ProgrammingExercise exercise, String participantName, LocalRepository studentRepo) throws Exception {
        final var projectKey = exercise.getProjectKey();
        String participantRepoName = projectKey.toLowerCase() + "-" + participantName;
        var participantRepoTestUrl = new GitUtilService.MockFileRepositoryUrl(studentRepo.originRepoFile);
        doReturn(participantRepoTestUrl).when(versionControlService).getCloneRepositoryUrl(projectKey, participantRepoName);
        doReturn(gitService.getRepositoryByLocalPath(studentRepo.localRepoFile.toPath())).when(gitService).getOrCheckoutRepository(participantRepoTestUrl.getURL(), true);
        doReturn(participantRepoName).when(continuousIntegrationService).getRepositorySlugFromUrl(participantRepoTestUrl.getURL());
        doReturn(participantRepoName).when(versionControlService).getRepositorySlugFromUrl(participantRepoTestUrl.getURL());
    }

    protected void setupRepositoryMocks(ProgrammingExercise exercise, LocalRepository exerciseRepo, LocalRepository solutionRepo, LocalRepository testRepo) throws Exception {
        final var projectKey = exercise.getProjectKey();

        String exerciseRepoName = projectKey.toLowerCase() + "-" + RepositoryType.TEMPLATE.getName();
        String testRepoName = projectKey.toLowerCase() + "-" + RepositoryType.TESTS.getName();
        String solutionRepoName = projectKey.toLowerCase() + "-" + RepositoryType.SOLUTION.getName();

        var exerciseRepoTestUrl = new GitUtilService.MockFileRepositoryUrl(exerciseRepo.originRepoFile);
        var testRepoTestUrl = new GitUtilService.MockFileRepositoryUrl(testRepo.originRepoFile);
        var solutionRepoTestUrl = new GitUtilService.MockFileRepositoryUrl(solutionRepo.originRepoFile);

        doReturn(exerciseRepoTestUrl).when(versionControlService).getCloneRepositoryUrl(projectKey, exerciseRepoName);
        doReturn(testRepoTestUrl).when(versionControlService).getCloneRepositoryUrl(projectKey, testRepoName);
        doReturn(solutionRepoTestUrl).when(versionControlService).getCloneRepositoryUrl(projectKey, solutionRepoName);

        doReturn(gitService.getRepositoryByLocalPath(exerciseRepo.localRepoFile.toPath())).when(gitService).getOrCheckoutRepository(exerciseRepoTestUrl.getURL(), true);
        doReturn(gitService.getRepositoryByLocalPath(testRepo.localRepoFile.toPath())).when(gitService).getOrCheckoutRepository(testRepoTestUrl.getURL(), true);
        doReturn(gitService.getRepositoryByLocalPath(solutionRepo.localRepoFile.toPath())).when(gitService).getOrCheckoutRepository(solutionRepoTestUrl.getURL(), true);

        doReturn(exerciseRepoName).when(continuousIntegrationService).getRepositorySlugFromUrl(exerciseRepoTestUrl.getURL());
        doReturn(testRepoName).when(continuousIntegrationService).getRepositorySlugFromUrl(testRepoTestUrl.getURL());
        doReturn(solutionRepoName).when(continuousIntegrationService).getRepositorySlugFromUrl(solutionRepoTestUrl.getURL());

        doReturn(exerciseRepoName).when(versionControlService).getRepositorySlugFromUrl(exerciseRepoTestUrl.getURL());
        doReturn(testRepoName).when(versionControlService).getRepositorySlugFromUrl(testRepoTestUrl.getURL());
        doReturn(solutionRepoName).when(versionControlService).getRepositorySlugFromUrl(solutionRepoTestUrl.getURL());

        doReturn(projectKey).when(versionControlService).getProjectKeyFromUrl(any());
    }
}
