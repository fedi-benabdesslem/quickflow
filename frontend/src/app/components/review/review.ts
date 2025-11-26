import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { AuthService } from '../../services/auth.service';
import { StateService } from '../../services/state.service';
import { ReviewData } from '../../types';

@Component({
  selector: 'app-review',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './review.html',
  styleUrls: ['./review.css']
})
export class ReviewComponent implements OnInit {
  reviewData: ReviewData | null = null;

  subject = '';
  content = '';
  recipients = '';
  people = '';
  location = '';
  date = '';

  error = signal<string>('');
  success = signal<string>('');
  loading = signal<boolean>(false);

  constructor(
    private api: ApiService,
    private auth: AuthService,
    private state: StateService,
    private router: Router
  ) { }

  ngOnInit() {
    this.reviewData = this.state.reviewData();
    if (!this.reviewData) {
      this.router.navigate(['/home']);
      return;
    }

    this.subject = this.reviewData.subject || '';
    this.content = this.reviewData.content || '';

    if (this.reviewData.type === 'email') {
      this.recipients = this.reviewData.recipients || '';
    } else {
      this.people = this.reviewData.people || '';
      this.location = this.reviewData.location || '';
      this.date = this.reviewData.date || '';
    }
  }

  logout() {
    this.auth.logout();
    this.router.navigate(['/login']);
  }

  goBack() {
    this.router.navigate(['/home']);
    this.clearForm();
  }

  clearForm() {
    this.subject = '';
    this.content = '';
    this.recipients = '';
    this.people = '';
    this.location = '';
    this.date = '';
    this.error.set('');
    this.success.set('');
    this.state.setReviewData(null);
  }

  handleEdit() {
    if (this.reviewData) {
      if (this.reviewData.type === 'email') {
        this.router.navigate(['/email']);
      } else {
        this.router.navigate(['/meeting']);
      }
    }
  }

  async handleSend() {
    if (!this.content.trim() || !this.subject.trim()) {
      this.error.set('Subject and content required');
      return;
    }

    const token = this.auth.token();
    if (!token || !this.reviewData) {
      this.error.set('Authentication required');
      return;
    }

    this.error.set('');
    this.success.set('');
    this.loading.set(true);

    const requestData: Record<string, unknown> = {
      id: this.reviewData.id,
      subject: this.subject,
      content: this.content,
    };

    if (this.reviewData.type === 'email') {
      requestData['recipients'] = this.recipients.split(',').map(r => r.trim());
    } else {
      requestData['people'] = this.people.split(',').map(p => p.trim());
      requestData['location'] = this.location;
      requestData['date'] = this.date;
    }

    this.api.sendFinal(token, this.reviewData.type, requestData).subscribe({
      next: (data) => {
        if (data.status === 'success') {
          this.success.set(data.message || 'Sent successfully!');
          setTimeout(() => {
            this.router.navigate(['/home']);
            this.clearForm();
          }, 2000);
        } else {
          this.error.set(data.message || 'Failed to send');
        }
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Send error:', err);
        this.error.set('Network error. Try again.');
        this.loading.set(false);
      }
    });
  }
}
