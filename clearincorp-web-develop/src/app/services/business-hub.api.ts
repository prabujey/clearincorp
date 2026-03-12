// src/app/services/business-hub.api.ts
import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../environments/environment';
import { Observable } from 'rxjs';
import { map, catchError } from 'rxjs/operators';

import {
  AuditClickRequestDto,
  AuditClickResponseDto,
  AuditClickStatsDto,
  BusinessDto,
  BusinessImageDto,
  BusinessOwnerDto,
  BusinessRegistrationDto,
  BusinessServiceDto,
  CompanyLiteDto,
  CompanyFilterResponse,
  Page,
  ServiceIdName,
  WeightedBusinessHitDto,
} from '../models/business-backend';

export interface BusinessAdminItemDto {
  owner: {
    ownerId: number;
    firstName: string;
    lastName: string;
    personalEmail: string | null;
    contactNumber: string | null;
    createdOn: string;
  };
  business: {
    businessId: number;
    ownerId: number;
    serviceId: number;
    businessName: string;
    serviceDescription: string | null;
    yearsInBusiness: number | null;
    zipCode: string | null;
    businessAddress: string | null;
    websiteUrl: string | null;
    businessLicense: string | null;
    businessEmail: string | null;
    businessHours: string | null;
    city: string | null;
    state: string | null;
    createdBy: string | null;
    updatedBy: string | null;
    createdOn: string;
    updatedOn: string | null;
    verified?: boolean;
    statusName?: string;
  };
  verified?: boolean;
  statusName?: string;
}

export interface BusinessAdminPageDto {
  content: BusinessAdminItemDto[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
  verifiedCount?: number;
  unverifiedCount?: number;
  totalCount?: number;
}

export interface BusinessByAdminResponseDto {
  content: BusinessRegistrationDto[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface RejectBusinessResponse {
  businessId: number;
  message: string;
}

@Injectable({ providedIn: 'root' })
export class BusinessHubApi {
  private http = inject(HttpClient);

  private base = (environment.apiBaseUrl ?? '').replace(/\/+$/, '');
  private businessBase = `${this.base}/business`;
  private ownerBase = `${this.base}/business-owners`;
  private companyBase = `${this.base}/company`;

  // ✅ Default public host for local dev
  private readonly defaultPublicHost = 'https://dev.clearincorp.com';

  /**
   * ✅ Image public host resolver (NO environment.ts change)
   * - Local dev (localhost) -> uses https://dev.clearincorp.com
   * - Deployed -> uses current window.origin (ex: https://dev.clearincorp.com)
   */
  private resolvePublicHost(): string {
    if (typeof window === 'undefined') return this.defaultPublicHost;
    const h = window.location.hostname;
    if (h === 'localhost' || h === '127.0.0.1') return this.defaultPublicHost;
    return window.location.origin;
  }

  /** ✅ key: "business/2/images/image_1.jpg" -> "https://dev.clearincorp.com/business/2/images/image_1.jpg" */
  public publicImageUrlFromKey(key: string): string {
    const host = this.resolvePublicHost().replace(/\/+$/, '');
    const cleanKey = String(key || '').replace(/^\/+/, '');
    return `${host}/${cleanKey}`;
  }

  // ===================== OWNER BUSINESS LISTS =====================

  // ✅ In Progress (example backend rule: updated_by is NULL)
  getOwnerBusinessesInProgress(loginUserId: number): Observable<BusinessDto[]> {
    return this.http.get<BusinessDto[]>(`${this.ownerBase}/businesses/in-progress/${loginUserId}`);
  }

  // ✅ Registered (example backend rule: updated_by = 'admin')
  getOwnerBusinessesRegistered(loginUserId: number): Observable<BusinessDto[]> {
    return this.http.get<BusinessDto[]>(`${this.ownerBase}/businesses/registered/${loginUserId}`);
  }

  // ✅ Created by Admin list (no params)
  listAdminCreatedBusinesses(): Observable<BusinessRegistrationDto[]> {
    return this.http.get<BusinessRegistrationDto[]>(
      `${this.ownerBase}/businesses/admin-created-lists`
    );
  }

  // ✅ Reject API
  rejectBusiness(businessId: number): Observable<RejectBusinessResponse> {
    const params = new HttpParams().set('businessId', String(businessId));
    return this.http.patch<RejectBusinessResponse>(`${this.ownerBase}/reject`, {}, { params });
  }

  // ===================== DIRECTORY / SEARCH =====================

  searchPublicDirectory(opts: {
    q?: string;
    serviceId?: number | 'all';
    zip?: string;
    minRating?: number;
    page?: number;
    size?: number;
  }): Observable<{ content: BusinessDto[]; total?: number }> {
    let params = new HttpParams();
    if (opts.q) params = params.set('q', opts.q);
    if (opts.serviceId !== undefined && opts.serviceId !== 'all') {
      params = params.set('serviceId', String(opts.serviceId));
    }
    if (opts.zip) params = params.set('zip', opts.zip);
    if (opts.minRating !== undefined) params = params.set('minRating', String(opts.minRating));
    if (opts.page !== undefined) params = params.set('page', String(opts.page));
    if (opts.size !== undefined) params = params.set('size', String(opts.size));

    return this.http.get<{ content: BusinessDto[]; total?: number }>(
      `${this.base}/business-directory/search`,
      { params }
    );
  }

  // ===================== ADMIN LISTS =====================

  adminList(args: {
    page: number;
    size: number;
    serviceId?: number;
    zipCode?: string;
  }): Observable<BusinessAdminPageDto> {
    let params = new HttpParams()
      .set('page', String(Math.max(0, args.page)))
      .set('size', String(Math.min(Math.max(args.size, 1), 100)));

    if (args.serviceId != null) params = params.set('serviceId', String(args.serviceId));
    if (args.zipCode) params = params.set('zipCode', args.zipCode);

    return this.http.get<BusinessAdminPageDto>(`${this.businessBase}/admin`, { params });
  }

  getBusinessesByAdmin(opts: {
    serviceId?: number;
    zipCode?: string;
    page?: number;
    size?: number;
  }): Observable<BusinessByAdminResponseDto> {
    let params = new HttpParams()
      .set('page', String(opts.page ?? 0))
      .set('size', String(Math.min(Math.max(opts.size ?? 50, 1), 100)));

    if (opts.serviceId != null) params = params.set('serviceId', String(opts.serviceId));
    if (opts.zipCode) params = params.set('zipCode', opts.zipCode);

    return this.http.get<BusinessByAdminResponseDto>(`${this.businessBase}/by-admin`, { params });
  }

  // ===================== NLP SEARCH =====================

  searchWeighted(q: string, page = 0, size = 10): Observable<Page<WeightedBusinessHitDto>> {
    const params = new HttpParams().set('q', q).set('page', String(page)).set('size', String(size));
    return this.http.get<Page<WeightedBusinessHitDto>>(`${this.businessBase}/nlp`, { params });
  }

  // ===================== COMPANY FILTER =====================

  searchCompanyFilter(opts: {
    principalActivity?: string[];
    states?: string[];
    effectiveFrom?: string;
    effectiveTo?: string;
    search?: string;
    loginUserId?: number;
    page?: number;
    size?: number;
    sort?: string[];
  }): Observable<CompanyFilterResponse> {
    let params = new HttpParams()
      .set('page', String(opts.page ?? 0))
      .set('size', String(Math.min(Math.max(opts.size ?? 50, 1), 100)));

    if (opts.principalActivity?.length) {
      opts.principalActivity.forEach((a) => a && (params = params.append('principalActivity', a)));
    }

    if (opts.states?.length) {
      opts.states.forEach((s) => s && (params = params.append('states', s)));
    }

    if (opts.effectiveFrom) params = params.set('effectiveFrom', opts.effectiveFrom);
    if (opts.effectiveTo) params = params.set('effectiveTo', opts.effectiveTo);
    if (opts.search) params = params.set('search', opts.search);
    if (opts.loginUserId != null) params = params.set('loginUserId', String(opts.loginUserId));

    if (opts.sort?.length) {
      opts.sort.forEach((s) => s && (params = params.append('sort', s)));
    }

    return this.http.get<CompanyFilterResponse>(`${this.companyBase}/filter`, { params });
  }

  // ===================== CRUD =====================

  createBusiness(dto: BusinessDto): Observable<BusinessDto> {
    return this.http.post<BusinessDto>(this.businessBase, dto);
  }

  getCompany(companyId: number): Observable<CompanyLiteDto> {
    const params = new HttpParams().set('companyId', String(companyId));
    return this.http.get<CompanyLiteDto>(`${this.companyBase}/CompanyById`, { params });
  }

  getBusiness(businessId: number): Observable<BusinessRegistrationDto> {
    const params = new HttpParams().set('id', String(businessId));
    return this.http.get<BusinessRegistrationDto>(`${this.businessBase}/id`, { params });
  }

  updateBusiness(id: number, dto: BusinessDto): Observable<BusinessDto> {
    const params = new HttpParams().set('id', String(id));
    return this.http.put<BusinessDto>(`${this.businessBase}/${id}`, dto, { params });
  }

  deleteBusiness(id: number): Observable<void> {
    const params = new HttpParams().set('id', String(id));
    return this.http.delete<void>(`${this.businessBase}/${id}`, { params });
  }

  // ===================== CLICKS =====================

  trackClick(body: AuditClickRequestDto): Observable<AuditClickResponseDto> {
    return this.http.post<AuditClickResponseDto>(`${this.businessBase}/audit-clicks`, body);
  }

  clickStats(businessId: number): Observable<AuditClickStatsDto> {
    const params = new HttpParams().set('businessId', String(businessId));
    return this.http.get<AuditClickStatsDto>(`${this.businessBase}/status`, { params });
  }

  verifyIfPending(businessId: number, loginUserId: number): Observable<void> {
    const params = new HttpParams()
      .set('businessId', String(businessId))
      .set('loginUserId', String(loginUserId));
    return this.http.patch<void>(`${this.businessBase}/verify`, {}, { params });
  }

  // ===================== IMAGES =====================

  uploadImages(businessId: number, files: File[] | FileList): Observable<string[]> {
    const form = new FormData();
    form.append('businessId', String(businessId));

    if (files instanceof FileList) {
      for (let i = 0; i < files.length; i++) {
        const f = files.item(i);
        if (f) form.append('images', f, f.name);
      }
    } else {
      files.forEach((f) => form.append('images', f, f.name));
    }

    return this.http.post<string[]>(`${this.businessBase}/images`, form);
  }

  /**
   * ✅ Raw DTO list from backend:
   * GET /business/images?businessId=#
   */
  listImageDtos(businessId: number): Observable<BusinessImageDto[]> {
    const params = new HttpParams().set('businessId', String(businessId));
    return this.http.get<BusinessImageDto[]>(`${this.businessBase}/images`, { params });
  }

  /**
   * ✅ Legacy bytes endpoint (kept for compatibility)
   */
  getImageUrl(businessId: number, imageId: number): string {
    const b = this.businessBase.replace(/\/+$/, '');
    return `${b}/image?businessId=${encodeURIComponent(String(businessId))}&imageId=${encodeURIComponent(
      String(imageId)
    )}`;
  }

  /**
   * ✅ MAIN: returns usable <img src="..."> URLs.
   * Uses `key` when present, otherwise falls back to bytes endpoint by imageId.
   *
   * NOTE: This is what your BusinessHubService expects when it calls `api.listImages(...)`.
   */
  listImages(businessId: number): Observable<string[]> {
    return this.listImageDtos(businessId).pipe(
      map((arr: BusinessImageDto[]) => {
        const out: string[] = [];
        (arr ?? []).forEach((img: any) => {
          const key = String(img?.key ?? '').trim();
          if (key) {
            out.push(this.publicImageUrlFromKey(key));
            return;
          }

          const rawId: any = img?.imageId ?? img?.id ?? img?.image_id;
          const id = Number(rawId);
          if (Number.isFinite(id) && id > 0) out.push(this.getImageUrl(businessId, id));
        });
        return out;
      })
    );
  }

  /** alias (some components may call this name) */
  listImageUrls(businessId: number): Observable<string[]> {
    return this.listImages(businessId);
  }

  getFirstImage(businessId: number): Observable<string | null> {
    return this.listImages(businessId).pipe(map((arr) => (arr?.length ? arr[0] : null)));
  }

  downloadImage(businessId: number, imageId: number): Observable<Blob> {
    const params = new HttpParams()
      .set('businessId', String(businessId))
      .set('imageId', String(imageId));
    return this.http.get(`${this.businessBase}/image`, { params, responseType: 'blob' });
  }

  deleteImage(businessId: number, imageId: number): Observable<void> {
    const params = new HttpParams()
      .set('businessId', String(businessId))
      .set('imageId', String(imageId));
    return this.http.delete<void>(`${this.businessBase}/image`, { params });
  }

  deleteAllImages(businessId: number): Observable<{ deletedCount: number }> {
    const params = new HttpParams().set('businessId', String(businessId));
    return this.http.delete<{ deletedCount: number }>(`${this.businessBase}/images/all`, { params });
  }

  // ===================== OWNERS =====================

  /**
   * Some backends use POST /business-owners (no suffix),
   * others use POST /business-owners/create.
   * This supports both (tries /create first, falls back to base).
   */
  createOwner(dto: BusinessOwnerDto): Observable<BusinessOwnerDto> {
    return this.http.post<BusinessOwnerDto>(`${this.ownerBase}/create`, dto).pipe(
      catchError(() => this.http.post<BusinessOwnerDto>(`${this.ownerBase}`, dto))
    );
  }

  getOwner(id: number): Observable<BusinessOwnerDto> {
    return this.http.get<BusinessOwnerDto>(`${this.ownerBase}/get/${id}`);
  }

  listOwners(): Observable<BusinessOwnerDto[]> {
    return this.http.get<BusinessOwnerDto[]>(`${this.ownerBase}/list`);
  }

  getOwnerByEmail(email: string): Observable<BusinessOwnerDto> {
    const params = new HttpParams().set('email', email);
    return this.http.get<BusinessOwnerDto>(`${this.ownerBase}/email`, { params });
  }

  updateOwner(id: number, dto: BusinessOwnerDto): Observable<BusinessOwnerDto> {
    return this.http.put<BusinessOwnerDto>(`${this.ownerBase}/update/${id}`, dto);
  }

  deleteOwner(id: number): Observable<void> {
    return this.http.delete<void>(`${this.ownerBase}/delete/${id}`);
  }

  /**
   * Backward compatible:
   * - registerBusinessOwner(body, loginUserId)
   * - registerBusinessOwner(body, { loginUserId, companyId })
   */
  registerBusinessOwner(
    body: BusinessRegistrationDto,
    loginUserId?: number
  ): Observable<BusinessRegistrationDto>;
  registerBusinessOwner(
    body: BusinessRegistrationDto,
    opts?: { loginUserId?: number; companyId?: number }
  ): Observable<BusinessRegistrationDto>;
  registerBusinessOwner(
    body: BusinessRegistrationDto,
    loginUserIdOrOpts?: number | { loginUserId?: number; companyId?: number }
  ): Observable<BusinessRegistrationDto> {
    let params = new HttpParams();

    if (typeof loginUserIdOrOpts === 'number') {
      params = params.set('loginUserId', String(loginUserIdOrOpts));
    } else if (loginUserIdOrOpts) {
      if (loginUserIdOrOpts.loginUserId != null)
        params = params.set('loginUserId', String(loginUserIdOrOpts.loginUserId));
      if (loginUserIdOrOpts.companyId != null)
        params = params.set('companyId', String(loginUserIdOrOpts.companyId));
    }

    return this.http.post<BusinessRegistrationDto>(`${this.ownerBase}/register`, body, { params });
  }

  // ===================== SERVICES =====================

  listServices(): Observable<ServiceIdName[]> {
    return this.http.get<ServiceIdName[]>(`${this.base}/business-services/list`);
  }

  getService(id: number): Observable<BusinessServiceDto> {
    return this.http.get<BusinessServiceDto>(`${this.base}/business-services/get/${id}`);
  }

  createService(dto: BusinessServiceDto): Observable<BusinessServiceDto> {
    return this.http.post<BusinessServiceDto>(`${this.base}/business-services/create`, dto);
  }

  updateService(id: number, dto: BusinessServiceDto): Observable<BusinessServiceDto> {
    return this.http.put<BusinessServiceDto>(`${this.base}/business-services/update/${id}`, dto);
  }

  deleteService(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/business-services/delete/${id}`);
  }

  listCategoriesVM(): Observable<Array<{ id: number; name: string }>> {
    return this.listServices().pipe(
      map((svcs: ServiceIdName[]) => svcs.map((s) => ({ id: s.serviceId, name: s.serviceName })))
    );
  }

  // ===================== EXISTING COMPANIES =====================

 // ✅ NEW: Existing Companies list (supports multiple backend shapes)
getExistingCompanies(loginUserId: number): Observable<CompanyLiteDto[]> {
  // 1) try: GET /company/existing
  return this.http.get<CompanyLiteDto[]>(`${this.companyBase}/existing`).pipe(
    // 2) fallback: GET /company/existing/{loginUserId}
    catchError(() =>
      this.http.get<CompanyLiteDto[]>(`${this.companyBase}/existing/${loginUserId}`)
    ),
    // 3) fallback: GET /company/existing?loginUserId=
    catchError(() => {
      const params = new HttpParams().set('loginUserId', String(loginUserId));
      return this.http.get<CompanyLiteDto[]>(`${this.companyBase}/existing`, { params });
    })
  );
}
}
