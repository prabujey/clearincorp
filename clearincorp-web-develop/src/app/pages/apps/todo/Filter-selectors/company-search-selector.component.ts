import { Component, Input, Output, EventEmitter, OnInit, ChangeDetectionStrategy, ChangeDetectorRef, inject, DestroyRef, SimpleChanges, OnChanges, WritableSignal, signal } from "@angular/core";
import { CommonModule } from "@angular/common";
import { FormControl, ReactiveFormsModule } from "@angular/forms";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatInputModule } from "@angular/material/input";
import { MatAutocompleteModule, MatAutocompleteSelectedEvent } from "@angular/material/autocomplete";
import { MatChipsModule } from "@angular/material/chips";
import { MatIconModule } from "@angular/material/icon";
import { MatSelectModule } from "@angular/material/select";
import { MatButtonModule } from "@angular/material/button";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import { MatProgressBarModule } from "@angular/material/progress-bar";
import { MatTooltipModule } from "@angular/material/tooltip";
import { MatDividerModule } from "@angular/material/divider";
import { MatCheckboxModule } from "@angular/material/checkbox";

import { catchError, debounceTime, distinctUntilChanged, filter, map, switchMap, tap, startWith } from 'rxjs/operators';
import { CompanyFilterRequest, CompanySlim } from 'src/app/models/todo-models/assigned-task.model';
import { BehaviorSubject, Observable, of, combineLatest } from "rxjs";
import { TodoService } from "src/app/services/todo/todo.service";
import { PagedResponse } from "src/app/models/todo-models/personal-task.model";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";

// Import filter components
import { StateSelectorComponent } from "./state-selector.component";
import { PrincipalActivitySelectorComponent } from "./principal-activity-selector.component";
import { MatDatepickerModule, MatDateRangeInput, MatDateRangePicker, MatEndDate, MatStartDate } from "@angular/material/datepicker";
import { FormsModule } from "@angular/forms";

type RawValue = "ALL" | string;

@Component({
  selector: "app-company-search-selector",
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, FormsModule,
    MatFormFieldModule, MatInputModule, MatAutocompleteModule,
    MatChipsModule, MatIconModule, MatButtonModule, MatSelectModule,
    MatProgressSpinnerModule, MatProgressBarModule, MatTooltipModule, MatDividerModule,
    MatCheckboxModule, MatDatepickerModule,
    MatDateRangeInput, MatEndDate, MatStartDate, MatDateRangePicker,
    // Filter components
    StateSelectorComponent, PrincipalActivitySelectorComponent
  ],
  template: `
    <div class="company-filter-selector">

      <!-- 1. Dependent Filters -->
      <app-principal-activity-selector
        [label]="'Principal Activity'"
        [activities]="[]"
        [rawSelection]="rawActivities"
        (activitiesChange)="onFilterChange('activity', $event)"
        (selectionChange)="rawActivities = $event"
      >
      </app-principal-activity-selector>

      <mat-form-field appearance="outline" class="full-width date-range-field">
        <mat-label>Formation Date Range (Optional)</mat-label>
        <mat-date-range-input [rangePicker]="rangePicker">
          <input
            matStartDate
            placeholder="From"
            [(ngModel)]="fromDate"
            (ngModelChange)="onFilterChange('date', $event)"
          />
          <input
            matEndDate
            placeholder="To"
            [(ngModel)]="toDate"
            (ngModelChange)="onFilterChange('date', $event)"
          />
        </mat-date-range-input>
        <mat-datepicker-toggle matSuffix [for]="rangePicker"></mat-datepicker-toggle>
        <mat-date-range-picker #rangePicker></mat-date-range-picker>
      </mat-form-field>

      <app-state-selector
        [label]="'States'"
        [rawSelection]="rawStates"
        (statesChange)="onFilterChange('state', $event)"
        (selectionChange)="rawStates = $event"
      ></app-state-selector>

      <mat-divider class="section-divider"></mat-divider>

      <!-- 2. Search Input (with RxJS) -->
      <h4 class="filter-title">Search Companies</h4>
      <mat-form-field appearance="outline" class="full-width">
       
        <input
          matInput
          [formControl]="searchControl"
          placeholder="e.g. Tech Solutions"
          [matAutocomplete]="auto"
          autocomplete="off"
        />
        <mat-icon matSuffix *ngIf="isLoading()" class="search-spinner">
          <mat-spinner diameter="20"></mat-spinner>
        </mat-icon>
        <mat-autocomplete #auto="matAutocomplete" (optionSelected)="onCompanySelected($event)">
          <mat-option *ngIf="isLoading()" disabled>
            <span class="loading-text">Searching...</span>
          </mat-option>
          
          <ng-container *ngIf="!isLoading()">
            <mat-option *ngFor="let company of filteredCompanies()" [value]="company">
              {{ company.companyName }} ({{ company.state }})
            </mat-option>
            
            <mat-option disabled *ngIf="filteredCompanies().length === 0 && searchControl.value.length >= 3">
              No companies match your search and filters.
            </mat-option>
          </ng-container>
        </mat-autocomplete>
        <mat-hint>
          Search results are based on the filters set above.
        </mat-hint>
      </mat-form-field>

      <!-- 3. Selection Mode -->
      <div class="selection-mode">
        <mat-checkbox
          [checked]="selectAllFiltered()"
          (change)="toggleSelectAll($event.checked)"
          [disabled]="filteredCompanies().length === 0"
        >
          Assign to all {{ filteredCompanies().length }} filtered companies
        </mat-checkbox>
      </div>

      <!-- 4. Selected Companies Chips -->
      <div *ngIf="!selectAllFiltered() && selectedCompanies().length > 0" class="selected-companies">
        <h4 class="filter-title">Selected for Assignment ({{ selectedCompanies().length }})</h4>
        <mat-chip-set>
          <mat-chip
            *ngFor="let company of selectedCompanies()"
            (removed)="removeCompany(company)"
            color="accent" selected
          >
            <mat-icon matChipAvatar>business</mat-icon>
            {{ company.companyName }} ({{ company.state }})
            <button matChipRemove><mat-icon>cancel</mat-icon></button>
          </mat-chip>
        </mat-chip-set>
      </div>
      
      <div *ngIf="selectAllFiltered()" class="selected-companies all-selected-info">
          <mat-icon color="primary" style="padding-right:18px;">check_circle</mat-icon>
          <span>
            Will assign to <strong>all {{ filteredCompanies().length }} companies</strong> matching your filters
            (Principal Activity, Date, State).
          </span>
      </div>
    </div>
  `,
  styles: [`
    .company-filter-selector { margin: 16px 0; }
    .full-width { width: 100%; }
    .filter-title { font-size: 14px; font-weight: 600; margin-bottom: 8px; color: #374151; }
    .date-range-field { margin-top: 16px; }
    .section-divider { margin: 20px 0; }
    .search-spinner { color: #ec3252; }
    .loading-text { margin-left: 8px; font-style: italic; }
    .selection-mode { margin-top: 10px; }
    .selected-companies { margin-top: 16px; }
    mat-chip-set { display: flex; flex-wrap: wrap; gap: 8px; }
    .all-selected-info {
        display: flex;
        align-items: center;
        gap: 8px;
        padding: 12px;
        background: #f0f9ff;
        border: 1px solid #7dd3fc;
        border-radius: 8px;
        color: #0c4a6e;
    }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CompanySearchSelectorComponent implements OnInit, OnChanges {
  
  // --- Filter Inputs ---
  rawActivities: RawValue[] = ["ALL"];
  rawStates: RawValue[] = ["ALL"];
  fromDate: Date | null = null;
  toDate: Date | null = null;

  // --- Filter State Observables ---
  private activityFilter$ = new BehaviorSubject<string[]>([]);
  private stateFilter$ = new BehaviorSubject<string[]>([]);
  private dateFilter$ = new BehaviorSubject<{ from: string | undefined, to: string | undefined }>({ from: undefined, to: undefined });
  
  // --- Search State ---
  searchControl = new FormControl('', { nonNullable: true });
  
  // --- Combined State Observables ---
  isLoading: WritableSignal<boolean> = signal(false);
  filteredCompanies: WritableSignal<CompanySlim[]> = signal([]);
  
  // --- Selection State ---
  selectedCompanies: WritableSignal<CompanySlim[]> = signal([]);
  selectAllFiltered: WritableSignal<boolean> = signal(false);

  // --- Outputs ---
  /** FIX: Emits the array of selected CompanySlim objects */
  @Output() selectedCompaniesChange = new EventEmitter<CompanySlim[]>();
  @Output() bulkAssignFiltersChange = new EventEmitter<CompanyFilterRequest | null>();

  private destroyRef = inject(DestroyRef);

  constructor(private todoService: TodoService, private cdr: ChangeDetectorRef) {}

  ngOnInit() {
    const filters$ = combineLatest({
      activities: this.activityFilter$,
      states: this.stateFilter$,
      dates: this.dateFilter$,
      search: this.searchControl.valueChanges.pipe(
        startWith(''),
        debounceTime(1000),
        map(s => (s && s.length >= 3) ? s : '')
      )
    }).pipe(
      debounceTime(200),
      distinctUntilChanged((prev, curr) => JSON.stringify(prev) === JSON.stringify(curr))
    );

    filters$.pipe(
      tap(() => this.isLoading.set(true)),
      switchMap((filters: { activities: string[], states: string[], dates: { from: string | undefined, to: string | undefined }, search: string }) => {
        const request: CompanyFilterRequest = {
          principalActivity: filters.activities.length > 0 ? filters.activities : undefined,
          states: filters.states.length > 0 ? filters.states : undefined,
          effectiveFrom: filters.dates.from,
          effectiveTo: filters.dates.to,
          search: filters.search || undefined,
        };
        return this.todoService.searchCompanies(request, 0, 100).pipe(
          catchError(() => of({ 
              content: [], page: 0, size: 0, totalElements: 0, totalPages: 0, last: true 
          } as PagedResponse<CompanySlim>))
        );
      }),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((response: PagedResponse<CompanySlim>) => {
      this.filteredCompanies.set(response.content);
      this.isLoading.set(false);
      this.validateSelection(); 
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
  }

  // --- Filter Change Handlers ---
  onFilterChange(type: 'activity' | 'state' | 'date', value: any) {
    if (type === 'activity') {
      this.activityFilter$.next(value as string[]);
    }
    if (type === 'state') {
      this.stateFilter$.next(value as string[]);
    }
    if (type === 'date') {
      this.dateFilter$.next({
        from: this.dateToString(this.fromDate),
        to: this.dateToString(this.toDate)
      });
    }
    if (this.selectAllFiltered()) {
        this.selectAllFiltered.set(false);
        this.emitChanges();
    }
  }

  // --- Selection Handlers ---
  onCompanySelected(event: MatAutocompleteSelectedEvent): void {
    const company = event.option.value as CompanySlim;
    if (company && !this.selectedCompanies().some(c => c.companyId === company.companyId)) {
      this.selectedCompanies.set([...this.selectedCompanies(), company]);
      this.emitChanges();
    }
    this.searchControl.setValue('');
    event.option.deselect();
  }

  removeCompany(company: CompanySlim): void {
    this.selectedCompanies.set(
      this.selectedCompanies().filter(c => c.companyId !== company.companyId)
    );
    this.emitChanges();
  }

  toggleSelectAll(checked: boolean): void {
    this.selectAllFiltered.set(checked);
    if (checked) {
      this.selectedCompanies.set([]);
    }
    this.emitChanges();
  }

  private validateSelection(): void {
    if (this.selectAllFiltered() || this.selectedCompanies().length === 0) {
      this.emitChanges();
      return;
    }

    const filteredIds = new Set(this.filteredCompanies().map(c => c.companyId));
    const validSelection = this.selectedCompanies().filter(c => filteredIds.has(c.companyId));

    if (validSelection.length !== this.selectedCompanies().length) {
      this.selectedCompanies.set(validSelection);
    }
    this.emitChanges();
  }

  private emitChanges(): void {
    if (this.selectAllFiltered()) {
      // Mode 1: Bulk Assign
      this.selectedCompaniesChange.emit([]); // No individual companies
      this.bulkAssignFiltersChange.emit({
        principalActivity: this.activityFilter$.value.length ? this.activityFilter$.value : undefined,
        states: this.stateFilter$.value.length ? this.stateFilter$.value : undefined,
        effectiveFrom: this.dateFilter$.value.from,
        effectiveTo: this.dateFilter$.value.to,
        search: this.searchControl.value.length >= 3 ? this.searchControl.value : undefined,
      });
    } else {
      // Mode 2: Selected Assign
      /** FIX: Emit the array of selected CompanySlim objects */
      this.selectedCompaniesChange.emit(this.selectedCompanies());
      this.bulkAssignFiltersChange.emit(null); // Not a bulk assign
    }
  }

  // --- Utility ---
  private dateToString(date: Date | null | undefined): string | undefined {
    if (!(date instanceof Date) || isNaN(date.getTime())) return undefined;
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, "0");
    const d = String(date.getDate()).padStart(2, "0");
    return `${y}-${m}-${d}`;
  }
}

