import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';

import { ArtemisSharedModule } from 'app/shared';
import {
    LectureAttachmentsComponent,
    LectureComponent,
    LectureDetailComponent,
    lectureRoute,
    LectureService,
    LectureUpdateComponent
} from './';
import { FormDateTimePickerModule } from 'app/shared/date-time-picker/date-time-picker.module';
import { ArtemisConfirmButtonModule } from 'app/components/confirm-button/confirm-button.module';
import { ArtemisMarkdownEditorModule } from 'app/markdown-editor';

const ENTITY_STATES = [...lectureRoute];

@NgModule({
    imports: [
        ArtemisSharedModule,
        RouterModule.forChild(ENTITY_STATES),
        FormDateTimePickerModule,
        ArtemisConfirmButtonModule,
        ArtemisMarkdownEditorModule,
    ],
    declarations: [LectureComponent, LectureDetailComponent, LectureUpdateComponent, LectureAttachmentsComponent],
    entryComponents: [LectureComponent, LectureUpdateComponent, LectureAttachmentsComponent],
    providers: [LectureService],
})
export class ArtemisLectureModule {}
