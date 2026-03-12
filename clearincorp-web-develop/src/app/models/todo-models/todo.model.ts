import {
  FormBuilder,
  FormGroup,
  FormArray,
  Validators,
  FormControl,
  AbstractControl,
  ValidationErrors
} from '@angular/forms';
import { DESCRIPTION_MAX, NOTES_MAX, TITLE_PATTERN } from '../regexpattern';

export type Priority = "low" | "medium" | "high";
export type TaskType = "personal" | "assigned";

export interface FileAttachment {
  id: string;
  name: string;
  size: number;
  type: string;
  url: string;
  uploadDate: Date;
}

export interface User {
  id: string;
  name: string;
  role: string;
}

export interface Todo {
  id: string;
  masterId?: string;
  message: string;
  description?: string;
  targetDate?: string;
  priorityLevel: Priority;
  completionStatus: boolean;
  completionDate?: Date;
  completionNotes?: string;
  completionFiles?: FileAttachment[];
  edit: boolean;
  date: Date;
  type: "personal" | "assigned";
  assignedBy?: string;
  assignedTo?: string;
  companies?: string[];
  attachments?: FileAttachment[];
  states?: string[];
  principalActivity?: string; // for Superfiler filtering UI (optional)
  formationDate?: string;
  ignoredStatus?: boolean;
  ignoredReason?: string;
  ignoredAt?: string;         // ISO string
  ignoredByName?: string;
  creationDate: string | number | Date;
  attachmentKeys?: string[]; 
  companyDisplayName?: string;
  companyId?: number; 
  assigneeCount?: number; // NEW: For "By Me" view to check if it's a bulk assign
}


export interface TaskPermissions {
  canCreate: boolean;
  canEdit: boolean;
  canDelete: boolean;
  canAssign: boolean;
  canMarkDone: boolean;
  canIgnore: boolean;
}

export type SortBy = "latest" | "oldest" | "dueToday";

export interface TaskFilters {
  priority?: "low" | "medium" | "high" | "";
  company?: string;
  status?: "pending" | "completed" | "ignored" | "due-today" | "overdue" | "";
  assignedTo?: string;
  assigneeRole?: "all" | "superfiler" | "vendor"; // Admin list filter
  states?: string[]; // Admin list filter
  assignedSource?: "all" | "by-admin" | "to-consumer";
  sortBy: SortBy;
  dueDateFrom?: string;
  dueDateTo?: string;
  search?: string;
  // assignedTaskView?: "For Me" | "By Me";
  masterId?: string; // Added to align with backend API
}

export interface TaskViewData {
  task: Todo;
  currentUser: User;
  permissions: TaskPermissions;
  isTaskOwner: boolean;
  isAssignedToUser: boolean;
}

export class TasKCreationForms {
  constructor(private fb: FormBuilder) {}

 createPersonalTaskForm(): FormGroup {
    const today = this.getToday();

    return this.fb.group({
      message: [
        '',
        [
          Validators.required,
          Validators.minLength(3),
          Validators.maxLength(200),
          Validators.pattern(TITLE_PATTERN),
        ],
      ],
      priorityLevel: [
        'medium', Validators.required,
       
      ],
      targetDate: [
      null, Validators.required,
      ],
    });
  }

    createAssignedTaskForm(): FormGroup {
    const today = this.getToday();

    return this.fb.group({
      message: [
        '',
        [
          Validators.required,
          Validators.minLength(3),
          Validators.maxLength(200),
          Validators.pattern(TITLE_PATTERN),
        ],
      ],
      description: [
        '',
        [ Validators.required,
          Validators.maxLength(DESCRIPTION_MAX),
        ],
      ],
      priorityLevel: [
        'medium', Validators.required,
       
      ],
      targetDate: [
         null, Validators.required,
      ],
    });
  }

  createActionForm(): FormGroup {
    return this.fb.group({
      notes: [
        '',
        [ Validators.required,
          Validators.maxLength(NOTES_MAX),
        ],
      ],
    });
  }

   private getToday(): Date {
    const now = new Date();
    return new Date(now.getFullYear(), now.getMonth(), now.getDate());
  }
}

