import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnInit, ViewChild } from '@angular/core';
import { UMLModel } from '@ls1intum/apollon';
import * as moment from 'moment';
import { ModelingSubmission } from 'app/entities/modeling-submission.model';
import { ModelingExercise } from 'app/entities/modeling-exercise.model';
import { ModelingEditorComponent } from 'app/exercises/modeling/shared/modeling-editor.component';
import { ExamSubmissionComponent } from 'app/exam/participate/exercises/exam-submission.component';
import { Submission } from 'app/entities/submission.model';
import { Exercise } from 'app/entities/exercise.model';

@Component({
    selector: 'jhi-modeling-submission-exam',
    templateUrl: './modeling-exam-submission.component.html',
    providers: [{ provide: ExamSubmissionComponent, useExisting: ModelingExamSubmissionComponent }],
    styleUrls: ['./modeling-exam-submission.component.scss'],
    // change deactivation must be triggered manually
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ModelingExamSubmissionComponent extends ExamSubmissionComponent implements OnInit {
    @ViewChild(ModelingEditorComponent, { static: false })
    modelingEditor: ModelingEditorComponent;

    // IMPORTANT: this reference must be contained in this.studentParticipation.submissions[0] otherwise the parent component will not be able to react to changes
    @Input()
    studentSubmission: ModelingSubmission;

    @Input()
    exercise: ModelingExercise;
    umlModel: UMLModel; // input model for Apollon

    constructor(changeDetectorReference: ChangeDetectorRef) {
        super(changeDetectorReference);
    }

    ngOnInit(): void {
        // show submission answers in UI
        this.updateViewFromSubmission();
    }

    getSubmission(): Submission {
        return this.studentSubmission;
    }

    getExercise(): Exercise {
        return this.exercise;
    }

    updateViewFromSubmission(): void {
        if (this.studentSubmission.model) {
            // Updates the Apollon editor model state (view) with the latest modeling submission
            this.umlModel = JSON.parse(this.studentSubmission.model);
        }
    }

    /**
     * Updates the model of the submission with the current Apollon editor model state (view)
     */
    public updateSubmissionFromView(): void {
        if (!this.modelingEditor || !this.modelingEditor.getCurrentModel()) {
            return;
        }
        const currentApollonModel = this.modelingEditor.getCurrentModel();
        const diagramJson = JSON.stringify(currentApollonModel);
        if (this.studentSubmission && diagramJson) {
            this.studentSubmission.model = diagramJson;
        }
    }

    /**
     * Checks whether there are pending changes in the current model. Returns true if there are unsaved changes (i.e. the subission is NOT synced), false otherwise.
     */
    public hasUnsavedChanges(): boolean {
        return !this.studentSubmission.isSynced!;
    }

    /**
     * The exercise is still active if it's due date hasn't passed yet.
     */
    get isActive(): boolean {
        return this.exercise && (!this.exercise.dueDate || moment(this.exercise.dueDate).isSameOrAfter(moment()));
    }

    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    modelChanged(model: UMLModel) {
        this.studentSubmission.isSynced = false;
    }
}
