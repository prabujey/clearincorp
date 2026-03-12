import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Input,
  OnChanges,
  OnInit,
  Output,
  signal,
  SimpleChanges,
  WritableSignal,
} from "@angular/core";
import { CommonModule, DatePipe } from "@angular/common";
import {
  FormBuilder,
  FormGroup,
  Validators,
  ReactiveFormsModule,
} from "@angular/forms";
import { MatCardModule } from "@angular/material/card";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatInputModule } from "@angular/material/input";
import { MatSelectModule } from "@angular/material/select";
import { MatButtonModule } from "@angular/material/button";
import {
  MatDatepickerInput,
  MatDatepickerModule,
  MatDateRangeInput,
  MatDateRangePicker,
  MatEndDate,
  MatStartDate,
} from "@angular/material/datepicker";
import { MatNativeDateModule } from "@angular/material/core";
import { MatIconModule } from "@angular/material/icon";
import { MatTabsModule } from "@angular/material/tabs";
import { MatSnackBar } from "@angular/material/snack-bar";

import { TodoStateService } from "src/app/services/todo/todo-state.service";
import { SnackBarService } from "src/app/shared/snackbar.service";
import { LoadingService } from "src/app/services/loading/loading.service";
import { RolePermissionsService } from "src/app/services/todo/role-permissions.service";
import { FileUploadComponent } from "../file-upload/file-upload.component";
import {
  Todo,
  User,
  FileAttachment,
  TaskPermissions,
  TasKCreationForms,
} from "src/app/models/todo-models/todo.model";
import { ActivatedRoute, Router } from "@angular/router";
import { StateSelectorComponent } from "../Filter-selectors/state-selector.component";
import { FormsModule } from "@angular/forms";
import { MatDividerModule } from "@angular/material/divider";

import { CompanySearchSelectorComponent } from "../Filter-selectors/company-search-selector.component";
import {
  MockTodoService,
  Company,
} from "src/app/services/todo/mock-todo.service";
import { PrincipalActivitySelectorComponent } from "../Filter-selectors/principal-activity-selector.component";
import { PersonalTaskService } from "src/app/services/todo/personal-task.service";
import {
  CompanyFilterRequest,
  CompanySlim,
} from "src/app/models/todo-models/assigned-task.model";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
// REMOVED: File slider component is no longer declared here
import { MatTooltipModule } from "@angular/material/tooltip";
import { Subscription } from "rxjs";
import { Location } from "@angular/common";
import { FileSliderComponent } from "../file-slider/file-slider.component";
import { MatDialog } from "@angular/material/dialog";
import { TodoService } from "src/app/services/todo/todo.service";
import { SecureStorageService } from "src/app/services/storage/secure-storage.service";

@Component({
  selector: "app-task-create-edit-panel",
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    FormsModule,
    MatSelectModule,
    MatButtonModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatIconModule,
    MatDividerModule,
    FileUploadComponent,
    CompanySearchSelectorComponent,
    // CompanySelectorComponent as any,
    // StateSelectorComponent as any,
    // PrincipalActivitySelectorComponent as any,
    MatProgressSpinnerModule,
    // MatDateRangeInput,
    // MatDateRangePicker,
    MatDatepickerInput,
    // MatEndDate,
    // MatStartDate,
    // REMOVED: FileSliderComponent,
    MatTooltipModule,
  ],
  template: `
    <div
      [class.task-panel-container-full]="isEditOrCreateMode()"
      [class.task-panel-container-side]="mode === 'view-assignee'"
    >
      <!-- Use mat-card for full-page layout -->
      <mat-card class="full-page-card" *ngIf="isEditOrCreateMode()">
        <div class="panel-header">
          <button
            mat-icon-button
            (click)="goBack()"
            matTooltip="Back to Task List"
          >
            <mat-icon>arrow_back</mat-icon>
          </button>
          <h2 class="panel-title">
            <mat-icon>{{ getHeaderIcon() }}</mat-icon>
            {{ getHeaderTitle() }}
          </h2>
          <span class="header-spacer"></span>
        </div>

        <mat-card-content class="panel-content panel-content-full">
          <ng-container *ngTemplateOutlet="formContent"></ng-container>
        </mat-card-content>

        <div class="panel-actions">
          <span class="flex-spacer"></span>
          <button
            mat-flat-button
            class="button-all"
            type="button"
            (click)="
              selectedTabIndex === 0 ? onSubmitPersonal() : onSubmitAssigned()
            "
            [disabled]="isAssigning() || activeForm.invalid"
          >
            <mat-icon *ngIf="!isAssigning()">{{
              isEditMode ? "save" : "add"
            }}</mat-icon>
            {{
              isAssigning()
                ? "Processing..."
                : isEditMode
                ? "Save Changes"
                : selectedTabIndex === 0
                ? "Create Task"
                : "Assign Task"
            }}
          </button>
        </div>
      </mat-card>

      <!-- Use simple div for side-panel (view-assignee) layout -->
      <div class="side-panel-view" *ngIf="mode === 'view-assignee'">
        <div class="panel-header">
          <h2 class="panel-title">
            <mat-icon>{{ getHeaderIcon() }}</mat-icon>
            {{ getHeaderTitle() }}
          </h2>
          <button mat-icon-button (click)="goBack()" matTooltip="Close Panel">
            <mat-icon>close</mat-icon>
          </button>
        </div>

        <div class="panel-content panel-content-side">
          <ng-container *ngTemplateOutlet="viewAssigneeContent"></ng-container>
        </div>

        <div class="panel-actions">
          <ng-container *ngIf="mode === 'view-assignee' && editingTask">
            <ng-container
              *ngIf="
                canTakeAction(editingTask) &&
                !editingTask.completionStatus &&
                !editingTask.ignoredStatus
              "
            >
              <!-- <button  mat-flat-button  class="button-prev" (click)="onAction('ignore')" matTooltip="Ignore Task" [disabled]="actionForm.invalid|| isAssigning()">
                        <mat-icon>block</mat-icon> Ignore
                    </button>
                    <button mat-flat-button  class="button-all" (click)="onAction('done')" matTooltip="Mark as Done" [disabled]="actionForm.invalid || isAssigning()">
                      <mat-icon *ngIf="!isAssigning()">check_circle</mat-icon>
                      {{ isAssigning() ? 'Saving...' : 'Mark As Done' }}
                    </button> -->
              <span
                class="tooltip-wrap"
                [matTooltip]="
                  actionForm.invalid
                    ? 'Action Notes is required'
                    : isAssigning()
                    ? 'Please wait…'
                    : ''
                "
                [matTooltipDisabled]="!(actionForm.invalid || isAssigning())"
              >
                <button
                  mat-flat-button
                  class="button-prev"
                  (click)="onAction('ignore')"
                  [disabled]="actionForm.invalid || isAssigning()"
                >
                  <mat-icon>block</mat-icon> Ignore
                </button>
              </span>

              <span
                class="tooltip-wrap"
                [matTooltip]="
                  actionForm.invalid
                    ? 'Action Notes is required'
                    : isAssigning()
                    ? 'Please wait…'
                    : ''
                "
                [matTooltipDisabled]="!(actionForm.invalid || isAssigning())"
              >
                <button
                  mat-flat-button
                  class="button-all"
                  (click)="onAction('done')"
                  [disabled]="actionForm.invalid || isAssigning()"
                >
                  <mat-icon *ngIf="!isAssigning()">check_circle</mat-icon>
                  {{ isAssigning() ? "Saving..." : "Mark As Done" }}
                </button>
              </span>
            </ng-container>
          </ng-container>
        </div>
      </div>
    </div>

    <!-- ============================================= -->
    <!-- TEMPLATES: To avoid duplicating code         -->
    <!-- ============================================= -->

    <ng-template #formContent>
      <!-- Task Type Selector (Only visible for new creation) -->
      <div class="tabs-wrap" *ngIf="!isEditMode && canCreateAssignedTasks">
        <div class="custom-tabs" role="tablist" aria-label="Task type">
          <button
            type="button"
            class="tab-button"
            [class.active]="selectedTabIndex === 0"
            (click)="onTabChange(0)"
            role="tab"
          >
            Personal Task
          </button>
          <button
            type="button"
            class="tab-button"
            [class.active]="selectedTabIndex === 1"
            (click)="onTabChange(1)"
            role="tab"
          >
            Assigned Task
          </button>
        </div>
        <mat-divider></mat-divider>
      </div>

      <!-- The main form, handles both personal and assigned creation/editing -->
      <form
        [formGroup]="selectedTabIndex === 0 ? personalForm : assignedForm"
        class="task-form-layout"
      >
        <!-- Task Details -->
        <mat-form-field appearance="outline" class="full-width-grid">
          <mat-label>Task Title</mat-label>
          <input
            matInput
            formControlName="message"
            placeholder="Task Title"
            [readonly]="isEditMode"
          />
          <mat-error *ngIf="messageCtrl?.hasError('required')">
            Task title is required
          </mat-error>
          <mat-error *ngIf="messageCtrl?.hasError('minlength')">
            Task title must be at least 3 characters.
          </mat-error>
          <mat-error *ngIf="messageCtrl?.hasError('maxlength')">
            Task title cannot exceed 200 characters.
          </mat-error>
          <mat-error *ngIf="messageCtrl?.hasError('pattern')">
            Only letters, numbers, spaces, - . ' , & are allowed.
          </mat-error>
        </mat-form-field>

        <!-- Priority -->
        <mat-form-field appearance="outline">
          <mat-label>Priority Level</mat-label>
          <mat-select formControlName="priorityLevel">
            <mat-option value="low">Low</mat-option>
            <mat-option value="medium">Medium</mat-option>
            <mat-option value="high">High</mat-option>
          </mat-select>
        </mat-form-field>

        <!-- Due Date -->
        <mat-form-field appearance="outline">
          <mat-label>Due Date </mat-label>
          <input
            matInput
            [matDatepicker]="picker"
            [min]="minDate"
            formControlName="targetDate"
            placeholder="Select Due Date"
          />
          <mat-datepicker-toggle
            matSuffix
            [for]="picker"
          ></mat-datepicker-toggle>
          <mat-datepicker #picker></mat-datepicker>

          <mat-error *ngIf="targetDateCtrl?.hasError('required')">
            Due date is Required.
          </mat-error>
          <mat-error *ngIf="targetDateCtrl?.hasError('pastDate')">
            Due date cannot be in the past.
          </mat-error>
          <mat-error *ngIf="targetDateCtrl?.hasError('invalidDate')">
            Please choose a valid date.
          </mat-error>
        </mat-form-field>

        <!-- Description (Assigned Tasks only) -->
        <mat-form-field
          appearance="outline"
          class="full-width-grid"
          *ngIf="selectedTabIndex === 1"
        >
          <mat-label>Description </mat-label>
          <textarea
            matInput
            formControlName="description"
            rows="4"
            placeholder="Provide detailed instructions or context for this task..."
          ></textarea>

          <mat-error
            *ngIf="
              assignedForm.get('description')?.hasError('required') &&
              selectedTabIndex === 1
            "
          >
            Description is required for assigned tasks.
          </mat-error>

          <mat-error
            *ngIf="assignedForm.get('description')?.hasError('maxlength')"
          >
            Description cannot exceed 1000 characters.
          </mat-error>
        </mat-form-field>

        <!-- Assignment & Filtering (Assigned Tasks only) -->
        <ng-container
          *ngIf="
            selectedTabIndex === 1 && canCreateAssignedTasks && !isEditMode
          "
        >
          <div class="form-section-boxed full-width-grid">
            <h3 class="section-title" *ngIf="currentUser.role === 'Admin'">
              Assign To
            </h3>
            <h3 class="section-title" *ngIf="currentUser.role === 'SuperFiler'">
              Assignment & Filtering
            </h3>

            <!-- Admin Role Selector -->
            <ng-container *ngIf="currentUser.role === 'Admin'">
              <div class="assign-role-toggle-group">
                <button
                  type="button"
                  mat-flat-button
                  [class.active-toggle]="assignToRole === 'SuperFiler'"
                  (click)="assignToRole = 'SuperFiler'"
                >
                  SuperFiler
                </button>
                <button
                  type="button"
                  mat-flat-button
                  [class.active-toggle]="assignToRole === 'Vendor'"
                  (click)="assignToRole = 'Vendor'"
                >
                  Vendor
                </button>
                <button
                  type="button"
                  mat-flat-button
                  [class.active-toggle]="assignToRole === 'BOTH'"
                  (click)="assignToRole = 'BOTH'"
                >
                  Both
                </button>
              </div>
            </ng-container>

            <!-- Superfiler Company Filters -->
            <ng-container *ngIf="currentUser.role === 'SuperFiler'">
              <app-company-search-selector
                (selectedCompaniesChange)="onSelectedCompaniesChange($event)"
                (bulkAssignFiltersChange)="onBulkFiltersChange($event)"
              >
              </app-company-search-selector>
            </ng-container>
          </div>
        </ng-container>

        <!-- Attachments (for New Assigned Task) -->
        <div
          class="form-section-boxed full-width-grid"
          *ngIf="selectedTabIndex === 1"
        >
          <h3 class="section-title">Attachments</h3>
          <div *ngIf="isEditMode" class="mb-4">
            <div *ngIf="isLoadingAttachments()" class="loading-spinner">
              <mat-spinner diameter="30"></mat-spinner>
              <span>Loading existing files...</span>
            </div>
            <button
              mat-flat-button
              class="button-allset"
              *ngIf="
                !isLoadingAttachments() && fetchedAttachmentUrls().length > 0
              "
              (click)="openSlider(0)"
            >
              <mat-icon>perm_media</mat-icon>
              View {{ fetchedAttachmentUrls().length }} Existing Attachments
            </button>
            <div
              *ngIf="
                !isLoadingAttachments() && fetchedAttachmentUrls().length === 0
              "
              class="empty-files"
            >
              No existing attachments found.
            </div>
          </div>

          <app-file-upload
            (filesChange)="onAttachmentsChange($event)"
            (rawFilesChange)="onRawFilesChange($event)"
            [label]="
              isEditMode ? 'Upload New Files' : 'Attach Files (Optional)'
            "
          >
          </app-file-upload>
        </div>
      </form>
    </ng-template>

    <ng-template #viewAssigneeContent>
      <ng-container *ngIf="editingTask">
        <div class="task-info-section">
          <h3 class="task-message">{{ editingTask.message }}</h3>
          <p class="task-description">
            {{ editingTask.description || "No description provided." }}
          </p>

          <div class="info-badges">
            <span class="info-badge">
              <mat-icon>event</mat-icon>
              Due Date:
              {{
                ((editingTask.targetDate | date : "MMM d, y") || "")
                  .replace(" ", "")
                  .replace(", ", ",") | uppercase
              }}
            </span>
            <span
              class="info-badge"
              [class]="'priority-' + editingTask.priorityLevel"
            >
              <mat-icon>priority_high</mat-icon>
              Priority: {{ editingTask.priorityLevel | titlecase }}
            </span>
            <span
              class="info-badge"
              *ngIf="editingTask.type === 'assigned' && !isTaskOwner()"
            >
              <mat-icon>assignment_ind</mat-icon>
              Assigned By: {{ editingTask.assignedBy }}
            </span>
          </div>
        </div>

        <!-- Task Status -->
        <div class="task-status-section">
          <span class="status-label">
            <mat-icon *ngIf="editingTask.completionStatus" class="status-tick"
              >check_circle</mat-icon
            >
            <mat-icon *ngIf="editingTask.ignoredStatus" class="status-ignored"
              >block</mat-icon
            >
            <mat-icon
              *ngIf="
                !editingTask.completionStatus && !editingTask.ignoredStatus
              "
              class="status-pending"
              >schedule</mat-icon
            >
            Status:
            {{
              editingTask.completionStatus
                ? "Completed"
                : editingTask.ignoredStatus
                ? "Ignored"
                : "Pending"
            }}
          </span>
        </div>

        <div *ngIf="editingTask.type === 'assigned'">
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
            View {{ fetchedAttachmentUrls().length }} Attachments
          </button>
        </div>

        <!-- Completion/Ignore Read-only Details (After task is acted upon) -->
        <ng-container
          *ngIf="editingTask.completionStatus || editingTask.ignoredStatus"
        >
          <div class="completion-notes-section">
            <h4>
              {{
                editingTask.completionStatus
                  ? "Completion Notes"
                  : "Ignore Reason"
              }}
            </h4>
            <p class="notes-text">
              {{
                editingTask.completionNotes ||
                  editingTask.ignoredReason ||
                  "No notes provided."
              }}
            </p>
          </div>
        </ng-container>

        <!-- Dynamic Action Form for ASSIGNEE (Only visible for Assignees & Pending) -->
        <ng-container
          *ngIf="
            canTakeAction(editingTask) &&
            !editingTask.completionStatus &&
            !editingTask.ignoredStatus
          "
        >
          <form [formGroup]="actionForm" class="action-form">
            <h4 class="form-title">Action Notes</h4>
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Completion Notes / Ignore Reason</mat-label>
              <textarea
                matInput
                formControlName="notes"
                rows="4"
                placeholder="Enter notes about the task completion or reason for ignoring..."
              ></textarea>
              <mat-error *ngIf="actionForm.get('notes')?.hasError('required')">
                Action Notes is Required
              </mat-error>
              <mat-error *ngIf="actionForm.get('notes')?.hasError('maxlength')">
                Notes cannot exceed 255 characters.
              </mat-error>
            </mat-form-field>

            <app-file-upload
              (filesChange)="onAttachmentsChangeForAction($event)"
              (rawFilesChange)="onRawFilesChangeForAction($event)"
              label="Attach File (Optional)"
            ></app-file-upload>
          </form>
        </ng-container>
      </ng-container>
    </ng-template>
  `,
  styles: [
    `
      @use "src/variables" as *;
      /* --- Full Page Layout (Create/Edit) --- */
      .task-panel-container-full {
        width: 100%;
      }
      .full-page-card {
        border-radius: 12px;
        padding: 0;
        display: flex;
        flex-direction: column;
        height: 100%;
      }
      .panel-header {
        display: flex;
        align-items: center;
        padding: 16px 20px;
        border-bottom: 1px solid #e5e7eb;
        background-color: #f9fafb;
        justify-content: space-between;
        flex-shrink: 0;
      }
      .panel-title {
        font-size: 24px;
        font-weight: 700;
        margin-left: 30px;
        color: $title-color;
        display: flex;
        align-items: center;
      }
      .panel-title mat-icon {
        margin-right: 8px;
        margin-bottom: 3px;
        color: $primary-color;
        font-size: 25px;
      }
      .header-spacer {
        flex: 1 1 auto;
      }
      .panel-content-full {
        padding: 24px 30px;
        max-width: 1150px;
        margin: 0 auto;
        width: 100%;
        flex: 1 1 auto;
        overflow-y: auto;
      }
      .panel-actions {
        display: flex;
        justify-content: space-between;
        padding: 16px 20px;
        border-top: 1px solid #e5e7eb;
        gap: 10px;
        flex-shrink: 0;
        background-color: transparent;
      }
      .panel-actions button {
        font-weight: 600;
      }
      .flex-spacer {
        flex: 1 1 auto;
      }
      .tooltip-wrap {
        display: inline-block;
      }

      /* --- Side Panel Layout (View-Assignee) --- */
      .task-panel-container-side,
      .side-panel-view {
        display: flex;
        flex-direction: column;
        height: 100%;
        min-height: 0;
      }
      .panel-content-side {
        flex: 1 1 auto;
        min-height: 0;
        overflow-y: auto;
        padding: 20px;
      }

      /* --- Common Inner Form Styles --- */
      .tabs-wrap {
        margin-bottom: 20px;
        padding-bottom: 20px;
      }
      .custom-tabs {
        display: inline-flex;
        border-bottom: 2px solid transparent;
        gap: 20px;
        margin-bottom: 16px;
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
        color: #ec3252;
        font-weight: 600;
        border-bottom: 2px solid #ec3252;
      }

      .task-form-layout {
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: 20px;
      }
      .full-width-grid {
        grid-column: 1 / -1; /* Span both columns */
      }
      .form-section-boxed {
        border: 1px solid #e5e7eb;
        border-radius: 8px;
        padding: 15px;
        grid-column: 1 / -1; /* Ensure sections span full width */
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
      .full-width {
        width: 100%;
      }

      button[disabled] {
        background-color: #d6dbdf !important;
        color: #99a3a4 !important;
        cursor: none;
        opacity: 0.6;
        box-shadow: none !important;
      }
      @media (max-width: 768px) {
        .task-form-layout {
          grid-template-columns: 1fr; /* Stack columns on mobile */
        }
      }

      /* --- View-Assignee Styles --- */
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

      .task-status-section {
        margin-bottom: 20px;
      }
      .status-label {
        display: inline-flex;
        align-items: center;
        font-weight: 600;
        font-size: 16px;
      }
      .status-tick,
      .status-ignored,
      .status-pending {
        margin-right: 8px;
        font-size: 24px;
        height: 24px;
        width: 24px;
      }
      .status-tick {
        color: #10b981;
      }
      .status-ignored {
        color: #f59e0b;
      }
      .status-pending {
        color: #3b82f6;
      }

      .completion-notes-section {
        margin-top: 20px;
        padding: 15px;
        background-color: #f0f8ff;
        border-radius: 8px;
      }
      .completion-notes-section h4 {
        margin-top: 0;
        margin-bottom: 8px;
        font-weight: 600;
        color: #1e40af;
      }
      .notes-text {
        font-size: 14px;
        margin-bottom: 10px;
      }
      .attachments-section {
        margin-top: 20px;
        border: 1px solid #e5e7eb;
        border-radius: 8px;
        padding: 15px;
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

      .action-form {
        margin-top: 20px;
        padding-top: 15px;
        border-top: 1px dashed #d1d5db;
      }
      .form-title {
        font-size: 16px;
        font-weight: 700;
        color: #374151;
        margin-top: 0;
        margin-bottom: 15px;
      }

      .assign-role-toggle-group {
        display: flex;
        align-items: center;
        gap: 10px;
        margin-bottom: 10px;
      }
      .assign-role-toggle-group .toggle-label {
        font-weight: 500;
        color: #374151;
      }
      .assign-role-toggle-group button {
        min-width: 100px;
        background-color: #f3f4f6;
        color: #4b5563;
        box-shadow: none;
      }
      .assign-role-toggle-group button.active-toggle {
        background-color: $primary-color;
        color: white;
        font-weight: 600;
        box-shadow: 0 1px 3px rgba(0, 0, 0, 0.12), 0 1px 2px rgba(0, 0, 0, 0.24);
      }
      .mb-4 {
        margin-bottom: 1rem;
      }
    `,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TaskCreateEditComponent implements OnInit, OnChanges {
  mode: "create" | "edit" | "view-assignee" = "create";
  editingTask?: Todo | null;
  personalForm!: FormGroup;
  assignedForm!: FormGroup;
  actionForm!: FormGroup;

  selectedTabIndex = 0;
  taskType: "personal" | "assigned" = "personal";
  isEditMode = false;
  isAssigning = signal(false);
  minDate = new Date();

  currentUser!: User;
  canCreateAssignedTasks = false;

  availableCompanies: string[] = [];
  filteredCompanies: Company[] = [];
  isFilteringCompanies: boolean = false;
  assignToRole: "SuperFiler" | "Vendor" | "BOTH" = "SuperFiler";
  selectedStates: string[] = [];
  adminRawStates: ("ALL" | string)[] = [];
  sfRawStates: ("ALL" | string)[] = [];
  sfStates: string[] = [];
  principalActivity: string = "all";
  principalActivityList: string[] = [];
  selectedActivitiesRaw: ("ALL" | string)[] = ["ALL"];
  selectedActivities: ("ALL" | string)[] = [];
  fromDate: Date | null = null;
  toDate: Date | null = null;
  selectedCompanies: CompanySlim[] = [];
  bulkAssignFilters: CompanyFilterRequest | null = null;
  attachments: FileAttachment[] = [];
  rawFiles: File[] = [];
  rawFilesForAction: File[] = [];
  attachmentsForAction: FileAttachment[] = [];
  isLoadingAttachments = signal(false);
  private routeSub: Subscription | undefined;
  fetchedAttachmentUrls: WritableSignal<string[]> = signal([]);
  permissions: TaskPermissions | undefined;
  private formsInitialized = false;
  private mockTodoService = new MockTodoService();
  private taskFormFactory: TasKCreationForms;
  constructor(
    private fb: FormBuilder,
    private todoService: TodoService,
    private rolePermissionsService: RolePermissionsService,
    private snackBar: SnackBarService,
    private cdr: ChangeDetectorRef,
    private datePipe: DatePipe,
    private personalTaskService: PersonalTaskService,
    private dialog: MatDialog,
    private router: Router,
    private route: ActivatedRoute,
    private location: Location,
    private loadingService: LoadingService,
    private todoStateService: TodoStateService,
    private secureStorage : SecureStorageService,
  ) {
    this.taskFormFactory = new TasKCreationForms(this.fb);
  }

  ngOnInit() {
    this.loadUserData();
    this.initializeForms();
    this.routeSub = this.route.data.subscribe((data) => {
      this.mode = data["mode"];
      this.isEditMode = this.mode.toLowerCase() === "edit";

      if (this.mode === "create") {
        this.editingTask = null;
        this.checkForModeChange();
      } else {
        // For 'edit' or 'view', we need the ID from paramMap
        this.route.paramMap.subscribe((params) => {
          const id = params.get("id");
          if (id) {
            this.editingTask = this.todoService.getTodoById(id);
            console.log(this.editingTask);
            if (!this.editingTask) {
              // this.snackBar.open("Task not found.", "Close", { duration: 3000 });
              // this.goBack();
              const recoveryCallback = () => {
                // Check cache again after a load completes
                this.editingTask = this.todoService.getTodoById(id);
                if (this.editingTask) {
                  this.checkForModeChange();
                  //this.snackBar.open("Task data recovered.", "Close", { duration: 1500 });
                }
              };

              this.todoService.loadPersonalTasks({} as any).subscribe({
                next: recoveryCallback,
                error: (err) =>
                  console.error(
                    "Failed to load personal tasks on edit/view reload.",
                    err
                  ),
              });
              this.todoService.loadAssignedTasks({} as any, "By Me").subscribe({
                next: recoveryCallback,
                error: (err) =>
                  console.error(
                    "Failed to load assigned tasks on edit/view reload.",
                    err
                  ),
              });
            } else {
              this.checkForModeChange();
            }
          }
        });
      }
    });
    //this.checkForModeChange();
  }

  ngAfterViewInit() {
    if (this.routeSub) {
      this.routeSub.unsubscribe();
    }
    this.cdr.detectChanges();
  }

  ngOnChanges(changes: SimpleChanges): void {}

  private ensureFormsInitialized(): void {
    if (!this.formsInitialized) {
      this.initializeForms();
    }
  }

  goBack() {
    this.location.back();
  }

  isEditOrCreateMode(): boolean {
    return this.mode === "create" || this.mode === "edit";
  }

  getHeaderTitle(): string {
    if (this.mode === "create") return "Add New Task";
    if (this.mode === "edit") return "Edit Task";
    if (this.mode === "view-assignee" && this.editingTask)
      return `Task Details`; // UPDATED
    return "Task Details";
  }

  getHeaderIcon(): string {
    if (this.mode === "create") return "post_add";
    if (this.mode === "edit") return "edit_note";
    return "manage_search";
  }

  checkForModeChange() {
    this.ensureFormsInitialized();
    this.isEditMode = this.mode === "edit";
    this.isAssigning.set(false);
    this.fetchedAttachmentUrls.set([]);
    this.rawFiles = [];
    this.rawFilesForAction = [];

    if (this.mode === "create") {
      console.log("Reset called");

      this.resetFormsToDefaults();
    } else if (this.editingTask) {
      this.patchFormsFromTask(this.editingTask);
      this.evaluatePermissions(this.editingTask);

      const masterId =
        this.editingTask.masterId ||
        (this.editingTask.type === "assigned" ? this.editingTask.id : null);
      if ((this.mode === "view-assignee" || this.mode === "edit") && masterId) {
        this.fetchTaskAttachments(masterId);
      }
    } else {
      this.evaluatePermissions(null);
    }
    this.cdr.markForCheck();
  }

  // --- Form Initialization/Patching/Reset ---
  initializeForms() {
    this.personalForm = this.taskFormFactory.createPersonalTaskForm();
    this.assignedForm = this.taskFormFactory.createAssignedTaskForm();
    this.actionForm = this.taskFormFactory.createActionForm();

    this.formsInitialized = true;
  }

  loadUserData() {
    if (!this.formsInitialized) this.initializeForms();
    this.currentUser = this.todoService.getCurrentUser();
    const permissions = this.rolePermissionsService.getTaskPermissions(
      this.currentUser,
      "assigned"
    );
    this.canCreateAssignedTasks = permissions.canCreate;
  }

  resetFormsToDefaults(): void {
    console.log("Come to reset");

    this.personalForm.reset({
      message: "",
      priorityLevel: "medium",
      targetDate: "",
    });
    this.assignedForm.reset({
      message: "",
      description: "",
      priorityLevel: "medium",
      targetDate: "",
    });
    this.actionForm.reset({ notes: "" });

    this.attachments = [];
    this.rawFiles = [];
    this.attachmentsForAction = [];
    this.rawFilesForAction = [];
    this.selectedCompanies = [];
    this.bulkAssignFilters = null;

    this.selectedTabIndex = 0;
    // this.taskType = "personal";
    // this.assignToRole = "SuperFiler";

    this.fetchedAttachmentUrls.set([]);
    this.isLoadingAttachments.set(false);

    const raw = this.secureStorage.getItem<string>("todo_active_tab_type",'session');
   // const raw = localStorage.getItem("todo_active_tab_type");

    // normalize: remove quotes if saved via JSON.stringify
    let normalized = (raw ?? "personal").trim().toLowerCase();
    if (normalized.startsWith('"') && normalized.endsWith('"')) {
      normalized = normalized.slice(1, -1).trim();
    }

    const requestedType: "personal" | "assigned" =
      normalized === "assigned" ? "assigned" : "personal";

    const role = (this.currentUser?.role || "").toLowerCase();
    const isConsumerOrVendor = role === "consumer" || role === "vendor";

    const effectiveType: "personal" | "assigned" = isConsumerOrVendor
      ? "personal"
      : requestedType;

    this.taskType = effectiveType;

    this.selectedTabIndex = effectiveType === "personal" ? 0 : 1;

    console.log("raw:", raw);
    console.log("normalized:", normalized);
    console.log("requestedType:", requestedType);
    console.log("role:", role);
    console.log("effectiveType:", effectiveType);

    this.cdr.markForCheck();
  }

  patchFormsFromTask(task: Todo): void {
    if (!task) return;
    this.taskType = task.type;
    this.selectedTabIndex = task.type === "personal" ? 0 : 1;
    const form =
      task.type === "personal" ? this.personalForm : this.assignedForm;
    form.patchValue({
      message: task.message ?? "",
      description: task.description ?? "",
      priorityLevel: task.priorityLevel ?? "medium",
      targetDate: task.targetDate ? new Date(task.targetDate) : "",
    });
    form.markAsPristine();

    this.actionForm.patchValue({
      notes: task.completionNotes || task.ignoredReason || "",
    });

    this.cdr.markForCheck();
  }

  fetchTaskAttachments(masterId: string) {
    this.isLoadingAttachments.set(true);
    this.fetchedAttachmentUrls.set([]);
    this.todoService.getTaskAttachmentUrls(masterId).subscribe({
      next: (urls) => {
        this.fetchedAttachmentUrls.set(urls);
        this.isLoadingAttachments.set(false);
        this.cdr.markForCheck();
      },
      error: (err) => {
        console.error("Error fetching attachments:", err);
        this.snackBar.showError("Failed to load attachments", "Close");
        this.isLoadingAttachments.set(false);
        this.cdr.markForCheck();
      },
    });
  }

  // --- Permission and Action Helpers ---

  evaluatePermissions(task: Todo | null | undefined) {
    if (!task) {
      this.permissions = undefined;
      return;
    }
    this.currentUser = this.todoService.getCurrentUser();

    const isTaskOwner = task.assignedBy === "You";
    const isAssignedToUser = task.assignedTo === this.currentUser.role;

    if (task.type === "personal") {
      this.permissions = this.rolePermissionsService.getTaskPermissions(
        this.currentUser,
        task.type,
        true,
        false
      );
    } else {
      this.permissions = this.rolePermissionsService.getTaskPermissions(
        this.currentUser,
        task.type,
        isTaskOwner,
        isAssignedToUser
      );
    }
  }

  isTaskOwner(): boolean {
    if (!this.editingTask) return false;
    if (this.editingTask.type === "personal") return true;
    return this.permissions?.canEdit || this.permissions?.canDelete || false;
  }

  canEditTask(task: Todo): boolean {
    if (!this.editingTask) return false;
    if (this.editingTask.type === "personal")
      return !!this.permissions?.canEdit;
    return !!this.permissions?.canEdit && this.editingTask.assignedBy === "You";
  }

  canTakeAction(task: Todo): boolean {
    if (!this.editingTask) return false;
    return (
      !!this.permissions?.canMarkDone &&
      this.editingTask.assignedTo === this.currentUser.role
    );
  }

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

  // --- Event Handlers ---
  onTabChange(index: number) {
    this.selectedTabIndex = index;
    this.taskType = index === 0 ? "personal" : "assigned";
    this.cdr.markForCheck();
  }

  onSelectedCompaniesChange(companies: CompanySlim[]) {
    this.selectedCompanies = companies;
    this.bulkAssignFilters = null;
  }
  onBulkFiltersChange(filters: CompanyFilterRequest | null) {
    this.bulkAssignFilters = filters;
    if (filters) {
      this.selectedCompanies = [];
    }
  }

  onAttachmentsChange(files: FileAttachment[]) {
    this.attachments = files;
  }
  onRawFilesChange(files: File[]) {
    this.rawFiles = files;
  }
  onAttachmentsChangeForAction(files: FileAttachment[]) {
    this.attachmentsForAction = files;
  }
  onRawFilesChangeForAction(files: File[]) {
    this.rawFilesForAction = files;
  }

  // --- Submission/Action Logic ---

  onSubmitPersonal() {
    if (this.personalForm.invalid) return;
    this.isAssigning.set(true);

    const formValue = this.personalForm.value;
    const todoData: Partial<Todo> = {
      message: formValue.message,
      priorityLevel: formValue.priorityLevel,
      targetDate: this.dateToString(formValue.targetDate),
      type: "personal",
    };
    this.loadingService.show("Processing personal task...");
    const obs =
      this.isEditMode && this.editingTask
        ? this.todoService.updateTodo(this.editingTask.id, todoData, null)
        : this.todoService.addTodo(
            { ...todoData } as Omit<Todo, "id" | "date" | "creationDate">,
            null
          );

    obs.subscribe({
      next: (savedTodo) => {
        // UPDATED
        this.isAssigning.set(false);
        this.snackBar.showSuccess(
          `Personal task ${
            this.isEditMode ? "updated" : "created"
          } successfully!`,
          "Close"
        );
        this.router.navigate(["apps/todo/list", "personal"]);
        this.loadingService.hide();
      },
      error: (err: any) => {
        this.loadingService.hide();
        this.isAssigning.set(false);
        this.snackBar.showError(
          `Error ${this.isEditMode ? "updating" : "creating"} personal task.`,
          "Close"
        );
        console.error(`Error processing personal task:`, err);
      },
    });
  }

  onSubmitAssigned() {
    if (this.assignedForm.invalid) {
      this.snackBar.show("Missing info: Title and Due Date", "Close");
      return;
    }

    if (this.currentUser.role === "SuperFiler" && !this.isEditMode) {
      if (
        !this.bulkAssignFilters &&
        (!this.selectedCompanies || this.selectedCompanies.length === 0)
      ) {
        this.snackBar.show(
          "Choose at least one company or use 'Assign to all filtered companies'",
          "Close"
        );
        return;
      }
    }

    this.isAssigning.set(true);
    const formValue = this.assignedForm.value;
    this.loadingService.show("Processing assigned task...");
    // --- 1. Edit Logic ---
    if (this.isEditMode && this.editingTask) {
      const todoData: Partial<Todo> = {
        message: formValue.message,
        description: formValue.description,
        priorityLevel: formValue.priorityLevel,
        targetDate: this.dateToString(formValue.targetDate),
      };
      const masterId = this.editingTask.masterId || this.editingTask.id;
      this.todoService.updateTodo(masterId, todoData, this.rawFiles).subscribe({
        next: (savedTodo) => {
          // UPDATED
          this.isAssigning.set(false);
          this.snackBar.showSuccess(
            "Assigned task updated successfully!",
            "Close",
            {
              duration: 2500,
            }
          );
          this.router.navigate(["/apps/todo/analytics", masterId]);
          this.loadingService.hide();
        },
        error: (err) => {
          this.loadingService.hide();
          this.isAssigning.set(false);
          this.snackBar.showError("Failed to update assigned task.", "Close", {
            duration: 3000,
          });
          console.error("Error updating assigned task:", err);
        },
      });
      return;
    }

    // --- 2. Creation Logic ---
    let assignedTargetName = "Consumer";
    if (this.currentUser.role === "Admin") {
      assignedTargetName = this.assignToRole;
    }

    const todoData: Omit<
      Todo,
      "id" | "date" | "creationDate" | "attachmentKeys"
    > = {
      message: formValue.message,
      description: formValue.description,
      priorityLevel: formValue.priorityLevel,
      targetDate: this.dateToString(formValue.targetDate)!,
      type: "assigned",
      assignedBy: this.currentUser.name,
      assignedTo: assignedTargetName,
      companies: [],
      attachments: [],
      completionStatus: false,
      edit: false,
    };

    this.todoService
      .addTodo(
        todoData,
        this.rawFiles,
        this.bulkAssignFilters,
        this.selectedCompanies
      )
      .subscribe({
        next: (savedTodo) => {
          // UPDATED
          this.isAssigning.set(false);
          let successMessage = "Task assigned successfully";
          if (this.currentUser.role === "Admin") {
            const targetLabel =
              assignedTargetName === "BOTH"
                ? "Superfilers and Vendors"
                : `${assignedTargetName}s`;
            successMessage = `Task assigned to ${targetLabel}`;
          } else if (this.bulkAssignFilters) {
            successMessage = `Task assigned to all filtered companies`;
          } else {
            successMessage = `Task assigned to ${this.selectedCompanies.length} companies`;
          }
          this.snackBar.showSuccess(successMessage, "Close");
          if (this.currentUser.role.toLowerCase() === "superfiler") {
            // Superfiler flow: Go to "By Me" view
            this.todoStateService.saveAssignedView("By Me");
          }
          this.router.navigate(["/apps/todo/list", "assigned"]);
          this.loadingService.hide();
        },
        error: (err) => {
          this.loadingService.hide();
          this.isAssigning.set(false);
          const detail =
            err.error?.details ||
            err.error?.message ||
            err.message ||
            "See console for details.";
          this.snackBar.showError(`Failed to  assigning task`, "Close");
          console.error("Error assigning task:", err);
        },
      });
  }

  onAction(action: "done" | "ignore") {
    if (!this.editingTask) return;

    this.isAssigning.set(true);

    const task = this.editingTask;
    const notes = this.actionForm.value.notes || "";

    this.loadingService.show("Updating task...");
    if (action === "done") {
      const obs =
        this.editingTask.type === "personal"
          ? this.todoService.markTaskDone(this.editingTask.id, undefined, null)
          : this.todoService.markTaskDone(
              this.editingTask.id,
              notes,
              this.rawFilesForAction
            );

      obs.subscribe({
        next: (savedTodo) => {
          // UPDATED
          this.isAssigning.set(false);
          this.snackBar.showSuccess("Task updated as completed", "Close");
          this.goBack(); // UPDATED
          this.loadingService.hide();
        },
        error: (err) => {
          this.loadingService.hide();
          this.isAssigning.set(false);
          this.snackBar.showError("Failed to update task", "Close");
          console.error("Error completing task:", err);
        },
      });
    } else if (action === "ignore") {
      this.todoService
        .ignoreTask(this.editingTask.id, notes, this.rawFilesForAction)
        .subscribe({
          next: (savedTodo) => {
            // UPDATED
            this.isAssigning.set(false);
            this.snackBar.showSuccess("Task updated as ignored.", "Close");
            this.goBack(); // UPDATED
            this.loadingService.hide();
          },
          error: (err) => {
            this.isAssigning.set(false);
            this.snackBar.showError("Failed to update task", "Close");
            console.error("Error ignoring task:", err);
            this.loadingService.hide();
          },
        });
    }
  }

  // --- Utility Functions (unchanged) ---
  private dateToString(date: Date | null | undefined): string | undefined {
    if (!(date instanceof Date) || isNaN(date.getTime())) return undefined;
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, "0");
    const d = String(date.getDate()).padStart(2, "0");
    return `${y}-${m}-${d}`;
  }

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
  isCompanyFilterActive(): boolean {
    return (
      this.sfStates.length > 0 ||
      this.selectedActivities.length > 0 ||
      !!this.fromDate ||
      !!this.toDate
    );
  }

  refreshAssignableUsers() {
    /* ... */
  }

  recomputeCompanies() {
    if (!this.isCompanyFilterActive()) {
      this.filteredCompanies = [];
      this.isFilteringCompanies = false;
      this.cdr.markForCheck();
      return;
    }

    this.isFilteringCompanies = true;
    this.filteredCompanies = [];

    this.mockTodoService
      .getCompaniesFiltered({
        states: this.sfStates,
        principalActivities: this.selectedActivities,
        fromDate: this.dateToString(this.fromDate),
        toDate: this.dateToString(this.toDate),
      })
      .subscribe({
        next: (results) => {
          this.filteredCompanies = results;
          this.isFilteringCompanies = false;
          this.cdr.markForCheck();
        },
        error: () => {
          this.snackBar.showError("Failed to filter companies", "Close");
          this.isFilteringCompanies = false;
          this.filteredCompanies = [];
          this.cdr.markForCheck();
        },
      });
  }

  downloadFile(url: string) {
    window.open(url, "_blank");
  }

  get messageCtrl() {
    return (
      this.selectedTabIndex === 0 ? this.personalForm : this.assignedForm
    ).get("message");
  }

  get targetDateCtrl() {
    return (
      this.selectedTabIndex === 0 ? this.personalForm : this.assignedForm
    ).get("targetDate");
  }

  get activeForm(): FormGroup {
    return this.selectedTabIndex === 0 ? this.personalForm : this.assignedForm;
  }
}
