// src/app/services/token.service.ts
import { Injectable } from '@angular/core';
import { jwtDecode } from 'jwt-decode';
import * as CryptoJS from 'crypto-js';
import { SecureStorageService } from "src/app/services/storage/secure-storage.service";
import { SessionFlagsService } from "src/app/services/login/session-flags.service";
import { environment } from 'src/environments/environment';

@Injectable({ providedIn: 'root' })
export class TokenService {
  private secretKey = environment.cryptoSecretKey; // 🔒 Use env variable in production

  constructor(private secureStorage : SecureStorageService,  private sessionFlags: SessionFlagsService,) {}

  private getKey(type?: 'id'): string {
    return type === 'id' ? 'idToken' : 'accessToken';
  }

  saveToken(token: string, type?: 'id'): void {
    const tokenKey = this.getKey(type);
    if(tokenKey === 'accessToken') {
      this.secureStorage.setAccessToken(token);
    } else if(tokenKey === 'idToken') {
      this.secureStorage.setIdToken(token);
    }
  }

  getToken(type?: 'id'): string | null {
    const tokenKey = this.getKey(type);
    if(tokenKey === 'accessToken') {
      return this.secureStorage.getAccessToken();
    } else if(tokenKey === 'idToken') {
      return this.secureStorage.getIdToken();
    }
    return null;
  }

  getRole(): string | null {
    // Extracts role from the access token
    const token = this.getToken();
    if (!token) return null;

    try {
      const decoded: any = jwtDecode(token);
      return decoded['cognito:groups']?.[0] || null;
    } catch {
      return null;
    }
  }

  getEmail(): string | null {
    // Extracts role from the access token
    const token = this.getToken('id');
    if (!token) return null;

    try {
      const decoded: any = jwtDecode(token);
      return decoded?.['email'] || null;
    } catch {
      return null;
    }
  }

  isTokenExpired(): boolean {
    const token = this.getToken();
    if (!token) return true;

    const decoded: any = jwtDecode(token);
    const exp = decoded?.exp;
    if (!exp) return true;

    const now = Math.floor(Date.now() / 1000);
    return now >= exp;
  }

  clearToken(): void {
    this.secureStorage.clearTokens();
    this.secureStorage.clearLoggedInUser();
    this.secureStorage.clearCompanyDetails();
    localStorage.removeItem('email');
    this.sessionFlags.clear();
    sessionStorage.clear();
  }
}
