import {
  Component,
  OnInit,
  DestroyRef,
  HostListener,
  signal,
  inject,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { forkJoin, of, combineLatest, Observable } from 'rxjs';
import { catchError, finalize, map, switchMap, tap } from 'rxjs/operators';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { BusinessHubService, BusinessCardVM } from 'src/app/services/business-hub.service';
import { BusinessHubApi } from 'src/app/services/business-hub.api';
import { AuditClickRequestDto, AuditClickResponseDto } from 'src/app/models/business-backend';

type RatingFilter = 'all' | '4.5' | '4.0' | '3.5';

@Component({
  selector: 'app-category-detail',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './category-detail.component.html',
  styleUrls: ['./category-detail.component.scss'],
})
export class CategoryDetailComponent implements OnInit {
  private hub = inject(BusinessHubService);
  private api = inject(BusinessHubApi);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private destroyRef = inject(DestroyRef);

  readonly fallbackHero = '/assets/placeholder-business.jpg';

  // route param: :value  (serviceId string OR "all")
  categoryValue = signal<string>('all');

  // UI state
  pageTitle = signal<string>('All Services');
  serviceName = signal<string>('All Services');

  searchQuery = signal<string>('');
  zip = signal<string>('');
  ratingFilter = signal<RatingFilter>('all');

  ratingMenuOpen = signal<boolean>(false);
  loading = signal<boolean>(false);

  // data
  businesses = signal<BusinessCardVM[]>([]);

  // cache to avoid refetching same gallery hero
  private heroCache = new Map<number, string>();
  private heroRequested = new Set<number>();

  ngOnInit(): void {
    combineLatest([this.route.paramMap, this.route.queryParamMap])
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(([pm, qm]) => {
        const value = (pm.get('value') ?? 'all').trim();
        this.categoryValue.set(value || 'all');

        const q = (qm.get('q') ?? '').trim();
        const zip = (qm.get('zip') ?? '').trim();

        this.searchQuery.set(q);
        this.zip.set(zip);

        // Title: label from query, else from serviceId -> serviceName
        const label = (qm.get('label') ?? '').trim();
        if (label) {
          this.pageTitle.set(label);
          this.serviceName.set(label);
        } else {
          this.applyTitleFromServiceId(value);
        }

        this.fetchBusinesses();
      });
  }

  private applyTitleFromServiceId(value: string): void {
    if (!value || value === 'all') {
      this.pageTitle.set('All Services');
      this.serviceName.set('All Services');
      return;
    }

    const sid = Number(value);
    if (!Number.isFinite(sid) || sid <= 0) {
      this.pageTitle.set('Business Services');
      this.serviceName.set('Business Services');
      return;
    }

    this.hub
      .getServiceName(sid)
      .pipe(
        catchError(() => of('Business Services')),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe((name) => {
        const t = name?.trim() || 'Business Services';
        this.pageTitle.set(t);
        this.serviceName.set(t);
      });
  }

  private fetchBusinesses(): void {
    const q = this.searchQuery()?.trim() || undefined;
    const zip = this.zip()?.trim() || undefined;

    const minRating =
      this.ratingFilter() === 'all' ? undefined : Number(this.ratingFilter());

    this.loading.set(true);

    this.hub
      .searchDirectory({
        q,
        zip,
        minRating,
        categoryValue: this.categoryValue() === 'all' ? undefined : this.categoryValue(),
        page: 0,
        size: 50,
      })
      .pipe(
        catchError(() => {
          // fallback (keeps old flow alive if directory endpoint fails)
          if (this.categoryValue() !== 'all' && !q && !zip && minRating == null) {
            return this.hub.getBusinessesByCategory(this.categoryValue()).pipe(
              catchError(() => of([] as BusinessCardVM[]))
            );
          }
          return of([] as BusinessCardVM[]);
        }),
        finalize(() => this.loading.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe((rows) => {
        this.businesses.set(rows || []);
        this.fillMissingHeroImages(rows || []);
      });
  }

  /**
   * ✅ If card hero is placeholder, fetch first gallery image:
   * listImages -> key -> public URL:
   * https://dev.clearincorp.com/business/2/images/image_1.jpg
   */
  private fillMissingHeroImages(rows: BusinessCardVM[]): void {
    const targets = rows.filter((b) => this.isPlaceholderOrEmpty(b.image));

    if (!targets.length) return;

    const calls: Observable<{ id: number; url: string | null }>[] = [];

    for (const b of targets) {
      if (this.heroCache.has(b.id)) continue;
      if (this.heroRequested.has(b.id)) continue;

      this.heroRequested.add(b.id);

      calls.push(
        this.hub.getFirstGalleryImage(b.id).pipe(
          map((url) => ({ id: b.id, url })),
          catchError(() => of({ id: b.id, url: null }))
        )
      );
    }

    if (!calls.length) return;

    forkJoin(calls)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((results) => {
        const next = [...this.businesses()];
        let changed = false;

        for (const r of results) {
          if (r.url) {
            this.heroCache.set(r.id, r.url);

            const idx = next.findIndex((x) => x.id === r.id);
            if (idx >= 0 && this.isPlaceholderOrEmpty(next[idx].image)) {
              next[idx] = { ...next[idx], image: r.url };
              changed = true;
            }
          }
        }

        if (changed) this.businesses.set(next);
      });
  }

  private isPlaceholderOrEmpty(v: string | null | undefined): boolean {
    const s = String(v ?? '').trim();
    if (!s) return true;
    return (
      s.includes('placeholder-business.jpg') ||
      s === '/assets/placeholder-business.jpg' ||
      s === 'assets/placeholder-business.jpg'
    );
  }

  // ===================== TEMPLATE HELPERS =====================

  filteredBusinesses(): BusinessCardVM[] {
    return this.businesses();
  }

  trackById(_: number, b: BusinessCardVM) {
    return b.id;
  }

  heroImageUrl(b: BusinessCardVM): string {
    return (b.image && !this.isPlaceholderOrEmpty(b.image)) ? b.image : this.fallbackHero;
  }

  onHeroImgError(e: Event): void {
    const img = e.target as HTMLImageElement;
    if (img && img.src !== this.fallbackHero) img.src = this.fallbackHero;
  }

  getStarsArray(r: number): number[] {
    const rating = Number(r || 0);
    const filled = Math.floor(rating);
    return Array.from({ length: 5 }, (_, i) => (i < filled ? 1 : 0));
  }

  getShortAddress(address: string): string {
    return (address || '').split(',')[0];
  }

  ratingLabel(): string {
    return this.ratingFilter() === 'all' ? 'All Ratings' : `${this.ratingFilter()}+`;
  }

  // ===================== ACTIONS =====================

onBack(): void {
  this.router.navigate(['/apps/business-hub'], {
    queryParams: { hubMode: 'CONSUMER_HUB', from: 'marketplace' },
    queryParamsHandling: 'merge',
  });
}

  onSearchInput(e: Event): void {
    this.searchQuery.set((e.target as HTMLInputElement).value);
  }

  onZipInput(e: Event): void {
    this.zip.set((e.target as HTMLInputElement).value);
  }

  onSearch(): void {
    // keep URL in sync, then fetch happens via queryParamMap subscription
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {
        q: this.searchQuery()?.trim() || null,
        zip: this.zip()?.trim() || null,
      },
      queryParamsHandling: 'merge',
    });
  }

  toggleRatingMenu(): void {
    this.ratingMenuOpen.set(!this.ratingMenuOpen());
  }

  setRatingFilter(v: RatingFilter): void {
    this.ratingFilter.set(v);
    this.ratingMenuOpen.set(false);
    this.fetchBusinesses();
  }

  onBusinessDetail(businessId: number): void {
    const business = this.businesses().find((b) => b.id === businessId);
    if (!business) return;

    // stable session id for audit
    let sessionId = sessionStorage.getItem('sessionId');
    if (!sessionId) {
      sessionId = `session-${Date.now()}`;
      sessionStorage.setItem('sessionId', sessionId);
    }

    const body: AuditClickRequestDto = {
      businessId,
      ownerId: business.ownerId ?? null,
      sessionId,
    };

    const hero = this.heroImageUrl(business);

    this.api
      .trackClick(body)
      .pipe(
        catchError(() => of(null as unknown as AuditClickResponseDto)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(() => {
        this.router.navigate(['/apps/business/id', businessId], {
          state: { heroImage: hero },
        });
      });
  }
  

  // ===================== UX: close rating dropdown =====================

  @HostListener('document:click', ['$event'])
  onDocClick(ev: MouseEvent): void {
    if (!this.ratingMenuOpen()) return;
    const el = ev.target as HTMLElement | null;
    if (!el) return;
    if (!el.closest('.rating-filter')) this.ratingMenuOpen.set(false);
  }

  @HostListener('document:keydown', ['$event'])
  onDocKeydown(ev: KeyboardEvent): void {
    if (ev.key === 'Escape' && this.ratingMenuOpen()) this.ratingMenuOpen.set(false);
  }
}
