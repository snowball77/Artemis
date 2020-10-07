package de.tum.in.www1.artemis.web.rest;

import static de.tum.in.www1.artemis.web.rest.util.ResponseUtil.forbidden;
import static java.time.ZonedDateTime.now;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.Principal;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.audit.AuditEventRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJacksonValue;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import de.tum.in.www1.artemis.config.Constants;
import de.tum.in.www1.artemis.config.GuidedTourConfiguration;
import de.tum.in.www1.artemis.domain.*;
import de.tum.in.www1.artemis.domain.enumeration.AssessmentType;
import de.tum.in.www1.artemis.domain.modeling.ModelingExercise;
import de.tum.in.www1.artemis.domain.participation.*;
import de.tum.in.www1.artemis.domain.quiz.QuizExercise;
import de.tum.in.www1.artemis.service.*;
import de.tum.in.www1.artemis.service.connectors.ContinuousIntegrationService;
import de.tum.in.www1.artemis.service.feature.Feature;
import de.tum.in.www1.artemis.service.feature.FeatureToggle;
import de.tum.in.www1.artemis.service.feature.FeatureToggleService;
import de.tum.in.www1.artemis.web.rest.errors.AccessForbiddenException;
import de.tum.in.www1.artemis.web.rest.errors.BadRequestAlertException;
import de.tum.in.www1.artemis.web.rest.errors.EntityNotFoundException;
import de.tum.in.www1.artemis.web.rest.util.HeaderUtil;
import de.tum.in.www1.artemis.web.rest.util.ResponseUtil;

/**
 * REST controller for managing Participation.
 */
@RestController
@RequestMapping(ParticipationResource.Endpoints.ROOT)
@PreAuthorize("hasRole('ADMIN')")
public class ParticipationResource {

    private final Logger log = LoggerFactory.getLogger(ParticipationResource.class);

    private static final String ENTITY_NAME = "participation";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final ParticipationService participationService;

    private final ProgrammingExerciseParticipationService programmingExerciseParticipationService;

    private final QuizExerciseService quizExerciseService;

    private final ExerciseService exerciseService;

    private final CourseService courseService;

    private final AuthorizationCheckService authCheckService;

    private final Optional<ContinuousIntegrationService> continuousIntegrationService;

    private final AuthorizationCheckService authorizationCheckService;

    private final UserService userService;

    private final AuditEventRepository auditEventRepository;

    private final GuidedTourConfiguration guidedTourConfiguration;

    private final TeamService teamService;

    private final FeatureToggleService featureToggleService;

    public ParticipationResource(ParticipationService participationService, ProgrammingExerciseParticipationService programmingExerciseParticipationService,
            CourseService courseService, QuizExerciseService quizExerciseService, ExerciseService exerciseService, AuthorizationCheckService authCheckService,
            Optional<ContinuousIntegrationService> continuousIntegrationService, AuthorizationCheckService authorizationCheckService, UserService userService,
            AuditEventRepository auditEventRepository, GuidedTourConfiguration guidedTourConfiguration, TeamService teamService, FeatureToggleService featureToggleService) {
        this.participationService = participationService;
        this.programmingExerciseParticipationService = programmingExerciseParticipationService;
        this.quizExerciseService = quizExerciseService;
        this.exerciseService = exerciseService;
        this.courseService = courseService;
        this.authCheckService = authCheckService;
        this.continuousIntegrationService = continuousIntegrationService;
        this.authorizationCheckService = authorizationCheckService;
        this.userService = userService;
        this.auditEventRepository = auditEventRepository;
        this.guidedTourConfiguration = guidedTourConfiguration;
        this.teamService = teamService;
        this.featureToggleService = featureToggleService;
    }

    /**
     * POST /courses/:courseId/exercises/:exerciseId/participations : start the "participationId" exercise for the current user.
     *
     * @param courseId   only included for API consistency, not actually used
     * @param exerciseId the participationId of the exercise for which to init a participation
     * @return the ResponseEntity with status 201 (Created) and the participation within the body, or with status 404 (Not Found)
     * @throws URISyntaxException If the URI for the created participation could not be created
     */
    @PostMapping(Endpoints.START_PARTICIPATION)
    @PreAuthorize("hasAnyRole('USER', 'TA', 'INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<Participation> startParticipation(@PathVariable Long courseId, @PathVariable Long exerciseId) throws URISyntaxException {
        log.debug("REST request to start Exercise : {}", exerciseId);
        Exercise exercise = exerciseService.findOneWithAdditionalElements(exerciseId);
        User user = userService.getUserWithGroupsAndAuthorities();
        Participant participant = user;
        Course course = exercise.getCourseViaExerciseGroupOrCourseMember();
        if (!authCheckService.isAtLeastStudentInCourse(course, user)) {
            throw new AccessForbiddenException("You are not allowed to access this resource");
        }

        // if the user is a student and the exercise has a release date, he cannot start the exercise before the release date
        if (exercise.getReleaseDate() != null && exercise.getReleaseDate().isAfter(now())) {
            if (authCheckService.isOnlyStudentInCourse(course, user)) {
                return forbidden();
            }
        }

        // users cannot start the programming exercises if test run after due date or semi automatic grading is active and the due date has passed
        // Also don't allow participations if the feature is disabled
        if (exercise instanceof ProgrammingExercise) {
            var programmingExercise = (ProgrammingExercise) exercise;
            if (!featureToggleService.isFeatureEnabled(Feature.PROGRAMMING_EXERCISES) || (programmingExercise.getDueDate() != null
                    && now().isAfter(programmingExercise.getDueDate())
                    && (programmingExercise.getBuildAndTestStudentSubmissionsAfterDueDate() != null || programmingExercise.getAssessmentType() != AssessmentType.AUTOMATIC))) {
                return forbidden();
            }
        }

        // if this is a team-based exercise, set the participant to the team that the user belongs to
        if (exercise.isTeamMode()) {
            participant = teamService.findOneByExerciseAndUser(exercise, user)
                    .orElseThrow(() -> new BadRequestAlertException("Team exercise cannot be started without assigned team.", "participation", "cannotStart"));
        }

        StudentParticipation participation = participationService.startExercise(exercise, participant, true);
        // remove sensitive information before sending participation to the client
        participation.getExercise().filterSensitiveInformation();
        return ResponseEntity.created(new URI("/api/participations/" + participation.getId())).body(participation);
    }

    /**
     * POST /courses/:courseId/exercises/:exerciseId/resume-participation: resume the participation of the current user in the exercise identified by participationId
     *
     * @param courseId   only included for API consistency, not actually used
     * @param exerciseId participationId of the exercise for which to resume participation
     * @param principal  current user principal
     * @return ResponseEntity with status 200 (OK) and with updated participation as a body, or with status 500 (Internal Server Error)
     */
    @PutMapping(value = "/courses/{courseId}/exercises/{exerciseId}/resume-programming-participation")
    @PreAuthorize("hasAnyRole('USER', 'TA', 'INSTRUCTOR', 'ADMIN')")
    @FeatureToggle(Feature.PROGRAMMING_EXERCISES)
    public ResponseEntity<ProgrammingExerciseStudentParticipation> resumeParticipation(@PathVariable Long courseId, @PathVariable Long exerciseId, Principal principal) {
        log.debug("REST request to resume Exercise : {}", exerciseId);
        Exercise exercise;
        try {
            exercise = exerciseService.findOneWithAdditionalElements(exerciseId);
        }
        catch (EntityNotFoundException e) {
            log.info("Request to resume participation of non-existing Exercise with participationId {}.", exerciseId);
            throw new BadRequestAlertException(e.getMessage(), "exercise", "exerciseNotFound");
        }

        ProgrammingExerciseStudentParticipation participation = programmingExerciseParticipationService.findStudentParticipationByExerciseAndStudentId(exercise,
                principal.getName());

        User user = userService.getUserWithGroupsAndAuthorities();
        checkAccessPermissionOwner(participation, user);
        if (exercise instanceof ProgrammingExercise) {

            // users cannot resume the programming exercises if test run after due date or semi automatic grading is active and the due date has passed
            var pExercise = (ProgrammingExercise) exercise;
            if ((pExercise.getDueDate() != null && ZonedDateTime.now().isAfter(pExercise.getDueDate())
                    && (pExercise.getBuildAndTestStudentSubmissionsAfterDueDate() != null || pExercise.getAssessmentType() != AssessmentType.AUTOMATIC))) {
                return forbidden();
            }

            participation = participationService.resumeExercise(participation);
            // Note: in this case we might need an empty commit to make sure the build plan works correctly for subsequent student commits
            participation = participationService.performEmptyCommit(participation);
            if (participation != null) {
                addLatestResultToParticipation(participation);
                participation.getExercise().filterSensitiveInformation();
                return ResponseEntity.ok().headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, participation.getParticipant().getName()))
                        .body(participation);
            }
        }
        // error case
        log.info("Exercise with participationId {} is not an instance of ProgrammingExercise. Ignoring the request to resume participation", exerciseId);
        // remove sensitive information before sending participation to the client
        if (participation != null && participation.getExercise() != null) {
            participation.getExercise().filterSensitiveInformation();
        }
        return ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert(applicationName, true, ENTITY_NAME, "notProgrammingExercise",
                "Exercise is not an instance of ProgrammingExercise. Ignoring the request to resume participation")).body(participation);
    }

    /**
     * This makes sure the client can display the latest result immediately after loading this participation
     *
     * @param participation The participation to which the latest result should get added
     */
    private void addLatestResultToParticipation(Participation participation) {
        // Load results of participation as they are not contained in the current object
        participation = participationService.findOneWithEagerResults(participation.getId());

        Result result = participation.findLatestResult();
        if (result != null) {
            participation.setResults(Set.of(result));
        }
    }

    /**
     * PUT /participations : Updates an existing participation.
     *
     * @param participation the participation to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated participation, or with status 400 (Bad Request) if the participation is not valid, or with status
     *         500 (Internal Server Error) if the participation couldn't be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    // TODO we should not use this global resource here, instead we should use /courses/:courseId/exercises/:exerciseId/participations
    @PutMapping("/participations")
    @PreAuthorize("hasAnyRole('TA', 'INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<Participation> updateParticipation(@RequestBody StudentParticipation participation) throws URISyntaxException {
        log.debug("REST request to update Participation : {}", participation);
        Course course = participation.getExercise().getCourseViaExerciseGroupOrCourseMember();
        User user = userService.getUserWithGroupsAndAuthorities();
        if (!authorizationCheckService.isAtLeastTeachingAssistantInCourse(course, user)) {
            throw new AccessForbiddenException("You are not allowed to access this resource");
        }
        if (participation.getId() == null) {
            throw new BadRequestAlertException("The participation object needs to have an id to be changed", ENTITY_NAME, "idmissing");
        }
        if (participation.getPresentationScore() == null || participation.getPresentationScore() < 0) {
            participation.setPresentationScore(0);
        }
        if (participation.getPresentationScore() > 1) {
            participation.setPresentationScore(1);
        }
        StudentParticipation currentParticipation = participationService.findOneStudentParticipation(participation.getId());
        if (currentParticipation.getPresentationScore() != null && currentParticipation.getPresentationScore() > participation.getPresentationScore()) {
            log.info(user.getLogin() + " removed the presentation score of " + participation.getParticipantIdentifier() + " for exercise with participationId "
                    + participation.getExercise().getId());
        }

        Participation result = participationService.save(participation);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, participation.getParticipant().getName())).body(result);
    }

    /**
     * GET /exercises/:exerciseId/participations : get all the participations for an exercise
     *
     * @param exerciseId The participationId of the exercise
     * @param withLatestResult Whether the {@link Result results} for the participations should also be fetched
     * @return A list of all participations for the exercise
     */
    @GetMapping(value = "/exercises/{exerciseId}/participations")
    @PreAuthorize("hasAnyRole('TA', 'INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<List<StudentParticipation>> getAllParticipationsForExercise(@PathVariable Long exerciseId,
            @RequestParam(defaultValue = "false") boolean withLatestResult) {
        log.debug("REST request to get all Participations for Exercise {}", exerciseId);
        Exercise exercise = exerciseService.findOneWithAdditionalElements(exerciseId);
        Course course = exercise.getCourseViaExerciseGroupOrCourseMember();
        User user = userService.getUserWithGroupsAndAuthorities();
        if (!authorizationCheckService.isAtLeastTeachingAssistantInCourse(course, user)) {
            throw new AccessForbiddenException("You are not allowed to access this resource");
        }

        boolean examMode = exercise.hasExerciseGroup();
        List<StudentParticipation> participations;
        if (withLatestResult) {
            participations = participationService.findByExerciseIdWithLatestResult(exerciseId, examMode);
        }
        else {
            participations = participationService.findByExerciseId(exerciseId);
        }
        participations = participations.stream().filter(participation -> participation.getParticipant() != null).collect(Collectors.toList());

        Map<Long, Integer> submissionCountMap = participationService.countSubmissionsPerParticipationByExerciseId(exerciseId);
        participations.forEach(participation -> participation.setSubmissionCount(submissionCountMap.get(participation.getId())));

        return ResponseEntity.ok(participations);
    }

    /**
     * GET /courses/:courseId/participations : get all the participations for a course
     *
     * @param courseId The participationId of the course
     * @return A list of all participations for the given course
     */
    @GetMapping(value = "/courses/{courseId}/participations")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<List<StudentParticipation>> getAllParticipationsForCourse(@PathVariable Long courseId) {
        long start = System.currentTimeMillis();
        log.debug("REST request to get all Participations for Course {}", courseId);
        Course course = courseService.findOne(courseId);
        User user = userService.getUserWithGroupsAndAuthorities();
        if (!authorizationCheckService.isAtLeastInstructorInCourse(course, user)) {
            throw new AccessForbiddenException("You are not allowed to access this resource");
        }
        List<StudentParticipation> participations = participationService.findByCourseIdWithRelevantResult(courseId);
        int resultCount = 0;
        for (StudentParticipation participation : participations) {
            // make sure the registration number is explicitly shown in the client
            participation.getStudents().forEach(student -> student.setVisibleRegistrationNumber(student.getRegistrationNumber()));
            // we only need participationId, title, dates and max points
            // remove unnecessary elements
            Exercise exercise = participation.getExercise();
            exercise.setCourse(null);
            exercise.setStudentParticipations(null);
            exercise.setTutorParticipations(null);
            exercise.setExampleSubmissions(null);
            exercise.setAttachments(null);
            exercise.setCategories(null);
            exercise.setProblemStatement(null);
            exercise.setStudentQuestions(null);
            exercise.setGradingInstructions(null);
            exercise.setDifficulty(null);
            exercise.setMode(null);
            if (exercise instanceof ProgrammingExercise) {
                ProgrammingExercise programmingExercise = (ProgrammingExercise) exercise;
                programmingExercise.setSolutionParticipation(null);
                programmingExercise.setTemplateParticipation(null);
                programmingExercise.setTestRepositoryUrl(null);
                programmingExercise.setShortName(null);
                programmingExercise.setPublishBuildPlanUrl(null);
                programmingExercise.setProgrammingLanguage(null);
                programmingExercise.setPackageName(null);
                programmingExercise.setAllowOnlineEditor(null);
            }
            else if (exercise instanceof QuizExercise) {
                QuizExercise quizExercise = (QuizExercise) exercise;
                quizExercise.setQuizQuestions(null);
                quizExercise.setQuizPointStatistic(null);
            }
            else if (exercise instanceof TextExercise) {
                TextExercise textExercise = (TextExercise) exercise;
                textExercise.setSampleSolution(null);
            }
            else if (exercise instanceof ModelingExercise) {
                ModelingExercise modelingExercise = (ModelingExercise) exercise;
                modelingExercise.setSampleSolutionModel(null);
                modelingExercise.setSampleSolutionExplanation(null);
            }
            resultCount += participation.getResults().size();
        }
        long end = System.currentTimeMillis();
        log.info("Found " + participations.size() + " particpations with " + resultCount + " results in " + (end - start) + " ms");
        return ResponseEntity.ok().body(participations);
    }

    /**
     * GET /participations/:participationId : get the participation for the given "participationId" including its latest result.
     *
     * @param participationId the participationId of the participation to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the participation, or with status 404 (Not Found)
     */
    @GetMapping("/participations/{participationId}/withLatestResult")
    @PreAuthorize("hasAnyRole('USER', 'TA', 'INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<StudentParticipation> getParticipationWithLatestResult(@PathVariable Long participationId) {
        log.debug("REST request to get Participation : {}", participationId);
        StudentParticipation participation = participationService.findOneWithEagerResults(participationId);
        participationService.canAccessParticipation(participation);
        Result result = participation.getExercise().findLatestResultWithCompletionDate(participation);
        Set<Result> results = new HashSet<>();
        if (result != null) {
            results.add(result);
        }
        participation.setResults(results);
        return new ResponseEntity<>(participation, HttpStatus.OK);
    }

    /**
     * GET /participations/:participationId : get the participation for the given "participationId".
     *
     * @param participationId the participationId of the participation to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the participation, or with status 404 (Not Found)
     */
    @GetMapping("/participations/{participationId}")
    @PreAuthorize("hasAnyRole('USER', 'TA', 'INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<StudentParticipation> getParticipation(@PathVariable Long participationId) {
        log.debug("REST request to get participation : {}", participationId);
        StudentParticipation participation = participationService.findOneStudentParticipation(participationId);
        User user = userService.getUserWithGroupsAndAuthorities();
        checkAccessPermissionOwner(participation, user);
        return Optional.ofNullable(participation).map(result -> new ResponseEntity<>(result, HttpStatus.OK)).orElse(ResponseUtil.notFound());
    }

    /**
     * Retrieves the latest build artifact of a given programming exercise participation
     *
     * @param participationId The participationId of the participation
     * @return The latest build artifact (JAR/WAR) for the participation
     */
    @GetMapping(value = "/participations/{participationId}/buildArtifact")
    @PreAuthorize("hasAnyRole('USER', 'TA', 'INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<byte[]> getParticipationBuildArtifact(@PathVariable Long participationId) {
        log.debug("REST request to get Participation build artifact: {}", participationId);
        ProgrammingExerciseStudentParticipation participation = participationService.findProgrammingExerciseParticipation(participationId);
        User user = userService.getUserWithGroupsAndAuthorities();
        checkAccessPermissionOwner(participation, user);

        return continuousIntegrationService.get().retrieveLatestArtifact(participation);
    }

    /**
     * GET /courses/:courseId/exercises/:exerciseId/participation: get the user's participation for a specific exercise. Please note: 'courseId' is only included in the call for
     * API consistency, it is not actually used //TODO remove courseId from the URL
     *
     * @param exerciseId the participationId of the exercise for which to retrieve the participation
     * @param principal The principal in form of the user's identity
     * @return the ResponseEntity with status 200 (OK) and with body the participation, or with status 404 (Not Found)
     */
    @GetMapping(value = "/exercises/{exerciseId}/participation")
    @PreAuthorize("hasAnyRole('USER', 'TA', 'INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<MappingJacksonValue> getParticipation(@PathVariable Long exerciseId, Principal principal) {
        log.debug("REST request to get Participation for Exercise : {}", exerciseId);
        Exercise exercise = exerciseService.findOneWithAdditionalElements(exerciseId);
        Course course = exercise.getCourseViaExerciseGroupOrCourseMember();
        User user = userService.getUserWithGroupsAndAuthorities();
        if (!authorizationCheckService.isAtLeastStudentInCourse(course, user)) {
            throw new AccessForbiddenException("You are not allowed to access this resource");
        }
        MappingJacksonValue response;
        if (exercise instanceof QuizExercise) {
            response = participationForQuizExercise((QuizExercise) exercise, principal.getName());
        }
        else {
            Optional<StudentParticipation> optionalParticipation = participationService.findOneByExerciseAndStudentLoginAnyStateWithEagerResults(exercise, principal.getName());
            if (optionalParticipation.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.FAILED_DEPENDENCY, "No participation found for " + principal.getName() + " in exercise " + exerciseId);
            }
            Participation participation = optionalParticipation.get();
            response = new MappingJacksonValue(participation);
        }
        if (response == null) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        else {
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
    }

    private MappingJacksonValue participationForQuizExercise(QuizExercise quizExercise, String username) {
        if (!quizExercise.isStarted()) {
            // Quiz hasn't started yet => no Result, only quizExercise without questions
            quizExercise.filterSensitiveInformation();
            StudentParticipation participation = new StudentParticipation().exercise(quizExercise);
            return new MappingJacksonValue(participation);
        }
        else if (quizExercise.isSubmissionAllowed()) {
            // Quiz is active => construct Participation from
            // filtered quizExercise and submission from HashMap
            quizExercise = quizExerciseService.findOneWithQuestions(quizExercise.getId());
            quizExercise.filterForStudentsDuringQuiz();
            StudentParticipation participation = participationService.participationForQuizWithResult(quizExercise, username);
            // set view
            Class view = quizExerciseService.viewForStudentsInQuizExercise(quizExercise);
            MappingJacksonValue value = new MappingJacksonValue(participation);
            value.setSerializationView(view);
            return value;
        }
        else {
            // quiz has ended => get participation from database and add full quizExercise
            quizExercise = quizExerciseService.findOneWithQuestions(quizExercise.getId());
            // TODO: we get a lot of error message here, when the quiz has ended, students reload the page (or navigate again into it), but the participation (+ submission
            // + result) has not yet been stored in the database (because for 1500 students this can take up to 60s). We should handle this case here properly
            // The best would be a message to the user: please wait while the quiz results are being processed (show a progress animation in the client)
            StudentParticipation participation = participationService.participationForQuizWithResult(quizExercise, username);
            // avoid problems due to bidirectional associations between submission and result during serialization
            if (participation == null) {
                return null;
            }
            for (Result result : participation.getResults()) {
                if (result.getSubmission() != null) {
                    result.getSubmission().setResult(null);
                    result.getSubmission().setParticipation(null);
                }
            }
            return new MappingJacksonValue(participation);
        }
    }

    /**
     * DELETE /participations/:participationId : delete the "participationId" participation. This only works for student participations - other participations should not be deleted here!
     *
     * @param participationId the participationId of the participation to delete
     * @param deleteBuildPlan True, if the build plan should also get deleted
     * @param deleteRepository True, if the repository should also get deleted
     * @param principal The identity of the user accessing this resource
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/participations/{participationId}")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<Void> deleteParticipation(@PathVariable Long participationId, @RequestParam(defaultValue = "false") boolean deleteBuildPlan,
            @RequestParam(defaultValue = "false") boolean deleteRepository, Principal principal) {
        StudentParticipation participation = participationService.findOneStudentParticipation(participationId);

        if (participation instanceof ProgrammingExerciseParticipation && !featureToggleService.isFeatureEnabled(Feature.PROGRAMMING_EXERCISES)) {
            return forbidden();
        }

        User user = userService.getUserWithGroupsAndAuthorities();

        checkAccessPermissionAtLeastInstructor(participation, user);

        String name = participation.getParticipant().getName();
        var logMessage = "Delete Participation " + participationId + " of exercise " + participation.getExercise().getTitle() + " for " + name + ", deleteBuildPlan: "
                + deleteBuildPlan + ", deleteRepository: " + deleteRepository + " by " + principal.getName();
        var auditEvent = new AuditEvent(user.getLogin(), Constants.DELETE_PARTICIPATION, logMessage);
        auditEventRepository.add(auditEvent);
        log.info(logMessage);
        participationService.delete(participationId, deleteBuildPlan, deleteRepository);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, "participation", name)).build();
    }

    /**
     * DELETE guided-tour/participations/:participationId : delete the "participationId" participation of student participations for guided tutorials (e.g. when restarting a tutorial)
     * Please note: all users can delete their own participation when it belongs to a guided tutorial
     *
     * @param participationId the participationId of the participation to delete
     * @param deleteBuildPlan True, if the build plan should also get deleted
     * @param deleteRepository True, if the repository should also get deleted
     * @param principal The identity of the user accessing this resource
     * @return the ResponseEntity with status 200 (OK) or 403 (FORBIDDEN)
     */
    @DeleteMapping("guided-tour/participations/{participationId}")
    @PreAuthorize("hasAnyRole('USER', 'TA', 'INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<Void> deleteParticipationForGuidedTour(@PathVariable Long participationId, @RequestParam(defaultValue = "false") boolean deleteBuildPlan,
            @RequestParam(defaultValue = "false") boolean deleteRepository, Principal principal) {
        StudentParticipation participation = participationService.findOneStudentParticipation(participationId);

        if (participation instanceof ProgrammingExerciseParticipation && !featureToggleService.isFeatureEnabled(Feature.PROGRAMMING_EXERCISES)) {
            return forbidden();
        }

        User user = userService.getUserWithGroupsAndAuthorities();

        // Allow all users to delete their own StudentParticipations if it's for a tutorial
        if (participation.isOwnedBy(user)) {
            checkAccessPermissionAtLeastStudent(participation, user);
            if (!guidedTourConfiguration.isExerciseForTutorial(participation.getExercise())) {
                return forbidden();
            }
        }
        else {
            return forbidden();
        }

        String name = participation.getParticipant().getName();
        var logMessage = "Delete Participation " + participationId + " of exercise " + participation.getExercise().getTitle() + " for " + name + ", deleteBuildPlan: "
                + deleteBuildPlan + ", deleteRepository: " + deleteRepository + " by " + principal.getName();
        var auditEvent = new AuditEvent(user.getLogin(), Constants.DELETE_PARTICIPATION, logMessage);
        auditEventRepository.add(auditEvent);
        log.info(logMessage);
        participationService.delete(participationId, deleteBuildPlan, deleteRepository);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, "participation", name)).build();
    }

    /**
     * DELETE /participations/:participationId : remove the build plan of the ProgrammingExerciseStudentParticipation of the "participationId".
     * This only works for programming exercises.
     *
     * @param participationId the participationId of the ProgrammingExerciseStudentParticipation for which the build plan should be removed
     * @param principal The identity of the user accessing this resource
     * @return the ResponseEntity with status 200 (OK)
     */
    @PutMapping("/participations/{participationId}/cleanupBuildPlan")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    @FeatureToggle(Feature.PROGRAMMING_EXERCISES)
    public ResponseEntity<Participation> cleanupBuildPlan(@PathVariable Long participationId, Principal principal) {
        ProgrammingExerciseStudentParticipation participation = (ProgrammingExerciseStudentParticipation) participationService.findOneStudentParticipation(participationId);
        User user = userService.getUserWithGroupsAndAuthorities();
        checkAccessPermissionAtLeastInstructor(participation, user);
        log.info("Clean up participation with build plan {} by {}", participation.getBuildPlanId(), principal.getName());
        participationService.cleanupBuildPlan(participation);
        return ResponseEntity.ok().body(participation);
    }

    private void checkAccessPermissionAtLeastStudent(StudentParticipation participation, User user) {
        Course course = findCourseFromParticipation(participation);
        if (!authCheckService.isAtLeastStudentInCourse(course, user)) {
            throw new AccessForbiddenException("You are not allowed to access this resource");
        }
    }

    private void checkAccessPermissionAtLeastInstructor(StudentParticipation participation, User user) {
        Course course = findCourseFromParticipation(participation);
        if (!authCheckService.isAtLeastInstructorInCourse(course, user)) {
            throw new AccessForbiddenException("You are not allowed to access this resource");
        }
    }

    private void checkAccessPermissionOwner(StudentParticipation participation, User user) {
        Course course = findCourseFromParticipation(participation);
        if (!authCheckService.isOwnerOfParticipation(participation)) {
            if (!authorizationCheckService.isAtLeastTeachingAssistantInCourse(course, user)) {
                throw new AccessForbiddenException("You are not allowed to access this resource");
            }
        }
    }

    private Course findCourseFromParticipation(StudentParticipation participation) {
        if (participation.getExercise() != null && participation.getExercise().getCourseViaExerciseGroupOrCourseMember() != null) {
            return participation.getExercise().getCourseViaExerciseGroupOrCourseMember();
        }

        return participationService.findOneWithEagerCourse(participation.getId()).getExercise().getCourseViaExerciseGroupOrCourseMember();
    }

    /**
     * fetches all submissions of a specific participation
     * @param participationId the id of the participation
     * @return all submissions that belong to the participation
     */
    @GetMapping(value = "/participations/{participationId}/submissions")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<List<Submission>> getSubmissionsOfParticipation(@PathVariable Long participationId) {
        StudentParticipation participation = participationService.findOneStudentParticipation(participationId);
        User user = userService.getUserWithGroupsAndAuthorities();
        checkAccessPermissionAtLeastInstructor(participation, user);
        List<Submission> submissions = participationService.getSubmissionsWithParticipationId(participationId);
        return ResponseEntity.ok(submissions);
    }

    public static final class Endpoints {

        public static final String ROOT = "/api";

        public static final String START_PARTICIPATION = "/courses/{courseId}/exercises/{exerciseId}/participations";

        private Endpoints() {
        }
    }
}
