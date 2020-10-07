import { Injectable } from '@angular/core';
import { Result } from 'app/entities/result.model';
import { Course } from 'app/entities/course.model';
import { Exercise, ExerciseType } from 'app/entities/exercise.model';
import * as moment from 'moment';
import { Moment } from 'moment';
import { StudentParticipation } from 'app/entities/participation/student-participation.model';
import { InitializationState, Participation } from 'app/entities/participation/participation.model';

export const ABSOLUTE_SCORE = 'absoluteScore';
export const RELATIVE_SCORE = 'relativeScore';
export const MAX_SCORE = 'maxScore';
export const PRESENTATION_SCORE = 'presentationScore';
export const REACHABLE_SCORE = 'reachableScore';
export const CURRENT_RELATIVE_SCORE = 'currentRelativeScore';

@Injectable({ providedIn: 'root' })
export class CourseScoreCalculationService {
    private SCORE_NORMALIZATION_VALUE = 0.01;
    private courses: Course[] = [];

    constructor() {}

    calculateTotalScores(courseExercises: Exercise[]): Map<string, number> {
        const scores = new Map<string, number>();
        let absoluteScore = 0.0;
        let maxScore = 0;
        let reachableScore = 0;
        let presentationScore = 0;
        for (const exercise of courseExercises) {
            if (exercise.maxScore != null && (!exercise.dueDate || exercise.dueDate.isBefore(moment()))) {
                maxScore = maxScore + exercise.maxScore;
                const participation = this.getParticipationForExercise(exercise);
                if (participation) {
                    const result = this.getResultForParticipation(participation, exercise.dueDate!);
                    if (result && result.rated) {
                        let score = result.score;
                        // this should cover score is undefined and score is null
                        if (score == null) {
                            score = 0;
                        }
                        absoluteScore = absoluteScore + score * this.SCORE_NORMALIZATION_VALUE * exercise.maxScore;
                        reachableScore += exercise.maxScore;
                    }
                    presentationScore += participation.presentationScore ? participation.presentationScore : 0;

                    // programming exercises can be excluded here because their state is INITIALIZED even after the exercise is over
                    if (participation.initializationState === InitializationState.INITIALIZED && exercise.type !== ExerciseType.PROGRAMMING) {
                        reachableScore += exercise.maxScore;
                    }
                } else {
                    reachableScore += exercise.maxScore;
                }
            }
        }
        scores.set(ABSOLUTE_SCORE, CourseScoreCalculationService.round(absoluteScore, 1));
        if (maxScore > 0) {
            scores.set(RELATIVE_SCORE, CourseScoreCalculationService.round((absoluteScore / maxScore) * 100, 1));
        } else {
            scores.set(RELATIVE_SCORE, 0);
        }
        if (reachableScore > 0) {
            scores.set(CURRENT_RELATIVE_SCORE, CourseScoreCalculationService.round((absoluteScore / reachableScore) * 100, 1));
        } else {
            scores.set(CURRENT_RELATIVE_SCORE, 0);
        }
        scores.set(MAX_SCORE, maxScore);
        scores.set(PRESENTATION_SCORE, presentationScore);
        scores.set(REACHABLE_SCORE, reachableScore);
        return scores;
    }

    private static round(value: any, exp: number) {
        // helper function to make actually rounding possible
        if (typeof exp === 'undefined' || +exp === 0) {
            return Math.round(value);
        }

        value = +value;
        exp = +exp;

        if (isNaN(value) || !(exp % 1 === 0)) {
            return NaN;
        }

        // Shift
        value = value.toString().split('e');
        value = Math.round(+(value[0] + 'e' + (value[1] ? +value[1] + exp : exp)));

        // Shift back
        value = value.toString().split('e');
        return +(value[0] + 'e' + (value[1] ? +value[1] - exp : -exp));
    }

    updateCourse(course: Course) {
        // filter out the old course object with the same id
        this.courses = this.courses.filter((existingCourses) => existingCourses.id !== existingCourses.id);
        this.courses.push(course);
    }

    setCourses(courses: Course[]) {
        this.courses = courses;
    }

    getCourse(courseId: number) {
        return this.courses.find((course) => course.id === courseId);
    }

    getParticipationForExercise(exercise: Exercise) {
        if (exercise.studentParticipations != null && exercise.studentParticipations.length > 0) {
            const exerciseParticipation: StudentParticipation = exercise.studentParticipations[0];
            return CourseScoreCalculationService.convertDateForParticipationFromServer(exerciseParticipation);
        }
    }

    getResultForParticipation(participation: Participation | undefined, dueDate: Moment) {
        if (!participation) {
            return undefined;
        }
        const results = participation.results;
        const resultsArray: Result[] = [];
        let chosenResult: Result;

        if (results) {
            for (let i = 0; i < results.length; i++) {
                resultsArray.push(CourseScoreCalculationService.convertDateForResultFromServer(results[i]));
            }

            if (resultsArray.length <= 0) {
                chosenResult = new Result();
                chosenResult.score = 0;
                return chosenResult;
            }

            const ratedResults = resultsArray.filter((el) => el.rated);

            if (ratedResults.length === 1) {
                return ratedResults[0];
            }

            // sorting in descending order to have the last result at the beginning
            resultsArray.sort((result1, result2): number => {
                if (result1.completionDate! > result2.completionDate!) {
                    return -1;
                }
                if (result1.completionDate! < result2.completionDate!) {
                    return 1;
                }
                return 0;
            });

            const gracePeriodInSeconds = 10;
            if (dueDate === null || dueDate.add(gracePeriodInSeconds, 'seconds') >= resultsArray[0].completionDate!) {
                // find the first result that is before the due date
                chosenResult = resultsArray[0];
            } else if (dueDate.add(gracePeriodInSeconds, 'seconds') < resultsArray[0].completionDate!) {
                chosenResult = new Result();
                chosenResult.score = 0;
            } else {
                chosenResult = resultsArray[resultsArray.length - 1];
            }
        } else {
            chosenResult = new Result();
            chosenResult.score = 0;
        }

        return chosenResult;
    }

    private static convertDateForResultFromServer(result: Result): Result {
        result.completionDate = result.completionDate ? moment(result.completionDate) : undefined;
        return result;
    }

    private static convertDateForParticipationFromServer(participation: Participation): Participation {
        participation.initializationDate = participation.initializationDate ? moment(participation.initializationDate) : undefined;
        return participation;
    }
}
