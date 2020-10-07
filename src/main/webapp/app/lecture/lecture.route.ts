import { Injectable } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRouteSnapshot, Resolve, Routes } from '@angular/router';
import { UserRouteAccessService } from 'app/core/auth/user-route-access-service';
import { Observable, of } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import { LectureService } from './lecture.service';
import { LectureComponent } from './lecture.component';
import { LectureDetailComponent } from './lecture-detail.component';
import { LectureUpdateComponent } from './lecture-update.component';
import { Lecture } from 'app/entities/lecture.model';
import { LectureAttachmentsComponent } from 'app/lecture/lecture-attachments.component';
import { Authority } from 'app/shared/constants/authority.constants';

@Injectable({ providedIn: 'root' })
export class LectureResolve implements Resolve<Lecture> {
    constructor(private service: LectureService) {}

    resolve(route: ActivatedRouteSnapshot): Observable<Lecture> {
        const id = route.params['id'] ? route.params['id'] : undefined;
        if (id) {
            return this.service.find(id).pipe(
                filter((response: HttpResponse<Lecture>) => response.ok),
                map((lecture: HttpResponse<Lecture>) => lecture.body!),
            );
        }
        return of(new Lecture());
    }
}

export const lectureRoute: Routes = [
    {
        path: ':courseId/lectures',
        component: LectureComponent,
        data: {
            authorities: [Authority.INSTRUCTOR, Authority.ADMIN],
            pageTitle: 'artemisApp.lecture.home.title',
        },
        canActivate: [UserRouteAccessService],
    },
    {
        path: ':courseId/lectures/:id/view',
        component: LectureDetailComponent,
        resolve: {
            lecture: LectureResolve,
        },
        data: {
            authorities: [Authority.INSTRUCTOR, Authority.ADMIN],
            pageTitle: 'artemisApp.lecture.home.title',
        },
        canActivate: [UserRouteAccessService],
    },
    {
        path: ':courseId/lectures/:id/attachments',
        component: LectureAttachmentsComponent,
        resolve: {
            lecture: LectureResolve,
        },
        data: {
            authorities: [Authority.INSTRUCTOR, Authority.ADMIN],
            pageTitle: 'artemisApp.lecture.attachments.title',
        },
        canActivate: [UserRouteAccessService],
    },
    {
        path: ':courseId/lectures/new',
        component: LectureUpdateComponent,
        resolve: {
            lecture: LectureResolve,
        },
        data: {
            authorities: [Authority.INSTRUCTOR, Authority.ADMIN],
            pageTitle: 'artemisApp.lecture.home.title',
        },
        canActivate: [UserRouteAccessService],
    },
    {
        path: ':courseId/lectures/:id/edit',
        component: LectureUpdateComponent,
        resolve: {
            lecture: LectureResolve,
        },
        data: {
            authorities: [Authority.INSTRUCTOR, Authority.ADMIN],
            pageTitle: 'artemisApp.lecture.home.title',
        },
        canActivate: [UserRouteAccessService],
    },
];
