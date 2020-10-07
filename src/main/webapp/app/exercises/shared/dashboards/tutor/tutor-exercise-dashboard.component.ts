import { AfterViewInit, Component, OnInit } from '@angular/core';
import { SafeHtml } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';
import { CourseManagementService } from 'app/course/manage/course-management.service';
import { AlertService } from 'app/core/alert/alert.service';
import { User } from 'app/core/user/user.model';
import { HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { TutorParticipationService } from 'app/exercises/shared/dashboards/tutor/tutor-participation.service';
import { TextSubmissionService } from 'app/exercises/text/participate/text-submission.service';
import { ExampleSubmission } from 'app/entities/example-submission.model';
import { ArtemisMarkdownService } from 'app/shared/markdown.service';
import { TextExercise } from 'app/entities/text-exercise.model';
import { ModelingExercise } from 'app/entities/modeling-exercise.model';
import { UMLModel } from '@ls1intum/apollon';
import { ComplaintService } from 'app/complaints/complaint.service';
import { Complaint } from 'app/entities/complaint.model';
import { Submission, SubmissionExerciseType } from 'app/entities/submission.model';
import { ModelingSubmissionService } from 'app/exercises/modeling/participate/modeling-submission.service';
import { Observable, of } from 'rxjs';
import { map } from 'rxjs/operators';
import { StatsForDashboard } from 'app/course/dashboards/instructor-course-dashboard/stats-for-dashboard.model';
import { TranslateService } from '@ngx-translate/core';
import { FileUploadSubmissionService } from 'app/exercises/file-upload/participate/file-upload-submission.service';
import { FileUploadExercise } from 'app/entities/file-upload-exercise.model';
import { ProgrammingExercise } from 'app/entities/programming-exercise.model';
import { ProgrammingSubmissionService } from 'app/exercises/programming/participate/programming-submission.service';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { AccountService } from 'app/core/auth/account.service';
import { GuidedTourService } from 'app/guided-tour/guided-tour.service';
import { tutorAssessmentTour } from 'app/guided-tour/tours/tutor-assessment-tour';
import { Exercise, ExerciseType } from 'app/entities/exercise.model';
import { TutorParticipation, TutorParticipationStatus } from 'app/entities/participation/tutor-participation.model';
import { ExerciseService } from 'app/exercises/shared/exercise/exercise.service';
import { DueDateStat } from 'app/course/dashboards/instructor-course-dashboard/due-date-stat.model';
import { Exam } from 'app/entities/exam.model';
import { TextSubmission } from 'app/entities/text-submission.model';
import { SubmissionService } from 'app/exercises/shared/submission/submission.service';

export interface ExampleSubmissionQueryParams {
    readOnly?: boolean;
    toComplete?: boolean;
}

@Component({
    selector: 'jhi-courses',
    templateUrl: './tutor-exercise-dashboard.component.html',
    styles: ['jhi-collapsable-assessment-instructions { max-height: 100vh }'],
    providers: [CourseManagementService],
})
export class TutorExerciseDashboardComponent implements OnInit, AfterViewInit {
    exercise: Exercise;
    modelingExercise: ModelingExercise;
    courseId: number;
    exam?: Exam;
    // TODO fix tutorLeaderboard and side panel for exam exercises
    isExamMode = false;
    isTestRun = false;

    statsForDashboard = new StatsForDashboard();

    exerciseId: number;
    numberOfTutorAssessments = 0;
    numberOfSubmissions = new DueDateStat();
    numberOfAssessments = new DueDateStat();
    numberOfComplaints = 0;
    numberOfOpenComplaints = 0;
    numberOfTutorComplaints = 0;
    numberOfMoreFeedbackRequests = 0;
    numberOfOpenMoreFeedbackRequests = 0;
    numberOfTutorMoreFeedbackRequests = 0;
    totalAssessmentPercentage = new DueDateStat();
    tutorAssessmentPercentage = 0;
    tutorParticipationStatus: TutorParticipationStatus;
    submissions: Submission[] = [];
    unassessedSubmission?: Submission;
    exampleSubmissionsToReview: ExampleSubmission[] = [];
    exampleSubmissionsToAssess: ExampleSubmission[] = [];
    exampleSubmissionsCompletedByTutor: ExampleSubmission[] = [];
    tutorParticipation: TutorParticipation;
    nextExampleSubmissionId: number;
    exampleSolutionModel: UMLModel;
    complaints: Complaint[] = [];
    moreFeedbackRequests: Complaint[] = [];
    submissionLockLimitReached = false;
    openingAssessmentEditorForNewSubmission = false;

    formattedGradingInstructions?: SafeHtml;
    formattedProblemStatement?: SafeHtml;
    formattedSampleSolution?: SafeHtml;

    readonly ExerciseType = ExerciseType;

    stats = {
        toReview: {
            done: 0,
            total: 0,
        },
        toAssess: {
            done: 0,
            total: 0,
        },
    };

    NOT_PARTICIPATED = TutorParticipationStatus.NOT_PARTICIPATED;
    REVIEWED_INSTRUCTIONS = TutorParticipationStatus.REVIEWED_INSTRUCTIONS;
    TRAINED = TutorParticipationStatus.TRAINED;
    COMPLETED = TutorParticipationStatus.COMPLETED;

    tutor?: User;

    exerciseForGuidedTour?: Exercise;

    constructor(
        private exerciseService: ExerciseService,
        private jhiAlertService: AlertService,
        private translateService: TranslateService,
        private accountService: AccountService,
        private route: ActivatedRoute,
        private tutorParticipationService: TutorParticipationService,
        private submissionService: SubmissionService,
        private textSubmissionService: TextSubmissionService,
        private modelingSubmissionService: ModelingSubmissionService,
        private fileUploadSubmissionService: FileUploadSubmissionService,
        private artemisMarkdown: ArtemisMarkdownService,
        private router: Router,
        private complaintService: ComplaintService,
        private programmingSubmissionService: ProgrammingSubmissionService,
        private modalService: NgbModal,
        private guidedTourService: GuidedTourService,
    ) {}

    /**
     * Extracts the course and exercise ids from the route params and fetches the exercise from the server
     */
    ngOnInit(): void {
        this.exerciseId = Number(this.route.snapshot.paramMap.get('exerciseId'));
        this.courseId = Number(this.route.snapshot.paramMap.get('courseId'));
        this.isTestRun = this.route.snapshot.url[3]?.toString() === 'test-run-tutor-dashboard';

        this.loadAll();

        this.accountService.identity().then((user: User) => (this.tutor = user));
    }

    /**
     * Notifies the guided tour service that this component has loaded
     */
    ngAfterViewInit(): void {
        this.guidedTourService.componentPageLoaded();
    }

    /**
     * Loads all information from the server regarding this exercise that is needed for the tutor exercise dashboard
     */
    loadAll() {
        this.exerciseService.getForTutors(this.exerciseId).subscribe(
            (res: HttpResponse<Exercise>) => {
                this.exercise = res.body!;
                this.formattedGradingInstructions = this.artemisMarkdown.safeHtmlForMarkdown(this.exercise.gradingInstructions);
                this.formattedProblemStatement = this.artemisMarkdown.safeHtmlForMarkdown(this.exercise.problemStatement);

                switch (this.exercise.type) {
                    case ExerciseType.TEXT:
                        const textExercise = this.exercise as TextExercise;
                        this.formattedSampleSolution = this.artemisMarkdown.safeHtmlForMarkdown(textExercise.sampleSolution);
                        break;
                    case ExerciseType.MODELING:
                        this.modelingExercise = this.exercise as ModelingExercise;
                        if (this.modelingExercise.sampleSolutionModel) {
                            this.formattedSampleSolution = this.artemisMarkdown.safeHtmlForMarkdown(this.modelingExercise.sampleSolutionExplanation);
                            this.exampleSolutionModel = JSON.parse(this.modelingExercise.sampleSolutionModel);
                        }
                        break;
                    case ExerciseType.FILE_UPLOAD:
                        const fileUploadExercise = this.exercise as FileUploadExercise;
                        this.formattedSampleSolution = this.artemisMarkdown.safeHtmlForMarkdown(fileUploadExercise.sampleSolution);
                        break;
                }

                this.exerciseForGuidedTour = this.guidedTourService.enableTourForExercise(this.exercise, tutorAssessmentTour, false);
                this.tutorParticipation = this.exercise.tutorParticipations![0];
                this.tutorParticipationStatus = this.tutorParticipation.status!;
                if (this.exercise.exampleSubmissions && this.exercise.exampleSubmissions.length > 0) {
                    this.exampleSubmissionsToReview = this.exercise.exampleSubmissions.filter((exampleSubmission: ExampleSubmission) => !exampleSubmission.usedForTutorial);
                    this.exampleSubmissionsToAssess = this.exercise.exampleSubmissions.filter((exampleSubmission: ExampleSubmission) => exampleSubmission.usedForTutorial);
                }
                this.exampleSubmissionsCompletedByTutor = this.tutorParticipation.trainedExampleSubmissions || [];

                this.stats.toReview.total = this.exampleSubmissionsToReview.length;
                this.stats.toReview.done = this.exampleSubmissionsCompletedByTutor.filter((e) => !e.usedForTutorial).length;
                this.stats.toAssess.total = this.exampleSubmissionsToAssess.length;
                this.stats.toAssess.done = this.exampleSubmissionsCompletedByTutor.filter((e) => e.usedForTutorial).length;

                if (this.stats.toReview.done < this.stats.toReview.total) {
                    this.nextExampleSubmissionId = this.exampleSubmissionsToReview[this.stats.toReview.done].id!;
                } else if (this.stats.toAssess.done < this.stats.toAssess.total) {
                    this.nextExampleSubmissionId = this.exampleSubmissionsToAssess[this.stats.toAssess.done].id!;
                }

                // exercise belongs to an exam
                if (this.exercise?.exerciseGroup) {
                    this.isExamMode = true;
                    this.exam = this.exercise?.exerciseGroup?.exam;
                }

                this.getTutorAssessedSubmissions();

                // 1. We don't want to assess submissions before the exercise due date
                // 2. The assessment for team exercises is not started from the tutor exercise dashboard but from the team pages
                if ((!this.exercise.dueDate || this.exercise.dueDate.isBefore(Date.now())) && !this.exercise.teamMode && !this.isTestRun) {
                    this.getSubmissionWithoutAssessment();
                }
            },
            (response: string) => this.onError(response),
        );

        if (!this.isTestRun) {
            this.complaintService.getComplaintsForTutor(this.exerciseId).subscribe(
                (res: HttpResponse<Complaint[]>) => (this.complaints = res.body as Complaint[]),
                (error: HttpErrorResponse) => this.onError(error.message),
            );
            this.complaintService.getMoreFeedbackRequestsForTutor(this.exerciseId).subscribe(
                (res: HttpResponse<Complaint[]>) => (this.moreFeedbackRequests = res.body as Complaint[]),
                (error: HttpErrorResponse) => this.onError(error.message),
            );
        } else {
            this.complaintService.getComplaintsForTestRun(this.exerciseId).subscribe(
                (res: HttpResponse<Complaint[]>) => (this.complaints = res.body as Complaint[]),
                (error: HttpErrorResponse) => this.onError(error.message),
            );
        }

        this.exerciseService.getStatsForTutors(this.exerciseId).subscribe(
            (res: HttpResponse<StatsForDashboard>) => {
                this.statsForDashboard = StatsForDashboard.from(res.body!);
                this.numberOfSubmissions = this.statsForDashboard.numberOfSubmissions;
                this.numberOfAssessments = this.statsForDashboard.numberOfAssessments;
                this.numberOfComplaints = this.statsForDashboard.numberOfComplaints;
                this.numberOfOpenComplaints = this.statsForDashboard.numberOfOpenComplaints;
                this.numberOfMoreFeedbackRequests = this.statsForDashboard.numberOfMoreFeedbackRequests;
                this.numberOfOpenMoreFeedbackRequests = this.statsForDashboard.numberOfOpenMoreFeedbackRequests;
                const tutorLeaderboardEntry = this.statsForDashboard.tutorLeaderboardEntries.find((entry) => entry.userId === this.tutor!.id);
                if (tutorLeaderboardEntry) {
                    this.numberOfTutorAssessments = tutorLeaderboardEntry.numberOfAssessments;
                    this.numberOfTutorComplaints = tutorLeaderboardEntry.numberOfTutorComplaints;
                    this.numberOfTutorMoreFeedbackRequests = tutorLeaderboardEntry.numberOfTutorMoreFeedbackRequests;
                } else {
                    this.numberOfTutorAssessments = 0;
                    this.numberOfTutorComplaints = 0;
                    this.numberOfTutorMoreFeedbackRequests = 0;
                }

                if (this.numberOfSubmissions.inTime > 0) {
                    this.totalAssessmentPercentage.inTime = Math.floor((this.numberOfAssessments.inTime / this.numberOfSubmissions.inTime) * 100);
                } else {
                    this.totalAssessmentPercentage.inTime = 100;
                }
                if (this.numberOfSubmissions.late > 0) {
                    this.totalAssessmentPercentage.late = Math.floor((this.numberOfAssessments.late / this.numberOfSubmissions.late) * 100);
                } else {
                    this.totalAssessmentPercentage.late = 100;
                }
                if (this.numberOfSubmissions.total > 0) {
                    this.tutorAssessmentPercentage = Math.floor((this.numberOfTutorAssessments / this.numberOfSubmissions.total) * 100);
                } else {
                    this.tutorAssessmentPercentage = 100;
                }
            },
            (response: string) => this.onError(response),
        );
    }

    language(submission: Submission): string {
        if (submission.submissionExerciseType === SubmissionExerciseType.TEXT) {
            return (submission as TextSubmission).language || 'UNKNOWN';
        }
        return 'UNKNOWN';
    }

    /**
     * Get all the submissions from the server for which the current user is the assessor, which is the case for started or completed assessments. All these submissions get listed
     * in the exercise dashboard.
     */
    private getTutorAssessedSubmissions(): void {
        let submissionsObservable: Observable<HttpResponse<Submission[]>> = of();
        if (this.isTestRun) {
            submissionsObservable = this.submissionService.getTestRunSubmissionsForExercise(this.exerciseId);
        } else {
            // TODO: This could be one generic endpoint.
            switch (this.exercise.type) {
                case ExerciseType.TEXT:
                    submissionsObservable = this.textSubmissionService.getTextSubmissionsForExercise(this.exerciseId, { assessedByTutor: true });
                    break;
                case ExerciseType.MODELING:
                    submissionsObservable = this.modelingSubmissionService.getModelingSubmissionsForExercise(this.exerciseId, { assessedByTutor: true });
                    break;
                case ExerciseType.FILE_UPLOAD:
                    submissionsObservable = this.fileUploadSubmissionService.getFileUploadSubmissionsForExercise(this.exerciseId, { assessedByTutor: true });
                    break;
                case ExerciseType.PROGRAMMING:
                    submissionsObservable = this.programmingSubmissionService.getProgrammingSubmissionsForExercise(this.exerciseId, { assessedByTutor: true });
                    break;
            }
        }

        submissionsObservable
            .pipe(
                map((res) => res.body),
                map(this.reconnectEntities),
            )
            .subscribe((submissions: Submission[]) => {
                // Set the received submissions. As the result component depends on the submission we nest it into the participation.
                this.submissions = submissions.map((submission) => {
                    submission.participation!.submissions = [submission];
                    return submission;
                });
            });
    }

    /**
     * Reconnect submission, result and participation for all submissions in the given array.
     */
    private reconnectEntities = (submissions: Submission[]) => {
        return submissions.map((submission: Submission) => {
            if (submission.result) {
                // reconnect some associations
                submission.result.submission = submission;
                submission.result.participation = submission.participation;
                submission.participation!.results = [submission.result];
            }
            return submission;
        });
    };

    /**
     * Get a submission from the server that does not have an assessment yet (if there is one). The submission gets added to the end of the list of submissions in the exercise
     * dashboard and the user can start the assessment. Note, that the number of started but unfinished assessments is limited per user and course. If the user reached this limit,
     * the server will respond with a BAD REQUEST response here.
     */
    private getSubmissionWithoutAssessment(): void {
        let submissionObservable: Observable<Submission> = of();
        switch (this.exercise.type) {
            case ExerciseType.TEXT:
                submissionObservable = this.textSubmissionService.getTextSubmissionForExerciseWithoutAssessment(this.exerciseId, 'head');
                break;
            case ExerciseType.MODELING:
                submissionObservable = this.modelingSubmissionService.getModelingSubmissionForExerciseWithoutAssessment(this.exerciseId);
                break;
            case ExerciseType.FILE_UPLOAD:
                submissionObservable = this.fileUploadSubmissionService.getFileUploadSubmissionForExerciseWithoutAssessment(this.exerciseId);
                break;
            case ExerciseType.PROGRAMMING:
                submissionObservable = this.programmingSubmissionService.getProgrammingSubmissionForExerciseWithoutAssessment(this.exerciseId);
                break;
        }

        submissionObservable.subscribe(
            (submission: Submission) => {
                this.unassessedSubmission = submission;
                this.submissionLockLimitReached = false;
            },
            (error: HttpErrorResponse) => {
                if (error.status === 404) {
                    // there are no unassessed submission, nothing we have to worry about
                    this.unassessedSubmission = undefined;
                } else if (error.error && error.error.errorKey === 'lockedSubmissionsLimitReached') {
                    this.submissionLockLimitReached = true;
                } else {
                    this.onError(error.message);
                }
            },
        );
    }

    /**
     * Called after the tutor has read the instructions and creates a new tutor participation
     */
    readInstruction() {
        this.tutorParticipationService.create(this.tutorParticipation, this.exerciseId).subscribe((res: HttpResponse<TutorParticipation>) => {
            this.tutorParticipation = res.body!;
            this.tutorParticipationStatus = this.tutorParticipation.status!;
            this.jhiAlertService.success('artemisApp.tutorExerciseDashboard.participation.instructionsReviewed');
        }, this.onError);
    }

    /**
     * Returns whether the example submission for the given id has already been completed
     * @param exampleSubmissionId Id of the example submission which to check for completion
     */
    hasBeenCompletedByTutor(exampleSubmissionId: number) {
        return this.exampleSubmissionsCompletedByTutor.filter((exampleSubmission) => exampleSubmission.id === exampleSubmissionId).length > 0;
    }

    private onError(error: string) {
        this.jhiAlertService.error(error);
    }

    /**
     * Calculates the status of a submission by inspecting the result
     * @param submission Submission which to check
     */
    calculateStatus(submission: Submission) {
        if (submission.result && submission.result.completionDate) {
            return 'DONE';
        }
        return 'DRAFT';
    }

    /**
     * Uses the router to navigate to a given example submission
     * @param submissionId Id of submission where to navigate to
     * @param readOnly Flag whether the view should be opened in read-only mode
     * @param toComplete Flag whether the view should be opened in to-complete mode
     */
    openExampleSubmission(submissionId: number, readOnly?: boolean, toComplete?: boolean) {
        if (!this.exercise || !this.exercise.type || !submissionId) {
            return;
        }
        const route = `/course-management/${this.courseId}/${this.exercise.type}-exercises/${this.exercise.id}/example-submissions/${submissionId}`;
        // TODO CZ: add both flags and check for value in example submission components
        const queryParams: ExampleSubmissionQueryParams = {};
        if (readOnly) {
            queryParams.readOnly = readOnly;
        }
        if (toComplete) {
            queryParams.toComplete = toComplete;
        }

        this.router.navigate([route], { queryParams });
    }

    /**
     * Uses the router to navigate to the assessment editor for a given/new submission
     * @param submission Either submission or 'new'.
     */
    async openAssessmentEditor(submission: Submission | 'new'): Promise<void> {
        if (!this.exercise || !this.exercise.type || !submission) {
            return;
        }

        this.openingAssessmentEditorForNewSubmission = true;
        const submissionUrlParameter: number | 'new' = submission === 'new' ? 'new' : submission.id!;
        const route = `/course-management/${this.courseId}/${this.exercise.type}-exercises/${this.exercise.id}/submissions/${submissionUrlParameter}/assessment`;
        if (this.isTestRun) {
            await this.router.navigate([route], { queryParams: { testRun: this.isTestRun } });
        } else {
            await this.router.navigate([route]);
        }
        this.openingAssessmentEditorForNewSubmission = false;
    }

    async openCodeEditorWithStudentSubmission(participationId: number) {
        const route = `/course-management/${this.courseId}/${this.exercise.type}-exercises/${this.exercise.id}/code-editor/${participationId}/assessment`;
        if (this.isTestRun) {
            await this.router.navigate([route], { queryParams: { testRun: this.isTestRun } });
        } else {
            await this.router.navigate([route]);
        }
    }

    /**
     * Show complaint depending on the exercise type
     * @param complaint that we want to show
     */
    viewComplaint(complaint: Complaint) {
        if (this.exercise.type === ExerciseType.PROGRAMMING) {
            this.openCodeEditorWithStudentSubmission(complaint.result!.participation!.id!);
        } else {
            this.openAssessmentEditor(complaint.result!.submission!);
        }
    }

    /**
     * Casts an Exercise to a ProgrammingExercise
     * @param exercise Exercise to cast
     */
    asProgrammingExercise(exercise: Exercise) {
        return exercise as ProgrammingExercise;
    }

    /**
     * Navigates back to the tutor (exam) dashboard
     */
    back() {
        if (!this.isExamMode) {
            this.router.navigate([`/course-management/${this.courseId}/tutor-dashboard`]);
        } else {
            if (this.isTestRun) {
                this.router.navigate([`/course-management/${this.courseId}/exams/${this.exercise!.exerciseGroup!.exam!.id}/test-runs/assess`]);
            } else {
                this.router.navigate([`/course-management/${this.courseId}/exams/${this.exercise!.exerciseGroup!.exam!.id}/tutor-exam-dashboard`]);
            }
        }
    }
}
