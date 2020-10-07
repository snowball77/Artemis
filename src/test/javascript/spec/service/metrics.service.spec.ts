import { TestBed } from '@angular/core/testing';

import { JhiMetricsService } from 'app/admin/metrics/metrics.service';
import { SERVER_API_URL } from 'app/app.constants';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

describe('Logs Service', () => {
    let service: JhiMetricsService;
    let httpMock: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
        });

        service = TestBed.inject(JhiMetricsService);
        httpMock = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
        httpMock.verify();
    });

    describe('Service methods', () => {
        it('should call correct URL', () => {
            service.getMetrics().subscribe(() => {});

            const req = httpMock.expectOne({ method: 'GET' });
            const resourceUrl = SERVER_API_URL + 'management/jhimetrics';
            expect(req.request.url).toEqual(resourceUrl);
        });

        it('should return Metrics', () => {
            const metrics: any[] = [];

            service.getMetrics().subscribe((received) => {
                expect(received.body[0]).toEqual(metrics);
            });

            const req = httpMock.expectOne({ method: 'GET' });
            req.flush([metrics]);
        });

        it('should return Thread Dump', () => {
            const dump = [{ name: 'test1', threadState: 'RUNNABLE' }];

            service.threadDump().subscribe((received) => {
                expect(received.body[0]).toEqual(dump);
            });

            const req = httpMock.expectOne({ method: 'GET' });
            req.flush([dump]);
        });
    });
});
