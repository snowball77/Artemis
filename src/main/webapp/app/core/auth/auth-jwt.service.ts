import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { LocalStorageService, SessionStorageService } from 'ngx-webstorage';
import { SERVER_API_URL } from 'app/app.constants';
import { map } from 'rxjs/operators';

export class Credentials {
    constructor(public username: string, public password: string, public rememberMe: boolean) {}
}

type JwtToken = {
    id_token: string;
};

export interface IAuthServerProvider {
    getToken: () => string;
    login: (credentials: Credentials) => Observable<void>;
    loginWithToken: (jwt: string, rememberMe: boolean) => Promise<string>;
    storeAuthenticationToken: (jwt: string, rememberMe: boolean) => void;
    removeAuthTokenFromCaches: () => Observable<null>;
    clearCaches: () => Observable<null>;
}

@Injectable({ providedIn: 'root' })
export class AuthServerProvider implements IAuthServerProvider {
    constructor(private http: HttpClient, private localStorage: LocalStorageService, private sessionStorage: SessionStorageService) {}

    getToken() {
        return this.localStorage.retrieve('authenticationToken') || this.sessionStorage.retrieve('authenticationToken');
    }

    login(credentials: Credentials): Observable<void> {
        return this.http.post<JwtToken>(SERVER_API_URL + 'api/authenticate', credentials).pipe(map(response => this.authenticateSuccess(response, credentials.rememberMe)));
    }

    loginWithToken(jwt: string, rememberMe: boolean): Promise<string> {
        if (jwt) {
            this.storeAuthenticationToken(jwt, rememberMe);
            return Promise.resolve(jwt);
        } else {
            return Promise.reject('auth-jwt-service Promise reject'); // Put appropriate error message here
        }
    }

    private authenticateSuccess(response: JwtToken, rememberMe: boolean): void {
        const jwt = response.id_token;
        this.storeAuthenticationToken(jwt, rememberMe);
    }

    storeAuthenticationToken(jwt: string, rememberMe: boolean): void {
        if (rememberMe) {
            this.localStorage.store('authenticationToken', jwt);
        } else {
            this.sessionStorage.store('authenticationToken', jwt);
        }
    }

    /**
     * Removes the user's auth tokens from the browser's caches.
     * This will lead to all endpoint requests failing with a 401.
     */
    removeAuthTokenFromCaches(): Observable<null> {
        this.localStorage.clear('authenticationToken');
        this.sessionStorage.clear('authenticationToken');
        // The local or session storage might have to be cleared asynchronously in future due to updated browser apis. This is why this method is already acting if it was asynchronous.
        return of(null);
    }

    /**
     * Clears all the caches, should be invoked during logout
     */
    clearCaches(): Observable<null> {
        this.localStorage.clear();
        this.sessionStorage.clear();
        // The local or session storage might have to be cleared asynchronously in future due to updated browser apis. This is why this method is already acting if it was asynchronous.
        return of(null);
    }
}
