import { AuthConfig } from 'angular-oauth2-oidc';

export const authConfig: AuthConfig = {
    issuer: 'http://localhost:8180/realms/quickflow-realm',
    redirectUri: window.location.origin + '/home',
    postLogoutRedirectUri: window.location.origin + '/',
    clientId: 'quickflow-frontend',
    responseType: 'code',
    scope: 'openid profile email',
    requireHttps: false,
    skipIssuerCheck: true,
    oidc: true
};
