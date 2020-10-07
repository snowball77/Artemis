import { Component, EventEmitter, Input, Output, HostBinding } from '@angular/core';
import { Result } from 'app/entities/result.model';
import { ComplaintResponse } from 'app/entities/complaint-response.model';
import { Complaint, ComplaintType } from 'app/entities/complaint.model';

/**
 * The <jhi-assessment-layout> component provides the basic layout for an assessment page.
 * It shows the header, alerts for complaints on top and the complaint form at the bottom of the page.
 * The actual assessment needs to be inserted using content projection.
 * Components using this component need to provide Inputs and handle Outputs. This component does not perform assessment logic.
 */
@Component({
    selector: 'jhi-assessment-layout',
    templateUrl: './assessment-layout.component.html',
    styleUrls: ['./assessment-layout.component.scss'],
})
export class AssessmentLayoutComponent {
    @HostBinding('class.assessment-container') readonly assessmentContainerClass = true;

    @Input() hideBackButton = false;
    @Output() navigateBack = new EventEmitter<void>();

    @Input() isLoading: boolean;
    @Input() saveBusy: boolean;
    @Input() submitBusy: boolean;
    @Input() cancelBusy: boolean;
    @Input() nextSubmissionBusy: boolean;

    @Input() isTeamMode: boolean;
    @Input() isAssessor: boolean;
    @Input() isAtLeastInstructor: boolean;
    @Input() canOverride: boolean;
    @Input() isTestRun = false;

    @Input() result: Result | null;
    @Input() assessmentsAreValid: boolean;
    ComplaintType = ComplaintType;
    @Input() complaint: Complaint | null;
    @Input() hasAssessmentDueDatePassed: boolean;

    @Output() save = new EventEmitter<void>();
    @Output() submit = new EventEmitter<void>();
    @Output() cancel = new EventEmitter<void>();
    @Output() nextSubmission = new EventEmitter<void>();
    @Output() updateAssessmentAfterComplaint = new EventEmitter<ComplaintResponse>();

    /**
     * For team exercises, the team tutor is the assessor and handles both complaints and feedback requests himself
     * For individual exercises, complaints are handled by a secondary reviewer and feedback requests by the assessor himself
     * For exam test runs, the original assessor is allowed to respond to complaints.
     */
    get isAllowedToRespond(): boolean {
        if (this.complaint!.team) {
            return this.isAssessor;
        } else {
            if (this.isTestRun) {
                return this.isAssessor;
            }
            return this.complaint!.complaintType === ComplaintType.COMPLAINT ? !this.isAssessor : this.isAssessor;
        }
    }
}
