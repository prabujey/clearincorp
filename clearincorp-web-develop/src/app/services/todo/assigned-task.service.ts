import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from 'src/environments/environment';
import {
  PagedResponse,
  TaskItemDto,
  AssignedTaskItemFilter,
  AssignedTaskMasterFilter,
  SortToken,
  AssignedTaskMasterDto,
  TaskStatusUpdateRequest,
  BulkAdminAssignmentRequest,
  BulkAssignmentByCompanyFilterRequest,
  BulkSuperFilerAssignmentRequest,
  TaskAnalyticsResponse,
  AssignedTaskStatus
} from 'src/app/models/todo-models/assigned-task.model';

@Injectable({ providedIn: 'root' })
export class AssignedTaskService {
  private tasksUrl = `${environment.apiBaseUrl}/tasks`;
  private assignedTaskUrl = `${environment.apiBaseUrl}/tasks/assignedTask`;

  constructor(private http: HttpClient) {}

  /**
   * Helper function to create FormData for multipart requests
   */
  private createRequestFormData(requestDto: object, files: File[] | null): FormData {
    const formData = new FormData();
    const requestJson = JSON.stringify(requestDto);
    formData.append('request', new Blob([requestJson], { type: 'application/json' }));

    if (files) {
      files.forEach(file => {
        formData.append('files', file, file.name);
      });
    }
    return formData;
  }

  /**
   * NEW: Helper function just for the status update
   */
  private createStatusUpdateFormData(requestDto: TaskStatusUpdateRequest, files: File[] | null): FormData {
    const formData = new FormData();
    const requestJson = JSON.stringify(requestDto);
    formData.append('request', new Blob([requestJson], { type: 'application/json' }));

    if (files) {
      files.forEach(file => {
        // Assuming the backend expects 'files' for this endpoint too
        formData.append('files', file, file.name);
      });
    }
    return formData;
  }


  // --- CREATE (POST) Endpoints ---

  createTaskByAdmin(req: BulkAdminAssignmentRequest, files: File[] | null): Observable<AssignedTaskMasterDto> {
    const formData = this.createRequestFormData(req, files);
    return this.http.post<AssignedTaskMasterDto>(`${this.tasksUrl}/admin/bulk-assign`, formData);
  }

  createTaskBySuperFilerBulk(req: BulkAssignmentByCompanyFilterRequest, files: File[] | null): Observable<AssignedTaskMasterDto> {
    const formData = this.createRequestFormData(req, files);
    return this.http.post<AssignedTaskMasterDto>(`${this.tasksUrl}/SuperFiler/bulk-assign`, formData);
  }

  createTaskBySuperFilerSelected(req: BulkSuperFilerAssignmentRequest, files: File[] | null): Observable<AssignedTaskMasterDto> {
    const formData = this.createRequestFormData(req, files);
    return this.http.post<AssignedTaskMasterDto>(`${this.tasksUrl}/SuperFiler/selected-assign`, formData);
  }

  // --- READ (GET) Endpoints ---

  searchAssignedTaskItems(filter: AssignedTaskItemFilter, page = 0, size = 10, sort: SortToken[] = []):
    Observable<PagedResponse<TaskItemDto>> {
    let params = new HttpParams()
      .set('page', page)
      .set('size', size);

    if (filter.assigneeId != null) params = params.set('assigneeId', filter.assigneeId);
    if (filter.masterId != null) params = params.set('masterId', filter.masterId);
    if (filter.companyId != null) params = params.set('companyId', filter.companyId);
    if (filter.status != null) params = params.set('status', filter.status);
    if (filter.dueDateFrom) params = params.set('dueDateFrom', filter.dueDateFrom);
    if (filter.dueDateTo) params = params.set('dueDateTo', filter.dueDateTo);
    if (filter.search) params = params.set('search', filter.search);
    if (filter.priority != null) params = params.set('priority', filter.priority);
    if (filter.completed != null) params = params.set('completed', filter.completed);

    sort.forEach(s => {
      const v = typeof s === 'string' ? s : (s.dir ? `${s.field},${s.dir}` : s.field);
      if (v?.trim()) params = params.append('sort', v.trim());
    });

    return this.http.get<PagedResponse<TaskItemDto>>(this.tasksUrl, { params });
  }

  searchAssignedTaskMasters(filter: AssignedTaskMasterFilter, page = 0, size = 10, sort: SortToken[] = []):
    Observable<PagedResponse<AssignedTaskMasterDto>> {

    let params = new HttpParams()
      .set('page', page)
      .set('size', size);

    if (filter.createdById != null) params = params.set('createdById', filter.createdById);
    if (filter.priority) params = params.set('priority', filter.priority);
    if (filter.dueDateFrom) params = params.set('dueFrom', filter.dueDateFrom);
    if (filter.dueDateTo) params = params.set('dueTo', filter.dueDateTo);
    if (filter.search) params = params.set('query', filter.search);

    sort.forEach(s => {
      const v = typeof s === 'string' ? s : (s.dir ? `${s.field},${s.dir}` : s.field);
      if (v?.trim()) params = params.append('sort', v.trim());
    });

    return this.http.get<PagedResponse<AssignedTaskMasterDto>>(this.assignedTaskUrl, { params });
  }

  /**
   * Corresponds to: GET /tasks/api/directory-presigned-urls
   * @param masterId The UUID of the master task.
   */
  getPresignedUrls(masterId: string): Observable<string[]> {
    const headers = new HttpHeaders().set("X-Skip-Loading", "true");
    const params = new HttpParams().set('masterId', masterId);
    return this.http.get<string[]>(`${this.tasksUrl}/api/directory-presigned-urls`, { params , headers });
  }

  /**
   * NEW: Corresponds to GET /tasks/{masterId}
   */
  getTaskAnalytics(masterId: string, status: AssignedTaskStatus | null, page: number, size: number): Observable<TaskAnalyticsResponse> {
    let params = new HttpParams()
      .set('page', page)
      .set('size', size);
    
    if (status !== null && status !== undefined) {
      params = params.set('status', status);
    }

    return this.http.get<TaskAnalyticsResponse>(`${this.tasksUrl}/analytics/${masterId}`, { params });
  }


  // --- UPDATE (PUT/PATCH) Endpoints ---

  /**
   * UPDATED: Now consumes multipart/form-data as per backend controller
   */
  updateAssignedTaskMaster(masterId: string, dto: Partial<AssignedTaskMasterDto>, files: File[] | null): Observable<AssignedTaskMasterDto> {
    const updatePayload = { ...dto, id: masterId };
    // Use form-data helper
    const formData = this.createRequestFormData(updatePayload, files);
    return this.http.put<AssignedTaskMasterDto>(this.tasksUrl, formData);
  }

  /**
   * UPDATED: Changed from PATCH to PUT and now sends multipart/form-data
   * Corresponds to: PUT /tasks/status
   */
  updateTaskStatus(req: TaskStatusUpdateRequest, files: File[] | null): Observable<void> {
    const formData = this.createStatusUpdateFormData(req, files);
    // Returns 204 No Content, so we expect a void response
    return this.http.put<void>(`${this.tasksUrl}/status`, formData);
  }

  // --- DELETE Endpoint ---

  deleteAssignedTaskMaster(masterId: string): Observable<void> {
    const params = new HttpParams().set('id', masterId);
    return this.http.delete<void>(`${this.tasksUrl}/master`, { params });
  }
}
