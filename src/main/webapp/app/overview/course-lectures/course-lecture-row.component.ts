import { Component, HostBinding, Input, OnInit } from '@angular/core';
import * as moment from 'moment';
import { Moment } from 'moment';
import { Course } from 'app/entities/course.model';
import { ActivatedRoute, Router } from '@angular/router';
import { Lecture } from 'app/entities/lecture.model';

@Component({
    selector: 'jhi-course-lecture-row',
    templateUrl: './course-lecture-row.component.html',
    styleUrls: ['../course-exercises/course-exercise-row.scss'],
})
export class CourseLectureRowComponent implements OnInit {
    @HostBinding('class') classes = 'exercise-row';
    @Input() lecture: Lecture;
    @Input() course: Course;
    @Input() extendedLink = false;

    constructor(private router: Router, private route: ActivatedRoute) {}

    ngOnInit() {}

    getUrgentClass(date?: Moment) {
        if (!date) {
            return undefined;
        }
        const remainingDays = date.diff(moment(), 'days');
        if (0 <= remainingDays && remainingDays < 7) {
            return 'text-danger';
        }
    }

    showDetails() {
        const lectureToAttach = {
            ...this.lecture,
            startDate: this.lecture.startDate ? this.lecture.startDate.valueOf() : null,
            endDate: this.lecture.endDate ? this.lecture.endDate.valueOf() : null,
            course: {
                id: this.course.id,
            },
        };
        if (this.extendedLink) {
            this.router.navigate(['courses', this.course.id, 'lectures', this.lecture.id], {
                state: {
                    lecture: lectureToAttach,
                },
            });
        } else {
            this.router.navigate([this.lecture.id], {
                relativeTo: this.route,
                state: {
                    lecture: lectureToAttach,
                },
            });
        }
    }
}
