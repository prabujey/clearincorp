


import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs/operators';
import { MatDialogRef } from '@angular/material/dialog';

import {
  ForumService,
  TitleDto,
  ForumTopicDto,
  TitleCreateRequest
} from 'src/app/services/forum.service';

@Component({
  selector: 'app-create-topic-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="modal-content">
      <div class="modal-header">
        <h2 class="modal-title">Create New Topic</h2>
        <button class="close-btn" type="button" (click)="onClose()" [disabled]="isSaving">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="18" y1="6" x2="6" y2="18"></line>
            <line x1="6" y1="6" x2="18" y2="18"></line>
          </svg>
        </button>
      </div>

      <div class="modal-body">
        <div *ngIf="errorMsg" class="error-box">
          {{ errorMsg }}
        </div>

        <div class="form-group">
          <label class="form-label">Category</label>

          <select class="form-select" [(ngModel)]="selectedTopicId">
            <option [ngValue]="null" disabled>Select a category</option>

            <ng-container *ngIf="topics?.length; else fallbackTopics">
              <option *ngFor="let t of topics" [ngValue]="t.topicId">
                {{ t.topicName }}
              </option>
            </ng-container>

            <ng-template #fallbackTopics>
              <option *ngFor="let t of fallbackTopicNames; let i = index" [ngValue]="fallbackTopicIds[i]">
                {{ t }}
              </option>
            </ng-template>
          </select>

          <div class="hint" *ngIf="!topics?.length">
            (Using fallback categories. Prefer loading from backend /forum/topics)
          </div>
        </div>

        <div class="form-group">
          <label class="form-label">Title</label>
          <input
            type="text"
            class="form-input"
            placeholder="What's your question or topic?"
            [(ngModel)]="title"
            maxlength="100"
          />
          <div class="char-count">{{ title.length }}/100</div>
        </div>

        <div class="form-group">
          <label class="form-label">Description</label>
          <textarea
            class="form-textarea"
            placeholder="Provide details about your topic..."
            rows="6"
            [(ngModel)]="descriptionMd"
            maxlength="200"
          ></textarea>
          <div class="char-count">{{ descriptionMd.length }}/200</div>
        
        </div>

        <div class="tips-section">
          <div class="tips-header">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"></circle>
              <path d="M12 16v-4"></path>
              <path d="M12 8h.01"></path>
            </svg>
            Tips for a great topic
          </div>
          <ul class="tips-list">
            <li>Be clear and specific in your title</li>
            <li>Provide enough context in the description</li>
            <li>Choose the most relevant category</li>
            <li>Be respectful and constructive</li>
          </ul>
        </div>
      </div>

      <div class="modal-footer">
        <button class="btn-secondary" type="button" (click)="onClose()" [disabled]="isSaving">Cancel</button>
        <button
          class="btn-primary"
          type="button"
          [disabled]="!isValid() || isSaving"
          (click)="onCreate()"
        >
          {{ isSaving ? 'Creating...' : 'Create Topic' }}
        </button>
      </div>
    </div>
  `,
  styles: [`
    :host { display: block; }

    .modal-content {
      background: white;
      width: 100%;
      max-width: 720px;
      display: flex;
      flex-direction: column;
    }

    .modal-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 1.5rem;
      border-bottom: 1px solid #e5e7eb;
    }

    .modal-title {
      font-size: 1.25rem;
      font-weight: 700;
      color: #1f2937;
      margin: 0;
    }

    .close-btn {
      padding: 0.25rem;
      border: none;
      background: transparent;
      color: #9ca3af;
      cursor: pointer;
      border-radius: 0.375rem;
      transition: all 0.2s;
    }
    .close-btn:hover { background: #f3f4f6; color: #1f2937; }
    .close-btn:disabled { opacity: .5; cursor: not-allowed; }

    .modal-body { padding: 1.5rem; overflow-y: auto; max-height: 70vh; }

    .error-box{
      background:#fef2f2;
      border:1px solid #fecaca;
      color:#991b1b;
      padding: 0.75rem 0.9rem;
      border-radius: 0.5rem;
      font-size: 0.9rem;
      margin-bottom: 1rem;
    }

    .form-group { margin-bottom: 1.5rem; }
    .form-label {
      display: block;
      font-weight: 600;
      color: #1f2937;
      margin-bottom: 0.5rem;
      font-size: 0.9375rem;
    }

    .form-select, .form-input, .form-textarea {
      width: 100%;
      padding: 0.75rem;
      border: 1px solid #e5e7eb;
      border-radius: 0.5rem;
      font-size: 0.9375rem;
      font-family: inherit;
      transition: all 0.2s;
    }

    .form-select:focus, .form-input:focus, .form-textarea:focus {
      outline: none;
      border-color: #2563eb;
      box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
    }

    .form-textarea { resize: vertical; line-height: 1.5; }

    .char-count {
      text-align: right;
      font-size: 0.8125rem;
      color: #9ca3af;
      margin-top: 0.25rem;
    }

    .hint{
      font-size: 0.8rem;
      color: #6b7280;
      margin-top: .4rem;
    }

    .tips-section {
      background: #f9fafb;
      border: 1px solid #e5e7eb;
      border-radius: 0.5rem;
      padding: 1rem;
      margin-top: 1.5rem;
    }

    .tips-header {
      display: flex;
      gap: 0.5rem;
      align-items: center;
      font-weight: 700;
      color: #111827;
      margin-bottom: 0.5rem;
    }

    .tips-list {
      margin: 0;
      padding-left: 1.25rem;
      color: #374151;
      font-size: 0.9rem;
      line-height: 1.6;
    }

    .modal-footer {
      padding: 1.25rem 1.5rem;
      border-top: 1px solid #e5e7eb;
      display: flex;
      justify-content: flex-end;
      gap: 0.75rem;
    }

    .btn-secondary {
      padding: 0.65rem 1.1rem;
      border-radius: 0.6rem;
      border: 1px solid #e5e7eb;
      background: #fff;
      font-weight: 700;
      cursor: pointer;
    }

    .btn-primary {
      padding: 0.65rem 1.1rem;
      border-radius: 0.6rem;
      border: none;
      background: #BF0A30 ;
      color: #fff;
      font-weight: 800;
      cursor: pointer;
    }

    .btn-primary:disabled, .btn-secondary:disabled {
      opacity: .55;
      cursor: not-allowed;
    }
  `]
})
export class CreateTopicModalComponent implements OnInit {
  private readonly forum = inject(ForumService);
  private readonly dialogRef = inject(MatDialogRef<CreateTopicModalComponent, TitleDto | null>);

  topics: ForumTopicDto[] = [];
  fallbackTopicNames = ['General', 'Tax & Legal', 'Success Stories'];
  fallbackTopicIds = [1, 2, 3];

  selectedTopicId: number | null = null;
  title = '';
  descriptionMd = '';

  isSaving = false;
  errorMsg = '';

  ngOnInit(): void {
    // load categories if you have API; else fallback will show
    this.forum.getAllTopicIdsAndNames?.().subscribe({
      next: (ts) => (this.topics = ts || []),
      error: () => (this.topics = [])
    });
  }

  isValid(): boolean {
    return !!this.selectedTopicId && this.title.trim().length > 0;
  }

  onClose(): void {
    if (this.isSaving) return;
    this.dialogRef.close(null);
  }

  onCreate(): void {
    if (!this.isValid()) return;

    this.isSaving = true;
    this.errorMsg = '';

    const req: TitleCreateRequest = {
      topicId: Number(this.selectedTopicId),
      title: this.title.trim(),
      descriptionMd: (this.descriptionMd || '').trim(),
    };

    this.forum.createTitle(req).pipe(
      finalize(() => (this.isSaving = false))
    ).subscribe({
      next: (created: TitleDto) => this.dialogRef.close(created),
      error: (err) => {
        this.errorMsg = err?.error?.message || 'Failed to create topic. Please try again.';
      }
    });
  }
}
