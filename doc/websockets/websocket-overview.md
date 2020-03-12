# Websockets used in Artemis

## Client -> Server

### Client -> Server -> Client


## Server -> Client
 Topic     | When |
|------------------|----|
| "/topic/exercise/" + quizExerciseId + "/participation" | sendQuizResultToUser |
| "/topic/quizExercise/" + exerciseId + "/submission" | saveSubmission |
| "/topic/repository/" + participationId + "/files" | updateParticipationFiles |
| groupNotification.getTopic() | GroupNotificationService |
| userNotification.getTopic() | SingleUserNotificationService |
| "/topic/system-notification" | SystemNotificationService |
| "/topic/tracker" | ActivityService |
| "/topic/statistic/" + quiz.getId() | QuizStatisticService |
| "/topic/participation/" + participation.getId() + "/newResults" | WebsocketMessagingService |
| "/topic/programming-exercise/" + exercise.getId() + "/test-cases" | ResultService |
| submission | ProgrammingSubmissionService |
