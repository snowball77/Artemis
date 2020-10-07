package de.tum.in.www1.artemis.service;

import java.time.ZonedDateTime;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import de.tum.in.www1.artemis.domain.*;
import de.tum.in.www1.artemis.domain.enumeration.ComplaintType;
import de.tum.in.www1.artemis.domain.participation.StudentParticipation;
import de.tum.in.www1.artemis.repository.ComplaintRepository;
import de.tum.in.www1.artemis.repository.ComplaintResponseRepository;
import de.tum.in.www1.artemis.web.rest.ComplaintResponseResource;
import de.tum.in.www1.artemis.web.rest.errors.AccessForbiddenException;
import de.tum.in.www1.artemis.web.rest.errors.BadRequestAlertException;

/**
 * Service for managing complaint responses.
 */
@Service
public class ComplaintResponseService {

    private final Logger log = LoggerFactory.getLogger(ComplaintResponseResource.class);

    private static final String ENTITY_NAME = "complaintResponse";

    private ComplaintRepository complaintRepository;

    private ComplaintResponseRepository complaintResponseRepository;

    private UserService userService;

    private AuthorizationCheckService authorizationCheckService;

    public ComplaintResponseService(ComplaintRepository complaintRepository, ComplaintResponseRepository complaintResponseRepository, UserService userService,
            AuthorizationCheckService authorizationCheckService) {
        this.complaintRepository = complaintRepository;
        this.complaintResponseRepository = complaintResponseRepository;
        this.userService = userService;
        this.authorizationCheckService = authorizationCheckService;
    }

    /**
     * Create a new complaint response and update the 'accepted' flag of the corresponding complaint.
     *
     * @param complaintResponse the complaint response to create
     * @return the saved complaint response
     */
    public ComplaintResponse createComplaintResponse(ComplaintResponse complaintResponse) {
        if (complaintResponse.getId() != null) {
            throw new BadRequestAlertException("A new complaint response cannot already have an id", ENTITY_NAME, "idexists");
        }
        if (complaintResponse.getComplaint() == null || complaintResponse.getComplaint().getId() == null) {
            throw new BadRequestAlertException("A complaint response can be only associated to a complaint", ENTITY_NAME, "noresultid");
        }

        Long complaintId = complaintResponse.getComplaint().getId();

        // Do not trust user input
        Optional<Complaint> originalComplaintOptional = complaintRepository.findByIdWithEagerAssessor(complaintId);
        if (originalComplaintOptional.isEmpty()) {
            throw new BadRequestAlertException("The complaint you are referring to does not exist", ENTITY_NAME, "noresult");
        }

        Complaint originalComplaint = originalComplaintOptional.get();

        if (complaintResponseRepository.findByComplaint_Id(originalComplaint.getId()).isPresent()) {
            throw new BadRequestAlertException("The complaint you are referring to does already have a response", ENTITY_NAME, "complaintresponseexists");
        }

        Result originalResult = originalComplaint.getResult();
        User assessor = originalResult.getAssessor();
        User reviewer = this.userService.getUser();

        StudentParticipation studentParticipation = (StudentParticipation) originalResult.getParticipation();
        if (!isUserAuthorizedToRespondToComplaint(studentParticipation, originalComplaint, assessor, reviewer)) {
            throw new AccessForbiddenException("Insufficient permission for creating a complaint response");
        }

        originalComplaint.setAccepted(complaintResponse.getComplaint().isAccepted());
        originalComplaint = complaintRepository.save(originalComplaint);

        complaintResponse.setSubmittedTime(ZonedDateTime.now());
        complaintResponse.setReviewer(reviewer);
        // make sure the original complaint from the database is connected to the complaint response as we take it out later one and
        // potential changes on the client side (e.g. remove student id) should not be saved
        complaintResponse.setComplaint(originalComplaint);
        return complaintResponseRepository.save(complaintResponse);
    }

    /**
     * Checks whether the reviewer is authorized to respond to this complaint
     *
     * 1. Team Exercises
     *    => The team tutor assesses the submissions and responds to complaints and more feedback requests
     *
     * 2. Individual Exercises
     *    => Complaints can only be handled by a tutor who is not the original assessor
     *    => Complaints of exam test runs can be assessed by instructors. They are identified by the same user being the assessor and student
     *    => More feedback requests are handled by the assessor himself
     *
     * @param participation Participation to which the complaint belongs to
     * @param complaint Complaint for which to check
     * @param assessor Assessor of the submission whose assessment was complained about
     * @param reviewer Reviewer who is trying to create a response to the complaint
     * @return true if the tutor is allowed to respond to the complaint, false otherwise
     */
    private boolean isUserAuthorizedToRespondToComplaint(StudentParticipation participation, Complaint complaint, User assessor, User reviewer) {
        if (!authorizationCheckService.isAtLeastTeachingAssistantForExercise(participation.getExercise())) {
            return false;
        }
        if (participation.getParticipant() instanceof Team) {
            return assessor.equals(reviewer);
        }
        else if (complaint.getComplaintType() == null || complaint.getComplaintType().equals(ComplaintType.COMPLAINT)) {
            // if test run complaint
            if (complaint.getStudent() != null && complaint.getStudent().getLogin().equals(assessor.getLogin())
                    && authorizationCheckService.isAtLeastInstructorForExercise(participation.getExercise())) {
                return true;
            }
            return !assessor.equals(reviewer);
        }
        else if (complaint.getComplaintType() != null && complaint.getComplaintType().equals(ComplaintType.MORE_FEEDBACK)) {
            return assessor.equals(reviewer);
        }
        return false;
    }
}
