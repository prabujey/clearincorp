import { Injectable } from "@angular/core";
import {
  HttpInterceptor,
  HttpRequest,
  HttpHandler,
  HttpEvent,
  HttpErrorResponse,
} from "@angular/common/http";
import { Observable, throwError } from "rxjs";
import { catchError } from "rxjs/operators";
import { Router } from "@angular/router";

@Injectable()
export class ErrorInterceptor implements HttpInterceptor {
  private redirecting = false;
  constructor(private router: Router) {}

  intercept(
    req: HttpRequest<any>,
    next: HttpHandler
  ): Observable<HttpEvent<any>> {
    return next.handle(req).pipe(
      catchError((error: HttpErrorResponse) => {
        const onErrorPage = this.router.url.startsWith("/error");
        if (onErrorPage || this.redirecting) {
          return throwError(() => error);
        }

        if (!navigator.onLine || error.status === 0) {
          this.gotoError("network");
        }

        // Case 2: Client Side  Error
        // else if (error.status >= 400 && error.status < 500) {
        //   console.error("Client Side Error");
        //   this.router.navigate(["/error"], {
        //     queryParams: { type: "client" },
        //   });
        // }

        // Case 3: Internal Server Error (500 / 505)
        else if (error.status >= 500) {
          this.gotoError("server");
        }

        return throwError(() => error);
      })
    );
  }

  private gotoError(type: "network" | "server") {
    this.redirecting = true;

    this.router
      .navigate(["/error"], {
        queryParams: { type },
        replaceUrl: true,
      })
      .finally(() => setTimeout(() => (this.redirecting = false), 300));
  }
}
