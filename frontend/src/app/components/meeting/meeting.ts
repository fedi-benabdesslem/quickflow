import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { AuthService } from '../../services/auth.service';
import { StateService } from '../../services/state.service';
import { MeetingFormData, ReviewData } from '../../types';

@Component({
  selector: 'app-meeting',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './meeting.html',
  styleUrls: ['./meeting.css']
})
export class MeetingComponent implements OnInit {
  formData: MeetingFormData = {
    people: '',
    location: '',
    timeBegin: '',
    timeEnd: '',
    date: '',
    subject: '',
    details: ''
  } as any; // Type assertion to allow string for people initially

  error = signal<string>('');
  loading = signal<boolean>(false);

  constructor(
    private api: ApiService,
    private auth: AuthService,
    private state: StateService,
    private router: Router
  ) { }

  ngOnInit() {
    const reviewData = this.state.reviewData();
    if (reviewData && reviewData.type === 'pv') {
      this.formData = {
        people: reviewData.people || '',
        location: reviewData.location || '',
        timeBegin: reviewData.timeBegin || '',
        timeEnd: reviewData.timeEnd || '',
        date: reviewData.date || new Date().toISOString().split('T')[0],
        subject: reviewData.subject || '',
        details: reviewData.details || ''
      } as any;
    } else {
      const today = new Date().toISOString().split('T')[0];
      this.formData.date = today;
    }
  }

  logout() {
    this.auth.logout();
    this.router.navigate(['/login']);
  }

  goBack() {
    this.router.navigate(['/home']);
  }

  async handleSubmit() {
    const { people, location, timeBegin, timeEnd, date, subject } = this.formData as any;

    if (!people || !location || !timeBegin || !timeEnd || !date || !subject) {
      this.error.set('Please fill in all fields');
      return;
    }

    if (new Date(`2000-01-01T${timeEnd}`) <= new Date(`2000-01-01T${timeBegin}`)) {
      this.error.set('End time must be after start time');
      return;
    }

    this.error.set('');
    this.loading.set(true);

    const token = this.auth.token();
    if (!token) {
      this.error.set('Authentication required');
      this.loading.set(false);
      return;
    }

    const requestData: MeetingFormData = {
      people: (people as string).split(',').map(p => p.trim()),
      location,
      timeBegin,
      timeEnd,
      date,
      subject,
      details: this.formData.details
    };

    this.api.generateMeeting(token, requestData).subscribe({
      next: (data) => {
        if (data.status === 'success') {
          const reviewData: ReviewData = {
            type: 'pv',
            id: data.id || data.meetingId || '',
            subject: data.subject || subject,
            content: data.generatedContent || '',
            people: people,
            location: location,
            date: date,
            timeBegin: timeBegin,
            timeEnd: timeEnd,
            details: this.formData.details
          };
          this.state.setReviewData(reviewData);
          this.router.navigate(['/review']);

          // Reset form (optional, but good practice if we navigate back without data)
          this.formData = {
            people: '',
            location: '',
            timeBegin: '',
            timeEnd: '',
            date: new Date().toISOString().split('T')[0],
            subject: '',
            details: ''
          } as any;
        } else {
          this.error.set(data.message || 'Failed to generate summary');
        }
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Meeting error:', err);
        this.error.set('Network error. Try again.');
        this.loading.set(false);
      }
    });
  }
}
