import {
  Component,
  OnInit,
  OnDestroy,
  ViewChild,
  ElementRef,
  signal,
  computed,
  inject,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';

import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialogModule } from '@angular/material/dialog';

import { Subscription } from 'rxjs';
import { finalize } from 'rxjs/operators';

import { TokenService } from 'src/app/services/token/token.service';
import {
  BusinessHubService,
  Category,
} from 'src/app/services/business-hub.service';
import { BusinessRegistrationDto } from 'src/app/models/business-backend';
import { SecureStorageService } from 'src/app/services/storage/secure-storage.service';

/* ───────────────── TYPES ───────────────── */

type AdminRow = {
  businessId: number;
  businessName: string;
  email?: string | null;
  phone?: string | null;
  ownerName?: string | null;
  city?: string | null;
  state?: string | null;
  verified: boolean;

  serviceId?: number | null;
  serviceName?: string | null;

  createdBy: string | null;
  createdByAdmin: boolean;
};

type ConsumerBusiness = {
  companyId: number;
  companyName: string;
  state: string | null;

  businessId?: number | null;
  businessUpdatedBy?: string | null;

  verified?: boolean | null;
  statusName?: string | null;
};

type LaneKey = 'existing' | 'progress' | 'registered';

enum HubMode {
  ADMIN_HUB = 'ADMIN_HUB',
  CONSUMER_VIEW = 'CONSUMER_VIEW',
  CONSUMER_HUB = 'CONSUMER_HUB',
}

type PendingMove = {
  companyId: number;
  companyName: string;
  state: string | null;
  businessId: number;
  ts: number;
};

type BizCacheItem = {
  businessId: number;
  businessUpdatedBy: string | null; // null=pending, 'admin'=registered
};

interface AdminDetailView {
  businessId: number;
  verified: boolean;
  firstName: string;
  lastName: string;
  email: string;
  phoneNumber: string;
  address: string;
  city: string;
  state: string;
  zipCode: string;
  businessName: string;
  businessService: string;
  yearsInBusiness: string;
  website: string;
  serviceDescription: string;
  businessHours: string;
}

type UserIdentity = {
  loginUserId: number;
  role: string | null;
  isAdmin: boolean;
  firstName: string;
};

@Component({
  selector: 'app-business-hub',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,

    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatIconModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatTableModule,
    MatPaginatorModule,
    MatTooltipModule,
    MatDialogModule,
    MatSlideToggleModule,
  ],
  templateUrl: './business-hub.component.html',
  styleUrls: ['./business-hub.component.scss'],
})
export class BusinessHubComponent implements OnInit, OnDestroy {
  /* ───────────────── INJECTED ───────────────── */
  private hub = inject(BusinessHubService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private tokenService = inject(TokenService);

  constructor(private secureStorage: SecureStorageService) {}

  @ViewChild('uploader') uploader?: ElementRef<HTMLInputElement>;

  private subs: Subscription[] = [];
  private adminCreatedReq: Subscription | null = null;
  private adminListReq: Subscription | null = null;

  /* ✅ IMPORTANT: only available during in-app navigation (NOT refresh) */
  private readonly navState: any =
    this.router.getCurrentNavigation()?.extras?.state ?? null;

  /* ───────────────── USER IDENTIFICATION ───────────────── */
  private user = signal<UserIdentity>(this.resolveUserIdentity());

  private resolveUserIdentity(): UserIdentity {
    const role = (this.tokenService.getRole?.() ?? null) as string | null;

    let firstName = 'there';
    let loginUserId = 0;
    const raw = this.secureStorage.getLoggedInUserData();

    if (raw) {
      try {
        const u = JSON.parse(raw) as any;
        const f = String(u?.firstName ?? '').trim();
        if (f) firstName = f.charAt(0).toUpperCase() + f.slice(1);

        const id = Number(u?.loginUserId ?? u?.login_user_id ?? u?.id ?? 0);
        if (Number.isFinite(id) && id > 0) loginUserId = id;
      } catch {}
    }

    if (!loginUserId) {
      const ls = Number(this.secureStorage.getLoginUserId()) || 0;
      if (Number.isFinite(ls) && ls > 0) loginUserId = ls;
    }

    const isAdmin = String(role ?? '').toLowerCase() === 'admin';
    return { loginUserId, role, isAdmin, firstName };
  }

  private refreshUserIdentity(): void {
    this.user.set(this.resolveUserIdentity());
  }

  public isAdmin(): boolean {
    return this.user().isAdmin;
  }

  /* ───────────────── MODES ───────────────── */
  HubMode = HubMode;
  mode = signal<HubMode>(HubMode.CONSUMER_VIEW);

  marketplaceBackVisible = signal(false);
  private launchedFromLanding = false;

  private username = this.getUsername();

  /* ✅ Lane lock: prevents auto-switch if user chose lane */
  private laneLocked = false;
  private laneFromUrl = false;

  /* ───────────────── ADMIN STATE ───────────────── */

  rows = signal<AdminRow[]>([]);
  adminCreatedAll = signal<AdminRow[]>([]);

  total = signal(0);
  verifiedCount = signal(0);

  page = signal(0);
  size = signal(10);

  search = signal<string>('');
  zip = signal<string | null>(null);
  serviceId = signal<number | null>(null);

  isLoading = signal(false);

  createdByFilter = signal<'all' | 'admin' | 'users'>('all');
  private adminFilterTimer: any = null;

  displayedColumns: string[] = [
    'businessName',
    'email',
    'phone',
    'ownerName',
    'city',
    'state',
    'status',
    'actions',
  ];
  displayedColumnsLegacy: string[] = [
    'businessName',
    'email',
    'phone',
    'ownerName',
    'city',
    'state',
    'verified',
    'actions',
  ];

  totals = computed(() => {
    const t = this.total();
    const v = this.verifiedCount();
    return { total: t, verified: v, pending: Math.max(0, t - v) };
  });

  filteredRows = computed(() => {
    const createdFilter = this.createdByFilter();

    let list: AdminRow[] =
      createdFilter === 'admin' ? this.adminCreatedAll() : this.rows();

    if (createdFilter === 'admin') list = list.filter((r) => r.createdByAdmin);
    if (createdFilter === 'users') list = list.filter((r) => !r.createdByAdmin);

    const q = (this.search() || '').trim().toLowerCase();
    if (q) {
      list = list.filter(
        (r) =>
          (r.businessName || '').toLowerCase().includes(q) ||
          (r.email || '').toLowerCase().includes(q) ||
          (r.ownerName || '').toLowerCase().includes(q),
      );
    }

    if (createdFilter === 'admin') {
      const p = Math.max(0, this.page());
      const s = Math.max(1, this.size());
      const start = p * s;
      return list.slice(start, start + s);
    }

    return list;
  });

  private currentUploadBusinessId: number | null = null;
  serviceOptions = signal<{ id: number; name: string }[]>([]);

  /* ───────────────── ADMIN DETAIL VIEW ───────────────── */
  adminDetail: AdminDetailView | null = null;
  adminDetailLoading = false;

  uploadedImages: string[] = [];
  previewImageUrl: string | null = null;

  verifyBusy = false;
  rejectBusy = false;

  openImagePreview(url: string): void {
    this.previewImageUrl = url;
  }
  closeImagePreview(): void {
    this.previewImageUrl = null;
  }

  /* ───────────────── CONSUMER STATE ───────────────── */
  businesses: ConsumerBusiness[] = [];

  existingBusinesses: ConsumerBusiness[] = [];
  inProgressBusinesses: ConsumerBusiness[] = [];
  registeredBusinesses: ConsumerBusiness[] = [];

  ownerInProgressBusinesses: any[] = [];
  ownerRegisteredBusinesses: any[] = [];

  existingCount = 0;
  inProgressCount = 0;
  registeredCount = 0;
  totalBusinesses = 0;

  currentLane: LaneKey = 'existing';

  private userId = 0;

  trackByOwnerBusinessId = (_: number, b: any) => Number(b?.businessId ?? 0) || _;

  /* ───────────────── MARKETPLACE STATE ───────────────── */
  categories = signal<Category[]>([]);
  searchQuery = signal<string>('');
  selectedCategory = signal<string>('all');
  zipCode = signal<string>('');

  filteredCategories = computed(() => {
    const q = this.searchQuery().trim().toLowerCase();
    if (!q) return this.categories();
    return this.categories().filter(
      (c) => c.name.toLowerCase().includes(q) || c.value.toLowerCase().includes(q),
    );
  });

  /* ───────────────── STORAGE KEYS ───────────────── */
  private readonly CACHE_BASE = 'bh_company_biz_cache_v2';
  private readonly PENDING_BASE = 'bh_pending_moves_v2';

  private cacheKey(uid: number) {
    return `${this.CACHE_BASE}_${uid || 0}`;
  }
  private pendingKey(uid: number) {
    return `${this.PENDING_BASE}_${uid || 0}`;
  }

  private readonly PENDING_MOVE_KEY_LEGACY = 'bh_pending_move';

  /* ───────────────── NORMALIZERS ───────────────── */

  private normalizeCompanyId(c: any): number {
    const id =
      c?.companyId ??
      c?.company_id ??
      c?.companyID ??
      c?.companyIdFk ??
      c?.company_id_fk ??
      c?.id ??
      c?.company?.companyId ??
      c?.company?.company_id ??
      c?.company?.companyID ??
      c?.company?.id ??
      c?.companyLite?.companyId ??
      c?.companyLite?.id ??
      0;

    const n = Number(id);
    return Number.isFinite(n) && n > 0 ? n : 0;
  }

  private normalizeCompanyName(c: any): string {
    const name =
      c?.companyName ??
      c?.company_name ??
      c?.name ??
      c?.company?.companyName ??
      c?.company?.name ??
      '—';
    return String(name ?? '—');
  }

  private normalizeBusinessId(v: any): number | null {
    if (v === null || v === undefined || v === '') return null;
    const n = Number(v);
    return Number.isFinite(n) && n > 0 ? n : null;
  }

  private normalizeUpdatedBy(v: any): string | null {
    if (v === null || v === undefined) return null;
    const s = String(v).trim();
    if (!s) return null;
    if (s.toLowerCase() === 'null') return null;
    return s;
  }

  private getLoginUserIdSafe(): number {
    const raw = this.secureStorage.getLoggedInUserData();
    if (raw) {
      try {
        const u = JSON.parse(raw) as any;
        const id = Number(u?.loginUserId ?? u?.login_user_id ?? u?.id ?? 0);
        if (Number.isFinite(id) && id > 0) return id;
      } catch {}
    }
    const ls = Number(this.secureStorage.getLoginUserId()) || 0;
    if (Number.isFinite(ls) && ls > 0) return ls;
    return 0;
  }

  /* ───────────────── CACHE (LOCALSTORAGE) ───────────────── */
  private readCache(uid: number): Record<string, BizCacheItem> {
    try {
      const raw = localStorage.getItem(this.cacheKey(uid));
      if (!raw) return {};
      const parsed = JSON.parse(raw);
      return parsed && typeof parsed === 'object' ? parsed : {};
    } catch {
      return {};
    }
  }

  private writeCache(uid: number, cache: Record<string, BizCacheItem>): void {
    try {
      localStorage.setItem(this.cacheKey(uid), JSON.stringify(cache));
    } catch {}
  }

  private upsertCache(
    uid: number,
    companyId: number,
    businessId: number,
    businessUpdatedBy: string | null,
  ): void {
    if (!uid || !companyId || !businessId) return;
    const cache = this.readCache(uid);
    cache[String(companyId)] = {
      businessId,
      businessUpdatedBy: businessUpdatedBy ?? null,
    };
    this.writeCache(uid, cache);
  }

  private mergeWithCache(uid: number, list: ConsumerBusiness[]): ConsumerBusiness[] {
    const cache = this.readCache(uid);

    return (list ?? []).map((b) => {
      const cid = Number(b?.companyId ?? 0) || 0;
      if (!cid) return b;

      const apiBizId = this.normalizeBusinessId(b?.businessId);
      const apiUpd = this.normalizeUpdatedBy(b?.businessUpdatedBy);

      if (apiBizId) {
        this.upsertCache(uid, cid, apiBizId, apiUpd ?? null);
        return {
          ...b,
          companyId: cid,
          businessId: apiBizId,
          businessUpdatedBy: apiUpd ?? null,
        };
      }

      const cached = cache[String(cid)];
      if (cached?.businessId) {
        return {
          ...b,
          companyId: cid,
          businessId: cached.businessId,
          businessUpdatedBy: this.normalizeUpdatedBy(cached.businessUpdatedBy) ?? null,
        };
      }

      return { ...b, companyId: cid, businessId: null, businessUpdatedBy: null };
    });
  }

  /* ───────────────── PENDING MOVES (SESSIONSTORAGE) ───────────────── */
  private readPendingMoves(uid: number): PendingMove[] {
    try {
      const raw = sessionStorage.getItem(this.pendingKey(uid));
      if (!raw) return [];
      const arr = JSON.parse(raw);
      return Array.isArray(arr) ? (arr as PendingMove[]) : [];
    } catch {
      return [];
    }
  }

  private writePendingMoves(uid: number, moves: PendingMove[]): void {
    try {
      const now = Date.now();
      const keep = (moves || []).filter((m) => m?.ts && now - m.ts <= 15 * 60 * 1000);
      sessionStorage.setItem(this.pendingKey(uid), JSON.stringify(keep));
    } catch {}
  }

  private applyPendingMoves(
    uid: number,
    list: ConsumerBusiness[],
    apiBizIdPresentCompanyIds: Set<number>,
  ): ConsumerBusiness[] {
    const pending = this.readPendingMoves(uid);
    if (!pending.length) return list;

    const copy = [...(list || [])];
    const remaining: PendingMove[] = [];

    for (const pm of pending) {
      if (!pm?.companyId || !pm?.businessId) continue;
      if (!pm.ts || Date.now() - pm.ts > 15 * 60 * 1000) continue;

      const companyId = Number(pm.companyId) || 0;
      if (!companyId) continue;

      if (apiBizIdPresentCompanyIds.has(companyId)) continue;

      const idx = copy.findIndex((x) => Number(x.companyId) === companyId);
      const patched: ConsumerBusiness = {
        ...(idx >= 0 ? copy[idx] : {}),
        companyId,
        companyName:
          (idx >= 0 ? copy[idx]?.companyName : pm.companyName) || pm.companyName || '—',
        state: (idx >= 0 ? copy[idx]?.state : pm.state) ?? pm.state ?? null,
        businessId: pm.businessId,
        businessUpdatedBy: null,
      };

      if (idx >= 0) copy[idx] = patched;
      else copy.unshift(patched);

      this.upsertCache(uid, companyId, pm.businessId, null);
      remaining.push(pm);
    }

    this.writePendingMoves(uid, remaining);
    return copy;
  }

  /* ───────────────── LEGACY SINGLE PENDING MOVE ───────────────── */
  private readPendingMoveLegacy(): PendingMove | null {
    try {
      const raw = sessionStorage.getItem(this.PENDING_MOVE_KEY_LEGACY);
      if (!raw) return null;
      const obj = JSON.parse(raw) as PendingMove;

      if (!obj?.ts || Date.now() - obj.ts > 15 * 60 * 1000) {
        sessionStorage.removeItem(this.PENDING_MOVE_KEY_LEGACY);
        return null;
      }
      if (!obj.companyId || !obj.businessId) return null;
      return obj;
    } catch {
      return null;
    }
  }

  private clearPendingMoveLegacy(): void {
    try {
      sessionStorage.removeItem(this.PENDING_MOVE_KEY_LEGACY);
    } catch {}
  }

  private applyPendingMoveLegacy(list: ConsumerBusiness[]): ConsumerBusiness[] {
    const pm = this.readPendingMoveLegacy();
    if (!pm) return list;

    const idx = list.findIndex((x) => Number(x.companyId) === Number(pm.companyId));

    if (idx >= 0) {
      const fromApiBizId = this.normalizeBusinessId(list[idx].businessId);
      if (fromApiBizId && fromApiBizId > 0) {
        this.clearPendingMoveLegacy();
        return list;
      }

      const patched: ConsumerBusiness = {
        ...list[idx],
        businessId: pm.businessId,
        businessUpdatedBy: null,
      };

      const copy = [...list];
      copy[idx] = patched;
      return copy;
    }

    const injected: ConsumerBusiness = {
      companyId: pm.companyId,
      companyName: pm.companyName || '—',
      state: pm.state ?? null,
      businessId: pm.businessId,
      businessUpdatedBy: null,
    };

    return [injected, ...list];
  }

  /* ───────────────── ADMIN MAPPERS ───────────────── */

  private mapAdminRow(it: any): AdminRow {
    const b = it?.business ?? {};
    const o = it?.owner ?? {};

    const derivedVerified =
      b?.verified === true ||
      it?.verified === true ||
      String(it?.statusName ?? '').toLowerCase().includes('verified') ||
      String(b?.updatedBy ?? '').toLowerCase().includes('admin');

    const createdRaw = (b?.createdBy ?? it?.createdBy ?? '').toString().trim();
    const createdByAdmin = createdRaw.toLowerCase() === 'admin';

    const serviceIdRaw = b?.serviceId ?? it?.serviceId ?? it?.service_id ?? null;
    const sid =
      serviceIdRaw !== null && serviceIdRaw !== undefined
        ? Number(serviceIdRaw)
        : NaN;

    return {
      businessId: Number(b?.businessId ?? it?.businessId ?? 0),
      businessName: String(b?.businessName ?? it?.businessName ?? '—'),
      email: b?.businessEmail ?? it?.businessEmail ?? it?.email ?? null,
      phone: o?.contactNumber ?? b?.contactNumber ?? it?.contactNumber ?? null,
      ownerName:
        [o?.firstName ?? it?.ownerFirstName, o?.lastName ?? it?.ownerLastName]
          .filter(Boolean)
          .join(' ') || null,
      city: b?.city ?? it?.city ?? null,
      state: b?.state ?? it?.state ?? null,
      verified: !!derivedVerified,
      serviceId: Number.isFinite(sid) && sid > 0 ? sid : null,
      serviceName: b?.serviceName ?? it?.serviceName ?? it?.service_name ?? null,
      createdBy: createdRaw || null,
      createdByAdmin,
    };
  }

  private mapBusinessToDetail(dto: any): AdminDetailView {
    const anyReg: any = dto ?? {};
    const business: any = anyReg.business ?? anyReg ?? {};
    const owner: any = anyReg.owner ?? anyReg ?? {};

    const verified =
      business?.verified === true ||
      anyReg?.verified === true ||
      String(business?.statusName ?? '').toLowerCase().includes('verified') ||
      String(business?.updatedBy ?? '').toLowerCase().trim() === 'admin';

    const businessId = Number(business?.businessId ?? anyReg.businessId ?? 0) || 0;

    return {
      businessId,
      verified,
      firstName: owner.firstName ?? owner.ownerFirstName ?? owner.personalFirstName ?? '',
      lastName: owner.lastName ?? owner.ownerLastName ?? owner.personalLastName ?? '',
      email: owner.personalEmail ?? owner.email ?? business.businessEmail ?? '',
      phoneNumber:
        owner.contactNumber ?? business.contactPhone ?? business.businessPhone ?? '',
      address:
        business.businessAddress ?? anyReg.address ?? anyReg.streetAddress ?? '',
      city: business.city ?? anyReg.city ?? '',
      state: business.state ?? anyReg.state ?? '',
      zipCode: business.zipCode ?? anyReg.zipCode ?? anyReg.zip ?? '',
      businessName: business.businessName ?? '',
      businessService:
        business.serviceName ??
        business.businessServiceName ??
        (business.serviceId != null ? String(business.serviceId) : ''),
      yearsInBusiness: business.yearsInBusiness ?? '',
      website: business.websiteUrl ?? '',
      serviceDescription: business.serviceDescription ?? business.description ?? '',
      businessHours: business.businessHours ?? '',
    };
  }

  private updateRowVerified(businessId: number, verified: boolean): void {
    {
      const rows = this.rows();
      const idx = rows.findIndex((r) => r.businessId === businessId);
      if (idx >= 0) {
        const copy = [...rows];
        copy[idx] = { ...copy[idx], verified };
        this.rows.set(copy);
      }
    }
    {
      const all = this.adminCreatedAll();
      const idx = all.findIndex((r) => r.businessId === businessId);
      if (idx >= 0) {
        const copy = [...all];
        copy[idx] = { ...copy[idx], verified };
        this.adminCreatedAll.set(copy);
      }
    }
  }

  /* ───────────────────────────────
   * ✅ APP INIT
   * ─────────────────────────────── */
  private ignoreFirstQueryParamEvent = true;

  ngOnInit(): void {
    this.refreshUserIdentity();
    this.username = this.getUsername();

    const snap = this.route.snapshot.queryParamMap;

    // lane from URL (if present)
    const laneSnap = snap.get('lane') as LaneKey | null;
    if (laneSnap === 'existing' || laneSnap === 'progress' || laneSnap === 'registered') {
      this.currentLane = laneSnap;
      this.laneLocked = true;
      this.laneFromUrl = true;
    } else {
      this.laneLocked = false;
      this.laneFromUrl = false;
    }

    // ADMIN
    if (this.isAdmin()) {
      this.initAdminFlow();
      return;
    }

    // ✅ Always start consumer flow (loads data + queryParam watcher)
    this.mode.set(HubMode.CONSUMER_VIEW);
    this.initConsumerViewFlow();

    // ✅ Open marketplace only during in-app navigation OR if URL forces it
    const hubModeSnap = (snap.get('hubMode') || '').trim();
    const forceConsumerHub = hubModeSnap === HubMode.CONSUMER_HUB;

    const openMarketplaceViaNav = !!this.navState?.bhOpenMarketplace;
    const from = String(this.navState?.from ?? snap.get('from') ?? '').toLowerCase();

    if (openMarketplaceViaNav || forceConsumerHub) {
      this.mode.set(HubMode.CONSUMER_HUB);
      this.marketplaceBackVisible.set(from === 'marketplace' || openMarketplaceViaNav);
      this.marketplaceLoadCategories();

      // ✅ remove URL flags so refresh won't stick to marketplace
      if (forceConsumerHub) this.clearHubQueryParamsOnce();
    }
  }

  ngOnDestroy(): void {
    if (this.adminFilterTimer) clearTimeout(this.adminFilterTimer);
    this.subs.forEach((s) => s.unsubscribe());
    this.adminCreatedReq?.unsubscribe();
    this.adminListReq?.unsubscribe();
  }

  /* ───────────────────────────────
   * ✅ ADMIN FLOW
   * ─────────────────────────────── */

  private initAdminFlow(): void {
    this.mode.set(HubMode.ADMIN_HUB);
    this.adminFetchPagedList();
    this.marketplaceLoadCategories();
    this.adminLoadServiceOptions();
  }

  setCreatedByFilter(filter: 'all' | 'admin' | 'users'): void {
    this.createdByFilter.set(filter);
    this.page.set(0);

    if (!this.isAdmin()) return;

    if (filter !== 'admin') {
      this.adminCreatedAll.set([]);
      this.adminFetchPagedList();
      return;
    }

    this.adminCreatedReq?.unsubscribe();
    this.isLoading.set(true);

    this.adminCreatedReq = this.hub
      .listAdminCreatedBusinesses()
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe({
        next: (list: BusinessRegistrationDto[]) => {
          const mapped: AdminRow[] = (list ?? []).map((it) => this.mapAdminRow(it));
          this.adminCreatedAll.set(mapped);
          this.total.set(mapped.length);
          this.verifiedCount.set(mapped.filter((r) => r.verified).length);
        },
        error: (err) => {
          console.error('listAdminCreatedBusinesses failed', err);
          this.adminCreatedAll.set([]);
          this.total.set(0);
          this.verifiedCount.set(0);
        },
      });

    this.subs.push(this.adminCreatedReq);
  }

  private adminFetchPagedList(): void {
    if (!this.isAdmin()) return;
    if (this.createdByFilter() === 'admin') return;

    this.adminListReq?.unsubscribe();
    this.isLoading.set(true);

    this.adminListReq = this.hub
      .adminList({
        page: this.page(),
        size: this.size(),
        serviceId: this.serviceId() ?? undefined,
        zipCode: this.zip() ?? undefined,
      })
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe({
        next: (resp: any) => {
          const pageObj = resp?.page ?? {};
          const items: any[] = Array.isArray(resp?.content)
            ? resp.content
            : Array.isArray(pageObj?.content)
            ? pageObj.content
            : [];

          const mapped: AdminRow[] = items.map((it: any) => this.mapAdminRow(it));
          this.rows.set(mapped);

          const totalFromPage =
            typeof pageObj?.totalElements === 'number'
              ? pageObj.totalElements
              : mapped.length;

          this.total.set(Number(resp?.totalCount ?? totalFromPage ?? mapped.length) || 0);
          this.verifiedCount.set(
            Number(resp?.verifiedCount ?? mapped.filter((r) => r.verified).length) || 0,
          );
        },
        error: (err: any) => {
          console.error('adminList failed', err);
          this.rows.set([]);
          this.total.set(0);
          this.verifiedCount.set(0);
        },
      });

    this.subs.push(this.adminListReq);
  }

  fetchAdmin(): void {
    this.adminFetchPagedList();
  }

  onPage(e: PageEvent): void {
    this.page.set(e.pageIndex);
    this.size.set(e.pageSize);
    if (this.createdByFilter() === 'admin') return;
    this.adminFetchPagedList();
  }

  onAdminSearchInput(e: Event): void {
    const value = (e.target as HTMLInputElement | null)?.value ?? '';
    this.search.set(value);
    this.page.set(0);
  }

  onZipInput(e: Event): void {
    const raw = (e.target as HTMLInputElement | null)?.value ?? '';
    const v = raw.trim();
    this.zip.set(v.length ? v : null);
    this.adminScheduleFetch();
  }

  onServiceSelect(event: Event): void {
    const value = (event.target as HTMLSelectElement).value;
    const n = value ? Number(value) : NaN;

    this.serviceId.set(Number.isFinite(n) && n > 0 ? n : null);
    this.page.set(0);
    this.adminScheduleFetch();
  }

  private adminScheduleFetch(): void {
    if (!this.isAdmin()) return;

    if (this.adminFilterTimer) clearTimeout(this.adminFilterTimer);
    this.adminFilterTimer = setTimeout(() => {
      this.page.set(0);
      if (this.createdByFilter() === 'admin') this.setCreatedByFilter('admin');
      else this.adminFetchPagedList();
    }, 250);
  }

  clearFilters(): void {
    this.search.set('');
    this.zip.set(null);
    this.serviceId.set(null);
    this.createdByFilter.set('all');
    this.adminCreatedAll.set([]);
    this.page.set(0);
    this.adminFetchPagedList();
  }

  detailSource: 'admin' | 'consumer' = 'admin';

  private openBusinessDetail(businessId: number, source: 'admin' | 'consumer'): void {
    const id = Number(businessId) || 0;
    if (!id) return;

    this.detailSource = source;

    this.adminDetail = null;
    this.adminDetailLoading = true;
    this.uploadedImages = [];
    this.previewImageUrl = null;

    const sub = this.hub
      .getBusinessById(id)
      .pipe(finalize(() => (this.adminDetailLoading = false)))
      .subscribe({
        next: (dto: any) => {
          this.adminDetail = this.mapBusinessToDetail(dto);

          const imgSub = this.hub.listImages(id).subscribe({
            next: (imgs: string[]) => (this.uploadedImages = imgs ?? []),
            error: (err) => {
              console.error('listImages failed', err);
              this.uploadedImages = [];
            },
          });

          this.subs.push(imgSub);
        },
        error: (err) => {
          console.error('getBusiness failed', err);
          this.adminDetail = null;
        },
      });

    this.subs.push(sub);
  }

  onAdminView(row: AdminRow): void {
    if (!row?.businessId) return;
    this.openBusinessDetail(row.businessId, 'admin');
  }

  viewOwnerBusiness(businessId: number): void {
    this.openBusinessDetail(businessId, 'consumer');
  }

  backFromDetail(): void {
    this.adminDetail = null;
    this.adminDetailLoading = false;
    this.uploadedImages = [];
    this.previewImageUrl = null;
  }

  backToAdminList(): void {
    this.backFromDetail();
  }

  openUploaderFor(businessId: number): void {
    this.currentUploadBusinessId = businessId;
    this.uploader?.nativeElement.click();
  }

  handleImagesSelected(event: Event): void {
    if (!this.currentUploadBusinessId) return;

    const input = event.target as HTMLInputElement | null;
    const files = input?.files;
    if (!files || files.length === 0) return;

    const arr = Array.from(files);

    const sub = this.hub.uploadImages(this.currentUploadBusinessId, arr as File[]).subscribe({
      next: () => {
        if (input) input.value = '';
        if (this.adminDetail?.businessId) {
          const imgSub = this.hub.listImages(this.adminDetail.businessId).subscribe({
            next: (imgs) => (this.uploadedImages = imgs ?? []),
            error: () => (this.uploadedImages = []),
          });
          this.subs.push(imgSub);
        }
      },
      error: (err: any) => {
        console.error('uploadImages failed', err);
        if (input) input.value = '';
      },
    });

    this.subs.push(sub);
  }

  onVerifyToggle(checked: boolean): void {
    if (!this.isAdmin()) return;
    if (!this.adminDetail) return;

    const businessId = this.adminDetail.businessId;
    const loginUserId = this.user().loginUserId;

    if (!businessId || !loginUserId) {
      console.warn('Missing businessId or loginUserId for verify');
      return;
    }

    const previous = this.adminDetail.verified;
    this.adminDetail.verified = checked;
    this.verifyBusy = true;

    const sub = this.hub
      .toggleVerify(businessId, loginUserId)
      .pipe(finalize(() => (this.verifyBusy = false)))
      .subscribe({
        next: () => {
          this.updateRowVerified(businessId, checked);

          const before = this.verifiedCount();
          if (!previous && checked) this.verifiedCount.set(before + 1);
          else if (previous && !checked)
            this.verifiedCount.set(Math.max(0, before - 1));
        },
        error: (err) => {
          console.error('toggleVerify failed', err);
          this.adminDetail!.verified = previous;
        },
      });

    this.subs.push(sub);
  }

  onRejectBusiness(): void {
    if (!this.adminDetail) return;

    const businessId = Number(this.adminDetail.businessId || 0);
    if (!businessId) return;

    const ok = confirm('Are you sure you want to reject this business submission?');
    if (!ok) return;

    this.rejectBusy = true;

    const sub = this.hub
      .rejectBusiness(businessId)
      .pipe(finalize(() => (this.rejectBusy = false)))
      .subscribe({
        next: (res: any) => {
          alert(res?.message ?? 'Rejected successfully');
          this.backToAdminList();
          if (this.createdByFilter() === 'admin') this.setCreatedByFilter('admin');
          else this.adminFetchPagedList();
        },
        error: (err: any) => {
          console.error('rejectBusiness failed', err);
          alert('Reject failed. Please try again.');
        },
      });

    this.subs.push(sub);
  }

  onAdminAddBusiness(): void {
    this.router.navigate(['/apps/register'], { queryParams: { source: 'admin' } });
  }

  private adminLoadServiceOptions(): void {
    const sub = this.hub.listServices().subscribe({
      next: (list) => {
        const options = (list ?? [])
          .map((s: any) => ({
            id: Number(
              s.id ??
                s.serviceId ??
                s.service_id ??
                s.businessServiceId ??
                s.business_service_id ??
                0,
            ),
            name: s.serviceName ?? s.name ?? s.title ?? 'Unnamed service',
          }))
          .filter((o) => Number.isFinite(o.id) && o.id > 0);

        this.serviceOptions.set(options);
      },
      error: (err) => {
        console.error('Failed to load services', err);
        this.serviceOptions.set([]);
      },
    });

    this.subs.push(sub);
  }

  /* ───────────────────────────────
   * ✅ CONSUMER FLOW
   * ─────────────────────────────── */

  private refreshConsumerViewData(): void {
    this.consumerFetchBusinesses();
    this.loadExistingCompanies();
    this.preloadOwnerBusinessCounts();
  }

  private initConsumerViewFlow(): void {
    this.marketplaceBackVisible.set(false);

    // ✅ Always load everything so counts + lists come immediately
    this.refreshConsumerViewData();
    this.marketplaceLoadCategories();

    const qpSub = this.route.queryParamMap.subscribe((qp) => {
      if (this.ignoreFirstQueryParamEvent) {
        this.ignoreFirstQueryParamEvent = false;
        return;
      }

      const lane = qp.get('lane') as LaneKey | null;
      const refresh = qp.get('refresh') === '1';

      const hubMode = (qp.get('hubMode') || '').trim();
      const goHub = hubMode === HubMode.CONSUMER_HUB;

      if (goHub) {
        this.mode.set(HubMode.CONSUMER_HUB);
        const from = (qp.get('from') || '').toLowerCase();
        this.marketplaceBackVisible.set(from === 'marketplace');
        this.marketplaceLoadCategories();
        this.clearHubQueryParamsOnce();
        return;
      }

      if (lane === 'existing' || lane === 'progress' || lane === 'registered') {
        this.currentLane = lane;
        this.laneLocked = true;
        this.laneFromUrl = true;
      }

      if (refresh) {
        this.mode.set(HubMode.CONSUMER_VIEW);
        this.refreshConsumerViewData();
      }
    });

    this.subs.push(qpSub);
  }

  private consumerFetchBusinesses(): void {
    const loginUserId = this.user().loginUserId;

    if (!loginUserId) {
      this.businesses = [];
      this.totalBusinesses = 0;
      this.consumerSplitByStatus([]);
      return;
    }

    const sub = this.hub.getUserCompanies(loginUserId).subscribe({
      next: (resp: any) => {
        // ✅ supports array response OR page response
        const list: any[] = Array.isArray(resp)
          ? resp
          : Array.isArray(resp?.content)
          ? resp.content
          : Array.isArray(resp?.page?.content)
          ? resp.page.content
          : [];

        this.totalBusinesses =
          Number(resp?.totalElements ?? resp?.page?.totalElements ?? list.length) ||
          list.length;

        const mapped: ConsumerBusiness[] = list.map((c: any) => {
          const companyId = this.normalizeCompanyId(c);
          const companyName = this.normalizeCompanyName(c);
          const state = c?.state ?? c?.companyState ?? c?.company_state ?? null;

          const businessId = this.normalizeBusinessId(
            c?.businessId ??
              c?.business_id ??
              c?.businessID ??
              c?.businessIdFk ??
              c?.business?.businessId ??
              c?.business?.id ??
              null,
          );

          const businessUpdatedBy = this.normalizeUpdatedBy(
            c?.businessUpdatedBy ??
              c?.business_updated_by ??
              c?.updatedBy ??
              c?.updated_by ??
              c?.business?.updatedBy ??
              c?.business?.updated_by ??
              null,
          );

          return {
            companyId,
            companyName,
            state,
            businessId,
            businessUpdatedBy,
          };
        });

        const mergedCache = this.mergeWithCache(loginUserId, mapped);

        const apiBizPresent = new Set<number>();
        mergedCache.forEach((x) => {
          const cid = Number(x.companyId) || 0;
          const bid = this.normalizeBusinessId(x.businessId);
          if (cid && bid) apiBizPresent.add(cid);
        });

        const mergedPending = this.applyPendingMoves(loginUserId, mergedCache, apiBizPresent);
        const mergedLegacy = this.applyPendingMoveLegacy(mergedPending);

        this.businesses = mergedLegacy;
        this.consumerSplitByStatus(mergedLegacy);
      },
      error: (err: any) => {
        console.error('getUserCompanies failed', err);
        this.businesses = [];
        this.totalBusinesses = 0;
        this.consumerSplitByStatus([]);
      },
    });

    this.subs.push(sub);
  }

  private consumerSplitByStatus(all: ConsumerBusiness[]): void {
    const existing: ConsumerBusiness[] = [];
    const inProgress: ConsumerBusiness[] = [];
    const registered: ConsumerBusiness[] = [];

    all.forEach((b) => {
      const bizId = b.businessId ?? null;
      const updatedBy = (b.businessUpdatedBy ?? '').toString().trim().toLowerCase();

      if (!bizId) {
        existing.push(b);
        return;
      }
      if (updatedBy === 'admin') {
        registered.push(b);
        return;
      }
      inProgress.push(b);
    });

    this.inProgressBusinesses = inProgress;
    this.registeredBusinesses = registered;

    this.inProgressCount = inProgress.length;
    this.registeredCount = registered.length;

    // existingCount comes from loadExistingCompanies()
    this.autoSelectLaneIfEmpty();
  }

  setLane(lane: LaneKey): void {
    this.currentLane = lane;

    // ✅ user clicked, so lock auto switching
    this.laneLocked = true;

    if (lane === 'existing') this.loadExistingCompanies();
    if (lane === 'progress') this.loadOwnerInProgressBusinesses();
    if (lane === 'registered') this.loadOwnerRegisteredBusinesses();
  }

  isLaneActive(lane: LaneKey): boolean {
    return this.currentLane === lane;
  }

  /* ✅ FIX: if existing=0 but progress/registered exists, show that lane by default */
  private autoSelectLaneIfEmpty(): void {
    if (this.laneLocked) return;

    const e = Number(this.existingCount || 0);
    const p = Number(this.inProgressCount || 0);
    const r = Number(this.registeredCount || 0);

    if (this.currentLane === 'existing' && e > 0) return;
    if (this.currentLane === 'progress' && p > 0) return;
    if (this.currentLane === 'registered' && r > 0) return;

    if (p > 0) {
      this.currentLane = 'progress';
      this.loadOwnerInProgressBusinesses();
      return;
    }

    if (r > 0) {
      this.currentLane = 'registered';
      this.loadOwnerRegisteredBusinesses();
      return;
    }

    this.currentLane = 'existing';
  }

  private loadOwnerInProgressBusinesses(): void {
    const uid = this.getLoginUserIdSafe();
    if (!uid) {
      this.ownerInProgressBusinesses = [];
      this.inProgressCount = 0;
      return;
    }

    const sub = this.hub.getInProgressBusinesses(uid).subscribe({
      next: (list) => {
        this.ownerInProgressBusinesses = list ?? [];
        this.inProgressCount = Array.isArray(this.ownerInProgressBusinesses)
          ? this.ownerInProgressBusinesses.length
          : 0;
        this.autoSelectLaneIfEmpty();
      },
      error: (err) => {
        console.error('in-progress api failed', err);
        this.ownerInProgressBusinesses = [];
        this.inProgressCount = 0;
        this.autoSelectLaneIfEmpty();
      },
    });

    this.subs.push(sub);
  }

  private loadOwnerRegisteredBusinesses(): void {
    const uid = this.getLoginUserIdSafe();
    if (!uid) {
      this.ownerRegisteredBusinesses = [];
      this.registeredCount = 0;
      return;
    }

    const sub = this.hub.getRegisteredBusinesses(uid).subscribe({
      next: (list) => {
        this.ownerRegisteredBusinesses = list ?? [];
        this.registeredCount = Array.isArray(this.ownerRegisteredBusinesses)
          ? this.ownerRegisteredBusinesses.length
          : 0;
        this.autoSelectLaneIfEmpty();
      },
      error: (err) => {
        console.error('registered api failed', err);
        this.ownerRegisteredBusinesses = [];
        this.registeredCount = 0;
        this.autoSelectLaneIfEmpty();
      },
    });

    this.subs.push(sub);
  }

  private preloadOwnerBusinessCounts(): void {
    this.loadOwnerInProgressBusinesses();
    this.loadOwnerRegisteredBusinesses();
  }

  private navigateToRegister(extraState?: any): void {
    this.router.navigate(['/apps/register'], {
      state: { step: 0, ...(extraState ?? {}) },
    });
  }

  startRegistration(): void {
    this.navigateToRegister({ step: 0 });
  }

  openCompanyFromCard(companyId: number): void {
    if (!companyId) {
      this.navigateToRegister({ step: 0 });
      return;
    }
    this.navigateToRegister({ companyId });
  }

  viewSubmission(b: ConsumerBusiness): void {
    if (!b.businessId) return;

    this.router.navigate(['/apps/marketplace/business', b.businessId], {
      queryParams: { readonly: '1' },
    });
  }

  trackByBusinessId(index: number, b: ConsumerBusiness) {
    return b.companyId ?? b.companyName ?? index;
  }

  /* ───────────────── MARKETPLACE FLOW ───────────────── */

  accessMarketplace(): void {
    this.goToMarketplace();
  }

  private goToMarketplace(): void {
    this.mode.set(HubMode.CONSUMER_HUB);
    this.marketplaceLoadCategories();

    if (!this.launchedFromLanding) this.marketplaceBackVisible.set(true);
    else this.marketplaceBackVisible.set(false);
  }

  backFromMarketplace(): void {
    this.mode.set(HubMode.CONSUMER_VIEW);
    this.marketplaceBackVisible.set(false);

    if (!this.laneFromUrl) this.laneLocked = false;
    this.refreshConsumerViewData();
  }

  private marketplaceLoadCategories(): void {
    if (this.categories().length) return;

    const sub = this.hub.getCategories().subscribe({
      next: (cats) => this.categories.set(cats ?? []),
      error: (err) => console.error('getCategories failed', err),
    });

    this.subs.push(sub);
  }

  onJoinNow(): void {
    this.navigateToRegister({ step: 0 });
  }

  onCategoryClick(value: string): void {
    this.router.navigate(['/apps/category', value]);
  }

  onSearchInput(e: Event): void {
    this.searchQuery.set((e.target as HTMLInputElement).value);
  }

  onCategorySelect(e: Event): void {
    this.selectedCategory.set((e.target as HTMLSelectElement).value || 'all');
  }

  onZipCodeInput(e: Event): void {
    const v = (e.target as HTMLInputElement).value || '';
    this.zipCode.set(v.replace(/\D+/g, '').slice(0, 5));
  }

  onSearch(): void {
    const cat = this.selectedCategory();

    if (cat && cat !== 'all') {
      this.router.navigate(['/apps/category', cat], {
        queryParams: {
          q: this.searchQuery().trim() || null,
          zip: this.zipCode() || null,
        },
        queryParamsHandling: 'merge',
      });
      return;
    }

    this.router.navigate(['/apps/business-hub'], {
      queryParams: {
        q: this.searchQuery().trim() || null,
        zip: this.zipCode() || null,
      },
      queryParamsHandling: 'merge',
    });
  }

  /* ───────────────── HEADER TEXT ───────────────── */
  getWelcomeMessage(): string {
    const total =
      (this.existingBusinesses?.length ?? 0) +
      (this.inProgressCount ?? 0) +
      (this.registeredCount ?? 0);

    return total === 0 ? `Welcome, ${this.username}!` : `Welcome back, ${this.username}!`;
  }

  getSubtitleMessage(): string {
    const total =
      (this.existingBusinesses?.length ?? 0) +
      (this.inProgressCount ?? 0) +
      (this.registeredCount ?? 0);

    return total === 0
      ? "Let's get your business registered and listed in our BusinessHub."
      : 'Continue your registration or manage your existing BusinessHub listings.';
  }

  private getUsername(): string {
    const raw = this.secureStorage.getLoggedInUserData();
    if (!raw) return 'there';
    try {
      const u = JSON.parse(raw) as { firstName?: string };
      if (u.firstName) {
        const f = u.firstName.trim();
        return f.charAt(0).toUpperCase() + f.slice(1);
      }
      return 'there';
    } catch {
      return 'there';
    }
  }

  /* ───────────────── EXISTING (DRAFT) LIST ───────────────── */

  private loadExistingCompanies(): void {
    const uid = this.getLoginUserIdSafe();
    this.userId = uid;

    if (!uid) {
      this.existingBusinesses = [];
      this.existingCount = 0;
      this.autoSelectLaneIfEmpty();
      return;
    }

    this.isLoading.set(true);

    const sub = this.hub
      .getExistingCompanies(uid)
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe({
        next: (list: any[]) => {
          const completedIds = this.getCompletedProgressCompanyIds();
          const completedMap = this.buildCompletedProgressNameMap();

          let mapped: ConsumerBusiness[] = (list ?? []).map((c: any) => {
            const companyId = this.normalizeCompanyId(c);
            const apiName = this.normalizeCompanyName(c);

            return {
              companyId,
              companyName: this.getDisplayCompanyNameFromCompletedMap(
                companyId,
                apiName,
                completedMap,
              ),
              state: c?.state ?? c?.companyState ?? c?.company_state ?? null,
              businessId: null,
              businessUpdatedBy: null,
            };
          });

          if (completedIds.size > 0) {
            mapped = mapped.filter((x) => completedIds.has(Number(x.companyId)));
          }

          if ((!mapped || mapped.length === 0) && completedIds.size > 0) {
            mapped = this.buildExistingFallbackFromCompletedProgress(completedMap);
          }

          this.existingBusinesses = mapped;
          this.existingCount = mapped.length;

          this.autoSelectLaneIfEmpty();
        },
        error: (err) => {
          console.error('company/existing api failed', err);

          const completedIds = this.getCompletedProgressCompanyIds();
          const completedMap = this.buildCompletedProgressNameMap();
          if (completedIds.size > 0) {
            const mapped = this.buildExistingFallbackFromCompletedProgress(completedMap);
            this.existingBusinesses = mapped;
            this.existingCount = mapped.length;
          } else {
            this.existingBusinesses = [];
            this.existingCount = 0;
          }

          this.autoSelectLaneIfEmpty();
        },
      });

    this.subs.push(sub);
  }

  /* ───────────────── Local completedProgress helpers ───────────────── */

  private safeParse<T>(str: string | null): T | null {
    if (!str) return null;
    try {
      return JSON.parse(str) as T;
    } catch {
      return null;
    }
  }

  private getCompletedProgressCompanyIds(): Set<number> {
    const ids = new Set<number>();

    const raw = this.secureStorage.getItem<string>('completedProgressList', 'session');
    const items = this.safeParse<any[]>(raw);

    if (Array.isArray(items)) {
      for (const it of items) {
        const cid = Number(it?.companyId ?? it?.company_id ?? it?.id ?? 0);
        if (Number.isFinite(cid) && cid > 0) ids.add(cid);
      }
    }

    return ids;
  }

  private clearHubQueryParamsOnce(): void {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {
        hubMode: null,
        hub: null,
        mode: null,
        from: null,
      },
      queryParamsHandling: 'merge',
      replaceUrl: true,
    });
  }

  private buildCompletedProgressNameMap(): Record<number, string> {
    const map: Record<number, string> = {};

    const raw = this.secureStorage.getItem<string>('completedProgressList', 'session');
    const items = this.safeParse<any[]>(raw);

    if (Array.isArray(items)) {
      for (const i of items) {
        const cid = Number(i?.companyId ?? 0);
        if (!Number.isFinite(cid) || cid <= 0) continue;

        const name = String(i?.companyName ?? '').trim();
        const llc = String(i?.llcName ?? '').trim();
        const finalName = (name + (llc ? ' ' + llc : '')).trim();

        if (finalName) map[cid] = finalName;
      }
    }

    return map;
  }

  private getDisplayCompanyNameFromCompletedMap(
    companyId: number,
    apiName: any,
    completedMap: Record<number, string>,
  ): string {
    const cleanApi = String(apiName ?? '').trim();
    if (cleanApi && cleanApi !== '—' && cleanApi.toLowerCase() !== 'null') return cleanApi;

    const local = completedMap[companyId];
    return local ? local : '—';
  }

  private buildExistingFallbackFromCompletedProgress(
    completedMap: Record<number, string>,
  ): ConsumerBusiness[] {
    const raw = this.secureStorage.getItem<string>('completedProgressList', 'session');
    const items = this.safeParse<any[]>(raw);
    if (!Array.isArray(items)) return [];

    return items
      .map((i) => {
        const companyId = Number(i?.companyId ?? i?.company_id ?? 0);
        if (!Number.isFinite(companyId) || companyId <= 0) return null;

        const apiName = String(i?.companyName ?? '').trim();
        const companyName = this.getDisplayCompanyNameFromCompletedMap(
          companyId,
          apiName,
          completedMap,
        );

        return {
          companyId,
          companyName,
          state: i?.state ?? i?.companyState ?? i?.company_state ?? null,
          businessId: null,
          businessUpdatedBy: null,
        } as ConsumerBusiness;
      })
      .filter(Boolean) as ConsumerBusiness[];
  }
}
