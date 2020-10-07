package de.tum.in.www1.artemis.web.rest;

import static de.tum.in.www1.artemis.web.rest.util.ResponseUtil.*;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import de.tum.in.www1.artemis.domain.*;
import de.tum.in.www1.artemis.domain.participation.StudentParticipation;
import de.tum.in.www1.artemis.repository.TextSubmissionRepository;
import de.tum.in.www1.artemis.security.jwt.AtheneTrackingTokenProvider;
import de.tum.in.www1.artemis.service.*;
import de.tum.in.www1.artemis.service.scheduled.TextClusteringScheduleService;
import de.tum.in.www1.artemis.web.rest.errors.AccessForbiddenException;
import de.tum.in.www1.artemis.web.rest.errors.BadRequestAlertException;

/**
 * REST controller for managing TextSubmission.
 */
@RestController
@RequestMapping("/api")
public class TextSubmissionResource {

    private static final String ENTITY_NAME = "textSubmission";

    private final Logger log = LoggerFactory.getLogger(TextSubmissionResource.class);

    private final TextSubmissionRepository textSubmissionRepository;

    private final ExerciseService exerciseService;

    private final TextExerciseService textExerciseService;

    private final AuthorizationCheckService authorizationCheckService;

    private final TextSubmissionService textSubmissionService;

    private final TextAssessmentService textAssessmentService;

    private final UserService userService;

    private final GradingCriterionService gradingCriterionService;

    private final Optional<TextClusteringScheduleService> textClusteringScheduleService;

    private final ExamSubmissionService examSubmissionService;

    private final Optional<AtheneTrackingTokenProvider> atheneTrackingTokenProvider;

    public TextSubmissionResource(TextSubmissionRepository textSubmissionRepository, ExerciseService exerciseService, TextExerciseService textExerciseService,
            AuthorizationCheckService authorizationCheckService, TextSubmissionService textSubmissionService, UserService userService,
            GradingCriterionService gradingCriterionService, TextAssessmentService textAssessmentService, Optional<TextClusteringScheduleService> textClusteringScheduleService,
            ExamSubmissionService examSubmissionService, Optional<AtheneTrackingTokenProvider> atheneTrackingTokenProvider) {
        this.textSubmissionRepository = textSubmissionRepository;
        this.exerciseService = exerciseService;
        this.textExerciseService = textExerciseService;
        this.authorizationCheckService = authorizationCheckService;
        this.textSubmissionService = textSubmissionService;
        this.userService = userService;
        this.gradingCriterionService = gradingCriterionService;
        this.textClusteringScheduleService = textClusteringScheduleService;
        this.textAssessmentService = textAssessmentService;
        this.examSubmissionService = examSubmissionService;
        this.atheneTrackingTokenProvider = atheneTrackingTokenProvider;
    }

    /**
     * POST /exercises/{exerciseId}/text-submissions : Create a new textSubmission. This is called when a student saves his/her answer
     *
     * @param exerciseId     the id of the exercise for which to init a participation
     * @param principal      the current user principal
     * @param textSubmission the textSubmission to create
     * @return the ResponseEntity with status 200 (OK) and the Result as its body, or with status 4xx if the request is invalid
     */
    @PostMapping("/exercises/{exerciseId}/text-submissions")
    @PreAuthorize("hasAnyRole('USER', 'TA', 'INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<TextSubmission> createTextSubmission(@PathVariable Long exerciseId, Principal principal, @RequestBody TextSubmission textSubmission) {
        log.debug("REST request to save TextSubmission : {}", textSubmission);
        if (textSubmission.getId() != null) {
            throw new BadRequestAlertException("A new textSubmission cannot already have an ID", ENTITY_NAME, "idexists");
        }

        return handleTextSubmission(exerciseId, principal, textSubmission);
    }

    /**
     * PUT /exercises/{exerciseId}/text-submissions : Updates an existing textSubmission. This function is called by the text editor for saving and submitting text submissions. The
     * submit specific handling occurs in the TextSubmissionService.save() function.
     *
     * @param exerciseId     the id of the exercise for which to init a participation
     * @param principal      the current user principal
     * @param textSubmission the textSubmission to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated textSubmission, or with status 400 (Bad Request) if the textSubmission is not valid, or with status
     *         500 (Internal Server Error) if the textSubmission couldn't be updated
     */
    @PutMapping("/exercises/{exerciseId}/text-submissions")
    @PreAuthorize("hasAnyRole('USER', 'TA', 'INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<TextSubmission> updateTextSubmission(@PathVariable Long exerciseId, Principal principal, @RequestBody TextSubmission textSubmission) {
        log.debug("REST request to update TextSubmission : {}", textSubmission);
        if (textSubmission.getId() == null) {
            return createTextSubmission(exerciseId, principal, textSubmission);
        }

        return handleTextSubmission(exerciseId, principal, textSubmission);
    }

    @NotNull
    private ResponseEntity<TextSubmission> handleTextSubmission(Long exerciseId, Principal principal, TextSubmission textSubmission) {
        long start = System.currentTimeMillis();
        final User user = userService.getUserWithGroupsAndAuthorities();
        final TextExercise textExercise = textExerciseService.findOne(exerciseId);

        // Apply further checks if it is an exam submission
        Optional<ResponseEntity<TextSubmission>> examSubmissionAllowanceFailure = examSubmissionService.checkSubmissionAllowance(textExercise, user);
        if (examSubmissionAllowanceFailure.isPresent()) {
            return examSubmissionAllowanceFailure.get();
        }

        // Prevent multiple submissions (currently only for exam submissions)
        textSubmission = (TextSubmission) examSubmissionService.preventMultipleSubmissions(textExercise, textSubmission, user);

        // Check if the user is allowed to submit
        Optional<ResponseEntity<TextSubmission>> submissionAllowanceFailure = textSubmissionService.checkSubmissionAllowance(textExercise, textSubmission, user);
        if (submissionAllowanceFailure.isPresent()) {
            return submissionAllowanceFailure.get();
        }

        textSubmission = textSubmissionService.handleTextSubmission(textSubmission, textExercise, principal);

        this.textSubmissionService.hideDetails(textSubmission, user);
        long end = System.currentTimeMillis();
        log.info("handleTextSubmission took " + (end - start) + "ms for exercise " + exerciseId + " and user " + principal.getName());

        return ResponseEntity.ok(textSubmission);
    }

    /**
     * GET /text-submissions/:id : get the "id" textSubmission.
     *
     * @param id the id of the textSubmission to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the textSubmission, or with status 404 (Not Found)
     */
    @GetMapping("/text-submissions/{id}")
    public ResponseEntity<TextSubmission> getTextSubmission(@PathVariable Long id) {
        log.debug("REST request to get TextSubmission : {}", id);
        Optional<TextSubmission> optionalTextSubmission = textSubmissionRepository.findById(id);

        if (optionalTextSubmission.isEmpty()) {
            return notFound();
        }
        final TextSubmission textSubmission = optionalTextSubmission.get();

        // Add the jwt token as a header to the response for tutor-assessment tracking to the request if the athene profile is set
        final ResponseEntity.BodyBuilder bodyBuilder = ResponseEntity.ok();
        if (textSubmission.getResult() != null) {
            this.atheneTrackingTokenProvider
                    .ifPresent(atheneTrackingTokenProvider -> atheneTrackingTokenProvider.addTokenToResponseEntity(bodyBuilder, textSubmission.getResult()));
        }

        return bodyBuilder.body(textSubmission);
    }

    /**
     * GET /text-submissions : get all the textSubmissions for an exercise. It is possible to filter, to receive only the one that have been already submitted, or only the one
     * assessed by the tutor who is doing the call.
     * In case of exam exercise, it filters out all test run submissions.
     *
     * @param exerciseId exerciseID  for which all submissions should be returned
     * @param submittedOnly mark if only submitted Submissions should be returned
     * @param assessedByTutor mark if only assessed Submissions should be returned
     * @return the ResponseEntity with status 200 (OK) and the list of textSubmissions in body
     */
    @GetMapping(value = "/exercises/{exerciseId}/text-submissions")
    @PreAuthorize("hasAnyRole('TA', 'INSTRUCTOR', 'ADMIN')")
    // TODO: separate this into 2 calls, one for instructors (with all submissions) and one for tutors (only the submissions for the requesting tutor)
    public ResponseEntity<List<TextSubmission>> getAllTextSubmissions(@PathVariable Long exerciseId, @RequestParam(defaultValue = "false") boolean submittedOnly,
            @RequestParam(defaultValue = "false") boolean assessedByTutor) {
        log.debug("REST request to get all TextSubmissions");
        User user = userService.getUserWithGroupsAndAuthorities();
        Exercise exercise = textExerciseService.findOne(exerciseId);
        if (assessedByTutor) {
            if (!authorizationCheckService.isAtLeastTeachingAssistantForExercise(exercise)) {
                throw new AccessForbiddenException("You are not allowed to access this resource");
            }
        }
        else if (!authorizationCheckService.isAtLeastInstructorForExercise(exercise)) {
            throw new AccessForbiddenException("You are not allowed to access this resource");
        }

        List<TextSubmission> textSubmissions;
        final boolean examMode = exercise.hasExerciseGroup();
        if (assessedByTutor) {
            textSubmissions = textSubmissionService.getAllTextSubmissionsAssessedByTutorWithForExercise(exerciseId, user, examMode);
        }
        else {
            textSubmissions = textSubmissionService.getTextSubmissionsByExerciseId(exerciseId, submittedOnly, examMode);
        }

        // tutors should not see information about the student of a submission
        if (!authorizationCheckService.isAtLeastInstructorForExercise(exercise, user)) {
            textSubmissions.forEach(submission -> textSubmissionService.hideDetails(submission, user));
        }

        // remove unnecessary data from the REST response
        textSubmissions.forEach(submission -> {
            if (submission.getParticipation() != null && submission.getParticipation().getExercise() != null) {
                submission.getParticipation().setExercise(null);
            }
        });

        return ResponseEntity.ok().body(textSubmissions);
    }

    /**
     * GET /text-submission-without-assessment : get one textSubmission without assessment.
     *
     * @param exerciseId exerciseID  for which a submission should be returned
     * TODO: Replace ?head=true with HTTP HEAD request
     * @param skipAssessmentOrderOptimization optional value to define if the assessment queue should be skipped. Use if only checking for needed assessments.
     * @param lockSubmission optional value to define if the submission should be locked and has the value of false if not set manually
     * @return the ResponseEntity with status 200 (OK) and the list of textSubmissions in body
     */
    @GetMapping(value = "/exercises/{exerciseId}/text-submission-without-assessment")
    @PreAuthorize("hasAnyRole('TA', 'INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<TextSubmission> getTextSubmissionWithoutAssessment(@PathVariable Long exerciseId,
            @RequestParam(value = "head", defaultValue = "false") boolean skipAssessmentOrderOptimization,
            @RequestParam(value = "lock", defaultValue = "false") boolean lockSubmission) {
        log.debug("REST request to get a text submission without assessment");
        Exercise exercise = exerciseService.findOne(exerciseId);

        if (!authorizationCheckService.isAtLeastTeachingAssistantForExercise(exercise)) {
            return forbidden();
        }
        if (!(exercise instanceof TextExercise)) {
            return badRequest();
        }

        // Check if tutors can start assessing the students submission
        boolean startAssessingSubmissions = this.textSubmissionService.checkIfExerciseDueDateIsReached(exercise);
        if (!startAssessingSubmissions) {
            return forbidden();
        }

        // Tutors cannot start assessing submissions if Athene is currently processing
        if (textClusteringScheduleService.isPresent() && textClusteringScheduleService.get().currentlyProcessing((TextExercise) exercise)) {
            return notFound();
        }

        // Check if the limit of simultaneously locked submissions has been reached
        textSubmissionService.checkSubmissionLockLimit(exercise.getCourseViaExerciseGroupOrCourseMember().getId());

        final TextSubmission textSubmission;
        if (lockSubmission) {
            textSubmission = textSubmissionService.findAndLockTextSubmissionToBeAssessed((TextExercise) exercise, exercise.hasExerciseGroup());
            textAssessmentService.prepareSubmissionForAssessment(textSubmission);
        }
        else {
            Optional<TextSubmission> optionalTextSubmission;
            if (skipAssessmentOrderOptimization) {
                optionalTextSubmission = textSubmissionService.getRandomTextSubmissionEligibleForNewAssessment((TextExercise) exercise, true, exercise.hasExerciseGroup());
            }
            else {
                optionalTextSubmission = this.textSubmissionService.getRandomTextSubmissionEligibleForNewAssessment((TextExercise) exercise, exercise.hasExerciseGroup());
            }
            if (optionalTextSubmission.isEmpty()) {
                return notFound();
            }
            textSubmission = optionalTextSubmission.get();
        }

        List<GradingCriterion> gradingCriteria = gradingCriterionService.findByExerciseIdWithEagerGradingCriteria(exerciseId);
        exercise.setGradingCriteria(gradingCriteria);

        // Make sure the exercise is connected to the participation in the json response
        final StudentParticipation studentParticipation = (StudentParticipation) textSubmission.getParticipation();
        studentParticipation.setExercise(exercise);
        textSubmission.getParticipation().getExercise().setGradingCriteria(gradingCriteria);
        // Remove sensitive information of submission depending on user
        textSubmissionService.hideDetails(textSubmission, userService.getUserWithGroupsAndAuthorities());

        final ResponseEntity.BodyBuilder bodyBuilder = ResponseEntity.ok();

        // Add the jwt token as a header to the response for tutor-assessment tracking to the request if the athene profile is set
        if (textSubmission.getResult() != null) {
            this.atheneTrackingTokenProvider
                    .ifPresent(atheneTrackingTokenProvider -> atheneTrackingTokenProvider.addTokenToResponseEntity(bodyBuilder, textSubmission.getResult()));
        }
        return bodyBuilder.body(textSubmission);
    }
}
