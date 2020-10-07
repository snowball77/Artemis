import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Subscription } from 'rxjs/Subscription';
import { TranslateService } from '@ngx-translate/core';
import { sortBy } from 'lodash';
import { Course } from 'app/entities/course.model';
import { CourseManagementService } from 'app/course/manage/course-management.service';
import { Result } from 'app/entities/result.model';
import * as moment from 'moment';
import { Exercise, ExerciseType } from 'app/entities/exercise.model';
import {
    ABSOLUTE_SCORE,
    CourseScoreCalculationService,
    MAX_SCORE,
    PRESENTATION_SCORE,
    RELATIVE_SCORE,
    REACHABLE_SCORE,
    CURRENT_RELATIVE_SCORE,
} from 'app/overview/course-score-calculation.service';
import { InitializationState } from 'app/entities/participation/participation.model';

const QUIZ_EXERCISE_COLOR = '#17a2b8';
const PROGRAMMING_EXERCISE_COLOR = '#fd7e14';
const MODELING_EXERCISE_COLOR = '#6610f2';
const TEXT_EXERCISE_COLOR = '#B00B6B';
const FILE_UPLOAD_EXERCISE_COLOR = '#2D9C88';

export interface CourseStatisticsDataSet {
    data: Array<number>;
    backgroundColor: Array<any>;
}

@Component({
    selector: 'jhi-course-statistics',
    templateUrl: './course-statistics.component.html',
    styleUrls: ['../course-overview.scss'],
})
export class CourseStatisticsComponent implements OnInit, OnDestroy {
    readonly QUIZ = ExerciseType.QUIZ;

    private courseId: number;
    private courseExercises: Exercise[];
    private paramSubscription: Subscription;
    private courseUpdatesSubscription: Subscription;
    private translateSubscription: Subscription;
    course?: Course;

    // absolute score
    totalScore = 0;
    absoluteScores = {};

    // relative score
    totalRelativeScore = 0;
    relativeScores = {};

    // max score
    totalMaxScore = 0;
    totalMaxScores = {};

    // current max score
    reachableScore = 0;
    reachableScores = {};

    // current max score
    currentRelativeScore = 0;
    currentRelativeScores = {};

    // presentation score
    totalPresentationScore = 0;
    presentationScores = {};
    presentationScoreEnabled = false;

    // this is not an actual exercise, it contains more entries
    groupedExercises: any[][] = [];
    doughnutChartColors = [QUIZ_EXERCISE_COLOR, PROGRAMMING_EXERCISE_COLOR, MODELING_EXERCISE_COLOR, TEXT_EXERCISE_COLOR, FILE_UPLOAD_EXERCISE_COLOR, 'rgba(0, 0, 0, 0.5)'];

    public doughnutChartLabels: string[] = ['Quiz Points', 'Programming Points', 'Modeling Points', 'Text Points', 'File Upload Points', 'Missing Points'];
    public exerciseTitles: object = {
        quiz: {
            name: this.translateService.instant('artemisApp.course.quizExercises'),
            color: QUIZ_EXERCISE_COLOR,
        },
        modeling: {
            name: this.translateService.instant('artemisApp.course.modelingExercises'),
            color: MODELING_EXERCISE_COLOR,
        },
        programming: {
            name: this.translateService.instant('artemisApp.course.programmingExercises'),
            color: PROGRAMMING_EXERCISE_COLOR,
        },
        text: {
            name: this.translateService.instant('artemisApp.course.textExercises'),
            color: TEXT_EXERCISE_COLOR,
        },
        'file-upload': {
            name: this.translateService.instant('artemisApp.course.fileUploadExercises'),
            color: FILE_UPLOAD_EXERCISE_COLOR,
        },
    };

    public doughnutChartData: CourseStatisticsDataSet[] = [
        {
            data: [0, 0, 0, 0, 0, 0],
            backgroundColor: this.doughnutChartColors,
        },
    ];

    chartColors = [
        {
            // green
            backgroundColor: 'rgba(40, 167, 69, 0.8)',
            hoverBackgroundColor: 'rgba(40, 167, 69, 1)',
            borderColor: 'rgba(40, 167, 69, 1)',
            pointBackgroundColor: 'rgba(40, 167, 69, 1)',
            pointBorderColor: '#fff',
            pointHoverBackgroundColor: '#fff',
            pointHoverBorderColor: 'rgba(40, 167, 69, 1)',
        },
        {
            // red
            backgroundColor: 'rgba(220, 53, 69, 0.8)',
            hoverBackgroundColor: 'rgba(220, 53, 69, 1)',
            borderColor: 'rgba(220, 53, 69, 1)',
            pointBackgroundColor: 'rgba(220, 53, 69, 1)',
            pointBorderColor: '#fff',
            pointHoverBackgroundColor: '#fff',
            pointHoverBorderColor: 'rgba(220, 53, 69, 1)',
        },
        {
            // blue
            backgroundColor: 'rgba(62, 138, 204, 0.8)',
            hoverBackgroundColor: 'rgba(62, 138, 204, 1)',
            borderColor: 'rgba(62, 138, 204, 1)',
            pointBackgroundColor: 'rgba(62, 138, 204, 1)',
            pointBorderColor: '#fff',
            pointHoverBackgroundColor: '#fff',
            pointHoverBorderColor: 'rgba(62, 138, 204, 1)',
        },
    ];
    public barChartOptions: any = {
        scaleShowVerticalLines: false,
        maintainAspectRatio: false,
        responsive: true,
        scales: {
            xAxes: [
                {
                    stacked: true,
                    ticks: {
                        autoSkip: false,
                        maxRotation: 0,
                        minRotation: 0,
                    },
                    gridLines: {
                        display: false,
                    },
                },
            ],
            yAxes: [
                {
                    stacked: true,
                },
            ],
        },
        tooltips: {
            backgroundColor: 'rgba(0, 0, 0, 1)',
            width: 120,
            callbacks: {
                label: (tooltipItem: any, data: any) => {
                    return data.datasets[tooltipItem.datasetIndex].tooltips[tooltipItem.index];
                },
                afterLabel: (tooltipItem: any, data: any) => {
                    return data.datasets[tooltipItem.datasetIndex].footer[tooltipItem.index];
                },
            },
        },
    };
    public barChartType = 'horizontalBar';

    public doughnutChartType = 'doughnut';
    public totalScoreOptions: object = {
        cutoutPercentage: 75,
        scaleShowVerticalLines: false,
        responsive: false,
        tooltips: {
            backgroundColor: 'rgba(0, 0, 0, 1)',
        },
    };

    constructor(
        private courseService: CourseManagementService,
        private courseCalculationService: CourseScoreCalculationService,
        private translateService: TranslateService,
        private route: ActivatedRoute,
    ) {}

    ngOnInit() {
        this.paramSubscription = this.route.parent!.params.subscribe((params) => {
            this.courseId = parseInt(params['courseId'], 10);
        });

        this.course = this.courseCalculationService.getCourse(this.courseId);
        this.onCourseLoad();

        this.courseUpdatesSubscription = this.courseService.getCourseUpdates(this.courseId).subscribe((course: Course) => {
            this.courseCalculationService.updateCourse(course);
            this.course = this.courseCalculationService.getCourse(this.courseId);
            this.onCourseLoad();
        });

        this.translateSubscription = this.translateService.onLangChange.subscribe(() => {
            this.exerciseTitles = {
                quiz: {
                    name: this.translateService.instant('artemisApp.course.quizExercises'),
                    color: QUIZ_EXERCISE_COLOR,
                },
                modeling: {
                    name: this.translateService.instant('artemisApp.course.modelingExercises'),
                    color: MODELING_EXERCISE_COLOR,
                },
                programming: {
                    name: this.translateService.instant('artemisApp.course.programmingExercises'),
                    color: PROGRAMMING_EXERCISE_COLOR,
                },
                text: {
                    name: this.translateService.instant('artemisApp.course.textExercises'),
                    color: TEXT_EXERCISE_COLOR,
                },
                'file-upload': {
                    name: this.translateService.instant('artemisApp.course.fileUploadExercises'),
                    color: FILE_UPLOAD_EXERCISE_COLOR,
                },
            };
            this.groupExercisesByType();
        });
    }

    ngOnDestroy() {
        this.translateSubscription.unsubscribe();
        this.courseUpdatesSubscription.unsubscribe();
        this.paramSubscription.unsubscribe();
    }

    private onCourseLoad() {
        this.courseExercises = this.course!.exercises!;
        this.calculateMaxScores();
        this.calculateReachableScores();
        this.calculateAbsoluteScores();
        this.calculateRelativeScores();
        this.calculatePresentationScores();
        this.calculateCurrentRelativeScores();
        this.groupExercisesByType();
    }

    groupExercisesByType() {
        let exercises = this.course!.exercises;
        const groupedExercises: any[] = [];
        const exerciseTypes: string[] = [];
        // adding several years to be sure that exercises without due date are sorted at the end. this is necessary for the order inside the statistic charts
        exercises = sortBy(exercises, [(exercise: Exercise) => (exercise.dueDate || moment().add(5, 'year')).valueOf()]);
        exercises.forEach((exercise) => {
            if (!exercise.dueDate || exercise.dueDate.isBefore(moment()) || exercise.type === ExerciseType.PROGRAMMING) {
                let index = exerciseTypes.indexOf(exercise.type!);
                if (index === -1) {
                    index = exerciseTypes.length;
                    exerciseTypes.push(exercise.type!);
                }
                if (!groupedExercises[index]) {
                    groupedExercises[index] = {
                        type: exercise.type,
                        relativeScore: 0,
                        totalMaxScore: 0,
                        absoluteScore: 0,
                        presentationScore: 0,
                        presentationScoreEnabled: exercise.presentationScoreEnabled,
                        names: [],
                        scores: { data: [], label: 'Score', tooltips: [], footer: [] },
                        missedScores: { data: [], label: 'Missed score', tooltips: [], footer: [] },
                        notGraded: { data: [], label: 'Not graded', tooltips: [], footer: [] },
                        reachableScore: 0,
                        currentRelativeScore: 0,
                    };
                }

                if (!exercise.studentParticipations || exercise.studentParticipations.length === 0) {
                    groupedExercises[index] = this.createPlaceholderChartElement(groupedExercises[index], exercise.title!, 'exerciseNotParticipated', false);
                } else {
                    exercise.studentParticipations.forEach((participation) => {
                        if (participation.results && participation.results.length > 0) {
                            const participationResult = this.courseCalculationService.getResultForParticipation(participation, exercise.dueDate!);
                            if (participationResult && participationResult.rated) {
                                const participationScore = participationResult.score!;
                                const missedScore = 100 - participationScore;
                                groupedExercises[index].scores.data.push(participationScore);
                                groupedExercises[index].missedScores.data.push(missedScore);
                                groupedExercises[index].notGraded.data.push(0);
                                groupedExercises[index].notGraded.tooltips.push(null);
                                groupedExercises[index].names.push(exercise.title);
                                groupedExercises[index].scores.footer.push(null);
                                groupedExercises[index].missedScores.footer.push(null);
                                groupedExercises[index].notGraded.footer.push(null);
                                this.generateTooltip(participationResult, groupedExercises[index]);
                            }
                        } else {
                            if (
                                participation.initializationState === InitializationState.FINISHED &&
                                (!exercise.dueDate || participation.initializationDate!.isBefore(exercise.dueDate!))
                            ) {
                                groupedExercises[index] = this.createPlaceholderChartElement(groupedExercises[index], exercise.title!, 'exerciseNotGraded', true);
                            } else {
                                groupedExercises[index] = this.createPlaceholderChartElement(groupedExercises[index], exercise.title!, 'exerciseParticipatedAfterDueDate', false);
                            }
                        }
                    });
                }

                groupedExercises[index].relativeScore = this.relativeScores[exercise.type!];
                groupedExercises[index].totalMaxScore = this.totalMaxScores[exercise.type!];
                groupedExercises[index].currentRelativeScore = this.currentRelativeScores[exercise.type!];
                groupedExercises[index].reachableScore = this.reachableScores[exercise.type!];
                groupedExercises[index].absoluteScore = this.absoluteScores[exercise.type!];
                groupedExercises[index].presentationScore = this.presentationScores[exercise.type!];
                // check if presentation score is enabled for at least one exercise
                groupedExercises[index].presentationScoreEnabled = groupedExercises[index].presentationScoreEnabled || exercise.presentationScoreEnabled;
                groupedExercises[index].values = [groupedExercises[index].scores, groupedExercises[index].missedScores, groupedExercises[index].notGraded];
            }
        });
        this.groupedExercises = groupedExercises;
    }

    createPlaceholderChartElement(chartElement: any, exerciseTitle: string, tooltipMessage: string, isNotGraded: boolean) {
        const tooltip = this.translateService.instant(`artemisApp.courseOverview.statistics.${tooltipMessage}`, { exercise: exerciseTitle });
        chartElement.notGraded.data.push(isNotGraded ? 100 : 0);
        chartElement.scores.data.push(0);
        chartElement.missedScores.data.push(isNotGraded ? 0 : 100);
        chartElement.names.push(exerciseTitle);
        chartElement.notGraded.tooltips.push(isNotGraded ? tooltip : null);
        chartElement.scores.tooltips.push(null);
        chartElement.missedScores.tooltips.push(isNotGraded ? null : tooltip);
        chartElement.scores.footer.push(null);
        chartElement.missedScores.footer.push(
            tooltipMessage === 'exerciseParticipatedAfterDueDate' ? this.translateService.instant(`artemisApp.courseOverview.statistics.noPointsForExercise`) : null,
        );
        chartElement.notGraded.footer.push(null);
        return chartElement;
    }

    generateTooltip(result: Result, groupedExercise: any): void {
        if (!result.resultString) {
            groupedExercise.scores.tooltips.push(
                this.translateService.instant('artemisApp.courseOverview.statistics.exerciseAchievedScore', {
                    points: 0,
                    percentage: 0,
                }),
            );
            groupedExercise.missedScores.tooltips.push(
                this.translateService.instant('artemisApp.courseOverview.statistics.exerciseMissedScore', {
                    points: '',
                    percentage: 100,
                }),
            );
            return;
        }

        const replaced = result.resultString.replace(',', '.');
        const split = replaced.split(' ');

        // custom result strings
        if (!replaced.includes('passed') && !replaced.includes('points')) {
            if (result.score! >= 50) {
                groupedExercise.scores.tooltips.push(`${result.resultString} (${result.score}%)`);
                groupedExercise.missedScores.tooltips.push(`(${100 - result.score!}%)`);
            } else {
                groupedExercise.scores.tooltips.push(`(${result.score}%)`);
                groupedExercise.missedScores.tooltips.push(`${result.resultString} (${100 - result.score!}%)`);
            }

            return;
        }

        // exercise results strings are mostly 'x points' or 'x of y points'
        if (replaced.includes('points')) {
            if (split.length === 2) {
                groupedExercise.scores.tooltips.push(
                    this.translateService.instant('artemisApp.courseOverview.statistics.exerciseAchievedScore', {
                        points: parseFloat(split[0]),
                        percentage: result.score,
                    }),
                );
                groupedExercise.missedScores.tooltips.push(
                    this.translateService.instant('artemisApp.courseOverview.statistics.exerciseMissedScore', {
                        points: '',
                        percentage: 100 - result.score!,
                    }),
                );
                return;
            }
            if (split.length === 4) {
                groupedExercise.scores.tooltips.push(
                    this.translateService.instant('artemisApp.courseOverview.statistics.exerciseAchievedScore', {
                        points: parseFloat(split[0]),
                        percentage: result.score,
                    }),
                );
                groupedExercise.missedScores.tooltips.push(
                    this.translateService.instant('artemisApp.courseOverview.statistics.exerciseMissedScore', {
                        points: parseFloat(split[2]) - parseFloat(split[0]),
                        percentage: 100 - result.score!,
                    }),
                );
                return;
            }
        }

        // programming exercise result strings are mostly 'x passed' or 'x of y passed'
        if (replaced.includes('passed')) {
            if (split.length === 2) {
                groupedExercise.scores.tooltips.push(parseFloat(split[0]) + ' tests passed (' + result.score + '%).');
                groupedExercise.missedScores.tooltips.push('(' + (100 - result.score!) + '%)');
                return;
            }
            if (split.length === 4) {
                groupedExercise.scores.tooltips.push(parseFloat(split[0]) + ' tests passed (' + result.score + '%).');
                groupedExercise.missedScores.tooltips.push(parseFloat(split[2]) - parseFloat(split[0]) + ' tests failed (' + (100 - result.score!) + '%).');
                return;
            }
        }
    }

    calculateAbsoluteScores(): void {
        const quizzesTotalScore = this.calculateScoreTypeForExerciseType(ExerciseType.QUIZ, ABSOLUTE_SCORE);
        const programmingExerciseTotalScore = this.calculateScoreTypeForExerciseType(ExerciseType.PROGRAMMING, ABSOLUTE_SCORE);
        const modelingExerciseTotalScore = this.calculateScoreTypeForExerciseType(ExerciseType.MODELING, ABSOLUTE_SCORE);
        const textExerciseTotalScore = this.calculateScoreTypeForExerciseType(ExerciseType.TEXT, ABSOLUTE_SCORE);
        const fileUploadExerciseTotalScore = this.calculateScoreTypeForExerciseType(ExerciseType.FILE_UPLOAD, ABSOLUTE_SCORE);
        this.totalScore = this.calculateTotalScoreForTheCourse(ABSOLUTE_SCORE);
        const totalMissedPoints = this.reachableScore - this.totalScore;
        const absoluteScores = {};
        absoluteScores[ExerciseType.QUIZ] = quizzesTotalScore;
        absoluteScores[ExerciseType.PROGRAMMING] = programmingExerciseTotalScore;
        absoluteScores[ExerciseType.MODELING] = modelingExerciseTotalScore;
        absoluteScores[ExerciseType.TEXT] = textExerciseTotalScore;
        absoluteScores[ExerciseType.FILE_UPLOAD] = fileUploadExerciseTotalScore;
        this.absoluteScores = absoluteScores;
        this.doughnutChartData[0].data = [
            quizzesTotalScore,
            programmingExerciseTotalScore,
            modelingExerciseTotalScore,
            textExerciseTotalScore,
            fileUploadExerciseTotalScore,
            totalMissedPoints,
        ];
    }

    calculateMaxScores() {
        const quizzesTotalMaxScore = this.calculateScoreTypeForExerciseType(ExerciseType.QUIZ, MAX_SCORE);
        const programmingExerciseTotalMaxScore = this.calculateScoreTypeForExerciseType(ExerciseType.PROGRAMMING, MAX_SCORE);
        const modelingExerciseTotalMaxScore = this.calculateScoreTypeForExerciseType(ExerciseType.MODELING, MAX_SCORE);
        const textExerciseTotalMaxScore = this.calculateScoreTypeForExerciseType(ExerciseType.TEXT, MAX_SCORE);
        const fileUploadExerciseTotalMaxScore = this.calculateScoreTypeForExerciseType(ExerciseType.FILE_UPLOAD, MAX_SCORE);
        const totalMaxScores = {};
        totalMaxScores[ExerciseType.QUIZ] = quizzesTotalMaxScore;
        totalMaxScores[ExerciseType.PROGRAMMING] = programmingExerciseTotalMaxScore;
        totalMaxScores[ExerciseType.MODELING] = modelingExerciseTotalMaxScore;
        totalMaxScores[ExerciseType.TEXT] = textExerciseTotalMaxScore;
        totalMaxScores[ExerciseType.FILE_UPLOAD] = fileUploadExerciseTotalMaxScore;
        this.totalMaxScores = totalMaxScores;
        this.totalMaxScore = this.calculateTotalScoreForTheCourse('maxScore');
    }

    calculateRelativeScores(): void {
        const quizzesRelativeScore = this.calculateScoreTypeForExerciseType(ExerciseType.QUIZ, RELATIVE_SCORE);
        const programmingExerciseRelativeScore = this.calculateScoreTypeForExerciseType(ExerciseType.PROGRAMMING, RELATIVE_SCORE);
        const modelingExerciseRelativeScore = this.calculateScoreTypeForExerciseType(ExerciseType.MODELING, RELATIVE_SCORE);
        const textExerciseRelativeScore = this.calculateScoreTypeForExerciseType(ExerciseType.TEXT, RELATIVE_SCORE);
        const fileUploadExerciseRelativeScore = this.calculateScoreTypeForExerciseType(ExerciseType.FILE_UPLOAD, RELATIVE_SCORE);
        const relativeScores = {};
        relativeScores[ExerciseType.QUIZ] = quizzesRelativeScore;
        relativeScores[ExerciseType.PROGRAMMING] = programmingExerciseRelativeScore;
        relativeScores[ExerciseType.MODELING] = modelingExerciseRelativeScore;
        relativeScores[ExerciseType.TEXT] = textExerciseRelativeScore;
        relativeScores[ExerciseType.FILE_UPLOAD] = fileUploadExerciseRelativeScore;
        this.relativeScores = relativeScores;
        this.totalRelativeScore = this.calculateTotalScoreForTheCourse(RELATIVE_SCORE);
    }

    calculateReachableScores() {
        const quizzesTotalCurrentMaxScore = this.calculateScoreTypeForExerciseType(ExerciseType.QUIZ, REACHABLE_SCORE);
        const programmingExerciseTotalCurrentMaxScore = this.calculateScoreTypeForExerciseType(ExerciseType.PROGRAMMING, REACHABLE_SCORE);
        const modelingExerciseTotalCurrentMaxScore = this.calculateScoreTypeForExerciseType(ExerciseType.MODELING, REACHABLE_SCORE);
        const textExerciseTotalCurrentMaxScore = this.calculateScoreTypeForExerciseType(ExerciseType.TEXT, REACHABLE_SCORE);
        const fileUploadExerciseTotalCurrentMaxScore = this.calculateScoreTypeForExerciseType(ExerciseType.FILE_UPLOAD, REACHABLE_SCORE);
        const reachableScores = {};
        reachableScores[ExerciseType.QUIZ] = quizzesTotalCurrentMaxScore;
        reachableScores[ExerciseType.PROGRAMMING] = programmingExerciseTotalCurrentMaxScore;
        reachableScores[ExerciseType.MODELING] = modelingExerciseTotalCurrentMaxScore;
        reachableScores[ExerciseType.TEXT] = textExerciseTotalCurrentMaxScore;
        reachableScores[ExerciseType.FILE_UPLOAD] = fileUploadExerciseTotalCurrentMaxScore;
        this.reachableScores = reachableScores;
        this.reachableScore = this.calculateTotalScoreForTheCourse(REACHABLE_SCORE);
    }

    calculateCurrentRelativeScores(): void {
        const quizzesCurrentRelativeScore = this.calculateScoreTypeForExerciseType(ExerciseType.QUIZ, CURRENT_RELATIVE_SCORE);
        const programmingExerciseCurrentRelativeScore = this.calculateScoreTypeForExerciseType(ExerciseType.PROGRAMMING, CURRENT_RELATIVE_SCORE);
        const modelingExerciseCurrentRelativeScore = this.calculateScoreTypeForExerciseType(ExerciseType.MODELING, CURRENT_RELATIVE_SCORE);
        const textExerciseCurrentRelativeScore = this.calculateScoreTypeForExerciseType(ExerciseType.TEXT, CURRENT_RELATIVE_SCORE);
        const fileUploadExerciseCurrentRelativeScore = this.calculateScoreTypeForExerciseType(ExerciseType.FILE_UPLOAD, CURRENT_RELATIVE_SCORE);
        const currentRelativeScores = {};
        currentRelativeScores[ExerciseType.QUIZ] = quizzesCurrentRelativeScore;
        currentRelativeScores[ExerciseType.PROGRAMMING] = programmingExerciseCurrentRelativeScore;
        currentRelativeScores[ExerciseType.MODELING] = modelingExerciseCurrentRelativeScore;
        currentRelativeScores[ExerciseType.TEXT] = textExerciseCurrentRelativeScore;
        currentRelativeScores[ExerciseType.FILE_UPLOAD] = fileUploadExerciseCurrentRelativeScore;
        this.currentRelativeScores = currentRelativeScores;
        this.currentRelativeScore = this.calculateTotalScoreForTheCourse(CURRENT_RELATIVE_SCORE);
    }

    calculatePresentationScores(): void {
        const programmingExercisePresentationScore = this.calculateScoreTypeForExerciseType(ExerciseType.PROGRAMMING, PRESENTATION_SCORE);
        const modelingExercisePresentationScore = this.calculateScoreTypeForExerciseType(ExerciseType.MODELING, PRESENTATION_SCORE);
        const textExercisePresentationScore = this.calculateScoreTypeForExerciseType(ExerciseType.TEXT, PRESENTATION_SCORE);
        const fileUploadExercisePresentationScore = this.calculateScoreTypeForExerciseType(ExerciseType.FILE_UPLOAD, PRESENTATION_SCORE);
        const presentationScores = {};
        presentationScores[ExerciseType.QUIZ] = 0;
        presentationScores[ExerciseType.PROGRAMMING] = programmingExercisePresentationScore;
        presentationScores[ExerciseType.MODELING] = modelingExercisePresentationScore;
        presentationScores[ExerciseType.TEXT] = textExercisePresentationScore;
        presentationScores[ExerciseType.FILE_UPLOAD] = fileUploadExercisePresentationScore;
        this.presentationScores = presentationScores;
        this.totalPresentationScore = this.calculateTotalScoreForTheCourse(PRESENTATION_SCORE);
    }

    calculateScores(filterFunction: (courseExercise: Exercise) => boolean) {
        let courseExercises = this.courseExercises;
        if (filterFunction) {
            courseExercises = courseExercises.filter(filterFunction);
        }
        return this.courseCalculationService.calculateTotalScores(courseExercises);
    }

    calculateScoreTypeForExerciseType(exerciseType: ExerciseType, scoreType: string): number {
        if (exerciseType !== undefined && scoreType !== undefined) {
            const filterFunction = (courseExercise: Exercise) => courseExercise.type === exerciseType;
            const scores = this.calculateScores(filterFunction);
            return scores.get(scoreType)!;
        } else {
            return NaN;
        }
    }

    calculateTotalScoreForTheCourse(scoreType: string): number {
        const scores = this.courseCalculationService.calculateTotalScores(this.courseExercises);
        return scores.get(scoreType)!;
    }
}
