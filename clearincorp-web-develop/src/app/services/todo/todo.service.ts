import { Injectable } from "@angular/core";
import {
  Todo,
  User,
  FileAttachment,
  TaskFilters,
} from "src/app/models/todo-models/todo.model";
import { TokenService } from "../token/token.service";
import { ErrorHandlerUtil } from "src/app/shared/error-handler.util";
import { HttpClient } from "@angular/common/http";
import { map, Observable } from "rxjs";
import {
  PersonalTaskCreateDto,
  PersonalTaskFilter,
  PersonalTaskResponseDto,
  PersonalTaskUpdateDto,
  Priority,
  SortToken,
} from "src/app/models/todo-models/personal-task.model";
import {
  AssignedTaskItemFilter,
  AssignedTaskMasterDto,
  AssignedTaskMasterFilter,
  AssignedTaskStatus,
  BaseAssignmentRequest,
  BulkAdminAssignmentRequest,
  BulkAssignmentByCompanyFilterRequest,
  BulkSuperFilerAssignmentRequest,
  CompanyFilterRequest,
  CompanySlim,
  TaskItemDto,
  TaskStatusUpdateRequest,
  TaskAnalyticsResponse,
} from "src/app/models/todo-models/assigned-task.model";
import { PersonalTaskService } from "./personal-task.service";
import { AssignedTaskService } from "./assigned-task.service";
import { CompanyService } from "./company.service"; // Import new service
import { PagedResponse } from "src/app/models/todo-models/personal-task.model";
import { SecureStorageService } from "src/app/services/storage/secure-storage.service";
@Injectable({
  providedIn: "root",
})
export class TodoService {
  private mockUsers: any[] = [
    { id: "1", name: "Admin", role: "admin" },
    { id: "2", name: "Superfiler", role: "superfiler" },
    { id: "3", name: "Vendor ", role: "vendor" },
    { id: "4", name: "Consumer", role: "consumer" },
  ];
  private userStates: Record<string, string[]> = {
    "Superfiler User": ["CA", "NY", "TX"],
    "Vendor User": ["CA", "FL"],
  };

  private personalTodosCache: Todo[] = [];
  private assignedTodosCache: Todo[] = [];
  private _currentUser: User = this.mockUsers[0] as User;
  private companyMapCache: Record<number, string> | null = null;

  public mapPersonalTaskApiParams = this._mapToPersonalTaskApiParams.bind(this);

  constructor(
    private tokenService: TokenService,
    private personalTaskService: PersonalTaskService,
    private http: HttpClient,
    private assignedTaskService: AssignedTaskService,
    private companyService: CompanyService,
    private errorHandler: ErrorHandlerUtil,
    private secureStorage: SecureStorageService
  ) {}

  searchCompanies(
    filter: CompanyFilterRequest,
    page: number = 0,
    size: number = 20
  ): Observable<PagedResponse<CompanySlim>> {
    return this.companyService.searchCompanies(filter, page, size);
  }

  getTaskAttachmentUrls(masterId: string): Observable<string[]> {
    return this.assignedTaskService.getPresignedUrls(masterId);
  }

  // NEW: Pass-through for analytics
  getTaskAnalytics(
    masterId: string,
    status: AssignedTaskStatus | null,
    page: number,
    size: number
  ): Observable<TaskAnalyticsResponse> {
    return this.assignedTaskService.getTaskAnalytics(
      masterId,
      status,
      page,
      size
    );
  }

  private mapToTodo(dto: PersonalTaskResponseDto): Todo {
    return {
      id: dto.id,
      message: dto.taskTitle,
      description: undefined,
      targetDate: dto.dueDate,
      priorityLevel: dto.priority.toLowerCase() as Todo["priorityLevel"],
      completionStatus: dto.completed,
      ignoredStatus: false,
      edit: false,
      date: new Date(dto.createdOn || dto.updatedOn || Date.now()),
      type: "personal",
      creationDate: dto.createdOn || new Date().toISOString(),
      attachmentKeys: [],
    };
  }

  private mapTaskItemDtoToTodo(dto: TaskItemDto): Todo {
    const priorityLevel = (
      dto.priority || "medium"
    ).toLowerCase() as Todo["priorityLevel"];
    const status = dto.status;
    const isCompleted = status === "DONE";
    const isIgnored = status === "IGNORED";
    const currentUserRole = this.getCurrentUser().role;

    return {
      id: dto.taskId, // This is the TaskItem ID
      masterId: dto.masterId, // This is the MasterTask ID
      message: dto.taskTitle,
      description: dto.description,
      targetDate: dto.dueDate,
      priorityLevel: priorityLevel,
      completionStatus: isCompleted,
      ignoredStatus: isIgnored,
      completionNotes: dto.notes, // Map notes
      edit: false,
      date: new Date(dto.createdOn || dto.updatedOn || Date.now()),
      type: "assigned",
      assignedBy: "ClearInCorp",
      assignedTo: currentUserRole,
      companyId: dto.companyId,
      companies: [],
      attachments: [], // We need S3 keys to fetch these
      attachmentKeys: [], // Note: Item DTO doesn't have keys, only master
      creationDate: dto.createdOn || new Date().toISOString(),
      assigneeCount: 1, // "For Me" tasks always represent a single item
    };
  }

  private mapTaskMasterDtoToTodo(dto: AssignedTaskMasterDto): Todo {
    const priorityLevel = (
      dto.priority || "medium"
    ).toLowerCase() as Todo["priorityLevel"];

    return {
      id: dto.id, // This is the MasterID
      masterId: dto.id,
      message: dto.taskTitle,
      description: dto.description,
      targetDate: dto.dueDate,
      priorityLevel: priorityLevel,
      completionStatus: false,
      ignoredStatus: false,
      edit: false,
      date: new Date(dto.createdOn || dto.updatedOn || Date.now()),
      type: "assigned",
      assignedBy: "You", // This view is "By Me"
      assignedTo: "Multiple",
      companies: [],
      attachments: [],
      attachmentKeys: dto.attachmentKeys || [], // Map attachment keys
      creationDate: dto.createdOn || new Date().toISOString(),
      // MOCKED: Assume master tasks have > 1 assignee for demo logic
      // In a real app, the backend DTO should provide this count
      assigneeCount: 10,
    };
  }
  private mapPriorityToNumber(priority: Todo["priorityLevel"]): number {
    const priorityMap: Record<Todo["priorityLevel"], number> = {
      low: 0,
      medium: 1,
      high: 2,
    };
    return priorityMap[priority] ?? 1;
  }

  private mapRoleToRoleId(roleName: string): number | undefined {
    const roleMap: Record<string, number> = {
      superfiler: 3,
      vendor: 4,
    };
    return roleMap[roleName.toLowerCase()];
  }

  private getTodayDateString(): string {
    const today = new Date();
    const y = today.getFullYear();
    const m = String(today.getMonth() + 1).padStart(2, "0");
    const d = String(today.getDate()).padStart(2, "0");
    return `${y}-${m}-${d}`;
  }

  private _mapToPersonalTaskApiParams(filters: TaskFilters): {
    filter: PersonalTaskFilter;
    sort: SortToken[];
  } {
    const user = this.getCurrentUser();
    const fmtLocal = (d: Date) => {
      const y = d.getFullYear();
      const m = String(d.getMonth() + 1).padStart(2, "0");
      const day = String(d.getDate()).padStart(2, "0");
      return `${y}-${m}-${day}`; // yyyy-MM-dd (no UTC drift)
    };

    const today = new Date();
    const yesterday = new Date(
      today.getFullYear(),
      today.getMonth(),
      today.getDate() - 1
    );

    const apiFilter: PersonalTaskFilter = {
      loginUserId: parseInt(user.id, 10) || 1,
      priority:
        (filters.priority ?? "") === ""
          ? undefined
          : (filters.priority!.toUpperCase() as Priority),
      dueDateFrom: filters.dueDateFrom || undefined,
      dueDateTo: filters.dueDateTo || undefined,
      search: filters.search || undefined,
      completed:
        filters.status === "completed" || filters.status === "ignored"
          ? true
          : filters.status === "pending"
          ? false
          : undefined,
    };
    if (filters.status === "overdue") {
      apiFilter.completed = false;
      apiFilter.dueDateTo = fmtLocal(yesterday);
    }
    let apiSort: SortToken[] = [];
    switch (filters.sortBy) {
      case "oldest":
        apiSort.push({ field: "createdOn", dir: "asc" });
        break;
      case "dueToday":
        const todayStr = this.getTodayDateString();
        apiFilter.dueDateFrom = todayStr;
        apiFilter.dueDateTo = todayStr;
        apiSort.push({ field: "createdOn", dir: "asc" }); // As requested
        break;
      case "latest":
      default:
        apiSort.push({ field: "createdOn", dir: "desc" }); // Default to Latest
        break;
    }
    return { filter: apiFilter, sort: apiSort };
  }
  private _mapToTaskItemApiParams(filters: TaskFilters): {
    filter: AssignedTaskItemFilter;
    sort: SortToken[];
  } {
    const user = this.getCurrentUser();
    const fmtLocal = (d: Date) => {
      const y = d.getFullYear();
      const m = String(d.getMonth() + 1).padStart(2, "0");
      const day = String(d.getDate()).padStart(2, "0");
      return `${y}-${m}-${day}`; // yyyy-MM-dd (no UTC drift)
    };

    const today = new Date();
    const yesterday = new Date(
      today.getFullYear(),
      today.getMonth(),
      today.getDate() - 1
    );
    const apiFilter: AssignedTaskItemFilter = {
      assigneeId: parseInt(user.id, 10) || 0,
      dueDateFrom: filters.dueDateFrom || undefined,
      dueDateTo: filters.dueDateTo || undefined,
      search: filters.search || undefined,
      masterId: filters.masterId || undefined, // Pass masterId
    };
    if (filters.status === "completed") {
      apiFilter.status = AssignedTaskStatus.DONE;
    } else if (filters.status === "ignored") {
      apiFilter.status = AssignedTaskStatus.IGNORED; // UPDATED ENUM
    } else if (filters.status === "pending") {
      apiFilter.status = AssignedTaskStatus.PENDING; // UPDATED ENUM
    } else if (filters.status === "overdue") {
      apiFilter.status = AssignedTaskStatus.PENDING;
      apiFilter.dueDateTo = fmtLocal(yesterday);
    }
    if (filters.priority) {
      apiFilter.priority = filters.priority.toUpperCase() as Priority;
    }
    let apiSort: SortToken[] = [];
    switch (filters.sortBy) {
      case "oldest":
        apiSort.push({ field: "createdOn", dir: "asc" });
        break;
      case "dueToday":
        const todayStr = this.getTodayDateString();
        apiFilter.dueDateFrom = todayStr;
        apiFilter.dueDateTo = todayStr;
        apiSort.push({ field: "createdOn", dir: "asc" }); // As requested
        break;
      case "latest":
      default:
        apiSort.push({ field: "createdOn", dir: "desc" }); // Default to Latest
        break;
    }
    return { filter: apiFilter, sort: apiSort };
  }

  private _mapToTaskMasterApiParams(filters: TaskFilters): {
    filter: AssignedTaskMasterFilter;
    sort: SortToken[];
  } {
    const user = this.getCurrentUser();
    const apiFilter: AssignedTaskMasterFilter = {
      createdById: parseInt(user.id, 10) || 0,
      dueDateFrom: filters.dueDateFrom || undefined,
      dueDateTo: filters.dueDateTo || undefined,
      search: filters.search || undefined,
    };
    if (filters.priority) {
      apiFilter.priority = filters.priority.toUpperCase() as Priority;
    }
    let apiSort: SortToken[] = [];
    switch (filters.sortBy) {
      case "oldest":
        apiSort.push({ field: "createdOn", dir: "asc" });
        break;
      case "dueToday":
        const todayStr = this.getTodayDateString();
        apiFilter.dueDateFrom = todayStr;
        apiFilter.dueDateTo = todayStr;
        apiSort.push({ field: "createdOn", dir: "asc" }); // As requested
        break;
      case "latest":
      default:
        apiSort.push({ field: "createdOn", dir: "desc" }); // Default to Latest
        break;
    }
    return { filter: apiFilter, sort: apiSort };
  }

  /**
   * UPDATED: Now returns PagedResponse<Todo>
   */
  public loadPersonalTasks(
    filters: TaskFilters,
    page: number = 0,
    size: number = 10
  ): Observable<PagedResponse<Todo>> {
    const { filter: apiFilter, sort: apiSort } =
      this._mapToPersonalTaskApiParams(filters);

    // Type response as 'any' to handle the unexpected nested structure
    return this.personalTaskService
      .searchPersonalToDo(apiFilter, page, size, apiSort)
      .pipe(
        map((response: any) => {
          // **FIX:** Get the page data from the nested 'pageData' object
          const pageData = response.pageData;
          if (!pageData || !Array.isArray(pageData.content)) {
            console.error(
              "Invalid personal task response structure. Missing 'pageData' or 'pageData.content'.",
              response
            );
            // Return a default empty page to prevent further crashes
            return {
              content: [],
              page: 0,
              size: size,
              totalElements: 0,
              totalPages: 0,
              last: true,
              stats: {},
            };
          }

          const todos = pageData.content.map(this.mapToTodo.bind(this));
          this.personalTodosCache = todos;

          // **FIX:** Reconstruct the PagedResponse object that the list component expects
          return {
            content: todos,
            page: pageData.page,
            size: pageData.size,
            totalElements: pageData.totalElements,
            totalPages: pageData.totalPages,
            last: pageData.last,

            // **NEW:** Pass the stats object through for the UI to use
            stats: response.stats,
          };
        })
      );
  }

  /**
   * UPDATED: Now returns PagedResponse<Todo>
   */
  public loadAssignedTasks(
    filters: TaskFilters,
    assignedView: "For Me" | "By Me",
    page: number = 0,
    size: number = 10 // Defaulted to 10
  ): Observable<PagedResponse<Todo>> {
    // const user = this.getCurrentUser();
    // const userRole = user.role.toLowerCase();
    // let viewType: 'For Me' | 'By Me' = 'For Me';

    // if (userRole === 'admin') {
    //   viewType = filters.assignedTaskView || 'By Me';
    // } else if (userRole === 'superfiler') {
    //   viewType = filters.assignedTaskView || 'For Me';
    // } else {
    //   // Default for other roles (Vendor, Consumer)
    //   viewType = 'For Me';
    // }

    // if (viewType === 'By Me') {
    //   const { filter: apiFilter, sort: apiSort } = this._mapToTaskMasterApiParams(filters);
    //   return this.assignedTaskService.searchAssignedTaskMasters(apiFilter, page, size, apiSort).pipe(
    //     map(pagedResponse => {
    //       const todos = pagedResponse.content.map(this.mapTaskMasterDtoToTodo.bind(this));
    //       this.assignedTodosCache = todos; // Cache only the current page
    //       return {
    //         ...pagedResponse,
    //         content: todos
    //       };
    //     })
    //   );
    // } else {
    //   // This is 'For Me'
    //   const { filter: apiFilter, sort: apiSort } = this._mapToTaskItemApiParams(filters);

    //   // UPDATED: Handle nested response for 'For Me' tasks
    //   return this.assignedTaskService.searchAssignedTaskItems(apiFilter, page, size, apiSort).pipe(
    //     map((response: any) => { // Type as 'any' to read nested properties

    //       // **FIX:** Get the page data from the nested 'pageData' object
    //       const pageData = response.pageData;
    //       if (!pageData || !Array.isArray(pageData.content)) {
    //           console.error("Invalid assigned task ('For Me') response structure. Missing 'pageData' or 'pageData.content'.", response);
    //           // Return a default empty page to prevent further crashes
    //           return { content: [], page: 0, size: size, totalElements: 0, totalPages: 0, last: true, stats: {} };
    //       }

    //       const todos = pageData.content.map(this.mapTaskItemDtoToTodo.bind(this));
    //       this.assignedTodosCache = todos; // Cache only the current page

    //       // **FIX:** Reconstruct the PagedResponse object
    //       return {
    //         content: todos,
    //         page: pageData.page,
    //         size: pageData.size,
    //         totalElements: pageData.totalElements,
    //         totalPages: pageData.totalPages,
    //         last: pageData.last,

    //         // **NEW:** Pass the stats object through for the UI to use
    //         stats: response.stats
    //       };
    //     })
    //   );
    if (assignedView === "By Me") {
      const { filter: apiFilter, sort: apiSort } =
        this._mapToTaskMasterApiParams(filters);
      return this.assignedTaskService
        .searchAssignedTaskMasters(apiFilter, page, size, apiSort)
        .pipe(
          map((pagedResponse) => {
            const todos = pagedResponse.content.map(
              this.mapTaskMasterDtoToTodo.bind(this)
            );
            this.assignedTodosCache = todos; // Cache only the current page
            return {
              ...pagedResponse,
              content: todos,
            };
          })
        );
    } else {
      // This is 'For Me'
      const { filter: apiFilter, sort: apiSort } =
        this._mapToTaskItemApiParams(filters);

      return this.assignedTaskService
        .searchAssignedTaskItems(apiFilter, page, size, apiSort)
        .pipe(
          map((response: any) => {
            const pageData = response.pageData;
            if (!pageData || !Array.isArray(pageData.content)) {
              console.error(
                "Invalid assigned task ('For Me') response structure. Missing 'pageData' or 'pageData.content'.",
                response
              );
              return {
                content: [],
                page: 0,
                size: size,
                totalElements: 0,
                totalPages: 0,
                last: true,
                stats: {},
              };
            }

            const todos = pageData.content.map(
              this.mapTaskItemDtoToTodo.bind(this)
            );
            this.assignedTodosCache = todos; // Cache only the current page

            return {
              content: todos,
              page: pageData.page,
              size: pageData.size,
              totalElements: pageData.totalElements,
              totalPages: pageData.totalPages,
              last: pageData.last,
              stats: response.stats,
            };
          })
        );
    }
  }

  // Synchronous getter uses the local cache
  getTodos(): Todo[] {
    return [...this.personalTodosCache, ...this.assignedTodosCache];
  }

  getUsers() {
    return this.mockUsers as User[];
  }

  // Synchronous getter
  getCurrentUser(): User {
    const userId = this.secureStorage.getLoginUserId();
    const userData = this.secureStorage.getLoggedInUserData();
    const role = this.tokenService.getRole();
    let firstName = "User";
    if (userData) {
      try {
        const parsed = JSON.parse(userData);
        firstName = parsed.firstName || "User";
      } catch (error) {
        console.error("Error parsing userData:", error);
      }
    }
    let normalizedRole = (role || "Consumer").toLowerCase();
    if (normalizedRole === "admin") normalizedRole = "Admin";
    else if (normalizedRole === "superfiler") normalizedRole = "SuperFiler";
    else if (normalizedRole === "vendor") normalizedRole = "Vendor";
    else if (normalizedRole === "consumer") normalizedRole = "Consumer";
    const currentUser: User = {
      id: userId || "0",
      name: firstName,
      role: normalizedRole,
    };
    this._currentUser = currentUser;
    return this._currentUser;
  }

  setCurrentUser(user: User) {
    this._currentUser = user;
  }

  getUsersByRole(role: User["role"]) {
    return this.getUsers().filter((u) => u.role === role);
  }

  getAssignableUsersByRoleAndStates(
    role: Extract<User["role"], "Superfiler" | "Vendor">,
    states: string[]
  ) {
    const pool = this.getUsersByRole(role);
    if (!states || states.length === 0) return pool; // treat “All” as no filter
    return pool.filter((u) => {
      const covered = this.userStates[u.name] || [];
      return covered.some((s) => states.includes(s));
    });
  }

  getCompaniesFiltered(opts: {
    states?: string[];
    principalActivity?: string;
    formationDate?: string;
    fromDate?: string; // YYYY-MM-DD
    toDate?: string;
  }): string[] {
    const all = Array.from(
      new Set(
        this.getTodos()
          .flatMap((t) => t.companies ?? [])
          .filter(Boolean)
      )
    );

    return all.length ? all : ["Company A", "Company B", "Company C"]; // Provide mock data if tasks are empty
  }

  addTodo(
    todo: Omit<Todo, "id" | "date" | "creationDate">,
    files: File[] | null,
    bulkAssignFilters: CompanyFilterRequest | null = null,
    selectedCompanies: CompanySlim[] | null = null
  ): Observable<Todo> {
    const user = this.getCurrentUser();
    const userId = parseInt(user.id, 10) || 1;

    if (todo.type === "personal") {
      // Personal tasks don't support file uploads yet via backend
      const createDto: PersonalTaskCreateDto = {
        taskTitle: todo.message,
        description: todo.description,
        priority: todo.priorityLevel.toUpperCase() as Priority,
        dueDate: todo.targetDate,
        loginUserId: userId,
      };
      return this.personalTaskService
        .createPersonalToDo(createDto)
        .pipe(map(this.mapToTodo.bind(this)));
    }

    if (todo.type === "assigned") {
      const baseReq: Omit<BaseAssignmentRequest, "priority"> & {
        priority: Priority;
      } = {
        taskTitle: todo.message,
        description: todo.description,
        createdById: userId,
        priority: todo.priorityLevel.toUpperCase() as Priority,
        dueDate: todo.targetDate!,
      };

      if (user.role.toLowerCase() === "admin") {
        const assignedRoleName = todo.assignedTo || ""; // e.g., 'SuperFiler', 'Vendor', 'BOTH'
        let roleIds: number[] = [];

        if (assignedRoleName.toLowerCase() === "both") {
          roleIds = [3, 4]; // SuperFiler + Vendor
        } else {
          const roleId = this.mapRoleToRoleId(assignedRoleName);
          if (roleId) roleIds.push(roleId);
        }

        if (roleIds.length === 0) {
          return new Observable<Todo>((observer) => {
            observer.error(
              new Error(`Admin cannot assign to role: ${assignedRoleName}`)
            );
            observer.complete();
          });
        }

        const adminReq: BulkAdminAssignmentRequest = {
          ...baseReq,
          // roleIds: roleIds,
          assigneeRole: roleIds,
        };

        return this.assignedTaskService
          .createTaskByAdmin(adminReq, files)
          .pipe(map(this.mapTaskMasterDtoToTodo.bind(this)));
      } else if (user.role.toLowerCase() === "superfiler") {
        // Check if this is a BULK (filter-based) assignment
        if (bulkAssignFilters) {
          const sfBulkReq: BulkAssignmentByCompanyFilterRequest = {
            ...baseReq,
            filter: bulkAssignFilters,
          };
          return this.assignedTaskService
            .createTaskBySuperFilerBulk(sfBulkReq, files)
            .pipe(map(this.mapTaskMasterDtoToTodo.bind(this)));
        }

        // Otherwise, it's a SELECTED assignment
        if (!selectedCompanies || selectedCompanies.length === 0) {
          return new Observable<Todo>((observer) => {
            observer.error(
              new Error("SuperFiler must select companies or use bulk assign.")
            );
            observer.complete();
          });
        }

        const sfSelectedReq: BulkSuperFilerAssignmentRequest = {
          ...baseReq,
          targets: selectedCompanies,
        };

        return this.assignedTaskService
          .createTaskBySuperFilerSelected(sfSelectedReq, files)
          .pipe(map(this.mapTaskMasterDtoToTodo.bind(this)));
      }
    }

    return new Observable<Todo>((observer) => {
      observer.error(
        new Error("Unsupported task type or user role for addTodo")
      );
      observer.complete();
    });
  }

  /**
   * UPDATED: Now supports passing files to updateAssignedTaskMaster
   */
  updateTodo(
    id: string,
    updates: Partial<Todo>,
    files: File[] | null
  ): Observable<Todo> {
    const existingTodo = this.getTodoById(id);
    if (!existingTodo) {
      return new Observable<Todo>((observer) => {
        observer.error(new Error("Todo not found"));
        observer.complete();
      });
    }

    if (existingTodo.type === "personal") {
      const updateDto: PersonalTaskUpdateDto = {
        taskTitle: updates.message,
        description: updates.description,
        priority: updates.priorityLevel?.toUpperCase() as Priority,
        dueDate: updates.targetDate,
        completed: false,
        loginUserId: parseInt(this.getCurrentUser().id, 10) || 1,
      };

      return this.personalTaskService
        .updatePersonalToDo(existingTodo.id, updateDto)
        .pipe(map(this.mapToTodo.bind(this)));
    }

    if (existingTodo.type === "assigned") {
      const updateDto: Partial<AssignedTaskMasterDto> = {
        description: updates.description,
        priority: updates.priorityLevel?.toUpperCase() as Priority,
        dueDate: updates.targetDate,
      };
      // Pass files to the updated service method
      return this.assignedTaskService
        .updateAssignedTaskMaster(existingTodo.id, updateDto, files)
        .pipe(
          map(this.mapTaskMasterDtoToTodo.bind(this)),
          map((updatedTodo) => {
            const aIdx = this.assignedTodosCache.findIndex((t) => t.id === id);
            if (aIdx > -1) this.assignedTodosCache[aIdx] = updatedTodo;
            return updatedTodo;
          })
        );
    }
    return new Observable<Todo>((observer) => {
      observer.error(new Error("Unsupported task type for updateTodo"));
      observer.complete();
    });
  }

  deleteTodo(id: string): Observable<void> {
    const existingTodo = this.getTodoById(id);
    if (existingTodo && existingTodo.type === "personal") {
      return this.personalTaskService.deletePersonalToDo(existingTodo.id).pipe(
        map(() => {
          this.personalTodosCache = this.personalTodosCache.filter(
            (t) => t.id !== id
          );
        })
      );
    }
    let masterIdToDelete: string | undefined = id;
    if (existingTodo && existingTodo.type === "assigned") {
      masterIdToDelete = existingTodo.masterId || existingTodo.id;
    } else {
      masterIdToDelete = id;
    }
    if (!masterIdToDelete) {
      return new Observable<void>((observer) => {
        observer.error(new Error("Could not determine masterId for deletion."));
        observer.complete();
      });
    }
    return this.assignedTaskService
      .deleteAssignedTaskMaster(masterIdToDelete)
      .pipe(
        map(() => {
          this.assignedTodosCache = this.assignedTodosCache.filter(
            (t) => t.id !== masterIdToDelete && t.masterId !== masterIdToDelete
          );
        })
      );
  }

  /**
   * UPDATED: Now calls PUT /tasks/status with files
   * @param id The TaskItem ID (task.id)
   * @param status The new status enum
   * @param notes Optional notes
   * @param files Optional files
   */
  updateTaskStatus(
    id: string,
    status: AssignedTaskStatus,
    notes: string = "",
    files: File[] | null
  ): Observable<void> {
    const existingTodo = this.getTodoById(id);
    if (!existingTodo) {
      return new Observable<void>((observer) => {
        observer.error(new Error("Not found"));
        observer.complete();
      });
    }

    if (existingTodo.type === "assigned") {
      const req: TaskStatusUpdateRequest = {
        taskId: existingTodo.id, // Pass the TaskItem ID in the request body
        status: status,
        notes: notes || undefined,
      };
      // Call the updated service method with files
      return this.assignedTaskService.updateTaskStatus(req, files).pipe(
        map(() => {
          // Update cache
          const updatedTodo = { ...existingTodo };
          if (status === AssignedTaskStatus.DONE) {
            updatedTodo.completionStatus = true;
            updatedTodo.ignoredStatus = false;
            updatedTodo.completionNotes = notes;
          } else if (status === AssignedTaskStatus.IGNORED) {
            updatedTodo.completionStatus = false;
            updatedTodo.ignoredStatus = true;
            updatedTodo.ignoredReason = notes;
          }
          const aIdx = this.assignedTodosCache.findIndex((t) => t.id === id);
          if (aIdx > -1) this.assignedTodosCache[aIdx] = updatedTodo;
        })
      );
    }

    return new Observable<void>((observer) => {
      observer.error(new Error("Unsupported task type for status update"));
      observer.complete();
    });
  }

  /**
   * UPDATED: Now passes files to updateTaskStatus
   */
  markTaskDone(
    id: string,
    notes?: string,
    files?: File[] | null
  ): Observable<Todo> {
    const existingTodo = this.getTodoById(id);
    if (!existingTodo) {
      return new Observable<Todo>((observer) => {
        observer.error(new Error("Not found"));
        observer.complete();
      });
    }
    if (existingTodo.type === "personal") {
      return this.personalTaskService
        .setPersonalToDoCompleted(existingTodo.id, true)
        .pipe(map(this.mapToTodo.bind(this)));
    }
    if (existingTodo.type === "assigned") {
      const attachments: File[] | null = files ?? null;
      return this.updateTaskStatus(
        id,
        AssignedTaskStatus.DONE,
        notes,
        attachments
      ).pipe(
        map(() => this.getTodoById(id)!) // Return the updated todo from cache
      );
    }
    return new Observable<Todo>((observer) => {
      observer.error(new Error("Unsupported task type for markTaskDone"));
      observer.complete();
    });
  }

  /**
   * UPDATED: Now passes files to updateTaskStatus (e.g., if user attaches proof of ignore)
   */
  ignoreTask(id: string, reason = "", files: File[] | null): Observable<void> {
    const existingTodo = this.getTodoById(id);
    if (!existingTodo) {
      return new Observable<void>((observer) => {
        observer.error(new Error("Not found"));
        observer.complete();
      });
    }
    if (existingTodo.type === "personal") {
      return new Observable<void>((observer) => {
        observer.next(void 0);
        observer.complete();
      });
    }
    if (existingTodo.type === "assigned") {
      return this.updateTaskStatus(
        id,
        AssignedTaskStatus.IGNORED,
        reason,
        files
      );
    }
    return new Observable<void>((observer) => {
      observer.error(new Error("Unsupported task type for ignoreTask"));
      observer.complete();
    });
  }

  getTodoById(id: string): Todo | undefined {
    let task =
      this.personalTodosCache.find((todo) => todo.id === id) ||
      this.assignedTodosCache.find((todo) => todo.id === id);
    return task;
  }

  // This is now a mock file upload, as the real files are handled by the service
  uploadFile(file: File): Promise<FileAttachment> {
    return new Promise((resolve) => {
      setTimeout(() => {
        const fileAttachment: FileAttachment = {
          id: Math.random().toString(36).substr(2, 9),
          name: file.name,
          size: file.size,
          type: file.type,
          url: URL.createObjectURL(file), // Mock URL
          uploadDate: new Date(),
        };
        resolve(fileAttachment);
      }, 500); // Faster mock
    });
  }
  private safeParse<T>(str: string | null): T | null {
    if (!str) return null;
    try {
      return JSON.parse(str) as T;
    } catch {
      return null;
    }
  }

  private buildStoredCompanyMap(): Record<number, string> {
    const map: Record<number, string> = {};

    // ✅ your localStorage keys
    const listKeys = ["progressList", "completedProgressList", "draftList"];

    // arrays
    for (const key of listKeys) {
      const items = this.safeParse<any[]>(
        this.secureStorage.getItem<string>(key, "session")
      );
      if (Array.isArray(items)) {
        for (const i of items) {
          if (i?.companyId && i?.companyName) {
            map[Number(i.companyId)] =
              String(i.companyName) + " " + String(i.llcName);
          }
        }
      }
    }

    // selectedCompany (single object)
    const selected = this.safeParse<any>(
      localStorage.getItem("selectedCompany")
    );
    if (selected?.companyId && selected?.companyName) {
      map[Number(selected.companyId)] = String(selected.companyName);
    }

    return map;
  }
  public refreshCompanyMap(): void {
    this.companyMapCache = this.buildStoredCompanyMap();
  }

  public getCompanyNameById(companyId?: number | null): string {
    if (!companyId) return "-";
    if (!this.companyMapCache) this.refreshCompanyMap();
    return this.companyMapCache![companyId] ?? "-";
  }

  getPresignedUrlsForKeys(taskId: string) {
   return  this.assignedTaskService.getPresignedUrls(taskId);
    // need to update return type
    // return this.http.post<string[]>(`/tasks/attachments/presign`, { keys });
  }
}
