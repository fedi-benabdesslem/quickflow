import { Injectable, signal, computed } from '@angular/core';
import { OAuthService } from 'angular-oauth2-oidc';
import { User } from '../types';

@Injectable({
    providedIn: 'root'
})
export class AuthService {
    private _user = signal<User | null>(null);
    private _token = signal<string | null>(null);
    private _loading = signal<boolean>(true);

    user = computed(() => this._user());
    token = computed(() => this._token());
    loading = computed(() => this._loading());

    constructor(private oauthService: OAuthService) {
        // Load user profile if authenticated
        if (this.oauthService.hasValidAccessToken()) {
            this.loadUserProfile();
        } else {
            this._loading.set(false);
        }
    }

    login() {
        this.oauthService.initLoginFlow();
    }

    logout() {
        // Use the library's built-in logout - it works when Keycloak is configured correctly
        this.oauthService.logOut();
        this._user.set(null);
        this._token.set(null);
    }

    editProfile() {
        // Redirect to Keycloak account management page
        const issuer = this.oauthService.issuer;
        const accountUrl = `${issuer}/account`;
        window.location.href = accountUrl;
    }

    private loadUserProfile() {
        const claims = this.oauthService.getIdentityClaims() as any;
        if (claims) {
            this._user.set({
                id: claims.sub,
                username: claims.preferred_username || claims.name,
                email: claims.email
            });
            this._token.set(this.oauthService.getAccessToken());
        }
        this._loading.set(false);
    }

    isAuthenticated(): boolean {
        return this.oauthService.hasValidAccessToken();
    }

    getAccessToken(): string {
        return this.oauthService.getAccessToken();
    }
}
