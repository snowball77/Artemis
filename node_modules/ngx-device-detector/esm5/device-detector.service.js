import * as tslib_1 from "tslib";
/**
 * Created by ahsanayaz on 08/11/2016.
 */
import { PLATFORM_ID, Inject, Injectable } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import * as Constants from './device-detector.constants';
import { ReTree } from './retree';
import * as i0 from "@angular/core";
var DeviceDetectorService = /** @class */ (function () {
    function DeviceDetectorService(platformId) {
        this.platformId = platformId;
        this.ua = '';
        this.userAgent = '';
        this.os = '';
        this.browser = '';
        this.device = '';
        this.os_version = '';
        this.browser_version = '';
        this.reTree = new ReTree();
        if (isPlatformBrowser(this.platformId) && typeof window !== 'undefined') {
            this.userAgent = window.navigator.userAgent;
        }
        this.setDeviceInfo(this.userAgent);
    }
    /**
     * @author Ahsan Ayaz
     * @desc Sets the initial value of the device when the service is initiated.
     * This value is later accessible for usage
     */
    DeviceDetectorService.prototype.setDeviceInfo = function (ua) {
        var _this = this;
        if (ua === void 0) { ua = this.userAgent; }
        if (ua !== this.userAgent) {
            this.userAgent = ua;
        }
        var mappings = [
            { const: 'OS', prop: 'os' },
            { const: 'BROWSERS', prop: 'browser' },
            { const: 'DEVICES', prop: 'device' },
            { const: 'OS_VERSIONS', prop: 'os_version' },
        ];
        mappings.forEach(function (mapping) {
            _this[mapping.prop] = Object.keys(Constants[mapping.const]).reduce(function (obj, item) {
                if (Constants[mapping.const][item] === 'device') {
                    // hack for iOS 13 Tablet
                    if (isPlatformBrowser(_this.platformId) &&
                        (!!_this.reTree.test(_this.userAgent, Constants.TABLETS_RE['iPad']) ||
                            (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1))) {
                        obj[Constants[mapping.const][item]] = 'iPad';
                        return Object;
                    }
                }
                obj[Constants[mapping.const][item]] = _this.reTree.test(ua, Constants[mapping.const + "_RE"][item]);
                return obj;
            }, {});
        });
        mappings.forEach(function (mapping) {
            _this[mapping.prop] = Object.keys(Constants[mapping.const])
                .map(function (key) {
                return Constants[mapping.const][key];
            })
                .reduce(function (previousValue, currentValue) {
                if (mapping.prop === 'device' && previousValue === Constants[mapping.const].ANDROID) {
                    // if we have the actual device found, instead of 'Android', return the actual device
                    return _this[mapping.prop][currentValue] ? currentValue : previousValue;
                }
                else {
                    return previousValue === Constants[mapping.const].UNKNOWN && _this[mapping.prop][currentValue]
                        ? currentValue
                        : previousValue;
                }
            }, Constants[mapping.const].UNKNOWN);
        });
        this.browser_version = '0';
        if (this.browser !== Constants.BROWSERS.UNKNOWN) {
            var re = Constants.BROWSER_VERSIONS_RE[this.browser];
            var res = this.reTree.exec(ua, re);
            if (!!res) {
                this.browser_version = res[1];
            }
        }
    };
    /**
     * @author Ahsan Ayaz
     * @desc Returns the device information
     * @returns the device information object.
     */
    DeviceDetectorService.prototype.getDeviceInfo = function () {
        var deviceInfo = {
            userAgent: this.userAgent,
            os: this.os,
            browser: this.browser,
            device: this.device,
            os_version: this.os_version,
            browser_version: this.browser_version,
        };
        return deviceInfo;
    };
    /**
     * @author Ahsan Ayaz
     * @desc Compares the current device info with the mobile devices to check
     * if the current device is a mobile and also check current device is tablet so it will return false.
     * @returns whether the current device is a mobile
     */
    DeviceDetectorService.prototype.isMobile = function (userAgent) {
        var _this = this;
        if (userAgent === void 0) { userAgent = this.userAgent; }
        if (this.isTablet(userAgent)) {
            return false;
        }
        var match = Object.keys(Constants.MOBILES_RE).find(function (mobile) {
            return _this.reTree.test(userAgent, Constants.MOBILES_RE[mobile]);
        });
        return !!match;
    };
    /**
     * @author Ahsan Ayaz
     * @desc Compares the current device info with the tablet devices to check
     * if the current device is a tablet.
     * @returns whether the current device is a tablet
     */
    DeviceDetectorService.prototype.isTablet = function (userAgent) {
        var _this = this;
        if (userAgent === void 0) { userAgent = this.userAgent; }
        if (isPlatformBrowser(this.platformId) &&
            (!!this.reTree.test(this.userAgent, Constants.TABLETS_RE['iPad']) ||
                (typeof navigator !== 'undefined' && navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1))) {
            return true;
        }
        var match = Object.keys(Constants.TABLETS_RE).find(function (mobile) {
            return !!_this.reTree.test(userAgent, Constants.TABLETS_RE[mobile]);
        });
        return !!match;
    };
    /**
     * @author Ahsan Ayaz
     * @desc Compares the current device info with the desktop devices to check
     * if the current device is a desktop device.
     * @returns whether the current device is a desktop device
     */
    DeviceDetectorService.prototype.isDesktop = function (userAgent) {
        if (userAgent === void 0) { userAgent = this.userAgent; }
        var desktopDevices = [Constants.DEVICES.PS4, Constants.DEVICES.CHROME_BOOK, Constants.DEVICES.UNKNOWN];
        if (this.device === Constants.DEVICES.UNKNOWN) {
            if (this.isMobile(userAgent) || this.isTablet(userAgent)) {
                return false;
            }
        }
        return desktopDevices.indexOf(this.device) > -1;
    };
    DeviceDetectorService.ngInjectableDef = i0.defineInjectable({ factory: function DeviceDetectorService_Factory() { return new DeviceDetectorService(i0.inject(i0.PLATFORM_ID)); }, token: DeviceDetectorService, providedIn: "root" });
    DeviceDetectorService = tslib_1.__decorate([
        Injectable({
            providedIn: 'root',
        }),
        tslib_1.__param(0, Inject(PLATFORM_ID)),
        tslib_1.__metadata("design:paramtypes", [Object])
    ], DeviceDetectorService);
    return DeviceDetectorService;
}());
export { DeviceDetectorService };
//# sourceMappingURL=data:application/json;base64,eyJ2ZXJzaW9uIjozLCJmaWxlIjoiZGV2aWNlLWRldGVjdG9yLnNlcnZpY2UuanMiLCJzb3VyY2VSb290Ijoibmc6Ly9uZ3gtZGV2aWNlLWRldGVjdG9yLyIsInNvdXJjZXMiOlsiZGV2aWNlLWRldGVjdG9yLnNlcnZpY2UudHMiXSwibmFtZXMiOltdLCJtYXBwaW5ncyI6IjtBQUFBOztHQUVHO0FBQ0gsT0FBTyxFQUFFLFdBQVcsRUFBRSxNQUFNLEVBQUUsVUFBVSxFQUFFLE1BQU0sZUFBZSxDQUFBO0FBQy9ELE9BQU8sRUFBRSxpQkFBaUIsRUFBRSxNQUFNLGlCQUFpQixDQUFBO0FBQ25ELE9BQU8sS0FBSyxTQUFTLE1BQU0sNkJBQTZCLENBQUE7QUFDeEQsT0FBTyxFQUFFLE1BQU0sRUFBRSxNQUFNLFVBQVUsQ0FBQTs7QUFjakM7SUFVRSwrQkFBeUMsVUFBZTtRQUFmLGVBQVUsR0FBVixVQUFVLENBQUs7UUFUeEQsT0FBRSxHQUFHLEVBQUUsQ0FBQTtRQUNQLGNBQVMsR0FBRyxFQUFFLENBQUE7UUFDZCxPQUFFLEdBQUcsRUFBRSxDQUFBO1FBQ1AsWUFBTyxHQUFHLEVBQUUsQ0FBQTtRQUNaLFdBQU0sR0FBRyxFQUFFLENBQUE7UUFDWCxlQUFVLEdBQUcsRUFBRSxDQUFBO1FBQ2Ysb0JBQWUsR0FBRyxFQUFFLENBQUE7UUFDcEIsV0FBTSxHQUFHLElBQUksTUFBTSxFQUFFLENBQUE7UUFHbkIsSUFBSSxpQkFBaUIsQ0FBQyxJQUFJLENBQUMsVUFBVSxDQUFDLElBQUksT0FBTyxNQUFNLEtBQUssV0FBVyxFQUFFO1lBQ3ZFLElBQUksQ0FBQyxTQUFTLEdBQUcsTUFBTSxDQUFDLFNBQVMsQ0FBQyxTQUFTLENBQUE7U0FDNUM7UUFDRCxJQUFJLENBQUMsYUFBYSxDQUFDLElBQUksQ0FBQyxTQUFTLENBQUMsQ0FBQTtJQUNwQyxDQUFDO0lBRUQ7Ozs7T0FJRztJQUNILDZDQUFhLEdBQWIsVUFBYyxFQUFtQjtRQUFqQyxpQkFzREM7UUF0RGEsbUJBQUEsRUFBQSxLQUFLLElBQUksQ0FBQyxTQUFTO1FB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QUEsRUFBQSxZQUFZLElBQUksQ0FBQyxTQUFTO1FBQ3pDLElBQU0sY0FBYyxHQUFHLENBQUMsU0FBUyxDQUFDLE9BQU8sQ0FBQyxHQUFHLEVBQUUsU0FBUyxDQUFDLE9BQU8sQ0FBQyxXQUFXLEVBQUUsU0FBUyxDQUFDLE9BQU8sQ0FBQyxPQUFPLENBQUMsQ0FBQTtRQUN4RyxJQUFJLElBQUksQ0FBQyxNQUFNLEtBQUssU0FBUyxDQUFDLE9BQU8sQ0FBQyxPQUFPLEVBQUU7WUFDN0MsSUFBSSxJQUFJLENBQUMsUUFBUSxDQUFDLFNBQVMsQ0FBQyxJQUFJLElBQUksQ0FBQyxRQUFRLENBQUMsU0FBUyxDQUFDLEVBQUU7Z0JBQ3hELE9BQU8sS0FBSyxDQUFBO2FBQ2I7U0FDRjtRQUNELE9BQU8sY0FBYyxDQUFDLE9BQU8sQ0FBQyxJQUFJLENBQUMsTUFBTSxDQUFDLEdBQUcsQ0FBQyxDQUFDLENBQUE7SUFDakQsQ0FBQzs7SUFqSlUscUJBQXFCO1FBSGpDLFVBQVUsQ0FBQztZQUNWLFVBQVUsRUFBRSxNQUFNO1NBQ25CLENBQUM7UUFXYSxtQkFBQSxNQUFNLENBQUMsV0FBVyxDQUFDLENBQUE7O09BVnJCLHFCQUFxQixDQWtKakM7Z0NBdEtEO0NBc0tDLEFBbEpELElBa0pDO1NBbEpZLHFCQUFxQiIsInNvdXJjZXNDb250ZW50IjpbIi8qKlxuICogQ3JlYXRlZCBieSBhaHNhbmF5YXogb24gMDgvMTEvMjAxNi5cbiAqL1xuaW1wb3J0IHsgUExBVEZPUk1fSUQsIEluamVjdCwgSW5qZWN0YWJsZSB9IGZyb20gJ0Bhbmd1bGFyL2NvcmUnXG5pbXBvcnQgeyBpc1BsYXRmb3JtQnJvd3NlciB9IGZyb20gJ0Bhbmd1bGFyL2NvbW1vbidcbmltcG9ydCAqIGFzIENvbnN0YW50cyBmcm9tICcuL2RldmljZS1kZXRlY3Rvci5jb25zdGFudHMnXG5pbXBvcnQgeyBSZVRyZWUgfSBmcm9tICcuL3JldHJlZSdcblxuZXhwb3J0IGludGVyZmFjZSBEZXZpY2VJbmZvIHtcbiAgdXNlckFnZW50OiBzdHJpbmdcbiAgb3M6IHN0cmluZ1xuICBicm93c2VyOiBzdHJpbmdcbiAgZGV2aWNlOiBzdHJpbmdcbiAgb3NfdmVyc2lvbjogc3RyaW5nXG4gIGJyb3dzZXJfdmVyc2lvbjogc3RyaW5nXG59XG5cbkBJbmplY3RhYmxlKHtcbiAgcHJvdmlkZWRJbjogJ3Jvb3QnLFxufSlcbmV4cG9ydCBjbGFzcyBEZXZpY2VEZXRlY3RvclNlcnZpY2Uge1xuICB1YSA9ICcnXG4gIHVzZXJBZ2VudCA9ICcnXG4gIG9zID0gJydcbiAgYnJvd3NlciA9ICcnXG4gIGRldmljZSA9ICcnXG4gIG9zX3ZlcnNpb24gPSAnJ1xuICBicm93c2VyX3ZlcnNpb24gPSAnJ1xuICByZVRyZWUgPSBuZXcgUmVUcmVlKClcblxuICBjb25zdHJ1Y3RvcihASW5qZWN0KFBMQVRGT1JNX0lEKSBwcml2YXRlIHBsYXRmb3JtSWQ6IGFueSkge1xuICAgIGlmIChpc1BsYXRmb3JtQnJvd3Nlcih0aGlzLnBsYXRmb3JtSWQpICYmIHR5cGVvZiB3aW5kb3cgIT09ICd1bmRlZmluZWQnKSB7XG4gICAgICB0aGlzLnVzZXJBZ2VudCA9IHdpbmRvdy5uYXZpZ2F0b3IudXNlckFnZW50XG4gICAgfVxuICAgIHRoaXMuc2V0RGV2aWNlSW5mbyh0aGlzLnVzZXJBZ2VudClcbiAgfVxuXG4gIC8qKlxuICAgKiBAYXV0aG9yIEFoc2FuIEF5YXpcbiAgICogQGRlc2MgU2V0cyB0aGUgaW5pdGlhbCB2YWx1ZSBvZiB0aGUgZGV2aWNlIHdoZW4gdGhlIHNlcnZpY2UgaXMgaW5pdGlhdGVkLlxuICAgKiBUaGlzIHZhbHVlIGlzIGxhdGVyIGFjY2Vzc2libGUgZm9yIHVzYWdlXG4gICAqL1xuICBzZXREZXZpY2VJbmZvKHVhID0gdGhpcy51c2VyQWdlbnQpIHtcbiAgICBpZiAodWEgIT09IHRoaXMudXNlckFnZW50KSB7XG4gICAgICB0aGlzLnVzZXJBZ2VudCA9IHVhXG4gICAgfVxuICAgIGxldCBtYXBwaW5ncyA9IFtcbiAgICAgIHsgY29uc3Q6ICdPUycsIHByb3A6ICdvcycgfSxcbiAgICAgIHsgY29uc3Q6ICdCUk9XU0VSUycsIHByb3A6ICdicm93c2VyJyB9LFxuICAgICAgeyBjb25zdDogJ0RFVklDRVMnLCBwcm9wOiAnZGV2aWNlJyB9LFxuICAgICAgeyBjb25zdDogJ09TX1ZFUlNJT05TJywgcHJvcDogJ29zX3ZlcnNpb24nIH0sXG4gICAgXVxuXG4gICAgbWFwcGluZ3MuZm9yRWFjaChtYXBwaW5nID0+IHtcbiAgICAgIHRoaXNbbWFwcGluZy5wcm9wXSA9IE9iamVjdC5rZXlzKENvbnN0YW50c1ttYXBwaW5nLmNvbnN0XSkucmVkdWNlKChvYmo6IGFueSwgaXRlbTogYW55KSA9PiB7XG4gICAgICAgIGlmIChDb25zdGFudHNbbWFwcGluZy5jb25zdF1baXRlbV0gPT09ICdkZXZpY2UnKSB7XG4gICAgICAgICAgLy8gaGFjayBmb3IgaU9TIDEzIFRhYmxldFxuICAgICAgICAgIGlmIChcbiAgICAgICAgICAgIGlzUGxhdGZvcm1Ccm93c2VyKHRoaXMucGxhdGZvcm1JZCkgJiZcbiAgICAgICAgICAgICghIXRoaXMucmVUcmVlLnRlc3QodGhpcy51c2VyQWdlbnQsIENvbnN0YW50cy5UQUJMRVRTX1JFWydpUGFkJ10pIHx8XG4gICAgICAgICAgICAgIChuYXZpZ2F0b3IucGxhdGZvcm0gPT09ICdNYWNJbnRlbCcgJiYgbmF2aWdhdG9yLm1heFRvdWNoUG9pbnRzID4gMSkpXG4gICAgICAgICAgKSB7XG4gICAgICAgICAgICBvYmpbQ29uc3RhbnRzW21hcHBpbmcuY29uc3RdW2l0ZW1dXSA9ICdpUGFkJ1xuICAgICAgICAgICAgcmV0dXJuIE9iamVjdFxuICAgICAgICAgIH1cbiAgICAgICAgfVxuICAgICAgICBvYmpbQ29uc3RhbnRzW21hcHBpbmcuY29uc3RdW2l0ZW1dXSA9IHRoaXMucmVUcmVlLnRlc3QodWEsIENvbnN0YW50c1tgJHttYXBwaW5nLmNvbnN0fV9SRWBdW2l0ZW1dKVxuICAgICAgICByZXR1cm4gb2JqXG4gICAgICB9LCB7fSlcbiAgICB9KVxuXG4gICAgbWFwcGluZ3MuZm9yRWFjaChtYXBwaW5nID0+IHtcbiAgICAgIHRoaXNbbWFwcGluZy5wcm9wXSA9IE9iamVjdC5rZXlzKENvbnN0YW50c1ttYXBwaW5nLmNvbnN0XSlcbiAgICAgICAgLm1hcChrZXkgPT4ge1xuICAgICAgICAgIHJldHVybiBDb25zdGFudHNbbWFwcGluZy5jb25zdF1ba2V5XVxuICAgICAgICB9KVxuICAgICAgICAucmVkdWNlKChwcmV2aW91c1ZhbHVlLCBjdXJyZW50VmFsdWUpID0+IHtcbiAgICAgICAgICBpZiAobWFwcGluZy5wcm9wID09PSAnZGV2aWNlJyAmJiBwcmV2aW91c1ZhbHVlID09PSBDb25zdGFudHNbbWFwcGluZy5jb25zdF0uQU5EUk9JRCkge1xuICAgICAgICAgICAgLy8gaWYgd2UgaGF2ZSB0aGUgYWN0dWFsIGRldmljZSBmb3VuZCwgaW5zdGVhZCBvZiAnQW5kcm9pZCcsIHJldHVybiB0aGUgYWN0dWFsIGRldmljZVxuICAgICAgICAgICAgcmV0dXJuIHRoaXNbbWFwcGluZy5wcm9wXVtjdXJyZW50VmFsdWVdID8gY3VycmVudFZhbHVlIDogcHJldmlvdXNWYWx1ZVxuICAgICAgICAgIH0gZWxzZSB7XG4gICAgICAgICAgICByZXR1cm4gcHJldmlvdXNWYWx1ZSA9PT0gQ29uc3RhbnRzW21hcHBpbmcuY29uc3RdLlVOS05PV04gJiYgdGhpc1ttYXBwaW5nLnByb3BdW2N1cnJlbnRWYWx1ZV1cbiAgICAgICAgICAgICAgPyBjdXJyZW50VmFsdWVcbiAgICAgICAgICAgICAgOiBwcmV2aW91c1ZhbHVlXG4gICAgICAgICAgfVxuICAgICAgICB9LCBDb25zdGFudHNbbWFwcGluZy5jb25zdF0uVU5LTk9XTilcbiAgICB9KVxuXG4gICAgdGhpcy5icm93c2VyX3ZlcnNpb24gPSAnMCdcbiAgICBpZiAodGhpcy5icm93c2VyICE9PSBDb25zdGFudHMuQlJPV1NFUlMuVU5LTk9XTikge1xuICAgICAgbGV0IHJlID0gQ29uc3RhbnRzLkJST1dTRVJfVkVSU0lPTlNfUkVbdGhpcy5icm93c2VyXVxuICAgICAgbGV0IHJlcyA9IHRoaXMucmVUcmVlLmV4ZWModWEsIHJlKVxuICAgICAgaWYgKCEhcmVzKSB7XG4gICAgICAgIHRoaXMuYnJvd3Nlcl92ZXJzaW9uID0gcmVzWzFdXG4gICAgICB9XG4gICAgfVxuICB9XG5cbiAgLyoqXG4gICAqIEBhdXRob3IgQWhzYW4gQXlhelxuICAgKiBAZGVzYyBSZXR1cm5zIHRoZSBkZXZpY2UgaW5mb3JtYXRpb25cbiAgICogQHJldHVybnMgdGhlIGRldmljZSBpbmZvcm1hdGlvbiBvYmplY3QuXG4gICAqL1xuICBwdWJsaWMgZ2V0RGV2aWNlSW5mbygpOiBEZXZpY2VJbmZvIHtcbiAgICBjb25zdCBkZXZpY2VJbmZvOiBEZXZpY2VJbmZvID0ge1xuICAgICAgdXNlckFnZW50OiB0aGlzLnVzZXJBZ2VudCxcbiAgICAgIG9zOiB0aGlzLm9zLFxuICAgICAgYnJvd3NlcjogdGhpcy5icm93c2VyLFxuICAgICAgZGV2aWNlOiB0aGlzLmRldmljZSxcbiAgICAgIG9zX3ZlcnNpb246IHRoaXMub3NfdmVyc2lvbixcbiAgICAgIGJyb3dzZXJfdmVyc2lvbjogdGhpcy5icm93c2VyX3ZlcnNpb24sXG4gICAgfVxuICAgIHJldHVybiBkZXZpY2VJbmZvXG4gIH1cblxuICAvKipcbiAgICogQGF1dGhvciBBaHNhbiBBeWF6XG4gICAqIEBkZXNjIENvbXBhcmVzIHRoZSBjdXJyZW50IGRldmljZSBpbmZvIHdpdGggdGhlIG1vYmlsZSBkZXZpY2VzIHRvIGNoZWNrXG4gICAqIGlmIHRoZSBjdXJyZW50IGRldmljZSBpcyBhIG1vYmlsZSBhbmQgYWxzbyBjaGVjayBjdXJyZW50IGRldmljZSBpcyB0YWJsZXQgc28gaXQgd2lsbCByZXR1cm4gZmFsc2UuXG4gICAqIEByZXR1cm5zIHdoZXRoZXIgdGhlIGN1cnJlbnQgZGV2aWNlIGlzIGEgbW9iaWxlXG4gICAqL1xuICBwdWJsaWMgaXNNb2JpbGUodXNlckFnZW50ID0gdGhpcy51c2VyQWdlbnQpOiBib29sZWFuIHtcbiAgICBpZiAodGhpcy5pc1RhYmxldCh1c2VyQWdlbnQpKSB7XG4gICAgICByZXR1cm4gZmFsc2VcbiAgICB9XG4gICAgY29uc3QgbWF0Y2ggPSBPYmplY3Qua2V5cyhDb25zdGFudHMuTU9CSUxFU19SRSkuZmluZChtb2JpbGUgPT4ge1xuICAgICAgcmV0dXJuIHRoaXMucmVUcmVlLnRlc3QodXNlckFnZW50LCBDb25zdGFudHMuTU9CSUxFU19SRVttb2JpbGVdKVxuICAgIH0pXG4gICAgcmV0dXJuICEhbWF0Y2hcbiAgfVxuXG4gIC8qKlxuICAgKiBAYXV0aG9yIEFoc2FuIEF5YXpcbiAgICogQGRlc2MgQ29tcGFyZXMgdGhlIGN1cnJlbnQgZGV2aWNlIGluZm8gd2l0aCB0aGUgdGFibGV0IGRldmljZXMgdG8gY2hlY2tcbiAgICogaWYgdGhlIGN1cnJlbnQgZGV2aWNlIGlzIGEgdGFibGV0LlxuICAgKiBAcmV0dXJucyB3aGV0aGVyIHRoZSBjdXJyZW50IGRldmljZSBpcyBhIHRhYmxldFxuICAgKi9cbiAgcHVibGljIGlzVGFibGV0KHVzZXJBZ2VudCA9IHRoaXMudXNlckFnZW50KSB7XG4gICAgaWYgKFxuICAgICAgaXNQbGF0Zm9ybUJyb3dzZXIodGhpcy5wbGF0Zm9ybUlkKSAmJlxuICAgICAgKCEhdGhpcy5yZVRyZWUudGVzdCh0aGlzLnVzZXJBZ2VudCwgQ29uc3RhbnRzLlRBQkxFVFNfUkVbJ2lQYWQnXSkgfHxcbiAgICAgICAgKHR5cGVvZiBuYXZpZ2F0b3IgIT09ICd1bmRlZmluZWQnICYmIG5hdmlnYXRvci5wbGF0Zm9ybSA9PT0gJ01hY0ludGVsJyAmJiBuYXZpZ2F0b3IubWF4VG91Y2hQb2ludHMgPiAxKSlcbiAgICApIHtcbiAgICAgIHJldHVybiB0cnVlXG4gICAgfVxuICAgIGNvbnN0IG1hdGNoID0gT2JqZWN0LmtleXMoQ29uc3RhbnRzLlRBQkxFVFNfUkUpLmZpbmQobW9iaWxlID0+IHtcbiAgICAgIHJldHVybiAhIXRoaXMucmVUcmVlLnRlc3QodXNlckFnZW50LCBDb25zdGFudHMuVEFCTEVUU19SRVttb2JpbGVdKVxuICAgIH0pXG4gICAgcmV0dXJuICEhbWF0Y2hcbiAgfVxuXG4gIC8qKlxuICAgKiBAYXV0aG9yIEFoc2FuIEF5YXpcbiAgICogQGRlc2MgQ29tcGFyZXMgdGhlIGN1cnJlbnQgZGV2aWNlIGluZm8gd2l0aCB0aGUgZGVza3RvcCBkZXZpY2VzIHRvIGNoZWNrXG4gICAqIGlmIHRoZSBjdXJyZW50IGRldmljZSBpcyBhIGRlc2t0b3AgZGV2aWNlLlxuICAgKiBAcmV0dXJucyB3aGV0aGVyIHRoZSBjdXJyZW50IGRldmljZSBpcyBhIGRlc2t0b3AgZGV2aWNlXG4gICAqL1xuICBwdWJsaWMgaXNEZXNrdG9wKHVzZXJBZ2VudCA9IHRoaXMudXNlckFnZW50KSB7XG4gICAgY29uc3QgZGVza3RvcERldmljZXMgPSBbQ29uc3RhbnRzLkRFVklDRVMuUFM0LCBDb25zdGFudHMuREVWSUNFUy5DSFJPTUVfQk9PSywgQ29uc3RhbnRzLkRFVklDRVMuVU5LTk9XTl1cbiAgICBpZiAodGhpcy5kZXZpY2UgPT09IENvbnN0YW50cy5ERVZJQ0VTLlVOS05PV04pIHtcbiAgICAgIGlmICh0aGlzLmlzTW9iaWxlKHVzZXJBZ2VudCkgfHwgdGhpcy5pc1RhYmxldCh1c2VyQWdlbnQpKSB7XG4gICAgICAgIHJldHVybiBmYWxzZVxuICAgICAgfVxuICAgIH1cbiAgICByZXR1cm4gZGVza3RvcERldmljZXMuaW5kZXhPZih0aGlzLmRldmljZSkgPiAtMVxuICB9XG59XG4iXX0=