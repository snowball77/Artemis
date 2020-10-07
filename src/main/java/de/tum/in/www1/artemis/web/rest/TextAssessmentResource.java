package de.tum.in.www1.artemis.web.rest;

import static de.tum.in.www1.artemis.web.rest.util.ResponseUtil.forbidden;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import de.tum.in.www1.artemis.domain.*;
import de.tum.in.www1.artemis.domain.participation.Participation;
import de.tum.in.www1.artemis.domain.participation.StudentParticipation;
import de.tum.in.www1.artemis.repository.ResultRepository;
import de.tum.in.www1.artemis.repository.TextBlockRepository;
import de.tum.in.www1.artemis.repository.TextSubmissionRepository;
import de.tum.in.www1.artemis.security.jwt.AtheneTrackingTokenProvider;
import de.tum.in.www1.artemis.service.*;
import de.tum.in.www1.artemis.web.rest.dto.TextAssessmentDTO;
import de.tum.in.www1.artemis.web.rest.dto.TextAssessmentUpdateDTO;
import de.tum.in.www1.artemis.web.rest.errors.BadRequestAlertException;
import de.tum.in.www1.artemis.web.rest.util.HeaderUtil;

/**
 * REST controller for managing TextAssessment.
 */
@RestController
@RequestMapping("/api/text-assessments")
public class TextAssessmentResource extends AssessmentResource {

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private static final String ENTITY_NAME = "textAssessment";

    private final Logger log = LoggerFactory.getLogger(TextAssessmentResource.class);

    private final TextBlockRepository textBlockRepository;

    private final TextAssessmentService textAssessmentService;

    private final TextExerciseService textExerciseService;

    private final TextSubmissionService textSubmissionService;

    private final TextSubmissionRepository textSubmissionRepository;

    private final WebsocketMessagingService messagingService;

    private final Optional<AtheneTrackingTokenProvider> atheneTrackingTokenProvider;

    private final Optional<AutomaticTextAssessmentConflictService> automaticTextAssessmentConflictService;

    private final GradingCriterionService gradingCriterionService;

    public TextAssessmentResource(AuthorizationCheckService authCheckService, TextAssessmentService textAssessmentService, TextBlockRepository textBlockRepository,
            TextExerciseService textExerciseService, TextSubmissionRepository textSubmissionRepository, UserService userService, TextSubmissionService textSubmissionService,
            WebsocketMessagingService messagingService, ExerciseService exerciseService, ResultRepository resultRepository, GradingCriterionService gradingCriterionService,
            Optional<AtheneTrackingTokenProvider> atheneTrackingTokenProvider, ExamService examService,
            Optional<AutomaticTextAssessmentConflictService> automaticTextAssessmentConflictService) {
        super(authCheckService, userService, exerciseService, textSubmissionService, textAssessmentService, resultRepository, examService);

        this.textAssessmentService = textAssessmentService;
        this.textBlockRepository = textBlockRepository;
        this.textExerciseService = textExerciseService;
        this.textSubmissionRepository = textSubmissionRepository;
        this.textSubmissionService = textSubmissionService;
        this.messagingService = messagingService;
        this.gradingCriterionService = gradingCriterionService;
        this.atheneTrackingTokenProvider = atheneTrackingTokenProvider;
        this.automaticTextAssessmentConflictService = automaticTextAssessmentConflictService;
    }

    /**
     * Saves a given manual textAssessment
     *
     * @param exerciseId the exerciseId of the exercise which will be saved
     * @param resultId the resultId the assessment belongs to
     * @param textAssessment the assessments
     * @return 200 Ok if successful with the corresponding result as body, but sensitive information are filtered out
     */
    @PutMapping("/exercise/{exerciseId}/result/{resultId}")
    @PreAuthorize("hasAnyRole('TA', 'INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<Result> saveTextAssessment(@PathVariable Long exerciseId, @PathVariable Long resultId, @RequestBody TextAssessmentDTO textAssessment) {
        User user = userService.getUserWithGroupsAndAuthorities();
        TextExercise textExercise = textExerciseService.findOne(exerciseId);
        checkTextExerciseForRequest(textExercise, user);

        final Optional<TextSubmission> optionalTextSubmission = textSubmissionRepository.findByResult_Id(resultId);
        if (optionalTextSubmission.isEmpty()) {
            throw new BadRequestAlertException("No text submission found for the given result.", "textSubmission", "textSubmissionNotFound");
        }

        StudentParticipation studentParticipation = (StudentParticipation) optionalTextSubmission.get().getParticipation();
        final var isAtLeastInstructor = authCheckService.isAtLeastInstructorForExercise(textExercise);

        if (!assessmentService.isAllowedToCreateOrOverrideResult(optionalTextSubmission.get().getResult(), textExercise, studentParticipation, user, isAtLeastInstructor)) {
            return forbidden("assessment", "assessmentSaveNotAllowed", "The user is not allowed to override the assessment");
        }

        saveTextBlocks(textAssessment.getTextBlocks(), optionalTextSubmission.get());
        Result result = textAssessmentService.saveAssessment(resultId, textAssessment.getFeedbacks());

        if (result.getParticipation() != null && result.getParticipation() instanceof StudentParticipation
                && !authCheckService.isAtLeastInstructorForExercise(textExercise, user)) {
            ((StudentParticipation) result.getParticipation()).filterSensitiveInformation();
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Submits manual textAssessments for a given result and notify the user if it's before the Assessment Due Date
     *
     * @param exerciseId the exerciseId of the exercise which will be saved
     * @param resultId the resultId the assessment belongs to
     * @param textAssessment the assessments which should be submitted
     * @return 200 Ok if successful with the corresponding result as a body, but sensitive information are filtered out
     */
    @PutMapping("/exercise/{exerciseId}/result/{resultId}/submit")
    @PreAuthorize("hasAnyRole('TA', 'INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<Result> submitTextAssessment(@PathVariable Long exerciseId, @PathVariable Long resultId, @RequestBody TextAssessmentDTO textAssessment) {
        User user = userService.getUserWithGroupsAndAuthorities();
        TextExercise textExercise = textExerciseService.findOne(exerciseId);
        checkTextExerciseForRequest(textExercise, user);

        final Optional<TextSubmission> optionalTextSubmission = textSubmissionRepository.findByResult_Id(resultId);
        if (optionalTextSubmission.isEmpty()) {
            throw new BadRequestAlertException("No text submission found for the given result.", "textSubmission", "textSubmissionNotFound");
        }

        StudentParticipation studentParticipation = (StudentParticipation) optionalTextSubmission.get().getParticipation();
        final var isAtLeastInstructor = authCheckService.isAtLeastInstructorForExercise(textExercise);

        if (!assessmentService.isAllowedToCreateOrOverrideResult(optionalTextSubmission.get().getResult(), textExercise, studentParticipation, user, isAtLeastInstructor)) {
            return forbidden("assessment", "assessmentSaveNotAllowed", "The user is not allowed to override the assessment");
        }

        saveTextBlocks(textAssessment.getTextBlocks(), optionalTextSubmission.get());
        Result result = textAssessmentService.submitAssessment(resultId, textExercise, textAssessment.getFeedbacks());
        studentParticipation = (StudentParticipation) result.getParticipation();
        if (studentParticipation.getExercise().getAssessmentDueDate() == null || studentParticipation.getExercise().getAssessmentDueDate().isBefore(ZonedDateTime.now())) {
            // TODO: we should send a result object here that includes the feedback (this might already be the case)
            messagingService.broadcastNewResult(studentParticipation, result);
        }

        if (!authCheckService.isAtLeastInstructorForExercise(textExercise, user)) {
            studentParticipation.filterSensitiveInformation();
        }

        // call feedback conflict service
        if (textExercise.isAutomaticAssessmentEnabled() && automaticTextAssessmentConflictService.isPresent()) {
            this.automaticTextAssessmentConflictService.get().asyncCheckFeedbackConsistency(textAssessment.getTextBlocks(), result.getFeedbacks(), exerciseId);
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Update an assessment after a complaint was accepted.
     *
     * @param submissionId     the id of the submission for which the assessment should be updated
     * @param assessmentUpdate the assessment update containing the new feedback items and the response to the complaint
     * @return the updated result
     */
    @ResponseStatus(HttpStatus.OK)
    @PutMapping("/text-submissions/{submissionId}/assessment-after-complaint")
    @PreAuthorize("hasAnyRole('TA', 'INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<Result> updateTextAssessmentAfterComplaint(@PathVariable Long submissionId, @RequestBody TextAssessmentUpdateDTO assessmentUpdate) {
        log.debug("REST request to update the assessment of submission {} after complaint.", submissionId);
        User user = userService.getUserWithGroupsAndAuthorities();
        TextSubmission textSubmission = textSubmissionService.findOneWithEagerResultFeedbackAndTextBlocks(submissionId);
        StudentParticipation studentParticipation = (StudentParticipation) textSubmission.getParticipation();
        long exerciseId = studentParticipation.getExercise().getId();
        TextExercise textExercise = textExerciseService.findOne(exerciseId);
        checkAuthorization(textExercise, user);
        saveTextBlocks(assessmentUpdate.getTextBlocks(), textSubmission);
        Result result = textAssessmentService.updateAssessmentAfterComplaint(textSubmission.getResult(), textExercise, assessmentUpdate);

        if (result.getParticipation() != null && result.getParticipation() instanceof StudentParticipation && !authCheckService.isAtLeastInstructorForExercise(textExercise)) {
            ((StudentParticipation) result.getParticipation()).setParticipant(null);
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Cancel an assessment of a given submission for the current user, i.e. delete the corresponding result / release the lock. Then the submission is available for assessment
     * again.
     *
     * @param submissionId the id of the submission for which the current assessment should be canceled
     * @param exerciseId the exerciseId of the exercise for which the assessment gets canceled
     * @return 200 Ok response if canceling was successful, 403 Forbidden if current user is not the assessor of the submission
     */
    @PutMapping("/exercise/{exerciseId}/submission/{submissionId}/cancel-assessment")
    @PreAuthorize("hasAnyRole('TA', 'INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<Void> cancelAssessment(@PathVariable Long exerciseId, @PathVariable Long submissionId) {
        return super.cancelAssessment(submissionId);
    }

    /**
     * Given an exerciseId and a submissionId, the method retrieves from the database all the data needed by the tutor to assess the submission. If the tutor has already started
     * assessing the submission, then we also return all the results the tutor has already inserted. If another tutor has already started working on this submission, the system
     * returns an error
     *
     * @param submissionId the id of the submission we want
     * @return a Participation of the tutor in the submission
     */
    @GetMapping("/submission/{submissionId}")
    @PreAuthorize("hasAnyRole('TA', 'INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<Participation> retrieveParticipationForSubmission(@PathVariable Long submissionId) {
        log.debug("REST request to get data for tutors text assessment submission: {}", submissionId);

        final Optional<TextSubmission> optionalTextSubmission = textSubmissionRepository.findByIdWithEagerParticipationExerciseResultAssessor(submissionId);
        if (optionalTextSubmission.isEmpty()) {
            return ResponseEntity.badRequest()
                    .headers(HeaderUtil.createFailureAlert(applicationName, true, "textSubmission", "textSubmissionNotFound", "No Submission was found for the given ID."))
                    .body(null);
        }

        final TextSubmission textSubmission = optionalTextSubmission.get();
        final Participation participation = textSubmission.getParticipation();
        final TextExercise exercise = (TextExercise) participation.getExercise();
        Result result = textSubmission.getResult();

        final User user = userService.getUserWithGroupsAndAuthorities();
        checkAuthorization(exercise, user);
        final boolean isAtLeastInstructorForExercise = authCheckService.isAtLeastInstructorForExercise(exercise, user);

        if (result != null && !isAtLeastInstructorForExercise && result.getAssessor() != null && !result.getAssessor().getLogin().equals(user.getLogin())
                && result.getCompletionDate() == null) {
            // If we already have a result, we need to check if it is locked.
            throw new BadRequestAlertException("This submission is being assessed by another tutor", ENTITY_NAME, "alreadyAssessed");
        }

        textAssessmentService.prepareSubmissionForAssessment(textSubmission);

        List<GradingCriterion> gradingCriteria = gradingCriterionService.findByExerciseIdWithEagerGradingCriteria(exercise.getId());
        exercise.setGradingCriteria(gradingCriteria);
        // Remove sensitive information of submission depending on user
        textSubmissionService.hideDetails(textSubmission, user);
        result = textSubmission.getResult();

        // Prepare for Response: Set Submissions and Results of Participation to include requested only.
        participation.setSubmissions(Set.of(textSubmission));
        participation.setResults(Set.of(result));

        // Remove Result from Submission, as it is send in participation.results[0]
        textSubmission.setResult(null);

        // Remove Submission from Result
        result.setSubmission(null);

        final ResponseEntity.BodyBuilder bodyBuilder = ResponseEntity.ok();
        final Result finalResult = result;

        // Add the jwt token as a header to the response for tutor-assessment tracking to the request if the athene profile is set
        this.atheneTrackingTokenProvider.ifPresent(atheneTrackingTokenProvider -> atheneTrackingTokenProvider.addTokenToResponseEntity(bodyBuilder, finalResult));
        return bodyBuilder.body(participation);
    }

    /**
     * Retrieve the result of an example assessment, only if the user is an instructor or if the submission is used for tutorial purposes.
     *
     * @param exerciseId   the id of the exercise
     * @param submissionId the id of the submission which must be connected to an example submission
     * @return the example result linked to the submission
     */
    // TODO: we should move this method up because it is independent of the exercise type
    @GetMapping("/exercise/{exerciseId}/submission/{submissionId}/example-result")
    @PreAuthorize("hasAnyRole('TA', 'INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<Result> getExampleResultForTutor(@PathVariable long exerciseId, @PathVariable long submissionId) {
        User user = userService.getUserWithGroupsAndAuthorities();
        log.debug("REST request to get example assessment for tutors text assessment: {}", submissionId);
        final var textExercise = textExerciseService.findOne(exerciseId);

        // If the user is not at least a tutor for this exercise, return error
        if (!authCheckService.isAtLeastTeachingAssistantForExercise(textExercise, user)) {
            return forbidden();
        }
        Submission submission = textAssessmentService.getSubmissionOfExampleSubmissionWithResult(submissionId);

        // If the user is not an instructor, and this is not an example submission used for tutorial, do not provide the results
        boolean isAtLeastInstructor = authCheckService.isAtLeastInstructorForExercise(textExercise, user);
        if (!submission.isExampleSubmission() && !isAtLeastInstructor) {
            return forbidden();
        }
        return ResponseEntity.ok(submission.getResult());
    }

    @Override
    String getEntityName() {
        return ENTITY_NAME;
    }

    /**
     * Checks if the given textExercise is valid and if the requester have the
     * required permissions
     * @param textExercise which needs to be checked
     * @throws BadRequestAlertException if no request was found
     */
    private void checkTextExerciseForRequest(@Nullable TextExercise textExercise, User user) {
        if (textExercise == null) {
            throw new BadRequestAlertException("No exercise was found for the given ID.", "textExercise", "exerciseNotFound");
        }

        validateExercise(textExercise);
        checkAuthorization(textExercise, user);
    }

    /**
     * Save TextBlocks received from Client (if present). We need to reference them to the submission first.
     * @param textBlocks received from Client
     * @param textSubmission to associate blocks with
     */
    private void saveTextBlocks(List<TextBlock> textBlocks, TextSubmission textSubmission) {
        if (textBlocks != null) {
            final Set<String> existingTextBlockIds = textSubmission.getBlocks().stream().map(TextBlock::getId).collect(toSet());
            textBlocks = textBlocks.stream().filter(tb -> !existingTextBlockIds.contains(tb.getId())).peek(tb -> {
                tb.setSubmission(textSubmission);
            }).collect(toList());
            textBlockRepository.saveAll(textBlocks);
        }
    }
}
