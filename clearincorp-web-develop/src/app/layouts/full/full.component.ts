import { BreakpointObserver, MediaMatcher } from "@angular/cdk/layout";
import { Component, OnInit, ViewChild, ViewEncapsulation } from "@angular/core";
import { Subscription } from "rxjs";
import { MatSidenav, MatSidenavContent } from "@angular/material/sidenav";
import { CoreService } from "src/app/services/core.service";
import { AppSettings } from "src/app/config";
import { filter } from "rxjs/operators";
import { NavigationEnd, Router } from "@angular/router";
import { navItems } from "./vertical/sidebar/sidebar-data";
import { NavService } from "../../services/nav.service";
import { AppNavItemComponent } from "./vertical/sidebar/nav-item/nav-item.component";
import { RouterModule } from "@angular/router";
import { MaterialModule } from "src/app/material.module";
import { CommonModule } from "@angular/common";
import { SidebarComponent } from "./vertical/sidebar/sidebar.component";
import { NgScrollbarModule } from "ngx-scrollbar";
import { NgIcon } from "@ng-icons/core";
import { HeaderComponent } from "./vertical/header/header.component";
import { AppHorizontalHeaderComponent } from "./horizontal/header/header.component";
import { AppHorizontalSidebarComponent } from "./horizontal/sidebar/sidebar.component";
import { AppBreadcrumbComponent } from "./shared/breadcrumb/breadcrumb.component";
import { CustomizerComponent } from "./shared/customizer/customizer.component";
import { UserModel } from "src/app/models/account-settings";
import { TokenService } from "src/app/services/token/token.service";
import { ServiceLogin } from "src/app/services/login/service-login";
import { SnackBarService } from "src/app/shared/snackbar.service";
import { UserService } from "src/app/shared/userService";
import { SecureStorageService } from "src/app/services/storage/secure-storage.service";

const MOBILE_VIEW = "screen and (max-width: 768px)";
const TABLET_VIEW = "screen and (min-width: 769px) and (max-width: 1024px)";
const MONITOR_VIEW = "screen and (min-width: 1024px)";
const BELOWMONITOR = "screen and (max-width: 1023px)";

// for mobile app sidebar
interface apps {
  id: number;
  img: string;
  title: string;
  subtitle: string;
  link: string;
}

interface quicklinks {
  id: number;
  title: string;
  link: string;
}

@Component({
  selector: "app-full",
  imports: [
    RouterModule,
    AppNavItemComponent,
    MaterialModule,
    CommonModule,
    SidebarComponent,
    NgScrollbarModule,
    NgIcon,
    HeaderComponent,
    AppHorizontalHeaderComponent,
    AppHorizontalSidebarComponent,
    AppBreadcrumbComponent,
    // CustomizerComponent,
  ],
  templateUrl: "./full.component.html",
  styles: [
    `
      $us-red: #bf0a30;
      $us-blue: #002868;
      $us-white: #ffffff;

      $us-flag-gradient: linear-gradient(
        165deg,
        rgba(198, 3, 46, 0.95) 0%,
        rgba(218, 109, 133, 0.85) 0%,
        rgba(255, 255, 255, 0.9) 40%,
        rgba(74, 130, 219, 0.9) 110%,
        #002868 100%
      );

      .us-flag-glow {
        background-color: #ebf3fb;
        border-radius: 999px;
        padding: 2px;
        display: inline-flex;
        align-items: center;
        justify-content: center;
      }

      .us-flag-glow-inner {
        background: #020617;
        border-radius: inherit;
        padding: 6px 10px;
        display: inline-flex;
        align-items: center;
        justify-content: center;
      }
    `,
  ],
  encapsulation: ViewEncapsulation.None,
})
export class FullComponent implements OnInit {
  navItems = navItems;
  role: string = "";
  user: UserModel;
  defaultAvatarUrl: string = "./assets/avatar-0.jpg";
  // avatarOptions = [
  //   { id: 1, url: "/assets/avatar-1.jpg" },
  //   { id: 2, url: "/assets/avatar-2.jpg" },
  //   { id: 3, url: "/assets/avatar-3.jpg" },
  //   { id: 4, url: "/assets/avatar-4.jpg" },
  //   { id: 5, url: "/assets/avatar-5.jpg" },
  //   { id: 6, url: "/assets/avatar-6.jpg" },
  // ];

  @ViewChild("leftsidenav")
  public sidenav: MatSidenav;
  resView = false;
  @ViewChild("content", { static: true }) content!: MatSidenavContent;
  //get options from service
  options = this.settings.getOptions();
  private layoutChangesSubscription = Subscription.EMPTY;
  private isMobileScreen = false;
  private isContentWidthFixed = true;
  private isCollapsedWidthFixed = false;
  private htmlElement!: HTMLHtmlElement;

  get isOver(): boolean {
    return this.isMobileScreen;
  }

  get isTablet(): boolean {
    return this.resView;
  }

  // for mobile app sidebar
  apps: apps[] = [
    {
      id: 1,
      img: "./assets/images/svgs/icon-dd-chat.svg",
      title: "Chat Application",
      subtitle: "Messages & Emails",
      link: "/apps/chat",
    },
    {
      id: 2,
      img: "/images/svgs/icon-dd-cart.svg",
      title: "eCommerce App",
      subtitle: "Buy a Product",
      link: "/apps/email/inbox",
    },
    {
      id: 3,
      img: "./assets/images/svgs/icon-dd-invoice.svg",
      title: "Invoice App",
      subtitle: "Get latest invoice",
      link: "/apps/invoice",
    },
    {
      id: 4,
      img: "./assets/images/svgs/icon-dd-date.svg",
      title: "Calendar App",
      subtitle: "Get Dates",
      link: "/apps/calendar",
    },
    {
      id: 5,
      img: "./assets/images/svgs/icon-dd-mobile.svg",
      title: "Contact Application",
      subtitle: "2 Unsaved Contacts",
      link: "/apps/contacts",
    },
    {
      id: 6,
      img: "./assets/images/svgs/icon-dd-lifebuoy.svg",
      title: "Tickets App",
      subtitle: "Create new ticket",
      link: "/apps/tickets",
    },
    {
      id: 7,
      img: "./assets/images/svgs/icon-dd-message-box.svg",
      title: "Email App",
      subtitle: "Get new emails",
      link: "/apps/email/inbox",
    },
    {
      id: 8,
      img: "./assets/images/svgs/icon-dd-application.svg",
      title: "Courses",
      subtitle: "Create new course",
      link: "/apps/courses",
    },
  ];

  quicklinks: quicklinks[] = [
    {
      id: 1,
      title: "Pricing Page",
      link: "/theme-pages/pricing",
    },
    {
      id: 2,
      title: "Authentication Design",
      link: "/authentication/side-login",
    },
    {
      id: 3,
      title: "Register Now",
      link: "/authentication/side-register",
    },
    {
      id: 4,
      title: "404 Error Page",
      link: "/authentication/error",
    },
    {
      id: 5,
      title: "Notes App",
      link: "/apps/notes",
    },
    {
      id: 6,
      title: "Employee App",
      link: "/apps/employee",
    },
    {
      id: 7,
      title: "Todo Application",
      link: "/apps/todo",
    },
    {
      id: 8,
      title: "Treeview",
      link: "/theme-pages/treeview",
    },
  ];

  constructor(
    private settings: CoreService,
    private mediaMatcher: MediaMatcher,
    private router: Router,
    private breakpointObserver: BreakpointObserver,
    private navService: NavService,
    private tokenService: TokenService,
    private userService: UserService,
    private loginService: ServiceLogin,
    private SnackBarService: SnackBarService,
    private secureStorage: SecureStorageService,
  ) {
    this.htmlElement = document.querySelector("html")!;
    this.layoutChangesSubscription = this.breakpointObserver
      .observe([MOBILE_VIEW, TABLET_VIEW, MONITOR_VIEW, BELOWMONITOR])
      .subscribe((state) => {
        // SidenavOpened must be reset true when layout changes
        this.options.sidenavOpened = true;
        this.isMobileScreen = state.breakpoints[BELOWMONITOR];
        if (this.options.sidenavCollapsed == false) {
          this.options.sidenavCollapsed = state.breakpoints[TABLET_VIEW];
        }
        this.isContentWidthFixed = state.breakpoints[MONITOR_VIEW];
        this.resView = state.breakpoints[BELOWMONITOR];
      });

    // Initialize project theme with options
    this.receiveOptions(this.options);

    // This is for scroll to top
    this.router.events
      .pipe(filter((event) => event instanceof NavigationEnd))
      .subscribe((e) => {
        this.content.scrollTo({ top: 0 });
      });
  }

  ngOnInit(): void {
    const storedRole = this.tokenService.getRole();
    if (storedRole) {
      this.role = storedRole;
    }
    // const stored = sessionStorage.getItem('userData');
    // if (stored) {
    //   const userData: UserModel = JSON.parse(stored);
    //   this.user = userData;
    //    // pick the matching URL (or fall back to default)
    // const match = this.avatarOptions.find(opt => opt.id === userData.avatarId);
    // this.avatarUrl = match?.url ?? '/assets/avatar-0.jpg';
    // }
    this.userService.user$.subscribe((userData) => {
      if (userData) {
        this.user = userData;
      }
    });
  }
  getProfileImageUrl(): string {
    // Priority: 1. Uploaded profile image, 2. Selected avatar, 3. Default avatar
    if (this.user?.profileImageUrl) {
      return this.user.profileImageUrl;
    }

    return this.defaultAvatarUrl;
  }

  onProfileImageError(event: Event): void {
    // If the profile image fails to load, fall back to default avatar
    const img = event.target as HTMLImageElement;
    img.src = this.defaultAvatarUrl;
  }

  ngOnDestroy() {
    this.layoutChangesSubscription.unsubscribe();
  }

  toggleCollapsed() {
    this.isContentWidthFixed = false;
    this.options.sidenavCollapsed = !this.options.sidenavCollapsed;
    this.resetCollapsedState();
  }

  resetCollapsedState(timer = 400) {
    setTimeout(() => this.settings.setOptions(this.options), timer);
  }

  onSidenavClosedStart() {
    this.isContentWidthFixed = false;
  }

  onSidenavOpenedChange(isOpened: boolean) {
    this.isCollapsedWidthFixed = !this.isOver;
    this.options.sidenavOpened = isOpened;
    this.settings.setOptions(this.options);
  }

  receiveOptions(options: AppSettings): void {
    this.options = options;
    this.toggleDarkTheme(options);
  }

  toggleDarkTheme(options: AppSettings) {
    if (options.theme === "dark") {
      this.htmlElement.classList.add("dark-theme");
      this.htmlElement.classList.remove("light-theme");
    } else {
      this.htmlElement.classList.remove("dark-theme");
      this.htmlElement.classList.add("light-theme");
    }
  }

  logout() {
    this.loginService.logout().subscribe({
      next: (response) => {
        console.log("Logout response:", response);
        this.performLocalCleanup();
        this.SnackBarService.showSuccess("Logged out successfully");
      },
      error: (error) => {
        // ⚠️ Handle logout failure
        this.SnackBarService.showError("Logged out (Server unreachable)");
        console.error("Logout error:", error);
        this.performLocalCleanup();
      },
    });
  }

  private performLocalCleanup() {
  this.tokenService.clearToken();
  this.secureStorage.removeItem("lastUrl","session");
  this.loginService.isLoggingOut = false;
  this.router.navigate(["/authentication/login"]);
}
}
