import { Directive, HostBinding, HostListener, Input, OnInit } from '@angular/core';
import { ProfileService } from 'app/shared/layouts/profiles/profile.service';
import { take, tap } from 'rxjs/operators';
import { createBuildPlanUrl } from 'app/exercises/programming/shared/utils/build-plan-link.directive';
import { ProfileInfo } from 'app/shared/layouts/profiles/profile-info.model';

@Directive({ selector: 'button[jhiBuildPlanButton], jhi-button[jhiBuildPlanButton]' })
export class BuildPlanButtonDirective implements OnInit {
    @HostBinding('style.visibility')
    visibility = 'hidden';

    private participationBuildPlanId: string;
    private exerciseProjectKey: string;
    private buildPlanLink: string | null;
    private templateLink: string;

    constructor(private profileService: ProfileService) {}

    /**
     * Life cycle hook called by Angular to indicate that Angular is done creating the component
     */
    ngOnInit(): void {
        this.profileService
            .getProfileInfo()
            .pipe(
                take(1),
                tap((info: ProfileInfo) => {
                    this.templateLink = info.buildPlanURLTemplate;
                    this.linkToBuildPlan = createBuildPlanUrl(this.templateLink, this.exerciseProjectKey, this.participationBuildPlanId);
                }),
            )
            .subscribe();
    }

    /**
     * Action on click.
     */
    @HostListener('click')
    onClick() {
        window.open(this.buildPlanLink!);
    }

    /**
     * Sets the exerciseProjectKey property and updates the linkToBuildPlan property.
     * @param {string} key new exerciseProjectKey value
     */
    @Input()
    set projectKey(key: string) {
        this.exerciseProjectKey = key;
        this.linkToBuildPlan = createBuildPlanUrl(this.templateLink, this.exerciseProjectKey, this.participationBuildPlanId);
    }

    /**
     * Sets the participationBuildPlanId property and updates the linkToBuildPlan property.
     * @param {string} planId new participationBuildPlanId value
     */
    @Input()
    set buildPlanId(planId: string) {
        this.participationBuildPlanId = planId;
        this.linkToBuildPlan = createBuildPlanUrl(this.templateLink, this.exerciseProjectKey, this.participationBuildPlanId);
    }

    /**
     * Sets buildPlanLink according to parameter string and visibility according to whether parameter is null.
     * null => hidden | not null => visible
     * @param {string} link link to build plan
     */
    set linkToBuildPlan(link: string | null) {
        this.buildPlanLink = link;
        this.visibility = link ? 'visible' : 'hidden';
    }
}
