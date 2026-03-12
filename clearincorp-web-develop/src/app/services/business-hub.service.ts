import { Injectable, signal } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import {
  BusinessHubApi,
  BusinessAdminPageDto,
  BusinessByAdminResponseDto,
  RejectBusinessResponse,
} from './business-hub.api';

import {
  BusinessDto,
  BusinessRegistrationDto,
  Page,
  WeightedBusinessHitDto,
  BusinessServiceDto,
  CompanyLiteDto,
  CompanyFilterResponse,
} from '../models/business-backend';

/** Card VM used in lists/grids */
export interface BusinessCardVM {
  id: number;
  name: string;
  description: string;
  rating: number;
  reviewCount: number;
  address: string;
  phone: string;
  email: string;
  website: string;
  hours: string;
  image: string;
  tags: string[];
  memberSince: number;
  verified: boolean;
  featured: boolean;
  ownerId?: number;
}

/** Detail VM used in the business detail page */
export interface DetailedBusinessVM extends BusinessCardVM {
  fullDescription: string;
  services: string[];
  gallery: string[];
  reviews: Array<{
    id: number;
    author: string;
    rating: number;
    comment: string;
    date: string;
  }>;
}

/** Dialog VM (business + owner) */
export interface BusinessDialogVM {
  business: any;
  owner: any;
  verified?: boolean;
}

export interface Category {
  name: string;
  value: string;
  count: number;
  image: string;
  color: string;
}

@Injectable({ providedIn: 'root' })
export class BusinessHubService {
  private loading = signal(false);
  getLoadingState() {
    return this.loading.asReadonly();
  }

  constructor(private api: BusinessHubApi) {}

  // ===================== OWNER-LANE APIs (optional) =====================

  // getInProgressBusinesses(loginUserId: number): Observable<BusinessDto[]> {
  //   const apiAny: any = this.api as any;
  //   if (typeof apiAny.getOwnerBusinessesInProgress === 'function') {
  //     return apiAny.getOwnerBusinessesInProgress(loginUserId) as Observable<BusinessDto[]>;
  //   }
  //   return new Observable<BusinessDto[]>((sub) => {
  //     sub.next([]);
  //     sub.complete();
  //   });
  // }

  // getRegisteredBusinesses(loginUserId: number): Observable<BusinessDto[]> {
  //   const apiAny: any = this.api as any;
  //   if (typeof apiAny.getOwnerBusinessesRegistered === 'function') {
  //     return apiAny.getOwnerBusinessesRegistered(loginUserId) as Observable<BusinessDto[]>;
  //   }
  //   return new Observable<BusinessDto[]>((sub) => {
  //     sub.next([]);
  //     sub.complete();
  //   });
  // }

  // getExistingCompanies(loginUserId: number): Observable<CompanyLiteDto[]> {
  //   const apiAny: any = this.api as any;
  //   if (typeof apiAny.getExistingCompanies === 'function') {
  //     return apiAny.getExistingCompanies(loginUserId) as Observable<CompanyLiteDto[]>;
  //   }
  //   return new Observable<CompanyLiteDto[]>((sub) => {
  //     sub.next([]);
  //     sub.complete();
  //   });
  // }
   getInProgressBusinesses(loginUserId: number): Observable<BusinessDto[]> {
  return this.api.getOwnerBusinessesInProgress(loginUserId);
}


getRegisteredBusinesses(loginUserId: number): Observable<BusinessDto[]> {
  return this.api.getOwnerBusinessesRegistered(loginUserId);
}
  getExistingCompanies(loginUserId: number): Observable<CompanyLiteDto[]> {
  return this.api.getExistingCompanies(loginUserId);
}

  // ===================== IMAGES / GALLERY =====================

  /** Returns usable <img src="..."> urls. Supports both listImages() and listImageUrls() API shapes. */
  getGalleryUrls(businessId: number): Observable<string[]> {
    const apiAny: any = this.api as any;

    if (typeof apiAny.listImageUrls === 'function') {
      return (apiAny.listImageUrls(businessId) as Observable<string[]>).pipe(
        map((urls) => (urls ?? []).filter(Boolean)),
      );
    }

    // fallback: listImages already mapped to string[]
    return this.api.listImages(businessId).pipe(map((urls: string[]) => (urls ?? []).filter(Boolean)));
  }

  /** Backward compat name used in hub component */
  listImages(businessId: number): Observable<string[]> {
    return this.getGalleryUrls(businessId);
  }

  getFirstGalleryImage(businessId: number): Observable<string | null> {
    return this.getGalleryUrls(businessId).pipe(
      map((arr: string[]) => (arr && arr.length ? arr[0] : null)),
    );
  }

  // ===================== CATEGORIES =====================

  getCategories(): Observable<Category[]> {
    return this.api.listCategoriesVM().pipe(
      map((services: Array<{ id: number; name: string }>) =>
        (services ?? []).map((s, i) => ({
          name: s.name,
          value: String(s.id),
          count: 0,
          image: this.pickCategoryImage(s.name, i),
          color: this.pickColor(i),
        })),
      ),
    );
  }

  getServiceName(serviceId: number): Observable<string> {
    return this.api.getService(serviceId).pipe(map((s: BusinessServiceDto) => s.serviceName));
  }

  // ===================== DIRECTORY / SEARCH =====================

  searchDirectory(opts: {
    q?: string;
    categoryValue?: string;
    zip?: string;
    minRating?: number;
    page?: number;
    size?: number;
  }): Observable<BusinessCardVM[]> {
    const serviceId =
      opts.categoryValue && opts.categoryValue !== 'all'
        ? Number(opts.categoryValue)
        : ('all' as const);

    return this.api
      .searchPublicDirectory({
        q: opts.q,
        serviceId,
        zip: opts.zip,
        minRating: opts.minRating,
        page: opts.page,
        size: opts.size,
      })
      .pipe(map((res) => (res?.content ?? []).map(this.toCardVM)));
  }

  getBusinessesByCategory(categoryServiceId: number | string): Observable<BusinessCardVM[]> {
    const sid = Number(categoryServiceId);
    return this.api.getBusinessesByAdmin({ serviceId: sid, page: 0, size: 50 }).pipe(
      map((resp: BusinessByAdminResponseDto) => {
        const regs = resp?.content ?? [];
        return regs.map((reg) => {
          const anyReg: any = reg;
          const b: BusinessDto = (anyReg.business ?? reg) as BusinessDto;
          return this.toCardVM(b);
        });
      }),
    );
  }

  searchWeighted(q: string, page = 0, size = 10): Observable<Page<WeightedBusinessHitDto>> {
    return this.api.searchWeighted(q, page, size);
  }

  // ===================== DETAIL / DIALOG / FETCH =====================

  getCompanyById(companyId: number): Observable<CompanyLiteDto> {
    return this.api.getCompany(companyId);
  }

  getBusinessById(businessId: number): Observable<BusinessRegistrationDto> {
    return this.api.getBusiness(businessId);
  }

  /** For PATCH form: use company endpoint */
  getBusinessDetails(companyId: number): Observable<CompanyLiteDto> {
    return this.getCompanyById(companyId);
  }

  getDetailsVM(businessId: number): Observable<DetailedBusinessVM> {
    return this.getBusinessById(businessId).pipe(
      map((reg) => this.toDetailedBusinessVMFromRegistration(reg)),
    );
  }

  getDetails(businessId: number): Observable<BusinessDialogVM> {
    return this.getBusinessById(businessId).pipe(map((reg) => this.toDialogVMFromRegistration(reg)));
  }

  // ===================== ADMIN / VERIFY / UPLOADS =====================

  adminList(args: {
    page: number;
    size: number;
    serviceId?: number;
    zipCode?: string;
  }): Observable<BusinessAdminPageDto> {
    return this.api.adminList(args);
  }

  /** NEW: createdBy == 'Admin' list endpoint */
  listAdminCreatedBusinesses(): Observable<BusinessRegistrationDto[]> {
    return this.api.listAdminCreatedBusinesses();
  }

  toggleVerify(businessId: number, loginUserId: number) {
    return this.api.verifyIfPending(businessId, loginUserId);
  }

  uploadImages(businessId: number, files: File[]) {
    return this.api.uploadImages(businessId, files);
  }

  rejectBusiness(businessId: number): Observable<RejectBusinessResponse> {
    return this.api.rejectBusiness(businessId);
  }

  // ✅ /company/filter
  getUserCompanies(loginUserId: number): Observable<CompanyFilterResponse> {
    return this.api.searchCompanyFilter({
      loginUserId,
      page: 0,
      size: 50,
    });
  }

  // ===================== REGISTRATION =====================

  registerBusinessAndOwner(payload: {
    owner: { firstName: string; lastName: string; email: string; phone: string };
    business: {
      serviceId: number;
      businessName: string;
      serviceDescription?: string;
      yearsInBusiness?: string;
      businessAddress?: string;
      city?: string;
      zipCode?: string;
      websiteUrl?: string;
      businessHours?: string;
      state?: string;
    };
    images?: File[];
    loginUserId?: number;
    companyId?: number;
  }): Observable<{ res: BusinessRegistrationDto; images: File[] }> {
    const body: BusinessRegistrationDto = {
      owner: {
        firstName: payload.owner.firstName,
        lastName: payload.owner.lastName,
        personalEmail: payload.owner.email,
        contactNumber: payload.owner.phone,
      },
      business: {
        businessId: undefined,
        ownerId: undefined,
        serviceId: payload.business.serviceId,
        businessName: payload.business.businessName,
        serviceDescription: payload.business.serviceDescription || null,
        yearsInBusiness: payload.business.yearsInBusiness,
        businessAddress: payload.business.businessAddress || null,
        city: payload.business.city || null,
        zipCode: payload.business.zipCode || null,
        websiteUrl: payload.business.websiteUrl || null,
        businessHours: payload.business.businessHours || null,
        businessEmail: payload.owner.email || null,
        state: payload.business.state || null,
        createdBy: null,
        updatedBy: null,
        createdOn: null,
        updatedOn: null,
      },
    };

    // supports both API signatures:
    // 1) registerBusinessOwner(body, loginUserId)
    // 2) registerBusinessOwner(body, { loginUserId, companyId })
    const apiAny: any = this.api as any;
    const secondArg =
      payload.companyId != null
        ? { loginUserId: payload.loginUserId, companyId: payload.companyId }
        : payload.loginUserId;

    return (apiAny.registerBusinessOwner(body, secondArg) as Observable<BusinessRegistrationDto>).pipe(
      map((res) => ({ res, images: payload.images || [] })),
    );
  }

  // ===================== SERVICES LIST =====================

  listServices(): Observable<any[]> {
    return this.api.listServices() as any;
  }

  // ===================== MAPPERS =====================

  /** Mapper for list/grid cards based on BusinessDto. */
  private toCardVM = (b: BusinessDto): BusinessCardVM => {
    const anyB: any = b;
    const galleryArr: string[] | undefined = Array.isArray(anyB.gallery) ? anyB.gallery : undefined;
    const firstGallery = galleryArr?.[0];

    return {
      id: b.businessId ?? 0,
      name: b.businessName,
      description: b.serviceDescription || '',
      rating: Number(anyB.avgRating ?? 0),
      reviewCount: Number(anyB.reviewsCount ?? 0),
      address: b.businessAddress || '',
      phone: anyB.contactPhone || '',
      email: b.businessEmail || '',
      website: b.websiteUrl || '',
      hours: b.businessHours || '',
      image: firstGallery ?? anyB.logoUrl ?? '/assets/placeholder-business.jpg',
      tags: anyB.tags ?? [],
      memberSince: b.createdOn ? new Date(b.createdOn).getFullYear() : 2022,
      verified:
        anyB.verified === true ||
        anyB.statusName === 'verified' ||
        (typeof anyB.updatedBy === 'string' && anyB.updatedBy.toLowerCase() === 'admin'),
      featured: Boolean(anyB.featured ?? false),
      ownerId: anyB.ownerId,
    };
  };

  /** Mapper: CompanyLiteDto → DetailedBusinessVM (kept for completeness) */
  private toDetailedBusinessVM(dto: CompanyLiteDto): DetailedBusinessVM {
    const user = (dto as any).loginUser || {};
    const effectiveYear = (dto as any).companyEffectiveDate
      ? new Date((dto as any).companyEffectiveDate).getFullYear()
      : 2022;

    const base: BusinessCardVM = {
      id: (dto as any).businessId,
      name: (dto as any).companyName,
      description: (dto as any).tradeName || 'No description provided.',
      rating: 0,
      reviewCount: 0,
      address: `${(dto as any).city || ''}, ${(dto as any).state || ''}`,
      phone: user.phoneNumber || '',
      email: user.email || '',
      website: '',
      hours: '',
      image: '/assets/placeholder-business.jpg',
      tags: [(dto as any).tradeName || 'Service'],
      memberSince: effectiveYear,
      verified: true,
      featured: false,
      ownerId: user.loginUserId,
    };

    const anyDto: any = dto as any;
    const gallery: string[] = Array.isArray(anyDto.gallery)
      ? anyDto.gallery.filter((u: any) => typeof u === 'string')
      : [];
    const hero = gallery.length ? gallery[0] : anyDto.image ?? base.image;

    return {
      ...base,
      image: hero,
      fullDescription: anyDto.fullDescription || base.description,
      services: (anyDto.services ?? []) as string[],
      gallery,
      reviews: ((anyDto.reviews ?? []) as any[]).map((r: any, idx: number) => ({
        id: r?.id ?? idx + 1,
        author: r?.author ?? 'User',
        rating: Number(r?.rating ?? 0),
        comment: r?.comment ?? r?.text ?? '',
        date: r?.date ?? '',
      })),
    };
  }

  /** Mapper: BusinessRegistrationDto → DetailedBusinessVM */
  private toDetailedBusinessVMFromRegistration(reg: BusinessRegistrationDto): DetailedBusinessVM {
    const anyReg: any = reg;
    const business: any = anyReg.business ?? {};
    const owner: any = anyReg.owner ?? {};

    const gallery: string[] = Array.isArray(business.gallery)
      ? business.gallery.filter((u: any) => typeof u === 'string')
      : [];

    const hero = gallery.length ? gallery[0] : business.logoUrl ?? '/assets/placeholder-business.jpg';

    const baseCard: BusinessCardVM = {
      id: business.businessId,
      name: business.businessName,
      description: business.serviceDescription || '',
      rating: Number(business.avgRating ?? 0),
      reviewCount: Number(business.reviewsCount ?? 0),
      address: business.businessAddress || '',
      phone: business.contactPhone || owner.contactNumber || '',
      email: business.businessEmail || owner.personalEmail || '',
      website: business.websiteUrl || '',
      hours: business.businessHours || '',
      image: hero,
      tags: business.tags ?? [business.serviceName].filter(Boolean),
      memberSince: business.createdOn ? new Date(business.createdOn).getFullYear() : 2022,
      verified:
        business.verified === true ||
        business.statusName === 'verified' ||
        (typeof business.updatedBy === 'string' && business.updatedBy.toLowerCase() === 'admin'),
      featured: Boolean(business.featured ?? false),
      ownerId: owner.ownerId,
    };

    const rawReviews: any[] = Array.isArray(business.reviews) ? business.reviews : [];
    const reviews = rawReviews.map((r: any, idx: number) => ({
      id: r?.id ?? idx + 1,
      author: r?.author ?? r?.reviewerName ?? 'User',
      rating: Number(r?.rating ?? 0),
      comment: r?.comment ?? r?.text ?? '',
      date: r?.date ?? r?.createdOn ?? '',
    }));

    return {
      ...baseCard,
      fullDescription: business.fullDescription || baseCard.description,
      services: [business.serviceName].filter(Boolean),
      gallery,
      reviews,
    };
  }

  /** Mapper for dialog model from BusinessRegistrationDto. */
  private toDialogVMFromRegistration(reg: BusinessRegistrationDto): BusinessDialogVM {
    const anyReg: any = reg;
    const business = anyReg.business ?? {};
    const owner = anyReg.owner ?? {};

    const verified =
      business?.verified === true ||
      anyReg?.verified === true ||
      business?.statusName === 'verified' ||
      (typeof business?.updatedBy === 'string' && business.updatedBy.toLowerCase() === 'admin');

    return { business, owner, verified };
  }

  private pickCategoryImage(_name: string, i: number): string {
    const images = [
      'https://images.unsplash.com/photo-1674981208693-de5a9c4c4f44?q=80&w=1080',
      'https://images.unsplash.com/photo-1643391448659-8e58f99958b6?q=80&w=1080',
      'https://images.unsplash.com/photo-1643049751039-5e112a5953ae?q=80&w=1080',
      'https://images.unsplash.com/photo-1660592868727-858d28c3ba52?q=80&w=1080',
      'https://images.unsplash.com/photo-1590764095558-abd89de9db5f?q=80&w=1080',
      'https://images.unsplash.com/photo-1667388968964-4aa652df0a9b?q=80&w=1080',
      'https://images.unsplash.com/photo-1564732005956-20420ebdab60?q=80&w=1080',
      'https://images.unsplash.com/photo-1725724812270-b1f4a304bfef?q=80&w=1080',
    ];
    return images[i % images.length];
  }

  private pickColor(i: number): string {
    const colors = [
      'from-gray-800 to-gray-900',
      'from-green-700 to-green-800',
      'from-blue-700 to-blue-800',
      'from-orange-600 to-orange-700',
      'from-amber-600 to-amber-700',
      'from-pink-600 to-pink-700',
      'from-yellow-600 to-yellow-700',
      'from-red-600 to-red-700',
      'from-purple-600 to-purple-700',
      'from-emerald-600 to-emerald-700',
      'from-slate-600 to-slate-700',
      'from-indigo-600 to-indigo-700',
    ];
    return colors[i % colors.length];
  }
}
