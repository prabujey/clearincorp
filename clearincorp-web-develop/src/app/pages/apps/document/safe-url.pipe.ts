// safe-url.pipe.ts
import { Pipe, PipeTransform } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

/**
 * SafeUrlPipe - Safely transforms URLs for use in iframes/embeds.
 * SECURITY: Only allows URLs from trusted domains to prevent XSS attacks.
 */
@Pipe({
    name: 'safeUrl',
    standalone: false
})
export class SafeUrlPipe implements PipeTransform {
  // Whitelist of allowed URL patterns (domains and protocols)
  private readonly ALLOWED_PATTERNS: RegExp[] = [
    // AWS S3 (legacy/migration period)
    /^https:\/\/.*\.s3\..*\.amazonaws\.com\//i,  // S3 presigned URLs
    /^https:\/\/.*\.execute-api\..*\.amazonaws\.com\//i, // API Gateway

    // Cloudflare R2 (new storage provider)
    /^https:\/\/[a-f0-9]+\.r2\.cloudflarestorage\.com\//i,  // R2 direct URLs
    /^https:\/\/pub-[a-f0-9]+\.r2\.dev\//i,                  // R2 public bucket URLs

    // App domains
    /^https:\/\/clearincorp\.com\//i,            // Production domain
    /^https:\/\/dev\.clearincorp\.com\//i,       // Dev domain
    /^https:\/\/.*\.clearincorp\.com\//i,        // All subdomains

    // Local protocols
    /^blob:/i,                                    // Blob URLs (local file handling)
    /^data:application\/pdf/i,                   // PDF data URLs
  ];

  constructor(private sanitizer: DomSanitizer) {}

  transform(url: string | SafeResourceUrl | undefined): SafeResourceUrl {
    if (!url) {
      // Return an empty safe URL if url is undefined
      return this.sanitizer.bypassSecurityTrustResourceUrl('');
    }

    if (typeof url === 'string') {
      // Validate URL against whitelist before bypassing security
      if (this.isAllowedUrl(url)) {
        return this.sanitizer.bypassSecurityTrustResourceUrl(url);
      }

      // Log warning for security auditing (don't expose in production)
      console.warn('SafeUrlPipe: Blocked untrusted URL');
      return this.sanitizer.bypassSecurityTrustResourceUrl('');
    }

    // If the value is already a SafeResourceUrl, just return it.
    return url;
  }

  private isAllowedUrl(url: string): boolean {
    // Check against whitelist patterns
    return this.ALLOWED_PATTERNS.some(pattern => pattern.test(url));
  }
}
