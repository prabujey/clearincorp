import { Component, EventEmitter, Input, Output } from "@angular/core";
import { CommonModule } from "@angular/common";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatSelectModule } from "@angular/material/select";
import { MatChipsModule } from "@angular/material/chips";
import { MatIconModule } from "@angular/material/icon";
import { MatButtonModule } from "@angular/material/button";
import { CompanyFormationService } from "src/app/services/apps/formation-wizard/formation-wizard.service";

type RawValue = "ALL" | string; // state code like 'CA'

@Component({
  selector: "app-state-selector",
  standalone: true,
  imports: [
    CommonModule,
    MatFormFieldModule,
    MatSelectModule,
    MatChipsModule,
    MatIconModule,
    MatButtonModule,
  ],
  template: `
    <div class="state-selector">

      <!-- Main State Selection Dropdown -->
      <mat-form-field appearance="outline" class="full-width">
        <mat-label>{{ label }}</mat-label>
        <mat-select
          multiple
          [value]="rawSelection"
          (selectionChange)="onRawSelectionChange($event.value)"
          placeholder="Select state(s)"
        >
         <mat-select-trigger>
            {{ triggerText }}
          </mat-select-trigger>

          <mat-option value="ALL" class="all-option">
            <strong>All States</strong>
          </mat-option>

          <mat-optgroup *ngIf="states.length" label="States">
            <mat-option *ngFor="let s of states" [value]="s">
              {{ s }}
            </mat-option>
          </mat-optgroup>
        </mat-select>
      </mat-form-field>

      <!-- Chips for current selection (Improved User Experience) -->
      <div *ngIf="rawSelection.length > 0" class="chips">
        <mat-chip-set role="listbox">

          <!-- Case 1: All States is selected (exclusive display) -->
          <ng-container *ngIf="rawSelection.includes('ALL'); else individualStates">
            <mat-chip removable (removed)="remove('ALL')" color="primary" selected>
              All States Selected
              <mat-icon matChipRemove>cancel</mat-icon>
            </mat-chip>
          </ng-container>

          <!-- Case 2: Individual states are selected (responsive wrapping) -->
          <ng-template #individualStates>
            <mat-chip *ngFor="let state of rawSelection" removable (removed)="remove(state)">
              {{ state }}
              <mat-icon matChipRemove>cancel</mat-icon>
            </mat-chip>
          </ng-template>

        </mat-chip-set>
      </div>
    </div>
  `,
  styles: [
    `
      .state-selector {
        margin: 0px 0;
      }
      .full-width {
        width: 100%;
      }
      .all-option {
        border-bottom: 1px solid #e0e0e0;
        margin-bottom: 8px;
      }
      .chips {
        margin-bottom: 10px;
      }
      /* mat-chip-set ensures responsive wrapping on smaller screens */
      mat-chip-set {
        display: flex;
        flex-wrap: wrap; /* Key for responsiveness */
        gap: 8px;
      }
    `,
  ],
})
export class StateSelectorComponent {
  /** Label text */
  @Input() label = "States";

  /** Available states */
  @Input() states: string[] = [];

  /** Current raw selection: 'ALL' and/or state codes */
  @Input() rawSelection: RawValue[] = [];

  /** Emits the flattened states (empty array === All States) */
  @Output() statesChange = new EventEmitter<string[]>();

  /** Emits raw selection too, if the parent wants it */
  @Output() selectionChange = new EventEmitter<RawValue[]>();

  constructor(private formationService: CompanyFormationService) {}

  ngOnInit(): void {
    this.fetchStates();
  }

    get triggerText(): string {
    if (!this.states?.length) return 'Select state(s)';

    // ALL picked (or empty meaning ALL by convention)
    if (this.rawSelection.includes('ALL') || this.rawSelection.length === 0) {
      return `All States (${this.states.length})`;
    }

    const n = this.rawSelection.length;
    return `(${n}) ${n === 1 ? 'State' : 'States'} Selected`;
  }

  onRawSelectionChange(values: RawValue[]) {
    // “ALL” behaves exclusively (like your company selector)
    if (values.includes("ALL")) {
      this.rawSelection = this.rawSelection.includes("ALL") ? [] : ["ALL"];
    } else {
      // Filter out 'ALL' if individual states are selected
      this.rawSelection = values.filter((v) => v !== "ALL");
    }

    this.selectionChange.emit(this.rawSelection);
    this.statesChange.emit(this.computeEffectiveStates(this.rawSelection));
  }

  remove(item: RawValue) {
    this.rawSelection = this.rawSelection.filter((x) => x !== item);
    this.selectionChange.emit(this.rawSelection);
    this.statesChange.emit(this.computeEffectiveStates(this.rawSelection));
  }

  private computeEffectiveStates(raw: RawValue[]): string[] {
    // Empty or ALL → treat as “All states” (emit empty list by convention)
    if (raw.length === 0 || raw.includes("ALL")) return [];
    // Unique list of selected states
    return Array.from(new Set(raw.filter((v) => v !== "ALL"))) as string[];
  }

  private fetchStates(): void {
    this.formationService.getStates().subscribe({
      next: (data: string[]) => {
        this.states = data;
      },
    });
  }
}
