import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from 'src/environments/environment';
import { PagedResponse, SortToken } from 'src/app/models/todo-models/personal-task.model';
import { CompanyFilterRequest, CompanySlim } from 'src/app/models/todo-models/assigned-task.model';

@Injectable({ providedIn: 'root' })
export class CompanyService {
  // Assuming the company controller is at /company
  private companyUrl = `${environment.apiBaseUrl}/company`;

  constructor(private http: HttpClient) {}

  /**
   * Searches for companies based on filter criteria.
   * Corresponds to: GET /company/filter
   */
  searchCompanies(
    filter: CompanyFilterRequest, 
    page: number = 0, 
    size: number = 20, 
    sort: SortToken[] = []
  ): Observable<PagedResponse<CompanySlim>> {
    const headers = new HttpHeaders().set("X-Skip-Loading", "true");
    
    let params = new HttpParams()
      .set('page', page)
      .set('size', size);

    // Map filter object to HttpParams
    if (filter.search) {
      params = params.set('search', filter.search);
    }
    if (filter.effectiveFrom) {
      params = params.set('effectiveFrom', filter.effectiveFrom);
    }
    if (filter.effectiveTo) {
      params = params.set('effectiveTo', filter.effectiveTo);
    }

    // Handle string arrays for principalActivity and states
    // The backend @RequestParam List<String> expects multiple params with the same name
    filter.principalActivity?.forEach(activity => {
      params = params.append('principalActivity', activity);
    });

    filter.states?.forEach(state => {
      params = params.append('states', state);
    });

    // Handle sort tokens
    sort.forEach(s => {
      const v = typeof s === 'string' ? s : (s.dir ? `${s.field},${s.dir}` : s.field);
      if (v?.trim()) params = params.append('sort', v.trim());
    });

    return this.http.get<PagedResponse<CompanySlim>>(`${this.companyUrl}/filter`, { params, headers });
  }
}
