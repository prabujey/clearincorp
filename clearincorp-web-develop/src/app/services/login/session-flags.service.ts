import { Injectable, OnDestroy } from "@angular/core";
import { CookieService } from "ngx-cookie-service";
import * as CryptoJS from "crypto-js";
import { BehaviorSubject, Observable } from "rxjs";
import { SecureStorageService } from "../storage/secure-storage.service";
import { environment } from "src/environments/environment";

export interface SessionFlags {
  keepMeSignedIn: boolean;
  isLoggedIn: boolean;
  sessionExpiryMs: number; // This will store the 1 day + now timestamp
}

@Injectable({ providedIn: "root" })
export class SessionFlagsService implements OnDestroy {
  private readonly cookieName = "cic_state";
  private readonly secret = environment.cryptoSecretKey; // NOTE: In production, consider environment variables
  private readonly broadcastChannelName = "cic_session_sync_channel";

  private readonly SESSION_DURATION_MS = 86100000; 

  // ---- Tab counting ----
  private readonly tabsKey = "cic_tabs_v1";
  private readonly heartbeatMs = 2000; // touch every 2s
  private readonly staleAfterMs = 12000; // consider tab dead after 12s
  private heartbeatHandle?: number;
  private closeHandled = false;

  private readonly tabId: string = this.initTabId();

  // 1. Reactive State: Components can subscribe to this to know login status changes instantly
  private sessionSubject = new BehaviorSubject<SessionFlags | null>(null);
  public readonly sessionState$: Observable<SessionFlags | null> =
    this.sessionSubject.asObservable();

  // 2. Broadcast Channel for Cross-Tab Communication
  private channel: BroadcastChannel;

  constructor(
    private cookies: CookieService,
    private secureStorage: SecureStorageService
  ) {
    const existingTabId = sessionStorage.getItem("cic_tab_id");
    const isReload = !!existingTabId;
    sessionStorage.setItem("cic_tab_id", this.tabId);
    this.touchTab();
    //this.secureStorage.setItem('cic_tab_id', this.tabId , "session");
    const aliveBefore = this.getAliveTabCountInternal() > 1;
    const initialFlags = this.getFlagsFromCookie();
    this.sessionSubject.next(initialFlags);

     if (initialFlags && !initialFlags.keepMeSignedIn && !aliveBefore && !isReload) {
      this.deleteCookieOnly();
      this.sessionSubject.next(null);
    } else {
      this.sessionSubject.next(initialFlags);
    }
   
    this.heartbeatHandle = window.setInterval(() => {
      this.touchTab();
      this.checkActiveExpiry();
    }, this.heartbeatMs);
    window.addEventListener("pagehide", this.handleTabClose, true);
    window.addEventListener("beforeunload", this.handleTabClose, true);

    // Initialize the BroadcastChannel to listen for changes from other tabs
    if (typeof BroadcastChannel !== "undefined") {
      this.channel = new BroadcastChannel(this.broadcastChannelName);
      this.channel.onmessage = (event) => {
        if (event.data === "SESSION_UPDATED") {
          const updatedFlags = this.getFlagsFromCookie();
          this.sessionSubject.next(updatedFlags);
        }
      };
    }
  }

    extendSession(): void {
    const currentFlags = this.sessionSubject.value;
    // We only extend if we are currently logged in
    if (!currentFlags || !currentFlags.isLoggedIn) return;

    const newExpiry = Date.now() + this.SESSION_DURATION_MS;
    
    const newFlags: SessionFlags = {
      ...currentFlags,
      sessionExpiryMs: newExpiry
    };

    this.saveFlagsToCookie(newFlags);
    console.log('Session extended to:', new Date(newExpiry).toLocaleTimeString());
  }

  private checkActiveExpiry() {
    const currentFlags = this.sessionSubject.value;

    // Only check if we think we are logged in
    if (
      currentFlags &&
      currentFlags.isLoggedIn &&
      currentFlags.sessionExpiryMs
    ) {
      const now = Date.now();
      if (now > currentFlags.sessionExpiryMs) {
        console.warn("Session active timer expired. Clearing session.");
        this.clear(); // This broadcasts the change, triggering dialogs in all tabs
      }
    }
  }

  private initTabId(): string {
    const existing = sessionStorage.getItem("cic_tab_id");
    //const existing = this.secureStorage.getItem<string>('cic_tab_id', 'session');
    if (existing) return existing;
    const id =
      crypto?.randomUUID?.() ?? `tab_${Math.random().toString(16).slice(2)}`;

    sessionStorage.setItem("cic_tab_id", id);
    // this.secureStorage.setItem('cic_tab_id', id, 'session'); // ✅ persist per-tab
    return id;
  }

  setFlags(keepMeSignedIn: boolean, isLoggedIn: boolean): void {
    // 3. Logic: Calculate 1 Day + Now
   // const twoMinutesInMs = 2 * 60 * 1000; // 2 min * 60 sec * 1000 ms
    const oneDayInMs = this.SESSION_DURATION_MS;
    const expiryTimestamp = Date.now() + oneDayInMs;

    const flags: SessionFlags = {
      keepMeSignedIn,
      isLoggedIn,
      sessionExpiryMs: expiryTimestamp,
    };
    this.saveFlagsToCookie(flags);
    
  }

    private saveFlagsToCookie(flags: SessionFlags): void {
    const payload = JSON.stringify(flags);
    const encrypted = CryptoJS.AES.encrypt(payload, this.secret).toString();

    const secure = location.protocol === 'https:';
    const sameSite: 'Lax' | 'Strict' | 'None' = 'Lax';

    const baseOptions = {
      path: '/',
      secure,
      sameSite
    } as const;

    if (flags.keepMeSignedIn) {
      // Physical cookie lasts 30 days if "Remember Me"
      const cookieExpiry = new Date(Date.now() + 30 * 24 * 60 * 60 * 1000); 
      this.cookies.set(this.cookieName, encrypted, {
        ...baseOptions,
        expires: cookieExpiry
      });
    } else {
      // Session cookie (clears on browser close)
      this.cookies.set(this.cookieName, encrypted, baseOptions);
    }

    // Update local state and broadcast
    this.sessionSubject.next(flags);
    this.channel?.postMessage('SESSION_UPDATED');
  }

  /**
   * Retrieves and decrypts flags. Returns null if invalid or missing.
   */
  getFlags(): SessionFlags | null {
    return this.sessionSubject.value;
  }

  getAliveTabCount(): number {
    return this.getAliveTabCountInternal();
  }

  isOnlyAliveTab(): boolean {
    return this.getAliveTabCount() === 1;
  }

  /**
   * Internal helper to read directly from cookie
   */
  private getFlagsFromCookie(): SessionFlags | null {
    const encrypted = this.cookies.get(this.cookieName);
    if (!encrypted) return null;

    try {
      const bytes = CryptoJS.AES.decrypt(encrypted, this.secret);
      const json = bytes.toString(CryptoJS.enc.Utf8);

      if (!json) {
        this.deleteCookieOnly();
        return null;
      }

      const flags = JSON.parse(json) as SessionFlags;

      // Optional: Check if the internal "1 day" timer has expired
      if (!flags.sessionExpiryMs || Date.now() > flags.sessionExpiryMs) {
        this.deleteCookieOnly();
        return null;
      }

      return flags;
    } catch (e) {
      console.error("Error parsing session cookie", e);
      this.deleteCookieOnly();
      return null;
    }
  }

  private deleteCookieOnly(): void {
    this.cookies.delete(this.cookieName, "/");
  }

  clear(): void {
    //this.cookies.delete(this.cookieName, '/');
    this.deleteCookieOnly();
    this.sessionSubject.next(null);
    this.channel.postMessage("SESSION_UPDATED");
  }

  // ngOnDestroy(): void {
  //   if (this.channel) {
  //     this.channel.close();
  //   }
  // }

  private touchTab(): void {
    const map = this.cleanupStale(this.readTabs());
    map[this.tabId] = Date.now();
    this.writeTabs(map);
  }
  private readTabs(): Record<string, number> {
    try {
      return JSON.parse(localStorage.getItem(this.tabsKey) || "{}");
      //  return JSON.parse(this.secureStorage.getItem<string>(this.tabsKey, 'local')|| '{}');
    } catch {
      return {};
    }
  }

  private writeTabs(map: Record<string, number>) {
    localStorage.setItem(this.tabsKey, JSON.stringify(map));
    // this.secureStorage.setItem(this.tabsKey, JSON.stringify(map),'local');
  }

  private cleanupStale(map: Record<string, number>) {
    const now = Date.now();
    for (const [id, ts] of Object.entries(map)) {
      if (now - ts > this.staleAfterMs) delete map[id];
    }
    return map;
  }

  private getAliveTabCountInternal(): number {
    const map = this.cleanupStale(this.readTabs());
    this.writeTabs(map); // persist cleanup
    return Object.keys(map).length;
  }

  private unregisterTab(): void {
    const map = this.cleanupStale(this.readTabs());
    delete map[this.tabId];
    this.writeTabs(map);
  }

  private handleTabClose = () => {
    if (this.closeHandled) return; // prevent double-run (pagehide + beforeunload)
    this.closeHandled = true;

    //const flags = this.getFlagsFromCookie(); // read cookie directly
   // const isLastTab = this.isOnlyAliveTab(); // ✅ CALL IT HERE (before unregister)

    this.unregisterTab();

    // // ✅ last-tab logout rule
    // if (isLastTab && flags && !flags.keepMeSignedIn) {
    //   this.deleteCookieOnly(); // removes cic_state so next open forces login
    // }
  };

  ngOnDestroy(): void {
    if (this.heartbeatHandle) clearInterval(this.heartbeatHandle);
    window.removeEventListener("pagehide", this.handleTabClose, true);
    window.removeEventListener("beforeunload", this.handleTabClose, true);
    this.channel?.close();
  }
}
