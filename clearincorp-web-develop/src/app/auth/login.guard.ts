import { Injectable } from '@angular/core';
import { CanActivate, Router, UrlTree } from '@angular/router';
import { TokenService } from 'src/app/services/token/token.service';
import { SessionFlagsService } from 'src/app/services/login/session-flags.service';
import { SecureStorageService } from '../services/storage/secure-storage.service';

@Injectable({
  providedIn: 'root',
})
export class LoginGuard implements CanActivate {
  constructor(
    private router: Router,
    private tokenService: TokenService,
    private sessionFlags: SessionFlagsService,
    private secureStorage: SecureStorageService
  ) {}

  canActivate(): boolean | UrlTree {
    // 1. Check if we have a valid session cookie
    const session = this.sessionFlags.getFlags();
    const hasValidSession =
      !!session &&
      session.isLoggedIn === true &&
      typeof session.sessionExpiryMs === 'number' &&
      Date.now() <= session.sessionExpiryMs;

    console.log("Session expiried ", session?.sessionExpiryMs);
    

    // 2. Check if we have a valid token
  
    if (hasValidSession) {
       const role = this.tokenService.getRole();
       return this.getRoleRedirect(role);
    }
    

    
      // ❌ No valid session → allow access to login page
      return true;
    

    // ❌ User is NOT logged in. Allow access to Login page.
   
  }

   private getRoleRedirect(role: string | null): UrlTree {
    if (!role) {
      // Fallback if no role found
      console.log("Fall back called");
      return this.router.createUrlTree(['/apps/dashboard']);
    }

    switch (role) {
      case 'Consumer': {
        const userDataString = this.secureStorage.getLoggedInUserData();
        const companyCountStr = this.secureStorage.getCompanyCount();
        if (userDataString && companyCountStr) {
          try {
            const userData = JSON.parse(userDataString);
            const companyCount = companyCountStr;

            if (companyCount >= 0 && userData.firstName && userData.lastName) {
               // Same logic as in AppComponent
               return this.router.createUrlTree(['/apps/dashboard']);
             } else {
              return this.router.createUrlTree(['/apps/account-settings']);
            }
          } catch {
            // If parsing fails, fall back
           return this.router.createUrlTree(['/apps/dashboard']);
          }
       }

        // If sessionStorage not available (cross-tab etc.), safe fallback
        return this.router.createUrlTree(['/apps/dashboard']);
      }

      case 'Admin':
        return this.router.createUrlTree(['/apps/admin']);

      case 'SuperFiler':
        return this.router.createUrlTree(['/apps/Files']);

      case 'Vendor':
        return this.router.createUrlTree(['/apps/consumer']);

      default:
        return this.router.createUrlTree(['/apps/dashboard']);
    }
  }
}