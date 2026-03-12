import { Injectable } from '@angular/core';
import { TaskFilters } from 'src/app/models/todo-models/todo.model';
import { SecureStorageService } from '../storage/secure-storage.service';

/**
 * Defines the view type for assigned tasks.
 */
type AssignedView = "For Me" | "By Me";

// Keys for localStorage persistence
const FILTERS_KEY = 'todo_filters';
const ASSIGNED_VIEW_KEY = 'todo_assigned_view';
const ACTIVE_TAB_KEY = 'todo_active_tab_type'; // 'personal' | 'assigned'

/**
 * This service manages persisting and retrieving non-URL state
 * (like filters and tab preferences) from localStorage
 * to ensure state retention across browser reloads.
 */
@Injectable({
  providedIn: 'root'
})
export class TodoStateService {

  constructor( private secureStorage : SecureStorageService,) { }

  /**
   * Safely saves a value to localStorage.
   * @param key The key to save under.
   * @param value The value to save.
   */
  private setItem(key: string, value: any): void {
    try {
      this.secureStorage.setItem(key, JSON.stringify(value),'session');
      //localStorage.setItem(key, JSON.stringify(value));
    } catch (e) {
      console.error('Error saving to Storage', e);
    }
  }

  /**
   * Safely retrieves and parses a value from localStorage.
   * @param key The key to retrieve.
   * @returns The parsed value or null if not found or on error.
   */
  private getItem<T>(key: string): T | null {
    try {

      const item = this.secureStorage.getItem<string>(key,'session');
      //const item = localStorage.getItem(key);
      return item ? JSON.parse(item) : null;
    } catch (e) {
      console.error('Error getting from Storage', e);
      return null;
    }
  }

  // --- Public State Management Methods ---

  public saveFilters(filters: TaskFilters): void {
    this.setItem(FILTERS_KEY, filters);
  }

  public getFilters(defaultFilters: TaskFilters): TaskFilters {
    return this.getItem<TaskFilters>(FILTERS_KEY) || defaultFilters;
  }

  public saveAssignedView(view: AssignedView): void {
    this.setItem(ASSIGNED_VIEW_KEY, view);
  }

  public getAssignedView(defaultView: AssignedView): AssignedView {
    return this.getItem<AssignedView>(ASSIGNED_VIEW_KEY) || defaultView;
  }

  public saveActiveTab(tab: 'personal' | 'assigned'): void {
    this.setItem(ACTIVE_TAB_KEY, tab);
  }

  public getActiveTab(defaultTab: 'personal' | 'assigned'): 'personal' | 'assigned' {
    return this.getItem<'personal' | 'assigned'>(ACTIVE_TAB_KEY) || defaultTab;
  }

  /**
   * Clears all To-Do related state from localStorage.
   */
  public clearTaskState(): void {
    this.secureStorage.removeItem(FILTERS_KEY,'session');
    this.secureStorage.removeItem(ASSIGNED_VIEW_KEY,'session');
    this.secureStorage.removeItem(ACTIVE_TAB_KEY,'session');
    //  localStorage.removeItem(FILTERS_KEY);
    //  localStorage.removeItem(ASSIGNED_VIEW_KEY);
    //  localStorage.removeItem(ACTIVE_TAB_KEY);
  }
}