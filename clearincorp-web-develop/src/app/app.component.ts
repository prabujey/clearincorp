import { Component, effect, HostListener } from "@angular/core";
import { RouterOutlet } from "@angular/router";
import { MatProgressBarModule } from "@angular/material/progress-bar";
import { CommonModule } from "@angular/common";
import { LoadingService } from "./services/loading/loading.service";
import { LottieComponent, AnimationOptions } from "ngx-lottie";
import { Router } from "@angular/router";
import { NavigationError } from "@angular/router";
import { filter } from "rxjs/operators";
import { MaterialModule } from "src/app/material.module";
import { FloatingChatbotComponent } from "./shared/floating-chatbot/floating-chatbot.component";
import { MatDialog } from "@angular/material/dialog";
import { TokenService } from "./services/token/token.service";
import { SessionExpiredDialogComponent } from "./shared/session-expired-dialog.component";
import { ServiceLogin } from "./services/login/service-login";
import { Subscription } from "rxjs";
import { SessionFlagsService } from "./services/login/session-flags.service";
import { SecureStorageService } from "./services/storage/secure-storage.service";
@Component({
  selector: "app-root",
  imports: [
    RouterOutlet,
    MatProgressBarModule,
    CommonModule,
    LottieComponent,
    MaterialModule,
    FloatingChatbotComponent,
  ],
  templateUrl: "./app.component.html",
  styles: [
    `
      .loading-overlay {
        position: fixed;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        background-color: rgba(0, 0, 0, 0.5);
        backdrop-filter: blur(3px);
        z-index: 9999;
        display: flex;
        justify-content: center;
        align-items: center;
      }

      .loading-content {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 16px;
      }

      .spinner-message {
        margin: 0;
        font-size: 16px;
        font-weight: 500;
        color: white;
        text-align: center;
      }

      .loading-bar {
        position: fixed;
        top: 0;
        left: 0;
        width: 100%;
        z-index: 10000;
        height: 4px;
      }

      /* Optional: Blue tint for monochrome horse animations */
      ng-lottie svg path {
        fill: #1e7ae1;
      }
    `,
  ],
})
export class AppComponent {
  title = "Spike Angular Admin Template";
  loading = this.loadingService.loading$;

  // Lottie configuration for realistic horse animation
  lottieOptions: AnimationOptions = {
    path: "assets/animations/loading.json",
    loop: true,
    autoplay: true,
  };

  //private authChannel = new BroadcastChannel('app_auth_channel');
  private authChannel?: BroadcastChannel;
  private sessionSub?: Subscription;

  constructor(
    public loadingService: LoadingService,
    private router: Router,
    private dialog: MatDialog,
    private tokenService: TokenService,
    private loginService: ServiceLogin,
    private sessionFlags: SessionFlagsService,
    private secureStorage: SecureStorageService
  ) {
    // effect(() => {
    //   console.log("Loading state changed:", this.loading());
    // });
  }
  ngOnInit() {
    if (typeof BroadcastChannel !== "undefined") {
      this.authChannel = new BroadcastChannel("app_auth_channel");
    }

    this.setupLogoutListener();
    this.setupSessionSyncListener();

    window.addEventListener("online", () => {
      console.log("✅ Internet Restored");

      // Double-check actual internet connectivity
      const checkInternet = setInterval(() => {
        fetch("https://www.google.com/favicon.ico", {
          method: "HEAD",
          mode: "no-cors",
        })
          .then(() => {
            console.log("✅ Internet Restored");

            const lastUrl = this.secureStorage.getItem<string>(
              "lastUrl",
              "session"
            );
            if (lastUrl) {
              const stepMatch = lastUrl.match(/step=(\d+)/);
              const step = stepMatch ? parseInt(stepMatch[1], 10) : null;

              if (step && [10, 11].includes(step)) {
                this.router.navigateByUrl("/apps/dashboard");
              } else {
                this.router.navigateByUrl(lastUrl);
              }
              this.secureStorage.removeItem("lastUrl", "session");
            }
            clearInterval(checkInternet);
          })
          .catch(() => {
            console.warn("❌ Network adapter up, but no actual internet yet");
          });
      }, 5000); // Check every 5 seconds
    });

    window.addEventListener("offline", () => {
      console.log("❌ Internet Lost");
    });

    this.router.events
      .pipe(filter((e): e is NavigationError => e instanceof NavigationError))
      .subscribe((e) => {
        // avoid loops
        if (this.router.url.startsWith("/error")) return;

        const msg = String(
          (e as any)?.error?.message ?? (e as any)?.error ?? ""
        );
        const isChunkError =
          msg.includes("ChunkLoadError") ||
          msg.includes("Loading chunk") ||
          msg.includes("Failed to fetch dynamically imported module");

        const type = !navigator.onLine || isChunkError ? "network" : "client";

        this.loadingService.hide();
        this.router.navigate(["/error"], {
          queryParams: { type },
          replaceUrl: true,
        });
      });
  }

  ngOnDestroy() {
    // 3. Clean up
    if (this.authChannel) {
      this.authChannel.close();
    }
    this.sessionSub?.unsubscribe();
  }

  private setupSessionSyncListener() {
    this.sessionSub = this.sessionFlags.sessionState$.subscribe((flags) => {
      const onLoginPage =
        this.router.url.includes("/login") ||
        this.router.url.includes("/authentication");

      // CASE 1: User Logged Out (Flags became null or isLoggedIn is false)
      if (!flags || !flags.isLoggedIn) {
        // If we are NOT on the login page and have a token, force a logout redirect
        if (!onLoginPage && this.tokenService.getToken()) {
          this.handleRemoteLogout();
        }
      }

      // CASE 2: User Logged In (Flags are valid)
      // This handles the edge case where a user logs in on Tab A,
      // and Tab B (which was sitting on Login page) should auto-redirect to their specific page.
      if (flags && flags.isLoggedIn && onLoginPage) {
        const role = this.tokenService.getRole();
        this.navigateBasedOnRole(role);
      }
    });
  }

  private navigateBasedOnRole(role: string | null) {
    if (!role) {
      // Fallback if no role found, though unlikely if logged in
      this.router.navigate(["/apps/dashboard"]);
      return;
    }

    switch (role) {
      case "Consumer": {
        const userDataString = this.secureStorage.getLoggedInUserData();
        const companyCountStr = this.secureStorage.getCompanyCount();
        if (userDataString && companyCountStr) {
          try {
            const userData = JSON.parse(userDataString);
            const companyCount = companyCountStr;

            if (companyCount >= 0 && userData.firstName && userData.lastName) {
              this.router.navigate(["/apps/dashboard"]);
            } else {
              this.router.navigate(["/apps/account-settings"]);
            }
          } catch {
            this.router.navigate(["/apps/dashboard"]);
          }
        } else {
          // Safe fallback for cross-tab login where sessionStorage isn't synced
          this.router.navigate(["/apps/dashboard"]);
        }
        break;
      }

      case "Admin":
        this.router.navigate(["/apps/admin"]);
        break;

      case "SuperFiler":
        this.router.navigate(["/apps/Files"]);
        break;

      case "Vendor":
        this.router.navigate(["/apps/consumer"]);
        break;

      default:
        // Default for unknown roles
        this.router.navigate(["/apps/dashboard"]);
        break;
    }
  }

  private setupLogoutListener() {
    if (!this.authChannel) return;

    this.authChannel.onmessage = (event) => {
      // SECURITY: Validate message structure to prevent injection attacks
      if (!event.data || typeof event.data !== 'object') {
        return;
      }

      if (this.loginService.isLoggingOut) {
        return;
      }

      // Validate message type and required fields
      if (event.data.type === "LOGOUT" &&
          typeof event.data.email === 'string' &&
          typeof event.data.role === 'string') {

        const incomingEmail = event.data.email;
        const incomingRole = event.data.role;

        const currentEmail = this.tokenService.getEmail();
        const currentRole = this.tokenService.getRole();

        // LOGIC: Only expire if the logged-out user matches the current tab's user
        if (
          currentEmail &&
          currentRole &&
          incomingEmail === currentEmail &&
          incomingRole === currentRole
        ) {
          this.handleRemoteLogout();
        }
      }
    };
  }

  private handleRemoteLogout() {
    // 1. Stop any loaders

    this.secureStorage.removeItem("lastUrl", "session");
    //localStorage.removeItem("email");
    this.tokenService.clearToken();
    this.loadingService.hide();

    // 2. Open the modal (disableClose forces them to click Login)
    if (this.dialog.openDialogs.length === 0) {
      this.dialog.open(SessionExpiredDialogComponent, {
        panelClass: "custom-dialog-panel",
        disableClose: true,
        width: "450px",
        backdropClass: "custom-ticket-dialog-backdrop",
      });
    }
  }

  @HostListener("document:keydown.enter", ["$event"])
  handleEnter(event: Event) {
    const active = document.activeElement as HTMLElement | null;
    if (!active) return;

    const tag = active.tagName.toLowerCase();

    if (tag === "input" && (active as HTMLInputElement).type !== "textarea") {
      const form = (active as HTMLInputElement).form;
      if (form) {
        event.preventDefault();
        (form as any).requestSubmit?.();
      }
      return;
    }

    if (tag === "button" || tag === "a") {
      event.preventDefault();
      active.click();
    }
  }
  @HostListener("window:offline", [])
  onOffline() {
    if (this.router.url.startsWith("/error")) return;
    const currentUrl = this.router.url;
    this.secureStorage.setItem("lastUrl", currentUrl, "session");
    this.loadingService.hide(); // stop loader
    this.router.navigate(["/error"], {
      queryParams: { type: "network" },
      replaceUrl: true,
    });
  }
}
