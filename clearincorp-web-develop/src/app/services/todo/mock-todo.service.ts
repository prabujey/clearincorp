import { Observable, of } from 'rxjs';
import { delay } from 'rxjs/operators';

// Define the core data models used across the app
export interface Company {
  id: string;      // Unique ID for assignment
  name: string;    // Company name
  state: string;   // Company state (for differentiation)
}

export interface CompanyFilterCriteria {
  states?: string[]; // Array of selected states
  principalActivities?: string[]; // Array of selected activities (renamed)
  fromDate?: string; // YYYY-MM-DD format
  toDate?: string;   // YYYY-MM-DD format
}

/**
 * Mocks the backend API calls related to fetching and searching companies.
 */
export class MockTodoService {

  // --- Mock Database (Data Source) ---
  private MOCK_COMPANIES: Company[] = [
    { id: 'C1001', name: 'Alpha Global Solutions', state: 'CA' }, // Tech/Service
    { id: 'C1002', name: 'Alpha Global Solutions', state: 'TX' }, // Tech/Service
    { id: 'C2001', name: 'Retail King Inc', state: 'NY' },       // Retail
    { id: 'C2002', name: 'Retail King Inc', state: 'NJ' },       // Retail
    { id: 'C3005', name: 'Tech Innovators LLC', state: 'FL' },    // Tech
    { id: 'C3006', name: 'Tech Innovators LLC', state: 'WA' },    // Tech
    { id: 'C4010', name: 'Service Masters Co.', state: 'CA' },   // Service
    { id: 'C5020', name: 'Midwest Services', state: 'OH' },      // Service
    { id: 'C6030', name: 'Texas Retail Hub', state: 'TX' },      // Retail
  ];

  /**
   * Simulates a backend search query for the Autocomplete (Scenario 1 & 3).
   */
  searchCompanies(term: string): Observable<Company[]> {
    console.log(`[Mock API] Simulating search for: "${term}"`);
    const lowerTerm = term.toLowerCase().trim();
    
    const results = this.MOCK_COMPANIES.filter(
      c => c.name.toLowerCase().includes(lowerTerm) || c.state.toLowerCase().includes(lowerTerm)
    );
    
    return of(results).pipe(delay(300)); 
  }

  /**
   * Simulates retrieving a list of companies based on complex criteria (Scenario 2).
   */
  getCompaniesFiltered(criteria: CompanyFilterCriteria): Observable<Company[]> {
    console.log('[Mock API] Filtering with criteria:', criteria);

    let results = this.MOCK_COMPANIES;
    
    // 1. Filter by State
    if (criteria.states && criteria.states.length > 0) {
      results = results.filter(c => criteria.states!.includes(c.state));
    }
    
    // 2. Filter by Principal Activity (checking if company name includes activity keyword)
    if (criteria.principalActivities && criteria.principalActivities.length > 0) {
      const lowerActivities = criteria.principalActivities.map(a => a.toLowerCase());
      
      results = results.filter(company => {
        const companyLowerName = company.name.toLowerCase();
        
        // Return true if company name matches any of the selected activities
        return lowerActivities.some(activity => companyLowerName.includes(activity));
      });
    }
    
    // Date range filter is ignored for simplicity here, as it's purely UI driven.

    return of(results).pipe(delay(500)); 
  }
}
