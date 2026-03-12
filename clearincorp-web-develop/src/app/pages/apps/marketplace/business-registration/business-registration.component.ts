// src/app/pages/apps/marketplace/business-registration/business-registration.component.ts
import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, NgForm } from '@angular/forms';
import { Router } from '@angular/router';

import { of } from 'rxjs';
import { switchMap, catchError, finalize, map } from 'rxjs/operators';

import { BusinessHubService } from 'src/app/services/business-hub.service';
import { CompanyLiteDto } from 'src/app/models/business-backend';
import { SecureStorageService } from 'src/app/services/storage/secure-storage.service';

type Option = { label: string; value: string | number };
type StateOption = { label: string; value: string };

@Component({
  selector: 'app-business-registration',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './business-registration.component.html',
  styleUrls: ['./business-registration.component.scss'],
})
export class BusinessRegistrationComponent implements OnInit, OnDestroy {
  companyId: number | null = null;
  private loginUserId = 0;

  isLoadingDetails = false;
  showSuccess = false;
  isSubmitting = false;

  formData: {
    firstName: string;
    lastName: string;
    email: string;
    phoneNumber: string;

    businessName: string;
    businessService: number | '';
    website: string;
    yearsInBusiness: string;
    serviceDescription: string;
    businessHours: string;

    address: string;
    city: string;
    state: string;
    zipCode: string;
  } = {
    firstName: '',
    lastName: '',
    email: '',
    phoneNumber: '',
    businessName: '',
    businessService: '',
    website: '',
    yearsInBusiness: '',
    serviceDescription: '',
    businessHours: '',
    address: '',
    city: '',
    state: '',
    zipCode: '',
  };

  serviceTypes: Option[] = [];
  yearsOptions: Option[] = [
    { label: '0–1 years', value: '0-1' },
    { label: '2–5 years', value: '2-5' },
    { label: '6–10 years', value: '6-10' },
    { label: '11–20 years', value: '11-20' },
    { label: '20+ years', value: '20+' },
  ];
  usStates: StateOption[] = [];

  uploadedImages: string[] = [];
  private uploadedFiles: File[] = [];
  private objectUrls: string[] = [];

  constructor(private router: Router, private hub: BusinessHubService,private secureStorage: SecureStorageService) {}

  ngOnInit(): void {
    this.loginUserId = this.getLoginUserIdSafe();
    this.loadDropdowns();

    const state = history.state;
    if (state && state.companyId) {
      this.companyId = Number(state.companyId);
      this.fetchBusinessDetails(this.companyId);
    }
  }

  ngOnDestroy(): void {
    this.objectUrls.forEach((u) => URL.revokeObjectURL(u));
    this.objectUrls = [];
  }

  private loadDropdowns(): void {
    this.hub.listServices().subscribe({
      next: (list: any[] | null) => {
        this.serviceTypes = (list || []).map((c: any) => ({
          label: c.serviceName,
          value: c.serviceId,
        }));
      },
      error: () => {
        this.serviceTypes = [
          { label: 'Medical', value: 1 },
          { label: 'Technology', value: 2 },
          { label: 'Consulting', value: 3 },
        ];
      },
    });

    this.usStates = [
      { label: 'Texas', value: 'TX' },
      { label: 'California', value: 'CA' },
      { label: 'Florida', value: 'FL' },
      { label: 'New York', value: 'NY' },
    ];
  }

  private fetchBusinessDetails(id: number): void {
    if (!id) return;
    this.isLoadingDetails = true;

    this.hub
      .getCompanyById(id)
      .pipe(finalize(() => (this.isLoadingDetails = false)))
      .subscribe({
        next: (details: CompanyLiteDto) => details && this.patchBusinessDetails(details),
        error: (error) => console.error('Error fetching business details:', error),
      });
  }

  private patchBusinessDetails(data: CompanyLiteDto): void {
    const anyData: any = data;
    const user = anyData.loginUser || {};

    this.formData.firstName = user.firstName || '';
    this.formData.lastName = user.lastName || '';
    this.formData.email = user.email || '';
    this.formData.phoneNumber = user.phoneNumber || '';

    this.formData.businessName = anyData.companyName || '';
    this.formData.city = anyData.city || '';
    this.formData.state = anyData.state || '';
    this.formData.zipCode = anyData.zipCode || '';
    this.formData.serviceDescription = anyData.tradeName || '';
  }

  onBack(): void {
    this.router.navigate(['/apps/business-hub']);
  }

  onImageUpload(evt: Event): void {
    const input = evt.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;

    const files = Array.from(input.files);

    for (const file of files) {
      if (file.size > 10 * 1024 * 1024) continue;

      this.uploadedFiles.push(file);

      const url = URL.createObjectURL(file);
      this.objectUrls.push(url);
      this.uploadedImages.push(url);
    }

    input.value = '';
  }

  removeImage(i: number): void {
    if (i < 0 || i >= this.uploadedImages.length) return;

    const url = this.uploadedImages[i];
    try {
      URL.revokeObjectURL(url);
    } catch {}

    this.uploadedImages.splice(i, 1);
    this.uploadedFiles.splice(i, 1);
  }

  private toNum(v: any): number | null {
    const n = Number(v);
    return Number.isFinite(n) && n > 0 ? n : null;
  }

  onSubmit(form?: NgForm): void {
    if (this.isSubmitting) return;
    if (form && !form.valid) return;

    if (!this.loginUserId) {
      alert('Login user id missing. Please login again.');
      return;
    }

    const p = this.formData;
    const serviceId = Number(p.businessService);

    if (!Number.isFinite(serviceId) || serviceId <= 0) {
      alert('Please choose a service type.');
      return;
    }

    const payload = {
      companyId: this.companyId ?? undefined,
      loginUserId: this.loginUserId,
      owner: {
        firstName: p.firstName.trim(),
        lastName: p.lastName.trim(),
        email: p.email.trim(),
        phone: p.phoneNumber.trim(),
      },
      business: {
        serviceId,
        businessName: p.businessName.trim(),
        serviceDescription: p.serviceDescription?.trim() || '',
        yearsInBusiness: p.yearsInBusiness ? String(p.yearsInBusiness) : '',
        businessAddress: p.address?.trim() || '',
        city: p.city?.trim() || '',
        state: p.state?.trim() || '',
        zipCode: p.zipCode?.trim() || '',
        websiteUrl: p.website?.trim() || '',
        businessHours: p.businessHours?.trim() || '',
      },
      images: this.uploadedFiles,
    };

    this.isSubmitting = true;

    this.hub
      .registerBusinessAndOwner(payload)
      .pipe(
        switchMap(({ res, images }) => {
          const businessId =
            this.toNum((res as any)?.business?.businessId) ??
            this.toNum((res as any)?.businessId) ??
            this.toNum((res as any)?.id) ??
            null;

          // ✅ upload images ONLY if businessId exists
          if (businessId && images && images.length) {
            return this.hub.uploadImages(businessId, images).pipe(
              catchError((err) => {
                console.error('uploadImages failed', err);
                return of([] as string[]);
              }),
              map(() => businessId)
            );
          }

          return of(businessId ?? 0);
        }),
        finalize(() => (this.isSubmitting = false))
      )
      .subscribe({
        next: () => {
          this.showSuccess = true;
          setTimeout(() => {
            this.router.navigate(['/apps/business-hub'], {
              queryParams: { lane: 'progress', refresh: 1, t: Date.now() },
            });
          }, 1500);
        },
        error: () => alert('Submission failed. Please try again.'),
      });
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

  private staticStates(): StateOption[] {
    return [
      { label: 'Texas', value: 'TX' },
      { label: 'California', value: 'CA' },
      { label: 'Florida', value: 'FL' },
      { label: 'New York', value: 'NY' },
    ];
  }

phoneDisplay = '';    // usa format

onPhoneInput(value: string) {
  const digits = (value || '').replace(/\D/g, '').slice(0, 10);

  // store digits only in your actual model (best for backend)
  this.formData.phoneNumber = digits;

  // show formatted in the input
  this.phoneDisplay = this.formatUSPhone(digits);
}

private formatUSPhone(digits: string): string {
  const a = digits.slice(0, 3);
  const b = digits.slice(3, 6);
  const c = digits.slice(6, 10);

  if (digits.length === 0) return '';
  if (digits.length < 4) return `(${a}`;
  if (digits.length < 7) return `(${a}) ${b}`;
  return `(${a}) ${b}-${c}`;
}
  
}
