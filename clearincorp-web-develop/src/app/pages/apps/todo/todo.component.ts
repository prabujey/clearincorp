import { Component, ViewChild, signal } from "@angular/core";
import { CommonModule } from "@angular/common";
// UPDATED: Import RouterModule for <router-outlet>
import { RouterModule, RouterOutlet } from "@angular/router";
import { MatToolbarModule } from "@angular/material/toolbar";
import { MatButtonModule } from "@angular/material/button";
import { MatIconModule } from "@angular/material/icon";
import { MatSelectModule } from "@angular/material/select";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatCard, MatCardContent, MatCardModule } from "@angular/material/card";
import { MatTooltipModule } from "@angular/material/tooltip";
import { MatDialog, MatDialogModule } from "@angular/material/dialog";

// REMOVED: Service and child component imports are no longer needed here
// They are managed by the router.

// UPDATED: Panel modes are no longer managed here
// export type PanelMode = 'list' | 'create' | 'edit' | 'view-assignee' | 'view-owner' | 'filter';

@Component({
  selector: "app-root",
  standalone: true,
  imports: [
    CommonModule,
    // UPDATED: Add RouterModule
    RouterModule,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatSelectModule,
    MatFormFieldModule,
    MatCardModule,
    MatTooltipModule,
    MatDialogModule,
    // REMOVED: All child component imports (TaskList, TaskCreateEdit, etc.)
  ],
  // UPDATED: Template is now a router outlet container
  template: `
     <div class="app-container">
      <main class="main-content-area" [class.side-panel-active]="(panelOutlet.isActivated)">
        <!-- 
          The main router outlet renders:
          - TaskListComponent
          - TaskCreateEditComponent
          - TaskAnalyticsPanelComponent
        -->
        <router-outlet></router-outlet>
      </main>

      <!-- 
        The named 'panel' outlet renders:
        - TaskFilterPanelComponent
        This allows it to overlay the main content.
      -->
      <div 
        class="detail-panel mat-elevation-z8" 
        [class.panel-open]="(panelOutlet.isActivated)"
      >
        <router-outlet name="panel" #panelOutlet="outlet"></router-outlet>
      </div>
    </div>
  `,
  // UPDATED: Styles are now for the new layout
  styles: [`
    .app-container {
        min-height: 100vh;
        background: transparent;
        padding: 0 0 24px 0;    
        position: relative; 
    }
    .main-content-area {
        max-width: 1400px;
        margin: 0 auto;
        transition: filter 0.3s ease;
    }
    
    .detail-panel {
        display: flex;
        flex-direction: column;
        position: fixed; 
        top: 0;
        right: 0;
        width: 450px;
        max-width: 100vw;
        height: 100vh;
        padding: 0;
        background: #ffffff;
        overflow: auto;
        z-index: 100;
        transform: translateX(100%);
        transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    }
    .detail-panel.panel-open {
        transform: translateX(0);
    }
    
    /* When panel is open, blur the main content on non-mobile */
    @media (min-width: 768px) {
      .main-content-area.side-panel-active {
          filter: blur(4px);
          pointer-events: none;
      }
    }
  `]
})
export class AppTodoComponent {
   @ViewChild('panelOutlet', { read: RouterOutlet }) panelOutlet?: RouterOutlet;
  // This component is now a simple layout shell.
  // All logic has been moved into the routed child components.
  // We only need the RouterOutlet object to check if the panel is active.
  constructor() {}
}