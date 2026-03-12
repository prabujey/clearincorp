/**
 * Firebase Authentication Service
 * Alternative to AWS Cognito authentication.
 *
 * To use Firebase Auth:
 * 1. Install: npm install firebase
 * 2. Add Firebase config to environment files
 * 3. Import and inject this service
 *
 * Firebase is client-side first - tokens are managed automatically by Firebase SDK.
 */
import { Injectable, Inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { BehaviorSubject, Observable, from } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from 'src/environments/environment';
import { SecureStorageService } from '../storage/secure-storage.service';
import { SessionFlagsService } from '../login/session-flags.service';

// Firebase imports - dynamically loaded
import type { FirebaseApp } from 'firebase/app';
import type { Auth, User, UserCredential } from 'firebase/auth';

@Injectable({ providedIn: 'root' })
export class FirebaseAuthService {
  private app: FirebaseApp | null = null;
  private auth: Auth | null = null;
  private currentUserSubject = new BehaviorSubject<User | null>(null);
  private initialized = false;

  currentUser$ = this.currentUserSubject.asObservable();
  isAuthenticated$ = this.currentUser$.pipe(map(user => user !== null));

  constructor(
    @Inject(PLATFORM_ID) private platformId: object,
    private secureStorage: SecureStorageService,
    private sessionFlags: SessionFlagsService
  ) {
    if (isPlatformBrowser(this.platformId)) {
      this.initializeFirebase();
    }
  }

  /**
   * Initialize Firebase SDK dynamically.
   * This prevents SSR issues and allows lazy loading.
   */
  private async initializeFirebase(): Promise<void> {
    if (this.initialized || !isPlatformBrowser(this.platformId)) {
      return;
    }

    // Check if Firebase config exists
    const firebaseConfig = (environment as any).firebase;
    if (!firebaseConfig || !firebaseConfig.apiKey) {
      console.warn('Firebase config not found in environment. Firebase auth disabled.');
      return;
    }

    try {
      // Dynamic imports to avoid SSR issues
      const { initializeApp } = await import('firebase/app');
      const { getAuth, onAuthStateChanged } = await import('firebase/auth');

      this.app = initializeApp(firebaseConfig);
      this.auth = getAuth(this.app);

      // Listen for auth state changes
      onAuthStateChanged(this.auth, async (user) => {
        this.currentUserSubject.next(user);

        if (user) {
          // Store token for API calls
          const token = await user.getIdToken();
          this.secureStorage.setIdToken(token);
          this.secureStorage.setAccessToken(token);
          this.secureStorage.setLoggedInUserEmail(user.email);

          // Update session flags
          this.sessionFlags.set({ isLoggedIn: true, email: user.email || '' });
        } else {
          this.clearAuth();
        }
      });

      this.initialized = true;
      console.log('Firebase Auth initialized successfully');
    } catch (error) {
      console.error('Failed to initialize Firebase:', error);
    }
  }

  /**
   * Sign in with email and password.
   */
  async signInWithEmail(email: string, password: string): Promise<UserCredential> {
    await this.ensureInitialized();

    const { signInWithEmailAndPassword } = await import('firebase/auth');
    return signInWithEmailAndPassword(this.auth!, email, password);
  }

  /**
   * Sign up with email and password.
   */
  async signUpWithEmail(email: string, password: string): Promise<UserCredential> {
    await this.ensureInitialized();

    const { createUserWithEmailAndPassword, sendEmailVerification } = await import('firebase/auth');
    const credential = await createUserWithEmailAndPassword(this.auth!, email, password);

    // Send verification email
    if (credential.user) {
      await sendEmailVerification(credential.user);
    }

    return credential;
  }

  /**
   * Sign in with Google popup.
   */
  async signInWithGoogle(): Promise<UserCredential> {
    await this.ensureInitialized();

    const { signInWithPopup, GoogleAuthProvider } = await import('firebase/auth');
    const provider = new GoogleAuthProvider();
    return signInWithPopup(this.auth!, provider);
  }

  /**
   * Sign out the current user.
   */
  async signOut(): Promise<void> {
    await this.ensureInitialized();

    const { signOut } = await import('firebase/auth');
    await signOut(this.auth!);
    this.clearAuth();
  }

  /**
   * Get the current user's ID token for API calls.
   */
  async getIdToken(forceRefresh = false): Promise<string | null> {
    const user = this.currentUserSubject.value;
    if (!user) return null;

    try {
      const token = await user.getIdToken(forceRefresh);
      this.secureStorage.setIdToken(token);
      this.secureStorage.setAccessToken(token);
      return token;
    } catch (error) {
      console.error('Failed to get ID token:', error);
      return null;
    }
  }

  /**
   * Refresh the current user's token.
   */
  async refreshToken(): Promise<string | null> {
    return this.getIdToken(true);
  }

  /**
   * Send password reset email.
   */
  async sendPasswordResetEmail(email: string): Promise<void> {
    await this.ensureInitialized();

    const { sendPasswordResetEmail } = await import('firebase/auth');
    await sendPasswordResetEmail(this.auth!, email);
  }

  /**
   * Check if user is currently authenticated.
   */
  isAuthenticated(): boolean {
    return this.currentUserSubject.value !== null;
  }

  /**
   * Get current user's email.
   */
  getEmail(): string | null {
    return this.currentUserSubject.value?.email || null;
  }

  /**
   * Get current user's UID.
   */
  getUid(): string | null {
    return this.currentUserSubject.value?.uid || null;
  }

  /**
   * Wait for Firebase to be initialized.
   */
  private async ensureInitialized(): Promise<void> {
    if (!this.initialized) {
      await this.initializeFirebase();
    }

    if (!this.auth) {
      throw new Error('Firebase Auth not initialized. Check your environment configuration.');
    }
  }

  /**
   * Clear all auth data.
   */
  private clearAuth(): void {
    this.secureStorage.clearTokens();
    this.secureStorage.clearLoggedInUser();
    this.sessionFlags.clear();
  }
}
