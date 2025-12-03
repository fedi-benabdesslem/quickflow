import { Routes } from '@angular/router';
import { HomeComponent } from './components/home/home';
import { MeetingComponent } from './components/meeting/meeting';
import { EmailComponent } from './components/email/email';
import { ReviewComponent } from './components/review/review';
import { inject } from '@angular/core';
import { AuthService } from './services/auth.service';
import { Router } from '@angular/router';

const authGuard = () => {
    const auth = inject(AuthService);

    if (auth.isAuthenticated()) {
        return true;
    }

    // Not authenticated - return false and let APP_INITIALIZER handle redirect to Keycloak
    return false;
};

export const routes: Routes = [
    { path: '', redirectTo: 'home', pathMatch: 'full' },
    { path: 'home', component: HomeComponent, canActivate: [authGuard] },
    { path: 'meeting', component: MeetingComponent, canActivate: [authGuard] },
    { path: 'email', component: EmailComponent, canActivate: [authGuard] },
    { path: 'review', component: ReviewComponent, canActivate: [authGuard] },
    { path: '**', redirectTo: 'home' }
];
