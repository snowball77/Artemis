import { Component, OnInit } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { StudentExam } from 'app/entities/student-exam.model';
import { StudentExamService } from 'app/exam/manage/student-exams/student-exam.service';
import { Course } from 'app/entities/course.model';
import { CourseManagementService } from 'app/course/manage/course-management.service';
import { User } from 'app/core/user/user.model';
import { Exercise, ExerciseType } from 'app/entities/exercise.model';
import { ArtemisDurationFromSecondsPipe } from 'app/shared/pipes/artemis-duration-from-seconds.pipe';
import { AlertService } from 'app/core/alert/alert.service';
import { round } from 'app/shared/util/utils';
import * as moment from 'moment';

@Component({
    selector: 'jhi-student-exam-detail',
    templateUrl: './student-exam-detail.component.html',
    providers: [ArtemisDurationFromSecondsPipe],
})
export class StudentExamDetailComponent implements OnInit {
    courseId: number;
    studentExam: StudentExam;
    course: Course;
    student: User;
    workingTimeForm: FormGroup;
    isSavingWorkingTime = false;
    maxTotalScore = 0;
    achievedTotalScore = 0;

    constructor(
        private route: ActivatedRoute,
        private studentExamService: StudentExamService,
        private courseService: CourseManagementService,
        private artemisDurationFromSecondsPipe: ArtemisDurationFromSecondsPipe,
        private alertService: AlertService,
    ) {}

    /**
     * Initialize the courseId and studentExam
     */
    ngOnInit(): void {
        this.loadStudentExam();
    }

    /**
     * Load the course and the student exam
     */
    loadStudentExam() {
        this.courseId = Number(this.route.snapshot.paramMap.get('courseId'));
        this.route.data.subscribe(({ studentExam }) => this.setStudentExam(studentExam));

        this.courseService.find(this.courseId).subscribe((courseResponse) => {
            this.course = courseResponse.body!;
        });
        this.student = this.studentExam.user!;
    }

    /**
     * Get an icon for the type of the given exercise.
     * @param exercise {Exercise}
     */
    exerciseIcon(exercise: Exercise): string {
        switch (exercise.type) {
            case ExerciseType.QUIZ:
                return 'check-double';
            case ExerciseType.FILE_UPLOAD:
                return 'file-upload';
            case ExerciseType.MODELING:
                return 'project-diagram';
            case ExerciseType.PROGRAMMING:
                return 'keyboard';
            default:
                return 'font';
        }
    }

    /**
     * Save the defined working time
     */
    saveWorkingTime() {
        this.isSavingWorkingTime = true;
        const seconds = this.workingTimeForm.controls.minutes.value * 60 + this.workingTimeForm.controls.seconds.value;
        this.studentExamService.updateWorkingTime(this.courseId, this.studentExam.exam!.id!, this.studentExam.id!, seconds).subscribe(
            (res) => {
                if (res.body) {
                    this.setStudentExam(res.body);
                }
                this.isSavingWorkingTime = false;
                this.alertService.success('artemisApp.studentExamDetail.saveWorkingTimeSuccessful');
            },
            () => {
                this.alertService.error('artemisApp.studentExamDetail.workingTimeCouldNotBeSaved');
                this.isSavingWorkingTime = false;
            },
        );
    }

    /**
     * Sets the student exam, initialised the component which allows changing the working time and sets the score of the student.
     * @param studentExam
     */
    private setStudentExam(studentExam: StudentExam) {
        this.studentExam = studentExam;
        this.initWorkingTimeForm();
        studentExam.exercises!.forEach((exercise) => {
            this.maxTotalScore += exercise.maxScore!;
            if (
                exercise.studentParticipations?.length &&
                exercise.studentParticipations.length > 0 &&
                exercise.studentParticipations[0].results?.length &&
                exercise.studentParticipations[0].results.length > 0
            ) {
                this.achievedTotalScore += (exercise.studentParticipations[0].results[0].score! * exercise.maxScore!) / 100;
                this.achievedTotalScore = this.rounding(this.achievedTotalScore);
            }
        });
    }

    private initWorkingTimeForm() {
        const workingTime = this.artemisDurationFromSecondsPipe.transform(this.studentExam.workingTime!);
        const workingTimeParts = workingTime.split(':');
        this.workingTimeForm = new FormGroup({
            minutes: new FormControl({ value: parseInt(workingTimeParts[0] ? workingTimeParts[0] : '0', 10), disabled: this.examIsVisible() }, [
                Validators.min(0),
                Validators.required,
            ]),
            seconds: new FormControl({ value: parseInt(workingTimeParts[1] ? workingTimeParts[1] : '0', 10), disabled: this.examIsVisible() }, [
                Validators.min(0),
                Validators.max(59),
                Validators.required,
            ]),
        });
    }

    examIsVisible(): boolean {
        if (this.studentExam.exam) {
            // Disable the form to edit the working time if the exam is already visible
            return moment(this.studentExam.exam.visibleDate).isBefore(moment());
        }
        // if exam is undefined, the form to edit the working time is disabled
        return true;
    }

    getWorkingTimeToolTip(): string {
        return this.examIsVisible()
            ? 'You cannot change the individual working time after the exam has become visible.'
            : 'You can change the individual working time of the student here.';
    }
    rounding(number: number) {
        return round(number, 1);
    }
}
