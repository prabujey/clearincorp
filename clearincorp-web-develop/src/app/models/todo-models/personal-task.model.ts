
export type Priority = 'LOW' | 'MEDIUM' | 'HIGH';


export interface PersonalTaskCreateDto {
  taskTitle: string;
  description?: string;
  priority: Priority;
  dueDate?: string;     // YYYY-MM-DD
  loginUserId: number;
}

export interface PersonalTaskUpdateDto {
  taskTitle?: string;
  description?: string;
  priority?: Priority;
  dueDate?: string;
  completed?: boolean;
  loginUserId?: number;
}

export interface PersonalTaskResponseDto {
  id: string;
  taskTitle: string;
  priority: Priority;
  dueDate: string;
  completed: boolean;
  createdOn?: string;
  updatedOn?: string;
  loginUserId?: number;
}

export interface PagedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface PersonalTaskFilter {
  loginUserId?: number;
  priority?: Priority;
  completed?: boolean;
  dueDateFrom?: string; 
  dueDateTo?: string;   
  search?: string;
}

export type SortToken = string | { field: string; dir?: 'asc' | 'desc' };