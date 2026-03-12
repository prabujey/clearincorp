// import { Component, Inject } from '@angular/core';
// import { CommonModule } from '@angular/common';
// import { FormsModule } from '@angular/forms';
// import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
// import { MatButtonModule } from '@angular/material/button';
// import { MatFormFieldModule } from '@angular/material/form-field';
// import { MatInputModule } from '@angular/material/input';

// export type EditTitleDialogData = {
//   title: string;       // dialog header
//   value: string;       // initial title
//   placeholder?: string;
//   saveText?: string;   // OK
// };

// @Component({
//   selector: 'app-edit-title-dialog',
//   standalone: true,
//   imports: [CommonModule, FormsModule, MatDialogModule, MatButtonModule, MatFormFieldModule, MatInputModule],
//   template: `
//     <h2 mat-dialog-title>{{ data.title }}</h2>

//     <div mat-dialog-content>
//       <mat-form-field appearance="outline" class="w-100">
//         <mat-label>Title</mat-label>
//         <input matInput [(ngModel)]="val" [placeholder]="data.placeholder ?? ''" />
//       </mat-form-field>
//     </div>

//     <div mat-dialog-actions align="end">
//       <button mat-button (click)="close(null)">Cancel</button>
//       <button mat-flat-button class="cic-ok" (click)="save()">
//         {{ data.saveText ?? 'OK' }}
//       </button>
//     </div>
//   `,
//   styles: [`
//     .w-100{ width:100%; }
//     .cic-ok.mat-mdc-unelevated-button,
//     .cic-ok.mat-mdc-raised-button{
//       background:#bf0a30 !important;
//       color:#fff !important;
//       border-radius:999px !important;
//       padding:10px 22px !important;
//       font-weight:700 !important;
//     }
//     .cic-ok:hover{ filter:brightness(0.92); }
//   `],
// })
// export class EditTitleDialogComponent {
//   val: string;

//   constructor(
//     private readonly ref: MatDialogRef<EditTitleDialogComponent>,
//     @Inject(MAT_DIALOG_DATA) public data: EditTitleDialogData
//   ) {
//     this.val = data.value ?? '';
//   }

//   close(v: string | null) { this.ref.close(v); }

//   save() { this.ref.close(this.val); } // (same behavior as prompt: can be empty, caller decides)
// }

// src/app/shared/edit-content-dialog.component.ts (Conceptual)

// src/app/shared/edit-content-dialog.component.ts (Conceptual)

import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatDialogModule } from '@angular/material/dialog';
export interface EditDialogData {
  content: string;
  title: string;
  isTitle: boolean; 
  mainContent: string; 
  secondaryContent?: string; // Topic Title
}
export interface EditDialogResult { 
  mainContent: string;
  secondaryContent?: string;
}


@Component({
  selector: 'app-edit-content-dialog',
  standalone: true,
  imports: [
    CommonModule, FormsModule, MatButtonModule, MatInputModule, MatFormFieldModule, MatDialogModule
  ],
  template: `
  <h2 mat-dialog-title>{{ data.title }}</h2>

<mat-dialog-content>
  <div style="padding-top: 10px;">
    <mat-form-field appearance="outline" style="width: 100%;">
      <mat-label>Content</mat-label>
      <textarea 
        matInput 
        [(ngModel)]="editedContent" 
        rows="10" 
        placeholder="Enter your reply...">
      </textarea>
    </mat-form-field>
  </div>
</mat-dialog-content>

<mat-dialog-actions align="end" style="padding: 16px 24px;">
  <button mat-button (click)="onCancel()">Cancel</button>
  <button 
    mat-flat-button class="button-all"
    color="primary" 
    [mat-dialog-close]="{ mainContent: editedContent } "
    [disabled]="!editedContent?.trim()">
    Save
  </button>
</mat-dialog-actions>
  `,
  styles: [`
   .w-100{ width:100%; }
    .cic-ok.mat-mdc-unelevated-button,
    .cic-ok.mat-mdc-raised-button{
      background:#bf0a30 !important;
      color:#fff !important;
      border-radius:999px !important;
      padding:10px 22px !important;
      font-weight:700 !important;
    }
    .cic-ok:hover{ filter:brightness(0.92); }
 
    `],
})
export class EditContentDialogComponent {
  editedContent: string;
  editedTitle: string;
  isSaving = false;

  constructor(
    public dialogRef: MatDialogRef<EditContentDialogComponent, EditDialogResult | null>,
    @Inject(MAT_DIALOG_DATA) public data: EditDialogData
  ) {
    this.editedContent = data.mainContent || '';
    this.editedTitle = data.secondaryContent || '';
  }
  private getValueOrUndefined(value: string | undefined): string | undefined {
    if (value === null || value === undefined || value.trim().length === 0) {
        return undefined;
    }
    return value.trim();
}
ngOnInit() {
  // Use a fallback to empty string to prevent the .trim() error
  // If editing a post, data.title exists. If a reply, it might be null.
  this.editedTitle = this.data?.title ?? '';
  
  // Some parts of your code used 'content', others 'mainContent'
  // Check both to be safe
  this.editedContent = this.data?.content || this.data?.mainContent || '';
}
  isValid(): boolean {
    const isTitleMode = this.data.isTitle;
    
    // Use the class properties, which are guaranteed to be initialized as strings:
    const newMain = this.editedContent.trim();
    const originalMain = this.data.mainContent.trim(); // data.mainContent is guaranteed to exist by interface

    if (isTitleMode) {
        // Title mode checks two fields (Title/secondary and Description/main)
        const newTitle = this.editedTitle.trim();
        const originalTitle = this.data.secondaryContent?.trim() ?? ''; // secondaryContent is optional
        // Title is mandatory in Title mode
        if (newTitle.length === 0) return false;

        // At least one of the two fields must have changed
        const titleChanged = newTitle !== originalTitle;
        const descChanged = newMain !== originalMain;

        return titleChanged || descChanged;
        
    } else {
        // Post/Reply mode (single field)
        const newMain = this.editedContent.trim();
        
        // 🟢 FIX 2: Define and initialize originalContent
        const originalContent = this.data.mainContent.trim() ?? '';
        // Content must be non-empty AND must have changed
        return newMain.length > 0 && newMain !== originalContent;
    }
  }
 
  

  onCancel(): void {
    this.dialogRef.close(null);
  }

  onSave(): void {
    this.isSaving = true;
    
    // 1. Get the trimmed values
    const newTitle = this.data.isTitle ? this.editedTitle : undefined;
    
    // 2. Check for empty title (Title must always be present in Title mode)
    if (this.data.isTitle && newTitle && newTitle.trim().length === 0) {
        this.isSaving = false;
        // You should ideally use a toast/snackbar here, but alert works for now.
        alert("Topic Title cannot be empty.");
        return; 
    }
    
    // 3. Construct the result object
    const result: EditDialogResult = {
        // mainContent (Description or Post/Reply content) is always required by the payload structure
        mainContent: this.editedContent.trim(), 
        
        // Use the helper to ensure secondaryContent is UNDEFINED if the user cleared it.
        secondaryContent: this.data.isTitle 
            ? this.getValueOrUndefined(newTitle) 
            : undefined 
    };

    this.dialogRef.close(result);
  }
}