package de.tum.in.www1.artemis.config;

import java.util.regex.Pattern;

/**
 * Application constants.
 */
public final class Constants {

    // Regex for acceptable logins
    public static final String LOGIN_REGEX = "^[_'.@A-Za-z0-9-]*$";

    public static final String SYSTEM_ACCOUNT = "system";

    public static final String DEFAULT_LANGUAGE = "en";

    public static final int QUIZ_GRACE_PERIOD_IN_SECONDS = 5;

    public static final String FILEPATH_ID_PLACHEOLDER = "PLACEHOLDER_FOR_ID";

    public static final String EXERCISE_TOPIC_ROOT = "/topic/exercise/";

    public static final String NEW_RESULT_TOPIC = "/topic/newResults";

    public static final String NEW_RESULT_RESOURCE_PATH = "/programming-exercises/new-result";

    public static final String NEW_RESULT_RESOURCE_API_PATH = "/api" + NEW_RESULT_RESOURCE_PATH;

    public static final String TEST_CASE_CHANGED_PATH = "/programming-exercises/test-cases-changed/";

    public static final String TEST_CASE_CHANGED_API_PATH = "/api" + TEST_CASE_CHANGED_PATH;

    public static final String PROGRAMMING_SUBMISSION_RESOURCE_PATH = "/programming-submissions/";

    public static final String PROGRAMMING_SUBMISSION_RESOURCE_API_PATH = "/api" + PROGRAMMING_SUBMISSION_RESOURCE_PATH;

    public static final String SYSTEM_NOTIFICATIONS_RESOURCE_PATH = "/system-notifications/";

    public static final String SYSTEM_NOTIFICATIONS_RESOURCE_PATH_ACTIVE_API_PATH = "/api" + SYSTEM_NOTIFICATIONS_RESOURCE_PATH + "active-notification";

    public static final String PROGRAMMING_SUBMISSION_TOPIC = "/newSubmissions";

    public static final String NEW_SUBMISSION_TOPIC = "/topic" + PROGRAMMING_SUBMISSION_TOPIC;

    // short names should have at least 3 characters and must start with a letter
    public static final String SHORT_NAME_REGEX = "^[a-zA-Z][a-zA-Z0-9]{2,}";

    public static final Pattern SHORT_NAME_PATTERN = Pattern.compile(SHORT_NAME_REGEX);

    public static final String FILE_ENDING_REGEX = "^[a-zA-Z0-9]{1,5}";

    public static final Pattern FILE_ENDING_PATTERN = Pattern.compile(FILE_ENDING_REGEX);

    public static final String TUM_USERNAME_REGEX = "^([a-z]{2}\\d{2}[a-z]{3})";

    public static final Pattern TUM_USERNAME_PATTERN = Pattern.compile(TUM_USERNAME_REGEX);

    public static final Pattern TITLE_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_\\-\\s]*");

    public static final String TUM_LDAP_MATRIKEL_NUMBER = "imMatrikelNr";

    public static final double COMPASS_SCORE_EQUALITY_THRESHOLD = 0.0001;

    // NOTE: the following values for programming exercises are hard-coded at the moment
    public static final String TEST_REPO_NAME = "tests";

    public static final String ASSIGNMENT_REPO_NAME = "assignment";

    // Used to cut off CI specific path segments when receiving static code analysis reports
    public static final String ASSIGNMENT_DIRECTORY = "/" + ASSIGNMENT_REPO_NAME + "/";

    // Used as a value for <sourceDirectory> for the Java template pom.xml
    public static final String STUDENT_WORKING_DIRECTORY = ASSIGNMENT_DIRECTORY + "src";

    public static final long MAX_NUMBER_OF_LOCKED_SUBMISSIONS_PER_TUTOR = 10;

    public static final long MAX_UPLOAD_FILESIZE_BYTES = 2 * 1024 * 1024; // 2 MiB

    public static final String TEST_CASES_CHANGED_NOTIFICATION = "The test cases of this programming exercise were updated. The student submissions should be build and tested so that results with the updated settings can be created.";

    public static final String TEST_CASES_CHANGED_RUN_COMPLETED_NOTIFICATION = "Build and Test run complete. New results were created for the programming exercise's student submissions with the updated test case settings.";

    public static final String BUILD_RUN_STARTED_FOR_PROGRAMMING_EXERCISE = "Build run triggered for programming exercise";

    public static final String BUILD_RUN_COMPLETE_FOR_PROGRAMMING_EXERCISE = "All builds triggered for programming exercise";

    public static final String PROGRAMMING_EXERCISE_FAILED_LOCK_OPERATIONS_NOTIFICATION = "The due date of this programming exercise has passed. When removing the write permissions for the student repositories, not all operations were successful. Number of failed operations: ";

    public static final String PROGRAMMING_EXERCISE_SUCCESSFUL_LOCK_OPERATION_NOTIFICATION = "The student repositories for this programming exercise were locked successfully when the due date passed.";

    public static final String PROGRAMMING_EXERCISE_FAILED_UNLOCK_OPERATIONS_NOTIFICATION = "The visible date of the exam of the programming exercise has passed. When adding the write permissions for the student repositories, not all operations were successful. Number of failed operations: ";

    public static final String PROGRAMMING_EXERCISE_SUCCESSFUL_UNLOCK_OPERATION_NOTIFICATION = "The student repositories for this programming exercise were unlocked successfully when the visible date of the exam passed.";

    public static final int FEEDBACK_DETAIL_TEXT_MAX_CHARACTERS = 5000;

    public static final String ASSIGNMENT_CHECKOUT_PATH = "assignment";

    public static final String TESTS_CHECKOUT_PATH = "tests";

    public static final int EXTERNAL_SYSTEM_REQUEST_BATCH_SIZE = 100;

    // Currently 10s.
    public static final int EXTERNAL_SYSTEM_REQUEST_BATCH_WAIT_TIME_MS = 10 * 1000; // 10s

    public static final String SETUP_COMMIT_MESSAGE = "Setup";

    public static final String REGISTER_FOR_COURSE = "REGISTER_FOR_COURSE";

    public static final String DELETE_EXERCISE = "DELETE_EXERCISE";

    public static final String DELETE_COURSE = "DELETE_COURSE";

    public static final String DELETE_EXAM = "DELETE_EXAM";

    public static final String ADD_USER_TO_EXAM = "ADD_USER_TO_EXAM";

    public static final String REMOVE_USER_FROM_EXAM = "REMOVE_USER_FROM_EXAM";

    public static final String DELETE_PARTICIPATION = "DELETE_PARTICIPATION";

    public static final String DELETE_TEAM = "DELETE_TEAM";

    public static final String DELETE_EXERCISE_GROUP = "DELETE_EXERCISE_GROUP";

    public static final String IMPORT_TEAMS = "IMPORT_TEAMS";

    public static final String INFO_BUILD_PLAN_URL_DETAIL = "buildPlanURLTemplate";

    public static final String INFO_SSH_CLONE_URL_DETAIL = "sshCloneURLTemplate";

    public static final String INFO_SSH_KEYS_URL_DETAIL = "sshKeysURL";

    public static final String EXTERNAL_USER_MANAGEMENT_URL = "externalUserManagementURL";

    public static final String EXTERNAL_USER_MANAGEMENT_NAME = "externalUserManagementName";

    public static final String REGISTRATION_ENABLED = "registrationEnabled";

    public static final String ALLOWED_EMAIL_PATTERN = "allowedEmailPattern";

    public static final String ALLOWED_EMAIL_PATTERN_READABLE = "allowedEmailPatternReadable";

    public static final String ARTEMIS_GROUP_DEFAULT_PREFIX = "artemis-";

    public static final String HAZELCAST_QUIZ_SCHEDULER = "quizScheduleServiceExecutor";

    public static final String HAZELCAST_QUIZ_PREFIX = "quiz-";

    public static final String HAZELCAST_EXERCISE_CACHE = HAZELCAST_QUIZ_PREFIX + "exercise-cache";

    public static final int HAZELCAST_QUIZ_EXERCISE_CACHE_SERIALIZER_ID = 1;

    private Constants() {
    }
}
