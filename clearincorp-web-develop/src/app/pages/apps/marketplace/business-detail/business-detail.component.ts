import {
  Component,
  OnInit,
  DestroyRef,
  HostListener,
  computed,
  signal,
  inject,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { BusinessHubService, DetailedBusinessVM } from 'src/app/services/business-hub.service';

type TabId = 'overview' | 'reviews' | 'gallery';

@Component({
  selector: 'app-business-detail',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './business-detail.component.html',
  styleUrls: ['./business-detail.component.scss'],
})
export class BusinessDetailComponent implements OnInit {
  isSaved = signal(false);
  activeTab = signal<TabId>('overview');
  business = signal<DetailedBusinessVM | null>(null);

  galleryLoading = signal(false);
  galleryError = signal<string | null>(null);
  private inFlight = false;

  // ✅ Lightbox (Google-like)
  lightboxOpen = signal(false);
  lightboxIndex = signal(0);

  lightboxUrl = computed(() => {
    const b = this.business();
    const urls = b?.gallery ?? [];
    const i = this.lightboxIndex();
    return urls[i] ?? b?.image ?? '';
  });

  readonly tabs = [
    { id: 'overview', label: 'Overview' },
    { id: 'reviews', label: 'Reviews' },
    { id: 'gallery', label: 'Gallery' },
  ] as const;

  private hub = inject(BusinessHubService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private destroyRef = inject(DestroyRef);

  ngOnInit(): void {
    this.route.paramMap
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((p) => {
        const id = Number(p.get('id'));
        if (!id) return;

        this.hub
          .getDetailsVM(id)
          .pipe(takeUntilDestroyed(this.destroyRef))
          .subscribe((b: DetailedBusinessVM) => {
            this.business.set(b);
            if (b?.id != null) {
              this.loadGalleryAndSetHero(b.id);
            }
          });
      });
  }

  private loadGalleryAndSetHero(businessId: number): void {
    if (this.inFlight) return;
    this.inFlight = true;

    this.galleryLoading.set(true);
    this.galleryError.set(null);

    this.hub
      .getGalleryUrls(businessId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (urls) => {
          const b = this.business();
          const list = (urls ?? []).filter(Boolean);

          if (b) {
            const hero = list[0] ?? b.image ?? '/assets/placeholder-business.jpg';
            this.business.set({
              ...b,
              image: hero,
              gallery: list,
            });
          }

          this.galleryLoading.set(false);
          this.inFlight = false;
        },
        error: (err) => {
          console.error('Failed to load gallery', err);
          this.galleryError.set('Failed to load images. Please try again.');
          this.galleryLoading.set(false);
          this.inFlight = false;
        },
      });
  }

  setActiveTab(tabId: TabId): void {
    this.activeTab.set(tabId);
  }

  onBack(): void {
    if (history.length > 1) history.back();
    else this.router.navigate(['/apps/business-hub']);
  }

  onJoinNow(): void {
    this.router.navigate(['/apps/register']);
  }

  toggleSave(): void {
    this.isSaved.set(!this.isSaved());
  }

  shareBusiness(): void {
    const b = this.business();
    if (!b) return;

    if ((navigator as any).share) {
      (navigator as any).share({ title: b.name, text: b.description, url: window.location.href });
    } else {
      navigator.clipboard.writeText(window.location.href);
      alert('Link copied to clipboard!');
    }
  }

  getStarsArray(rating: number): number[] {
    return Array.from({ length: 5 }, (_, i) => (i < Math.floor(rating) ? 1 : 0));
  }

  // ===================== ✅ Lightbox =====================

  openLightbox(index: number): void {
    const b = this.business();
    const total = b?.gallery?.length ?? 0;
    if (!total) return;

    this.lightboxIndex.set(Math.min(Math.max(index, 0), total - 1));
    this.lightboxOpen.set(true);
    this.lockScroll(true);
  }

  closeLightbox(): void {
    this.lightboxOpen.set(false);
    this.lockScroll(false);
  }

  nextImage(): void {
    const b = this.business();
    const total = b?.gallery?.length ?? 0;
    if (!total) return;

    const next = (this.lightboxIndex() + 1) % total;
    this.lightboxIndex.set(next);
  }

  prevImage(): void {
    const b = this.business();
    const total = b?.gallery?.length ?? 0;
    if (!total) return;

    const prev = (this.lightboxIndex() - 1 + total) % total;
    this.lightboxIndex.set(prev);
  }

  @HostListener('document:keydown', ['$event'])
  onKeydown(ev: KeyboardEvent): void {
    if (!this.lightboxOpen()) return;

    if (ev.key === 'Escape') this.closeLightbox();
    if (ev.key === 'ArrowRight') this.nextImage();
    if (ev.key === 'ArrowLeft') this.prevImage();
  }

  private lockScroll(lock: boolean): void {
    if (typeof document === 'undefined') return;
    document.body.style.overflow = lock ? 'hidden' : '';
  }

  onHeroError(e: Event): void {
    const img = e.target as HTMLImageElement;
    img.src = '/assets/placeholder-business.jpg';
  }

  onGalleryError(e: Event): void {
    const img = e.target as HTMLImageElement;
    img.style.opacity = '0.4';
  }

  normalizePhone(phone: string | null | undefined): string {
  if (!phone) return '';
  return phone.replace(/[^\d+]/g, '');
}

normalizeWebsite(url: string | null | undefined): string {
  if (!url) return '';
  const trimmed = url.trim();
  if (!trimmed) return '';
  return /^https?:\/\//i.test(trimmed) ? trimmed : `https://${trimmed}`;
}


}
