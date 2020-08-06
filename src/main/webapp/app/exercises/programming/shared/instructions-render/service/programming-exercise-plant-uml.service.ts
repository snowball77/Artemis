import { Injectable } from '@angular/core';
import { SERVER_API_URL } from 'app/app.constants';
import { HttpClient, HttpParameterCodec, HttpParams } from '@angular/common/http';
import { HttpUrlCustomEncoder } from 'app/shared/util/request-util';
import { Cacheable } from 'ngx-cacheable';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class ProgrammingExercisePlantUmlService {
    private resourceUrl = SERVER_API_URL + 'api/plantuml';
    private encoder: HttpParameterCodec;

    /**
     * Cacheable configuration
     */

    constructor(private http: HttpClient) {
        this.encoder = new HttpUrlCustomEncoder();
    }

    /**
     * Requests the plantuml png file as arraybuffer and converts it to base64.
     * @param plantUml - definition obtained by parsing the README markdown file.
     *
     * Note: we cache up to 100 results in 1 hour so that they do not need to be loaded several time
     */
    @Cacheable({
        /** Cacheable configuration **/
        maxCacheCount: 100,
        maxAge: 3600000, // ms
        slidingExpiration: true,
    })
    getPlantUmlImage(plantUml: string) {
        return this.http
            .get(`${this.resourceUrl}/png`, {
                params: new HttpParams({ encoder: this.encoder }).set('plantuml', plantUml),
                responseType: 'arraybuffer',
            })
            .map((res) => this.convertPlantUmlResponseToBase64(res));
    }

    /**
     * Requests the plantuml svg as string.
     * @param plantUml - definition obtained by parsing the README markdown file.
     *
     * Note: we cache up to 100 results in 1 hour so that they do not need to be loaded several time
     */
    @Cacheable({
        /** Cacheable configuration **/
        maxCacheCount: 100,
        maxAge: 3600000, // ms
        slidingExpiration: true,
    })
    getPlantUmlSvg(plantUml: string): Observable<string> {
        return this.http.get(`${this.resourceUrl}/svg`, {
            params: new HttpParams({ encoder: this.encoder }).set('plantuml', plantUml),
            responseType: 'text',
        });
    }

    private convertPlantUmlResponseToBase64(res: any): string {
        return Buffer.from(res, 'binary').toString('base64');
    }
}
