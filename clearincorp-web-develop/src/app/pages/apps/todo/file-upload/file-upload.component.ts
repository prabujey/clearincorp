import {
  Component,
  Input,
  Output,
  EventEmitter,
  ViewChild,
  ElementRef,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  OnDestroy,
} from "@angular/core";
import { CommonModule } from "@angular/common";
import { MatButtonModule } from "@angular/material/button";
import { MatIconModule } from "@angular/material/icon";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import { FileAttachment } from "src/app/models/todo-models/todo.model"; // Make sure this path is correct
import { MatSnackBar } from "@angular/material/snack-bar";
import { DomSanitizer, SafeUrl } from "@angular/platform-browser";
import {
  FileProcessingService,
  ProcessedFile,
} from "src/app/services/file-processing.service";
import { SnackBarService } from "src/app/shared/snackbar.service";

/**
 * Represents a file being managed by the component.
 */
interface ManagedFile {
  id: string;
  attachment: FileAttachment; // For UI display and output
  rawFile: File; // The processed file for API upload
  safePreviewUrl: SafeUrl; // For displaying image thumbnails
  isCompressed: boolean;
  originalSize: number;
}

@Component({
  selector: "app-file-upload",
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  template: `
    <div class="file-upload-container">
      <div class="upload-header">
        <label class="upload-label" for="file-upload-input">{{ label }}</label>
        <span class="file-count"
          >{{ managedFiles.length }}/{{ maxFiles }} files</span
        >
      </div>

      <div
        class="upload-area"
        (click)="fileInput.click()"
        (dragover)="onDragOver($event)"
        (dragleave)="onDragLeave($event)"
        (drop)="onDrop($event)"
        [class.uploading]="uploading"
        [class.disabled]="isDisabled()"
        [class.drag-over]="isDragOver"
      >
        <mat-icon class="upload-icon">cloud_upload</mat-icon>
        <p class="upload-text">
          {{
            uploading
              ? "Processing files..."
              : "Click to upload files or drag and drop"
          }}
        </p>
        <p class="upload-subtext">
          Images will be optimized. Max {{ maxFiles }} files.
        </p>
        <mat-spinner *ngIf="uploading" diameter="24"></mat-spinner>
      </div>

      <input
        #fileInput
        id="file-upload-input"
        type="file"
        multiple
        [accept]="accept"
        (change)="onFileSelect($event)"
        [disabled]="isDisabled()"
        class="visually-hidden"
      />

      <div *ngIf="managedFiles.length > 0" class="files-list">
        <h4 class="files-title">Attached Files</h4>
        <div *ngFor="let file of managedFiles" class="file-item">
          <div class="file-preview">
            <img
              *ngIf="file.attachment.type.startsWith('image/'); else pdfIcon"
              [src]="file.safePreviewUrl"
              alt="Preview of {{ file.attachment.name }}"
            />
            <ng-template #pdfIcon>
              <mat-icon class="file-icon-large">picture_as_pdf</mat-icon>
            </ng-template>
          </div>

          <div class="file-details">
            <p class="file-name">{{ file.attachment.name }}</p>
            <p class="file-meta">
              <span class="new-size">{{
                formatFileSize(file.attachment.size)
              }}</span>
            </p>
          </div>

          <div class="file-actions">
            <button
              mat-icon-button
              (click)="downloadFile(file.attachment)"
              matTooltip="Download"
            >
              <mat-icon>download</mat-icon>
            </button>
            <button
              mat-icon-button
              (click)="removeFile(file.id)"
              matTooltip="Remove"
            >
              <mat-icon>close</mat-icon>
            </button>
          </div>
        </div>
      </div>
    </div>
  `,
  // -----------------------------------------------------------------
  // ENHANCED "TOP APP" STYLES
  // -----------------------------------------------------------------
  styles: [
    `
      :host {
        --primary-color: #007bff;
        --border-color: #ddd;
        --border-radius: 8px;
        --text-color: #333;
        --text-color-light: #666;
        --bg-color-light: #f5f5f5;
        --bg-color-hover: #f9f9f9;
        --success-color: #28a745;
        --error-color: #dc3545;
        display: block;
        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto,
          Helvetica, Arial, sans-serif;
      }

      .file-upload-container {
        margin: 16px 0;
      }

      .upload-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 8px;
      }

      .upload-label {
        font-weight: 600;
        color: var(--text-color);
        font-size: 1rem;
      }

      .file-count {
        font-size: 0.8rem;
        color: var(--text-color-light);
      }

      .upload-area {
        border: 2px dashed var(--border-color);
        border-radius: var(--border-radius);
        padding: 24px;
        text-align: center;
        cursor: pointer;
        transition: all 0.2s ease-in-out;
        position: relative;
        background-color: #fff;
      }

      .upload-area:hover,
      .upload-area.drag-over {
        border-color: var(--primary-color);
        background-color: var(--bg-color-hover);
      }

      .upload-area.drag-over {
        box-shadow: 0 4px 12px rgba(0, 123, 255, 0.1);
      }

      .upload-area.uploading {
        pointer-events: none;
        opacity: 0.7;
      }

      .upload-area.disabled {
        cursor: not-allowed;
        background-color: var(--bg-color-light);
        opacity: 0.6;
      }

      .upload-icon {
        font-size: 40px;
        width: 40px;
        height: 40px;
        color: var(--primary-color);
        margin-bottom: 12px;
      }

      .upload-text {
        margin: 0 0 4px 0;
        color: var(--text-color);
        font-weight: 500;
      }

      .upload-subtext {
        margin: 0;
        font-size: 0.8rem;
        color: var(--text-color-light);
      }

      .visually-hidden {
        border: 0;
        clip: rect(0 0 0 0);
        height: 1px;
        margin: -1px;
        overflow: hidden;
        padding: 0;
        position: absolute;
        width: 1px;
      }

      .files-list {
        margin-top: 24px;
      }

      .files-title {
        font-size: 0.9rem;
        font-weight: 600;
        color: var(--text-color);
        margin-bottom: 8px;
        text-transform: uppercase;
        letter-spacing: 0.5px;
      }

      .file-item {
        display: flex;
        align-items: center;
        padding: 12px;
        background: var(--bg-color-light);
        border-radius: var(--border-radius);
        margin-bottom: 8px;
        gap: 12px;
      }

      .file-preview {
        flex-shrink: 0;
        width: 48px;
        height: 48px;
        border-radius: var(--border-radius);
        background-color: #e0e0e0;
        display: flex;
        align-items: center;
        justify-content: center;
        overflow: hidden;
      }

      .file-preview img {
        width: 100%;
        height: 100%;
        object-fit: cover;
      }

      .file-icon-large {
        font-size: 32px;
        width: 32px;
        height: 32px;
        color: var(--text-color-light);
      }

      .file-details {
        flex-grow: 1;
        min-width: 0; /* Prevents overflow */
      }

      .file-details .file-name {
        margin: 0 0 4px 0;
        font-weight: 500;
        color: var(--text-color);
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }

      .file-details .file-meta {
        margin: 0;
        font-size: 0.8rem;
        color: var(--text-color-light);
      }

      .file-meta .new-size {
        font-weight: 500;
        color: var(--text-color);
      }

      .file-meta .old-size {
        text-decoration: line-through;
        margin-left: 4px;
      }

      .compressed-tag {
        font-size: 0.75rem;
        font-weight: 600;
        color: var(--success-color);
        background-color: rgba(40, 167, 69, 0.1);
        padding: 2px 6px;
        border-radius: 4px;
        display: inline-block;
        margin-top: 4px;
      }

      .file-actions {
        display: flex;
        flex-shrink: 0;
        gap: 4px;
      }
    `,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush, // <-- OPTIMIZATION
})
export class FileUploadComponent implements OnDestroy {
  @Input() files: FileAttachment[] = [];
  @Input() maxFiles: number = 5;
  @Input() accept: string = "image/*,application/pdf";
  @Input() label: string = "Upload Files";

  @Output() filesChange = new EventEmitter<FileAttachment[]>();
  @Output() rawFilesChange = new EventEmitter<File[]>();

  @ViewChild("fileInput") fileInput!: ElementRef<HTMLInputElement>;

  uploading = false;
  isDragOver = false;
  managedFiles: ManagedFile[] = [];

  constructor(
    private snackBar: SnackBarService,
    private fileProcessingService: FileProcessingService, // <-- INJECT SERVICE
    private cdr: ChangeDetectorRef, // <-- INJECT ChangeDetectorRef
    private sanitizer: DomSanitizer // <-- INJECT DomSanitizer for previews
  ) {}

  isDisabled(): boolean {
    return this.uploading || this.managedFiles.length >= this.maxFiles;
  }

  // --- Drag and Drop Handlers ---
  onDragOver(event: DragEvent) {
    event.preventDefault();
    event.stopPropagation();
    if (!this.isDisabled()) {
      this.isDragOver = true;
      this.cdr.markForCheck();
    }
  }

  onDragLeave(event: DragEvent) {
    event.preventDefault();
    event.stopPropagation();
    this.isDragOver = false;
    this.cdr.markForCheck();
  }

  onDrop(event: DragEvent) {
    event.preventDefault();
    event.stopPropagation();
    this.isDragOver = false;
    if (this.isDisabled()) return;

    const files = event.dataTransfer?.files;
    if (files) {
      this.handleSelectedFiles(Array.from(files));
    }
  }

  // --- File Selection Handler ---
  onFileSelect(event: Event) {
    const input = event.target as HTMLInputElement;
    const files = input.files;
    if (files) {
      this.handleSelectedFiles(Array.from(files));
    }
    input.value = ""; // Clear the input
  }

  /**
   * Main logic for processing selected files (from click or drop)
   */
  private async handleSelectedFiles(selectedFiles: File[]) {
    // 1. Filter for only allowed types
    const validFiles = selectedFiles.filter(
      (file) =>
        file.type.startsWith("image/") || file.type === "application/pdf"
    );

    const invalidCount = selectedFiles.length - validFiles.length;
    if (invalidCount > 0) {
      this.snackBar.show(
        `${invalidCount} file(s) were ignored (only images and PDFs allowed)`,
        "Close"
      );
    }
    if (validFiles.length === 0) return;

    // 2. Hard limit: never allow more than maxFiles total
    const availableSlots = this.maxFiles - this.managedFiles.length;

    if (availableSlots <= 0) {
      this.snackBar.show(
        `You can upload only ${this.maxFiles} files.`,
        "Close"
      );
      return;
    }

    const allowedFiles = validFiles.slice(0, availableSlots);
    const ignoredCount = validFiles.length - allowedFiles.length;

    if (ignoredCount > 0) {
      this.snackBar.show(
        //  `Only ${availableSlots} file(s) added. ${ignoredCount} ignored (max ${this.maxFiles}).`,
        `Some files were skipped. Max ${this.maxFiles} attachments allowed`,
        "Close"
      );
    }

    this.uploading = true;
    this.cdr.markForCheck();

    try {
      // 3. Process only allowed files
      const processingPromises = allowedFiles.map((file) =>
        this.fileProcessingService.processFile(file)
      );
      const processedFiles = await Promise.all(processingPromises);

      // 4. Create ManagedFile objects
      const newManagedFiles = processedFiles.map((processed) =>
        this.createManagedFile(processed)
      );

      this.managedFiles = [...this.managedFiles, ...newManagedFiles];
      this.emitChanges();
    } catch (error) {
      console.error("File processing failed:", error);
      this.snackBar.showError("File processing failed", "Close");
    } finally {
      this.uploading = false;
      this.cdr.markForCheck();
    }
  }

  /**
   * Helper function to create the UI object
   */
  private createManagedFile(processedFile: ProcessedFile): ManagedFile {
    const id = Math.random().toString(36).substr(2, 9);

    // Create the FileAttachment (for output)
    const fileAttachment: FileAttachment = {
      id: id,
      name: processedFile.rawFile.name,
      size: processedFile.rawFile.size,
      type: processedFile.rawFile.type,
      url: processedFile.previewUrl,
      uploadDate: new Date(),
    };

    // Create the ManagedFile (for internal state)
    return {
      id: id,
      attachment: fileAttachment,
      rawFile: processedFile.rawFile,
      safePreviewUrl: this.sanitizer.bypassSecurityTrustUrl(
        processedFile.previewUrl
      ),
      isCompressed: processedFile.isCompressed,
      originalSize: processedFile.originalSize,
    };
  }

  removeFile(id: string) {
    const fileToRemove = this.managedFiles.find((f) => f.id === id);
    if (fileToRemove) {
      // Clean up the object URL to prevent memory leaks
      URL.revokeObjectURL(fileToRemove.attachment.url);
    }
    this.managedFiles = this.managedFiles.filter((f) => f.id !== id);
    this.emitChanges();
    this.cdr.markForCheck(); // Trigger change detection
  }

  private emitChanges() {
    this.filesChange.emit(this.managedFiles.map((f) => f.attachment));
    this.rawFilesChange.emit(this.managedFiles.map((f) => f.rawFile));
  }

  downloadFile(file: FileAttachment) {
    const link = document.createElement("a");
    link.href = file.url;
    link.download = file.name;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }

  formatFileSize(bytes: number): string {
    if (bytes === 0) return "0 Bytes";
    const k = 1024;
    const sizes = ["Bytes", "KB", "MB", "GB"];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + " " + sizes[i];
  }

  ngOnDestroy() {
    // Clean up all blob URLs when component is destroyed
    this.managedFiles.forEach((file) => {
      URL.revokeObjectURL(file.attachment.url);
    });
  }
}
