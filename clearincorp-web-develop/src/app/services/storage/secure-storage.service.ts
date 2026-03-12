import { Injectable, Inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import * as CryptoJS from 'crypto-js';
import { environment } from 'src/environments/environment';


export type StorageKind = 'local' | 'session';
export type StorageMode = 'local' | 'session' | 'both';

interface TokensGroup {
  accessToken?: string | null;
  idToken?: string | null;
}

interface LoggedInUserGroup {
  loginUserId?: string | null;
  email?: string | null;
  userData?: any;
}

interface CompanyDetailsGroup {
  companyCount?: number | null;
  progressList?: any;
}

@Injectable({ providedIn: 'root' })
export class SecureStorageService {
  // SECURITY: Store tokens in sessionStorage (cleared on browser close)
  // This is more secure than localStorage for sensitive auth tokens
  private readonly TOKENS_MODE: StorageMode        = 'session';
  private readonly LOGGEDIN_USER_MODE: StorageMode = 'session';
  private readonly COMPANY_DETAILS_MODE: StorageMode = 'local';

  private readonly KEY_TOKENS = 'tokens';
  private readonly KEY_LOGGED_IN_USER = 'logged_in_user';
  private readonly KEY_COMPANY_DETAILS = 'company_details';

  // Namespace prevents your app from wiping other data on localhost
  private readonly KEY_PREFIX = 'cic_S_';

  // SECURITY: Secret key should be injected at build time
  // Validate that a real key is present, not just a placeholder
  private readonly secretKey: string;

  private validateSecretKey(): void {
    if (!this.secretKey ||
        this.secretKey.includes('PLACEHOLDER') ||
        this.secretKey.length < 16) {
      console.error('SecureStorageService: Invalid or missing encryption key. Data will not be secure.');
    }
  }

  constructor(@Inject(PLATFORM_ID) private platformId: Object) {
    this.secretKey = environment.cryptoSecretKey || '';
    this.validateSecretKey();
  }

  // logged in user methods
  setLoginUserId(loginUserId: string | number): void {
    const mode = this.LOGGEDIN_USER_MODE;
    const current =
      this.readByMode<LoggedInUserGroup>(this.KEY_LOGGED_IN_USER, mode) || {};
    const updated: LoggedInUserGroup = {
      ...current,
      loginUserId: String(loginUserId),
    };
    this.storeByMode(this.KEY_LOGGED_IN_USER, updated, mode);
  }

  getLoginUserId(): string | null {
    const user = this.readByMode<LoggedInUserGroup>(
      this.KEY_LOGGED_IN_USER,
      this.LOGGEDIN_USER_MODE
    );
    return user?.loginUserId ?? null;
  }

  setLoggedInUserEmail(email: string | null): void {
    const mode = this.LOGGEDIN_USER_MODE;
    const current =
      this.readByMode<LoggedInUserGroup>(this.KEY_LOGGED_IN_USER, mode) || {};
    const updated: LoggedInUserGroup = { ...current, email: email ?? null };
    this.storeByMode(this.KEY_LOGGED_IN_USER, updated, mode);
  }

  getLoggedInUserEmail(): string | null {
    const user = this.readByMode<LoggedInUserGroup>(
      this.KEY_LOGGED_IN_USER,
      this.LOGGEDIN_USER_MODE
    );
    return user?.email ?? null;
  }

  setLoggedInUserData(userData: any): void {
    const mode = this.LOGGEDIN_USER_MODE;
    const current =
      this.readByMode<LoggedInUserGroup>(this.KEY_LOGGED_IN_USER, mode) || {};
    const updated: LoggedInUserGroup = { ...current, userData };
    this.storeByMode(this.KEY_LOGGED_IN_USER, updated, mode);
  }

  getLoggedInUserData(): any {
    const user = this.readByMode<LoggedInUserGroup>(
      this.KEY_LOGGED_IN_USER,
      this.LOGGEDIN_USER_MODE
    );
    return user?.userData ?? null;
  }

   setLoggedInUser(
    loginUserId: string | number,
    email: string | null,
    userData: any
  ): void {
    const mode = this.LOGGEDIN_USER_MODE;
    const updated: LoggedInUserGroup = {
      loginUserId: String(loginUserId),
      email: email ?? null,
      userData,
    };
    this.storeByMode(this.KEY_LOGGED_IN_USER, updated, mode);
  }

  getLoggedInUser(): LoggedInUserGroup | null {
    return this.readByMode<LoggedInUserGroup>(
      this.KEY_LOGGED_IN_USER,
      this.LOGGEDIN_USER_MODE
    );
  }

   clearLoggedInUser(): void {
    this.removeEverywhere(this.KEY_LOGGED_IN_USER);
  }

 //token methods
  setAccessToken(accessToken: string): void {
    const mode = this.TOKENS_MODE;
    const current = this.readByMode<TokensGroup>(this.KEY_TOKENS, mode) || {};
    const updated: TokensGroup = { ...current, accessToken };
    this.storeByMode(this.KEY_TOKENS, updated, mode);
  }

  setIdToken(idToken: string): void {
    const mode = this.TOKENS_MODE;
    const current = this.readByMode<TokensGroup>(this.KEY_TOKENS, mode) || {};
    const updated: TokensGroup = { ...current, idToken };
    this.storeByMode(this.KEY_TOKENS, updated, mode);
  }

  getAccessToken(): string | null {
    const tokens = this.readByMode<TokensGroup>(this.KEY_TOKENS, this.TOKENS_MODE);
    return tokens?.accessToken ?? null;
  }

  getIdToken(): string | null {
    const tokens = this.readByMode<TokensGroup>(this.KEY_TOKENS, this.TOKENS_MODE);
    return tokens?.idToken ?? null;
  }

  clearTokens(): void {
    this.removeEverywhere(this.KEY_TOKENS);
  }

   setCompanyCount(count: number): void {
    const mode = this.COMPANY_DETAILS_MODE;
    const current =
      this.readByMode<CompanyDetailsGroup>(this.KEY_COMPANY_DETAILS, mode) ||
      {};
    const updated: CompanyDetailsGroup = { ...current, companyCount: count };
    this.storeByMode(this.KEY_COMPANY_DETAILS, updated, mode);
  }

  getCompanyCount(): number | null {
    const details = this.readByMode<CompanyDetailsGroup>(
      this.KEY_COMPANY_DETAILS,
      this.COMPANY_DETAILS_MODE
    );
    return details?.companyCount ?? null;
  }

    setCompanyProgressList(progressList: any): void {
    const mode = this.COMPANY_DETAILS_MODE;
    const current =
      this.readByMode<CompanyDetailsGroup>(this.KEY_COMPANY_DETAILS, mode) ||
      {};
    const updated: CompanyDetailsGroup = { ...current, progressList };
    this.storeByMode(this.KEY_COMPANY_DETAILS, updated, mode);
  }

  getCompanyProgressList(): any {
    const details = this.readByMode<CompanyDetailsGroup>(
      this.KEY_COMPANY_DETAILS,
      this.COMPANY_DETAILS_MODE
    );
    return details?.progressList ?? null;
  }

   getCompanyDetails(): CompanyDetailsGroup | null {
    return this.readByMode<CompanyDetailsGroup>(
      this.KEY_COMPANY_DETAILS,
      this.COMPANY_DETAILS_MODE
    );
  }

  clearCompanyDetails(): void {
    this.removeEverywhere(this.KEY_COMPANY_DETAILS);
  }

 setItem(key: string, value: unknown, kind: StorageKind = 'local'): void {
    this.setItemRaw(key, value, kind);
  }

  getItem<T = unknown>(key: string, kind: StorageKind = 'local'): T | null {
    return this.getItemRaw<T>(key, kind);
  }

  removeItem(key: string, kind: StorageKind = 'local'): void {
    this.removeItemRaw(key, kind);
  }

  clear(kind: StorageKind = 'local'): void {
    const storage = this.getStorage(kind);
    if (!storage) return;

    const keysToRemove: string[] = [];
    for (let i = 0; i < storage.length; i++) {
      const k = storage.key(i);
      if (k && k.startsWith(this.KEY_PREFIX)) {
        keysToRemove.push(k);
      }
    }
    keysToRemove.forEach((k) => storage.removeItem(k));
  }

  // ========================================================================
  // ⚙️ MODE HELPERS (local / session / both)
  // ========================================================================

 private storeByMode(key: string, value: unknown, mode: StorageMode): void {
    if (mode === 'local' || mode === 'both') {
      this.setItemRaw(key, value, 'local');
    }
    if (mode === 'session' || mode === 'both') {
      this.setItemRaw(key, value, 'session');
    }
  }

  private readByMode<T = unknown>(key: string, mode: StorageMode): T | null {
    if (mode === 'session') {
      return this.getItemRaw<T>(key, 'session');
    }
    if (mode === 'local') {
      return this.getItemRaw<T>(key, 'local');
    }

    // mode === 'both' → prefer session, fallback local
    const fromSession = this.getItemRaw<T>(key, 'session');
    if (fromSession !== null && fromSession !== undefined) {
      return fromSession;
    }
    return this.getItemRaw<T>(key, 'local');
  }

   private removeEverywhere(key: string): void {
    this.removeItemRaw(key, 'local');
    this.removeItemRaw(key, 'session');
  }

  // ========================================================================
  // 🧩 INTERNAL RAW HELPERS
  // ========================================================================
 private setItemRaw(key: string, value: unknown, kind: StorageKind): void {
    const storage = this.getStorage(kind);
    if (!storage) return;

    try {
      const hashedKey = this.hashKey(key);
      const json = JSON.stringify(value);
      const cipherText = this.encrypt(json);
      storage.setItem(hashedKey, cipherText);
    } catch (e) {
      console.error('[SecureStorage] setItemRaw error', e);
    }
  }


  private getItemRaw<T = unknown>(key: string, kind: StorageKind): T | null {
    const storage = this.getStorage(kind);
    if (!storage) return null;

    try {
      const hashedKey = this.hashKey(key);
      const cipherText = storage.getItem(hashedKey);
      if (!cipherText) return null;

      const json = this.decrypt(cipherText);
      if (!json) return null;

      return JSON.parse(json) as T;
    } catch {
      return null;
    }
  }


  private removeItemRaw(key: string, kind: StorageKind): void {
    const storage = this.getStorage(kind);
    if (!storage) return;

    try {
      const hashedKey = this.hashKey(key);
      storage.removeItem(hashedKey);
    } catch (e) {
      console.error('[SecureStorage] removeItemRaw error', e);
    }
  }

  private getStorage(kind: StorageKind): Storage | null {
    if (!isPlatformBrowser(this.platformId)) {
      return null;
    }
    return kind === 'session' ? window.sessionStorage : window.localStorage;
  }

  private hashKey(key: string): string {
    let hash = 0;
    const combined = this.secretKey + key;
    for (let i = 0; i < combined.length; i++) {
      hash = (hash << 5) - hash + combined.charCodeAt(i);
      hash |= 0;
    }
    return `${this.KEY_PREFIX}${Math.abs(hash).toString(16)}`;
  }

  // --- Value "encryption" (XOR + base64) ---
  // This makes it unreadable to humans but is not strong cryptography.
  // For real security, swap this with proper AES (e.g. CryptoJS).

private encrypt(text: string): string {
  if (!text) return '';
  // AES encrypt with secretKey as passphrase
  const cipher = CryptoJS.AES.encrypt(text, this.secretKey);
  return cipher.toString(); // Base64 string
}

private decrypt(ciphertext: string): string | null {
  if (!ciphertext) return null;
  try {
    const bytes = CryptoJS.AES.decrypt(ciphertext, this.secretKey);
    const plain = bytes.toString(CryptoJS.enc.Utf8);
    // If secret is wrong or data is corrupted, plain will be ''
    return plain || null;
  } catch {
    return null;
  }
}
}
