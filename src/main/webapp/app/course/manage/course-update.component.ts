import { ActivatedRoute } from '@angular/router';
import { Component, OnInit, ViewChild } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { AlertService } from 'app/core/alert/alert.service';
import { Observable } from 'rxjs';
import { ImageCroppedEvent } from 'ngx-image-cropper';
import { base64StringToBlob } from 'blob-util';
import { filter, tap } from 'rxjs/operators';
import { regexValidator } from 'app/shared/form/shortname-validator.directive';
import { Course } from 'app/entities/course.model';
import { CourseManagementService } from './course-management.service';
import { ColorSelectorComponent } from 'app/shared/color-selector/color-selector.component';
import { ARTEMIS_DEFAULT_COLOR } from 'app/app.constants';
import { FileUploaderService } from 'app/shared/http/file-uploader.service';
import { CachingStrategy } from 'app/shared/image/secured-image.component';
import { ProfileInfo } from 'app/shared/layouts/profiles/profile-info.model';
import { ProfileService } from 'app/shared/layouts/profiles/profile.service';

@Component({
    selector: 'jhi-course-update',
    templateUrl: './course-update.component.html',
    styleUrls: ['./course-update.component.scss'],
})
export class CourseUpdateComponent implements OnInit {
    CachingStrategy = CachingStrategy;

    @ViewChild(ColorSelectorComponent, { static: false }) colorSelector: ColorSelectorComponent;
    readonly ARTEMIS_DEFAULT_COLOR = ARTEMIS_DEFAULT_COLOR;
    courseForm: FormGroup;
    course: Course;
    isSaving: boolean;
    courseImageFile?: Blob | File;
    courseImageFileName: string;
    isUploadingCourseImage: boolean;
    imageChangedEvent: any = '';
    croppedImage: any = '';
    showCropper = false;
    presentationScoreEnabled = false;
    complaintsEnabled = true; // default value
    customizeGroupNames = false; // default value

    shortNamePattern = /^[a-zA-Z][a-zA-Z0-9]{2,}$/; // must start with a letter and cannot contain special characters, at least 3 characters
    presentationScorePattern = /^[0-9]{0,4}$/; // makes sure that the presentation score is a positive natural integer greater than 0 and not too large

    constructor(
        private courseService: CourseManagementService,
        private activatedRoute: ActivatedRoute,
        private fileUploaderService: FileUploaderService,
        private jhiAlertService: AlertService,
        private profileService: ProfileService,
    ) {}

    ngOnInit() {
        this.isSaving = false;
        this.activatedRoute.data.subscribe(({ course }) => {
            this.course = course;
            // complaints are only enabled when at least one complaint is allowed and the complaint duration is positive
            this.complaintsEnabled = (this.course.maxComplaints! > 0 || this.course.maxTeamComplaints! > 0) && this.course.maxComplaintTimeDays! > 0;
        });

        this.profileService
            .getProfileInfo()
            .pipe(
                filter(Boolean),
                tap((info: ProfileInfo) => {
                    if (info.inProduction) {
                        // in production mode, the groups should not be customized by default when creating a course
                        // when editing a course, only admins can customize groups automatically
                        this.customizeGroupNames = !!this.course.id;
                    } else {
                        // developers typically want to customize the groups, therefore this is prefilled
                        this.customizeGroupNames = true;
                        if (!this.course.studentGroupName) {
                            this.course.studentGroupName = 'artemis-dev';
                        }
                        if (!this.course.teachingAssistantGroupName) {
                            this.course.teachingAssistantGroupName = 'artemis-dev';
                        }
                        if (!this.course.instructorGroupName) {
                            this.course.instructorGroupName = 'artemis-dev';
                        }
                    }
                }),
            )
            .subscribe();

        this.courseForm = new FormGroup({
            id: new FormControl(this.course.id),
            title: new FormControl(this.course.title, [Validators.required]),
            shortName: new FormControl(this.course.shortName, {
                validators: [Validators.required, Validators.minLength(3), regexValidator(this.shortNamePattern)],
                updateOn: 'blur',
            }),
            // note: we still reference them here so that they are used in the update method when the course is retrieved from the course form
            customizeGroupNames: new FormControl(this.customizeGroupNames),
            studentGroupName: new FormControl(this.course.studentGroupName),
            teachingAssistantGroupName: new FormControl(this.course.teachingAssistantGroupName),
            instructorGroupName: new FormControl(this.course.instructorGroupName),
            description: new FormControl(this.course.description),
            startDate: new FormControl(this.course.startDate),
            endDate: new FormControl(this.course.endDate),
            onlineCourse: new FormControl(this.course.onlineCourse),
            complaintsEnabled: new FormControl(this.complaintsEnabled),
            maxComplaints: new FormControl(this.course.maxComplaints, {
                validators: [Validators.required, Validators.min(0)],
            }),
            maxTeamComplaints: new FormControl(this.course.maxTeamComplaints, {
                validators: [Validators.required, Validators.min(0)],
            }),
            maxComplaintTimeDays: new FormControl(this.course.maxComplaintTimeDays, {
                validators: [Validators.required, Validators.min(0)],
            }),
            studentQuestionsEnabled: new FormControl(this.course.studentQuestionsEnabled),
            registrationEnabled: new FormControl(this.course.registrationEnabled),
            presentationScore: new FormControl({ value: this.course.presentationScore, disabled: this.course.presentationScore === 0 }, [
                Validators.min(1),
                regexValidator(this.presentationScorePattern),
            ]),
            color: new FormControl(this.course.color),
            courseIcon: new FormControl(this.course.courseIcon),
        });
        this.courseImageFileName = this.course.courseIcon!;
        this.croppedImage = this.course.courseIcon ? this.course.courseIcon : '';
        this.presentationScoreEnabled = this.course.presentationScore !== 0;
    }

    previousState() {
        window.history.back();
    }

    /**
     * Save the changes on a course
     * This function is called by pressing save after creating or editing a course
     */
    save() {
        this.isSaving = true;
        if (this.course.id !== undefined) {
            this.subscribeToSaveResponse(this.courseService.update(this.courseForm.getRawValue()));
        } else {
            this.subscribeToSaveResponse(this.courseService.create(this.courseForm.getRawValue()));
        }
    }

    openColorSelector(event: MouseEvent) {
        this.colorSelector.openColorSelector(event);
    }

    onSelectedColor(selectedColor: string) {
        this.courseForm.patchValue({ color: selectedColor });
    }

    /**
     * Async response after saving a course, handles appropriate action in case of error
     * @param result The Http response from the server
     */
    private subscribeToSaveResponse(result: Observable<HttpResponse<Course>>) {
        result.subscribe(
            () => this.onSaveSuccess(),
            (res: HttpErrorResponse) => this.onSaveError(res),
        );
    }

    /**
     * Action on successful course creation or edit
     */
    private onSaveSuccess() {
        this.isSaving = false;
        this.previousState();
    }

    /**
     * @function set course icon
     * @param $event {object} Event object which contains the uploaded file
     */
    setCourseImage($event: any): void {
        this.imageChangedEvent = $event;
        if ($event.target.files.length) {
            const fileList: FileList = $event.target.files;
            this.courseImageFile = fileList[0];
            this.courseImageFileName = this.courseImageFile['name'];
        }
    }

    /**
     * @param $event
     */
    imageCropped($event: ImageCroppedEvent) {
        this.croppedImage = $event.base64;
    }

    imageLoaded() {
        this.showCropper = true;
    }

    /**
     * @function uploadBackground
     * @desc Upload the selected file (from "Upload Background") and use it for the question's backgroundFilePath
     */
    uploadCourseImage(): void {
        const contentType = 'image/*';
        const b64Data = this.croppedImage.replace('data:image/png;base64,', '');
        const file = base64StringToBlob(b64Data, contentType);
        file['name'] = this.courseImageFileName;

        this.isUploadingCourseImage = true;
        this.fileUploaderService.uploadFile(file, file['name']).then(
            (response) => {
                this.courseForm.patchValue({ courseIcon: response.path });
                this.isUploadingCourseImage = false;
                this.courseImageFile = undefined;
                this.courseImageFileName = response.path!;
            },
            () => {
                this.isUploadingCourseImage = false;
                this.courseImageFile = undefined;
                this.courseImageFileName = this.course.courseIcon!;
            },
        );
        this.showCropper = false;
    }

    /**
     * Action on unsuccessful course creation or edit
     * @param error The error for providing feedback
     */
    private onSaveError(error: HttpErrorResponse) {
        const errorMessage = error.error ? error.error.title : error.headers.get('x-artemisapp-alert');
        // TODO: this is a workaround to avoid translation not found issues. Provide proper translations
        if (errorMessage) {
            const jhiAlert = this.jhiAlertService.error(errorMessage);
            jhiAlert.msg = errorMessage;
        }

        this.isSaving = false;
        window.scrollTo(0, 0);
    }

    get shortName() {
        return this.courseForm.get('shortName')!;
    }

    /**
     * Enable or disable presentation score input field based on presentationScoreEnabled checkbox
     */
    changePresentationScoreInput() {
        const presentationScoreControl = this.courseForm.controls['presentationScore'];
        if (presentationScoreControl.disabled) {
            presentationScoreControl.enable();
            this.presentationScoreEnabled = true;
        } else {
            presentationScoreControl.reset({ value: 0, disabled: true });
            this.presentationScoreEnabled = false;
        }
    }

    /**
     * Enable or disable complaints
     */
    changeComplaintsEnabled() {
        if (!this.complaintsEnabled) {
            this.complaintsEnabled = true;
            this.courseForm.controls['maxComplaints'].setValue(3);
            this.courseForm.controls['maxTeamComplaints'].setValue(3);
            this.courseForm.controls['maxComplaintTimeDays'].setValue(7);
        } else {
            this.complaintsEnabled = false;
            this.courseForm.controls['maxComplaints'].setValue(0);
            this.courseForm.controls['maxTeamComplaints'].setValue(0);
            this.courseForm.controls['maxComplaintTimeDays'].setValue(0);
        }
    }

    /**
     * Enable or disable the customization of groups
     */
    changeCustomizeGroupNames() {
        if (!this.customizeGroupNames) {
            this.customizeGroupNames = true;
            this.courseForm.controls['studentGroupName'].setValue('artemis-dev');
            this.courseForm.controls['teachingAssistantGroupName'].setValue('artemis-dev');
            this.courseForm.controls['instructorGroupName'].setValue('artemis-dev');
        } else {
            this.customizeGroupNames = false;
            this.courseForm.controls['studentGroupName'].setValue(null);
            this.courseForm.controls['teachingAssistantGroupName'].setValue(null);
            this.courseForm.controls['instructorGroupName'].setValue(null);
        }
    }
}
