import { Component, EventEmitter, Input, Output } from "@angular/core";
import { CommonModule } from "@angular/common";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatSelectModule } from "@angular/material/select";
import { MatChipsModule } from "@angular/material/chips";
import { MatIconModule } from "@angular/material/icon";
import { MatButtonModule } from "@angular/material/button";
import { CompanyFormationService } from "src/app/services/apps/formation-wizard/formation-wizard.service";

type RawValue = "ALL" | string; // activity code or label

@Component({
  selector: "app-principal-activity-selector",
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
    <div class="activity-selector">
      <mat-form-field appearance="outline" class="full-width">
        <mat-label>{{ label }}</mat-label>

        <mat-select
          multiple
          [value]="rawSelection"
          (selectionChange)="onRawSelectionChange($event.value)"
          placeholder="Select activity/activities"
        >
          <!-- 👇 Count / summary shown in the input field -->
          <mat-select-trigger>
            {{ triggerText }}
          </mat-select-trigger>

          <mat-option value="ALL" class="all-option">
            <strong>All Activities</strong>
          </mat-option>

          <mat-optgroup *ngIf="activities.length" label="Activities">
            <mat-option *ngFor="let a of activities" [value]="a">
              {{ a }}
            </mat-option>
          </mat-optgroup>
        </mat-select>
      </mat-form-field>

      <!-- Chips for current selection -->
      <div *ngIf="rawSelection.length > 0" class="chips">
        <mat-chip-set role="listbox">
          <ng-container *ngIf="rawSelection.includes('ALL'); else individual">
            <mat-chip removable (removed)="remove('ALL')" color="primary" selected>
              All Activities Selected
              <mat-icon matChipRemove>cancel</mat-icon>
            </mat-chip>
          </ng-container>

          <ng-template #individual>
            <mat-chip *ngFor="let item of rawSelection" removable (removed)="remove(item)">
              {{ item }}
              <mat-icon matChipRemove>cancel</mat-icon>
            </mat-chip>
          </ng-template>
        </mat-chip-set>
      </div>
    </div>
  `,
  styles: [`
    .activity-selector { margin: 0; }
    .full-width { width: 100%; }
    .all-option { border-bottom: 1px solid #e0e0e0; margin-bottom: 8px; }
    .chips {  margin-bottom: 15px;}
    mat-chip-set { display: flex; flex-wrap: wrap; gap: 8px; }
  `],
})
export class PrincipalActivitySelectorComponent {
  /** Label text */
  @Input() label = "Principal Activity";

  /** Available activities */
  @Input() activities: string[] = [];

  /** Current raw selection: 'ALL' and/or activity codes/labels */
  @Input() rawSelection: RawValue[] = [];

  /** Emits the flattened activities (empty array === All Activities) */
  @Output() activitiesChange = new EventEmitter<string[]>();

  /** Emits raw selection too, if the parent wants it */
  @Output() selectionChange = new EventEmitter<RawValue[]>();

  constructor(private formationService: CompanyFormationService) {}

  ngOnInit(): void {
    // If you already fetch activities in parent, pass via @Input and remove this.
    this.fetchActivities();
  }

  /** Text shown inside the select input */
  get triggerText(): string {
    if (!this.activities?.length) return 'Select activity/activities';

    if (this.rawSelection.includes('ALL') || this.rawSelection.length === 0) {
      return `All Activities (${this.activities.length})`;
    }

    const n = this.rawSelection.length;
     return `(${n}) Principal ${n === 1 ? 'Activity' : 'Activities'} Selected`;
  }

  onRawSelectionChange(values: RawValue[]) {
    if (values.includes("ALL")) {
      this.rawSelection = this.rawSelection.includes("ALL") ? [] : ["ALL"];
    } else {
      this.rawSelection = values.filter(v => v !== "ALL");
    }
    this.selectionChange.emit(this.rawSelection);
    this.activitiesChange.emit(this.computeEffective(this.rawSelection));
  }

  remove(item: RawValue) {
    this.rawSelection = this.rawSelection.filter(x => x !== item);
    this.selectionChange.emit(this.rawSelection);
    this.activitiesChange.emit(this.computeEffective(this.rawSelection));
  }

  private computeEffective(raw: RawValue[]): string[] {
    if (raw.length === 0 || raw.includes("ALL")) return [];
    return Array.from(new Set(raw.filter(v => v !== "ALL"))) as string[];
  }

  private fetchActivities(): void {
    this.formationService.getPrincipalActivities?.().subscribe({
      next: (data: string[]) => (this.activities = data || []),
      error: () => (this.activities = this.activities ?? []),
    });
  }
}
