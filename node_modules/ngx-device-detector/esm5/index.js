import * as tslib_1 from "tslib";
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DeviceDetectorService } from './device-detector.service';
var DeviceDetectorModule = /** @class */ (function () {
    function DeviceDetectorModule() {
    }
    DeviceDetectorModule_1 = DeviceDetectorModule;
    DeviceDetectorModule.forRoot = function () {
        return {
            ngModule: DeviceDetectorModule_1,
            providers: [DeviceDetectorService],
        };
    };
    var DeviceDetectorModule_1;
    DeviceDetectorModule = DeviceDetectorModule_1 = tslib_1.__decorate([
        NgModule({
            imports: [CommonModule],
        })
    ], DeviceDetectorModule);
    return DeviceDetectorModule;
}());
export { DeviceDetectorModule };
export { DeviceDetectorService } from './device-detector.service';
export { ReTree } from './retree';
export * from './device-detector.constants';
//# sourceMappingURL=data:application/json;base64,eyJ2ZXJzaW9uIjozLCJmaWxlIjoiaW5kZXguanMiLCJzb3VyY2VSb290Ijoibmc6Ly9uZ3gtZGV2aWNlLWRldGVjdG9yLyIsInNvdXJjZXMiOlsiaW5kZXgudHMiXSwibmFtZXMiOltdLCJtYXBwaW5ncyI6IjtBQUFBLE9BQU8sRUFBRSxRQUFRLEVBQXVCLE1BQU0sZUFBZSxDQUFBO0FBQzdELE9BQU8sRUFBRSxZQUFZLEVBQUUsTUFBTSxpQkFBaUIsQ0FBQTtBQUM5QyxPQUFPLEVBQUUscUJBQXFCLEVBQUUsTUFBTSwyQkFBMkIsQ0FBQTtBQUtqRTtJQUFBO0lBT0EsQ0FBQzs2QkFQWSxvQkFBb0I7SUFDeEIsNEJBQU8sR0FBZDtRQUNFLE9BQU87WUFDTCxRQUFRLEVBQUUsc0JBQW9CO1lBQzlCLFNBQVMsRUFBRSxDQUFDLHFCQUFxQixDQUFDO1NBQ25DLENBQUE7SUFDSCxDQUFDOztJQU5VLG9CQUFvQjtRQUhoQyxRQUFRLENBQUM7WUFDUixPQUFPLEVBQUUsQ0FBQyxZQUFZLENBQUM7U0FDeEIsQ0FBQztPQUNXLG9CQUFvQixDQU9oQztJQUFELDJCQUFDO0NBQUEsQUFQRCxJQU9DO1NBUFksb0JBQW9CO0FBU2pDLE9BQU8sRUFBRSxxQkFBcUIsRUFBYyxNQUFNLDJCQUEyQixDQUFBO0FBQzdFLE9BQU8sRUFBRSxNQUFNLEVBQUUsTUFBTSxVQUFVLENBQUE7QUFDakMsY0FBYyw2QkFBNkIsQ0FBQSIsInNvdXJjZXNDb250ZW50IjpbImltcG9ydCB7IE5nTW9kdWxlLCBNb2R1bGVXaXRoUHJvdmlkZXJzIH0gZnJvbSAnQGFuZ3VsYXIvY29yZSdcbmltcG9ydCB7IENvbW1vbk1vZHVsZSB9IGZyb20gJ0Bhbmd1bGFyL2NvbW1vbidcbmltcG9ydCB7IERldmljZURldGVjdG9yU2VydmljZSB9IGZyb20gJy4vZGV2aWNlLWRldGVjdG9yLnNlcnZpY2UnXG5cbkBOZ01vZHVsZSh7XG4gIGltcG9ydHM6IFtDb21tb25Nb2R1bGVdLFxufSlcbmV4cG9ydCBjbGFzcyBEZXZpY2VEZXRlY3Rvck1vZHVsZSB7XG4gIHN0YXRpYyBmb3JSb290KCk6IE1vZHVsZVdpdGhQcm92aWRlcnM8RGV2aWNlRGV0ZWN0b3JNb2R1bGU+IHtcbiAgICByZXR1cm4ge1xuICAgICAgbmdNb2R1bGU6IERldmljZURldGVjdG9yTW9kdWxlLFxuICAgICAgcHJvdmlkZXJzOiBbRGV2aWNlRGV0ZWN0b3JTZXJ2aWNlXSxcbiAgICB9XG4gIH1cbn1cblxuZXhwb3J0IHsgRGV2aWNlRGV0ZWN0b3JTZXJ2aWNlLCBEZXZpY2VJbmZvIH0gZnJvbSAnLi9kZXZpY2UtZGV0ZWN0b3Iuc2VydmljZSdcbmV4cG9ydCB7IFJlVHJlZSB9IGZyb20gJy4vcmV0cmVlJ1xuZXhwb3J0ICogZnJvbSAnLi9kZXZpY2UtZGV0ZWN0b3IuY29uc3RhbnRzJ1xuIl19