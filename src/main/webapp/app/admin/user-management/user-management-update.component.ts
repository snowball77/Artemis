import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { User } from 'app/core/user/user.model';
import { UserService } from 'app/core/user/user.service';
import { JhiLanguageHelper } from 'app/core/language/language.helper';

@Component({
    selector: 'jhi-user-management-update',
    templateUrl: './user-management-update.component.html',
})
export class UserManagementUpdateComponent implements OnInit {
    user: User;
    languages: string[];
    authorities: any[];
    isSaving: boolean;

    constructor(private languageHelper: JhiLanguageHelper, private userService: UserService, private route: ActivatedRoute) {}

    /**
     * Enable subscriptions to retrieve the user based on the activated route, all authorities and all languages on init
     */
    ngOnInit() {
        this.isSaving = false;
        this.route.data.subscribe(({ user }) => {
            this.user = user.body ? user.body : user;
        });
        this.authorities = [];
        this.userService.authorities().subscribe((authorities) => {
            this.authorities = authorities;
        });
        this.languages = this.languageHelper.getAll();
        // Empty array for new user
        if (!this.user.id) {
            this.user.groups = [];
        }
        // Set password to undefined. ==> If it still is undefined on save, it won't be changed for existing users. It will be random for new users
        this.user.password = undefined;
    }

    /**
     * Navigate to the previous page when the user cancels the update process
     */
    previousState() {
        window.history.back();
    }

    /**
     * Update or create user in the user management component
     */
    save() {
        this.isSaving = true;
        if (this.user.id) {
            this.userService.update(this.user).subscribe(
                () => this.onSaveSuccess(),
                () => this.onSaveError(),
            );
        } else {
            this.userService.create(this.user).subscribe(
                () => this.onSaveSuccess(),
                () => this.onSaveError(),
            );
        }
    }

    /**
     * Set isSaving to false and navigate to previous page
     * @param result
     */
    private onSaveSuccess() {
        this.isSaving = false;
        this.previousState();
    }

    /**
     * Set isSaving to false
     */
    private onSaveError() {
        this.isSaving = false;
    }

    shouldRandomizePassword(useRandomPassword: any) {
        if (useRandomPassword) {
            this.user.password = undefined;
        } else {
            this.user.password = '';
        }
    }
}
