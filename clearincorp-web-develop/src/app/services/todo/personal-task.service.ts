import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from 'src/environments/environment';
import { PersonalTaskCreateDto,PersonalTaskUpdateDto,Priority,PersonalTaskResponseDto ,PagedResponse,PersonalTaskFilter , SortToken } from 'src/app/models/todo-models/personal-task.model';

@Injectable({ providedIn: 'root' })
export class PersonalTaskService {
  private baseUrl = `${environment.apiBaseUrl}/personal-tasks`;

  constructor(private http: HttpClient) {}


searchPersonalToDo(filter: PersonalTaskFilter, page=0, size=10, sort: SortToken[]=[]):
    Observable<PagedResponse<PersonalTaskResponseDto>> {
    let params = new HttpParams().set('page', page).set('size', size);
    
    if (filter.loginUserId != null) params = params.set('loginUserId', filter.loginUserId);
    if (filter.priority) params = params.set('priority', filter.priority);
    if (filter.completed != null) params = params.set('completed', filter.completed);
    if (filter.dueDateFrom) params = params.set('dueDateFrom', filter.dueDateFrom);
    if (filter.dueDateTo) params = params.set('dueDateTo', filter.dueDateTo);
    if (filter.search) params = params.set('search', filter.search); 
    
    // Correctly map and append sort parameters
    sort.forEach(s => {
      const v = typeof s === 'string' ? s : (s.dir ? `${s.field},${s.dir}` : s.field);
      if (v?.trim()) params = params.append('sort', v.trim());
    });
    return this.http.get<PagedResponse<PersonalTaskResponseDto>>(this.baseUrl, { params });
  }

createPersonalToDo(dto: PersonalTaskCreateDto) {
    return this.http.post<PersonalTaskResponseDto>(this.baseUrl, dto);
}

updatePersonalToDo(id: string, dto: PersonalTaskUpdateDto) {
    return this.http.put<PersonalTaskResponseDto>(`${this.baseUrl}/${id}`, dto);
}

deletePersonalToDo(id: string) {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
}


setPersonalToDoCompleted(id: string, completed: boolean) {
    return this.http.patch<PersonalTaskResponseDto>(`${this.baseUrl }/${id}/completed`, null, {
      params: new HttpParams().set('completed', completed , )
    });
}

}