// src/app/pages/apps/todo/todo.routes.ts

import { Routes } from '@angular/router';
import { AppTodoComponent } from './todo.component';
import { TaskListComponent } from './task-list/task-list.component';
import { TaskCreateEditComponent } from './task-create-edit/task-create-edit.component';
import { TaskAnalyticsPanelComponent } from './task-analytics/task-analytics-panel.component';

export const TODO_ROUTES: Routes = [
  {
    path: '',
    component: AppTodoComponent,
    children: [
      // Default: Assigned list
      {
        path: '',
        redirectTo: 'list/assigned',
        pathMatch: 'full',
      },

      // List view
      // `:type` = 'assigned' | 'personal'
      {
        path: 'list/:type',
        component: TaskListComponent,
      },

      // Create new task
      {
        path: 'create',
        component: TaskCreateEditComponent,
        data: { mode: 'create' },
      },

      // Edit by ID
      {
        path: 'edit/:id',
        component: TaskCreateEditComponent,
        data: { mode: 'edit' },
      },

      // View as assignee
      {
        path: 'view/:id',
        component: TaskCreateEditComponent,
        data: { mode: 'view-assignee' },
      },

      // Analytics by ID
      {
        path: 'analytics/:id',
        component: TaskAnalyticsPanelComponent,
        // if you rely on mode inside, optionally:
        // data: { mode: 'analytics' },
      },

      // Filter side panel (named outlet)
      // URL example:
      // /apps/todo/(list/assigned//panel:filter)
      // {
      //   path: 'filter',
      //   component: TaskFilterPanelComponent,
      //   outlet: 'panel',
      // },
    ],
  },
];
