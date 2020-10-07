package de.tum.in.www1.artemis.service.connectors.bamboo;

import static de.tum.in.www1.artemis.config.Constants.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.http.HttpException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.appfire.bamboo.cli.BambooClient;
import com.appfire.common.cli.CliClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.tum.in.www1.artemis.domain.*;
import de.tum.in.www1.artemis.domain.enumeration.*;
import de.tum.in.www1.artemis.domain.participation.Participation;
import de.tum.in.www1.artemis.domain.participation.ProgrammingExerciseParticipation;
import de.tum.in.www1.artemis.exception.BambooException;
import de.tum.in.www1.artemis.exception.BitbucketException;
import de.tum.in.www1.artemis.repository.ProgrammingSubmissionRepository;
import de.tum.in.www1.artemis.repository.ResultRepository;
import de.tum.in.www1.artemis.service.FeedbackService;
import de.tum.in.www1.artemis.service.connectors.*;
import de.tum.in.www1.artemis.service.connectors.bamboo.dto.BambooBuildResultNotificationDTO;
import de.tum.in.www1.artemis.service.connectors.bamboo.dto.BambooProjectSearchDTO;
import de.tum.in.www1.artemis.service.connectors.bamboo.dto.QueriedBambooBuildResultDTO;
import de.tum.in.www1.artemis.web.rest.util.HeaderUtil;

@Service
@Profile("bamboo")
public class BambooService implements ContinuousIntegrationService {

    private final Logger log = LoggerFactory.getLogger(BambooService.class);

    @Value("${artemis.continuous-integration.url}")
    private URL bambooServerUrl;

    @Value("${artemis.continuous-integration.empty-commit-necessary}")
    private Boolean isEmptyCommitNecessary;

    @Value("${artemis.continuous-integration.user}")
    private String bambooUser;

    @Value("${artemis.continuous-integration.password}")
    private String bambooPassword;

    private final GitService gitService;

    private final ResultRepository resultRepository;

    private final ProgrammingSubmissionRepository programmingSubmissionRepository;

    private final Optional<VersionControlService> versionControlService;

    private final Optional<ContinuousIntegrationUpdateService> continuousIntegrationUpdateService;

    private final BambooBuildPlanService bambooBuildPlanService;

    private final FeedbackService feedbackService;

    private final RestTemplate restTemplate;

    private final BambooClient bambooClient;

    private final ObjectMapper mapper;

    public BambooService(GitService gitService, ResultRepository resultRepository, ProgrammingSubmissionRepository programmingSubmissionRepository,
            Optional<VersionControlService> versionControlService, Optional<ContinuousIntegrationUpdateService> continuousIntegrationUpdateService,
            BambooBuildPlanService bambooBuildPlanService, FeedbackService feedbackService, @Qualifier("bambooRestTemplate") RestTemplate restTemplate, BambooClient bambooClient,
            ObjectMapper mapper) {
        this.gitService = gitService;
        this.resultRepository = resultRepository;
        this.programmingSubmissionRepository = programmingSubmissionRepository;
        this.versionControlService = versionControlService;
        this.continuousIntegrationUpdateService = continuousIntegrationUpdateService;
        this.bambooBuildPlanService = bambooBuildPlanService;
        this.feedbackService = feedbackService;
        this.restTemplate = restTemplate;
        this.bambooClient = bambooClient;
        this.mapper = mapper;
    }

    @Override
    public void createBuildPlanForExercise(ProgrammingExercise programmingExercise, String planKey, URL sourceCodeRepositoryURL, URL testRepositoryURL) {
        bambooBuildPlanService.createBuildPlanForExercise(programmingExercise, planKey, getRepositorySlugFromUrl(sourceCodeRepositoryURL),
                getRepositorySlugFromUrl(testRepositoryURL));
    }

    /**
     * TODO: this method is not ideal here, but should als not be static in some util class. Think about improving the passing of URL arguments with slugs between
     * programming exercise generation and the CI services (BambooService and JenkinsService) who need to handle these
     *
     * Gets the repository slug from the given URL
     *
     * @param repositoryUrl The complete repository-url (including protocol, host and the complete path)
     * @return The repository slug
     */
    public String getRepositorySlugFromUrl(URL repositoryUrl) {
        // https://ga42xab@bitbucket.ase.in.tum.de/scm/EIST2016RME/RMEXERCISE-ga42xab.git
        String[] urlParts = repositoryUrl.getFile().split("/");
        String repositorySlug = urlParts[urlParts.length - 1];
        if (repositorySlug.endsWith(".git")) {
            repositorySlug = repositorySlug.substring(0, repositorySlug.length() - 4);
        }
        else {
            throw new IllegalArgumentException("No repository slug could be found");
        }

        return repositorySlug;
    }

    /**
     * Parse the project key from the repoUrl of the given repositoryUrl.
     *
     * @param repositoryUrl of the repo on the VCS server.
     * @return the project key that was parsed.
     */
    // TODO: this method has moved to BitbucketService, but missed the toUpperCase() there, so we reactivated it here
    private String getProjectKeyFromUrl(URL repositoryUrl) {
        // https://ga42xab@bitbucket.ase.in.tum.de/scm/EIST2016RME/RMEXERCISE-ga42xab.git
        return repositoryUrl.getFile().split("/")[2].toUpperCase();
    }

    @Override
    public void configureBuildPlan(ProgrammingExerciseParticipation participation) {
        String buildPlanId = participation.getBuildPlanId();
        URL repositoryUrl = participation.getRepositoryUrlAsUrl();
        String planProject = getProjectKeyFromBuildPlanId(buildPlanId);
        String planKey = participation.getBuildPlanId();
        updatePlanRepository(planProject, planKey, ASSIGNMENT_REPO_NAME, getProjectKeyFromUrl(repositoryUrl), repositoryUrl.toString(), Optional.empty());
        enablePlan(planProject, planKey);
    }

    @Override
    public void performEmptySetupCommit(ProgrammingExerciseParticipation participation) {
        // Empty commit - Bamboo bug workaround

        if (isEmptyCommitNecessary) {
            try {
                ProgrammingExercise exercise = participation.getProgrammingExercise();
                URL repositoryUrl = participation.getRepositoryUrlAsUrl();
                Repository repo = gitService.getOrCheckoutRepository(repositoryUrl, true);
                // we set user to null to make sure the Artemis user is used to create the setup commit, this is important to filter this commit later in
                // notifyPush in ProgrammingSubmissionService
                gitService.commitAndPush(repo, SETUP_COMMIT_MESSAGE, null);

                if (exercise == null) {
                    log.warn("Cannot access exercise in 'configureBuildPlan' to determine if deleting the repo after cloning make sense. Will decide to delete the repo");
                    gitService.deleteLocalRepository(repo);
                }
                else {
                    // only delete the git repository, if the online editor is NOT allowed
                    // this saves some performance on the server, when the student opens the online editor, because the repo does not need to be cloned again
                    // Note: the null check is necessary, because otherwise we might get a null pointer exception
                    if (exercise.isAllowOnlineEditor() == null || Boolean.FALSE.equals(exercise.isAllowOnlineEditor())) {
                        gitService.deleteLocalRepository(repo);
                    }
                }
            }
            catch (GitAPIException ex) {
                log.error("Git error while doing empty commit", ex);
            }
            catch (IOException ex) {
                log.error("IOError while doing empty commit", ex);
            }
            catch (InterruptedException ex) {
                log.error("InterruptedException while doing empty commit", ex);
            }
            catch (NullPointerException ex) {
                log.error("NullPointerException while doing empty commit", ex);
            }
        }
    }

    @Override
    public void createProjectForExercise(ProgrammingExercise programmingExercise) {
        // Do nothing since Bamboo automatically creates projects
    }

    /**
     * Triggers a build for the build plan in the given participation.
     *
     * @param participation the participation with the id of the build plan that should be triggered.
     */
    @Override
    public void triggerBuild(ProgrammingExerciseParticipation participation) throws HttpException {
        var buildPlan = participation.getBuildPlanId();
        HttpHeaders headers = HeaderUtil.createAuthorization(bambooUser, bambooPassword);
        HttpEntity<?> entity = new HttpEntity<>(headers);
        try {
            restTemplate.exchange(bambooServerUrl + "/rest/api/latest/queue/" + buildPlan, HttpMethod.POST, entity, Map.class);
        }
        catch (RestClientException e) {
            log.error("HttpError while triggering build plan " + buildPlan + " with error: " + e.getMessage());
            throw new HttpException("Communication failed when trying to trigger the Bamboo build plan " + buildPlan + " with the error: " + e.getMessage());
        }
    }

    @Override
    public boolean isBuildPlanEnabled(final String projectKey, final String planId) {
        final var headers = HeaderUtil.createAuthorization(bambooUser, bambooPassword);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        final var entity = new HttpEntity<>(null, headers);
        final var planInfo = restTemplate.exchange(bambooServerUrl + "/rest/api/latest/plan/" + planId, HttpMethod.GET, entity, Map.class, new HashMap<>()).getBody();
        return planInfo != null && planInfo.containsKey("enabled") && ((boolean) planInfo.get("enabled"));
    }

    @Override
    public void deleteBuildPlan(String projectKey, String buildPlanId) {
        deletePlan(buildPlanId);
    }

    /**
     * Delete project with given identifier from CI system.
     *
     * @param projectKey unique identifier for the project on CI system
     */
    @Override
    public void deleteProject(String projectKey) {
        try {
            log.info("Delete project " + projectKey);
            // TODO: use Bamboo REST API: DELETE "/rest/api/latest/project/{projectKey}"
            String message = bambooClient.getProjectHelper().deleteProject(projectKey);
            log.info("Delete project was successful. " + message);
        }
        catch (CliClient.ClientException | CliClient.RemoteRestException e) {
            log.error(e.getMessage());
        }
    }

    /**
     * Get the current status of the build for the given participation, i.e. INACTIVE, QUEUED, or BUILDING.
     *
     * @param participation participation for which to get status
     * @return build status
     */
    @Override
    public BuildStatus getBuildStatus(ProgrammingExerciseParticipation participation) {
        Map<String, Boolean> status = retrieveBuildStatus(participation.getBuildPlanId());
        if (status == null) {
            return BuildStatus.INACTIVE;
        }
        if (status.get("isActive") && !status.get("isBuilding")) {
            return BuildStatus.QUEUED;
        }
        else if (status.get("isActive") && status.get("isBuilding")) {
            return BuildStatus.BUILDING;
        }
        else {
            return BuildStatus.INACTIVE;
        }
    }

    @Override
    public List<BuildLogEntry> getLatestBuildLogs(ProgrammingSubmission programmingSubmission) {
        // Load the logs from the database
        List<BuildLogEntry> buildLogsFromDatabase = retrieveLatestBuildLogsFromDatabase(programmingSubmission);

        // If there are logs present in the database, return them (they were already filtered when inserted)
        if (!buildLogsFromDatabase.isEmpty()) {
            return buildLogsFromDatabase;
        }

        // Otherwise return the logs from Bamboo (and filter them now)
        ProgrammingExerciseParticipation programmingExerciseParticipation = (ProgrammingExerciseParticipation) programmingSubmission.getParticipation();

        var buildLogEntries = filterBuildLogs(retrieveLatestBuildLogsFromBamboo(programmingExerciseParticipation.getBuildPlanId()));

        // Add reference to ProgrammingSubmission
        buildLogEntries.forEach(buildLogEntry -> buildLogEntry.setProgrammingSubmission(programmingSubmission));

        // Set the received logs in order to avoid duplicate entries (this removes existing logs) & save them into the database
        programmingSubmission.setBuildLogEntries(buildLogEntries);
        programmingSubmissionRepository.save(programmingSubmission);

        return buildLogEntries;
    }

    @Override
    public String copyBuildPlan(String sourceProjectKey, String sourcePlanName, String targetProjectKey, String targetProjectName, String targetPlanName) {
        final var cleanPlanName = getCleanPlanName(targetPlanName);
        final var targetPlanKey = targetProjectKey + "-" + cleanPlanName;
        final var sourcePlanKey = sourceProjectKey + "-" + sourcePlanName;
        try {
            log.debug("Clone build plan " + sourcePlanKey + " to " + targetPlanKey);
            // TODO use REST API PUT "/rest/api/latest/clone/{projectKey}-{buildKey}"
            String message = bambooClient.getPlanHelper().clonePlan(sourcePlanKey, targetPlanKey, cleanPlanName, "", targetProjectName, true);
            log.info("Clone build plan " + sourcePlanKey + " was successful: " + message);
        }
        catch (CliClient.ClientException clientException) {
            if (clientException.getMessage().contains("already exists")) {
                log.info("Build Plan " + targetPlanKey + " already exists. Going to recover build plan information...");
                return targetPlanKey;
            }
            else {
                throw new BambooException("Something went wrong while cloning build plan " + sourcePlanKey + " to " + targetPlanKey + ":" + clientException.getMessage(),
                        clientException);
            }
        }
        catch (CliClient.RemoteRestException e) {
            throw new BambooException("Something went wrong while cloning build plan: " + e.getMessage(), e);
        }

        return targetPlanKey;
    }

    @Override
    public void givePlanPermissions(ProgrammingExercise programmingExercise, String planName) {
        bambooBuildPlanService.setBuildPlanPermissionsForExercise(programmingExercise, planName);
    }

    @Override
    public void removeAllDefaultProjectPermissions(String projectKey) {
        final var headers = HeaderUtil.createAuthorization(bambooUser, bambooPassword);
        // Bamboo always gives read rights
        final var permissionData = List.of(permissionToBambooPermission(CIPermission.READ));
        final var entity = new HttpEntity<>(permissionData, headers);
        final var roles = List.of("ANONYMOUS", "LOGGED_IN");

        roles.forEach(role -> {
            final var url = bambooServerUrl + "/rest/api/latest/permissions/project/" + projectKey + "/roles/" + role;
            final var response = restTemplate.exchange(url, HttpMethod.DELETE, entity, String.class);
            if (response.getStatusCode() != HttpStatus.NO_CONTENT && response.getStatusCode() != HttpStatus.NOT_MODIFIED) {
                throw new BambooException("Unable to remove default project permissions from exercise " + projectKey + "\n" + response.getBody());
            }
        });
    }

    @Override
    public void giveProjectPermissions(String projectKey, List<String> groupNames, List<CIPermission> permissions) {
        final var headers = HeaderUtil.createAuthorization(bambooUser, bambooPassword);
        final var permissionData = permissions.stream().map(this::permissionToBambooPermission).collect(Collectors.toList());
        final var entity = new HttpEntity<>(permissionData, headers);

        groupNames.forEach(group -> {
            final var url = bambooServerUrl + "/rest/api/latest/permissions/project/" + projectKey + "/groups/" + group;
            final var response = restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);
            if (response.getStatusCode() != HttpStatus.NO_CONTENT && response.getStatusCode() != HttpStatus.NOT_MODIFIED) {
                final var errorMessage = "Unable to give permissions to project " + projectKey + "; error body: " + response.getBody() + "; headers: " + response.getHeaders()
                        + "; status code: " + response.getStatusCode();
                log.error(errorMessage);
                throw new BambooException(errorMessage);
            }
        });
    }

    private String permissionToBambooPermission(CIPermission permission) {
        return switch (permission) {
            case EDIT -> "WRITE";
            case CREATE -> "CREATE";
            case READ -> "READ";
            case ADMIN -> "ADMINISTRATION";
            default -> throw new IllegalArgumentException("Unable to map Bamboo permission " + permission);
        };
    }

    @Override
    public void enablePlan(String projectKey, String planKey) throws BambooException {
        try {
            log.debug("Enable build plan " + planKey);
            // TODO use REST API PUT "/rest/api/latest/clone/{projectKey}-{buildKey}"
            String message = bambooClient.getPlanHelper().enablePlan(planKey, true);
            log.info("Enable build plan " + planKey + " was successful. " + message);
        }
        catch (CliClient.ClientException | CliClient.RemoteRestException e) {
            log.error(e.getMessage(), e);
            throw new BambooException("Something went wrong while enabling the build plan", e);
        }
    }

    @Override
    public void updatePlanRepository(String bambooProject, String bambooPlan, String bambooRepositoryName, String repoProjectName, String repoUrl,
            Optional<List<String>> triggeredBy) throws BambooException {
        try {
            final var repositoryName = versionControlService.get().getRepositoryName(new URL(repoUrl));
            continuousIntegrationUpdateService.get().updatePlanRepository(bambooProject, bambooPlan, bambooRepositoryName, repoProjectName, repositoryName, triggeredBy);
        }
        catch (MalformedURLException e) {
            throw new BambooException(e.getMessage(), e);
        }
    }

    /**
     * Deletes the given plan.
     *
     * @param planKey to identify the Bamboo plan to delete
     */
    private void deletePlan(String planKey) {
        try {
            log.info("Delete build plan " + planKey);
            // TODO use REST API DELETE "/rest/api/latest/clone/{projectKey}-{buildKey}"
            String message = bambooClient.getPlanHelper().deletePlan(planKey);
            log.info("Delete build plan was successful. " + message);
        }
        catch (CliClient.ClientException | CliClient.RemoteRestException e) {
            log.error(e.getMessage());
        }
    }

    /**
     * Extract the plan key from the Bamboo requestBody.
     *
     * @param requestBody The request Body received from the CI-Server.
     * @return the plan key or null if it can't be found.
     * @throws BambooException is thrown on casting errors.
     */
    @Override
    @SuppressWarnings("unchecked")
    public String getPlanKey(Object requestBody) throws BambooException {
        try {
            Map<String, Object> requestBodyMap = (Map<String, Object>) requestBody;
            Map<String, Object> planMap = (Map<String, Object>) requestBodyMap.get("plan");
            return (String) planMap.get("key");
        }
        catch (Exception e) {
            // TODO: Not sure when this is triggered, the method would return null if the planMap does not have a 'key'.
            log.error("Error when getting plan key");
            throw new BitbucketException("Could not get plan key", e);
        }
    }

    /**
     * React to a new build result from Bamboo, create the result and feedbacks and link the result to the submission and participation.
     *
     * @param participation The participation for which the build finished.
     * @param requestBody   The result notification received from the CI-Server.
     * @return the created result.
     */
    @Override
    public Result onBuildCompleted(ProgrammingExerciseParticipation participation, Object requestBody) {
        final var buildResult = mapper.convertValue(requestBody, BambooBuildResultNotificationDTO.class);
        log.debug("Retrieving build result (NEW) ...");
        try {
            // Filter the first build plan that was automatically executed when the build plan was created.
            if (isFirstBuildForThisPlan(buildResult)) {
                return null;
            }

            List<ProgrammingSubmission> submissions = programmingSubmissionRepository.findByParticipationIdAndResultIsNullOrderBySubmissionDateDesc(participation.getId());
            Optional<ProgrammingSubmission> latestMatchingPendingSubmission = submissions.stream().filter(submission -> {
                String matchingCommitHashInBuildMap = getCommitHash(buildResult, submission.getType());
                return matchingCommitHashInBuildMap != null && matchingCommitHashInBuildMap.equals(submission.getCommitHash());
            }).findFirst();

            Result result = createResultFromBuildResult(buildResult, participation);
            ProgrammingExercise programmingExercise = participation.getProgrammingExercise();
            ProgrammingSubmission programmingSubmission;
            if (latestMatchingPendingSubmission.isPresent()) {
                programmingSubmission = latestMatchingPendingSubmission.get();
            }
            else {
                // There can be two reasons for the case that there is no programmingSubmission:
                // 1) Manual build triggered from Bamboo.
                // 2) An unknown error that caused the programming submission not to be created when the code commits have been pushed
                // we can still get the commit hash from the payload of the Bamboo REST Call and "reverse engineer" the programming submission object to be consistent
                String commitHash = getCommitHash(buildResult, SubmissionType.MANUAL);
                log.warn("Could not find pending ProgrammingSubmission for Commit-Hash {} (Participation {}, Build-Plan {}). Will create a new one subsequently...", commitHash,
                        participation.getId(), participation.getBuildPlanId());
                programmingSubmission = new ProgrammingSubmission();
                programmingSubmission.setParticipation((Participation) participation);
                programmingSubmission.setSubmitted(true);
                programmingSubmission.setType(SubmissionType.OTHER);
                programmingSubmission.setCommitHash(commitHash);
                // In this case we don't know the submission time, so we use the result completion time as a fallback.
                programmingSubmission.setSubmissionDate(result.getCompletionDate());
                // Save to avoid TransientPropertyValueException.
            }
            final var hasArtifact = buildResult.getBuild().isArtifact();
            programmingSubmission.setBuildArtifact(hasArtifact);
            programmingSubmission.setBuildFailed(result.getResultString().equals("No tests found"));

            programmingSubmission = programmingSubmissionRepository.save(programmingSubmission);

            List<BuildLogEntry> buildLogEntries = new ArrayList<>();

            // Store logs into database. Append logs of multiple jobs.
            for (var job : buildResult.getBuild().getJobs()) {
                for (var bambooLog : job.getLogs()) {
                    // We have to unescape the HTML as otherwise symbols like '<' are not displayed correctly
                    buildLogEntries.add(new BuildLogEntry(bambooLog.getDate(), StringEscapeUtils.unescapeHtml(bambooLog.getLog()), programmingSubmission));
                }
            }

            // Set the received logs in order to avoid duplicate entries (this removes existing logs)
            programmingSubmission.setBuildLogEntries(filterBuildLogs(buildLogEntries));

            programmingSubmission = programmingSubmissionRepository.save(programmingSubmission);

            result.setSubmission(programmingSubmission);
            result.setRatedIfNotExceeded(programmingExercise.getDueDate(), programmingSubmission);
            // We can't save the result here, because we might later add more feedback items to the result (sequential test runs).
            // This seems like a bug in Hibernate/JPA: https://stackoverflow.com/questions/6763329/ordercolumn-onetomany-null-index-column-for-collection.
            return result;
        }
        catch (Exception e) {
            log.error("Error when creating build result from Bamboo notification: " + e.getMessage(), e);
            throw new BambooException("Could not create build result from Bamboo notification", e);
        }
    }

    @Override
    public ConnectorHealth health() {
        ConnectorHealth health;
        try {
            final var headers = HeaderUtil.createAuthorization(bambooUser, bambooPassword);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            final var entity = new HttpEntity<>(headers);
            final var status = restTemplate.exchange(bambooServerUrl + "/rest/api/latest/server", HttpMethod.GET, entity, JsonNode.class);
            health = status.getBody().get("state").asText().equals("RUNNING") ? new ConnectorHealth(true) : new ConnectorHealth(false);
        }
        catch (Exception emAll) {
            health = new ConnectorHealth(emAll);
        }

        health.setAdditionalInfo(Map.of("url", bambooServerUrl));
        return health;
    }

    @Override
    public Optional<String> getWebHookUrl(String projectKey, String buildPlanId) {
        // No webhooks needed between Bamboo and Bitbucket, so we return an empty Optional
        // See https://confluence.atlassian.com/bamboo/integrating-bamboo-with-bitbucket-server-779302772.html
        return Optional.empty();
    }

    /**
     * Check if the build result received is the initial build of the plan.
     *
     * @param buildResult Build result data provided by build notification.
     * @return true if build is the first build.
     */
    private boolean isFirstBuildForThisPlan(BambooBuildResultNotificationDTO buildResult) {
        final var reason = buildResult.getBuild().getReason();
        return reason != null && reason.contains("First build for this plan");
    }

    /**
     * Generate an Artemis result object from the CI build result. Will use the test case feedback as result feedback.
     *
     * @param buildResult Build result data provided by build notification.
     * @param participation to attach result to.
     * @return the created result (is not persisted in this method, only constructed!)
     */
    private Result createResultFromBuildResult(BambooBuildResultNotificationDTO buildResult, ProgrammingExerciseParticipation participation) {
        Result result = new Result();
        result.setAssessmentType(AssessmentType.AUTOMATIC);
        result.setSuccessful(buildResult.getBuild().isSuccessful());

        if (buildResult.getBuild().getTestSummary().getDescription().equals("No tests found")) {
            result.setResultString("No tests found");
        }
        else {
            int total = buildResult.getBuild().getTestSummary().getTotalCount();
            int passed = buildResult.getBuild().getTestSummary().getSuccessfulCount();
            result.setResultString(String.format("%d of %d passed", passed, total));
        }

        result.setCompletionDate(buildResult.getBuild().getBuildCompletedDate());
        result.setScore(0L); // the real score is calculated in the grading service
        result.setParticipation((Participation) participation);

        addFeedbackToResult(result, buildResult.getBuild().getJobs(), participation.getProgrammingExercise().isStaticCodeAnalysisEnabled());
        return result;
    }

    /**
     * Get the commit hash from the build map, the commit hash will be different for submission types or null.
     *
     * @param buildResult Build result data provided by build notification.
     * @param submissionType describes why the build was started.
     * @return if the commit hash for the given submission type was found, otherwise null.
     */
    private String getCommitHash(BambooBuildResultNotificationDTO buildResult, SubmissionType submissionType) {
        final Optional<String> optionalRelevantRepoName;
        if (List.of(SubmissionType.MANUAL, SubmissionType.INSTRUCTOR).contains(submissionType)) {
            optionalRelevantRepoName = Optional.of(ASSIGNMENT_REPO_NAME);
        }
        else if (submissionType.equals(SubmissionType.TEST)) {
            optionalRelevantRepoName = Optional.of(TEST_REPO_NAME);
        }
        else {
            optionalRelevantRepoName = Optional.empty();
        }

        return optionalRelevantRepoName.flatMap(relevantRepoName -> buildResult.getBuild().getVcs().stream()
                .filter(change -> change.getRepositoryName().equalsIgnoreCase(relevantRepoName)).findFirst().map(change -> change.getId())).orElse(null);
    }

    /**
     * Converts build result details into feedback and stores it in the result object
     *
     * @param result the result for which the feedback should be added
     * @param jobs   the jobs list of the requestBody
     * @param isStaticCodeAnalysisEnabled flag determining whether static code analysis was enabled
     * @return a list of feedback items stored in a result
     */
    private void addFeedbackToResult(Result result, List<BambooBuildResultNotificationDTO.BambooJobDTO> jobs, Boolean isStaticCodeAnalysisEnabled) {
        if (jobs == null) {
            return;
        }

        try {
            final ProgrammingLanguage programmingLanguage = ((ProgrammingExercise) result.getParticipation().getExercise()).getProgrammingLanguage();

            for (final var job : jobs) {

                // 1) add feedback for failed test cases
                for (final var failedTest : job.getFailedTests()) {
                    result.addFeedback(feedbackService.createFeedbackFromTestCase(failedTest.getName(), failedTest.getErrors(), false, programmingLanguage));
                }

                // 2) add feedback for passed test cases
                for (final var successfulTest : job.getSuccessfulTests()) {
                    result.addFeedback(feedbackService.createFeedbackFromTestCase(successfulTest.getName(), successfulTest.getErrors(), true, programmingLanguage));
                }

                // 3) process static code analysis feedback
                boolean hasStaticCodeAnalysisFeedback = false;
                if (Boolean.TRUE.equals(isStaticCodeAnalysisEnabled)) {
                    var reports = job.getStaticCodeAnalysisReports();
                    if (reports != null) {
                        var feedbackList = feedbackService.createFeedbackFromStaticCodeAnalysisReports(reports);
                        result.addFeedbacks(feedbackList);
                        hasStaticCodeAnalysisFeedback = feedbackList.size() > 0;
                    }
                }

                // Relevant feedback exists if tests failed or static code analysis found issues
                if (!job.getFailedTests().isEmpty() || hasStaticCodeAnalysisFeedback) {
                    result.setHasFeedback(true);
                }
            }

        }
        catch (Exception e) {
            log.error("Could not get feedback from jobs " + e);
        }
    }

    /**
     * Performs a request to the Bamboo REST API to retrive the latest result for the given plan.
     *
     * @param planKey the key of the plan for which to retrieve the latest result
     * @return a map containing the following data:
     * - successful:            if the build was successful
     * - buildTestSummary:      a string generated by Bamboo summarizing the build result
     * - buildCompletedDate:    the completion date of the build
     */
    public @Nullable QueriedBambooBuildResultDTO queryLatestBuildResultFromBambooServer(String planKey) {
        HttpHeaders headers = HeaderUtil.createAuthorization(bambooUser, bambooPassword);
        HttpEntity<?> entity = new HttpEntity<>(headers);
        ResponseEntity<QueriedBambooBuildResultDTO> response = null;
        try {
            response = restTemplate.exchange(
                    bambooServerUrl + "/rest/api/latest/result/" + planKey.toUpperCase()
                            + "-JOB1/latest.json?expand=testResults.failedTests.testResult.errors,artifacts,changes,vcsRevisions",
                    HttpMethod.GET, entity, QueriedBambooBuildResultDTO.class);
        }
        catch (Exception e) {
            log.warn("HttpError while retrieving latest build results from Bamboo for planKey " + planKey + ": " + e.getMessage());
        }
        if (response != null) {
            final var buildResult = response.getBody();

            // Filter out build log and static code analysis artifacts
            if (buildResult != null && buildResult.getArtifacts() != null) {
                List<String> artifactLabelFilter = StaticCodeAnalysisTool.getAllArtifactLabels();
                artifactLabelFilter.add("Build log");
                buildResult.getArtifacts().setArtifacts(
                        buildResult.getArtifacts().getArtifacts().stream().filter(artifact -> !artifactLabelFilter.contains(artifact.getName())).collect(Collectors.toList()));
            }

            // search for version control information
            // if (response.getBody().containsKey("vcsRevisions")) {
            // TODO: in case we have multiple commits here, we should expose this to the calling method so that this can potentially match this.
            // In the following example, the tests commit has is stored in vcsRevisionKey, but we might be interested in the assignment commit
            // "vcsRevisionKey":"20253bd4c2783aa5314efeee98d3503e4d25e668",
            // "vcsRevisions":{
            // "size":2,
            // "expand":"vcsRevision",
            // "vcsRevision":[
            // {
            // "repositoryId":239584155,
            // "repositoryName":"tests",
            // "vcsRevisionKey":"20253bd4c2783aa5314efeee98d3503e4d25e668"
            // },
            // {
            // "repositoryId":239584156,
            // "repositoryName":"assignment",
            // "vcsRevisionKey":"1c140ccff2be8c3d0d00c0d370557e258c1292cb"
            // }
            // ],
            // "start-index":0,
            // "max-result":2
            // },
            // List<Object> vcsRevisions = (List<Object>) response.getBody().get("vcsRevisions");
            // }

            return buildResult;
        }

        return null;
    }

    private List<BuildLogEntry> retrieveLatestBuildLogsFromDatabase(ProgrammingSubmission programmingSubmission) {
        Optional<ProgrammingSubmission> optionalProgrammingSubmission = programmingSubmissionRepository.findWithEagerBuildLogEntriesById(programmingSubmission.getId());
        if (optionalProgrammingSubmission.isPresent()) {
            return optionalProgrammingSubmission.get().getBuildLogEntries();
        }

        return List.of();
    }

    /**
     * Load the build log from the database.
     * Performs a request to the Bamboo REST API to retrieve the build log of the latest build, if the log is not available in the database.
     *
     * @param planKey to identify the build logs with.
     * @return the list of retrieved build logs.
     */
    private List<BuildLogEntry> retrieveLatestBuildLogsFromBamboo(String planKey) {
        HttpHeaders headers = HeaderUtil.createAuthorization(bambooUser, bambooPassword);
        HttpEntity<?> entity = new HttpEntity<>(headers);
        ResponseEntity<Map> response = null;
        try {
            response = restTemplate.exchange(bambooServerUrl + "/rest/api/latest/result/" + planKey.toUpperCase() + "-JOB1/latest.json?expand=logEntries&max-results=2000",
                    HttpMethod.GET, entity, Map.class);
        }
        catch (Exception e) {
            log.error("HttpError while retrieving build result logs from Bamboo: " + e.getMessage());
        }

        var logs = new ArrayList<BuildLogEntry>();

        if (response != null) {
            for (Map<String, Object> logEntry : (List<Map>) ((Map) response.getBody().get("logEntries")).get("logEntry")) {
                String logString = (String) logEntry.get("unstyledLog");
                // The log is provided in two attributes: with unescaped characters in unstyledLog and with escaped characters in log
                // We want to have unescaped characters but fail back to the escaped characters in case no unescaped characters are present
                if (logString == null) {
                    logString = (String) logEntry.get("log");
                }

                Instant instant = Instant.ofEpochMilli((long) logEntry.get("date"));
                ZonedDateTime logDate = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault());
                BuildLogEntry log = new BuildLogEntry(logDate, logString);
                logs.add(log);
            }
        }
        return logs;
    }

    /**
     * Filter the given list of unfiltered build log entries and return A NEW list only including the filtered build logs.
     * @param unfilteredBuildLogs the original, unfiltered list
     * @return the filtered list
     */
    private List<BuildLogEntry> filterBuildLogs(List<BuildLogEntry> unfilteredBuildLogs) {
        List<BuildLogEntry> filteredBuildLogs = new ArrayList<>();
        for (BuildLogEntry unfilteredBuildLog : unfilteredBuildLogs) {
            boolean compilationErrorFound = false;
            String logString = unfilteredBuildLog.getLog();

            if (logString.contains("COMPILATION ERROR")) {
                compilationErrorFound = true;
            }

            if (compilationErrorFound && logString.contains("BUILD FAILURE")) {
                // hide duplicated information that is displayed in the section COMPILATION ERROR and in the section BUILD FAILURE and stop here
                break;
            }

            // filter unnecessary logs
            if ((logString.startsWith("[INFO]") && !logString.contains("error")) || logString.startsWith("[WARNING]") || logString.startsWith("[ERROR] [Help 1]")
                    || logString.startsWith("[ERROR] For more information about the errors and possible solutions") || logString.startsWith("[ERROR] Re-run Maven using")
                    || logString.startsWith("[ERROR] To see the full stack trace of the errors") || logString.startsWith("[ERROR] -> [Help 1]")
                    || logString.startsWith("Unable to publish artifact") || logString.startsWith("NOTE: Picked up JDK_JAVA_OPTIONS")) {
                continue;
            }

            // Replace some unnecessary information and hide complex details to make it easier to read the important information
            logString = logString.replaceAll("/opt/bamboo-agent-home/xml-data/build-dir/", "");

            filteredBuildLogs.add(new BuildLogEntry(unfilteredBuildLog.getTime(), logString, unfilteredBuildLog.getProgrammingSubmission()));
        }

        return filteredBuildLogs;
    }

    /**
     * Gets the latest available artifact for the given participation.
     *
     * @param participation to use its buildPlanId to find the artifact.
     * @return the html representation of the artifact page.
     */
    public ResponseEntity<byte[]> retrieveLatestArtifact(ProgrammingExerciseParticipation participation) {
        // TODO: It would be better to directly pass the buildPlanId.
        String planKey = participation.getBuildPlanId();
        final var latestResult = queryLatestBuildResultFromBambooServer(planKey);
        // If the build has an artifact, the response contains an artifact key.
        // It seems this key is only available if the "Share" checkbox in Bamboo was used.
        if (latestResult != null && latestResult.getArtifacts() != null && !latestResult.getArtifacts().getArtifacts().isEmpty()) {
            // The URL points to the directory. Bamboo returns an "Index of" page.
            // Recursively walk through the responses until we get the actual artifact.
            return retrieveArtifactPage(latestResult.getArtifacts().getArtifacts().get(0).getLink().getLinkToArtifact().toString());
        }
        else {
            throw new BambooException("No build artifact available for this plan");
        }
    }

    @Override
    public String checkIfProjectExists(String projectKey, String projectName) {
        HttpHeaders headers = HeaderUtil.createAuthorization(bambooUser, bambooPassword);
        HttpEntity<?> entity = new HttpEntity<>(headers);
        try {
            restTemplate.exchange(bambooServerUrl + "/rest/api/latest/project/" + projectKey, HttpMethod.GET, entity, Map.class);
            log.warn("Bamboo project " + projectKey + " already exists");
            return "The project " + projectKey + " already exists in the CI Server. Please choose a different short name!";
        }
        catch (HttpClientErrorException e) {
            log.debug("Bamboo project " + projectKey + " does not exit");
            if (e.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
                // only if this is the case, we additionally check that the project name is unique
                final var response = restTemplate.exchange(bambooServerUrl + "/rest/api/latest/search/projects?searchTerm=" + projectName, HttpMethod.GET, entity,
                        BambooProjectSearchDTO.class);
                if (response.getBody() != null && response.getBody().getSize() > 0) {
                    final var exists = response.getBody().getSearchResults().stream().map(BambooProjectSearchDTO.SearchResultDTO::getSearchEntity)
                            .anyMatch(project -> project.getProjectName().equalsIgnoreCase(projectName));
                    if (exists) {
                        log.warn("Bamboo project with name" + projectName + " already exists");
                        return "The project " + projectName + " already exists in the CI Server. Please choose a different title!";
                    }
                }

                return null;
            }
        }
        return "The project already exists on the Continuous Integration Server. Please choose a different title and short name!";
    }

    /**
     * Gets the content from a Bamboo artifact link
     * Follows links on HTML directory pages, if necessary
     *
     * @param url of the artifact page.
     * @return the build artifact as html.
     */
    private ResponseEntity<byte[]> retrieveArtifactPage(String url) throws BambooException {
        HttpHeaders headers = HeaderUtil.createAuthorization(bambooUser, bambooPassword);
        HttpEntity<?> entity = new HttpEntity<>(headers);
        ResponseEntity<byte[]> response;

        try {
            response = restTemplate.exchange(url, HttpMethod.GET, entity, byte[].class);
        }
        catch (Exception e) {
            log.error("HttpError while retrieving build artifact", e);
            throw new BambooException("HttpError while retrieving build artifact");
        }

        // Note: Content-Type might contain additional elements such as the UTF-8 encoding, therefore we now use contains instead of equals
        if (response.getHeaders().containsKey("Content-Type") && response.getHeaders().get("Content-Type").get(0).contains("text/html")) {
            // This is an "Index of" HTML page.
            String html = new String(response.getBody(), StandardCharsets.UTF_8);
            Pattern pattern = Pattern.compile("href=\"(.*?)\"", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(html);
            if (matcher.find()) {
                url = matcher.group(1);
                // Recursively walk through the responses until we get the actual artifact.
                return retrieveArtifactPage(bambooServerUrl + url);
            }
            else {
                throw new BambooException("No artifact link found on artifact page");
            }
        }
        else {
            // Actual artifact file
            return response;
        }
    }

    /**
     * Retrieves the current build status of the given plan.
     *
     * @param planKey the key of the plan for which to retrieve the status
     * @return a map containing the following data:
     * - isActive: true if the plan is queued or building
     * - isBuilding: true if the plan is building
     */
    public Map<String, Boolean> retrieveBuildStatus(String planKey) {
        HttpHeaders headers = HeaderUtil.createAuthorization(bambooUser, bambooPassword);
        HttpEntity<?> entity = new HttpEntity<>(headers);
        ResponseEntity<Map> response = null;
        try {
            response = restTemplate.exchange(bambooServerUrl + "/rest/api/latest/plan/" + planKey.toUpperCase() + ".json", HttpMethod.GET, entity, Map.class);
        }
        catch (Exception e) {
            log.error("Bamboo HttpError '" + e.getMessage() + "' while retrieving build status for plan " + planKey, e);
        }
        if (response != null) {
            Map<String, Boolean> result = new HashMap<>();
            boolean isActive = (boolean) response.getBody().get("isActive");
            boolean isBuilding = (boolean) response.getBody().get("isBuilding");
            result.put("isActive", isActive);
            result.put("isBuilding", isBuilding);
            return result;
        }
        return null;
    }

    /**
     * Check if the given build plan is valid and accessible on Bamboo.
     *
     * @param buildPlanId unique identifier for build plan on CI system
     * @return true if the build plan id is valid.
     */
    @Override
    public boolean buildPlanIdIsValid(String projectKey, String buildPlanId) {
        HttpHeaders headers = HeaderUtil.createAuthorization(bambooUser, bambooPassword);
        HttpEntity<?> entity = new HttpEntity<>(headers);
        ResponseEntity<Map> response = null;
        try {
            response = restTemplate.exchange(bambooServerUrl + "/rest/api/latest/plan/" + buildPlanId.toUpperCase(), HttpMethod.GET, entity, Map.class);
        }
        catch (Exception e) {
            return false;
        }
        return true;
    }

    private String getProjectKeyFromBuildPlanId(String buildPlanId) {
        return buildPlanId.split("-")[0];
    }

    private String getCleanPlanName(String name) {
        return name.toUpperCase().replaceAll("[^A-Z0-9]", "");
    }
}
