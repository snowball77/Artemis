import { Component, OnDestroy, OnInit } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';
import { JhiAlertService, JhiEventManager, JhiParseLinks } from 'ng-jhipster';
import { Subscription } from 'rxjs/Subscription';

import { PageableSearch, SearchResult } from 'app/shared';
import { User } from 'app/core/user/user.model';
import { UserService } from 'app/core/user/user.service';
import { AccountService } from 'app/core/auth/account.service';
import { Observable, Subject } from 'rxjs';
import { first, tap } from 'rxjs/operators';

@Component({
    selector: 'jhi-user-management',
    templateUrl: './user-management.component.html',
})
export class UserManagementComponent implements OnInit, OnDestroy {
    private currentSearchParams: PageableSearch;

    currentAccount: User;
    error: string | null;
    success: string | null;

    userSearchResult: SearchResult<User>;

    private dialogErrorSource = new Subject<string>();
    dialogError$ = this.dialogErrorSource.asObservable();

    constructor(
        private userService: UserService,
        private alertService: JhiAlertService,
        private accountService: AccountService,
        private parseLinks: JhiParseLinks,
        private activatedRoute: ActivatedRoute,
        private router: Router,
        private eventManager: JhiEventManager,
    ) {}

    /**
     * Retrieves the current user and calls the {@link loadAll} and {@link registerChangeInUsers} methods on init
     */
    ngOnInit() {
        this.accountService.identity().then(user => {
            this.currentAccount = user!;
        });
    }

    /**
     * Unsubscribe from routeData
     */
    ngOnDestroy() {
        this.dialogErrorSource.unsubscribe();
    }

    /**
     * Update the user's activation status
     * @param user whose activation status should be changed
     * @param isActivated true if user should be activated, otherwise false
     */
    setActive(user: User, isActivated: boolean) {
        user.activated = isActivated;

        this.userService.update(user).subscribe(response => {
            if (response.status === 200) {
                this.error = null;
                this.success = 'OK';
            } else {
                this.success = null;
                this.error = 'ERROR';
            }
        });
    }

    searchForUsers(search: PageableSearch): void {
        this.currentSearchParams = search;
        this.userService
            .paginatedSearch(search)
            .pipe(
                first(),
                tap((result: SearchResult<User>) => {
                    this.userSearchResult = result;
                }),
            )
            .subscribe();
    }

    set changedPage(page: number) {
        this.currentSearchParams.page = page;
        this.searchForUsers(this.currentSearchParams);
    }

    /**
     * Deletes a user
     * @param login of the user that should be deleted
     */
    deleteUser(login: string) {
        this.userService.delete(login).subscribe(
            () => {
                this.eventManager.broadcast({
                    name: 'userListModification',
                    content: 'Deleted a user',
                });
                this.dialogErrorSource.next('');
            },
            (error: HttpErrorResponse) => this.dialogErrorSource.next(error.message),
        );
    }

    /**
     * Returns the search string to be displayed on the search text box after selecting a user
     *
     * @param user The user to be parsed
     */
    searchTextFromUser(user: User): string {
        return user.login || '';
    }

    /**
     * Returns the formatted string to be displayed in the found users after searching for one.
     *
     * @param user
     */
    searchResultFormatter(user: User): string {
        return `${user.name} (${user.login})`;
    }
}
