import { Component, Input, Output, EventEmitter } from '@angular/core';

@Component({
    selector: 'jhi-text-assessment-conflicts-header',
    templateUrl: './text-assessment-conflicts-header.component.html',
    styleUrls: ['./text-assessment-conflicts-header.component.scss'],
})
export class TextAssessmentConflictsHeaderComponent {
    @Input() numberOfConflicts: number;
    @Input() haveRights: boolean;
    @Input() overrideBusy: boolean;
    @Input() markBusy: boolean;
    @Input() isOverrideDisabled: boolean;
    @Input() isMarkingDisabled: boolean;
    @Output() didChangeConflictIndex = new EventEmitter<number>();
    @Output() overrideLeftSubmission = new EventEmitter<void>();
    @Output() markSelectedAsNoConflict = new EventEmitter<void>();

    currentConflictIndex = 1;

    onNextConflict() {
        if (this.currentConflictIndex < this.numberOfConflicts) {
            this.currentConflictIndex++;
            this.didChangeConflictIndex.emit(this.currentConflictIndex);
        }
    }

    onPrevConflict() {
        if (this.currentConflictIndex > 1) {
            this.currentConflictIndex--;
            this.didChangeConflictIndex.emit(this.currentConflictIndex);
        }
    }
}
