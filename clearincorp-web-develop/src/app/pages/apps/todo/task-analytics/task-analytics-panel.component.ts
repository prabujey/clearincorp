import {
  Component,
  EventEmitter,
  Input,
  OnChanges,
  OnInit,
  Output,
  SimpleChanges,
  signal,
  WritableSignal,
} from "@angular/core";
import { CommonModule, DatePipe } from "@angular/common";
import { MatCardModule } from "@angular/material/card";
import { MatButtonModule } from "@angular/material/button";
import { MatIconModule } from "@angular/material/icon";
import { MatTooltipModule } from "@angular/material/tooltip";
import { MatSnackBar } from "@angular/material/snack-bar";
import { MatPaginatorModule, PageEvent } from "@angular/material/paginator";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import { MatDividerModule } from "@angular/material/divider";

import { TodoService } from "src/app/services/todo/todo.service";
import { Todo, User } from "src/app/models/todo-models/todo.model";
import {
  AssignedTaskAnalyticsDto,
  AssignedTaskStatus,
  TaskStatusCounts,
} from "src/app/models/todo-models/assigned-task.model";
// REMOVED: File slider component is no longer declared here
import { DeleteConfirmationDialogComponent } from "src/app/shared/delete-confirmation-dialog.component";
import { MatDialog, MatDialogModule } from "@angular/material/dialog";
import { FileSliderComponent } from "../file-slider/file-slider.component";
import { Subscription } from "rxjs";
import { Router, ActivatedRoute } from "@angular/router";
import { SnackBarService } from "src/app/shared/snackbar.service";
import { LoadingService } from "src/app/services/loading/loading.service";

@Component({
  selector: "app-task-analytics-panel",
  standalone: true,
  imports: [
    CommonModule,
    DatePipe,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    MatPaginatorModule,
    MatProgressSpinnerModule,
    MatDividerModule,
    MatDialogModule,
    // REMOVED: FileSliderComponent,
  ],
  template: `
    <div class="analytics-panel-container">
      <mat-card class="analytics-card mat-elevation-z2">
        <!-- 
          UPDATED: Panel Header now includes Edit/Delete actions
        -->
        <div class="panel-header">
          <button
            mat-icon-button
            (click)="goToList()"
            matTooltip="Back to Task List"
          >
            <mat-icon>arrow_back</mat-icon>
          </button>
          <h2 class="panel-title">
            <mat-icon>analytics</mat-icon>
            Task Details & Status
          </h2>
          <span class="header-spacer"></span>

          <!-- Owner Actions -->
          <button
            mat-flat-button
            class="header-action-btn button-prev"
            (click)="onAction('delete')"
            matTooltip="Delete Task"
            *ngIf="
              masterTask?.type === 'personal' ||
              (masterTask?.type === 'assigned' &&
                masterTask?.assignedBy === 'You')
            "
          >
            <mat-icon>delete</mat-icon> Delete
          </button>
          <button
            mat-flat-button
            class="header-action-btn button-all"
            (click)="onAction('edit')"
            matTooltip="Edit Task"
            *ngIf="
              !masterTask?.completionStatus &&
              !masterTask?.ignoredStatus &&
              (masterTask?.type === 'personal' ||
                (masterTask?.type === 'assigned' &&
                  masterTask?.assignedBy === 'You'))
            "
          >
            <mat-icon>edit</mat-icon> Edit&nbsp;
          </button>
        </div>

        <!-- 
          NEW: Master Task Details Section 
        -->
        <div class="master-task-details" *ngIf="masterTask">
          <div class="task-info-section">
            <h3 class="task-message">{{ masterTask.message }}</h3>
            <p class="task-description">
              {{ masterTask.description || "No description provided." }}
            </p>

            <div class="info-badges">
              <span class="info-badge">
                <mat-icon>event</mat-icon>
                Due Date :
                {{
                  ((masterTask.targetDate | date : "MMM d, y") || "")
                    .replace(" ", "")
                    .replace(", ", ",") | uppercase
                }}
              </span>
              <span
                class="info-badge"
                [class]="'priority-' + masterTask.priorityLevel"
              >
                <mat-icon>priority_high</mat-icon>
                Priority: {{ masterTask.priorityLevel | titlecase }}
              </span>
              <!-- <span class="info-badge" *ngIf="masterTask.type === 'assigned'">
                <mat-icon>assignment_ind</mat-icon>
                Assigned To: {{ masterTask.assignedTo }}
              </span> -->
            </div>
          </div>

          <!-- 
              UPDATED: Attachments section now uses file slider button
              FIXED: Changed isLoadingAttachments to isLoadingAttachments()
            -->
          <div *ngIf="masterTask.type === 'assigned'">
            <div *ngIf="isLoadingAttachments()" class="loading-spinner">
              <mat-spinner diameter="30"></mat-spinner>
              <span>Loading files...</span>
            </div>

            <div
              *ngIf="
                !isLoadingAttachments() && fetchedAttachmentUrls().length === 0
              "
              class="empty-files"
            >
              No attachments found for this task.
            </div>

            <button
              mat-flat-button
              class="button-allset"
              *ngIf="
                !isLoadingAttachments() && fetchedAttachmentUrls().length > 0
              "
              (click)="openSlider(0)"
            >
              <mat-icon>attachment</mat-icon>
              View Attachments
            </button>
          </div>
        </div>
        <!-- End of Master Task Details -->

        <mat-card-content
          class="analytics-content"
          *ngIf="masterTask?.type === 'assigned'"
        >
          <!-- Loading Spinner for Analytics -->

          <!-- Analytics Content Ready -->

          <mat-divider></mat-divider>
          <h3 class="analytics-section-title">Assignee Status</h3>
          <!-- Metric Cards Section -->
          <div class="metrics-grid">
            <!-- Total -->
            <div
              class="metric-card bg-total"
              [class.active-filter]="activeStatusFilter === null"
              (click)="applyStatusFilter(null)"
            >
              <mat-icon class="metric-icon">list_alt</mat-icon>
              <div>
                <div class="metric-value">
                  {{ totalassignees }}
                </div>
                <div class="metric-label">Total Assignees</div>
              </div>
            </div>

            <!-- Pending -->
            <div
              class="metric-card bg-pending"
              [class.active-filter]="
                activeStatusFilter === AssignedTaskStatus.PENDING
              "
              (click)="applyStatusFilter(AssignedTaskStatus.PENDING)"
            >
              <mat-icon class="metric-icon">schedule</mat-icon>
              <div>
                <div class="metric-value">
                  {{ statusCounts.pending }}
                </div>
                <div class="metric-label">Pending</div>
              </div>
            </div>

            <!-- Completed -->
            <div
              class="metric-card bg-completed"
              [class.active-filter]="
                activeStatusFilter === AssignedTaskStatus.DONE
              "
              (click)="applyStatusFilter(AssignedTaskStatus.DONE)"
            >
              <mat-icon class="metric-icon">check_circle</mat-icon>
              <div>
                <div class="metric-value">
                  {{ statusCounts.done }}
                </div>
                <div class="metric-label">Completed</div>
              </div>
            </div>

            <!-- Ignored -->
            <div
              class="metric-card bg-ignored"
              [class.active-filter]="
                activeStatusFilter === AssignedTaskStatus.IGNORED
              "
              (click)="applyStatusFilter(AssignedTaskStatus.IGNORED)"
            >
              <mat-icon class="metric-icon">block</mat-icon>
              <div>
                <div class="metric-value">
                  {{ statusCounts.ignored }}
                </div>
                <div class="metric-label">Ignored</div>
              </div>
            </div>
          </div>

          <div *ngIf="isLoading" class="loading-container">
            <mat-spinner diameter="50" style="color:#bf0a30;"></mat-spinner>
            <p>Loading Task Details...</p>
          </div>

          <ng-container *ngIf="!isLoading">
            <div class="divider"></div>

            <!-- Assignee List Display -->
            <div
              class="assignee-list"
              *ngIf="analyticsData.length > 0"
              [class.with-company]="showCompanyColumn"
            >
              <div class="assignee-list-header">
                <span class="header-status-icon"></span>
                <span class="header-assignee">ASSIGNEE</span>
                <span
                  class="header-company"
                  *ngIf="currentUser.role === 'SuperFiler'"
                  >COMPANY / STATE</span
                >
                <span class="header-notes">ACTION NOTES</span>
                <span class="header-files">FILES</span>
                <span class="header-updated">LAST UPDATED</span>
                <!-- <span class="header-actions">ACTIONS</span> -->
              </div>

              <div
                *ngFor="let item of analyticsData"
                class="assignee-item-row"
                [class.completed]="item.status === 'DONE'"
                [class.ignored]="item.status === 'IGNORED'"
              >
                <!-- Status Icon -->
                <div class="assignee-status-icon-cell">
                  <mat-icon
                    *ngIf="item.status === 'DONE'"
                    class="status-tick"
                    matTooltip="Completed"
                    >check_circle</mat-icon
                  >
                  <mat-icon
                    *ngIf="item.status === 'IGNORED'"
                    class="status-ignored"
                    matTooltip="Ignored"
                    >block</mat-icon
                  >
                  <mat-icon
                    *ngIf="item.status === 'PENDING'"
                    class="status-pending"
                    matTooltip="Pending"
                    >schedule</mat-icon
                  >
                </div>

                <!-- Assignee Cell -->
                <div class="assignee-info-cell">
                  <div class="assignee-name">
                    {{ item.assigneeFirstName }} {{ item.assigneeLastName }}
                  </div>
                  <div class="assignee-email">
                    {{ item.assigneeEmail }}
                  </div>
                </div>

                <!-- Company Cell -->
                <div
                  class="assignee-company-cell"
                  *ngIf="currentUser.role === 'SuperFiler'"
                >
                  <div class="company-name">
                    {{ item.companyName || "N/A" }}
                  </div>
                  <div class="company-state">
                    {{ item.state || "N/A" }}
                  </div>
                </div>
                <div class="assignee-notes-cell">
                  <span
                    class="notes-preview"
                    *ngIf="item.notes; else noNotesTpl"
                    [matTooltip]="item.notes"
                    matTooltipPosition="above"
                  >
                    {{ item.notes }}
                  </span>

                  <ng-template #noNotesTpl>
                    <span class="notes-empty">—</span>
                  </ng-template>
                </div>

                <div class="assignee-files-cell">
                  <ng-container
                    *ngIf="
                      (item.attachmentKeys.length || 0) > 0;
                      else noFilesTpl
                    "
                  >
                    <button
                      mat-icon-button
                      class="files-btn"
                      [disabled]="isAssigneeFilesLoading(item.taskId)"
                      (click)="openAssigneeFiles(item)"
                      [matTooltip]="
                        'View ' + item.attachmentKeys.length + ' attachment(s)'
                      "
                    >
                      <mat-icon>attach_file</mat-icon>

                      <!-- small count badge -->
                      <span class="files-count">{{
                        item.attachmentKeys.length
                      }}</span>

                      <!-- tiny spinner while loading -->
                      <span
                        class="files-loading-dot"
                        *ngIf="isAssigneeFilesLoading(item.taskId)"
                      ></span>
                    </button>
                  </ng-container>

                  <ng-template #noFilesTpl>
                    <span class="files-empty">—</span>
                  </ng-template>
                </div>

                <!-- Last Updated Cell -->
                <div class="assignee-date-cell">
                  {{ item.updatedOn | date : "MMM d, y, h:mm a" }}
                </div>

                <!-- Per-item actions - Disabled as backend does not support it
                <div class="assignee-actions-cell">
                  <button mat-icon-button color="warn" matTooltip="Delete this item">
                    <mat-icon>delete_outline</mat-icon>
                  </button>
                </div>
                -->
              </div>
            </div>

            <mat-paginator
              *ngIf="totalTasks > 0"
              [length]="totalTasks"
              [pageSize]="pageSize"
              [pageSizeOptions]="pageSizeOptions"
              [pageIndex]="pageIndex"
              (page)="onPageChange($event)"
              showFirstLastButtons
              aria-label="Select page of assignees"
            ></mat-paginator>

            <!-- Empty State -->
            <div class="empty-state-list" *ngIf="analyticsData.length === 0">
              <mat-icon class="empty-icon">inbox</mat-icon>
              <h3>No assignees found</h3>
              <p>No tasks match the current status filter.</p>
            </div>
          </ng-container>
        </mat-card-content>

        <!-- Show simple message for personal tasks (no analytics) -->
        <mat-card-content
          class="analytics-content"
          *ngIf="masterTask?.type === 'personal'"
        >
          <div class="empty-state-list">
            <mat-icon class="empty-icon">person</mat-icon>
            <h3>This is a personal task.</h3>
            <p>Analytics and assignee tracking are not applicable.</p>
          </div>
        </mat-card-content>
      </mat-card>
    </div>

    <!-- 
      REMOVED: File Slider component
      It is now hosted in the root todo.component.ts
    -->
  `,
  styles: [
    `
      @use "src/variables" as *;
      /* Card and Layout */
      .analytics-card {
        border-radius: 12px;
        padding: 0;
      }
      .analytics-content {
        padding: 24px 30px;
      }
      .panel-header {
        display: flex;
        align-items: center;
        padding: 16px 20px;
        border-bottom: 1px solid #e5e7eb;
        background-color: #f9fafb;
      }
      .panel-title {
        font-size: 24px;
        font-weight: 700;
        margin: 0 0 0 10px;
        color: $title-color;
        display: flex;
        align-items: center;
      }
      .panel-title mat-icon {
        margin-right: 8px;

        color: $primary-color; /* Analytics Blue */
      }
      .header-spacer {
        flex: 1 1 auto;
      }
      .header-action-btn {
        margin-left: 10px;
      }

      /* --- Master Task Details (Copied from create-edit) --- */
      .master-task-details {
        padding: 24px 30px;
      }
      .task-info-section {
        padding-bottom: 15px;
        border-bottom: 1px solid #f3f4f6;
        margin-bottom: 15px;
      }
      .task-message {
        font-size: 24px;
        font-weight: 700;
        margin-top: 0;
        margin-bottom: 8px;
        color: $primary-color;
        line-height: 1.5;
      }
      .task-description {
        font-size: 14px;
        color: $description-color;
        margin-bottom: 15px;
        line-height: 1.5;
      }
      .info-badges {
        display: flex;
        flex-wrap: wrap;
        gap: 10px;
      }
      .info-badge {
        display: inline-flex;
        align-items: center;
        font-size: 13px;
        font-weight: 500;
        padding: 4px 8px;
        border-radius: 6px;
        background-color: #e5e7eb;
        color: #374151;
      }
      .info-badge mat-icon {
        font-size: 16px;
        height: 16px;
        width: 16px;
        margin-right: 4px;
      }
      .attachments-section {
        margin-top: 20px;
      }
      .loading-spinner {
        display: flex;
        align-items: center;
        gap: 10px;
        color: #6b7280;
      }
      .empty-files {
        color: #6b7280;
        font-style: italic;
      }

      .form-section {
        border: 1px solid #e5e7eb;
        border-radius: 8px;
        padding: 15px;
      }
      .section-title {
        font-size: 16px;
        font-weight: 600;
        color: #374151;
        margin-top: 0;
        margin-bottom: 15px;
        padding-bottom: 8px;
        border-bottom: 1px solid #f3f4f6;
      }

      /* --- Analytics Section --- */
      .analytics-section-title {
        font-size: 18px;
        font-weight: 600;
        color: #111827;
        margin-top: 20px;
        margin-bottom: 16px;
      }
      .loading-container {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        padding: 60px 20px;
        font-weight: 500;
        color: #6b7280;
      }
      .loading-container p {
        margin-top: 16px;
        font-size: 16px;
      }

      /* Metric Cards */
      .metrics-grid {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(90px, 1fr));
        gap: 16px;
        margin-bottom: 30px;
      }
      .metric-card {
        display: flex;
        align-items: center;
        padding: 20px 10px;
        border-radius: 12px;
        color: $title-color;
        gap: 16px;
        cursor: pointer;
        transition: transform 0.2s, box-shadow 0.2s;
      }
      .metric-card:hover {
        transform: translateY(-3px) scale(1.02);
        box-shadow: 0 0 0 2px rgba(0, 0, 0, 0.25),
          0 0 18px 4px rgba(0, 0, 0, 0.35);
      }
      .metric-card.active-filter {
        transform: translateY(-2px);
        box-shadow: 0 10px 24px rgba(0, 0, 0, 0.18),
          /* soft lift */ 0 0 0 2px rgba(236, 50, 82, 0.85),
          /* crisp brand ring */ 0 0 22px 6px rgba(236, 50, 82, 0.45); /* subtle pink glow */
      }
      .metric-card.active-filter:hover {
        transform: translateY(-3px) scale(1.03);
        box-shadow: 0 14px 32px rgba(0, 0, 0, 0.22),
          /* deeper shadow */ 0 0 0 2px rgba(236, 50, 82, 0.95),
          /* stronger ring */ 0 0 28px 10px rgba(236, 50, 82, 0.55); /* stronger glow */
      }
      .metric-icon {
        font-size: 40px;
        height: 40px;
        width: 40px;
        padding: 0 30px 0 0;
      }
      .metric-value {
        font-size: 28px;
        font-weight: 700;
        line-height: 1;
      }
      .metric-label {
        font-size: 14px;
        opacity: 0.8;
      }
      .bg-total {
        background: linear-gradient(135deg, #ebf3fb, #ebf3fb);
      }
      .bg-pending {
        background: linear-gradient(135deg, #ebf3fb, #ebf3fb);
      }
      .bg-completed {
        background: linear-gradient(135deg, #ebf3fb, #ebf3fb);
      }
      .bg-ignored {
        background: linear-gradient(135deg, #ebf3fb, #ebf3fb);
      }

      .divider {
        height: 1px;
        background: #f3f4f6;
        margin: 20px 0;
      }

      /* --- Assignee List Design --- */
      .assignee-list {
        display: flex;
        flex-direction: column;
        gap: 8px;
      }
      .assignee-list-header,
      .assignee-item-row {
        display: grid;
        grid-template-columns:
          44px /* status */
          minmax(220px, 1.6fr) /* assignee */
          minmax(240px, 1.4fr) /* notes */
          110px /* files */
          minmax(180px, 1fr); /* updated */
        align-items: center;
        gap: 12px;
      }
      .assignee-list.with-company .assignee-list-header,
      .assignee-list.with-company .assignee-item-row {
        grid-template-columns:
          44px /* status */
          minmax(220px, 1.4fr) /* assignee */
          minmax(220px, 1.2fr) /* company/state */
          minmax(240px, 1.4fr) /* notes */
          110px /* files */
          minmax(180px, 1fr); /* updated */
      }
      .assignee-list-header {
        padding: 10px 20px;
        color: #6b7280;
        font-size: 12px;
        font-weight: 600;
        text-transform: uppercase;
        border-bottom: 1px solid #e5e7eb;
        margin-bottom: 8px;
      }
      .assignee-item-row {
        align-items: center;
        padding: 12px 20px;
        background: #fff;
        border-radius: 8px;
        border: 1px solid #e5e7eb;
      }
      .assignee-item-row.completed {
        background-color: #f0fdf4; /* Light green */
      }
      .assignee-item-row.ignored {
        background-color: #fffbeb; /* Light yellow */
      }
      .header-updated,
      .assignee-date-cell {
        justify-self: center;
      }

      /* keep headers in one line */
      .assignee-list-header > span {
        white-space: nowrap;
      }

      .assignee-status-icon-cell {
        display: flex;
        align-items: center;
        justify-content: center;
      }
      .status-tick {
        color: #10b981;
        font-size: 20px;
      }
      .status-ignored {
        color: #f59e0b;
        font-size: 20px;
      }
      .status-pending {
        color: #3b82f6;
        font-size: 20px;
      }

      .assignee-info-cell {
        display: flex;
        flex-direction: column;
        align-items: flex-start;
      }
      .assignee-name {
        font-weight: 600;
        font-size: 15px;
        color: #1f2937;
      }
      .assignee-email {
        font-size: 13px;
        color: #6b7280;
      }

      .assignee-company-cell {
        font-size: 14px;
        color: #374151;
      }
      .company-name {
        font-weight: 500;
      }
      .company-state {
        font-size: 13px;
        color: #6b7280;
      }

      .assignee-date-cell {
        font-size: 14px;
        color: #4b5563;
        font-weight: 500;
      }

      /* Empty State */
      .empty-state-list {
        text-align: center;
        padding: 40px;
        color: #6b7280;
      }
      .empty-icon {
        font-size: 60px;
        height: 60px;
        width: 60px;
        color: #d1d5db;
        margin-bottom: 16px;
      }
      .priority-high {
        background-color: #fee2e2;
        color: #ef4444;
      }
      .priority-medium {
        background-color: #fef3c7;
        color: #f59e0b;
      }
      .priority-low {
        background-color: #d1fae5;
        color: #10b981;
      }
      .assignee-notes-cell .notes-preview {
        display: -webkit-box;
        -webkit-line-clamp: 2;
        -webkit-box-orient: vertical;
        overflow: hidden;
        font-size: 13px;
        color: #374151;
        line-height: 1.35;
      }
      .notes-empty {
        color: #9ca3af;
      }

      /* Files button styling */
      .assignee-files-cell {
        display: flex;
        align-items: center;
        justify-content: flex-start;
      }

      .files-btn {
        position: relative;
      }
      .files-count {
        position: absolute;
        top: -2px;
        right: -2px;
        font-size: 11px;
        font-weight: 700;
        background: #111827;
        color: #fff;
        border-radius: 999px;
        padding: 1px 6px;
      }

      .files-loading-dot {
        position: absolute;
        bottom: -2px;
        right: -2px;
        width: 8px;
        height: 8px;
        border-radius: 50%;
        background: #3b82f6;
      }

      .files-empty {
        color: #9ca3af;
      }
      @media (max-width: 768px) {
        .assignee-list-header,
        .assignee-item-row {
          grid-template-columns: 44px 1.5fr 1fr 110px; /* remove updated column */
        }

        .assignee-date-cell {
          display: none;
        }
      }
      @media (max-width: 900px) {
        .header-company,
        .assignee-company-cell,
        .header-updated,
        .assignee-date-cell {
          display: none;
        }

        .assignee-list-header,
        .assignee-item-row {
          grid-template-columns: 44px 1.6fr 1.4fr 110px; /* status, assignee, notes, files */
        }
      }
    `,
  ],
})
export class TaskAnalyticsPanelComponent implements OnInit, OnChanges {
  // @Input() masterTask: Todo | undefined;
  // @Output() closePanel = new EventEmitter<void>();
  // @Output() editRequested = new EventEmitter<string>(); // NEW
  // @Output() deleteRequested = new EventEmitter<Todo>(); // NEW
  // // NEW: Emitter to open the global slider
  // @Output() openFileSlider = new EventEmitter<{ files: string[], startIndex: number }>();

  masterTask: Todo | undefined;
  // Enums for template
  AssignedTaskStatus = AssignedTaskStatus;

  analyticsData: AssignedTaskAnalyticsDto[] = [];
  statusCounts: TaskStatusCounts = { pending: 0, done: 0, ignored: 0 };

  totalTasks: number = 0;
  pageSize = 10;
  pageIndex = 0;
  pageSizeOptions: number[] = [5, 10, 25, 100];

  activeStatusFilter: AssignedTaskStatus | null = null;
  isLoading = true;

  // NEW: For master attachments
  isLoadingAttachments = signal(false);
  fetchedAttachmentUrls: WritableSignal<string[]> = signal([]);

  private assigneeFileUrlCache = signal<Record<string, string[]>>({});
  private assigneeFileLoading = signal<Record<string, boolean>>({});

  // REMOVED: Signals for file slider, now handled by parent
  // showSlider = signal(false);
  // sliderStartIndex = signal(0);
  private routeSub: Subscription | undefined;
  currentUser: User;

  constructor(
    private todoService: TodoService,
    private snackBar: SnackBarService,
    private dialog: MatDialog,
    private loadingService: LoadingService,
    private router: Router,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.currentUser = this.todoService.getCurrentUser();
    this.routeSub = this.route.paramMap.subscribe((params) => {
      const id = params.get("id");
      if (id) {
        this.masterTask = this.todoService.getTodoById(id);
        if (!this.masterTask) {
          // this.snackBar.open("Task not found.", "Close", { duration: 3000 });
          // this.goToList();
          this.isLoading = false;
          this.todoService.loadAssignedTasks({} as any, "By Me").subscribe({
            next: () => {
              // On success, try fetching the task again from the now-populated cache
              this.masterTask = this.todoService.getTodoById(id);
              if (this.masterTask) {
                this.loadDataForMasterTask();
                // this.snackBar.open("Task data recovered.", "Close", { duration: 1500 });
              }
            },
            error: (err) =>
              console.error(
                "Failed to load assigned tasks on analytics reload.",
                err
              ),
          });
        } else {
          // Manually trigger the logic that was in ngOnChanges
          this.loadDataForMasterTask();
        }
      }
    });
  }
  ngOnDestroy(): void {
    if (this.routeSub) {
      this.routeSub.unsubscribe();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {}

  loadDataForMasterTask(): void {
    if (!this.masterTask) return;

    this.pageIndex = 0;
    this.activeStatusFilter = null;

    if (this.masterTask.type === "assigned") {
      this.loadAnalytics();
      this.fetchTaskAttachments();
    } else {
      this.isLoading = false;
      this.analyticsData = [];
      this.statusCounts = { pending: 0, done: 0, ignored: 0 };
      this.totalTasks = 0;
    }
  }

  loadAnalytics(): void {
    if (!this.masterTask) return;

    this.isLoading = true;
    const masterId = this.masterTask.masterId || this.masterTask.id;

    this.todoService
      .getTaskAnalytics(
        masterId,
        this.activeStatusFilter,
        this.pageIndex,
        this.pageSize
      )
      .subscribe({
        next: (response) => {
          this.statusCounts = response.statusCounts;
          this.analyticsData = response.tasks.content;
          this.totalTasks = response.tasks.totalElements;
          this.isLoading = false;
        },
        error: (err) => {
          this.isLoading = false;
          this.snackBar.showError("Failed to load task analytics.", "Close");
          console.error("Error loading analytics:", err);
        },
      });
  }

  /**
   * NEW: Fetches master attachments
   */
  fetchTaskAttachments(): void {
    if (!this.masterTask) return;
    const masterId = this.masterTask.masterId || this.masterTask.id;

    this.isLoadingAttachments.set(true);
    this.fetchedAttachmentUrls.set([]);
    this.todoService.getTaskAttachmentUrls(masterId).subscribe({
      next: (urls) => {
        console.log("Fetched File");

        this.fetchedAttachmentUrls.set(urls);
        this.isLoadingAttachments.set(false);
      },
      error: (err) => {
        console.error("Error fetching attachments:", err);
        this.snackBar.showError("Failed to load task attachments.", "Close");
        this.isLoadingAttachments.set(false);
      },
    });
  }

  onPageChange(event: PageEvent): void {
    this.pageSize = event.pageSize;
    this.pageIndex = event.pageIndex;
    this.loadAnalytics();
  }

  applyStatusFilter(status: AssignedTaskStatus | null): void {
    if (this.activeStatusFilter === status) {
      if (status !== null) {
        this.activeStatusFilter = null;
      } else {
        return;
      }
    } else {
      this.activeStatusFilter = status;
    }

    this.pageIndex = 0;
    this.loadAnalytics();
  }

  /**
   * NEW: Handles Edit/Delete actions for the master task
   */
  onAction(action: "edit" | "delete"): void {
    if (!this.masterTask) return;

    if (action === "edit") {
      // Navigate to the edit route
      this.router.navigate(["/apps/todo/edit", this.masterTask.id]);
    } else if (action === "delete") {
      const dialogRef = this.dialog.open(DeleteConfirmationDialogComponent, {
        panelClass: "custom-dialog-panel",
        maxWidth: "auto",
        width: "450px",
        data: {
          taskTitle: this.masterTask.message,
          confirmationText: this.masterTask.message,
        },
        backdropClass: "custom-ticket-dialog-backdrop",
        disableClose: true,
      });
      dialogRef.afterClosed().subscribe((result) => {
        if (result === true) {
          // Only emit if confirmed
          this.deleteRequested(this.masterTask!);
        }
      });
    }
  }

  deleteRequested(task: Todo): void {
    this.loadingService.show("Deleting task...");
    const masterId = task.masterId || task.id;
    this.todoService.deleteTodo(masterId).subscribe({
      next: () => {
        this.snackBar.showSuccess("Task deleted successfully!", "Close");
        this.goToList(); // Navigate back to list
        this.loadingService.hide();
      },
      error: (err) => {
        this.loadingService.hide();
        this.snackBar.showError("Failed to delete task.", "Close");
        console.error("Error deleting task:", err);
      },
    });
  }

  goToList(): void {
    this.router.navigate(["/apps/todo/list/assigned"]);
  }

  /**
   * UPDATED: Method now emits an event to the parent component
   * instead of managing its own state.
   */
  openSlider(index: number) {
    const files = this.fetchedAttachmentUrls();
    const startIndex = Math.max(0, Math.min(index ?? 0, files.length - 1));

    this.dialog.open(FileSliderComponent, {
      data: { files, initialIndex: startIndex, title: "Attachments" },
      width: "auto",
      maxWidth: "auto",
      panelClass: "custom-dialog-panel",
      autoFocus: false,
      restoreFocus: false,
      hasBackdrop: true,
      backdropClass: "custom-ticket-dialog-backdrop",
    });
  }

  /**
   * NEW: Utility for file names
   */
  getFileNameFromUrl(url: string): string {
    try {
      const parsedUrl = new URL(url);
      const pathname = decodeURIComponent(parsedUrl.pathname);
      const parts = pathname.split("/");
      const encodedFilename = parts.pop();

      if (encodedFilename) {
        const finalName = encodedFilename.split("/").pop();
        return finalName || "file";
      }
      return "file";
    } catch (e) {
      return url.substring(url.lastIndexOf("/") + 1).split("?")[0] || "file";
    }
  }

  get totalassignees(): number {
    return (
      (this.statusCounts.done || 0) +
      (this.statusCounts.pending || 0) +
      (this.statusCounts.ignored || 0)
    );
  }
  isAssigneeFilesLoading(taskId: string): boolean {
    return !!this.assigneeFileLoading()[taskId];
  }

  openAssigneeFiles(item: AssignedTaskAnalyticsDto) {
    const taskId = item.taskId;
    const keys = item.attachmentKeys || [];

    if (!taskId || keys.length === 0) return;

    // If already cached, open immediately
    const cached = this.assigneeFileUrlCache()[taskId];
    if (cached?.length) {
      this.dialog.open(FileSliderComponent, {
        data: { files: cached, initialIndex: 0, title: "Assignee Attachments" },
        width: "auto",
        maxWidth: "auto",
        panelClass: "custom-dialog-panel",
        autoFocus: false,
        restoreFocus: false,
        hasBackdrop: true,
        backdropClass: "custom-ticket-dialog-backdrop",
      });
      return;
    }

    // Otherwise fetch URLs from backend using keys
    this.setAssigneeLoading(taskId, true);

    this.todoService.getPresignedUrlsForKeys(taskId).subscribe({
      next: (urls) => {
        this.setAssigneeUrls(taskId, urls || []);
        this.setAssigneeLoading(taskId, false);

        if ((urls || []).length > 0) {
          this.dialog.open(FileSliderComponent, {
            data: {
              files: urls,
              initialIndex: 0,
              title: "Assignee Attachments",
            },
            width: "auto",
            maxWidth: "auto",
            panelClass: "custom-dialog-panel",
            autoFocus: false,
            restoreFocus: false,
            hasBackdrop: true,
            backdropClass: "custom-ticket-dialog-backdrop",
          });
        } else {
          this.snackBar.showError("No attachments available.", "Close");
        }
      },
      error: (err) => {
        console.error("Failed to load assignee attachments:", err);
        this.setAssigneeLoading(taskId, false);
        this.snackBar.showError("Failed to load attachments.", "Close");
      },
    });
  }

  private setAssigneeLoading(taskId: string, loading: boolean) {
    const curr = { ...this.assigneeFileLoading() };
    curr[taskId] = loading;
    this.assigneeFileLoading.set(curr);
  }

  private setAssigneeUrls(taskId: string, urls: string[]) {
    const curr = { ...this.assigneeFileUrlCache() };
    curr[taskId] = urls;
    this.assigneeFileUrlCache.set(curr);
  }
  get showCompanyColumn(): boolean {
    const role = (this.currentUser?.role || "").toLowerCase();
    return role === "superfiler" || role === "admin";
  }
}
