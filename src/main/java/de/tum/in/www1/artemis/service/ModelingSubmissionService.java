package de.tum.in.www1.artemis.service;

import static java.util.stream.Collectors.toList;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import de.tum.in.www1.artemis.domain.Result;
import de.tum.in.www1.artemis.domain.Submission;
import de.tum.in.www1.artemis.domain.User;
import de.tum.in.www1.artemis.domain.enumeration.AssessmentType;
import de.tum.in.www1.artemis.domain.enumeration.InitializationState;
import de.tum.in.www1.artemis.domain.enumeration.SubmissionType;
import de.tum.in.www1.artemis.domain.modeling.ModelingExercise;
import de.tum.in.www1.artemis.domain.modeling.ModelingSubmission;
import de.tum.in.www1.artemis.domain.participation.StudentParticipation;
import de.tum.in.www1.artemis.repository.ModelingSubmissionRepository;
import de.tum.in.www1.artemis.repository.ResultRepository;
import de.tum.in.www1.artemis.repository.StudentParticipationRepository;
import de.tum.in.www1.artemis.repository.SubmissionRepository;
import de.tum.in.www1.artemis.service.compass.CompassService;
import de.tum.in.www1.artemis.web.rest.errors.EntityNotFoundException;

@Service
public class ModelingSubmissionService extends SubmissionService {

    private final Logger log = LoggerFactory.getLogger(ModelingSubmissionService.class);

    private final ModelingSubmissionRepository modelingSubmissionRepository;

    private final CompassService compassService;

    private final SubmissionVersionService submissionVersionService;

    public ModelingSubmissionService(ModelingSubmissionRepository modelingSubmissionRepository, SubmissionRepository submissionRepository, ResultRepository resultRepository,
            CompassService compassService, UserService userService, SubmissionVersionService submissionVersionService, ParticipationService participationService,
            StudentParticipationRepository studentParticipationRepository, AuthorizationCheckService authCheckService, CourseService courseService, ExamService examService) {
        super(submissionRepository, userService, authCheckService, courseService, resultRepository, examService, studentParticipationRepository, participationService);
        this.modelingSubmissionRepository = modelingSubmissionRepository;
        this.resultRepository = resultRepository;
        this.compassService = compassService;
        this.submissionVersionService = submissionVersionService;
    }

    /**
     * Given an exerciseId, returns all the modeling submissions for that exercise, including their results. Submissions can be filtered to include only already submitted
     * submissions
     *
     * @param exerciseId    - the id of the exercise we are interested into
     * @param submittedOnly - if true, it returns only submission with submitted flag set to true
     * @param examMode - set flag to ignore exam test run submissions
     * @return a list of modeling submissions for the given exercise id
     */
    @Transactional(readOnly = true)
    public List<ModelingSubmission> getModelingSubmissions(Long exerciseId, boolean submittedOnly, boolean examMode) {
        List<StudentParticipation> participations;
        if (examMode) {
            participations = studentParticipationRepository.findAllWithEagerSubmissionsAndEagerResultsAndEagerAssessorByExerciseIdIgnoreTestRuns(exerciseId);
        }
        else {
            participations = studentParticipationRepository.findAllWithEagerSubmissionsAndEagerResultsAndEagerAssessorByExerciseId(exerciseId);
        }
        List<ModelingSubmission> submissions = new ArrayList<>();
        for (StudentParticipation participation : participations) {
            Optional<Submission> optionalSubmission = participation.findLatestSubmission();
            if (optionalSubmission.isPresent()) {
                if (submittedOnly && !optionalSubmission.get().isSubmitted()) {
                    // filter out non submitted submissions if the flag is set to true
                    continue;
                }
                submissions.add((ModelingSubmission) optionalSubmission.get());
            }
            // avoid infinite recursion
            participation.getExercise().setStudentParticipations(null);
        }
        return submissions;
    }

    /**
     * Get the modeling submission with the given ID from the database and lock the submission to prevent other tutors from receiving and assessing it. Additionally, check if the
     * submission lock limit has been reached.
     *
     * @param submissionId     the id of the modeling submission
     * @param modelingExercise the corresponding exercise
     * @return the locked modeling submission
     */
    @Transactional
    public ModelingSubmission getLockedModelingSubmission(Long submissionId, ModelingExercise modelingExercise) {
        ModelingSubmission modelingSubmission = findOneWithEagerResultAndFeedbackAndAssessorAndParticipationResults(submissionId);

        if (modelingSubmission.getResult() == null || modelingSubmission.getResult().getAssessor() == null) {
            checkSubmissionLockLimit(modelingExercise.getCourseViaExerciseGroupOrCourseMember().getId());
            modelingSubmission = assignAutomaticResultToSubmission(modelingSubmission);
        }

        lockSubmission(modelingSubmission, modelingExercise);
        return modelingSubmission;
    }

    /**
     * Get a modeling submission of the given exercise that still needs to be assessed, assign the automatic result of Compass to it and lock the submission to prevent other tutors from receiving and assessing it.
     *
     * @param modelingExercise the exercise the submission should belong to
     * @param removeTestRunParticipations flag to determine if test runs should be removed. This should be set to true for exam exercises
     * @return a locked modeling submission that needs an assessment
     */
    @Transactional
    public ModelingSubmission lockModelingSubmissionWithoutResult(ModelingExercise modelingExercise, boolean removeTestRunParticipations) {
        ModelingSubmission modelingSubmission = getRandomModelingSubmissionEligibleForNewAssessment(modelingExercise, removeTestRunParticipations)
                .orElseThrow(() -> new EntityNotFoundException("Modeling submission for exercise " + modelingExercise.getId() + " could not be found"));
        modelingSubmission = assignAutomaticResultToSubmission(modelingSubmission);
        lockSubmission(modelingSubmission, modelingExercise);
        return modelingSubmission;
    }

    /**
     * Given an exercise, find a modeling submission for that exercise which still doesn't have a manual result. If the diagram type is supported by Compass we get the next optimal
     * submission from Compass, i.e. the submission for which an assessment means the most knowledge gain for the automatic assessment mechanism. If it's not supported by Compass
     * we just get a random submission without assessment. If there is no submission without manual result we return an empty optional. Note, that we cannot use a readonly
     * transaction here as it is making problems when initially loading the calculation engine and assessing all submissions automatically: we would get an sql exception
     * "Connection is read-only" from hibernate when saving the result in CompassService#assessAutomatically.
     *
     * @param modelingExercise the modeling exercise for which we want to get a modeling submission without result
     * @param examMode flag to determine if test runs should be removed. This should be set to true for exam exercises
     * @return a modeling submission without any result
     */
    @Transactional
    public Optional<ModelingSubmission> getRandomModelingSubmissionEligibleForNewAssessment(ModelingExercise modelingExercise, boolean examMode) {
        // if the diagram type is supported by Compass, ask Compass for optimal (i.e. most knowledge gain for automatic assessments) submissions to assess next
        if (compassService.isSupported(modelingExercise)) {
            List<Long> modelsWaitingForAssessment = compassService.getModelsWaitingForAssessment(modelingExercise.getId());

            // shuffle the model list to prevent that the user gets the same submission again after canceling an assessment
            Collections.shuffle(modelsWaitingForAssessment);

            for (Long submissionId : modelsWaitingForAssessment) {
                Optional<ModelingSubmission> submission = modelingSubmissionRepository.findWithEagerResultAndFeedbackAndAssessorAndParticipationResultsById(submissionId);
                if (submission.isPresent()) {
                    return submission;
                }
                else {
                    compassService.removeModelWaitingForAssessment(modelingExercise.getId(), submissionId);
                }
            }
        }

        var submissionWithoutResult = super.getRandomSubmissionEligibleForNewAssessment(modelingExercise, examMode);
        if (submissionWithoutResult.isPresent()) {
            ModelingSubmission modelingSubmission = (ModelingSubmission) submissionWithoutResult.get();
            return Optional.of(modelingSubmission);
        }
        return Optional.empty();
    }

    /**
     * Given an exercise id and a tutor id, it returns all the modeling submissions where the tutor has a result associated.
     *
     * @param exerciseId - the id of the exercise we are looking for
     * @param tutor - the tutor we are interested in
     * @param examMode - flag should be set to ignore the test run submissions
     * @return a list of modeling submissions
     */
    public List<ModelingSubmission> getAllModelingSubmissionsAssessedByTutorForExercise(Long exerciseId, User tutor, boolean examMode) {
        var submissions = super.getAllSubmissionsAssessedByTutorForExercise(exerciseId, tutor, examMode);
        return submissions.stream().map(submission -> (ModelingSubmission) submission).collect(toList());
    }

    /**
     * Saves the given submission and the corresponding model and creates the result if necessary. This method used for creating and updating modeling submissions. If it is used
     * for a submit action, Compass is notified about the new model. Rolls back if inserting fails - occurs for concurrent createModelingSubmission() calls.
     *
     * @param modelingSubmission the submission that should be saved
     * @param modelingExercise   the exercise the submission belongs to
     * @param username           the name of the corresponding user
     * @return the saved modelingSubmission entity
     */
    public ModelingSubmission save(ModelingSubmission modelingSubmission, ModelingExercise modelingExercise, String username) {
        Optional<StudentParticipation> optionalParticipation = participationService.findOneByExerciseAndStudentLoginWithEagerSubmissionsAnyState(modelingExercise, username);
        if (optionalParticipation.isEmpty()) {
            throw new EntityNotFoundException("No participation found for " + username + " in exercise with id " + modelingExercise.getId());
        }
        StudentParticipation participation = optionalParticipation.get();

        final var exerciseDueDate = modelingExercise.getDueDate();
        if (exerciseDueDate != null && exerciseDueDate.isBefore(ZonedDateTime.now()) && participation.getInitializationDate().isBefore(exerciseDueDate)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        // remove result from submission (in the unlikely case it is passed here), so that students cannot inject a result
        modelingSubmission.setResult(null);

        // update submission properties
        // NOTE: from now on we always set submitted to true to prevent problems here!
        modelingSubmission.setSubmitted(true);
        modelingSubmission.setSubmissionDate(ZonedDateTime.now());
        modelingSubmission.setType(SubmissionType.MANUAL);
        modelingSubmission.setParticipation(participation);
        modelingSubmission = modelingSubmissionRepository.save(modelingSubmission);

        // versioning of submission
        try {
            if (modelingExercise.isTeamMode()) {
                submissionVersionService.saveVersionForTeam(modelingSubmission, username);
            }
            else {
                submissionVersionService.saveVersionForIndividual(modelingSubmission, username);
            }
        }
        catch (Exception ex) {
            log.error("Modeling submission version could not be saved: " + ex);
        }

        participation.addSubmissions(modelingSubmission);

        try {
            notifyCompass(modelingSubmission, modelingExercise);
        }
        catch (Exception ex) {
            log.warn("There was an exception when notifying Compass about a new modeling submission with error message: " + ex.getMessage()
                    + ". Artemis will ignore this error and continue to save the modeling submission", ex);
        }
        participation.setInitializationState(InitializationState.FINISHED);

        StudentParticipation savedParticipation = studentParticipationRepository.save(participation);
        if (modelingSubmission.getId() == null) {
            Optional<Submission> optionalSubmission = savedParticipation.findLatestSubmission();
            if (optionalSubmission.isPresent()) {
                modelingSubmission = (ModelingSubmission) optionalSubmission.get();
            }
        }

        log.debug("return model: " + modelingSubmission.getModel());
        return modelingSubmission;
    }

    /**
     * Soft lock the submission to prevent other tutors from receiving and assessing it. We remove the model from the models waiting for assessment in Compass to prevent other
     * tutors from retrieving it in the first place. Additionally, we set the assessor and save the result to soft lock the assessment in the client, i.e. the client will not allow
     * tutors to assess a model when an assessor is already assigned. If no result exists for this submission we create one first.
     *
     * @param modelingSubmission the submission to lock
     * @param modelingExercise   the exercise to which the submission belongs to (needed for Compass)
     */
    private void lockSubmission(ModelingSubmission modelingSubmission, ModelingExercise modelingExercise) {
        var result = lockSubmission(modelingSubmission);
        if (result.getAssessor() == null && compassService.isSupported(modelingExercise)) {
            compassService.removeModelWaitingForAssessment(modelingExercise.getId(), modelingSubmission.getId());
        }
    }

    /**
     * Assigns an automatic result generated by Compass to the given modeling submission and saves the updated submission to the database. If the given submission already contains
     * a manual result, it will not get updated with the automatic result.
     *
     * @param modelingSubmission the modeling submission that should be updated with an automatic result generated by Compass
     * @return the updated modeling submission
     */
    private ModelingSubmission assignAutomaticResultToSubmission(ModelingSubmission modelingSubmission) {
        Result existingResult = modelingSubmission.getResult();
        if (existingResult != null && existingResult.getAssessmentType() != null && existingResult.getAssessmentType().equals(AssessmentType.MANUAL)) {
            return modelingSubmission;
        }
        StudentParticipation studentParticipation = (StudentParticipation) modelingSubmission.getParticipation();
        long exerciseId = studentParticipation.getExercise().getId();
        Result automaticResult = compassService.getAutomaticResultForSubmission(modelingSubmission.getId(), exerciseId);
        if (automaticResult != null) {
            automaticResult.setSubmission(modelingSubmission);
            modelingSubmission.setResult(automaticResult);
            modelingSubmission.getParticipation().addResult(automaticResult);
            modelingSubmission = modelingSubmissionRepository.save(modelingSubmission);
            resultRepository.save(automaticResult);

            compassService.removeAutomaticResultForSubmission(modelingSubmission.getId(), exerciseId);
        }

        return modelingSubmission;
    }

    /**
     * Adds a model to compass service to include it in the automatic grading process.
     *
     * @param modelingSubmission the submission which contains the model
     * @param modelingExercise   the exercise the submission belongs to
     */
    public void notifyCompass(ModelingSubmission modelingSubmission, ModelingExercise modelingExercise) {
        if (compassService.isSupported(modelingExercise)) {
            this.compassService.addModel(modelingExercise.getId(), modelingSubmission.getId(), modelingSubmission.getModel());
        }
    }

    /**
     * Get the modeling submission with the given id from the database. Throws an EntityNotFoundException if no submission could be found for the given id.
     *
     * @param submissionId the id of the submission that should be loaded from the database
     * @return the modeling submission with the given id
     */
    public ModelingSubmission findOne(Long submissionId) {
        return modelingSubmissionRepository.findById(submissionId)
                .orElseThrow(() -> new EntityNotFoundException("Modeling submission with id \"" + submissionId + "\" does not exist"));
    }

    /**
     * Get the modeling submission with the given id from the database. The submission is loaded together with its result and the assessor. Throws an EntityNotFoundException if no
     * submission could be found for the given id.
     *
     * @param submissionId the id of the submission that should be loaded from the database
     * @return the modeling submission with the given id
     */
    public ModelingSubmission findOneWithEagerResult(Long submissionId) {
        return modelingSubmissionRepository.findByIdWithEagerResult(submissionId)
                .orElseThrow(() -> new EntityNotFoundException("Modeling submission with id \"" + submissionId + "\" does not exist"));
    }

    /**
     * Get the modeling submission with the given id from the database. The submission is loaded together with its result, the feedback of the result and the assessor of the
     * result. Throws an EntityNotFoundException if no submission could be found for the given id.
     *
     * @param submissionId the id of the submission that should be loaded from the database
     * @return the modeling submission with the given id
     */
    public ModelingSubmission findOneWithEagerResultAndFeedback(Long submissionId) {
        return modelingSubmissionRepository.findByIdWithEagerResultAndFeedback(submissionId)
                .orElseThrow(() -> new EntityNotFoundException("Modeling submission with id \"" + submissionId + "\" does not exist"));
    }

    /**
     * Get the modeling submission with the given id from the database. The submission is loaded together with its result, the feedback of the result, the assessor of the result,
     * its participation and all results of the participation. Throws an EntityNotFoundException if no submission could be found for the given id.
     *
     * @param submissionId the id of the submission that should be loaded from the database
     * @return the modeling submission with the given id
     */
    private ModelingSubmission findOneWithEagerResultAndFeedbackAndAssessorAndParticipationResults(Long submissionId) {
        return modelingSubmissionRepository.findWithEagerResultAndFeedbackAndAssessorAndParticipationResultsById(submissionId)
                .orElseThrow(() -> new EntityNotFoundException("Modeling submission with id \"" + submissionId + "\" does not exist"));
    }
}
