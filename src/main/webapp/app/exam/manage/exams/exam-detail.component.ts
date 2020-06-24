import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Exam } from 'app/entities/exam.model';
import { ExamManagementService } from 'app/exam/manage/exam-management.service';

import { CachingStrategy } from 'app/shared/image/secured-image.component';

@Component({
    selector: 'jhi-exam-detail',
    templateUrl: './exam-detail.component.html',
})
export class ExamDetailComponent implements OnInit {
    CachingStrategy = CachingStrategy;
    exam: Exam;

    constructor(private route: ActivatedRoute, private examManagementService: ExamManagementService) {}

    /**
     * Initialize the exam
     */
    ngOnInit(): void {
        this.route.data.subscribe(({ exam }) => (this.exam = exam));
    }

    /**
     * Go back to the previous page.
     */
    previousState() {
        window.history.back();
    }
}
