import { Component } from '@angular/core';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { Router } from '@angular/router';
import { TokenService } from '../services/token/token.service';

@Component({
  selector: 'app-session-expired-dialog',
  standalone: true,
  imports: [MatDialogModule, MatButtonModule],
  template: `
    <h2 mat-dialog-title>Session Expired</h2>
    <mat-dialog-content>
      <p>Your session has expired. Please log in again to continue</p>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-raised-button class="button-allset" (click)="onLogin()">
        Login
      </button>
    </mat-dialog-actions>
  `,
})
export class SessionExpiredDialogComponent {
  constructor(
    private dialogRef: MatDialogRef<SessionExpiredDialogComponent>,
    private router: Router,
    private tokenservice : TokenService
  ) {}

  onLogin() {
    this.dialogRef.close();
    this.tokenservice.clearToken();
    this.router.navigate(['/authentication/login']);
  }
}