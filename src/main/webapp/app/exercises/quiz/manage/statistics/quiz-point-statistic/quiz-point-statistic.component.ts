import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { ChartOptions } from 'chart.js';
import { calculateTickMax, createOptions, DataSet, DataSetProvider } from '../quiz-statistic/quiz-statistic.component';
import { Subscription } from 'rxjs/Subscription';
import * as moment from 'moment';
import { QuizStatisticUtil } from 'app/exercises/quiz/shared/quiz-statistic-util.service';
import { AccountService } from 'app/core/auth/account.service';
import { JhiWebsocketService } from 'app/core/websocket/websocket.service';
import { PointCounter } from 'app/entities/quiz/point-counter.model';
import { QuizExerciseService } from 'app/exercises/quiz/manage/quiz-exercise.service';
import { QuizPointStatistic } from 'app/entities/quiz/quiz-point-statistic.model';
import { QuizExercise } from 'app/entities/quiz/quiz-exercise.model';

@Component({
    selector: 'jhi-quiz-point-statistic',
    templateUrl: './quiz-point-statistic.component.html',
})
export class QuizPointStatisticComponent implements OnInit, OnDestroy, DataSetProvider {
    quizExercise: QuizExercise;
    quizPointStatistic: QuizPointStatistic;
    private sub: Subscription;

    labels: string[] = [];
    data: number[] = [];
    colors: string[] = [];
    chartType = 'bar';
    datasets: DataSet[] = [];

    label: string[] = [];
    ratedData: number[] = [];
    unratedData: number[] = [];
    backgroundColor: string[] = [];

    maxScore: number;
    rated = true;
    participants: number;
    websocketChannelForData: string;
    quizExerciseChannel: string;

    // options for chart.js style
    options: ChartOptions;

    // timer
    waitingForQuizStart = false;
    remainingTimeText = '?';
    remainingTimeSeconds = 0;
    interval: any;

    constructor(
        private route: ActivatedRoute,
        private router: Router,
        private accountService: AccountService,
        private translateService: TranslateService,
        private quizExerciseService: QuizExerciseService,
        private quizStatisticUtil: QuizStatisticUtil,
        private jhiWebsocketService: JhiWebsocketService,
    ) {
        this.options = createOptions(this);
    }

    ngOnInit() {
        this.sub = this.route.params.subscribe((params) => {
            // use different REST-call if the User is a Student
            if (this.accountService.hasAnyAuthorityDirect(['ROLE_ADMIN', 'ROLE_INSTRUCTOR', 'ROLE_TA'])) {
                this.quizExerciseService.find(params['exerciseId']).subscribe((res) => {
                    this.loadQuizSuccess(res.body!);
                });
            }

            // subscribe websocket for new statistical data
            this.websocketChannelForData = '/topic/statistic/' + params['exerciseId'];
            this.jhiWebsocketService.subscribe(this.websocketChannelForData);

            if (!this.quizExerciseChannel) {
                this.quizExerciseChannel = '/topic/courses/' + params['courseId'] + '/quizExercises';

                // quizExercise channel => react to changes made to quizExercise (e.g. start date)
                this.jhiWebsocketService.subscribe(this.quizExerciseChannel);
                this.jhiWebsocketService.receive(this.quizExerciseChannel).subscribe(
                    (quiz) => {
                        if (this.waitingForQuizStart && params['exerciseId'] === quiz.id) {
                            this.loadQuizSuccess(quiz);
                        }
                    },
                    () => {},
                );
            }

            // ask for new Data if the websocket for new statistical data was notified
            this.jhiWebsocketService.receive(this.websocketChannelForData).subscribe((quiz) => {
                this.loadNewData(quiz.quizPointStatistic);
            });

            // add Axes-labels based on selected language
            this.translateService.get('showStatistic.quizPointStatistic.xAxes').subscribe((xLabel) => {
                this.options.scales!.xAxes![0].scaleLabel!.labelString = xLabel;
            });
            this.translateService.get('showStatistic.quizPointStatistic.yAxes').subscribe((yLabel) => {
                this.options.scales!.yAxes![0].scaleLabel!.labelString = yLabel;
            });
        });

        // update displayed times in UI regularly
        this.interval = setInterval(() => {
            this.updateDisplayedTimes();
        }, 200);
    }

    /**
     * updates all displayed (relative) times in the UI
     */
    updateDisplayedTimes() {
        const translationBasePath = 'showStatistic.';
        // update remaining time
        if (this.quizExercise && this.quizExercise.adjustedDueDate) {
            const endDate = this.quizExercise.adjustedDueDate;
            if (endDate.isAfter(moment())) {
                // quiz is still running => calculate remaining seconds and generate text based on that
                this.remainingTimeSeconds = endDate.diff(moment(), 'seconds');
                this.remainingTimeText = this.relativeTimeText(this.remainingTimeSeconds);
            } else {
                // quiz is over => set remaining seconds to negative, to deactivate 'Submit' button
                this.remainingTimeSeconds = -1;
                this.remainingTimeText = this.translateService.instant(translationBasePath + 'quizhasEnded');
            }
        } else {
            // remaining time is unknown => Set remaining seconds to 0, to keep 'Submit' button enabled
            this.remainingTimeSeconds = 0;
            this.remainingTimeText = '?';
        }
    }

    /**
     * Express the given timespan as humanized text
     *
     * @param remainingTimeSeconds {number} the amount of seconds to display
     * @return {string} humanized text for the given amount of seconds
     */
    relativeTimeText(remainingTimeSeconds: number) {
        if (remainingTimeSeconds > 210) {
            return Math.ceil(remainingTimeSeconds / 60) + ' min';
        } else if (remainingTimeSeconds > 59) {
            return Math.floor(remainingTimeSeconds / 60) + ' min ' + (remainingTimeSeconds % 60) + ' s';
        } else {
            return remainingTimeSeconds + ' s';
        }
    }

    ngOnDestroy() {
        clearInterval(this.interval);
        this.jhiWebsocketService.unsubscribe(this.websocketChannelForData);
    }

    getDataSets() {
        return this.datasets;
    }

    getParticipants() {
        return this.participants;
    }

    /**
     * load the new quizPointStatistic from the server if the Websocket has been notified
     *
     * @param {QuizPointStatistic} statistic: the new quizPointStatistic
     *                                          from the server with the new Data.
     */
    loadNewData(statistic: QuizPointStatistic) {
        // if the Student finds a way to the Website
        //      -> the Student will be send back to Courses
        if (!this.accountService.hasAnyAuthorityDirect(['ROLE_ADMIN', 'ROLE_INSTRUCTOR', 'ROLE_TA'])) {
            this.router.navigate(['courses']);
        }
        this.quizPointStatistic = statistic;
        this.loadData();
    }

    /**
     * This functions loads the Quiz, which is necessary to build the Web-Template
     *
     * @param {QuizExercise} quizExercise: the quizExercise,
     *                              which the this quiz-point-statistic presents.
     */
    loadQuizSuccess(quizExercise: QuizExercise) {
        // if the Student finds a way to the Website
        //      -> the Student will be send back to Courses
        if (!this.accountService.hasAnyAuthorityDirect(['ROLE_ADMIN', 'ROLE_INSTRUCTOR', 'ROLE_TA'])) {
            this.router.navigate(['courses']);
        }
        this.quizExercise = quizExercise;
        this.quizExercise.adjustedDueDate = moment().add(this.quizExercise.remainingTime, 'seconds');
        this.waitingForQuizStart = !this.quizExercise.started;
        this.quizPointStatistic = this.quizExercise.quizPointStatistic!;
        this.maxScore = this.calculateMaxScore();

        this.loadData();
    }

    /**
     * calculate the maximal  possible Score for the quiz
     *
     * @return (number): sum over the Scores of all questions
     */
    calculateMaxScore() {
        let result = 0;

        this.quizExercise.quizQuestions!.forEach((question) => {
            result = result + question.score!;
        });
        return result;
    }

    /**
     * load the Data from the Json-entity to the chart: myChart
     */
    loadData() {
        // reset old data
        this.label = [];
        this.backgroundColor = [];
        this.ratedData = [];
        this.unratedData = [];
        // set data based on the pointCounters
        this.order(this.quizPointStatistic.pointCounters!).forEach((pointCounter) => {
            this.label.push(pointCounter.points!.toString());
            this.ratedData.push(pointCounter.ratedCounter!);
            this.unratedData.push(pointCounter.unRatedCounter!);
            this.backgroundColor.push('#428bca');
        });

        this.labels = this.label;
        this.colors = this.backgroundColor;

        // load data into the chart
        this.loadDataInDiagram();
    }

    /**
     * check if the rated or unrated
     * load the rated or unrated data into the diagram
     */
    loadDataInDiagram() {
        if (this.rated) {
            this.participants = this.quizPointStatistic.participantsRated!;
            this.data = this.ratedData;
        } else {
            // load the unrated data
            this.participants = this.quizPointStatistic.participantsUnrated!;
            this.data = this.unratedData;
        }
        this.datasets = [
            {
                data: this.data,
                backgroundColor: this.colors,
            },
        ];
        this.options.scales!.yAxes![0]!.ticks!.max = calculateTickMax(this);
    }

    /**
     *
     * Recalculate the complete statistic on the server in case something went wrong with it
     *
     */
    recalculate() {
        this.quizExerciseService.recalculate(this.quizExercise.id!).subscribe((res) => {
            this.loadQuizSuccess(res.body!);
        });
    }

    /**
     * switch between showing and hiding the solution in the chart
     *  1. change the amount of  participants
     *  2. change the bar-Data
     */
    switchRated() {
        this.rated = !this.rated;
        this.loadDataInDiagram();
    }

    /**
     * order the point cursors ascending
     */
    order(pointCursors: Array<PointCounter>) {
        // TODO: use sorting service
        return pointCursors.sort((a: PointCounter, b: PointCounter) => {
            if (a.points! < b.points!) {
                return -1;
            }
            if (a.points! > b.points!) {
                return 1;
            }
            // a must be equal to b
            return 0;
        });
    }
}
