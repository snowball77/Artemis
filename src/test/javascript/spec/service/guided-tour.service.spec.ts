import * as chai from 'chai';
import * as sinonChai from 'sinon-chai';
import { ComponentFixture, fakeAsync, inject, TestBed, tick } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { Observable, of } from 'rxjs';
import { CookieService } from 'ngx-cookie-service';
import { LocalStorageService, SessionStorageService } from 'ngx-webstorage';
import { TranslateService } from '@ngx-translate/core';
import { ArtemisSharedModule } from 'app/shared/shared.module';
import { ArtemisTestModule } from '../test.module';
import { SERVER_API_URL } from 'app/app.constants';
import { GuidedTour } from 'app/guided-tour/guided-tour.model';
import { GuidedTourService } from 'app/guided-tour/guided-tour.service';
import { GuidedTourState, Orientation, ResetParticipation, UserInteractionEvent } from 'app/guided-tour/guided-tour.constants';
import { GuidedTourComponent } from 'app/guided-tour/guided-tour.component';
import { GuidedTourMapping, GuidedTourSetting } from 'app/guided-tour/guided-tour-setting.model';
import { ModelingTaskTourStep, TextTourStep, UserInterActionTourStep } from 'app/guided-tour/guided-tour-step.model';
import { MockAccountService } from '../helpers/mocks/service/mock-account.service';
import { AccountService } from 'app/core/auth/account.service';
import { DeviceDetectorService } from 'ngx-device-detector';
import { Course } from 'app/entities/course.model';
import { MockTranslateService } from '../helpers/mocks/service/mock-translate.service';
import { GuidedTourModelingTask, personUML } from 'app/guided-tour/guided-tour-task.model';
import { completedTour } from 'app/guided-tour/tours/general-tour';
import { SinonStub, stub } from 'sinon';
import { HttpResponse } from '@angular/common/http';
import { ParticipationService } from 'app/exercises/shared/participation/participation.service';
import { Exercise, ExerciseType } from 'app/entities/exercise.model';
import { InitializationState } from 'app/entities/participation/participation.model';
import { NavbarComponent } from 'app/shared/layouts/navbar/navbar.component';
import { StudentParticipation } from 'app/entities/participation/student-participation.model';
import { MockSyncStorage } from '../helpers/mocks/service/mock-sync-storage.service';
import { MockCookieService } from '../helpers/mocks/service/mock-cookie.service';
import { CourseManagementService } from 'app/course/manage/course-management.service';

chai.use(sinonChai);
const expect = chai.expect;

describe('GuidedTourService', () => {
    const tour: GuidedTour = {
        settingsKey: 'tour',
        resetParticipation: ResetParticipation.EXERCISE_PARTICIPATION,
        steps: [
            new TextTourStep({ highlightSelector: '.random-selector', headlineTranslateKey: '', contentTranslateKey: '' }),
            new TextTourStep({ headlineTranslateKey: '', contentTranslateKey: '', orientation: Orientation.TOPLEFT }),
        ],
    };

    const tourWithUserInteraction: GuidedTour = {
        settingsKey: 'tour_user_interaction',
        resetParticipation: ResetParticipation.EXERCISE_PARTICIPATION,
        steps: [
            new UserInterActionTourStep({
                highlightSelector: '.random-selector',
                headlineTranslateKey: '',
                contentTranslateKey: '',
                userInteractionEvent: UserInteractionEvent.CLICK,
            }),
            new TextTourStep({ headlineTranslateKey: '', contentTranslateKey: '', orientation: Orientation.TOPLEFT }),
        ],
    };

    const tourWithCourseAndExercise: GuidedTour = {
        settingsKey: 'tour_with_course_and_exercise',
        resetParticipation: ResetParticipation.EXERCISE_PARTICIPATION,
        steps: [
            new TextTourStep({ headlineTranslateKey: '', contentTranslateKey: '' }),
            new TextTourStep({ headlineTranslateKey: '', contentTranslateKey: '', orientation: Orientation.TOPLEFT }),
        ],
    };

    const tourWithModelingTask: GuidedTour = {
        settingsKey: 'tour_modeling_task',
        resetParticipation: ResetParticipation.EXERCISE_PARTICIPATION,
        steps: [
            new ModelingTaskTourStep({
                headlineTranslateKey: '',
                contentTranslateKey: '',
                modelingTask: new GuidedTourModelingTask(personUML.name, ''),
                userInteractionEvent: UserInteractionEvent.MODELING,
            }),
        ],
    };

    describe('Service method', () => {
        let service: GuidedTourService;
        let httpMock: HttpTestingController;
        const expected = new GuidedTourSetting('guided_tour_key', 1, GuidedTourState.STARTED);

        beforeEach(() => {
            TestBed.configureTestingModule({
                imports: [ArtemisTestModule, ArtemisSharedModule, HttpClientTestingModule],
                providers: [
                    { provide: DeviceDetectorService },
                    { provide: LocalStorageService, useClass: MockSyncStorage },
                    { provide: SessionStorageService, useClass: MockSyncStorage },
                ],
            })
                .overrideModule(ArtemisTestModule, { set: { declarations: [], exports: [] } })
                .compileComponents();

            service = TestBed.inject(GuidedTourService);
            httpMock = TestBed.inject(HttpTestingController);
        });

        afterEach(() => {
            httpMock.verify();
        });

        it('should call the correct update URL and return the right JSON object', () => {
            service.guidedTourSettings = [];
            service['updateGuidedTourSettings']('guided_tour_key', 1, GuidedTourState.STARTED).subscribe();
            const req = httpMock.expectOne({ method: 'PUT' });
            const resourceUrl = SERVER_API_URL + 'api/guided-tour-settings';
            expect(req.request.url).equal(`${resourceUrl}`);
            expect(service.guidedTourSettings).to.eql([expected]);
        });

        it('should call the correct delete URL', () => {
            service.guidedTourSettings = [new GuidedTourSetting('guided_tour_key', 1, GuidedTourState.STARTED)];
            service['deleteGuidedTourSetting']('guided_tour_key').subscribe();
            const req = httpMock.expectOne({ method: 'DELETE' });
            const resourceUrl = SERVER_API_URL + 'api/guided-tour-settings';
            expect(req.request.url).equal(`${resourceUrl}/guided_tour_key`);
            expect(service.guidedTourSettings).to.eql([]);
        });
    });

    describe('Guided tour methods', () => {
        let guidedTourComponent: GuidedTourComponent;
        let guidedTourComponentFixture: ComponentFixture<GuidedTourComponent>;
        let router: Router;
        let guidedTourService: GuidedTourService;
        let participationService: ParticipationService;
        let courseService: CourseManagementService;

        let findParticipationStub: SinonStub;
        let deleteParticipationStub: SinonStub;
        let deleteGuidedTourSettingStub: SinonStub;
        let navigationStub: SinonStub;

        beforeEach(() => {
            TestBed.configureTestingModule({
                imports: [
                    ArtemisTestModule,
                    ArtemisSharedModule,
                    RouterTestingModule.withRoutes([
                        {
                            path: 'courses',
                            component: NavbarComponent,
                        },
                    ]),
                ],
                declarations: [NavbarComponent, GuidedTourComponent],
                providers: [
                    { provide: LocalStorageService, useClass: MockSyncStorage },
                    { provide: SessionStorageService, useClass: MockSyncStorage },
                    { provide: CookieService, useClass: MockCookieService },
                    { provide: AccountService, useClass: MockAccountService },
                    { provide: DeviceDetectorService },
                    { provide: TranslateService, useClass: MockTranslateService },
                ],
            })
                .overrideModule(ArtemisTestModule, { set: { declarations: [], exports: [] } })
                .overrideTemplate(NavbarComponent, '<div class="random-selector"></div>')
                .compileComponents()
                .then(() => {
                    guidedTourComponentFixture = TestBed.createComponent(GuidedTourComponent);
                    guidedTourComponent = guidedTourComponentFixture.componentInstance;

                    const navBarComponentFixture = TestBed.createComponent(NavbarComponent);
                    // eslint-disable-next-line @typescript-eslint/no-unused-vars
                    const navBarComponent = navBarComponentFixture.componentInstance;

                    router = TestBed.inject(Router);
                    guidedTourService = TestBed.inject(GuidedTourService);
                    participationService = TestBed.inject(ParticipationService);
                    courseService = TestBed.inject(CourseManagementService);

                    findParticipationStub = stub(participationService, 'findParticipation');
                    deleteParticipationStub = stub(participationService, 'deleteForGuidedTour');
                    // @ts-ignore
                    deleteGuidedTourSettingStub = stub(guidedTourService, 'deleteGuidedTourSetting');
                    navigationStub = stub(router, 'navigateByUrl');
                });
        });

        function prepareGuidedTour(guidedTour: GuidedTour) {
            // Prepare GuidedTourService and GuidedTourComponent
            spyOn(guidedTourService, 'init').and.returnValue(of());
            spyOn(guidedTourService, 'getLastSeenTourStepIndex').and.returnValue(0);
            spyOn<any>(guidedTourService, 'checkSelectorValidity').and.returnValue(true);
            spyOn<any>(guidedTourService, 'checkTourState').and.returnValue(true);
            spyOn<any>(guidedTourService, 'updateGuidedTourSettings').and.returnValue(of());
            spyOn<any>(guidedTourService, 'enableTour').and.callFake(() => {
                guidedTourService['availableTourForComponent'] = guidedTour;
                guidedTourService.currentTour = guidedTour;
            });
            spyOn<any>(guidedTourComponent, 'subscribeToDotChanges').and.callFake(() => {});
        }

        async function startCourseOverviewTour(guidedTour: GuidedTour) {
            guidedTourComponent.ngAfterViewInit();

            await guidedTourComponentFixture.ngZone!.run(() => {
                router.navigateByUrl('/courses');
            });

            // Start course overview tour
            expect(guidedTourComponentFixture.debugElement.query(By.css('.tour-step'))).to.not.exist;
            guidedTourService['enableTour'](guidedTour, true);
            guidedTourService['startTour']();
            guidedTourComponentFixture.detectChanges();
            expect(guidedTourComponentFixture.debugElement.query(By.css('.tour-step'))).to.exist;
            expect(guidedTourService.isOnFirstStep).to.be.true;
            expect(guidedTourService.currentTourStepDisplay).to.equal(1);
            expect(guidedTourService.currentTourStepCount).to.equal(2);
        }

        describe('Tours without user interaction', () => {
            beforeEach(async () => {
                prepareGuidedTour(tour);
                await startCourseOverviewTour(tour);
            });

            it('should start and finish the course overview guided tour', async () => {
                // Navigate to next step
                const nextButton = guidedTourComponentFixture.debugElement.query(By.css('.next-button'));
                expect(nextButton).to.exist;
                nextButton.nativeElement.click();
                expect(guidedTourService.isOnLastStep).to.be.true;

                // Finish guided tour
                nextButton.nativeElement.click();
                guidedTourComponentFixture.detectChanges();
                expect(guidedTourComponentFixture.debugElement.query(By.css('.tour-step'))).to.not.exist;
            });

            it('should start and skip the tour', () => {
                const skipButton = guidedTourComponentFixture.debugElement.query(By.css('.close'));
                expect(skipButton).to.exist;
                skipButton.nativeElement.click();
                guidedTourComponentFixture.detectChanges();
                expect(guidedTourComponentFixture.debugElement.query(By.css('.tour-step'))).to.not.exist;
            });

            it('should prevent backdrop from advancing', () => {
                const backdrop = guidedTourComponentFixture.debugElement.queryAll(By.css('.guided-tour-overlay'));
                expect(backdrop).to.exist;
                expect(backdrop.length).to.equal(4);
                backdrop.forEach((overlay) => {
                    overlay.nativeElement.click();
                });
                guidedTourComponentFixture.detectChanges();
                expect(guidedTourService.isOnFirstStep).to.be.true;
            });
        });

        describe('Tours with user interaction', () => {
            beforeEach(async () => {
                prepareGuidedTour(tourWithUserInteraction);
                await startCourseOverviewTour(tourWithUserInteraction);
            });

            it('should disable the next button', () => {
                guidedTourComponentFixture.detectChanges();
                const nextButton = guidedTourComponentFixture.debugElement.nativeElement.querySelector('.next-button').disabled;
                expect(nextButton).to.exist;
            });
        });

        describe('Tour for a certain course and exercise', () => {
            const guidedTourMapping = { courseShortName: 'tutorial', tours: { tour_with_course_and_exercise: 'git' } } as GuidedTourMapping;
            const exercise1 = { id: 1, shortName: 'git', type: ExerciseType.PROGRAMMING } as Exercise;
            const exercise2 = { id: 2, shortName: 'test', type: ExerciseType.PROGRAMMING } as Exercise;
            const exercise3 = { id: 3, shortName: 'git', type: ExerciseType.MODELING } as Exercise;
            const course1 = { id: 1, shortName: 'tutorial', exercises: [exercise2, exercise1] } as Course;
            const course2 = { id: 2, shortName: 'test' } as Course;

            function resetCurrentTour(): void {
                guidedTourService['currentCourse'] = undefined;
                guidedTourService['currentExercise'] = undefined;
                guidedTourService.currentTour = completedTour;
                guidedTourService.resetTour();
            }

            function currentCourseAndExerciseNull(): void {
                expect(guidedTourService.currentTour).to.be.undefined;
                expect(guidedTourService['currentCourse']).to.be.undefined;
                expect(guidedTourService['currentExercise']).to.be.undefined;
            }

            beforeEach(async () => {
                guidedTourService.guidedTourMapping = guidedTourMapping;
                prepareGuidedTour(tourWithCourseAndExercise);
                resetCurrentTour();
            });

            it('should start the tour for the matching course title', () => {
                spyOn(courseService, 'findWithExercises').and.returnValue(of({ body: course1 } as HttpResponse<any>));
                const courses = [course1];

                // enable tour for matching course title
                guidedTourService.enableTourForCourseOverview(courses, tourWithCourseAndExercise, true);
                expect(guidedTourService.currentTour).to.equal(tourWithCourseAndExercise);
                expect(guidedTourService['currentCourse']).to.equal(course1);
                expect(guidedTourService['currentExercise']).to.equal(exercise1);
                resetCurrentTour();

                const tourWithoutExerciseMapping = { courseShortName: 'tutorial', tours: { tour_with_course_and_exercise: '' } } as GuidedTourMapping;
                guidedTourService.guidedTourMapping = tourWithoutExerciseMapping;

                // enable tour for matching course title
                guidedTourService.enableTourForCourseOverview(courses, tourWithCourseAndExercise, true);
                expect(guidedTourService.currentTour).to.equal(tourWithCourseAndExercise);
                expect(guidedTourService['currentCourse']).to.equal(course1);
                expect(guidedTourService['currentExercise']).to.be.undefined;
                resetCurrentTour();
            });

            it('should disable the tour for not matching course title', () => {
                const courses = [course2];
                // disable tour for not matching titles
                guidedTourService.enableTourForCourseOverview(courses, tourWithCourseAndExercise, true);
                currentCourseAndExerciseNull();
            });

            it('should start the tour for the matching exercise short name', () => {
                // disable tour for exercises without courses
                guidedTourService.currentTour = undefined;
                guidedTourService.enableTourForExercise(exercise1, tourWithCourseAndExercise, true);
                currentCourseAndExerciseNull();
                resetCurrentTour();

                // disable tour for not matching course and exercise identifiers
                exercise2.course = course2;
                guidedTourService.enableTourForExercise(exercise2, tourWithCourseAndExercise, true);
                currentCourseAndExerciseNull();
                resetCurrentTour();

                // disable tour for not matching course identifier
                exercise3.course = course2;
                guidedTourService.enableTourForExercise(exercise3, tourWithCourseAndExercise, true);
                currentCourseAndExerciseNull();
                resetCurrentTour();

                // enable tour for matching course and exercise identifiers
                exercise1.course = course1;
                guidedTourService.enableTourForExercise(exercise1, tourWithCourseAndExercise, true);
                expect(guidedTourService.currentTour).to.equal(tourWithCourseAndExercise);
                expect(guidedTourService['currentCourse']).to.equal(course1);
                expect(guidedTourService['currentExercise']).to.equal(exercise1);
            });

            it('should start the tour for the matching course / exercise short name', () => {
                guidedTourService.currentTour = undefined;

                // enable tour for matching course / exercise short name
                guidedTourService.enableTourForCourseExerciseComponent(course1, tourWithCourseAndExercise, true);
                expect(guidedTourService.currentTour).to.equal(tourWithCourseAndExercise);

                course1.exercises!.forEach((exercise) => {
                    exercise.course = course1;
                    if (exercise === exercise1) {
                        expect(guidedTourService['isGuidedTourAvailableForExercise'](exercise)).to.be.true;
                    } else {
                        expect(guidedTourService['isGuidedTourAvailableForExercise'](exercise)).to.be.false;
                    }
                });

                // disable tour for not matching course without exercise
                guidedTourService.currentTour = undefined;
                guidedTourService.enableTourForCourseExerciseComponent(course2, tourWithCourseAndExercise, true);
                expect(guidedTourService.currentTour).to.be.undefined;

                // disable tour for not matching course but matching exercise identifier
                guidedTourService.currentTour = undefined;
                course2.exercises = [exercise3];
                guidedTourService.enableTourForCourseExerciseComponent(course2, tourWithCourseAndExercise, true);
                expect(guidedTourService.currentTour).to.be.undefined;
            });

            describe('Tour with student participation', () => {
                const studentParticipation1 = { id: 1, student: { id: 1 }, exercise: exercise1, initializationState: InitializationState.INITIALIZED } as StudentParticipation;
                const studentParticipation2 = { id: 2, student: { id: 1 }, exercise: exercise3, initializationState: InitializationState.INITIALIZED } as StudentParticipation;
                const httpResponse1 = { body: studentParticipation1 } as HttpResponse<StudentParticipation>;
                const httpResponse2 = { body: studentParticipation2 } as HttpResponse<StudentParticipation>;
                const exercise4 = { id: 4, title: 'git', type: ExerciseType.MODELING } as Exercise;

                function prepareParticipation(exercise: Exercise, studentParticipation: StudentParticipation, httpResponse: HttpResponse<StudentParticipation>) {
                    exercise.course = course1;
                    exercise.studentParticipations = [studentParticipation];
                    findParticipationStub.reset();
                    deleteParticipationStub.reset();
                    deleteGuidedTourSettingStub.reset();
                    navigationStub.reset();
                    findParticipationStub.returns(Observable.of(httpResponse));
                    deleteParticipationStub.returns(Observable.of(undefined));
                    deleteGuidedTourSettingStub.returns(Observable.of(undefined));
                }

                it('should find and delete the student participation for exercise', () => {
                    course1.exercises!.push(exercise4);

                    prepareParticipation(exercise1, studentParticipation1, httpResponse1);
                    guidedTourService.enableTourForExercise(exercise1, tourWithCourseAndExercise, true);
                    guidedTourService.restartTour();
                    expect(findParticipationStub).to.have.been.calledOnceWithExactly(1);
                    expect(deleteParticipationStub).to.have.been.calledOnceWithExactly(1, { deleteBuildPlan: true, deleteRepository: true });
                    expect(deleteGuidedTourSettingStub).to.have.been.calledOnceWith('tour_with_course_and_exercise');
                    expect(navigationStub).to.have.been.calledOnceWith('/courses/1/exercises');

                    prepareParticipation(exercise4, studentParticipation2, httpResponse2);
                    guidedTourService.enableTourForExercise(exercise4, tourWithCourseAndExercise, true);
                    guidedTourService.restartTour();
                    expect(findParticipationStub).to.have.been.calledOnceWithExactly(4);
                    expect(deleteParticipationStub).to.have.been.calledOnceWithExactly(2, { deleteBuildPlan: false, deleteRepository: false });
                    expect(deleteGuidedTourSettingStub).to.have.been.calledOnceWith('tour_with_course_and_exercise');
                    expect(navigationStub).to.have.been.calledOnceWith('/courses/1/exercises');

                    const index = course1.exercises!.findIndex((exercise) => (exercise.id = exercise4.id));
                    course1.exercises!.splice(index, 1);
                });
            });
        });

        describe('Modeling check', () => {
            it('should enable the next step if the results are correct', inject(
                [],
                fakeAsync(() => {
                    const enableNextStep = spyOn<any>(guidedTourService, 'enableNextStepClick').and.returnValue(of());
                    guidedTourService.currentTour = tourWithModelingTask;
                    guidedTourService.updateModelingResult(personUML.name, true);
                    tick(0);
                    expect(enableNextStep.calls.count()).to.equal(1);
                }),
            ));
        });
    });
});
