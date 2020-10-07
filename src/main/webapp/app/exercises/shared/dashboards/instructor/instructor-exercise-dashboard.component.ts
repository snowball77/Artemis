import { AlertService } from 'app/core/alert/alert.service';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { StatsForDashboard } from 'app/course/dashboards/instructor-course-dashboard/stats-for-dashboard.model';
import { Exercise } from 'app/entities/exercise.model';
import { ExerciseService } from 'app/exercises/shared/exercise/exercise.service';
import { ResultService } from 'app/exercises/shared/result/result.service';
import { DueDateStat } from 'app/course/dashboards/instructor-course-dashboard/due-date-stat.model';

@Component({
    selector: 'jhi-instructor-exercise-dashboard',
    templateUrl: './instructor-exercise-dashboard.component.html',
    providers: [],
})
export class InstructorExerciseDashboardComponent implements OnInit {
    exercise: Exercise;
    courseId: number;

    stats = new StatsForDashboard();

    dataForAssessmentPieChart: number[];
    totalManualAssessmentPercentage = new DueDateStat();
    totalAutomaticAssessmentPercentage = new DueDateStat();

    constructor(
        private exerciseService: ExerciseService,
        private route: ActivatedRoute,
        private jhiAlertService: AlertService,
        private resultService: ResultService,
        private router: Router,
    ) {}

    /**
     * Extracts the course and exercise ids from the route params and fetches the exercise from the server
     */
    ngOnInit(): void {
        this.courseId = Number(this.route.snapshot.paramMap.get('courseId'));
        const exerciseId = Number(this.route.snapshot.paramMap.get('exerciseId'));
        this.loadExercise(exerciseId);
    }

    /**
     * Navigates back to the instructor dashboard where the user came from
     */
    back() {
        this.router.navigate([`/course-management/${this.courseId}/instructor-dashboard`]);
    }

    /**
     * Computes the stats for the assessment charts.
     * Percentages are rounded towards zero.
     */
    public setStatistics() {
        if (this.stats.numberOfSubmissions.inTime > 0) {
            this.totalManualAssessmentPercentage.inTime = Math.floor(
                ((this.stats.numberOfAssessments.inTime - this.stats.numberOfAutomaticAssistedAssessments.inTime) / this.stats.numberOfSubmissions.inTime) * 100,
            );
            this.totalAutomaticAssessmentPercentage.inTime = Math.floor((this.stats.numberOfAutomaticAssistedAssessments.inTime / this.stats.numberOfSubmissions.inTime) * 100);
        } else {
            this.totalManualAssessmentPercentage.inTime = 100;
        }
        if (this.stats.numberOfSubmissions.late > 0) {
            this.totalManualAssessmentPercentage.late = Math.floor(
                ((this.stats.numberOfAssessments.late - this.stats.numberOfAutomaticAssistedAssessments.late) / this.stats.numberOfSubmissions.late) * 100,
            );
            this.totalAutomaticAssessmentPercentage.late = Math.floor((this.stats.numberOfAutomaticAssistedAssessments.late / this.stats.numberOfSubmissions.late) * 100);
        } else {
            this.totalManualAssessmentPercentage.late = 100;
        }

        this.dataForAssessmentPieChart = [
            this.stats.numberOfSubmissions.total - this.stats.numberOfAssessments.total,
            this.stats.numberOfAssessments.total - this.stats.numberOfAutomaticAssistedAssessments.total,
            this.stats.numberOfAutomaticAssistedAssessments.total,
        ];
    }

    private loadExercise(exerciseId: number) {
        this.exerciseService.find(exerciseId).subscribe(
            (res: HttpResponse<Exercise>) => (this.exercise = res.body!),
            (response: HttpErrorResponse) => this.onError(response.message),
        );

        this.exerciseService.getStatsForInstructors(exerciseId).subscribe(
            (res: HttpResponse<StatsForDashboard>) => {
                this.stats = StatsForDashboard.from(Object.assign({}, this.stats, res.body));
                this.setStatistics();
            },
            (response: string) => this.onError(response),
        );
    }

    private onError(error: string) {
        this.jhiAlertService.error(error);
    }
}
