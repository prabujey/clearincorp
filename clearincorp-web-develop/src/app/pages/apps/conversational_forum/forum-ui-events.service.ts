import { Injectable } from '@angular/core';
import { Observable, Subject ,BehaviorSubject } from 'rxjs';
import { TitleDto } from 'src/app/services/forum.service';

@Injectable({ providedIn: 'root' })
export class ForumUiEventsService {
  private readonly _openCreate = new Subject<void>();
  private readonly _closeCreate = new Subject<void>();
  private readonly _topicCreated = new Subject<TitleDto>();
  private readonly _openNotifications = new Subject<void>();
private readonly _isSidebarDisabled = new BehaviorSubject<boolean>(false);
  readonly openCreate$: Observable<void> = this._openCreate.asObservable();
  readonly closeCreate$: Observable<void> = this._closeCreate.asObservable();
  readonly topicCreated$: Observable<TitleDto> = this._topicCreated.asObservable();
  readonly openNotifications$: Observable<void> = this._openNotifications.asObservable();
// ✅ NEW: Observable for the sidebar component to subscribe to
  readonly isSidebarDisabled$: Observable<boolean> = this._isSidebarDisabled.asObservable();
  requestOpenCreate(): void {
    this.setSidebarDisabled(true);
    this._openCreate.next();
  }

  requestCloseCreate(): void {
    // When creation is manually closed, re-enable the sidebar
    
    this._closeCreate.next();
  }

  publishTopicCreated(topic: TitleDto): void {
    // When a topic is successfully created, re-enable the sidebar
    
    this._topicCreated.next(topic);
  }

  requestOpenNotifications(): void {
    this._openNotifications.next();
  }
  setSidebarDisabled(state: boolean): void {
    this._isSidebarDisabled.next(state);
  }
}
