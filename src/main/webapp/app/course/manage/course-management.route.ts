import { Injectable } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRouteSnapshot, Resolve, Routes } from '@angular/router';
import { UserRouteAccessService } from 'app/core/auth/user-route-access-service';
import { Observable, of } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import { Course } from 'app/entities/course.model';
import { CourseManagementService } from './course-management.service';
import { CourseManagementComponent } from './course-management.component';
import { CourseDetailComponent } from './course-detail.component';
import { CourseUpdateComponent } from './course-update.component';
import { CourseManagementExercisesComponent } from './course-management-exercises.component';
import { CourseGroupComponent } from 'app/course/manage/course-group.component';
import { Authority } from 'app/shared/constants/authority.constants';
import { RatingListComponent } from 'app/exercises/shared/rating/rating-list/rating-list.component';

@Injectable({ providedIn: 'root' })
export class CourseResolve implements Resolve<Course> {
    constructor(private service: CourseManagementService) {}

    /**
     * Resolves the route by extracting the courseId and returns the course with that Id if it exists
     * and creates a new course otherwise
     * @param route - contains the information about the route to be resolved
     */
    resolve(route: ActivatedRouteSnapshot): Observable<Course> {
        if (route.params['courseId']) {
            return this.service.find(route.params['courseId']).pipe(
                filter((response: HttpResponse<Course>) => response.ok),
                map((course: HttpResponse<Course>) => course.body!),
            );
        }
        return of(new Course());
    }
}

export const courseManagementRoute: Routes = [
    {
        path: '',
        component: CourseManagementComponent,
        data: {
            authorities: [Authority.TA, Authority.INSTRUCTOR, Authority.ADMIN],
            pageTitle: 'artemisApp.course.home.title',
        },
        canActivate: [UserRouteAccessService],
    },
    {
        path: 'new',
        component: CourseUpdateComponent,
        resolve: {
            course: CourseResolve,
        },
        data: {
            authorities: [Authority.ADMIN],
            pageTitle: 'artemisApp.course.home.title',
        },
        canActivate: [UserRouteAccessService],
    },
    {
        path: ':courseId/view',
        component: CourseDetailComponent,
        resolve: {
            course: CourseResolve,
        },
        data: {
            authorities: [Authority.TA, Authority.INSTRUCTOR, Authority.ADMIN],
            pageTitle: 'artemisApp.course.home.title',
        },
        canActivate: [UserRouteAccessService],
    },
    {
        path: ':courseId/exercises',
        component: CourseManagementExercisesComponent,
        resolve: {
            course: CourseResolve,
        },
        data: {
            authorities: [Authority.INSTRUCTOR, Authority.TA, Authority.ADMIN],
            pageTitle: 'artemisApp.course.home.title',
        },
        canActivate: [UserRouteAccessService],
    },
    {
        path: ':courseId/edit',
        component: CourseUpdateComponent,
        resolve: {
            course: CourseResolve,
        },
        data: {
            authorities: [Authority.INSTRUCTOR, Authority.ADMIN],
            pageTitle: 'artemisApp.course.home.title',
        },
        canActivate: [UserRouteAccessService],
    },
    {
        path: ':courseId/groups/:courseGroup',
        component: CourseGroupComponent,
        resolve: {
            course: CourseResolve,
        },
        data: {
            authorities: [Authority.INSTRUCTOR, Authority.ADMIN],
            pageTitle: 'artemisApp.course.detail.title',
        },
        canActivate: [UserRouteAccessService],
    },
    {
        path: ':courseId/ratings',
        component: RatingListComponent,
        data: {
            authorities: [Authority.INSTRUCTOR, Authority.ADMIN],
            pageTitle: 'artemisApp.ratingList.pageTitle',
        },
        canActivate: [UserRouteAccessService],
    },
];

const COURSE_MANAGEMENT_ROUTES = [...courseManagementRoute];

export const courseManagementState: Routes = [
    {
        path: '',
        children: COURSE_MANAGEMENT_ROUTES,
    },
];
