

// conversational_forum/forum-header/forum-header.component.ts (Corrected)
import { Component, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ForumService } from 'src/app/services/forum.service';
import { Observable, Subscription, of, map } from 'rxjs';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ForumUiEventsService } from '../forum-ui-events.service';
import { Notification } from 'src/app/models/forum.model'; 

@Component({
  selector: 'app-forum-header',
  standalone: true,
  imports: [CommonModule, MatIconModule, MatTooltipModule],
  template: `
    <header class="forum-header">
      <div class="header-content">
        <div class="logo-section">
          <h1 class="logo">LLC Community Forum</h1>
        </div>

        <div class="header-actions">
          <!-- <button
            type="button"
            class="btn-icon notification-btn"
            (click)="openNotifications()"
            matTooltip="Notifications"
          >
            <mat-icon>notifications</mat-icon>
            <ng-container *ngIf="(unreadCount$ | async) as count">
              <span class="notification-badge" *ngIf="count > 0">
                {{ count }}
              </span>
            </ng-container>
          </button> -->

          <button type="button" class="btn-primary" (click)="openCreate()">
            <mat-icon>add</mat-icon> <span class="btn-text">New Topic</span>
          </button>
        </div>
      </div>
    </header>
  `,
  styles: [`
    .forum-header { background: white; border-bottom: 1px solid #e5e7eb; position: sticky; top: 0; z-index: 100; }
    .header-content { max-width: 1200px; margin: 0 auto; padding: 1rem 1.5rem; display: flex; justify-content: space-between; align-items: center; gap: 1rem; }
    .logo { font-size: 1.25rem; font-weight: 700; color: #1f2937; margin: 0; }
    .header-actions { display: flex; align-items: center; gap: 0.75rem; }
    .btn-icon { position: relative; padding: 0.5rem; border: none; background: transparent; border-radius: 0.5rem; cursor: pointer; color: #6b7280; transition: all 0.2s; }
    .btn-icon:hover { background: #f3f4f6; color: #1f2937; }
    .notification-badge { position: absolute; top: 0.25rem; right: 0.25rem; background: #ef4444; color: white; font-size: 0.625rem; font-weight: 600; padding: 0.125rem 0.375rem; border-radius: 9999px; min-width: 1.125rem; text-align: center; }
    .btn-primary { display: flex; align-items: center; gap: 0.5rem; padding: 0.625rem 1rem; background: #BF0A30 ; color: white; border: none; border-radius: 0.5rem; font-weight: 500; font-size: 0.875rem; cursor: pointer; transition: background 0.2s; }
    .btn-primary:hover { background: #BF0A30 ; }

    @media (max-width: 768px) {
      .header-content { padding: 0.875rem 1rem; }
      .logo { font-size: 1.125rem; }
    }
    @media (max-width: 640px) {
      .header-content { padding: 0.75rem 1rem; }
      .logo { font-size: 1rem; }
      .btn-text { display: none; }
      .btn-primary { padding: 0.625rem; }
      .header-actions { gap: 0.5rem; }
    }
  `]
})
export class ForumHeaderComponent implements OnDestroy {
  private forumService = inject(ForumService); 
  private ui = inject(ForumUiEventsService);

  unreadCount$: Observable<number>;
  private subscription: Subscription | null = null;

  constructor() {
    const serviceAny = this.forumService as any;

    if (typeof serviceAny.getNotifications === 'function') {
        this.unreadCount$ = serviceAny.getNotifications().pipe(
            map((notifications: Notification[]) => notifications.filter(n => !n.isRead).length)
        );
    } else {
      this.unreadCount$ = of(0);
    }
  }

  ngOnDestroy(): void {
    this.subscription?.unsubscribe();
  }

  openCreate(): void {
    this.ui.requestOpenCreate();
  }

  openNotifications(): void {
    this.ui.requestOpenNotifications();
  }
}