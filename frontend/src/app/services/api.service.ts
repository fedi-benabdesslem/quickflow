import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, catchError, of } from 'rxjs';
import { ApiResponse, MeetingFormData } from '../types';

@Injectable({
    providedIn: 'root'
})
export class ApiService {
    private apiUrl = '/api'; // Proxy handles the rest

    constructor(private http: HttpClient) { }

    private getHeaders(token?: string): HttpHeaders {
        let headers = new HttpHeaders({
            'Content-Type': 'application/json'
        });
        if (token) {
            headers = headers.set('Authorization', `Bearer ${token}`);
        }
        return headers;
    }

    private handleError<T>(result?: T) {
        return (error: any): Observable<T> => {
            console.error(error);
            return of(result as T);
        };
    }

    login(username: string, password: string): Observable<ApiResponse> {
        return this.http.post<ApiResponse>(`${this.apiUrl}/login`, { username, password })
            .pipe(catchError(this.handleError<ApiResponse>({ status: 'error', message: 'Network error' })));
    }

    signup(username: string, email: string, password: string): Observable<ApiResponse> {
        return this.http.post<ApiResponse>(`${this.apiUrl}/signup`, { username, email, password })
            .pipe(catchError(this.handleError<ApiResponse>({ status: 'error', message: 'Network error' })));
    }

    generateMeeting(token: string, formData: MeetingFormData): Observable<ApiResponse> {
        return this.http.post<ApiResponse>(`${this.apiUrl}/meeting/generate`, formData, { headers: this.getHeaders(token) })
            .pipe(catchError(this.handleError<ApiResponse>({ status: 'error', message: 'Network error' })));
    }

    sendEmail(token: string, recipients: string, content: string, userId?: string): Observable<ApiResponse> {
        const body = {
            recipients: recipients.split(',').map(email => email.trim()),
            userId: userId || 'anonymous',
            input: content,
            bulletPoints: []
        };
        return this.http.post<ApiResponse>(`${this.apiUrl}/email/send`, body, { headers: this.getHeaders(token) })
            .pipe(catchError(this.handleError<ApiResponse>({ status: 'error', message: 'Network error' })));
    }

    sendFinal(token: string, reviewType: 'email' | 'pv', data: Record<string, unknown>): Observable<ApiResponse> {
        return this.http.post<ApiResponse>(`${this.apiUrl}/${reviewType}/send-final`, data, { headers: this.getHeaders(token) })
            .pipe(catchError(this.handleError<ApiResponse>({ status: 'error', message: 'Network error' })));
    }
}
