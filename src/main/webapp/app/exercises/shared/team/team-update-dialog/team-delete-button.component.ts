import { Component, EventEmitter, Input, Output, OnDestroy } from '@angular/core';
import { Team } from 'app/entities/team.model';
import { Subject } from 'rxjs';
import { ButtonSize, ButtonType } from 'app/shared/components/button.component';
import { TeamService } from 'app/exercises/shared/team/team.service';
import { Exercise } from 'app/entities/exercise.model';
import { JhiAlertService } from 'ng-jhipster';

@Component({
    selector: 'jhi-team-delete-button',
    template: `
        <button
            jhiDeleteButton
            *ngIf="exercise.isAtLeastInstructor"
            [buttonSize]="buttonSize"
            [entityTitle]="team.shortName"
            deleteQuestion="artemisApp.team.delete.question"
            deleteConfirmationText="artemisApp.team.delete.typeNameToConfirm"
            (delete)="removeTeam($event)"
            [dialogError]="dialogError$"
        >
            <fa-icon [icon]="'trash-alt'" class="mr-1"></fa-icon>
        </button>
    `,
})
export class TeamDeleteButtonComponent implements OnDestroy {
    ButtonType = ButtonType;
    ButtonSize = ButtonSize;

    @Input() team: Team;
    @Input() exercise: Exercise;
    @Input() buttonSize: ButtonSize = ButtonSize.SMALL;

    @Output() delete = new EventEmitter<Team>();

    private dialogErrorSource = new Subject<string>();
    dialogError$ = this.dialogErrorSource.asObservable();

    constructor(private jhiAlertService: JhiAlertService, private teamService: TeamService) {}

    /**
     * Life cycle hook to indicate component creation is done
     */
    ngOnDestroy(): void {
        this.dialogErrorSource.unsubscribe();
    }

    /**
     * Deletes the provided team on the server and emits the delete event
     *
     * @param additionalChecksValues Not used here
     */
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    removeTeam = (additionalChecksValues: { [key: string]: boolean }) => {
        this.teamService.delete(this.exercise, this.team.id!).subscribe(
            () => {
                this.delete.emit(this.team);
                this.dialogErrorSource.next('');
            },
            () => this.jhiAlertService.error('artemisApp.team.removeTeam.error'),
        );
    };
}
