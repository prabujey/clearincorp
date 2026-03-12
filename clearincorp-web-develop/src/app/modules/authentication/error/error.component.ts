import { Component } from "@angular/core";
import { RouterModule, Router } from "@angular/router";
import { ActivatedRoute } from "@angular/router";
import { MaterialModule } from "../../../material.module";
import { CommonModule } from "@angular/common";
import { ServiceLogin } from "src/app/services/login/service-login";
import { finalize } from "rxjs/operators";
import { TokenService } from "src/app/services/token/token.service";
import { SecureStorageService } from "src/app/services/storage/secure-storage.service";
import { SnackBarService } from "src/app/shared/snackbar.service";

@Component({
  selector: "app-error",
  standalone: true,
  imports: [RouterModule, MaterialModule, CommonModule],
  templateUrl: "./error.component.html",
  styles: [
    `
      .error-img {
        width: 600px;
        height: 600px;
        border-radius: 50%;
        object-fit: cover;
        display: block;
        margin: 0 auto 16px; /* centers image */
        border: 3px solid #e26b85;
        background: #002868;
      }
      .me-2 { margin-right: 8px; }

      /* Optional: responsive circle for smaller screens */
      @media (max-width: 768px) {
        .error-img {
          width: 280px;
          height: 280px;
        }
      }
    `,
  ],
})
export class AppErrorComponent {
  errorType: string | null = null;

  constructor(private route: ActivatedRoute, private router: Router , private loginService :ServiceLogin, private tokenService : TokenService,private SnackBarService: SnackBarService,
      private secureStorage: SecureStorageService,) {
    const dataType = this.route.snapshot.data?.["type"];
    this.route.queryParams.subscribe((params) => {
      this.errorType = params["type"] || dataType || "unknown";
    });
    // Prevent back navigation to this error page
    // history.pushState(null, "", location.href);
    // window.onpopstate = () => {
    //   history.go(1);
    // };
  }

   handleReturnToLogin() {
   
      this.loginService.logout().subscribe({
      next: (response) => {
        console.log("Logout response:", response);
        this.performLocalCleanup();
        this.SnackBarService.showSuccess("Logged out successfully");
      },
      error: (error) => {
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
