import { Component, OnDestroy, OnInit } from '@angular/core';
import { HttpErrorResponse, HttpHeaders, HttpResponse } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';
import { JhiAlertService, JhiEventManager, JhiParseLinks } from 'ng-jhipster';
import { Subscription } from 'rxjs/Subscription';

import { ITEMS_PER_PAGE } from 'app/shared';
import { onError } from 'app/utils/global.utils';
import { User } from 'app/core/user/user.model';
import { UserService } from 'app/core/user/user.service';
import { AccountService } from 'app/core/auth/account.service';
import { Subject } from 'rxjs';

@Component({
    selector: 'jhi-user-management',
    templateUrl: './user-management.component.html',
})
export class UserManagementComponent implements OnInit, OnDestroy {
    currentAccount: User;
    users: User[];
    error: string | null;
    success: string | null;
    routeData: Subscription;
    links: any;
    totalItems: string;
    itemsPerPage: number;
    page: number;
    predicate: string;
    previousPage: number;
    reverse: boolean;

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
    ) {
        this.itemsPerPage = ITEMS_PER_PAGE;
        this.routeData = this.activatedRoute.data.subscribe(data => {
            this.page = data['pagingParams'].page;
            this.previousPage = data['pagingParams'].page;
            this.reverse = data['pagingParams'].ascending;
            this.predicate = data['pagingParams'].predicate;
        });
    }

    /**
     * Retrieves the current user and calls the {@link loadAll} and {@link registerChangeInUsers} methods on init
     */
    ngOnInit() {
        this.accountService.identity().then(user => {
            this.currentAccount = user!;
            this.loadAll();
            this.registerChangeInUsers();
        });
    }

    /**
     * Unsubscribe from routeData
     */
    ngOnDestroy() {
        this.routeData.unsubscribe();
        this.dialogErrorSource.unsubscribe();
    }

    /**
     * Subscribe to event 'userListModification' and call {@link loadAll} on event broadcast
     */
    registerChangeInUsers() {
        this.eventManager.subscribe('userListModification', () => this.loadAll());
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
                this.loadAll();
            } else {
                this.success = null;
                this.error = 'ERROR';
            }
        });
    }

    /**
     * Retrieve the list of users from the user service for a single page in the user management based on the page, size and sort configuration
     */
    loadAll() {
        this.userService
            .query({
                page: this.page - 1,
                size: this.itemsPerPage,
                sort: this.sort(),
            })
            .subscribe(
                (res: HttpResponse<User[]>) => this.onSuccess(res.body!, res.headers),
                (res: HttpErrorResponse) => onError(this.alertService, res),
            );
    }

    /**
     * Sorts parameters by specified order
     */
    sort() {
        const result = [this.predicate + ',' + (this.reverse ? 'asc' : 'desc')];
        if (this.predicate !== 'id') {
            result.push('id');
        }
        return result;
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

    private onSuccess(data: User[], headers: HttpHeaders) {
        this.links = this.parseLinks.parse(headers.get('link')!);
        this.totalItems = headers.get('X-Total-Count')!;
        this.users = data;
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
