import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TranslateModule } from '@ngx-translate/core';
import { RouterTestingModule } from '@angular/router/testing';
import { RatingListComponent } from 'app/exercises/shared/rating/rating-list/rating-list.component';
import { ArtemisSharedModule } from 'app/shared/shared.module';
import { ArtemisTestModule } from '../../test.module';
import { MockActivatedRoute } from '../../helpers/mocks/activated-route/mock-activated-route';
import { RatingService } from 'app/exercises/shared/rating/rating.service';
import { ActivatedRoute, Router } from '@angular/router';
import { DifferencePipe } from 'ngx-moment';
import { RatingModule as StarRatingComponent } from 'ng-starrating';

import { of } from 'rxjs';
import { Participation } from 'app/entities/participation/participation.model';
import { Result } from 'app/entities/result.model';
import { Rating } from 'app/entities/rating.model';
import { Exercise, ExerciseType } from 'app/entities/exercise.model';

describe('RatingListComponent', () => {
    let component: RatingListComponent;
    let fixture: ComponentFixture<RatingListComponent>;
    let router: Router;

    const ratings = [{ id: 1 }, { id: 2 }, { id: 3 }];

    beforeEach(async () => {
        return TestBed.configureTestingModule({
            imports: [ArtemisSharedModule, TranslateModule.forRoot(), ArtemisTestModule, RouterTestingModule.withRoutes([]), StarRatingComponent],
            declarations: [RatingListComponent],
            providers: [
                {
                    provide: ActivatedRoute,
                    useValue: new MockActivatedRoute({ courseId: 123 }),
                },
                DifferencePipe,
                {
                    provide: RatingService,
                    useValue: {
                        getRatingsForDashboard() {
                            return of(ratings);
                        },
                    },
                },
            ],
        })
            .overrideModule(ArtemisTestModule, { set: { declarations: [], exports: [] } })
            .compileComponents()
            .then(() => {
                fixture = TestBed.createComponent(RatingListComponent);
                component = fixture.componentInstance;
                router = TestBed.inject(Router);

                component.ngOnInit();
            });
    });

    it('should initialize ratings', () => {
        expect(component.ratings.length).toEqual(3);
    });

    it('should not open exercise du to missing participation', () => {
        const rating = { id: 1, result: { id: 1 } as Result } as Rating;
        const routerNavigateSpy = jest.spyOn(router, 'navigate').mockImplementation(() => of(true).toPromise());
        component.openResult(rating);
        expect(routerNavigateSpy).not.toHaveBeenCalled();
    });

    it('should not open exercise du to missing exercise', () => {
        const rating = { id: 1, result: { id: 1, participation: { id: 1 } as Participation } as Result } as Rating;
        const routerNavigateSpy = jest.spyOn(router, 'navigate').mockImplementation(() => of(true).toPromise());
        component.openResult(rating);
        expect(routerNavigateSpy).not.toHaveBeenCalled();
    });

    it('should open exercise', () => {
        const rating = { id: 1, result: { id: 1, participation: { id: 1, exercise: { id: 1, type: ExerciseType.TEXT } as Exercise } as Participation } as Result } as Rating;
        const routerNavigateSpy = jest.spyOn(router, 'navigate').mockImplementation(() => of(true).toPromise());
        component.openResult(rating);
        expect(routerNavigateSpy).toHaveBeenCalled();
    });
});
