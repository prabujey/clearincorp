import { Injectable } from '@angular/core';
import {
  CanActivate,
  ActivatedRouteSnapshot,
  RouterStateSnapshot,
  Router,
  UrlTree,
} from '@angular/router';
import { Observable } from 'rxjs';
import { TokenService } from 'src/app/services/token/token.service';
import { SessionFlagsService } from '../services/login/session-flags.service';
import { SecureStorageService } from '../services/storage/secure-storage.service';

@Injectable({
  providedIn: 'root',
})
export class AuthGuard implements CanActivate {
  
  constructor(private router: Router, private tokenService: TokenService,  private sessionFlags: SessionFlagsService,private secureStorage : SecureStorageService,) {}

  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): Observable<boolean | UrlTree> | Promise<boolean | UrlTree> | boolean | UrlTree {

    const session = this.sessionFlags.getFlags();

    const isSessionValid =
      !!session &&
      session.isLoggedIn === true &&
      typeof session.sessionExpiryMs === 'number' &&
      Date.now() <= session.sessionExpiryMs;
    
    const isUserContextValid = this.checkLoginStatus();
    
    if (isSessionValid && isUserContextValid) {
      // User is logged in; grant access to the route
      console.log('User is logged in and emails match.');
      return true;    
    } 
  if (!isSessionValid) {
        this.sessionFlags.clear();
        this.tokenService.clearToken();
        console.log("Authguard Expiry called " ,session?.sessionExpiryMs);
    }
      console.log("Authguard called ");
      
      return this.router.createUrlTree(['/authentication/login']);
    
  }

 
  
  private checkLoginStatus(): boolean {
    const userId = this.secureStorage.getLoginUserId();
    const localEmail = localStorage.getItem('email') || this.secureStorage.getLoggedInUserEmail();
    const tokenEmail = this.tokenService.getEmail();
    if (!userId || !tokenEmail || !localEmail) {
    //   console.warn('AuthGuard: Missing Data', { userId, tokenEmail, localEmail });
      return false;
    }

    // 2. Check consistency (Case Insensitive)
    if (tokenEmail.toLowerCase() !== localEmail.toLowerCase()) {
    //   console.warn(`AuthGuard: Email mismatch. Token: ${tokenEmail}, LS: ${localEmail}`);
      return false;
    }

    return true;
  }
}
