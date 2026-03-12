import { Component, Inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

export interface DeleteDialogData {
  taskTitle: string;
  confirmationText: string;
}

@Component({
  selector: 'app-delete-confirmation-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule
  ],
  template: `
    <h2 mat-dialog-title class="dialog-title">
      <mat-icon class="icon-color">warning</mat-icon>
      Are you sure?
    </h2>
    <mat-dialog-content class="dialog-content">
      <p>
        This action <strong>cannot</strong> be undone. This will permanently delete the
        task:
      </p>
      <p class="task-title-emphasis">
        <strong>{{ data.taskTitle }}</strong>
      </p>
    
      <p>
        Please type the task title to confirm.
      </p>
      <mat-form-field appearance="outline" class="full-width">
        <mat-label>Type to confirm</mat-label>
        <input 
          matInput 
          [(ngModel)]="confirmationInput"
          (ngModelChange)="onInputChange($event)"
          placeholder="{{ data.confirmationText }}"
          cdkFocusInitial
        />
      </mat-form-field>
      
     
    </mat-dialog-content>
    <mat-dialog-actions align="end" style="margin-bottom:5px;">
      <button mat-flat-button class="button-prev" (click)="onCancel()">Cancel</button>
      <button 
        mat-flat-button 
        class="button-all"
        [disabled]="!isConfirmed()"
        (click)="onConfirm()"
      >
        Delete this task
      </button>
    
    </mat-dialog-actions>
  `,
  styles: [`
    .dialog-title {
      display: flex;
      align-items: center;
      gap: 8px;
      color: #dc2626; /* red-600 */
      font-weight: 700;
      margin-top: 12px;
    }
    .dialog-content {
      color: #374151; /* gray-700 */
    }
    .icon-color{
      color : #ea2b2bff;
    }
    .task-title-emphasis {
      background-color: #f3f4f6; /* gray-100 */
      border: 1px solid #e5e7eb; /* gray-200 */
      border-radius: 6px;
      padding: 10px;
      font-size: 15px;
    }
    .full-width {
      width: 100%;
      margin-top: 10px;
    }
      button[disabled] {
  background-color: #d6dbdf !important;
  color: #99a3a4 !important;
  cursor: none;
  opacity: .6;
  box-shadow: none !important;
}
  `]
})
export class DeleteConfirmationDialogComponent {
  confirmationInput: string = '';
    requiredText = '';

  isConfirmed = signal(false);
 

  constructor(
    public dialogRef: MatDialogRef<DeleteConfirmationDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: DeleteDialogData
  ) {
     const title = data?.taskTitle?.trim() ?? '';
    const confirm = data?.confirmationText?.trim();
    this.requiredText = (confirm && confirm.length > 0) ? confirm : title;
  }

  onInputChange(value: string) {
     const typed = (value ?? '').trim();
    // Compare against requiredText which is always a defined string
    this.isConfirmed.set(typed === this.requiredText);
  }

  onCancel(): void {
    this.dialogRef.close(false);
  }

  onConfirm(): void {
   
      if (this.isConfirmed()) this.dialogRef.close(true);
    
  }
}
