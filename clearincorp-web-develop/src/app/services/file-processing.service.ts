import { Injectable } from '@angular/core';
import imageCompression from 'browser-image-compression';

/**
 * Interface for the result of processing a file.
 * MAKE SURE THIS 'export' KEYWORD IS HERE.
 */
export interface ProcessedFile {
  rawFile: File;
  isCompressed: boolean;
  originalSize: number;
  previewUrl: string; // The blob URL for preview
}

@Injectable({
  providedIn: 'root'
})
export class FileProcessingService {

  constructor() { }

  /**
   * Processes a single file. Compresses images, passes PDFs through.
   * MAKE SURE THIS METHOD IS PUBLIC (it is by default).
   * @param file The original File object
   * @returns A Promise resolving to a ProcessedFile object
   */
  async processFile(file: File): Promise<ProcessedFile> {
    
    const originalSize = file.size;

    // --- Image Compression Logic ---
    if (file.type.startsWith('image/')) {
      const options = {
        maxSizeMB: 1,
        maxWidthOrHeight: 1920,
        useWebWorker: true,
        initialQuality: 0.8
      };
      
      try {
        const compressedBlob = await imageCompression(file, options);
        const compressedFile = new File([compressedBlob], file.name, {
          type: file.type,
          lastModified: Date.now(),
        });

        return {
          rawFile: compressedFile,
          isCompressed: true,
          originalSize: originalSize,
          previewUrl: URL.createObjectURL(compressedFile)
        };

      } catch (error) {
        console.warn('Image compression failed, using original file.', error);
        // Fallback to original file if compression fails
        return {
          rawFile: file,
          isCompressed: false,
          originalSize: originalSize,
          previewUrl: URL.createObjectURL(file)
        };
      }
    }

    // --- PDF / Other File Logic ---
    return {
      rawFile: file,
      isCompressed: false,
      originalSize: originalSize,
      previewUrl: URL.createObjectURL(file) // Use original file for URL
    };
  }
}