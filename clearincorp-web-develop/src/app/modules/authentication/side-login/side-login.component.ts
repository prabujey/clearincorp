import { Component, inject, OnInit, OnDestroy } from "@angular/core";
import { CommonModule } from "@angular/common";
import {
  FormsModule,
  ReactiveFormsModule,
  FormControl,
  FormGroupDirective,
  NgForm,
} from "@angular/forms";
import { Router, RouterModule } from "@angular/router";
import { FormBuilder, FormGroup, Validators } from "@angular/forms";
import { createSideLoginForm } from "src/app/models/side-login";

import {
  HttpClientModule,
  HttpClient,
  HttpHeaders,
} from "@angular/common/http";
import { finalize, switchMap, catchError } from "rxjs/operators";
import { Observable, of } from "rxjs";
import { ServiceLogin } from "src/app/services/login/service-login";
import { UserProgress } from "src/app/models/side-login";
import { MaterialModule } from "src/app/material.module";
import { CoreService } from "src/app/services/core.service";
import { SnackBarService } from "src/app/shared/snackbar.service";
import { AccountSettingService } from "src/app/services/apps/account-settings/account-settings.service";
import { UserModel } from "src/app/models/account-settings";
import { AppComponent } from "src/app/app.component";
import { TokenService } from "src/app/services/token/token.service";
import { NgxMaskDirective, NgxMaskPipe } from "ngx-mask";
import { ErrorStateMatcher } from "@angular/material/core";
import { UserService } from "src/app/shared/userService";
import { LoadingService } from "src/app/services/loading/loading.service";
import { SessionFlagsService } from "src/app/services/login/session-flags.service";
import { SecureStorageService } from "src/app/services/storage/secure-storage.service";
import { NgOtpInputModule } from 'ng-otp-input';

export class TouchedOnlyErrorStateMatcher implements ErrorStateMatcher {
  isErrorState(
    control: FormControl | null,
    form: FormGroupDirective | NgForm | null
  ): boolean {
    return !!(control && control.invalid && (control.touched || control.dirty));
  }
}
@Component({
  selector: "app-side-login",
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    RouterModule,
    HttpClientModule,
    MaterialModule,
   // NgxMaskDirective,
    NgOtpInputModule,
    // NgxMaskPipe,
  ],
  templateUrl: "./side-login.component.html",
  styleUrls: ["./side-login.component.scss"],
  providers: [
    { provide: ErrorStateMatcher, useClass: TouchedOnlyErrorStateMatcher },
  ],
})
export class AppSideLoginComponent implements OnInit, OnDestroy {
  form: FormGroup;
  otpSent = false;
  otpInvalid = false;
  message = "";
  loading = false;
  error = false;
  otpexpired = false;
  options: any;
  reload = false;
  showotp = false;
  errorStateMatcher = new TouchedOnlyErrorStateMatcher();
  // Add with your other fields
  private readonly OTP_EXPIRY_KEY = "otpExpiryTs";
  private visibilityHandler = () => this.updateTimeRemainingFromClock();

  // Timer properties
  timeRemaining = 300; // 5 minutes in seconds
  timerActive = false;
  private timerInterval: any;

  // Validation state tracking
  private validationMessages = {
    email: {
      required: "Email address is required",
      email: "Please enter a valid email address",
      pattern: "Please enter a valid email address",
    },
    otp: {
      required: "Verification code is required",
      minlength: "Please enter a valid 6-digit verification code",
    },
  };

  appComponent = inject(AppComponent);

  private readonly EMAIL_KEY = "rememberedEmail";

  otpConfig = {
    allowNumbersOnly: true,
    length: 6,
    isPasswordInput: false,
    disableAutoFocus: false,
    placeholder: '',
    inputStyles: {
      'width': '40px',
      'height': '40px',
      'border-radius': '8px',
      'border': '1px solid #c4c4c4',
      'font-size': '1.2rem',
      'font-weight': '600'
    },
    containerStyles: {
      'gap': '6px' ,
      'display': 'flex',
      'justify-content': 'space-between',
      'margin': '10px 0'
    }
  };

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private snackBarService: SnackBarService,
    private loginService: ServiceLogin,
    private settings: CoreService,
    private accountSettingService: AccountSettingService,
    private tokenService: TokenService,
    private userService: UserService,
    private loadingService: LoadingService,
    private sessionFlags: SessionFlagsService,
    private secureStorage: SecureStorageService,
    private http: HttpClient
  ) {
    this.form = createSideLoginForm(this.fb);

    // Add real-time validation
    this.setupRealTimeValidation();
  }

  ngOnInit() {
    sessionStorage.removeItem(this.OTP_EXPIRY_KEY);
    this.options = this.settings.getOptions();
    const saved = localStorage.getItem(this.EMAIL_KEY);
    if (saved) {
      this.form.patchValue({ email: saved, rememberMe: true });
    }

    // ⏱️ Resume timer if an expiry was previously set
    const savedExpiryStr = sessionStorage.getItem(this.OTP_EXPIRY_KEY);
    if (savedExpiryStr) {
      const expiry = parseInt(savedExpiryStr, 10);
      const now = Date.now();
      if (expiry > now) {
        this.otpSent = true;
        this.showotp = true;
        this.otpexpired = false;
        this.startTimer(expiry); // resume using existing expiry
      } else {
        // already expired
        this.onTimerExpired();
      }
    }

    //
  }

  ngOnDestroy() {
    this.clearTimer();
  }

  private setupRealTimeValidation() {
    // Email validation
    this.f["email"].valueChanges.subscribe(() => {
      if (this.f["email"].touched) {
        this.validateEmailField();
      }
    });

    // OTP validation
    this.f["otp"].valueChanges.subscribe(() => {
      if (this.f["otp"].value && this.f["otp"].value.length > 0) {
        this.validateOtpField();
      }
    });
  }

  private validateEmailField() {
    const emailControl = this.f["email"];
    if (emailControl.invalid && emailControl.touched) {
      // Email validation handled by template
      return false;
    }
    return true;
  }

  private validateOtpField() {
    const otpControl = this.f["otp"];
    if (otpControl.value && otpControl.value.length === 6) {
      this.otpInvalid = false;
      return true;
    } else if (
      otpControl.value &&
      otpControl.value.length > 0 &&
      otpControl.value.length < 6
    ) {
      this.otpInvalid = true;
      return false;
    }
    return false;
  }

  get f() {
    return this.form.controls as { [key: string]: any };
  }

  private get normalizedEmail(): string {
    return this.f["email"].value.trim().toLowerCase();
  }

  // Timer methods
  // Timer now driven by a fixed expiry timestamp
  private startTimer(existingExpiryTs?: number) {
    // 1) Clear any existing interval FIRST
    this.clearTimer();

    // 2) Compute/persist expiry
    const expiry = existingExpiryTs ?? Date.now() + 300 * 1000; // 5 minutes
    //this.secureStorage.setItem(this.OTP_EXPIRY_KEY, String(expiry),'session');
    sessionStorage.setItem(this.OTP_EXPIRY_KEY, String(expiry));

    // 3) Prime the display
    this.updateTimeRemainingFromClock();

    // 4) Now mark active and start ticking
    this.timerActive = true;
    this.timerInterval = setInterval(() => {
      this.updateTimeRemainingFromClock();
    }, 1000);
  }

  private updateTimeRemainingFromClock() {
    const expiryStr = sessionStorage.getItem(this.OTP_EXPIRY_KEY);
    if (!expiryStr) return;

    const expiry = parseInt(expiryStr, 10);
    const now = Date.now();
    const diffMs = Math.max(0, expiry - now);
    this.timeRemaining = Math.floor(diffMs / 1000);

    if (diffMs <= 0) {
      this.onTimerExpired();
    }
  }

  private clearTimer() {
    if (this.timerInterval) {
      clearInterval(this.timerInterval);
      this.timerInterval = null;
    }
    this.timerActive = false;
  }

  private onTimerExpired() {
    this.clearTimer();
    sessionStorage.removeItem(this.OTP_EXPIRY_KEY);
    this.otpexpired = true;
    this.error = true;
    this.message = "OTP expired. Please request a new one.";
  }

  formatTime(seconds: number): string {
    const minutes = Math.floor(seconds / 60);
    const remainingSeconds = seconds % 60;
    return `${minutes.toString().padStart(2, "0")}:${remainingSeconds
      .toString()
      .padStart(2, "0")}`;
  }

  sendOtp() {
    if (this.f["email"].invalid || this.loading) return;

    // Mark email as touched for validation display
    this.f["email"].markAsTouched();

    if (!this.validateEmailField()) {
      this.error = true;
      this.message = "Please enter a valid email address";
      return;
    }

    this.reload = true;
    this.loading = true;
    this.message = "";
    const email = this.normalizedEmail;
   // this.otpSent = true;

    this.loginService
      .sendOtp(email)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: (res) => {
          localStorage.clear();
          sessionStorage.clear();
          this.message = res.message || "OTP sent successfully.";
          this.reload = false;
          this.error = false;
          this.showotp = true;
          if (
            res.message?.includes("User is deleted. Please contact support.")
          ) {
            this.showotp = false;
            this.otpSent = false;
          } else {
            this.showotp = true;
            this.otpSent = true;
          }
          this.otpexpired = false;
          this.otpInvalid = false;
          this.f["otp"].setValue("");
          localStorage.setItem("email", this.f["email"].value.toLowerCase());
          this.handleRememberMe();

          // Start the countdown timer
          this.startTimer();
        },
        error: (err) => {
          this.error = true;
          this.message =
            err.status === 400
              ? err.error?.message || "❌ Invalid email."
              : "❌ Something went wrong.";
          this.otpSent = false;
          this.showotp = false;
        },
      });
  }

  validateOtp() {
    if (!this.f["otp"].value || this.f["otp"].value.length < 6) {
      this.otpInvalid = true;
      this.error = true;
      this.message = "Please enter a valid 6-digit verification code";
      return;
    }

    this.otpInvalid = false;
    this.loading = true;
    this.message = "";
    const email = this.normalizedEmail;
    const token = this.f["otp"].value;
    const keepMeSignedIn = this.f["rememberMe"]?.value || false;

    this.loginService
      .validateOtp(email, token)
      // .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: (res) => {
          this.message = res.message;

          if (res.message.includes("OTP expired")) {
            this.error = true;
            this.otpexpired = true;
            this.otpInvalid = true;
            this.clearTimer();
            this.loading = false;
          } else if (res.message.includes("successfully")) {
            this.error = false;
            this.sessionFlags.setFlags(keepMeSignedIn, true);
            const finishLogin = (finalImageUrl: string) => {
              this.clearTimer();
              sessionStorage.removeItem(this.OTP_EXPIRY_KEY);
              const userData: UserModel = {
                firstName: res.firstName,
                lastName: res.lastName,
                email: res.email || localStorage.getItem("email") || "",
                phoneNumber: res.phoneNumber,
                profileImageUrl: finalImageUrl, // Store the Base64 string here
              };

              this.secureStorage.setLoggedInUser(
                res.loginUserId,
                userData.email,
                JSON.stringify(userData)
              );
              try {
                this.secureStorage.setLoggedInUserData(
                  JSON.stringify(userData)
                );
              } catch (e) {
                console.warn(
                  "Session Storage is full, image not persisted to session.",
                  e
                );
                userData.profileImageUrl = "";
                this.secureStorage.setLoggedInUserData(
                  JSON.stringify(userData)
                );
                userData.profileImageUrl = finalImageUrl; // Restore for memory
              }

              this.userService.updateUser(userData);
              this.tokenService.saveToken(res.accessToken);
              this.tokenService.saveToken(res.idToken, "id");

              const user_role = this.tokenService.getRole() ?? "";
              this.navigateBasedOnRole(user_role, res.companyCount);

              this.snackBarService.show("Logged in successfully!", "Close", {
                duration: 3000,
              });
              this.otpInvalid = false;
              // this.loading = false;
            };

            // If we have a URL, download it. If it's empty or already data, just proceed.
            if (res.profileImageUrl && res.profileImageUrl.startsWith("http")) {
              this.downloadAndConvertImage(res.profileImageUrl).subscribe(
                (base64) => {
                  finishLogin(base64);
                  console.log("downloded");
                }
              );
            } else {
              finishLogin(res.profileImageUrl || "");
            }
          } else {
            this.error = true;
            this.otpInvalid = true;
            if (!this.message) {
              this.message = "❌ Incorrect OTP.";
            }
            this.loading = false;
          }
        },
        error: (err) => {
          this.error = true;
          this.otpInvalid = true;
          this.message = err.error?.message || "❌ Incorrect OTP";
          this.loading = false;
        },
      });
  }
  private downloadAndConvertImage(url: string): Observable<string> {
    const headers = new HttpHeaders().set("X-Skip-Loading", "true");

    return this.http.get(url, { responseType: "blob", headers }).pipe(
      switchMap((blob) => {
        return new Observable<string>((observer) => {
          const reader = new FileReader();
          reader.onloadend = () => {
            observer.next(reader.result as string); // This is the Base64 string
            observer.complete();
          };
          reader.onerror = () => {
            // If conversion fails, return original url (better than nothing)
            observer.next(url);
            observer.complete();
          };
          reader.readAsDataURL(blob);
        });
      }),
      catchError((err) => {
        console.error("Failed to download profile image:", err);
        // If download fails (e.g. CORS or 404), return the original URL so the UI can try to show it anyway
        return of(url);
      })
    );
  }

  onButtonClick() {
    if (this.otpSent && this.otpexpired) {
      this.sendOtp();
    } else if (this.otpSent && !this.otpexpired) {
      this.validateOtp();
    } else {
      this.sendOtp();
    }
  }

  canProceed(): boolean {
    if (this.otpSent && !this.otpexpired) {
      return (
        this.f["otp"] && this.f["otp"].value?.length === 6 && !this.otpInvalid
      );
    } else {
      return this.f["email"] && this.f["email"].valid;
    }
  }

  get buttonText(): string {
    if (this.otpSent && this.otpexpired) return "Resend OTP";
    if (this.otpSent) return "Login";
    return "Send OTP";
  }

  resetEmail(event: Event) {
    event.stopPropagation();
    event.preventDefault();
    this.f["email"].setValue("");
    this.f["otp"].setValue("");
    this.f["email"].markAsUntouched();
    this.f["otp"].markAsUntouched();
    this.form.patchValue({ rememberMe: false });
    localStorage.removeItem(this.EMAIL_KEY);
    this.otpSent = false;
    this.showotp = false;
    this.otpInvalid = false;
    this.loading = false;
    this.message = "";
    this.error = false;
    this.otpexpired = false;

    // Clear timer when resetting
    this.clearTimer();
    sessionStorage.removeItem(this.OTP_EXPIRY_KEY);
    
  }

  onOtpChange(otp: string) {
    this.f['otp'].setValue(otp);
    this.otpInvalid = false;
    if (this.error && !this.otpexpired) {
      this.error = false;
      this.message = "";
    }
    // Optional: Trigger validation immediately if length is 6
    // if (otp.length === 6) {
    //   this.validateOtpField();
    // }
  }

  editEmail(event: Event) {
    event.stopPropagation();
    event.preventDefault();    
    // Clear only OTP related data
    this.f["otp"].setValue(""); 
    this.f["otp"].markAsUntouched();
    this.otpSent = false;
    this.showotp = false;
    this.otpInvalid = false;
    this.loading = false;
    this.message = "";
    this.error = false;
    this.otpexpired = false;

    // Clear timer
    this.clearTimer();
    sessionStorage.removeItem(this.OTP_EXPIRY_KEY);
  }

  handleRememberMe() {
    this.f["rememberMe"].value
      ? localStorage.setItem(this.EMAIL_KEY, this.f["email"].value)
      : localStorage.removeItem(this.EMAIL_KEY);
  }

  navigateBasedOnRole(role: string, companyCount: number) {
    switch (role) {
      case "Consumer":
        const userData = JSON.parse(this.secureStorage.getLoggedInUserData());
        this.secureStorage.setCompanyCount(companyCount);
        if (
          companyCount >= 0 &&
          userData.firstName !== null &&
          userData.firstName !== undefined &&
          userData.lastName !== null &&
          userData.lastName !== undefined
        ) {
          this.router.navigate(["/apps/dashboard"]);
        } else {
          this.router.navigate(["/apps/account-settings"]);
        }

        break;
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
        this.router.navigate(["/authentication/login"]);
    }
  }

  getButtonIcon(): string {
    if (this.otpSent && this.otpexpired) return "refresh";
    if (this.otpSent) return "login";
    return "send";
  }

  // Helper method to get validation message
  getValidationMessage(field: "email" | "otp", errorType: string): string {
    const fieldMessages = this.validationMessages[field];
    if (fieldMessages && errorType in fieldMessages) {
      return fieldMessages[errorType as keyof typeof fieldMessages];
    }
    return "Invalid input";
  }
}
