import { Injectable } from '@angular/core';
import { Routes, Resolve, ActivatedRouteSnapshot } from '@angular/router';
import { Observable } from 'rxjs';

import { UserRouteAccessService } from 'app/core/auth/user-route-access-service';
import { TextSubmissionAssessmentComponent } from './text-submission-assessment.component';
import { StudentParticipation } from 'app/entities/participation/student-participation.model';
import { TextAssessmentsService } from 'app/exercises/text/assess/text-assessments.service';
import { TextSubmissionService } from 'app/exercises/text/participate/text-submission.service';
import { Authority } from 'app/shared/constants/authority.constants';

@Injectable({ providedIn: 'root' })
export class StudentParticipationResolver implements Resolve<StudentParticipation | undefined> {
    constructor(private textAssessmentsService: TextAssessmentsService) {}

    /**
     * Resolves the needed StudentParticipations for the TextSubmissionAssessmentComponent using the TextAssessmentsService.
     * @param route
     */
    resolve(route: ActivatedRouteSnapshot) {
        const submissionId = Number(route.paramMap.get('submissionId'));

        if (submissionId) {
            return this.textAssessmentsService.getFeedbackDataForExerciseSubmission(submissionId).catch(() => Observable.of(undefined));
        }
        return Observable.of(undefined);
    }
}

@Injectable({ providedIn: 'root' })
export class NewStudentParticipationResolver implements Resolve<StudentParticipation | undefined> {
    constructor(private textSubmissionService: TextSubmissionService) {}

    /**
     * Resolves the needed StudentParticipations for the TextSubmissionAssessmentComponent using the TextAssessmentsService.
     * @param route
     */
    resolve(route: ActivatedRouteSnapshot) {
        const exerciseId = Number(route.paramMap.get('exerciseId'));

        if (exerciseId) {
            return this.textSubmissionService
                .getTextSubmissionForExerciseWithoutAssessment(exerciseId, 'lock')
                .map((submission) => <StudentParticipation>submission.participation)
                .catch(() => Observable.of(undefined));
        }
        return Observable.of(undefined);
    }
}

export const NEW_ASSESSMENT_PATH = 'new/assessment';
export const textSubmissionAssessmentRoutes: Routes = [
    {
        path: NEW_ASSESSMENT_PATH,
        component: TextSubmissionAssessmentComponent,
        data: {
            authorities: [Authority.ADMIN, Authority.INSTRUCTOR, Authority.TA],
            pageTitle: 'artemisApp.textAssessment.title',
        },
        resolve: {
            studentParticipation: NewStudentParticipationResolver,
        },
        runGuardsAndResolvers: 'always',
        canActivate: [UserRouteAccessService],
    },
    {
        path: ':submissionId/assessment',
        component: TextSubmissionAssessmentComponent,
        data: {
            authorities: [Authority.ADMIN, Authority.INSTRUCTOR, Authority.TA],
            pageTitle: 'artemisApp.textAssessment.title',
        },
        resolve: {
            studentParticipation: StudentParticipationResolver,
        },
        runGuardsAndResolvers: 'paramsChange',
        canActivate: [UserRouteAccessService],
    },
];
