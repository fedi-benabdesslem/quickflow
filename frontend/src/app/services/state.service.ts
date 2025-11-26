import { Injectable, signal } from '@angular/core';
import { ReviewData } from '../types';

@Injectable({
    providedIn: 'root'
})
export class StateService {
    private reviewDataSignal = signal<ReviewData | null>(null);

    readonly reviewData = this.reviewDataSignal.asReadonly();

    setReviewData(data: ReviewData | null) {
        this.reviewDataSignal.set(data);
    }
}
