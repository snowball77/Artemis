# Websockets used in Artemis

## Client -> Server
| Topic     | When |
|------------------|----|
| `${this.websocketResourceUrlSend}/files` | CodeEditorRepositoryFileService:updateFiles |
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
| Topic     | When |
|------------------|----|
| '/topic/tracker' | ngOnInit |
| '/user/topic/modelingSubmission/' + this.submission.id | subscribeToAutomaticSubmissionWebsocket |
| `/topic/programming-exercise/${exerciseId}/test-cases` | initTestCaseSubscription |
| `/topic/programming-exercises/${programmingExerciseId}/test-cases-changed` | initTestCaseStateSubscription |
| '/topic/programming-exercises/%programmingExerciseId%/all-builds-triggered' | ProgrammingBuildRunService |
| '/topic/participation/%participationId%/newSubmission' | ProgrammingSubmissionService |
| `${this.websocketResourceUrlReceive}/files` | CodeEditorRepositoryFileService |
| '/topic/statistic/' + params['exerciseId'] | DragAndDropQuestionStatisticComponent |
| '/topic/statistic/' + params['exerciseId'] | MultipleChoiceQuestionStatisticComponent |
| '/topic/statistic/' + params['exerciseId'] | QuizPointStatisticComponent |
| '/topic/quizExercise/' + params['exerciseId'] | QuizPointStatisticComponent |
| '/topic/statistic/' + params['exerciseId'] | QuizStatisticComponent |
| '/topic/statistic/' + params['exerciseId'] | ShortAnswerQuestionStatisticComponent |
| '/user/topic/quizExercise/' + this.quizId + '/submission' | QuizParticipationComponent |
| '/user/topic/quizExercise/' + this.quizId + '/participation' | QuizParticipationComponent |
| '/topic/quizExercise/' + this.quizId | QuizParticipationComponent |
| `/topic/participation/${participationId}/newResults` | ParticipationWebsocketService |
| `/topic/user/${user.id}/notifications` | NotificationService |
| `/topic/course/${course.id}/${GroupNotificationType.STUDENT}` | NotificationService |
| `/topic/course/${course.id}/${GroupNotificationType.INSTRUCTOR}` | NotificationService |
| `/topic/course/${course.id}/${GroupNotificationType.TA}` | NotificationService |
| `/topic/management/feature-toggles` | FeatureToggleService |
| '/topic/system-notification' | SystemNotificationComponent |