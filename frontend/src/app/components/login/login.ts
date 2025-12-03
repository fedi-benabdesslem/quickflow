import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './login.html',
  styleUrls: ['./login.css']
})
export class LoginComponent implements OnInit {

  constructor(public authService: AuthService, private router: Router) { }

  ngOnInit() {
    // Immediately redirect if already authenticated (prevents flash)
    if (this.authService.isAuthenticated()) {
      this.router.navigate(['/home'], { replaceUrl: true });
    }
  }

  handleLogin() {
    this.authService.login();
  }
}
