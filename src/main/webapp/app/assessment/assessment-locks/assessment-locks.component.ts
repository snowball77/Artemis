import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Location } from '@angular/common';
import { FileUploadAssessmentsService } from 'app/exercises/file-upload/assess/file-upload-assessment.service';
import { TranslateService } from '@ngx-translate/core';
import { Submission, SubmissionExerciseType } from 'app/entities/submission.model';
import { CourseManagementService } from 'app/course/manage/course-management.service';
import { HttpResponse } from '@angular/common/http';
import { Course } from 'app/entities/course.model';
import { Exercise, getIcon, getIconTooltip } from 'app/entities/exercise.model';
import { AlertService } from 'app/core/alert/alert.service';
import { ModelingAssessmentService } from 'app/exercises/modeling/assess/modeling-assessment.service';
import { TextAssessmentsService } from 'app/exercises/text/assess/text-assessments.service';

@Component({
    selector: 'jhi-assessment-locks',
    templateUrl: './assessment-locks.component.html',
})
export class AssessmentLocksComponent implements OnInit {
    course: Course;
    courseId: number;
    tutorId: number;
    exercises: Exercise[] = [];

    submissions: Submission[] = [];

    private cancelConfirmationText: string;

    getIcon = getIcon;
    getIconTooltip = getIconTooltip;

    constructor(
        private route: ActivatedRoute,
        private jhiAlertService: AlertService,
        private modelingAssessmentService: ModelingAssessmentService,
        private textAssessmentsService: TextAssessmentsService,
        private fileUploadAssessmentsService: FileUploadAssessmentsService,
        translateService: TranslateService,
        private location: Location,
        private courseService: CourseManagementService,
    ) {
        translateService.get('artemisApp.assessment.messages.confirmCancel').subscribe((text) => (this.cancelConfirmationText = text));
    }

    public async ngOnInit(): Promise<void> {
        this.route.params.subscribe((params) => {
            this.courseId = Number(params['courseId']);
        });
        this.route.queryParams.subscribe((queryParams) => {
            this.tutorId = Number(queryParams['tutorId']);
        });
        this.getAllLockedSubmissions();
    }

    /**
     * Get all locked submissions for course and user.
     */
    getAllLockedSubmissions() {
        this.courseService.findAllLockedSubmissionsOfCourse(this.courseId).subscribe(
            (response: HttpResponse<Submission[]>) => {
                this.submissions.push(...(response.body ?? []));
            },
            (response: string) => this.onError(response),
        );
    }

    /**
     * Cancel the current assessment.
     * @param canceledSubmission submission
     */
    cancelAssessment(canceledSubmission: Submission) {
        const confirmCancel = window.confirm(this.cancelConfirmationText);
        if (confirmCancel) {
            switch (canceledSubmission.submissionExerciseType) {
                case SubmissionExerciseType.MODELING:
                    this.modelingAssessmentService.cancelAssessment(canceledSubmission.id!).subscribe();
                    break;
                case SubmissionExerciseType.TEXT:
                    if (canceledSubmission.participation?.exercise?.id) {
                        this.textAssessmentsService.cancelAssessment(canceledSubmission.participation.exercise.id, canceledSubmission.id!).subscribe();
                    }
                    break;
                case SubmissionExerciseType.FILE_UPLOAD:
                    this.fileUploadAssessmentsService.cancelAssessment(canceledSubmission.id!).subscribe();
                    break;
                default:
                    break;
            }
            this.submissions = this.submissions.filter((submission) => submission !== canceledSubmission);
        }
    }

    /**
     * Navigates back in browser.
     */
    back() {
        this.location.back();
    }

    /**
     * Pass on an error to the browser console and the jhiAlertService.
     * @param error
     */
    private onError(error: string) {
        this.jhiAlertService.error(error);
    }
}
