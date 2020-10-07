import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Exam } from 'app/entities/exam.model';
import { ExamManagementService } from 'app/exam/manage/exam-management.service';
import { Observable } from 'rxjs';
import { HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { AlertService } from 'app/core/alert/alert.service';
import { Course } from 'app/entities/course.model';
import { CourseManagementService } from 'app/course/manage/course-management.service';
import * as moment from 'moment';

@Component({
    selector: 'jhi-exam-update',
    templateUrl: './exam-update.component.html',
})
export class ExamUpdateComponent implements OnInit {
    exam: Exam;
    course: Course;
    isSaving: boolean;

    constructor(
        private route: ActivatedRoute,
        private examManagementService: ExamManagementService,
        private jhiAlertService: AlertService,
        private courseManagementService: CourseManagementService,
    ) {}

    ngOnInit(): void {
        this.route.data.subscribe(({ exam }) => {
            this.exam = exam;
            this.courseManagementService.find(Number(this.route.snapshot.paramMap.get('courseId'))).subscribe(
                (response: HttpResponse<Course>) => {
                    this.exam.course = response.body!;
                    this.course = response.body!;
                },
                (err: HttpErrorResponse) => this.onError(err),
            );
            if (!this.exam.gracePeriod) {
                this.exam.gracePeriod = 180;
            }
        });
    }

    previousState() {
        window.history.back();
    }

    save() {
        this.isSaving = true;
        if (this.exam.id !== undefined) {
            this.subscribeToSaveResponse(this.examManagementService.update(this.course.id!, this.exam));
        } else {
            this.subscribeToSaveResponse(this.examManagementService.create(this.course.id!, this.exam));
        }
    }

    subscribeToSaveResponse(result: Observable<HttpResponse<Exam>>) {
        result.subscribe(
            () => this.onSaveSuccess(),
            (err: HttpErrorResponse) => this.onSaveError(err),
        );
    }

    private onSaveSuccess() {
        this.isSaving = false;
        this.previousState();
    }

    private onSaveError(error: HttpErrorResponse) {
        this.jhiAlertService.error(error.message);
        this.isSaving = false;
    }

    private onError(error: HttpErrorResponse) {
        this.jhiAlertService.error(error.message);
    }

    get isValidConfiguration(): boolean {
        const examConductionDatesValid = this.isValidVisibleDate && this.isValidStartDate && this.isValidEndDate;
        const examReviewDatesValid = this.isValidPublishResultsDate && this.isValidExamStudentReviewStart && this.isValidExamStudentReviewEnd;
        return examConductionDatesValid && examReviewDatesValid;
    }

    get isValidVisibleDate(): boolean {
        return this.exam.visibleDate !== undefined;
    }

    get isValidStartDate(): boolean {
        return this.exam.startDate !== undefined && moment(this.exam.startDate).isAfter(this.exam.visibleDate);
    }

    get isValidEndDate(): boolean {
        return this.exam.endDate !== undefined && moment(this.exam.endDate).isAfter(this.exam.startDate);
    }

    get isValidPublishResultsDate(): boolean {
        // allow instructors to set publishResultsDate later
        if (!this.exam.publishResultsDate) {
            return true;
        }
        // check for undefined because undefined is otherwise treated as the now moment by moment.js
        return this.exam.endDate !== undefined && moment(this.exam.publishResultsDate).isAfter(this.exam.endDate);
    }

    get isValidExamStudentReviewStart(): boolean {
        // allow instructors to set examStudentReviewStart later
        if (!this.exam.examStudentReviewStart) {
            return true;
        }
        // check for undefined because undefined is otherwise treated as the now moment by moment.js
        return this.exam.publishResultsDate !== undefined && moment(this.exam.examStudentReviewStart).isAfter(this.exam.publishResultsDate);
    }

    get isValidExamStudentReviewEnd(): boolean {
        // allow instructors to set examStudentReviewEnd later
        if (!this.exam.examStudentReviewEnd) {
            return true;
        }
        // check for undefined because undefined is otherwise treated as the now moment by moment.js
        return this.exam.examStudentReviewStart !== undefined && moment(this.exam.examStudentReviewEnd).isAfter(this.exam.examStudentReviewStart);
    }
}
