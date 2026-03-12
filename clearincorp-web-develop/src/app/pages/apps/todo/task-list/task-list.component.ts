import {
  Component,
  EventEmitter,
  Inject,
  Input,
  OnInit,
  Output,
  OnDestroy,
} from "@angular/core"; // Added OnDestroy
import { CommonModule, DatePipe } from "@angular/common";
import { MatCardModule } from "@angular/material/card";
import { MatButtonModule } from "@angular/material/button";
import { MatIconModule } from "@angular/material/icon";
import { MatTabsModule } from "@angular/material/tabs";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatSelectModule } from "@angular/material/select";
import {
  MatChipListbox,
  MatChipRemove,
  MatChipRow,
  MatChipsModule,
} from "@angular/material/chips";
import {
  MAT_DIALOG_DATA,
  MatDialog,
  MatDialogModule,
  MatDialogRef,
} from "@angular/material/dialog";
import { MatSnackBar, MatSnackBarRef } from "@angular/material/snack-bar";
import { MatTooltipModule } from "@angular/material/tooltip";
import { MatCheckboxModule } from "@angular/material/checkbox";

import { TodoService } from "src/app/services/todo/todo.service";
import { RolePermissionsService } from "src/app/services/todo/role-permissions.service";
import {
  Todo,
  User,
  TaskFilters,
  SortBy,
  TaskPermissions,
  TaskViewData,
} from "src/app/models/todo-models/todo.model";
// UPDATED: Import Router, ActivatedRoute
import { ActivatedRoute, Router } from "@angular/router";
import { MatPaginatorModule, PageEvent } from "@angular/material/paginator";
import { MatInputModule } from "@angular/material/input";
import { MatMenu, MatMenuModule } from "@angular/material/menu";
import { FormsModule } from "@angular/forms";
// REMOVED: PanelMode
import { Observable, Subscription } from "rxjs"; // Kept Observable, Added Subscription
// UPDATED: Import TodoStateService
import { TodoStateService } from "src/app/services/todo/todo-state.service";
import { SnackBarService } from "src/app/shared/snackbar.service";
import { LoadingService } from "src/app/services/loading/loading.service";

@Component({
  selector: "app-task-list",
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatTabsModule,
    MatFormFieldModule,
    MatSelectModule,
    MatChipsModule,
    // MatChipRow,
    MatMenu,
    // MatChipRemove,
    MatDialogModule,
    MatTooltipModule,
    MatCheckboxModule,
    //  StateSelectorComponent,
    // TaskCompletionModalComponent,
    MatPaginatorModule,
    //TaskViewDetailsModalComponent,
    MatSelectModule,
    MatInputModule,
    MatMenuModule,
    MatButtonModule,
  ],
  // Template is unchanged, but click handlers are modified
  template: `
    <div class="task-list-container">
      <mat-card class="list-card mat-elevation-z2">
        <div class="header-section">
          <div class="header-content">
            <h1 class="page-title">To-Do</h1>
            <p class="page-subtitle">
              Create, track, and complete all your tasks in one place.
            </p>
          </div>
        </div>
        <div class="tabs-wrap">
          <div class="custom-tabs" role="tablist" aria-label="Task type">
            <button
              type="button"
              class="tab-button"
              [class.active]="selectedTabIndex === 0"
              (click)="onTabChange(0)"
              role="tab"
            >
              Personal Tasks
            </button>
            <button
              type="button"
              class="tab-button"
              [class.active]="selectedTabIndex === 1"
              (click)="onTabChange(1)"
              role="tab"
            >
              Assigned Tasks
            </button>
            <span class="flex-spacer"></span>
            <!-- Add Task Button UPDATED -->
            <button
              mat-flat-button
              color="primary"
              (click)="goToCreate()"
              matTooltip="Add New Task"
              class="button-all"
              *ngIf="
                (selectedTabIndex === 0 &&
                  (currentUser.role.toLowerCase() === 'consumer' ||
                    currentUser.role.toLowerCase() === 'vendor')) ||
                currentUser.role.toLowerCase() === 'admin' ||
                currentUser.role.toLowerCase() === 'superfiler'
              "
            >
              <mat-icon>add</mat-icon>
              Add New Task &nbsp;
            </button>
          </div>
        </div>

        <mat-card-content class="task-content">
          <!-- Metric Cards Section -->
          <div
            class="metrics-grid"
            *ngIf="
              !(
                (currentUser.role.toLowerCase() === 'superfiler' ||
                  currentUser.role.toLowerCase() === 'admin') &&
                assignedView === 'By Me' &&
                selectedTabIndex === 1
              )
            "
          >
            <div
              class="metric-card bg-total"
              [class.active-filter]="
                activeFilters.status === '' &&
                activeFilters.sortBy === defaultSort
              "
              (click)="applyStatusFilter('')"
            >
              <mat-icon class="metric-icon">list_alt</mat-icon>
              <div>
                <div class="metric-value">
                  {{ selectedTabIndex === 0 ? totalPersonal : totalAssigned }}
                </div>
                <div class="metric-label">
                  Total {{ taskType | titlecase }} Tasks
                </div>
              </div>
            </div>

            <!-- Metric Card: Completed -->
            <div
              class="metric-card bg-completed"
              [class.active-filter]="activeFilters.status === 'completed'"
              (click)="applyStatusFilter('completed')"
            >
              <mat-icon class="metric-icon">check_circle</mat-icon>
              <div>
                <div class="metric-value">
                  {{
                    selectedTabIndex === 0
                      ? statusCountsPersonal.completed
                      : statusCountsAssigned.completed
                  }}
                </div>
                <div class="metric-label">Completed</div>
              </div>
            </div>

            <!-- Metric Card: Pending -->
            <div
              class="metric-card bg-pending"
              [class.active-filter]="activeFilters.status === 'pending'"
              (click)="applyStatusFilter('pending')"
            >
              <mat-icon class="metric-icon">schedule</mat-icon>
              <div>
                <div class="metric-value">
                  {{
                    selectedTabIndex === 0
                      ? statusCountsPersonal.pending
                      : statusCountsAssigned.pending
                  }}
                </div>
                <div class="metric-label">Pending</div>
              </div>
            </div>

            <!-- Metric Card: Overdue -->
            <div
              class="metric-card bg-overdue"
              [class.active-filter]="activeFilters.status === 'overdue'"
              (click)="applyStatusFilter('overdue')"
            >
              <mat-icon class="metric-icon">warning</mat-icon>
              <div>
                <div class="metric-value">
                  {{
                    selectedTabIndex === 0
                      ? statusCountsPersonal.overdue
                      : statusCountsAssigned.overdue
                  }}
                </div>
                <div class="metric-label">Overdue</div>
              </div>
            </div>

            <!-- Metric Card: Ignored -->
            <div
              class="metric-card bg-ignored"
              *ngIf="taskType === 'assigned'"
              [class.active-filter]="activeFilters.status === 'ignored'"
              (click)="applyStatusFilter('ignored')"
            >
              <mat-icon class="metric-icon">block</mat-icon>
              <div>
                <div class="metric-value">
                  {{
                    selectedTabIndex === 0
                      ? statusCountsPersonal.ignored
                      : statusCountsAssigned.ignored
                  }}
                </div>
                <div class="metric-label">Ignored</div>
              </div>
            </div>
          </div>

          <div class="filter-controls-bar">
            <!-- Filter Button (Opens Side Panel) -->
            <div class="filter-controls-left">
              <!-- ADDED: SuperFiler View Toggle -->
              <div
                class="task-type-toggle-group"
                *ngIf="
                  currentUser.role.toLowerCase() === 'superfiler' &&
                  selectedTabIndex === 1
                "
              >
                <button
                  type="button"
                  mat-flat-button
                  [class.active-toggle]="assignedView === 'For Me'"
                  (click)="onTaskViewChange('For Me')"
                  matTooltip="Show tasks assigned to you"
                >
                  Tasks Assigned to Me
                </button>
                <button
                  type="button"
                  mat-flat-button
                  [class.active-toggle]="assignedView === 'By Me'"
                  (click)="onTaskViewChange('By Me')"
                  matTooltip="Show tasks created by you"
                >
                  Tasks I Assigned
                </button>
              </div>
            </div>

            <!-- Right Side: View Toggle & Clear -->
            <div class="filter-controls-right">
              <!--   <button
              mat-stroked-button
              (click)="openFilterPanel()"
              class="action-btn"
            >
              <mat-icon>filter_alt</mat-icon>
              Filters
            </button> -->

              <button
                mat-stroked-button
                [matMenuTriggerFor]="priorityMenu"
                class="action-btn"
              >
                <mat-icon style="color: #bf0a30;">flag</mat-icon>
                Priority:&nbsp;{{ activeFilters.priority || "All" | titlecase }}
              </button>

              <mat-menu #priorityMenu="matMenu">
                <button
                  mat-menu-item
                  (click)="onPriorityChange('')"
                  class="priority-item"
                >
                  <span class="priority-label">All Priorities</span>
                </button>
                <button
                  mat-menu-item
                  (click)="onPriorityChange('high')"
                  class="priority-item"
                >
                  <span class="priority-dot priority-high-dot"></span>
                  <span class="priority-label">&nbsp;High</span>
                </button>
                <button
                  mat-menu-item
                  (click)="onPriorityChange('medium')"
                  class="priority-item"
                >
                  <span class="priority-dot priority-medium-dot"></span>
                  <span class="priority-label">&nbsp;Medium</span>
                </button>
                <button
                  mat-menu-item
                  (click)="onPriorityChange('low')"
                  class="priority-item"
                >
                  <span class="priority-dot priority-low-dot"></span>
                  <span class="priority-label">&nbsp;Low</span>
                </button>
              </mat-menu>

              <!-- Sort Button -->
              <button
                mat-stroked-button
                [matMenuTriggerFor]="sortMenu"
                class="action-btn"
              >
                <mat-icon style="color: #bf0a30;">sort</mat-icon>
                Sort By: &nbsp;{{ activeFilters.sortBy | titlecase }}
              </button>
              <mat-menu #sortMenu="matMenu">
                <button mat-menu-item (click)="onSortChange('latest')">
                  <span>Latest</span>
                </button>
                <button mat-menu-item (click)="onSortChange('oldest')">
                  <span>Oldest</span>
                </button>
                <button
                  mat-menu-item
                  *ngIf="activeFilters.status !== 'overdue'"
                  (click)="onSortChange('dueToday')"
                >
                  <span>Due Today</span>
                </button>
              </mat-menu>

              <button
                mat-button
                class="clear-filters-btn ml-auto"
                (click)="clearFilters()"
                *ngIf="hasActiveFilters()"
              >
                Clear Filters
              </button>
            </div>
          </div>

          <div class="divider"></div>

          <!-- Task List Display -->
          <div
            class="tasks-list"
            *ngIf="
              (selectedTabIndex === 0
                ? personalTasks.length
                : assignedTasks.length) > 0
            "
          >
            <div class="task-list-header">
              <span class="header-status-icon"></span>
              <span class="header-title">Task Title</span>
              <span class="header-due-date">Due Date</span>
              <span class="header-priority">Priority</span>
              <span class="header-actions">Actions</span>
            </div>

            <!-- List of Tasks: Using paginatedTasks -->
            <div
              *ngFor="
                let task of selectedTabIndex === 0
                  ? personalTasks
                  : assignedTasks
              "
              class="task-item-row"
              [class.completed]="task.completionStatus"
              [class.overdue]="
                !task.completionStatus &&
                !task.ignoredStatus &&
                isTaskOverdue(task)
              "
              [class.selected-row]="selectedTask?.id === task.id"
              (click)="onRowClick(task)"
            >
              <!-- Status Icon -->
              <div class="task-status-icon-cell">
                <mat-icon
                  *ngIf="task.completionStatus"
                  class="status-tick"
                  matTooltip="Completed"
                  >check_circle</mat-icon
                >
                <mat-icon
                  *ngIf="task.ignoredStatus"
                  class="status-ignored"
                  matTooltip="Ignored"
                  >block</mat-icon
                >
                <mat-icon
                  *ngIf="!task.completionStatus && !task.ignoredStatus"
                  class="status-pending"
                  matTooltip="Pending"
                  >schedule</mat-icon
                >
              </div>

              <!-- Title Cell -->
              <div class="task-title-cell">
                <div
                  class="task-message-text"
                  [class.completed-text]="task.completionStatus"
                >
                  {{ task.message }}
                </div>
                <!-- Assigned Task Info -->
                <ng-container *ngIf="task.type === 'assigned'">
                  <div class="task-assignment-info" *ngIf="showCompanyColumn">
                    <span class="info-badge">
                      <mat-icon>business</mat-icon>
                      {{ task.companyDisplayName || "-" }}
                    </span>
                  </div>
                </ng-container>
              </div>

              <div class="task-date-cell">
                <span
                  [class.text-overdue]="
                    !task.completionStatus &&
                    !task.ignoredStatus &&
                    isTaskOverdue(task)
                  "
                >
                  {{
                    ((task?.targetDate | date : "MMM&nbsp;d,&nbsp;y") || "")
                      .toUpperCase()
                      .replace(" ", "")
                      .replace(", ", ",")
                  }}
                </span>
              </div>

              <div class="task-priority-cell">
                <span
                  class="priority-badge"
                  [class]="'priority-' + task.priorityLevel"
                >
                  {{ task.priorityLevel | titlecase }}
                </span>
              </div>

              <!-- Action Menu -->
              <div class="task-actions-cell">
                <!-- UPDATED: All actions now use onAction -->
                <button
                  mat-icon-button
                  (click)="onAction('view', task); $event.stopPropagation()"
                  matTooltip="View details"
                  aria-label="View task details"
                  class="view-btn"
                  *ngIf="task.type === 'assigned'"
                >
                  <i class="fa-sharp-duotone fa-solid fa-eye"></i>
                </button>

                <ng-container *ngIf="task.type === 'personal'">
                  <div class="personal-action-row">
                    <button
                      mat-icon-button
                      (click)="onAction('edit', task); $event.stopPropagation()"
                      *ngIf="!task.completionStatus && !task.ignoredStatus"
                      matTooltip="Edit"
                    >
                      <mat-icon>edit</mat-icon>
                    </button>
                    <button
                      mat-icon-button
                      (click)="
                        onAction('delete', task); $event.stopPropagation()
                      "
                      matTooltip="Delete"
                    >
                      <mat-icon style="color:red;">delete</mat-icon>
                    </button>
                    <button
                      mat-icon-button
                      (click)="
                        onAction('complete', task); $event.stopPropagation()
                      "
                      *ngIf="!task.completionStatus && !task.ignoredStatus"
                      matTooltip="Mark as completed"
                    >
                      <mat-icon color="primary">check_circle</mat-icon>
                    </button>
                  </div>
                </ng-container>
              </div>
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
            aria-label="Select page of tasks"
          ></mat-paginator>

          <!-- Empty State -->
          <div
            class="empty-state-list"
            *ngIf="
              (selectedTabIndex === 0 && personalTasks.length === 0) ||
              (selectedTabIndex === 1 && assignedTasks.length === 0)
            "
          >
            <mat-icon class="empty-icon">fact_check</mat-icon>
            <h3>
              No {{ selectedTabIndex === 0 ? "personal" : "assigned" }} tasks
              found
            </h3>
            <p>
              Either you're all done, or the filters need a reset. Give them a
              try!
            </p>
          </div>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  // Styles unchanged
  styles: [
    `
      @use "src/variables" as *;

      .task-list-container {
        padding: 0;
      }
      .list-card {
        border-radius: 12px;
        padding: 0;
      }
      .task-content {
        padding: 24px 30px;
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
        transition: transform 0.2s ease, box-shadow 0.2s ease,
          opacity 0.15s ease;
        transform-origin: center;
        will-change: transform;
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

      /* Active + hover: a bit more glow + lift */
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
        background: linear-gradient(135deg, #ebf3fb, #ebf3fb); /* Slate */
      }
      .bg-pending {
        background: linear-gradient(135deg, #ebf3fb, #ebf3fb); /* Blue */
      }
      .bg-overdue {
        background: linear-gradient(135deg, #ebf3fb, #ebf3fb); /* Red */
      }
      .bg-ignored {
        background: linear-gradient(
          135deg,
          #ebf3fb,
          #ebf3fb
        ); /* Amber/Orange */
      }
      .bg-completed {
        background: linear-gradient(135deg, #ebf3fb, #ebf3fb); /* Green */
      }
      .header-section {
        display: flex;
        justify-content: space-between;
        align-items: flex-start;
        margin-bottom: 3px;
        margin-left: 28px;
        margin-top: 20px;
        flex-wrap: wrap; // big horizontal space
        row-gap: 16px;

        .header-content {
          max-width: 500px;

          .page-title {
            font-size: $font-size-xxl;
            font-weight: $font-weight-bold;
            color: $title-color; // Dark text
            margin-bottom: 22px;
            letter-spacing: -0.5px;
          }

          .page-subtitle {
            font-size: $font-size-base;
            color: $color-text-muted; // Gray text
            line-height: $line-height-normal;
            font-weight: $font-weight-normal;
            margin: 0;
          }
        }
      }
      /* Tabs and Divider */
      .tabs-wrap {
        padding: 20px 30px 0;
        border-bottom: 1px solid #f3f4f6;
      }
      .custom-tabs {
        display: flex;
        border-bottom: 2px solid transparent;
        gap: 20px;
        width: 100%;
      }
      .flex-spacer {
        flex: 1 1 auto; /* grows to fill space */
      }
      .tab-button {
        appearance: none;
        background: transparent;
        border: 0;
        padding: 10px 0;
        font-size: 16px;
        font-weight: 500;
        color: #6b7280;
        cursor: pointer;
        transition: color 0.15s ease, border-bottom 0.15s ease;
        border-bottom: 2px solid transparent;
      }
      .tab-button.active {
        color: $primary-color;
        font-weight: 600;
        border-bottom: 2px solid $primary-color;
      }

      /* Filter/Sort Bar */
      .filter-controls-bar {
        display: flex;
        gap: 12px;
        align-items: center;
        flex-wrap: wrap;
        margin-bottom: 10px;
        justify-content: space-between;
      }
      /* ADDED: Container for left buttons */
      .filter-controls-left {
        display: flex;
        align-items: center;
        gap: 12px;
        flex-wrap: wrap;
      }
      /* ADDED: Container for right buttons */
      .filter-controls-right {
        display: flex;
        align-items: center;
        gap: 12px;
        flex-wrap: wrap;
      }
      .action-btn {
        height: 44px;
        font-weight: 500;
        padding: 0 16px;
        border-radius: 8px;
        min-width: 120px;
        color: #374151;
      }
      .clear-filters-btn {
        height: 44px;
        padding: 0 16px;
        color: #9ca3af;
        border: 1px solid #d1d5db;
        border-radius: 8px;
        min-width: 120px;
      }

      .empty-state-list {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;

        text-align: center;
        padding: 40px 16px;
        min-height: 260px;

        color: #6b7280; /* subtle gray for text */
      }

      .empty-state-list h3 {
        margin: 12px 0 8px;
        font-size: 20px;
        font-weight: 600;
        color: #111827;
      }

      .empty-state-list p {
        margin: 0;
        font-size: 16px;
        max-width: 360px;
      }

      .empty-icon {
        font-size: 74px; /* 🔥 bigger icon */
        height: 74px;
        width: 74px;
        color: #ef4444; /* soft blue/gray, tweak as you like */
        margin-bottom: 8px;
      }
      .task-type-toggle-group {
        display: flex;
        align-items: center;
        gap: 10px;
      }
      .task-type-toggle-group button {
        min-width: 100px;
        height: 44px; /* Match other buttons */
        background-color: #f3f4f6;
        color: #4b5563;
        box-shadow: none;
        font-weight: 500;
        border-radius: 8px;
      }
      .task-type-toggle-group button.active-toggle {
        background-color: $primary-color;
        color: white;
        font-weight: 600;
        box-shadow: 0 1px 3px rgba(0, 0, 0, 0.12), 0 1px 2px rgba(0, 0, 0, 0.24);
      }
      .divider {
        height: 1px;
        background: #f3f4f6;
        margin: 20px 0;
      }

      /* --- Task List Design --- */
      .tasks-list {
        display: flex;
        flex-direction: column;
        gap: 8px;
      }

      /* Updated Grid Layout */
      .task-list-header {
        display: grid;
        grid-template-columns: 48px minmax(300px, 1fr) 180px 120px 140px;
        padding: 10px 20px;
        color: $color-secondary;
        font-size: $font-size-md;
        font-weight: $font-weight-semibold;

        border-bottom: 1px solid #e5e7eb;
        margin-bottom: 8px;
      }
      .task-item-row {
        display: grid;
        grid-template-columns: 48px minmax(300px, 1fr) 180px 120px 140px;
        align-items: center;
        padding: 12px 20px;
        background: #fff;
        border-radius: 8px;
        border: 1px solid #e5e7eb;
        transition: background-color 0.2s;
        cursor: pointer; /* Makes entire row feel interactive */
      }
      .task-item-row:hover {
        background-color: #f9fafb;
      }

      .task-status-icon-cell {
        display: flex;
        align-items: center;
        justify-content: center;
      }
      .status-tick {
        color: #10b981; /* Green tick */
        font-size: 20px;
      }
      .status-ignored {
        color: #f59e0b; /* Blocked eye/ignored */
        font-size: 20px;
      }
      .status-pending {
        color: #3b82f6; /* Pending/schedule icon */
        font-size: 20px;
      }

      /* Task Title Cell */
      .task-title-cell {
        display: flex;
        flex-direction: column;
        align-items: flex-start;
      }
      .task-message-text {
        font-weight: 600;
        font-size: 16px;
        color: $title-color;
      }
      .task-date-cell {
        font-weight: 500;
        font-size: 15px;
        color: $title-color;
      }
      .task-message-text.completed-text {
        text-decoration: line-through;
        color: #9ca3af;
      }
      .task-desc-preview {
        font-size: 13px;
        color: #6b7280;
        margin: 4px 0 0 0;
      }
      .task-assignment-info {
        display: flex;
        flex-wrap: wrap;
        gap: 12px;
        margin-top: 8px;
        font-size: 12px;
        color: #4b5563;
      }
      .info-badge mat-icon {
        font-size: 14px;
        height: 14px;
        width: 14px;
        margin-right: 2px;
        vertical-align: middle;
      }

      .header-actions {
        justify-self: center;
      }
      /* Task Actions Cell */
      .task-actions-cell {
        display: flex; /* make it a flex container */
        justify-content: center; /* right-align the content */
        align-items: center;
        gap: 8px; /* spacing between groups/buttons */
        white-space: nowrap;
      }
      .personal-action-row {
        display: flex;
        align-items: center;
        gap: 4px; /* space between icons */
        flex-wrap: nowrap; /* keep on one line */
      }

      .task-actions-cell .view-btn i {
        font-weight: 600;
        font-size: 25px;
        color: $title-color;
        margin-top: 2px;
      }

      .priority-badge {
        display: inline-flex;
        align-items: center;
        padding: 2px 10px;
        border-radius: 9999px;
        font-size: 0.8rem;
        font-weight: 600;
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

      .priority-item {
        display: flex;
        align-items: center;
        justify-content: space-between; /* text left, dot right */
        width: 100%;
      }

      .priority-label {
        font-size: 14px;
      }

      .priority-dot {
        display: inline-block;
        width: 10px;
        height: 10px;
        border-radius: 50%;
        flex-shrink: 0;
        margin-right: 7px;
        padding: 6px;
      }

      .priority-high-dot {
        background-color: #ef4444;
      }

      .priority-medium-dot {
        background-color: #f59e0b;
      }

      .priority-low-dot {
        background-color: #10b981;
      }

      /* Media Queries */
      @media (max-width: 1024px) {
        .task-list-header {
          grid-template-columns: 0.5fr 2.5fr 1.5fr 1fr 0.5fr;
        }
        .task-item-row {
          grid-template-columns: 0.5fr 2.5fr 1.5fr 1fr 0.5fr;
        }
      }

      @media (prefers-reduced-motion: reduce) {
        .metric-card {
          transition: none;
        }
        .metric-card:hover,
        .metric-card.active-filter:hover {
          transform: none;
        }
      }

      @media (max-width: 768px) {
        .task-list-header,
        .task-item-row {
          grid-template-columns: 1fr;
          gap: 10px;
        }
        .task-list-header {
          display: none;
        }
        .task-item-row {
          flex-direction: column;
          align-items: stretch;
        }
        .task-status-icon-cell {
          justify-content: flex-start;
          margin-bottom: 8px;
        }
        .task-actions-cell {
          text-align: left;
          border-top: 1px solid #f3f4f6;
          padding-top: 8px;
          margin-top: 8px;
        }
      }
    `,
  ],
})
export class TaskListComponent implements OnInit, OnDestroy {
  // REMOVED: All @Inputs and @Outputs

  // --- Local State ---
  selectedTabIndex = 0;
  currentUser!: User;
  taskType: "personal" | "assigned" = "personal";

  personalTasks: Todo[] = [];
  assignedTasks: Todo[] = [];

  readonly defaultSort: SortBy = "latest";
  activeFilters!: TaskFilters;
  assignedView: "For Me" | "By Me" = "For Me";

  pageSize = 10;
  pageIndex = 0;
  pageSizeOptions: number[] = [5, 10, 25, 100];
  totalTasks: number = 0;

  priorityCountsPersonal = { low: 0, medium: 0, high: 0, all: 0 };
  statusCountsPersonal = {
    completed: 0,
    pending: 0,
    ignored: 0,
    dueToday: 0,
    overdue: 0,
    all: 0,
  };
  priorityCountsAssigned = { low: 0, medium: 0, high: 0, all: 0 };
  statusCountsAssigned = {
    completed: 0,
    pending: 0,
    ignored: 0,
    dueToday: 0,
    overdue: 0,
    all: 0,
  };

  // To satisfy template
  selectedTask: Todo | undefined;

  private routeSub: Subscription | undefined;

  constructor(
    private todoService: TodoService,
    private rolePermissionsService: RolePermissionsService,
    private snackBar: SnackBarService,
    private dialog: MatDialog,
    private loadingService: LoadingService,
    // UPDATED: Injected services
    private router: Router,
    private route: ActivatedRoute,
    private todoStateService: TodoStateService
  ) {
    this.currentUser = this.todoService.getCurrentUser();
  }

  ngOnInit() {
    // 1. Get default "For Me" / "By Me" view based on role
    const userRole = this.currentUser.role.toLowerCase();
    const defaultAssignedView: "For Me" | "By Me" =
      userRole === "admin"
        ? "By Me"
        : userRole === "superfiler"
        ? "For Me"
        : "For Me";

    // 2. Load persisted state from localStorage
    this.assignedView =
      this.todoStateService.getAssignedView(defaultAssignedView);
    this.activeFilters = this.todoStateService.getFilters({
      priority: "",
      status: "",
      sortBy: this.defaultSort,
      company: "all",
      assigneeRole: "all",
      states: [],
      dueDateFrom: "",
      dueDateTo: "",
      search: "",
    });

    // 3. Subscribe to route parameter changes
    // This drives the component's state (Personal vs. Assigned tab)
    // It also triggers a reload if query params change (e.g., from filter panel)
    this.routeSub = this.route.paramMap.subscribe((params) => {
      const type = (
        params.get("type") as "personal" | "assigned"
      ).toLowerCase();
      if (type === "personal" || type === "assigned") {
        this.selectedTabIndex = type === "personal" ? 0 : 1;
        this.taskType = type;
        this.todoStateService.saveActiveTab(type); // Save for filter panel
        this.pageIndex = 0; // Reset page index on tab change
        this.loadTasksForCurrentTab();
      } else {
        // Fallback to default route
        this.router.navigate(["/apps/todo/list", "assigned"]);
      }
    });
  }

  ngOnDestroy(): void {
    // Clean up subscription
    if (this.routeSub) {
      this.routeSub.unsubscribe();
    }
  }

  /**
   * Loads tasks based on the currently active tab (taskType)
   */
  loadTasksForCurrentTab() {
    if (this.taskType === "personal") {
      this.loadPersonalTasks();
    } else {
      this.loadAssignedTasks(this.assignedView);
    }
  }

  // --- Data Loading (Largely Unchanged) ---

  loadPersonalTasks() {
    this.loadingService.show("Loading personal tasks...");
    this.todoService
      .loadPersonalTasks(this.activeFilters, this.pageIndex, this.pageSize)
      .subscribe({
        next: (pagedResponse: any) => {
          this.personalTasks = pagedResponse.content;
          this.totalTasks = pagedResponse.totalElements;
          if (pagedResponse.stats) {
            this.statusCountsPersonal = {
              completed: pagedResponse.stats.completedCount || 0,
              pending: pagedResponse.stats.pendingCount || 0,
              overdue: pagedResponse.stats.overdueCount || 0,
              ignored: 0,
              dueToday: 0,
              all: pagedResponse.totalElements || 0,
            };
          } else {
            this.recomputeAllCounts();
          }
          this.loadingService.hide();
        },
        error: (err) => {
          this.loadingService.hide();
          this.snackBar.showError("Personal tasks couldn’t be loaded", "Close");
          console.error("Error loading personal tasks from API:", err);
        },
      });
  }

  loadAssignedTasks(assignedView: "For Me" | "By Me") {
    this.loadingService.show("Loading assigned tasks...");
    this.todoService
      .loadAssignedTasks(
        this.activeFilters,
        assignedView,
        this.pageIndex,
        this.pageSize
      )
      .subscribe({
        next: (pagedResponse: any) => {
          const content = pagedResponse.content || [];
          this.assignedTasks = this.attachCompanyNames(content);
          // this.assignedTasks = pagedResponse.content;
          this.totalTasks = pagedResponse.totalElements;
          if (pagedResponse.stats) {
            this.statusCountsAssigned = {
              completed: pagedResponse.stats.completedCount || 0,
              pending: pagedResponse.stats.pendingCount || 0,
              overdue: pagedResponse.stats.overdueCount || 0,
              ignored: pagedResponse.stats.ignoredCount || 0,
              dueToday: 0,
              all: pagedResponse.totalElements || 0,
            };
          } else {
            this.recomputeAssignedCounts();
          }
          this.loadingService.hide();
        },
        error: (err) => {
          this.loadingService.hide();
          this.snackBar.showError("Assigned tasks couldn’t be loaded", "Close");
          console.error("Error loading assigned tasks from API:", err);
          /* ... error handling ... */
        },
      });
  }

  // --- Event Handlers (Refactored for Routing) ---

  /**
   * Navigates to the corresponding list route when a tab is clicked.
   */
  onTabChange(index: number) {
    const newType = index === 0 ? "personal" : "assigned";
    if (this.taskType !== newType) {
      this.router.navigate(["/apps/todo/list", newType]);
    }
  }

  /**
   * Navigates to the create task page.
   */
  goToCreate() {
    this.router.navigate(["/apps/todo/create"]);
  }
  get showCompanyColumn(): boolean {
    return (
      (this.currentUser?.role || "").toLowerCase() === "consumer" &&
      this.selectedTabIndex === 1 // only Assigned Tasks tab
    );
  }

  /**
   * Navigates to open the filter panel in the named outlet.
   */
  openFilterPanel() {
    this.router.navigate(["/apps/todo", { outlets: { panel: ["filter"] } }]);
  }

  /**
   * Updates sort, saves state, and reloads tasks.
   */
  onSortChange(sortBy: SortBy) {
    this.pageIndex = 0;
    this.activeFilters = { ...this.activeFilters, sortBy: sortBy };
    this.todoStateService.saveFilters(this.activeFilters);
    this.loadTasksForCurrentTab();
  }

  onPriorityChange(priority: "low" | "medium" | "high" | "") {
    this.pageIndex = 0;
    this.activeFilters = { ...this.activeFilters, priority };
    this.todoStateService.saveFilters(this.activeFilters);
    this.loadTasksForCurrentTab();
  }

  /**
   * Updates status filter, saves state, and reloads tasks.
   */
  applyStatusFilter(status: TaskFilters["status"]) {
    this.pageIndex = 0;
    if (this.assignedView === "By Me" && this.selectedTabIndex === 1) {
      this.snackBar.showError(
        "Status filter not available in this view",
        "Close"
      );
      return;
    }
    const isSameStatus = this.activeFilters.status === status;
    // const newStatus =
    //   this.activeFilters.status === status && status !== "" ? "" : status;
    // this.activeFilters = { ...this.activeFilters, status: newStatus };
    if (isSameStatus && status !== "") {
      // 👉 Clicking the same status again → remove status filter only
      this.activeFilters = {
        ...this.activeFilters,
        status: "",
      };
    } else {
      // 👉 New status selected OR "Total" clicked
      //    Clear all other filters, keep only this status (or '')
      this.activeFilters = {
        ...this.resetNonStatusFilters(),
        status: status || "",
      };
    }
    this.todoStateService.saveFilters(this.activeFilters);
    this.loadTasksForCurrentTab();
  }

  /**
   * Updates the "For Me" / "By Me" view, saves state, and reloads tasks.
   */
  onTaskViewChange(view: "For Me" | "By Me") {
    if (this.assignedView === view) {
      return; // No change
    }
    this.pageIndex = 0; // Reset to first page
    this.assignedView = view;
    this.todoStateService.saveAssignedView(view);
    this.loadAssignedTasks(view); // Reload assigned tasks
  }

  /**
   * Clears filters, saves state, and reloads tasks.
   */
  clearFilters() {
    this.pageIndex = 0;
    this.activeFilters = {
      ...this.resetNonStatusFilters(),
      status: this.activeFilters.status,
      priority: "",
      sortBy: this.defaultSort,
      company: "all",
      assigneeRole: "all",
      states: [],
      dueDateFrom: "",
      dueDateTo: "",
      search: "",
    } as TaskFilters;
    this.todoStateService.saveFilters(this.activeFilters);
    this.loadTasksForCurrentTab();
  }

  hasActiveFilters(): boolean {
    // ... logic unchanged ...
    const f = this.activeFilters;
    let isActive =
      f.priority !== "" ||
      // (f.status !== "" && f.status !== undefined) ||
      f.sortBy !== this.defaultSort ||
      f.dueDateFrom !== "" ||
      f.dueDateTo !== "" ||
      f.search !== "";
    if (this.selectedTabIndex === 1) {
      isActive =
        isActive ||
        f.company !== "all" ||
        f.assigneeRole !== "all" ||
        Boolean(f.states && f.states.length > 0);
    }
    return isActive;
  }

  // --- Master/Detail/Action Handlers ---

  onRowClick(task: Todo) {
    // Click row to view
    // this.onAction('view', task);
  }

  /**
   * Handles all actions for a task (view, edit, delete, complete).
   * Navigates to the appropriate route.
   */
  onAction(
    action: "view" | "edit" | "delete" | "complete" | "ignore",
    task: Todo
  ) {
    if (action === "edit") {
      this.router.navigate(["/apps/todo/edit", task.id]);
    } else if (action === "delete") {
      this.onDeleteTask(task);
    } else if (action === "complete" && task.type === "personal") {
      this.markTaskDone(task);
    } else if (action === "view") {
      // Logic to determine which view to show
      const isTaskOwner = task.assignedBy === "You";
      if (
        task.type === "personal" ||
        (task.type === "assigned" && isTaskOwner)
      ) {
        // Go to Owner/Analytics view
        this.router.navigate(["/apps/todo/analytics", task.id]);
      } else {
        // Go to Assignee view
        this.router.navigate(["/apps/todo/view", task.id]);
      }
    }
  }

  markTaskDone(task: Todo) {
    this.loadingService.show("Updating task...");
    this.todoService.markTaskDone(task.id).subscribe({
      next: () => {
        this.snackBar.showSuccess(
          `Personal task updated as completed`,
          "Close"
        );
        this.loadTasksForCurrentTab(); // Reload list
        this.loadingService.hide();
      },
      error: (err: any) => {
        this.loadingService.hide();
        /* ... error handling ... */
      },
    });
  }

  onDeleteTask(task: Todo) {
    this.loadingService.show("Deleting task...");
    // ... confirmation logic (unchanged) ...
    const idToDelete =
      task.type === "assigned" ? task.masterId || task.id : task.id;
    this.todoService.deleteTodo(idToDelete).subscribe({
      next: () => {
        this.snackBar.showSuccess(
          `${
            task.type === "personal" ? "Personal" : "Assigned"
          } task deleted successfully`,
          "Close"
        );
        this.loadTasksForCurrentTab(); // Reload list
        this.loadingService.hide();
      },
      error: (err: any) => {
        this.loadingService.hide();
        /* ... error handling ... */
      },
    });
  }

  onPageChange(event: PageEvent) {
    this.pageSize = event.pageSize;
    this.pageIndex = event.pageIndex;
    this.loadTasksForCurrentTab();
  }

  // --- All other private helper methods (isTaskOverdue, getPermissions, etc.) remain unchanged ---
  // ... (isTaskOverdue, getPermissions, canEditAssignedTask, etc.) ...
  isTaskOverdue(task: Todo): boolean {
    if (task.completionStatus || task.ignoredStatus || !task.targetDate)
      return false;
    const targetDateMidnight = new Date(task.targetDate.slice(0, 10));
    targetDateMidnight.setHours(0, 0, 0, 0);
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    return targetDateMidnight < today;
  }
  private getPermissions(task: Todo): TaskPermissions {
    const isTaskOwner = this.assignedView === "By Me";
    const isAssignedToUser = this.assignedView === "For Me";
    return this.rolePermissionsService.getTaskPermissions(
      this.currentUser,
      task.type,
      isTaskOwner,
      isAssignedToUser
    );
  }
  canEditAssignedTask(task: Todo): boolean {
    const perms = this.getPermissions(task);
    return (
      perms.canEdit &&
      this.assignedView === "By Me" &&
      !task.completionStatus &&
      !task.ignoredStatus
    );
  }
  canDeleteAssignedTask(task: Todo): boolean {
    const perms = this.getPermissions(task);
    return perms.canDelete && this.assignedView === "By Me";
  }
  canTakeAction(task: Todo): boolean {
    const perms = this.getPermissions(task);
    return perms.canMarkDone && this.assignedView === "For Me";
  }
  private roleOfNameOrRole(value?: string) {
    if (!value) return undefined;
    const v = value.toLowerCase?.() ?? "";
    if (
      v === "admin" ||
      v === "superfiler" ||
      v === "vendor" ||
      v === "consumer"
    )
      return v;
    const u = this.todoService.getUsers().find((x) => x.name === value);
    return u?.role;
  }
  private recomputeAssignedCounts() {
    this.priorityCountsAssigned = this.buildPriorityCounts(this.assignedTasks);
    this.statusCountsAssigned = this.buildStatusCounts(this.assignedTasks);
  }
  private recomputeAllCounts() {
    this.priorityCountsPersonal = this.buildPriorityCounts(this.personalTasks);
    this.statusCountsPersonal = this.buildStatusCounts(this.personalTasks);
    this.recomputeAssignedCounts();
  }
  private buildPriorityCounts(list: Todo[]) {
    const c = { low: 0, medium: 0, high: 0, all: list.length };
    for (const t of list) {
      switch ((t.priorityLevel || "").toLowerCase()) {
        case "low":
          c.low++;
          break;
        case "medium":
          c.medium++;
          break;
        case "high":
          c.high++;
          break;
      }
    }
    return c;
  }
  private buildStatusCounts(list: Todo[]) {
    const todayStr = new Date().toISOString().slice(0, 10);
    const todayMidnight = new Date(todayStr);
    todayMidnight.setHours(0, 0, 0, 0);
    const c = {
      completed: 0,
      pending: 0,
      ignored: 0,
      dueToday: 0,
      overdue: 0,
      all: list.length,
    };
    for (const t of list) {
      const done = !!t.completionStatus;
      const ignored = !!t.ignoredStatus;
      let isTaskOverdue = false;
      if (t.targetDate) {
        const targetDateStr = t.targetDate.slice(0, 10);
        const targetDateMidnight = new Date(targetDateStr);
        targetDateMidnight.setHours(0, 0, 0, 0);
        if (targetDateMidnight < todayMidnight) {
          isTaskOverdue = true;
        }
      }
      if (done) {
        c.completed++;
      } else if (ignored) {
        c.ignored++;
      } else if (isTaskOverdue) {
        c.overdue++;
      } else {
        c.pending++;
        if (t.targetDate && t.targetDate.slice(0, 10) === todayStr) {
          c.dueToday++;
        }
      }
    }
    return c;
  }
  get totalPersonal(): number {
    return (
      (this.statusCountsPersonal.completed || 0) +
      (this.statusCountsPersonal.pending || 0)
    );
  }
  get totalAssigned(): number {
    return (
      (this.statusCountsAssigned.completed || 0) +
      (this.statusCountsAssigned.pending || 0) +
      (this.statusCountsAssigned.ignored || 0)
    );
  }
  private resetNonStatusFilters(): TaskFilters {
    return {
      ...this.activeFilters,
      priority: "",
      sortBy: this.defaultSort,
      company: "all",
      assigneeRole: "all",
      states: [],
      dueDateFrom: "",
      dueDateTo: "",
      search: "",
    };
  }

  private attachCompanyNames(list: any[]): any[] {
    this.todoService.refreshCompanyMap(); // refresh once per load

    return (list || []).map((t: any) => ({
      ...t,
      companyDisplayName: this.todoService.getCompanyNameById(t.companyId), // ✅ only ONE
    }));
  }
}
