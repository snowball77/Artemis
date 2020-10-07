import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import * as moment from 'moment';
import { SERVER_API_URL } from 'app/app.constants';
import { Exam } from 'app/entities/exam.model';
import { createRequestOption } from 'app/shared/util/request-util';
import { StudentDTO } from 'app/entities/student-dto.model';
import { StudentExam } from 'app/entities/student-exam.model';
import { ExerciseGroup } from 'app/entities/exercise-group.model';
import { ExamScoreDTO } from 'app/exam/exam-scores/exam-score-dtos.model';
import { ExamInformationDTO } from 'app/entities/exam-information.model';

type EntityResponseType = HttpResponse<Exam>;
type EntityArrayResponseType = HttpResponse<Exam[]>;

@Injectable({ providedIn: 'root' })
export class ExamManagementService {
    public resourceUrl = SERVER_API_URL + 'api/courses';

    constructor(private router: Router, private http: HttpClient) {}

    /**
     * Create an exam on the server using a POST request.
     * @param courseId The course id.
     * @param exam The exam to create.
     */
    create(courseId: number, exam: Exam): Observable<EntityResponseType> {
        const copy = ExamManagementService.convertDateFromClient(exam);
        return this.http
            .post<Exam>(`${this.resourceUrl}/${courseId}/exams`, copy, { observe: 'response' })
            .pipe(map((res: EntityResponseType) => ExamManagementService.convertDateFromServer(res)));
    }

    /**
     * Update an exam on the server using a PUT request.
     * @param courseId The course id.
     * @param exam The exam to update.
     */
    update(courseId: number, exam: Exam): Observable<EntityResponseType> {
        const copy = ExamManagementService.convertDateFromClient(exam);
        return this.http
            .put<Exam>(`${this.resourceUrl}/${courseId}/exams`, copy, { observe: 'response' })
            .pipe(map((res: EntityResponseType) => ExamManagementService.convertDateFromServer(res)));
    }

    /**
     * Find an exam on the server using a GET request.
     * @param courseId The course id.
     * @param examId The id of the exam to get.
     * @param withStudents Boolean flag whether to fetch all students registered for the exam
     * @param withExerciseGroups Boolean flag whether to fetch all exercise groups of the exam
     */
    find(courseId: number, examId: number, withStudents = false, withExerciseGroups = false): Observable<EntityResponseType> {
        const options = createRequestOption({ withStudents, withExerciseGroups });
        return this.http
            .get<Exam>(`${this.resourceUrl}/${courseId}/exams/${examId}`, { params: options, observe: 'response' })
            .pipe(map((res: EntityResponseType) => ExamManagementService.convertDateFromServer(res)));
    }

    /**
     * Find all scores of an exam.
     * @param courseId The id of the course.
     * @param examId The id of the exam.
     */
    getExamScores(courseId: number, examId: number): Observable<HttpResponse<ExamScoreDTO>> {
        return this.http.get<ExamScoreDTO>(`${this.resourceUrl}/${courseId}/exams/${examId}/scores`, { observe: 'response' });
    }

    /**
     * Query exams of the given course via get request.
     * @param courseId The course id.
     * @param req The query request options.
     */
    query(courseId: number, req?: any): Observable<EntityArrayResponseType> {
        const options = createRequestOption(req);
        return this.http
            .get<Exam[]>(`${this.resourceUrl}/${courseId}/exams`, { params: options, observe: 'response' })
            .pipe(map((res: EntityArrayResponseType) => ExamManagementService.convertDateArrayFromServer(res)));
    }

    /**
     * Find all exams for the given course.
     * @param courseId The course id.
     */
    findAllExamsForCourse(courseId: number): Observable<HttpResponse<Exam[]>> {
        return this.http
            .get<Exam[]>(`${this.resourceUrl}/${courseId}/exams`, { observe: 'response' })
            .pipe(map((res: EntityArrayResponseType) => ExamManagementService.convertDateArrayFromServer(res)));
    }

    /**
     * Returns the exam with the provided unique identifier for the tutor dashboard
     * @param courseId - the id of the course
     * @param examId - the id of the exam
     * @param isTestRun - boolean to determine whether it is a test run
     */
    getExamWithInterestingExercisesForTutorDashboard(courseId: number, examId: number, isTestRun: boolean): Observable<EntityResponseType> {
        let url: string;
        if (isTestRun) {
            url = `${this.resourceUrl}/${courseId}/exams/${examId}/for-exam-tutor-test-run-dashboard`;
        } else {
            url = `${this.resourceUrl}/${courseId}/exams/${examId}/for-exam-tutor-dashboard`;
        }
        return this.http
            .get<Exam>(url, { observe: 'response' })
            .pipe(map((res: EntityResponseType) => ExamManagementService.convertDateFromServer(res)));
    }

    getLatestIndividualEndDateOfExam(courseId: number, examId: number): Observable<HttpResponse<ExamInformationDTO>> {
        const url = `${this.resourceUrl}/${courseId}/exams/${examId}/latest-end-date`;
        return this.http
            .get<ExamInformationDTO>(url, { observe: 'response' })
            .pipe(
                map((res: HttpResponse<ExamInformationDTO>) => {
                    res.body!.latestIndividualEndDate = moment(res.body!.latestIndividualEndDate);
                    return res;
                }),
            );
    }

    /**
     * Delete an exam on the server using a DELETE request.
     * @param courseId The course id.
     * @param examId The id of the exam to delete.
     */
    delete(courseId: number, examId: number): Observable<HttpResponse<any>> {
        return this.http.delete<any>(`${this.resourceUrl}/${courseId}/exams/${examId}`, { observe: 'response' });
    }

    /**
     * Add a student to the registered users for an exam
     * @param courseId The course id.
     * @param examId The id of the exam to which to add the student
     * @param studentLogin Login of the student
     */
    addStudentToExam(courseId: number, examId: number, studentLogin: string): Observable<HttpResponse<any>> {
        return this.http.post<any>(`${this.resourceUrl}/${courseId}/exams/${examId}/students/${studentLogin}`, { observe: 'response' });
    }

    /**
     * Add students to the registered users for an exam
     * @param courseId The course id.
     * @param examId The id of the exam to which to add the student
     * @param studentDtos Student DTOs of student to add to the exam
     * @return studentDtos of students that were not found in the system
     */
    addStudentsToExam(courseId: number, examId: number, studentDtos: StudentDTO[]): Observable<HttpResponse<StudentDTO[]>> {
        return this.http.post<any>(`${this.resourceUrl}/${courseId}/exams/${examId}/students`, studentDtos, { observe: 'response' });
    }

    /**
     * Remove a student to the registered users for an exam
     * @param courseId The course id.
     * @param examId The id of the exam from which to remove the student
     * @param studentLogin Login of the student
     * @param withParticipationsAndSubmission
     */
    removeStudentFromExam(courseId: number, examId: number, studentLogin: string, withParticipationsAndSubmission = false): Observable<HttpResponse<any>> {
        const options = createRequestOption({ withParticipationsAndSubmission });
        return this.http.delete<any>(`${this.resourceUrl}/${courseId}/exams/${examId}/students/${studentLogin}`, { params: options, observe: 'response' });
    }

    /**
     * Generate all student exams for all registered students of the exam.
     * @param courseId
     * @param examId
     * @returns a list with the generate student exams
     */
    generateStudentExams(courseId: number, examId: number): Observable<HttpResponse<StudentExam[]>> {
        return this.http.post<any>(`${this.resourceUrl}/${courseId}/exams/${examId}/generate-student-exams`, {}, { observe: 'response' });
    }

    /**
     * Generate a test run student exam based on the testRunConfiguration.
     * @param courseId the id of the course
     * @param examId the id of the exam
     * @param testRunConfiguration the desired configuration
     * @returns the created test run
     */
    createTestRun(courseId: number, examId: number, testRunConfiguration: StudentExam): Observable<HttpResponse<StudentExam>> {
        return this.http.post<StudentExam>(`${this.resourceUrl}/${courseId}/exams/${examId}/test-run`, testRunConfiguration, { observe: 'response' });
    }

    /**
     * Delete a test run
     * @param courseId the id of the course
     * @param examId the id of the exam
     * @param testRunId the id of the test run
     */
    deleteTestRun(courseId: number, examId: number, testRunId: number): Observable<HttpResponse<StudentExam>> {
        return this.http.delete<StudentExam>(`${this.resourceUrl}/${courseId}/exams/${examId}/test-run/${testRunId}`, { observe: 'response' });
    }

    /**
     * Find all the test runs for the exam
     * @param courseId the id of the course
     * @param examId the id of the exam
     */
    findAllTestRunsForExam(courseId: number, examId: number): Observable<HttpResponse<StudentExam[]>> {
        return this.http.get<StudentExam[]>(`${this.resourceUrl}/${courseId}/exams/${examId}/test-runs`, { observe: 'response' });
    }

    /**
     * Generate missing student exams for newly added students of the exam.
     * @param courseId
     * @param examId
     * @returns a list with the generate student exams
     */
    generateMissingStudentExams(courseId: number, examId: number): Observable<HttpResponse<StudentExam[]>> {
        return this.http.post<any>(`${this.resourceUrl}/${courseId}/exams/${examId}/generate-missing-student-exams`, {}, { observe: 'response' });
    }

    /**
     * Start all the exercises for all the student exams belonging to the exam
     * @param courseId course to which the exam belongs
     * @param examId exam to which the student exams belong
     * @returns number of generated participations
     */
    startExercises(courseId: number, examId: number): Observable<HttpResponse<number>> {
        return this.http.post<any>(`${this.resourceUrl}/${courseId}/exams/${examId}/student-exams/start-exercises`, {}, { observe: 'response' });
    }

    /**
     * Evaluate all the quiz exercises belonging to the exam
     * @param courseId id of the course to which the exam belongs
     * @param examId id of the exam for which the quiz exercises should be evaluated
     * @returns number of evaluated exercises
     */
    evaluateQuizExercises(courseId: number, examId: number): Observable<HttpResponse<number>> {
        return this.http.post<any>(`${this.resourceUrl}/${courseId}/exams/${examId}/student-exams/evaluate-quiz-exercises`, {}, { observe: 'response' });
    }

    /**
     * Unlock all the programming exercises belonging to the exam
     * @param courseId id of the course to which the exam belongs
     * @param examId id of the exam for which the programming exercises should be unlocked
     * @returns number of exercises for which the repositories were unlocked
     */
    unlockAllRepositories(courseId: number, examId: number): Observable<HttpResponse<number>> {
        return this.http.post<any>(`${this.resourceUrl}/${courseId}/exams/${examId}/student-exams/unlock-all-repositories`, {}, { observe: 'response' });
    }

    /**
     * Lock all the programming exercises belonging to the exam
     * @param courseId id of the course to which the exam belongs
     * @param examId id of the exam for which the programming exercises should be locked
     * @returns number of exercises for which the repositories were locked
     */
    lockAllRepositories(courseId: number, examId: number): Observable<HttpResponse<number>> {
        return this.http.post<any>(`${this.resourceUrl}/${courseId}/exams/${examId}/student-exams/lock-all-repositories`, {}, { observe: 'response' });
    }

    /**
     * Save the exercise groups of an exam in the given order.
     * @param courseId The course id.
     * @param examId The exam id.
     * @param exerciseGroups List of exercise groups.
     */
    updateOrder(courseId: number, examId: number, exerciseGroups: ExerciseGroup[]): Observable<HttpResponse<ExerciseGroup[]>> {
        return this.http.put<ExerciseGroup[]>(`${this.resourceUrl}/${courseId}/exams/${examId}/exerciseGroupsOrder`, exerciseGroups, { observe: 'response' });
    }

    private static convertDateFromClient(exam: Exam): Exam {
        return Object.assign({}, exam, {
            startDate: exam.startDate && moment(exam.startDate).isValid() ? exam.startDate.toJSON() : undefined,
            endDate: exam.endDate && moment(exam.endDate).isValid() ? exam.endDate.toJSON() : undefined,
            visibleDate: exam.visibleDate && moment(exam.visibleDate).isValid() ? exam.visibleDate.toJSON() : undefined,
            publishResultsDate: exam.publishResultsDate && moment(exam.publishResultsDate).isValid() ? exam.publishResultsDate.toJSON() : undefined,
            examStudentReviewStart: exam.examStudentReviewStart && moment(exam.examStudentReviewStart).isValid() ? exam.examStudentReviewStart.toJSON() : undefined,
            examStudentReviewEnd: exam.examStudentReviewEnd && moment(exam.examStudentReviewEnd).isValid() ? exam.examStudentReviewEnd.toJSON() : undefined,
        });
    }

    private static convertDateFromServer(res: EntityResponseType): EntityResponseType {
        if (res.body) {
            res.body.startDate = res.body.startDate ? moment(res.body.startDate) : undefined;
            res.body.endDate = res.body.endDate ? moment(res.body.endDate) : undefined;
            res.body.visibleDate = res.body.visibleDate ? moment(res.body.visibleDate) : undefined;
            res.body.publishResultsDate = res.body.publishResultsDate ? moment(res.body.publishResultsDate) : undefined;
            res.body.examStudentReviewStart = res.body.examStudentReviewStart ? moment(res.body.examStudentReviewStart) : undefined;
            res.body.examStudentReviewEnd = res.body.examStudentReviewEnd ? moment(res.body.examStudentReviewEnd) : undefined;
        }
        return res;
    }

    private static convertDateArrayFromServer(res: EntityArrayResponseType): EntityArrayResponseType {
        if (res.body) {
            res.body.forEach((exam: Exam) => {
                exam.startDate = exam.startDate ? moment(exam.startDate) : undefined;
                exam.endDate = exam.endDate ? moment(exam.endDate) : undefined;
                exam.visibleDate = exam.visibleDate ? moment(exam.visibleDate) : undefined;
                exam.publishResultsDate = exam.publishResultsDate ? moment(exam.publishResultsDate) : undefined;
                exam.examStudentReviewStart = exam.examStudentReviewStart ? moment(exam.examStudentReviewStart) : undefined;
                exam.examStudentReviewEnd = exam.examStudentReviewEnd ? moment(exam.examStudentReviewEnd) : undefined;
            });
        }
        return res;
    }
}
