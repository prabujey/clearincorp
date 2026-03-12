
import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { TextFieldModule } from '@angular/cdk/text-field'; // ⬅️ needed for cdkTextareaAutosize
import { Post, User, Reply } from 'src/app/models/forum.model';

export interface ReplyDialogData {
  post: Post;                 // uses loginUserName, content, createdAt, etc.
  replyingTo: Reply | null;   // optional: nested reply target
  currentUser: User | null;   // may be null during first render
}

@Component({
  selector: 'app-reply-dialog',
  standalone: true,
  imports: [
    CommonModule, FormsModule, MatDialogModule, MatButtonModule,
    MatFormFieldModule, MatInputModule, MatIconModule, TextFieldModule
  ],
  template: `
    <div class="reply-dialog-header">
      <h2 mat-dialog-title>Replying to {{ data.post?.loginUserName || 'Post Author' }}</h2>
      <button mat-icon-button (click)="onCancel()" aria-label="Close dialog">
        <mat-icon>close</mat-icon>
      </button>
    </div>

    <mat-dialog-content>
      <div class="post-context">
        <div class="context-header">
          <img [src]="defaultAvatar" [alt]="data.post?.loginUserName || 'User avatar'" class="avatar">
          <span class="author-name">{{ data.post?.loginUserName || 'Unknown user' }}</span>
          <span class="time">{{ getTimeAgo(data.post?.createdAt) }}</span>
        </div>
        <p class="post-content">
          {{ data.post?.content || '—' }}
        </p>
      </div>

      <div class="new-reply-form">
        <img
          [src]="data.currentUser?.avatar || data.currentUser?.avatarUrl || defaultAvatar"
          [alt]="data.currentUser?.name || 'You'"
          class="avatar"
        />
        <mat-form-field appearance="outline" class="reply-input">
          <mat-label>Your reply</mat-label>
          <textarea
            matInput
            [(ngModel)]="replyContent"
            cdkTextareaAutosize
            #autosize="cdkTextareaAutosize"
            cdkAutosizeMinRows="3"
            cdkAutosizeMaxRows="8"
            placeholder="Share your thoughts..."
          ></textarea>
        </mat-form-field>
      </div>
    </mat-dialog-content>

    <mat-dialog-actions align="end">
      <button mat-stroked-button (click)="onCancel()">Cancel</button>
      <button
        mat-flat-button
        color="primary"
        [disabled]="isBlank(replyContent)"
        (click)="onSubmit()"
      >
        Post Reply
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .reply-dialog-header {
      display: flex; justify-content: space-between; align-items: center;
      padding: 0 12px 0 24px; border-bottom: 1px solid #e5e7eb;
    }
    h2[mat-dialog-title] { font-size: 1.25rem; font-weight: 600; margin-bottom: 0; }

    mat-dialog-content { padding: 20px 24px; }

    .post-context {
      background: #f9fafb; border-radius: 8px; padding: 12px; margin-bottom: 16px;
      border: 1px solid #f3f4f6;
    }
    .context-header { display: flex; align-items: center; gap: 10px; }
    .avatar { width: 32px; height: 32px; border-radius: 50%; }
    .author-name { font-weight: 600; }
    .time { font-size: 12px; color: #6b7280; margin-left: auto; }
    .post-content { margin: 8px 0 0; color: #374151; line-height: 1.6; }

    .new-reply-form { display: flex; gap: 12px; align-items: flex-start; }
    .reply-input { flex: 1; }

    mat-dialog-actions { padding: 12px 24px; }
  `]
})
export class ReplyDialogComponent {
  replyContent = '';
  defaultAvatar = 'assets/avatar-0.jpg'; // ensure this path exists

  constructor(
    public dialogRef: MatDialogRef<ReplyDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: ReplyDialogData
  ) {}

  isBlank(v: string | null | undefined): boolean {
    return !v || !v.trim();
  }

  onCancel(): void { this.dialogRef.close(); }

  onSubmit(): void {
    if (!this.isBlank(this.replyContent)) {
      this.dialogRef.close(this.replyContent.trim());
    }
  }

  getTimeAgo(dateInput?: string | Date | null): string {
    if (!dateInput) return '';
    const d = typeof dateInput === 'string' ? new Date(dateInput) : dateInput;
    if (isNaN(d.getTime())) return '';
    const s = Math.floor((Date.now() - d.getTime()) / 1000);
    if (s < 60) return 'just now';
    const m = Math.floor(s / 60); if (m < 60) return `${m}m ago`;
    const h = Math.floor(m / 60); if (h < 24) return `${h}h ago`;
    const days = Math.floor(h / 24); if (days < 7) return `${days}d ago`;
    const w = Math.floor(days / 7); return `${w}w ago`;
  }
}
