import { Priority, SortToken } from './personal-task.model';

// --- BASE REQUEST ---
// This is an internal type, not exported, to avoid repetition
export interface BaseAssignmentRequest {
    taskTitle: string;
    description?: string;
    createdById: number;
    priority: Priority; // Use the string enum 'LOW', 'MEDIUM', 'HIGH'
    dueDate: string; // YYYY-MM-DD
}

// --- ADMIN ---
export interface BulkAdminAssignmentRequest extends BaseAssignmentRequest {
    // FIX: Changed from assigneeRole: string.
    // The backend expects the array of IDs under this field name.
    assigneeRole: number[]; 
    roleIds?: number[]; // Kept for safety, but likely unused
}

// --- SUPERFILER (Bulk by Filter) ---
export interface CompanyFilterRequest {
    principalActivity?: string[];
    states?: string[];
    effectiveFrom?: string; // YYYY-MM-DD
    effectiveTo?: string; // YYYY-MM-DD
    search?: string;
}

export interface BulkAssignmentByCompanyFilterRequest extends BaseAssignmentRequest {
    filter: CompanyFilterRequest;
}

// --- SUPERFILER (Selected) ---
export interface BulkSuperFilerAssignmentRequest extends BaseAssignmentRequest {
    targets: CompanySlim[];
}

// --- COMPANY SLIM DTO (for search results) ---
export interface CompanySlim {
    state: string;
    companyId: number;
    companyName: string;
    loginUserId: number;
}


// --- MASTER DTO (Response) ---
export interface AssignedTaskMasterDto {
    id: string; // This is the MasterTask UUID
    taskTitle: string;
    description?: string;
    createdById: number;
    priority: Priority;
    dueDate?: string;
    createdOn?: string;
    updatedOn?: string;
    attachmentKeys: string[]; // List of S3 keys
}

// --- ITEM DTO (for 'For Me' list) ---
export interface TaskItemDto {
    taskId: string; // This is the TaskItem UUID
    assigneeId: number;
    status: 'PENDING' | 'DONE' | 'IGNORED';
    notes?: string; // Notes for completion or ignore
    companyId?: number;
    masterId: string; // MasterTask UUID
    taskTitle: string;
    description?: string;
    priority: Priority;
    dueDate: string;
    createdOn: string;
    updatedOn: string;
}

// --- STATUS UPDATE ---
export enum AssignedTaskStatus {
    PENDING = 'PENDING',
    DONE = 'DONE',
    IGNORED = 'IGNORED',
}

export interface TaskStatusUpdateRequest {
    taskId: string; // ADDED: The ID of the TaskItem to update
    status: AssignedTaskStatus;
    notes?: string;
    // attachmentKeys are handled by the 'files' part of the multipart request
}

// --- FILTERS ---
export interface AssignedTaskItemFilter {
    assigneeId?: number;
    companyId?: number;
    masterId?: string; // Added to match backend
    status?: AssignedTaskStatus;
    dueDateFrom?: string;
    dueDateTo?: string;
    search?: string;
    priority?: Priority;
    completed?: boolean;
}

export interface AssignedTaskMasterFilter {
    createdById?: number;
    priority?: Priority;
    dueDateFrom?: string;
    dueDateTo?: string;
    search?: string;
}

// --- NEW: ANALYTICS MODELS ---

/**
 * Represents the status counts for a master task.
 * (com.trivine.llc.api.dto.TaskStatusCounts)
 */
export interface TaskStatusCounts {
    pending: number;
    done: number;
    ignored: number;
}

/**
 * Represents the detailed info for a single assignee in the analytics view.
 * (com.trivine.llc.api.dto.AssignedTaskAnalyticsDto)
 */
export interface AssignedTaskAnalyticsDto {
    taskId: string;
    notes?: string;
    status: 'PENDING' | 'DONE' | 'IGNORED';
    createdOn: string; // ISO DateTime string
    updatedOn: string; // ISO DateTime string
    attachmentKeys: string[];
    assigneeFirstName: string;
    assigneeLastName: string;
    assigneeEmail: string;
    companyName?: string;
    state?: string;
}

/**
 * The wrapper response for the analytics endpoint.
 * (com.trivine.llc.api.dto.TaskAnalyticsResponse)
 */
export interface TaskAnalyticsResponse {
    tasks: PagedResponse<AssignedTaskAnalyticsDto>;
    statusCounts: TaskStatusCounts;
}


// --- COMMON ---
export type { SortToken };
export interface PagedResponse<T> {
    content: T[];
    page: number;
    size: number;
    totalElements: number;
    totalPages: number;
    last: boolean;
}
