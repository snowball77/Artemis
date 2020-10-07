import { ComponentFixture, TestBed, async, inject, tick, fakeAsync } from '@angular/core/testing';
import { FormBuilder } from '@angular/forms';
import { of, throwError } from 'rxjs';
import { JhiLanguageService } from 'ng-jhipster';

import { ArtemisTestModule } from '../../test.module';
import { EMAIL_ALREADY_USED_TYPE, LOGIN_ALREADY_USED_TYPE } from 'app/shared/constants/error.constants';
import { RegisterService } from 'app/account/register/register.service';
import { RegisterComponent } from 'app/account/register/register.component';
import { MockLanguageService } from '../../helpers/mocks/service/mock-language.service';
import { User } from 'app/core/user/user.model';
import { MockSyncStorage } from '../../helpers/mocks/service/mock-sync-storage.service';
import { LocalStorageService, SessionStorageService } from 'ngx-webstorage';
import { ProfileService } from 'app/shared/layouts/profiles/profile.service';
import { MockProfileService } from '../../helpers/mocks/service/mock-profile.service';

describe('Component Tests', () => {
    describe('RegisterComponent', () => {
        let fixture: ComponentFixture<RegisterComponent>;
        let comp: RegisterComponent;

        beforeEach(async(() => {
            TestBed.configureTestingModule({
                imports: [ArtemisTestModule],
                declarations: [RegisterComponent],
                providers: [
                    FormBuilder,
                    { provide: LocalStorageService, useClass: MockSyncStorage },
                    { provide: SessionStorageService, useClass: MockSyncStorage },
                    { provide: ProfileService, useClass: MockProfileService },
                ],
            })
                .overrideTemplate(RegisterComponent, '')
                .compileComponents();
        }));

        beforeEach(() => {
            fixture = TestBed.createComponent(RegisterComponent);
            comp = fixture.componentInstance;
            comp.isRegistrationEnabled = true;
        });

        it('should ensure the two passwords entered match', () => {
            comp.registerForm.patchValue({
                password: 'password',
                confirmPassword: 'non-matching',
            });

            comp.register();

            expect(comp.doNotMatch).toBe(true);
        });

        it('should update success to true after creating an account', inject(
            [RegisterService, JhiLanguageService],
            fakeAsync((service: RegisterService, mockTranslate: MockLanguageService) => {
                spyOn(service, 'save').and.returnValue(of({}));
                comp.registerForm.patchValue({
                    password: 'password',
                    confirmPassword: 'password',
                });

                comp.register();
                tick();
                const user = new User();
                user.email = '';
                user.firstName = '';
                user.lastName = '';
                user.password = 'password';
                user.login = '';
                user.langKey = 'en';
                expect(service.save).toHaveBeenCalledWith(user);
                expect(comp.success).toBe(true);
                expect(mockTranslate.getCurrentLanguageSpy).toHaveBeenCalled();
                expect(comp.errorUserExists).toBe(false);
                expect(comp.errorEmailExists).toBe(false);
                expect(comp.error).toBe(false);
            }),
        ));

        it('should notify of user existence upon 400/login already in use', inject(
            [RegisterService],
            fakeAsync((service: RegisterService) => {
                spyOn(service, 'save').and.returnValue(
                    throwError({
                        status: 400,
                        error: { type: LOGIN_ALREADY_USED_TYPE },
                    }),
                );
                comp.registerForm.patchValue({
                    password: 'password',
                    confirmPassword: 'password',
                });

                comp.register();
                tick();

                expect(comp.errorUserExists).toBe(true);
                expect(comp.errorEmailExists).toBe(false);
                expect(comp.error).toBe(false);
            }),
        ));

        it('should notify of email existence upon 400/email address already in use', inject(
            [RegisterService],
            fakeAsync((service: RegisterService) => {
                spyOn(service, 'save').and.returnValue(
                    throwError({
                        status: 400,
                        error: { type: EMAIL_ALREADY_USED_TYPE },
                    }),
                );
                comp.registerForm.patchValue({
                    password: 'password',
                    confirmPassword: 'password',
                });

                comp.register();
                tick();

                expect(comp.errorEmailExists).toBe(true);
                expect(comp.errorUserExists).toBe(false);
                expect(comp.error).toBe(false);
            }),
        ));

        it('should notify of generic error', inject(
            [RegisterService],
            fakeAsync((service: RegisterService) => {
                spyOn(service, 'save').and.returnValue(
                    throwError({
                        status: 503,
                    }),
                );
                comp.registerForm.patchValue({
                    password: 'password',
                    confirmPassword: 'password',
                });

                comp.register();
                tick();

                expect(comp.errorUserExists).toBe(false);
                expect(comp.errorEmailExists).toBe(false);
                expect(comp.error).toBe(true);
            }),
        ));
    });
});
