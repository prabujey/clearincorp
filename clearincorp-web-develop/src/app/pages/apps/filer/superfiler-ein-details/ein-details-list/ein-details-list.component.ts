import {
  Component,
  OnInit,
  AfterViewInit,
  ViewChild,
  ChangeDetectorRef,
} from "@angular/core";
import { MatTableDataSource, MatTable } from "@angular/material/table";
import { MatPaginator, PageEvent } from "@angular/material/paginator";
import { MatDialog } from "@angular/material/dialog";
import {
  FilerService,
  PagedResponse,
} from "src/app/services/apps/filer/filer.service";
import { EinCompany } from "src/app/models/filer";

import { EinDetailsViewComponent } from "../ein-details-view/ein-details-view.component";
import { Router } from "@angular/router";
import { HttpErrorResponse } from "@angular/common/http";
import { MaterialModule } from "src/app/material.module";
import { FormsModule, ReactiveFormsModule, FormControl } from "@angular/forms";
import { NgIcon } from "@ng-icons/core";
import { CommonModule } from "@angular/common";
import { SnackBarService } from "src/app/shared/snackbar.service";
import { Observable } from "rxjs";
import { map, startWith } from "rxjs/operators";

@Component({
  selector: "app-ein-details-list",
  templateUrl: "./ein-details-list.component.html",
  styles: [
    `
      /* Container for both filters */
      .state-search-container {
        display: flex;
        align-items: center;
        gap: 8px; // space between the two fields
        max-width: 0; // start collapsed
        overflow: hidden;
        opacity: 0;
        transition: max-width 0.3s ease, opacity 0.3s ease;

        &.show-state-search {
          max-width: 420px; // ~ 200px + 200px + 8px gap + padding if any
          opacity: 1;
          overflow: visible;
        }
      }

      /* If you still need the search-field transition from before */
      .search-field {
      flex: 1;
          min-width: 250px;
          max-width: 450px;
        transition: opacity 0.2s ease-in-out;
        opacity: 1;
      }

      /* Mobile tweaks */
      @media (max-width: 768px) {
        .state-search-container.show-state-search {
          max-width: 300px; // shrink for smaller screens
        }

        .search-field {
          margin-bottom: 16px;
        }
      }

      /* Rotate icon when active */
      .settings-icon {
        transition: transform 0.3s ease-in-out;

        &.active {
          transform: rotate(180deg);
        }
      }

      .status-viewed {
        color: #4caf50;
        font-weight: 500;
      }

      .status-not-viewed {
        color: #ff9800;
        font-weight: 500;
      }


      .expedited-toggle {
        display: inline-flex;
        border-radius: 10px;
        border: 1px solid #e5e7eb;
        overflow: hidden;
        background: #ffffff;
      }

      .expedited-toggle .toggle-btn {
        border: none;
        background: transparent;
        padding: 12px 20px;
        font-size: 15px;
        font-weight: 500;
        color: #4b5563;
        cursor: pointer;
        min-width: 0;
        white-space: nowrap;

        &:first-child {
          border-right: 1px solid #e5e7eb;
        }

        &:hover {
          background-color: #f9fafb;
        }

        &.active {
          background-color: #bf0a30; // or use your $primary-color
          color: #ffffff;
        }
      }

      .empty-state-card {
        max-width: 520px;
        margin: 0 auto;
        padding: 20px 24px;
        border-radius: 16px;
        background: transparent;
        text-align: center;
        display: flex;
        flex-direction: column;
        align-items: center;
      }

      .empty-icon {
        width: 44px;
        height: 44px;
        border-radius: 999px;
        display: flex;
        align-items: center;
        justify-content: center;
        margin-bottom: 10px;
        background: rgba(239, 68, 68, 0.08); // soft warn background

        mat-icon {
          font-size: 22px;
          width: 22px;
          height: 22px;
          color: #ef4444; // warn red
        }
      }

      .empty-title {
        margin: 0 0 4px 0;
        font-size: 16px;
        font-weight: 600;
        color: #111827;
      }

      .empty-text {
        margin: 0 0 14px 0;
        font-size: 13px;
        line-height: 1.5;
        color: #6b7280;
      }

      .empty-action-btn {
        margin-top: 4px;
        padding: 6px 18px;
        border-radius: 999px;
        font-size: 13px;
        font-weight: 500;
        text-transform: none;
      }

    `,
  ],
  standalone: true,
  imports: [
    MaterialModule,
    FormsModule,
    ReactiveFormsModule,
    NgIcon,
    CommonModule,
  ],
})
export class EinDetailsListComponent implements OnInit, AfterViewInit {
  @ViewChild(MatTable) table!: MatTable<EinCompany>;
  @ViewChild(MatPaginator) paginator!: MatPaginator;

  stateControl = new FormControl("");
  filteredStates!: Observable<string[]>;

  states: string[] = [];

  displayedColumns: string[] = ["companyName", "state", "view", "status"];

  dataSource = new MatTableDataSource<EinCompany>([]);
  isLoading = false;
  searchValue = "";
  showStateSearch = false;

  expeditedFilter: 'noExpedited' | 'expedited' = 'noExpedited';
  noExpeditedCount = 0;
  expeditedCount = 0;

  // pagination
  totalItems = 0;
  pageIndex = 0;
  pageSize = 5;
  lastPage = false;

  constructor(
    private dialog: MatDialog,
    private filerService: FilerService,
    private cdr: ChangeDetectorRef,
    private router: Router,
    private snackBarService: SnackBarService
  ) {
    this.filteredStates = this.stateControl.valueChanges.pipe(
      startWith(""),
      map((v) => this._filterStates(v || ""))
    );
  }

  ngOnInit(): void {
    this.loadStates();
    // this.loadPage();
  }

  ngAfterViewInit(): void {
    this.pageIndex = 0;
    this.pageSize = 5;
    this.loadPage();

    this.paginator.page.subscribe((evt: PageEvent) => {
      this.pageIndex = evt.pageIndex;
      this.pageSize = evt.pageSize;
      this.loadPage();
    });
  }

  private _filterStates(value: string): string[] {
    const filter = value.toLowerCase();
    return this.states.filter((s) => s.toLowerCase().includes(filter));
  }

  loadStates(): void {
    this.filerService.getStates().subscribe({
      next: (s) => (this.states = s),
      error: () => this.snackBarService.showError("Failed to load states"),
    });
  }

  /** only called when the user picks one of the mat-option values */
  onStateSelected(state: string) {
    this.stateControl.setValue(state);
    this.pageIndex = 0;
    this.loadPage();
    this.paginator.firstPage(); // reset page
  }

  /** clear both the field and your filter */
  clearState() {
    this.stateControl.setValue("");
    this.pageIndex = 0;
    this.loadPage();
  }

  applyFilters(): void {
    this.pageIndex = 0;
    this.paginator.pageIndex = 0;
    this.loadPage();
  }

  onSearch(): void {
    this.searchValue = this.searchValue?.trim() || "";
    this.applyFilters();
  }

  private loadPage(): void {
    const isExpedited: boolean | undefined = this.expeditedFilter === 'noExpedited' ? false : true;
    this.isLoading = true;
    this.filerService
      .fetchEinStatusFromApi(
        this.pageIndex,
        this.pageSize,
        this.searchValue,
        this.stateControl.value || "",
        isExpedited 
      )
      .subscribe({
        next: (resp: PagedResponse<EinCompany>) => {
          this.dataSource.data = resp.content;
          this.totalItems = resp.totalElements;
          this.lastPage = resp.last;
             if (isExpedited) {
        this.expeditedCount = resp.totalElements;
      } else {
        this.noExpeditedCount = resp.totalElements;
      }

          // Set paginator props correctly
          this.paginator.length = resp.totalElements;
          this.paginator.pageIndex = resp.pageNumber;
          this.paginator.pageSize = resp.pageSize;
          this.isLoading = false;
          this.updateOtherExpeditedCount(!isExpedited);
        },
        error: () => {
          this.isLoading = false;
          this.snackBarService.showError("Failed to load EIN companies");
        },
      });
  }
private updateOtherExpeditedCount(otherIsExpedited: boolean): void {
  // Use fetchEinStatusFromApi instead of fetchFillersFromApi to match the EIN list
  this.filerService
    .fetchEinStatusFromApi(
      0, 
      1, 
      this.searchValue.trim() || "", 
      this.stateControl.value || "", 
      otherIsExpedited
    )
    .subscribe({
      next: (resp) => {
        if (otherIsExpedited) {
          this.expeditedCount = resp.totalElements;
        } else {
          this.noExpeditedCount = resp.totalElements;
        }
        this.cdr.detectChanges(); // Ensure UI updates
      }
    });
}

  private updateDataSource(data: EinCompany[]): void {
    this.dataSource.data = data;
    this.paginator.firstPage();
  }

  openDocument(company: EinCompany): void {
    if (!company.companyId) {
      this.snackBarService.showError(
        "Invalid company data. Please refresh and try again.",
        "error"
      );
      return;
    }

    this.isLoading = true;
    this.filerService.getEinFile(company.companyId).subscribe({
      next: (documentBlob: Blob) => {
        const dialogRef = this.dialog.open(EinDetailsViewComponent, {
          panelClass: "custom-dialog-panel",
          data: {
            documentBlob,
            company,
            companyId: company.companyId,
            documentType: "EIN",
          },
          backdropClass: "custom-ticket-dialog-backdrop",
          autoFocus: false,
          disableClose: true,
        });

        dialogRef.afterClosed().subscribe((result) => {
          if (result === "viewed") {
            company.viewStatus = true;
            this.cdr.detectChanges();
          }
          this.isLoading = false;
        });
      },
      error: (error: HttpErrorResponse) => {
        this.isLoading = false;
        this.snackBarService.showError("Failed to load EIN document");
      },
    });
  }
  clearFilters(): void {
    this.searchValue = "";
    this.stateControl.setValue("");
    this.expeditedFilter = 'expedited';
    this.pageIndex = 0;
    this.paginator.pageIndex = 0;
    this.loadPage();
  }

  toggleStateSearch(): void {
    this.showStateSearch = !this.showStateSearch;
  }

  setExpeditedFilter(value: 'noExpedited' | 'expedited') {
  this.expeditedFilter = value;
  this. applyFilters(); // reuse your existing method
}
}
