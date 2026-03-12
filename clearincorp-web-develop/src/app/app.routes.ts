import { Routes } from "@angular/router";
import { BlankComponent } from "./layouts/blank/blank.component";
import { FullComponent } from "./layouts/full/full.component";

import { LandingPageComponent } from "./LANDING_PAGE/landing-page/landing-page.component";

import { AuthGuard } from "./auth/auth.guard"; // Ensure this path is correct
import { LoginGuard } from "./auth/login.guard";
import { AppErrorComponent } from "./modules/authentication/error/error.component";

export const routes: Routes = [
  // Landing page route (if you want to show the chatbot here as well, add the flag)
  {
    path: "",
    pathMatch: "full",
    redirectTo: "authentication/login",
  },
    {
    path: "error",
    component: BlankComponent,
    children: [{ path: "", component: AppErrorComponent }],
  },

  // Secured main application routes
  {
    path: "",
    component: FullComponent,
    canActivate: [AuthGuard], // Protect all children of FullComponent
    children: [
      {
        path: "dashboard",
        loadChildren: () =>
          import("./modules/modules.routes").then((m) => m.ModulesRoutes),
        // data: { showChatbot: true },  Chatbot will show on the dashboard
      },
      {
        path: "wizard",
        loadChildren: () =>
          import("./pages/formation-wizard/wizard.routes").then(
            (m) => m.WizardRoutes
          ),
        // data: { showChatbot: true },  Chatbot will show on the wizard page
      },
      {
        path: "apps",
        loadChildren: () =>
          import("./pages/apps/apps.routes").then((m) => m.AppsRoutes),
        // data: { showChatbot: true },  Chatbot will show on the apps page
      },
    ],
  },

  // Open authentication routes using a blank layout
  {
    path: "",
    component: BlankComponent,
    children: [
      {
        path: "authentication",
        canActivate: [LoginGuard], 
        loadChildren: () =>
          import("./modules/authentication/authentication.routes").then(
            (m) => m.AuthenticationRoutes
          ),
        //data: { showChatbot: false },  Chatbot will NOT show on authentication pages
      },
    ],
  },

  // Secured profile routes (if applicable, add route data flags as needed)

  // Fallback route redirects to an error page inside the authentication module
  // {
  //   path: "**",
  //   redirectTo: "error",
  // },

    {
    path: "**",
    component: BlankComponent,
    children: [
      { path: "", component: AppErrorComponent, data: { type: "notfound" } },
    ],
  },
];
