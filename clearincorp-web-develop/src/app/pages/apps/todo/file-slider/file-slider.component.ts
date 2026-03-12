import { Component, Input, OnInit, HostListener, ChangeDetectionStrategy, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { Inject, Optional } from '@angular/core';



export interface FileSliderData {
  files: string[];
  initialIndex?: number;
  title?: string;
}

@Component({
  selector: 'app-file-slider',
  standalone: true,
  imports: [
    CommonModule, 
    MatIconModule, 
    MatButtonModule, 
    MatProgressSpinnerModule,
    MatTooltipModule,
    MatDialogModule,
  ],
  template: `
    <div class="slider-backdrop"></div>
    <div class="slider-container">
      <!-- Header -->
      <div class="slider-header">
        <span class="file-name" [matTooltip]="getFileNameFromUrl(currentFile)">
          {{ getFileNameFromUrl(currentFile) }}
        </span>
        <span class="spacerr"></span>
        <span class="file-counter">File {{ currentIndex() + 1 }} of {{ files.length }}</span>
        <a mat-icon-button [href]="currentFile" target="_blank" download matTooltip="Download File">
          <mat-icon>download</mat-icon>
        </a>
        <button mat-icon-button (click)="dialogClose()" class="close-btn" matTooltip="Close (Esc)">
          <mat-icon>close</mat-icon>
        </button>
      </div>

      <!-- Content -->
      <div class="slider-content">
        <!-- Previous Button -->
        <button mat-icon-button class="nav-btn prev-btn" (click)="prev()" [disabled]="currentIndex() === 0" matTooltip="Previous (Left Arrow)">
          <mat-icon>keyboard_arrow_left</mat-icon>
        </button>

        <!-- File Viewer -->
        <div class="file-viewer">
          <div *ngIf="isLoading()" class="loading-indicator">
            <mat-spinner diameter="50"></mat-spinner>
          </div>
          <ng-container [ngSwitch]="getFileType(currentFile)">
            <!-- Image -->
            <img *ngSwitchCase="'image'" 
                 [src]="currentFile" 
                 alt="Attachment" 
                 class="file-content-image"
                 [class.hidden]="isLoading()"
                 (load)="isLoading.set(false)"
                 (error)="handleImageError()">
            
            <!-- PDF -->
           <iframe *ngSwitchCase="'pdf'"
        class="file-content-iframe"
        
        [class.hidden]="isLoading()"
        [src]="trustedUrl()"
        (load)="isLoading.set(false)">
</iframe>


            <!-- Other/Error -->
            <div *ngSwitchDefault class="file-content-other">
              <mat-icon class="other-icon">description</mat-icon>
              <h3>Unsupported Preview</h3>
              <p>This file type cannot be previewed directly.</p>
              <a mat-flat-button [href]="currentFile" target="_blank" download class="button-all">
                <mat-icon>download</mat-icon>
                Download {{ getFileNameFromUrl(currentFile) }}
              </a>
            </div>
          </ng-container>
        </div>

        <!-- Next Button -->
        <button mat-icon-button class="nav-btn next-btn" (click)="next()" [disabled]="currentIndex() === files.length - 1" matTooltip="Next (Right Arrow)">
          <mat-icon>keyboard_arrow_right</mat-icon>
        </button>
      </div>
    </div>
  `,
  styles: [`
    /* UPDATED: 
      - Backdrop now has a semi-transparent background and blur effect.
      - Container is perfectly centered (left: 50%).
    */
    .slider-backdrop {
      position: fixed; 
      top: 0;
      left: 0;
      width: 100%;
      height: 100%;
      background: rgba(0, 0, 0, 0.5); /* Changed from transparent */
      backdrop-filter: blur(4px); /* Added blur effect */
      -webkit-backdrop-filter: blur(4px); /* For Safari */
      z-index: 1040; /* Lower than container */
    }
    .slider-container {
     position: fixed;
      top: 50%;
      left: 50%; /* Changed from 55% to 50% for perfect centering */
      transform: translate(-50%, -50%);
      width: min(100vw - 32px, 1150px);
      height: min(100vh - 32px, 800px);
      background: #2b2b2b;
      border-radius: 14px;
      box-shadow: 0 10px 40px rgba(0,0,0,0.5);
      display: flex;
      flex-direction: column;
      overflow: hidden;
      z-index: 1050; /* Higher than backdrop */
      color: white;
    }
       .slider-header {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 8px 14px 8px 16px;
      min-height: 44px;
      background: #1e1e1e;
      border-bottom: 1px solid #444;
      flex-shrink: 0;
    }
    /* END UPDATED STYLES */

    .file-name {
      font-weight: 500;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
      flex-shrink: 1;
      min-width: 100px;
    }
    .spacerr { flex: 1 1 auto; }
    .file-counter {
      font-size: 14px;
      color: #aaa;
      white-space: nowrap;
    }
    .slider-header button, .slider-header a {
      color: white;
    }
    .slider-content {
      flex: 1 1 auto;
      display: flex;
      align-items: center;
      justify-content: space-between;
      min-height: 0;
      padding: 10px;
      gap: 10px;
    }
    .nav-btn {
      width: 50px;
      height: 50px;
      background: rgba(255, 255, 255, 0.1);
      color: white;
      z-index: 10;
      flex-shrink: 0;
    }
    .nav-btn:disabled {
      background: rgba(255, 255, 255, 0.05);
      color: #666;
      cursor: not-allowed;
    }
    .nav-btn mat-icon {
      font-size: 30px;
      height: 30px;
      width: 30px;
    }
    .file-viewer {
      flex: 1 1 auto;
      height: 100%;
      display: flex;
      align-items: center;
      justify-content: center;
      overflow: hidden;
      position: relative;
    }
    .loading-indicator {
      position: absolute;
      top: 50%;
      left: 50%;
      transform: translate(-50%, -50%);
      z-index: 5;
    }
    .file-content-image, .file-content-iframe {
      max-width: 100%;
      max-height: 100%;
      object-fit: contain;
      border-radius: 4px;
      border: none;
    }
    .file-content-iframe {
      width: 100%;
      height: 100%;
      background: white;
    }
    .file-content-other {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      color: #ccc;
      text-align: center;
      padding: 20px;
    }
    .other-icon {
      font-size: 80px;
      width: 80px;
      height: 80px;
      color: #777;
    }
    .file-content-other h3 {
      margin: 16px 0 8px;
    }
    .file-content-other p {
      margin-bottom: 24px;
      color: #999;
    }
    .button-all {
      background-color: #ec3252;
      color: white;
    }
    .hidden {
      visibility: hidden;
      opacity: 0;
    }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FileSliderComponent implements OnInit {
  @Input() files: string[] = [];
  @Input() initialIndex: number = 0;
  //@Output() close = new EventEmitter<void>();

  currentIndex = signal(0);
  currentFile = '';
  isLoading = signal(true);
  fileType = signal<'image' | 'pdf' | 'other'>('image');
  trustedUrl = signal<SafeResourceUrl | null>(null);

  isSwiping = signal(false);


  constructor(private sanitizer: DomSanitizer, @Optional() public dialogRef?: MatDialogRef<FileSliderComponent>,
    @Optional() @Inject(MAT_DIALOG_DATA) public dialogData?: FileSliderData) {}

  @HostListener('document:keydown', ['$event'])
  handleKeydown(event: KeyboardEvent) {
    if (event.key === 'Escape') {
      this.dialogClose();
    }
    if (event.key === 'ArrowRight') {
      this.next();
    }
    if (event.key === 'ArrowLeft') {
      this.prev();
    }
  }

   @HostListener('wheel', ['$event'])
  onWheel(event: WheelEvent) {
    // Check if it's primarily a horizontal swipe and we're not already processing one
    if (Math.abs(event.deltaX) > Math.abs(event.deltaY) && !this.isSwiping()) {
      // Prevent the browser's default back/forward navigation
      event.preventDefault(); 

      const threshold = 10; // Swipe sensitivity

      if (event.deltaX > threshold) {
        // Swiped left (fingers moving left) -> Go Next
        this.isSwiping.set(true);
        this.next();
      } else if (event.deltaX < -threshold) {
        // Swiped right (fingers moving right) -> Go Previous
        this.isSwiping.set(true);
        this.prev();
      }

      // If we triggered a swipe, set a timeout to reset the flag
      // This prevents one swipe from triggering multiple navigations
      if (this.isSwiping()) {
        setTimeout(() => {
          this.isSwiping.set(false);
        }, 500); // 500ms cooldown
      }
    }
    // If it's a vertical swipe (deltaY > deltaX), we do nothing
    // and let the browser handle it (e.g., scrolling a PDF).
  }

  ngOnInit(): void {
    if (this.dialogData?.files?.length) {
      this.files = this.dialogData.files;
      if (typeof this.dialogData.initialIndex === 'number') {
        this.initialIndex = this.dialogData.initialIndex!;
      }
    }
    this.goToIndex(this.initialIndex);
  }

  dialogClose(result?: any) { this.dialogRef?.close(result); }

  goToIndex(index: number): void {
    if (index < 0 || index >= this.files.length) return;
    
    this.isLoading.set(true);
    this.currentIndex.set(index);
    this.currentFile = this.files[index];
    const type = this.determineFileType(this.currentFile);
    this.fileType.set(type);

    if (type === 'pdf') {
    this.trustedUrl.set(
      this.sanitizer.bypassSecurityTrustResourceUrl(this.currentFile)
    );
  } else {
    this.trustedUrl.set(null);
  }
    // If it's not an image or PDF, we're not "loading" a preview
  if (type === 'other') this.isLoading.set(false);
  }

  next(): void {
    if (this.currentIndex() < this.files.length - 1) {
      this.goToIndex(this.currentIndex() + 1);
    }
  }

  prev(): void {
    if (this.currentIndex() > 0) {
      this.goToIndex(this.currentIndex() - 1);
    }
  }

  getSafeUrl(url: string): SafeResourceUrl {
    return this.sanitizer.bypassSecurityTrustResourceUrl(url);
  }

  determineFileType(url: string): 'image' | 'pdf' | 'other' {
    const ext = url.split('.').pop()?.toLowerCase()?.split('?')[0]; // Handle query params
    if (['png', 'jpg', 'jpeg', 'gif', 'webp', 'svg'].includes(ext!)) {
      return 'image';
    }
    if (ext === 'pdf') {
      return 'pdf';
    }
    return 'other';
  }

  getFileType(url: string): 'image' | 'pdf' | 'other' {
    // This is just for the ngSwitch in the template
    return this.fileType();
  }
  
  handleImageError() {
    this.isLoading.set(false);
    this.fileType.set('other');
  }

  getFileNameFromUrl(url: string): string {
    try {
        // Try parsing as URL to handle complex URLs with query params
        const parsedUrl = new URL(url);
        // Decode pathname (e.g., "file%20name.pdf" -> "file name.pdf")
        const pathname = decodeURIComponent(parsedUrl.pathname);
        // Get the last part of the path
        const filename = pathname.substring(pathname.lastIndexOf('/') + 1);
        return filename || 'file';
    } catch (e) {
        // Fallback for simple strings or relative paths
        return url.substring(url.lastIndexOf('/') + 1).split('?')[0] || 'file';
    }
  }
}
