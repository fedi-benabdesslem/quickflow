import { ApplicationConfig, provideZoneChangeDetection, APP_INITIALIZER } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideOAuthClient, OAuthService } from 'angular-oauth2-oidc';
import { authConfig } from './auth.config';

import { routes } from './app.routes';

export function initializeApp(oauthService: OAuthService): () => Promise<void> {
  return () => {
    oauthService.configure(authConfig);

    // Check if this is a redirect from an email action (has iss but no code)
    const urlParams = new URLSearchParams(window.location.search);
    const hasIss = urlParams.has('iss');
    const hasCode = urlParams.has('code');

    // If it's an email action redirect (iss without code), skip OAuth processing entirely
    if (hasIss && !hasCode) {
      console.log('Email verification redirect detected - redirecting to Keycloak login');
      // Clear the URL parameters immediately
      window.history.replaceState({}, document.title, window.location.pathname);
      // Redirect to Keycloak login (don't call tryLogin!)
      return oauthService.loadDiscoveryDocument().then(() => {
        oauthService.initLoginFlow();
      });
    }

    // Normal OAuth flow - wrap in try/catch to handle state/nonce errors gracefully
    return oauthService.loadDiscoveryDocumentAndTryLogin()
      .then(() => {
        // If not authenticated, redirect to Keycloak
        if (!oauthService.hasValidAccessToken()) {
          oauthService.initLoginFlow();
        }
      })
      .catch(err => {
        // If there's an error (like wrong state/nonce), just redirect to login
        console.warn('OAuth error, redirecting to login:', err);
        return oauthService.loadDiscoveryDocument().then(() => {
          oauthService.initLoginFlow();
        });
      });
  };
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideHttpClient(),
    provideOAuthClient(),
    {
      provide: APP_INITIALIZER,
      useFactory: initializeApp,
      deps: [OAuthService],
      multi: true
    }
  ]
};
