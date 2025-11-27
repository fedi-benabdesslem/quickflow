import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './home.html',
  styleUrls: ['./home.css']
})
export class HomeComponent {
  constructor(
    public auth: AuthService,
    private router: Router
  ) { }

  logout() {
    this.auth.logout();
    this.router.navigate(['/login']);
  }

  editProfile() {
    this.auth.editProfile();
  }

  navigateTo(path: string) {
    this.router.navigate([path]);
  }
}
