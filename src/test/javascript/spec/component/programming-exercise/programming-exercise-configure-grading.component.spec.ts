import { async, ComponentFixture, fakeAsync, flush, TestBed, tick } from '@angular/core/testing';
import * as moment from 'moment';
import { Moment } from 'moment';
import { DebugElement } from '@angular/core';
import { By } from '@angular/platform-browser';
import * as sinonChai from 'sinon-chai';
import { sortBy as _sortBy } from 'lodash';
import { LocalStorageService, SessionStorageService } from 'ngx-webstorage';
import { of, Subject } from 'rxjs';
import { ActivatedRoute, Params, Router } from '@angular/router';
import { SinonSpy, SinonStub, spy, stub } from 'sinon';
import { CookieService } from 'ngx-cookie-service';
import { AlertService } from 'app/core/alert/alert.service';
import * as chai from 'chai';
import { ArtemisTestModule } from '../../test.module';
import { TranslateModule } from '@ngx-translate/core';
import { MockSyncStorage } from '../../helpers/mocks/service/mock-sync-storage.service';
import { MockProgrammingExerciseGradingService } from '../../helpers/mocks/service/mock-programming-exercise-grading.service';
import { ProgrammingExerciseTestCase } from 'app/entities/programming-exercise-test-case.model';
import { ArtemisSharedModule } from 'app/shared/shared.module';
import { ArtemisProgrammingExerciseGradingModule } from 'app/exercises/programming/manage/grading/programming-exercise-grading.module';
import { expectElementToBeDisabled, expectElementToBeEnabled, getElement } from '../../helpers/utils/general.utils';
import { ProgrammingExerciseWebsocketService } from 'app/exercises/programming/manage/services/programming-exercise-websocket.service';
import { MockProgrammingExerciseWebsocketService } from '../../helpers/mocks/service/mock-programming-exercise-websocket.service';
import { ProgrammingBuildRunService } from 'app/exercises/programming/participate/programming-build-run.service';
import { MockProgrammingBuildRunService } from '../../helpers/mocks/service/mock-programming-build-run.service';
import { FeatureToggleService } from 'app/shared/feature-toggle/feature-toggle.service';
import { MockFeatureToggleService } from '../../helpers/mocks/service/mock-feature-toggle.service';
import { EditableField, ProgrammingExerciseConfigureGradingComponent } from 'app/exercises/programming/manage/grading/programming-exercise-configure-grading.component';
import { ProgrammingExerciseService, ProgrammingExerciseTestCaseStateDTO } from 'app/exercises/programming/manage/services/programming-exercise.service';
import { ProgrammingExercise } from 'app/entities/programming-exercise.model';
import {
    ProgrammingExerciseGradingService,
    ProgrammingExerciseTestCaseUpdate,
    StaticCodeAnalysisCategoryUpdate,
} from 'app/exercises/programming/manage/services/programming-exercise-grading.service';
import { MockActivatedRouteWithSubjects } from '../../helpers/mocks/activated-route/mock-activated-route-with-subjects';
import { MockCookieService } from '../../helpers/mocks/service/mock-cookie.service';
import { MockProgrammingExerciseService } from '../../helpers/mocks/service/mock-programming-exercise.service';
import { MockRouter } from '../../helpers/mocks/mock-router';
import { StaticCodeAnalysisCategory, StaticCodeAnalysisCategoryState } from 'app/entities/static-code-analysis-category.model';

chai.use(sinonChai);
const expect = chai.expect;

describe('ProgrammingExerciseConfigureGradingComponent', () => {
    let comp: ProgrammingExerciseConfigureGradingComponent;
    let fixture: ComponentFixture<ProgrammingExerciseConfigureGradingComponent>;
    let debugElement: DebugElement;

    let route: ActivatedRoute;
    let gradingService: ProgrammingExerciseGradingService;
    let programmingExerciseService: ProgrammingExerciseService;

    let updateTestCasesStub: SinonStub;
    let updateCategoriesStub: SinonStub;
    let resetTestCasesStub: SinonStub;
    let notifyTestCasesSpy: SinonSpy;
    let testCasesChangedStub: SinonStub;
    let getExerciseTestCaseStateStub: SinonStub;
    let getCodeAnalysisCategoriesStub: SinonStub;
    let loadExerciseStub: SinonStub;
    let programmingExerciseWebsocketService: ProgrammingExerciseWebsocketService;

    let routeSubject: Subject<Params>;
    let testCasesChangedSubject: Subject<boolean>;
    let getExerciseTestCaseStateSubject: Subject<{ body: ProgrammingExerciseTestCaseStateDTO }>;
    let getCodeAnalysisCategoriesSubject: Subject<StaticCodeAnalysisCategory[]>;

    const testCaseTableId = '#testCaseTable';
    const tableEditingInput = '.table-editable-field__input';
    const rowClass = 'datatable-body-row';
    const saveTableButton = '#save-table-button';
    const resetTableButton = '#reset-table-button';
    const triggerSubmissionRunButton = '#trigger-all-button > button';
    const testCasesNoUnsavedChanges = '#test-case-status-no-unsaved-changes';
    const testCasesUnsavedChanges = '#test-case-status-unsaved-changes';
    const testCasesUpdated = '#test-case-status-updated';
    const testCasesNoUpdated = '#test-case-status-no-updated';
    const codeAnalysisTableId = '#codeAnalysisTable';

    const exerciseId = 1;
    const exercise = {
        id: exerciseId,
        staticCodeAnalysisEnabled: true,
    } as ProgrammingExercise;
    const testCases1 = [
        { id: 1, testName: 'testBubbleSort', active: true, weight: 1, bonusMultiplier: 1, bonusPoints: 0, afterDueDate: false },
        { id: 2, testName: 'testMergeSort', active: true, weight: 1, bonusMultiplier: 1, bonusPoints: 0, afterDueDate: true },
        { id: 3, testName: 'otherTest', active: false, weight: 1, bonusMultiplier: 1, bonusPoints: 0, afterDueDate: false },
    ] as ProgrammingExerciseTestCase[];
    const codeAnalysisCategories1 = [
        {
            id: 1,
            name: 'Bad Practice',
            state: StaticCodeAnalysisCategoryState.Graded,
            penalty: 1,
            maxPenalty: 10,
        },
        {
            id: 2,
            name: 'Styling',
            state: StaticCodeAnalysisCategoryState.Feedback,
            penalty: 0,
            maxPenalty: 0,
        },
    ] as StaticCodeAnalysisCategory[];

    const getExerciseTestCasteStateDTO = (released: boolean, hasStudentResult: boolean, testCasesChanged: boolean, buildAndTestStudentSubmissionsAfterDueDate?: Moment) => ({
        body: {
            released,
            hasStudentResult,
            testCasesChanged,
            buildAndTestStudentSubmissionsAfterDueDate,
        },
    });

    const getSaveButton = () => {
        return getElement(debugElement, saveTableButton);
    };

    const getResetButton = () => {
        return getElement(debugElement, resetTableButton);
    };

    const getTriggerButton = () => {
        return getElement(debugElement, triggerSubmissionRunButton);
    };

    const getUnsavedChangesBadge = () => {
        return getElement(debugElement, testCasesUnsavedChanges);
    };

    const getNoUnsavedChangesBadge = () => {
        return getElement(debugElement, testCasesNoUnsavedChanges);
    };

    const getUpdatedTestCaseBadge = () => {
        return getElement(debugElement, testCasesUpdated);
    };

    const getNoUpdatedTestCaseBadge = () => {
        return getElement(debugElement, testCasesNoUpdated);
    };

    beforeEach(async(() => {
        TestBed.configureTestingModule({
            imports: [TranslateModule.forRoot(), ArtemisTestModule, ArtemisSharedModule, ArtemisProgrammingExerciseGradingModule],
            providers: [
                AlertService,
                { provide: ProgrammingExerciseService, useClass: MockProgrammingExerciseService },
                { provide: ProgrammingExerciseWebsocketService, useClass: MockProgrammingExerciseWebsocketService },
                { provide: ProgrammingExerciseGradingService, useClass: MockProgrammingExerciseGradingService },
                { provide: ProgrammingBuildRunService, useClass: MockProgrammingBuildRunService },
                { provide: LocalStorageService, useClass: MockSyncStorage },
                { provide: SessionStorageService, useClass: MockSyncStorage },
                { provide: CookieService, useClass: MockCookieService },
                { provide: ActivatedRoute, useClass: MockActivatedRouteWithSubjects },
                { provide: Router, useClass: MockRouter },
                { provide: FeatureToggleService, useClass: MockFeatureToggleService },
            ],
        })
            .compileComponents()
            .then(() => {
                fixture = TestBed.createComponent(ProgrammingExerciseConfigureGradingComponent);
                debugElement = fixture.debugElement;
                comp = fixture.componentInstance as ProgrammingExerciseConfigureGradingComponent;

                gradingService = debugElement.injector.get(ProgrammingExerciseGradingService);
                route = debugElement.injector.get(ActivatedRoute);
                const router = debugElement.injector.get(Router);
                programmingExerciseWebsocketService = debugElement.injector.get(ProgrammingExerciseWebsocketService);
                programmingExerciseService = debugElement.injector.get(ProgrammingExerciseService);

                updateTestCasesStub = stub(gradingService, 'updateTestCase');
                updateCategoriesStub = stub(gradingService, 'updateCodeAnalysisCategories');
                notifyTestCasesSpy = spy(gradingService, 'notifyTestCases');
                resetTestCasesStub = stub(gradingService, 'reset');

                // @ts-ignore
                (router as MockRouter).setUrl('/');
                routeSubject = new Subject();
                // @ts-ignore
                (route as MockActivatedRouteWithSubjects).setSubject(routeSubject);

                testCasesChangedStub = stub(programmingExerciseWebsocketService, 'getTestCaseState');
                getExerciseTestCaseStateStub = stub(programmingExerciseService, 'getProgrammingExerciseTestCaseState');
                getCodeAnalysisCategoriesStub = stub(gradingService, 'getCodeAnalysisCategories');
                loadExerciseStub = stub(programmingExerciseService, 'find');

                getExerciseTestCaseStateSubject = new Subject();
                getCodeAnalysisCategoriesSubject = new Subject();

                testCasesChangedSubject = new Subject<boolean>();
                testCasesChangedStub.returns(testCasesChangedSubject);
                getExerciseTestCaseStateStub.returns(getExerciseTestCaseStateSubject);
                getCodeAnalysisCategoriesStub.returns(getCodeAnalysisCategoriesSubject);
                loadExerciseStub.returns(of({ body: exercise }));
            });
    }));

    afterEach(() => {
        notifyTestCasesSpy.restore();
        testCasesChangedStub.restore();
        getExerciseTestCaseStateStub.restore();
    });

    it('should create a datatable with the correct amount of rows when test cases come in (hide inactive tests)', fakeAsync(() => {
        comp.ngOnInit();
        routeSubject.next({ exerciseId, tab: 'test-cases' });
        getExerciseTestCaseStateSubject.next(getExerciseTestCasteStateDTO(true, true, false, moment()));
        getCodeAnalysisCategoriesSubject.next(codeAnalysisCategories1);

        (gradingService as any).next(testCases1);

        fixture.detectChanges();

        const table = debugElement.query(By.css(testCaseTableId));
        const rows = table.queryAll(By.css(rowClass));

        expect(comp.testCases).to.deep.equal(testCases1);
        expect(rows).to.have.lengthOf(testCases1.filter(({ active }) => active).length);

        const saveButton = debugElement.query(By.css(saveTableButton));
        expect(saveButton).to.exist;
        expect(saveButton.nativeElement.disabled).to.be.true;

        tick();
        fixture.destroy();
    }));

    it('should create a datatable with the correct amount of rows when test cases come in (show inactive tests)', fakeAsync(() => {
        comp.ngOnInit();
        comp.showInactive = true;
        routeSubject.next({ exerciseId, tab: 'test-cases' });
        getExerciseTestCaseStateSubject.next(getExerciseTestCasteStateDTO(true, true, false, moment()));
        getCodeAnalysisCategoriesSubject.next(codeAnalysisCategories1);

        (gradingService as any).next(testCases1);

        fixture.detectChanges();

        const table = debugElement.query(By.css(testCaseTableId));
        const rows = table.queryAll(By.css(rowClass));

        expect(comp.testCases).to.deep.equal(testCases1);
        expect(rows).to.have.lengthOf(testCases1.length);

        const saveButton = debugElement.query(By.css(saveTableButton));
        expect(saveButton).to.exist;
        expect(saveButton.nativeElement.disabled).to.be.true;

        tick();
        fixture.destroy();
    }));

    it('should update test case when an input field is updated', fakeAsync(() => {
        comp.ngOnInit();
        comp.showInactive = true;
        routeSubject.next({ exerciseId, tab: 'test-cases' });
        getExerciseTestCaseStateSubject.next(getExerciseTestCasteStateDTO(true, true, false, moment()));
        getCodeAnalysisCategoriesSubject.next(codeAnalysisCategories1);

        const orderedTests = _sortBy(testCases1, 'testName');

        (gradingService as any).next(testCases1);

        fixture.detectChanges();

        const table = debugElement.query(By.css(testCaseTableId));

        // get first weight input
        const editingInputs = table.queryAll(By.css(tableEditingInput));
        expect(editingInputs).to.have.lengthOf(testCases1.length * 3);

        const weightInput = editingInputs[0].nativeElement;
        expect(weightInput).to.exist;
        weightInput.focus();

        // Set new weight.
        weightInput.value = '20';
        weightInput.dispatchEvent(new Event('blur'));

        const multiplierInput = editingInputs[1].nativeElement;
        expect(multiplierInput).to.exist;
        multiplierInput.focus();

        // Set new multiplier.
        multiplierInput.value = '2';
        multiplierInput.dispatchEvent(new Event('blur'));

        const bonusInput = editingInputs[2].nativeElement;
        expect(bonusInput).to.exist;
        bonusInput.focus();

        // Set new bonus.
        bonusInput.value = '1';
        bonusInput.dispatchEvent(new Event('blur'));

        fixture.detectChanges();

        expect(comp.changedTestCaseIds).to.deep.equal([orderedTests[0].id]);

        // Trigger button should be disabled.
        let triggerButton = getTriggerButton();
        expectElementToBeDisabled(triggerButton);

        // Save weight.
        updateTestCasesStub.returns(of([{ ...orderedTests[0], weight: 20, bonusMultiplier: 2, bonusPoints: 1 }]));
        const saveButton = getSaveButton();
        expectElementToBeEnabled(saveButton);
        saveButton.click();

        fixture.detectChanges();

        const testThatWasUpdated = _sortBy(comp.testCases, 'testName')[0];
        expect(updateTestCasesStub).to.have.been.calledOnceWithExactly(exerciseId, [
            new ProgrammingExerciseTestCaseUpdate(testThatWasUpdated.id, 20, 1, 2, testThatWasUpdated.afterDueDate),
        ]);
        expect(testThatWasUpdated.weight).to.equal(20);
        expect(comp.changedTestCaseIds).to.have.lengthOf(0);

        testCasesChangedSubject.next(true);
        // Trigger button is now enabled because the tests were saved.
        expect(comp.hasUpdatedGradingConfig).to.be.true;

        fixture.detectChanges();

        triggerButton = getTriggerButton();
        expectElementToBeEnabled(triggerButton);

        tick();
        fixture.destroy();
        flush();
    }));

    it('should be able to update the value of the afterDueDate boolean', async () => {
        comp.ngOnInit();
        comp.showInactive = true;
        routeSubject.next({ exerciseId, tab: 'test-cases' });
        getExerciseTestCaseStateSubject.next(getExerciseTestCasteStateDTO(true, true, false, moment()));
        getCodeAnalysisCategoriesSubject.next(codeAnalysisCategories1);

        const orderedTests = _sortBy(testCases1, 'testName');

        (gradingService as any).next(testCases1);

        fixture.detectChanges();
        await fixture.whenStable();

        const table = debugElement.query(By.css(testCaseTableId));
        const checkboxes = table.queryAll(By.css('.table-editable-field__checkbox'));
        expect(checkboxes).to.have.lengthOf(testCases1.length);
        checkboxes[0].nativeElement.click();

        await fixture.whenStable();
        fixture.detectChanges();

        expect(comp.changedTestCaseIds).to.deep.equal([orderedTests[0].id]);

        // The UI should now show that there are unsaved changes.
        expect(getUnsavedChangesBadge()).to.exist;
        expect(getNoUnsavedChangesBadge()).not.to.exist;

        // Save weight.
        updateTestCasesStub.returns(of({ ...orderedTests[0], afterDueDate: true }));
        const saveTestCases = debugElement.query(By.css(saveTableButton));
        expect(saveTestCases).to.exist;
        expect(saveTestCases.nativeElement.disabled).to.be.false;
        saveTestCases.nativeElement.click();

        fixture.detectChanges();

        const testThatWasUpdated = _sortBy(comp.testCases, 'testName')[0];
        expect(updateTestCasesStub).to.have.been.calledOnceWithExactly(exerciseId, [ProgrammingExerciseTestCaseUpdate.from(testThatWasUpdated)]);

        await new Promise((resolve) => setTimeout(resolve));
        fixture.destroy();
    });

    it('should not be able to update the value of the afterDueDate boolean if the programming exercise does not have a buildAndTestAfterDueDate', async () => {
        comp.ngOnInit();
        comp.showInactive = true;
        routeSubject.next({ exerciseId, tab: 'test-cases' });
        getExerciseTestCaseStateSubject.next(getExerciseTestCasteStateDTO(true, true, false, undefined));
        getCodeAnalysisCategoriesSubject.next(codeAnalysisCategories1);

        (gradingService as any).next(testCases1);

        fixture.detectChanges();
        await fixture.whenStable();

        const table = debugElement.query(By.css(testCaseTableId));
        const checkboxes = table.queryAll(By.css('.table-editable-field__checkbox'));
        expect(checkboxes).to.have.lengthOf(testCases1.length);
        expect(checkboxes.every(({ nativeElement: { disabled } }) => disabled)).to.be.true;

        fixture.destroy();
    });

    it('should show the updatedTests badge when the exercise is released and has student results', fakeAsync(() => {
        comp.ngOnInit();
        routeSubject.next({ exerciseId, tab: 'test-cases' });
        // @ts-ignore
        (gradingService as MockProgrammingExerciseGradingService).next(testCases1);
        getExerciseTestCaseStateSubject.next(getExerciseTestCasteStateDTO(true, true, false, moment()));
        getCodeAnalysisCategoriesSubject.next(codeAnalysisCategories1);

        fixture.detectChanges();

        expect(getNoUnsavedChangesBadge()).to.exist;
        expect(getNoUpdatedTestCaseBadge()).to.exist;

        tick();
        fixture.destroy();
        flush();
    }));

    it('should not show the updatedTests badge when the exercise is released and has no student results', fakeAsync(() => {
        comp.ngOnInit();
        routeSubject.next({ exerciseId, tab: 'test-cases' });
        // @ts-ignore
        (gradingService as MockProgrammingExerciseGradingService).next(testCases1);
        getExerciseTestCaseStateSubject.next(getExerciseTestCasteStateDTO(true, false, false, moment()));
        getCodeAnalysisCategoriesSubject.next(codeAnalysisCategories1);

        fixture.detectChanges();

        expect(getNoUnsavedChangesBadge()).to.exist;
        expect(getNoUpdatedTestCaseBadge()).not.to.exist;

        tick();
        fixture.destroy();
        flush();
    }));

    it('should not show the updatedTests badge when the exercise is not released and has student results (edge case)', fakeAsync(() => {
        comp.ngOnInit();
        routeSubject.next({ exerciseId, tab: 'test-cases' });
        // @ts-ignore
        (gradingService as MockProgrammingExerciseGradingService).next(testCases1);
        getExerciseTestCaseStateSubject.next(getExerciseTestCasteStateDTO(false, true, false, moment()));
        getCodeAnalysisCategoriesSubject.next(codeAnalysisCategories1);

        fixture.detectChanges();

        expect(getNoUnsavedChangesBadge()).to.exist;
        expect(getNoUpdatedTestCaseBadge()).not.to.exist;

        tick();
        fixture.destroy();
        flush();
    }));

    it('should show that there are updated test cases if the getExerciseTestCaseState call returns this info', fakeAsync(() => {
        comp.ngOnInit();
        routeSubject.next({ exerciseId, tab: 'test-cases' });
        // @ts-ignore
        (gradingService as MockProgrammingExerciseGradingService).next(testCases1);
        getExerciseTestCaseStateSubject.next(getExerciseTestCasteStateDTO(true, true, true, moment()));
        getCodeAnalysisCategoriesSubject.next(codeAnalysisCategories1);

        fixture.detectChanges();

        expect(getUpdatedTestCaseBadge()).to.exist;
        expect(getNoUpdatedTestCaseBadge()).not.to.exist;

        tick();
        fixture.destroy();
        flush();
    }));

    it('should reset all test cases when the reset button is clicked', fakeAsync(() => {
        comp.ngOnInit();
        routeSubject.next({ exerciseId, tab: 'test-cases' });
        getExerciseTestCaseStateSubject.next(getExerciseTestCasteStateDTO(true, true, false, moment()));
        getCodeAnalysisCategoriesSubject.next(codeAnalysisCategories1);

        (gradingService as any).next(testCases1);

        fixture.detectChanges();

        comp.updateEditedField(3, testCases1[0], EditableField.WEIGHT);
        comp.updateEditedField(4, testCases1[1], EditableField.WEIGHT);

        comp.updateEditedField(2, testCases1[1], EditableField.BONUS_MULTIPLIER);
        comp.updateEditedField(3, testCases1[2], EditableField.BONUS_MULTIPLIER);

        comp.updateEditedField(4, testCases1[0], EditableField.BONUS_POINTS);
        comp.updateEditedField(10, testCases1[2], EditableField.BONUS_POINTS);

        const updatedTestCases: ProgrammingExerciseTestCase[] = [
            { ...testCases1[0], weight: 3, bonusPoints: 4 },
            { ...testCases1[1], weight: 4, bonusMultiplier: 2 },
            { ...testCases1[2], bonusMultiplier: 3, bonusPoints: 10 },
        ];
        updateTestCasesStub.returns(of(updatedTestCases));

        // Save tests.
        comp.saveTestCases();

        fixture.detectChanges();

        expect(updateTestCasesStub).to.have.been.calledOnce;

        expect(comp.changedTestCaseIds).to.have.lengthOf(0);
        testCasesChangedSubject.next(true);

        // Reset button is now enabled because the tests were saved.
        expect(comp.hasUpdatedGradingConfig).to.be.true;

        fixture.detectChanges();

        resetTestCasesStub.returns(of(testCases1));

        const resetButton = getResetButton();
        expectElementToBeEnabled(resetButton);
        resetButton.click();

        fixture.detectChanges();

        expect(resetTestCasesStub).to.have.been.calledOnceWithExactly(exerciseId);
        expect(comp.testCases).to.deep.equal(testCases1);
        expect(comp.changedTestCaseIds).to.have.lengthOf(0);

        tick();
        fixture.destroy();
        flush();
    }));

    it('should update sca category when an input field is updated', fakeAsync(() => {
        comp.ngOnInit();
        routeSubject.next({ exerciseId, tab: 'code-analysis' });
        getExerciseTestCaseStateSubject.next(getExerciseTestCasteStateDTO(true, true, false, moment()));
        getCodeAnalysisCategoriesSubject.next(codeAnalysisCategories1);

        fixture.detectChanges();

        const table = debugElement.query(By.css(codeAnalysisTableId));

        const gradedCategories = comp.staticCodeAnalysisCategories.filter((category) => category.state === StaticCodeAnalysisCategoryState.Graded);

        // get inputs
        const editingInputs = table.queryAll(By.css(tableEditingInput));
        expect(editingInputs).to.have.lengthOf(gradedCategories.length * 2);

        const penaltyInput = editingInputs[0].nativeElement;
        expect(penaltyInput).to.exist;
        penaltyInput.focus();

        // Set new penalty.
        penaltyInput.value = '20';
        penaltyInput.dispatchEvent(new Event('blur'));

        const maxPenaltyInput = editingInputs[1].nativeElement;
        expect(maxPenaltyInput).to.exist;
        maxPenaltyInput.focus();

        // Set new max penalty.
        maxPenaltyInput.value = '100';
        maxPenaltyInput.dispatchEvent(new Event('blur'));

        fixture.detectChanges();

        expect(comp.changedCategoryIds).to.deep.equal([gradedCategories[0].id]);

        // Trigger button should be disabled.
        let triggerButton = getTriggerButton();
        expectElementToBeDisabled(triggerButton);

        const updatedCategory: StaticCodeAnalysisCategory = { ...gradedCategories[0], penalty: 20, maxPenalty: 100 };

        // Save weight.
        updateCategoriesStub.returns(of([updatedCategory]));
        const saveButton = getSaveButton();
        expectElementToBeEnabled(saveButton);
        saveButton.click();

        fixture.detectChanges();

        expect(updateCategoriesStub).to.have.been.calledOnceWithExactly(exerciseId, [StaticCodeAnalysisCategoryUpdate.from(updatedCategory)]);

        const categoryThatWasUpdated = comp.staticCodeAnalysisCategories.find((category) => category.id === updatedCategory.id)!;
        expect(categoryThatWasUpdated.penalty).to.equal(20);
        expect(categoryThatWasUpdated.maxPenalty).to.equal(100);
        expect(comp.changedCategoryIds).to.have.lengthOf(0);

        testCasesChangedSubject.next(true);
        // Trigger button is now enabled because the tests were saved.
        expect(comp.hasUpdatedGradingConfig).to.be.true;

        fixture.detectChanges();

        triggerButton = getTriggerButton();
        expectElementToBeEnabled(triggerButton);

        tick();
        fixture.destroy();
        flush();
    }));
});
