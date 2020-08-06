import { HttpParams } from '@angular/common/http';
import { HttpParameterCodec } from '@angular/common/http';

export const createRequestOption = (req?: any): HttpParams => {
    let options: HttpParams = new HttpParams();
    if (req) {
        Object.keys(req).forEach((key) => {
            if (key !== 'sort') {
                options = options.set(key, req[key]);
            }
        });
        if (req.sort) {
            req.sort.forEach((val: any) => {
                options = options.append('sort', val);
            });
        }
    }
    return options;
};

/**
 * @class HttpUrlCustomEncoder
 * @desc Custom HttpParamEncoder implementation which defaults to using encodeURIComponent to encode params
 */
export class HttpUrlCustomEncoder implements HttpParameterCodec {
    /**
     * Encodes key.
     * @param k - key to be encoded.
     */
    encodeKey(k: string): string {
        return encodeURIComponent(k);
    }

    /**
     * Encodes value.
     * @param v - value to be encoded.
     */
    encodeValue(v: string): string {
        return encodeURIComponent(v);
    }

    /**
     * Decodes key.
     * @param k - key to be decoded.
     */
    decodeKey(k: string): string {
        return decodeURIComponent(k);
    }

    /**
     * Decodes value.
     * @param v - value to be decoded.
     */
    decodeValue(v: string) {
        return decodeURIComponent(v);
    }
}
