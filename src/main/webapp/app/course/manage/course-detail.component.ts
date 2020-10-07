import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { Subscription } from 'rxjs/Subscription';
import { JhiEventManager } from 'ng-jhipster';

import { Course } from 'app/entities/course.model';
import { CourseManagementService } from './course-management.service';
import { CachingStrategy } from 'app/shared/image/secured-image.component';
import { AlertService } from 'app/core/alert/alert.service';

@Component({
    selector: 'jhi-course-detail',
    templateUrl: './course-detail.component.html',
    styleUrls: ['./course-detail.component.scss'],
})
export class CourseDetailComponent implements OnInit, OnDestroy {
    CachingStrategy = CachingStrategy;
    course: Course;
    private subscription: Subscription;
    private eventSubscriber: Subscription;

    constructor(private eventManager: JhiEventManager, private courseService: CourseManagementService, private route: ActivatedRoute, private jhiAlertService: AlertService) {}

    /**
     * On init load the course information and subscribe to listen for changes in courses.
     */
    ngOnInit() {
        this.subscription = this.route.params.subscribe((params) => {
            this.load(params['courseId']);
        });
        this.registerChangeInCourses();
    }

    /**
     * Fetch a course from the server.
     * @param courseId ID of the course to fetch.
     */
    load(courseId: number) {
        this.courseService.find(courseId).subscribe((courseResponse: HttpResponse<Course>) => {
            this.course = courseResponse.body!;
        });
    }

    /**
     * Register for the currently loaded course.
     */
    registerForCourse() {
        this.courseService.registerForCourse(this.course.id!).subscribe(
            (userResponse) => {
                if (userResponse.body != null) {
                    const message = 'Registered user for course ' + this.course.title;
                    const jhiAlert = this.jhiAlertService.info(message);
                    jhiAlert.msg = message;
                }
            },
            (error: HttpErrorResponse) => {
                const errorMessage = error.headers.get('X-artemisApp-message')!;
                // TODO: this is a workaround to avoid translation not found issues. Provide proper translations
                const jhiAlert = this.jhiAlertService.error(errorMessage);
                jhiAlert.msg = errorMessage;
            },
        );
    }

    /**
     * Go back to the previous page.
     */
    previousState() {
        window.history.back();
    }

    /**
     * On destroy unsubscribe all subscriptions.
     */
    ngOnDestroy() {
        this.subscription.unsubscribe();
        this.eventManager.destroy(this.eventSubscriber);
    }

    /**
     * Subscribe to changes in courses and reload the course after a change.
     */
    registerChangeInCourses() {
        this.eventSubscriber = this.eventManager.subscribe('courseListModification', () => this.load(this.course.id!));
    }
}
