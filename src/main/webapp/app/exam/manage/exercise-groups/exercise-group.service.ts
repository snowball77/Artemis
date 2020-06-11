import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SERVER_API_URL } from 'app/app.constants';
import { ExerciseGroup } from 'app/entities/exercise-group.model';

type EntityResponseType = HttpResponse<ExerciseGroup>;
type EntityArrayResponseType = HttpResponse<ExerciseGroup[]>;

@Injectable({ providedIn: 'root' })
export class ExerciseGroupService {
    public resourceUrl = SERVER_API_URL + 'api/courses';

    constructor(private router: Router, private http: HttpClient) {}

    /**
     * Create an exercise group on the server using a POST request.
     * @param courseId The course id.
     * @param examId The exam id.
     * @param exerciseGroup The exercise group to create.
     */
    create(courseId: number, examId: number, exerciseGroup: ExerciseGroup): Observable<EntityResponseType> {
        return this.http.post<ExerciseGroup>(`${this.resourceUrl}/${courseId}/exams/${examId}/exerciseGroups`, exerciseGroup, { observe: 'response' });
    }

    /**
     * Update an exercise group on the server using a PUT request.
     * @param courseId The course id.
     * @param examId The exam id.
     * @param exerciseGroup The exercise group to update.
     */
    update(courseId: number, examId: number, exerciseGroup: ExerciseGroup): Observable<EntityResponseType> {
        return this.http.put<ExerciseGroup>(`${this.resourceUrl}/${courseId}/exams/${examId}/exerciseGroups`, exerciseGroup, { observe: 'response' });
    }

    /**
     * Find an exercise group on the server using a GET request.
     * @param courseId The course id.
     * @param examId The exam id.
     * @param exerciseGroupId The id of the exercise group to get.
     */
    find(courseId: number, examId: number, exerciseGroupId: number): Observable<EntityResponseType> {
        return this.http.get<ExerciseGroup>(`${this.resourceUrl}/${courseId}/exams/${examId}/exerciseGroups/${exerciseGroupId}`, { observe: 'response' });
    }

    /**
     * Delete an exercise group on the server using a DELETE request.
     * @param courseId The course id.
     * @param examId The exam id.
     * @param exerciseGroupId The id of the exercise group to delete.
     */
    delete(courseId: number, examId: number, exerciseGroupId: number): Observable<HttpResponse<any>> {
        return this.http.delete<any>(`${this.resourceUrl}/${courseId}/exams/${examId}/exerciseGroups/${exerciseGroupId}`, { observe: 'response' });
    }

    /**
     * Find all exercise groups for the given exam.
     * @param courseId The course id.
     * @param examId The exam id.
     */
    findAllForExam(courseId: number, examId: number): Observable<EntityArrayResponseType> {
        return this.http.get<ExerciseGroup[]>(`${this.resourceUrl}/${courseId}/exams/${examId}/exerciseGroups`, { observe: 'response' });
    }
}
