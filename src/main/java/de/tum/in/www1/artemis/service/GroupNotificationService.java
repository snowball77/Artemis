package de.tum.in.www1.artemis.service;

import static de.tum.in.www1.artemis.domain.notification.GroupNotificationFactory.createNotification;

import java.time.ZonedDateTime;

import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

import de.tum.in.www1.artemis.domain.Attachment;
import de.tum.in.www1.artemis.domain.Exercise;
import de.tum.in.www1.artemis.domain.StudentQuestion;
import de.tum.in.www1.artemis.domain.StudentQuestionAnswer;
import de.tum.in.www1.artemis.domain.enumeration.GroupNotificationType;
import de.tum.in.www1.artemis.domain.enumeration.NotificationType;
import de.tum.in.www1.artemis.domain.notification.GroupNotification;
import de.tum.in.www1.artemis.repository.GroupNotificationRepository;

@Service
public class GroupNotificationService {

    private final GroupNotificationRepository groupNotificationRepository;

    private final SimpMessageSendingOperations messagingTemplate;

    private final UserService userService;

    public GroupNotificationService(GroupNotificationRepository groupNotificationRepository, SimpMessageSendingOperations messagingTemplate, UserService userService) {
        this.groupNotificationRepository = groupNotificationRepository;
        this.messagingTemplate = messagingTemplate;
        this.userService = userService;
    }

    /**
     * Notify student groups about an attachment change.
     *
     * @param attachment that has been changed
     * @param notificationText that should be displayed
     */
    public void notifyStudentGroupAboutAttachmentChange(Attachment attachment, String notificationText) {
        // Do not send a notification before the release date of the attachment.
        if (attachment.getReleaseDate() != null && attachment.getReleaseDate().isAfter(ZonedDateTime.now())) {
            return;
        }
        // Create and send the notification.
        saveAndSend(createNotification(attachment, userService.getUser(), GroupNotificationType.STUDENT, NotificationType.ATTACHMENT_CHANGE, notificationText));
    }

    /**
     * Notify students groups about an exercise opened for practice.
     *
     * @param exercise that has been opened for practice
     */
    public void notifyStudentGroupAboutExercisePractice(Exercise exercise) {
        saveAndSend(createNotification(exercise, userService.getUser(), GroupNotificationType.STUDENT, NotificationType.EXERCISE_PRACTICE, null));
    }

    /**
     * Notify student groups about an exercise started.
     *
     * @param exercise that has been started
     */
    public void notifyStudentGroupAboutExerciseStart(Exercise exercise) {
        saveAndSend(createNotification(exercise, userService.getUser(), GroupNotificationType.STUDENT, NotificationType.EXERCISE_STARTED, null));
    }

    /**
     * Notify student groups about an exercise update.
     *
     * @param exercise that has been updated
     * @param notificationText that should be displayed
     */
    public void notifyStudentGroupAboutExerciseUpdate(Exercise exercise, String notificationText) {
        // Do not send a notification before the release date of the exercise.
        if (exercise.getReleaseDate() != null && exercise.getReleaseDate().isAfter(ZonedDateTime.now())) {
            return;
        }
        // Create and send the notification.
        saveAndSend(createNotification(exercise, userService.getUser(), GroupNotificationType.STUDENT, NotificationType.EXERCISE_UPDATED, notificationText));
    }

    /**
     * Notify tutor groups about the creation of an exercise.
     *
     * @param exercise that has been created
     */
    public void notifyTutorGroupAboutExerciseCreated(Exercise exercise) {
        saveAndSend(createNotification(exercise, userService.getUser(), GroupNotificationType.TA, NotificationType.EXERCISE_CREATED, null));
    }

    /**
     * Notify instructor groups about an exercise update.
     *
     * @param exercise that has been updated
     * @param notificationText that should be displayed
     */
    public void notifyInstructorGroupAboutExerciseUpdate(Exercise exercise, String notificationText) {
        saveAndSend(createNotification(exercise, null, GroupNotificationType.INSTRUCTOR, NotificationType.EXERCISE_UPDATED, notificationText));
    }

    /**
     * Notify tutor and instructor groups about a new question in an exercise.
     *
     * @param studentQuestion that has been posted
     */
    public void notifyTutorAndInstructorGroupAboutNewQuestionForExercise(StudentQuestion studentQuestion) {
        saveAndSend(createNotification(studentQuestion, userService.getUser(), GroupNotificationType.TA, NotificationType.NEW_QUESTION_FOR_EXERCISE));
        saveAndSend(createNotification(studentQuestion, userService.getUser(), GroupNotificationType.INSTRUCTOR, NotificationType.NEW_QUESTION_FOR_EXERCISE));
    }

    /**
     * Notify tutor and instructor groups about a new question in a lecture.
     *
     * @param studentQuestion that has been posted
     */
    public void notifyTutorAndInstructorGroupAboutNewQuestionForLecture(StudentQuestion studentQuestion) {
        saveAndSend(createNotification(studentQuestion, userService.getUser(), GroupNotificationType.TA, NotificationType.NEW_QUESTION_FOR_LECTURE));
        saveAndSend(createNotification(studentQuestion, userService.getUser(), GroupNotificationType.INSTRUCTOR, NotificationType.NEW_QUESTION_FOR_LECTURE));
    }

    /**
     * Notify tutor and instructor groups about a new answer for an exercise.
     *
     * @param studentQuestionAnswer that has been submitted for a question
     */
    public void notifyTutorAndInstructorGroupAboutNewAnswerForExercise(StudentQuestionAnswer studentQuestionAnswer) {
        saveAndSend(createNotification(studentQuestionAnswer, userService.getUser(), GroupNotificationType.TA, NotificationType.NEW_ANSWER_FOR_EXERCISE));
        saveAndSend(createNotification(studentQuestionAnswer, userService.getUser(), GroupNotificationType.INSTRUCTOR, NotificationType.NEW_ANSWER_FOR_EXERCISE));
    }

    /**
     * Notify tutor and instructor groups about a new answer for a lecture.
     *
     * @param studentQuestionAnswer that has been submitted for a question
     */
    public void notifyTutorAndInstructorGroupAboutNewAnswerForLecture(StudentQuestionAnswer studentQuestionAnswer) {
        saveAndSend(createNotification(studentQuestionAnswer, userService.getUser(), GroupNotificationType.TA, NotificationType.NEW_ANSWER_FOR_LECTURE));
        saveAndSend(createNotification(studentQuestionAnswer, userService.getUser(), GroupNotificationType.INSTRUCTOR, NotificationType.NEW_ANSWER_FOR_LECTURE));
    }

    /**
     * Saves the given notification in database and sends it to the client via websocket.
     *
     * @param notification that should be saved and sent
     */
    private void saveAndSend(GroupNotification notification) {
        groupNotificationRepository.save(notification);
        messagingTemplate.convertAndSend(notification.getTopic(), notification);
    }
}
