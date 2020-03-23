# Websockets used in Artemis

## Client -> Server
| Topic     | When |
|------------------|----|
| '${this.websocketResourceUrlSend}/files' | CodeEditorRepositoryFileService:updateFiles |
| '/topic/quizExercise/' + this.quizId + '/submission' | QuizParticipationComponent:onSubmit |


## Server -> Client
| Topic     | When |
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

### Subscriptions on Client:
| Topic     | When | Who |
|------------------|----|----|
| '/topic/tracker' | ngOnInit | Admin |
| '/user/topic/modelingSubmission/' + this.submission.id | subscribeToAutomaticSubmissionWebsocket | Student/Team of submission |
| '/topic/programming-exercise/${exerciseId}/test-cases' | initTestCaseSubscription (editing of test cases) | Instructor |
| '/topic/programming-exercises/${programmingExerciseId}/test-cases-changed' | initTestCaseStateSubscription | Instructor |
| '/topic/programming-exercises/%programmingExerciseId%/all-builds-triggered' | ProgrammingBuildRunService | Instructor (Trigger all builds) |
| '/topic/participation/%participationId%/newSubmission' | ProgrammingSubmissionService | Student/Team of participation |
| '${this.websocketResourceUrlReceive}/files' | CodeEditorRepositoryFileService | Instructor/TA |
| '/topic/statistic/' + params['exerciseId'] | DragAndDropQuestionStatisticComponent | Instructor/TA |
| '/topic/statistic/' + params['exerciseId'] | MultipleChoiceQuestionStatisticComponent | Instructor/TA |
| '/topic/statistic/' + params['exerciseId'] | QuizPointStatisticComponent | Instructor/TA |
| '/topic/statistic/' + params['exerciseId'] | QuizStatisticComponent | Instructor/TA |
| '/topic/statistic/' + params['exerciseId'] | ShortAnswerQuestionStatisticComponent | Instructor/TA |
| '/topic/quizExercise/' + params['exerciseId'] | QuizPointStatisticComponent | Instructor/TA |
| '/user/topic/quizExercise/' + this.quizId + '/submission' | QuizParticipationComponent | Student |
| '/user/topic/quizExercise/' + this.quizId + '/participation' | QuizParticipationComponent | Student |
| '/topic/quizExercise/' + this.quizId | QuizParticipationComponent | Student |
| '/topic/participation/${participationId}/newResults' | ParticipationWebsocketService | Student/Team of participation |
| '/topic/user/${user.id}/notifications' | NotificationService | User with ID |
| '/topic/course/${course.id}/${GroupNotificationType.STUDENT}' | NotificationService | Student |
| '/topic/course/${course.id}/${GroupNotificationType.INSTRUCTOR}' | NotificationService | Instructor |
| '/topic/course/${course.id}/${GroupNotificationType.TA}' | NotificationService | TA |
| '/topic/management/feature-toggles' | FeatureToggleService | Everyone |
| '/topic/system-notification' | SystemNotificationComponent | Everyone |
