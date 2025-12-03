import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { AuthService } from '../../services/auth.service';
import { StateService } from '../../services/state.service';
import { ReviewData } from '../../types';

@Component({
  selector: 'app-email',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './email.html',
  styleUrls: ['./email.css']
})
export class EmailComponent implements OnInit {
  recipients = '';
  content = '';
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
    const reviewData = this.state.reviewData();
    if (reviewData && reviewData.type === 'email') {
      this.recipients = reviewData.recipients || '';
      this.content = reviewData.details || '';
    }
  }

  logout() {
    this.auth.logout();
    this.router.navigate(['/login']);
  }

  goBack() {
    this.router.navigate(['/home']);
  }

  isValidEmail(email: string): boolean {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
  }

  async handleSubmit() {
    if (!this.recipients.trim() || !this.content.trim()) {
      this.error.set('Please fill in all fields');
      this.success.set('');
      return;
    }

    const emailList = this.recipients.split(',').map(email => email.trim());
    for (const email of emailList) {
      if (!this.isValidEmail(email)) {
        this.error.set(`Invalid email: ${email}`);
        this.success.set('');
        return;
      }
    }

    const token = this.auth.token();
    if (!token) {
      this.error.set('Authentication required');
      return;
    }

    this.error.set('');
    this.success.set('');
    this.loading.set(true);

    this.api.sendEmail(token, this.recipients, this.content, this.auth.user()?.id).subscribe({
      next: (data) => {
        if (data.status === 'success') {
          this.success.set('Email generated! Redirecting to review...');
          setTimeout(() => {
            const reviewData: ReviewData = {
              type: 'email',
              id: data.emailId || '',
              subject: data.subject || 'Draft Email',
              content: data.generatedContent || '',
              recipients: this.recipients,
              details: this.content
            };
            this.state.setReviewData(reviewData);
            this.router.navigate(['/review']);

            this.recipients = '';
            this.content = '';
          }, 1500);
        } else {
          this.error.set(data.message || 'Failed to generate email');
        }
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Email error:', err);
        this.error.set('Network error. Try again.');
        this.loading.set(false);
      }
    });
  }
}
