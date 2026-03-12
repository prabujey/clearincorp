



// conversational_forum/forum-main.component.ts (Corrected)
// conversational_forum/forum-main.component.ts (Corrected)
import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
// FIX 1: Import map and BehaviorSubject
import { forkJoin, of, Observable, Subscription, BehaviorSubject } from 'rxjs'; 
import { catchError, finalize, map ,delay} from 'rxjs/operators'; 

import {
  ForumService,
  TitleDto,
  PostDto,
  Page,
  ForumTopicDto,
  UserModel,
  UserProfileService,
} from 'src/app/services/forum.service';

import { TitleWithPostsVM } from './topic-detail/topic-detail.component';
import { TopicItem } from 'src/app/models/forum.model';

import { ForumHeaderComponent } from './forum-header/forum-header.component'; 
import { SearchBarComponent } from './search-bar/search-bar.component';
import { CategoryFilterComponent } from './category-filter/category-filter.component';
import { TopicsListComponent } from './topics-list/topics-list.component';
import { TopicDetailComponent } from './topic-detail/topic-detail.component';
import { CreateTopicModalComponent } from './create-topic-modal/create-topic-modal.component';
import { ForumUiEventsService } from './forum-ui-events.service';
import { WsService, WsEvent } from 'src/app/services/ws.service';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';




type UserLiteVM = {
  loginUserId: number;
  fullName: string;
  profileImageUrl?: string;
};

@Component({
  selector: 'app-forum-main',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    ForumHeaderComponent,
    SearchBarComponent,
    CategoryFilterComponent,
    TopicsListComponent,
    TopicDetailComponent,
    CreateTopicModalComponent,
    
  ],
  template: `
    <mat-card class="cardWithShadow">
      <mat-card-content class="p-24">
        <div class="forum-container">
          <app-forum-header></app-forum-header>

          <main class="main-content">
            <div class="container">

              <ng-container *ngIf="!selectedTopicDetail; else detailTpl">
                <div class="topics-view">

                  <app-search-bar (search)="onSearch($any($event))"></app-search-bar>

                  <app-category-filter
                    (categorySelected)="onCategorySelected($any($event))">
                  </app-category-filter>

                  <div *ngIf="isLoading" class="loading-box">
                    Loading topics...
                  </div>

                  <app-topics-list
                    [class.hidden-while-loading]="isLoading"
                    [topics]="(displayedTopicsForList$ | async) ?? []"
                    [activeCategory]="activeCategoryForList"
                    [activeTopicId]="activeTopicId"
                    [usersByLoginId]="usersByLoginId"
                    (select)="onTopicSelected($any($event))">
                  </app-topics-list>

                  <div *ngIf="!isLoading && !(displayedTopicsForList$ | async)?.length" class="empty-box">
                    No topics found.
                  </div>

                </div>
              </ng-container>

              <ng-template #detailTpl>
                <div class="detail-view">

                  <div *ngIf="isLoading" class="loading-box">
                    Loading topic...
                  </div>

                  <app-topic-detail
                    *ngIf="!isLoading"
                    [topic]="selectedTopicDetail!"
                    [usersByLoginId]="usersByLoginId"
                    (back)="onBack()"
                    (replySubmitted)="onReplySubmitted($any($event))">
                  </app-topic-detail>

                </div>
              </ng-template>

            </div>
          </main>

         

          

        </div>
      </mat-card-content>
    </mat-card>
  `,
  styles: [`
    .forum-container { min-height: 100vh; background: #f9fafb; display: flex; flex-direction: column; }
    .main-content { flex: 1; padding: 2rem 0; }
    .container { max-width: 1400px; margin: 0 auto; padding: 0 1.5rem; }
    .topics-view, .detail-view { display: flex; flex-direction: column; gap: 1.5rem; }
    /* UX FIX: Make content unclickable when modal is open */
    .main-content.modal-open {
      pointer-events: none;
      user-select: none;
      opacity: 0.8; /* Subtle visual cue */
    }
    .loading-box, .empty-box { 
      padding: 2rem; 
      text-align: center;
      background: white;
      border-radius: 8px;
      border: 1px solid #e5e7eb;
    }
      :host ::ng-deep .cdk-overlay-container {
  z-index: 200000 !important; /* ✅ above any sidebar/header */
}

:host ::ng-deep .cdk-overlay-backdrop.forum-create-topic-backdrop {
  z-index: 200000 !important;
  pointer-events: auto !important; /* ✅ ensure it captures clicks */
}

:host ::ng-deep .cdk-overlay-pane.forum-create-topic-dialog {
  z-index: 200001 !important;
}
  :host ::ng-deep .cic-forum-dialog-backdrop{
  background: rgba(0,0,0,.55);
  backdrop-filter: blur(2px);
}

:host ::ng-deep .cic-forum-dialog-panel .mat-mdc-dialog-container{
  padding: 0;
  border-radius: 16px;
  overflow: hidden;
}

  
    @media (max-width: 640px) {
      .main-content { padding: 1.5rem 0; }
      .container { padding: 0 1rem; }
      .topics-view, .detail-view { gap: 1rem; }
    }
  `]
})
export class ForumMainComponent implements OnDestroy , OnInit {

  // FIX 2: Use local BehaviorSubject to manage list data
  public displayedTopicsForList$ = new BehaviorSubject<TopicItem[]>([]);

  selectedTopicDetail: TitleWithPostsVM | null = null;

  showCreateModal = false;
  showNotifications = false;
  isLoading = false;

  usersByLoginId: Record<number, UserLiteVM> = {};
  private subs = new Subscription();
  private currentCategory: string | null = 'All Topics';
  private currentSearch: string | null = null;

  private topics: ForumTopicDto[] = [];
  private selectedTopicId: number | null = null; 

  activeTopicId: number | string | null = null;

  private currentWsDestination: string | null = null;
  private profileLoadInFlight = new Set<number>();
private readonly dialog = inject(MatDialog);

  private readonly forum = inject(ForumService);
  private readonly ws = inject(WsService);
  private readonly GLOBAL_TITLE_LIST_DESTINATION = '/topic/titles'; // Assuming a simple global topic name
  private readonly userProfile = inject(UserProfileService);
  private readonly ui = inject(ForumUiEventsService);

  // BehaviorSubject to hold raw titles for internal use (e.g., profile preloading)
  private readonly rawTitles$ = new BehaviorSubject<TitleDto[]>([]);

  constructor() {
    this.subs.add(this.rawTitles$.subscribe((titles: TitleDto[]) => {
      // this.preloadProfilesForTitles(titles);
      // Update the final stream for the list component
      this.displayedTopicsForList$.next(this.toTopicItems(titles));
    }));
  }

  ngOnInit(): void {
    this.ws.connect();

    // FIX 3: Use correct ForumService method, type safe subscription
    this.forum.getAllTopicIdsAndNames().subscribe((ts: ForumTopicDto[]) => (this.topics = ts || [])); 
    this.loadTitles(0);
    this.subs.add(
    this.ws.subscribe(this.GLOBAL_TITLE_LIST_DESTINATION, (event: WsEvent<any>) => {
      if (event.type === 'NEW_TOPIC' || event.payload?.titleId) {
      // ✅ Update the list instantly without setting isLoading = true
      this.handleNewTitleCreated(event.payload);
    }
  })
);
    this.subs.add(this.ui.openCreate$.subscribe(() => this.openCreateDialog()));
    this.subs.add(this.ui.closeCreate$.subscribe(() => { this.showCreateModal = false; }));
    this.subs.add(this.ui.openNotifications$.subscribe(() => { this.showNotifications = true; }));
    this.subs.add(this.ui.topicCreated$.subscribe((_created: TitleDto) => {
      this.showCreateModal = false;
      if (_created) this.handleNewTitleCreated(_created);
    }));
    this.forum.getAllTopicIdsAndNames().subscribe((ts: ForumTopicDto[]) => (this.topics = ts || [])); 
  this.loadTitles(0);
  }

  ngOnDestroy(): void {
    this.subs.unsubscribe();
    this.rawTitles$.complete(); 
    this.displayedTopicsForList$.complete(); 
    this.ws.unsubscribeFromTopic(this.GLOBAL_TITLE_LIST_DESTINATION);
    if (this.currentWsDestination) {
      this.ws.unsubscribeFromTopic(this.currentWsDestination);
      this.currentWsDestination = null;
    }
    this.ws.disconnect();
  }

  private openCreateDialog(): void {
  const ref = this.dialog.open(CreateTopicModalComponent, {
    id: 'forum-create-topic',
    width: '720px',
    maxWidth: '92vw',
    hasBackdrop: true,              // ✅ must
    disableClose: false,
    panelClass: 'forum-create-topic-dialog',
    backdropClass: 'forum-create-topic-backdrop',
    autoFocus: false,
    restoreFocus: false,
    
  });

  this.subs.add(
    ref.afterClosed().subscribe((created: TitleDto | null) => {
      
      setTimeout(() => {
          this.ui.setSidebarDisabled(false);
      }, 0);
      if (created) {
        this.handleNewTitleCreated(created);
      }
    })
  );
}

private handleNewTitleCreated(newTitleDto: TitleDto): void {
    // 1. Get current list of raw DTOs
    const currentTitles = this.rawTitles$.getValue();
    const exists = currentTitles.find(t => t.titleId === newTitleDto.titleId);
    if (exists) return;
    // 2. Prepend the new DTO to the list
    const updatedTitles = [newTitleDto, ...currentTitles];
    
    // 3. Update the source BehaviorSubject. 
    // This instantly updates the list view without triggering the 'Loading...' screen.
    this.rawTitles$.next(updatedTitles);
}

  /* ================== TIME FORMAT (omitted for brevity) ================== */
  private formatClock(d: Date): string { return ''; }
  private formatDate(d: Date): string { return ''; }
  getTimeAgo(value?: string | Date | null): string { return ''; }
  getDateLabel(value?: string | Date | null): string { return ''; }
  
  /* ================== profile cache (omitted for brevity) ================== */
  private normalizeProfileImageUrl(v?: string | null): string | undefined {
    if (!v) return undefined;
    if (v.startsWith('data:image') || v.startsWith('http') || v.startsWith('/') || v.startsWith('assets/')) return v;
    return `data:image/png;base64,${v}`;
  }

  private upsertUser(loginUserId: number, fullName?: string, profileImageUrl?: string | null): void {
    if (!loginUserId) return;
    const current = this.usersByLoginId[loginUserId];
    const normalized = this.normalizeProfileImageUrl(profileImageUrl);

    this.usersByLoginId[loginUserId] = {
      loginUserId,
      fullName: fullName || current?.fullName || 'Unknown',
      profileImageUrl: normalized ?? current?.profileImageUrl
    };
  }

  // private preloadProfilesForTitles(items: TitleDto[]): void {
  //   const ids = Array.from(
  //     new Set(
  //       (items || [])
  //         .map(x => Number((x as any).createdByLoginId ?? (x as any).createdById))
  //         .filter(Boolean)
  //     )
  //   );

  //   const missing = ids.filter(id => {
  //     const hasImg = !!this.usersByLoginId[id]?.profileImageUrl;
  //     const inFlight = this.profileLoadInFlight.has(id);
  //     return !hasImg && !inFlight;
  //   });

  //   if (!missing.length) return;

  //   missing.forEach(id => this.profileLoadInFlight.add(id));

  //   forkJoin(
  //     missing.map(id =>
  //       this.userProfile.getUserByLoginUserId(id).pipe(
  //         catchError(() => of(null as any as UserModel))
  //       )
  //     )
  //   ).pipe(
  //     finalize(() => missing.forEach(id => this.profileLoadInFlight.delete(id)))
  //   ).subscribe((users) => {
  //     for (const u of users) {
  //       if (!u) continue;
  //       const loginUserId = Number((u as any).loginUserId ?? u.loginUserId);
  //       if (!loginUserId) continue;
  //       const fullName =
  //         [u.firstName, u.lastName].filter(Boolean).join(' ') ||
  //         this.usersByLoginId[loginUserId]?.fullName ||
  //         'Unknown';
  //       this.upsertUser(loginUserId, fullName, u.profileImageUrl);
  //     }
  //     // Since an avatar was updated, push the existing raw titles again to re-run toTopicItems
  //     const currentTitles = this.rawTitles$.getValue();
  //     this.displayedTopicsForList$.next(this.toTopicItems(currentTitles));
  //   });
  // }
  
  /* ================== loading ================== */

  private loadTitles(page = 0): void {
    this.isLoading = true;
    
    // listTitles expects number | undefined or number | null for topicId
    const topicId = this.currentCategory === 'All Topics' ? undefined : this.selectedTopicId ?? undefined;
    
    if (this.currentSearch && this.currentSearch.trim()) {
      // Use search method (which returns PostDto[])
      this.forum.search(this.currentSearch.trim(), page, 20).pipe( 
          finalize(() => (this.isLoading = false))
      ).subscribe({
          next: (results: PostDto[]) => { 
              // Map PostDto[] (search results) to TitleDto[] (TopicItem source)
              const titles: TitleDto[] = results.map(r => r as any as TitleDto); 
              this.rawTitles$.next(titles); 
          },
          error: () => {
             this.rawTitles$.next([]);
             this.isLoading = false;
          }
      });
    } else {
        // FIX 4: listTitles signature is (topicId?: number, page, size)
        this.forum.listTitles(topicId, page, 20).pipe(
          finalize(() => (this.isLoading = false))
        ).subscribe({
          next: (res) => {
             this.rawTitles$.next(res.content || []);
          },
          error: () => {
             this.rawTitles$.next([]);
          }
        });
    }
  }

  /* ================== mapping (omitted for brevity) ================== */
  private toVM(res: TitleDto & { posts?: Page<PostDto> | PostDto[] | null | undefined }): TitleWithPostsVM {
    const pageOrArray = res.posts as Page<PostDto> | PostDto[] | null | undefined;
    const postsArray: PostDto[] = Array.isArray(pageOrArray) ? pageOrArray : (pageOrArray?.content ?? []);
    const { posts: _ignored, ...rest } = res as any;

    return {
      ...(rest as Omit<TitleDto, 'posts'>),
      postsPage: Array.isArray(pageOrArray) ? null : (pageOrArray ?? null),
      posts: postsArray || []
    };
  }

  private pickCreatedAt(t: any): string | Date | null {
    return t?.createdAt ?? t?.createdOn ?? t?.createdDate ?? t?.created_at ?? t?.createdDateTime ?? t?.createdTimestamp ?? null;
  }

  private toTopicItems(list: TitleDto[]): TopicItem[] {
    return (list || []).map((t: any) => {
      const loginId = Number(t?.createdByLoginId ?? t?.createdById) || 0;
      const name = t?.createdByName ?? 'Unknown';
      const profileImageUrl = this.usersByLoginId[loginId]?.profileImageUrl;

      const createdAt = this.pickCreatedAt(t);
      const activity = t?.lastActivityAt ?? createdAt;

      return {
          topicId: t?.topicId ?? 0,
          titleId: t?.titleId ?? 0,
          title: t?.title ?? '',
          descriptionMd: t?.descriptionMd ?? '',
          category: this.topics.find((tc: ForumTopicDto) => tc.topicId === t.topicId)?.topicName ?? 'General',

          isPinned: t?.isPinned ?? t?.pinned ?? false,
          lastActivity: activity,
          replyCount: t?.replyCount ?? 0,
          viewCount: t?.viewCount ?? 0,

          createdByName: name,
          createdByLoginId: loginId,

          author: { name: name, avatar: profileImageUrl || 'assets/avatar-0.jpg' },

          __raw: t,
      } as TopicItem; 
    });
  }


  /* ================== filters/events (omitted for brevity) ================== */

  private resolveTopicIdByName(name?: string | null): number | null {
    if (!name) return null;
    const t = this.topics.find(x => x.topicName.trim().toLowerCase() === name.trim().toLowerCase());
    return t?.topicId ?? null;
  }

  onSearch(query: string): void {
    this.currentSearch = query;
    this.currentCategory = 'All Topics'; 
    this.selectedTopicId = null;
    this.loadTitles(0);
  }

  onCategorySelected(category: string | null): void {
    if (category === 'All Topics' || category === null) {
      this.selectedTopicId = null;
      this.currentCategory = 'All Topics';
    } else {
      this.selectedTopicId = this.resolveTopicIdByName(category);
      this.currentCategory = category;
    }
    this.currentSearch = null; 
    this.loadTitles(0);
  }

  get activeCategoryForList(): string | null {
    return this.currentCategory;
  }

  onCloseNotifications(): void {
    this.showNotifications = false;
  }

  /* ================== detail view (omitted for brevity) ================== */
  
  private subscribeToTitleWs(titleId: number) {
    if (this.currentWsDestination) {
      this.ws.unsubscribeFromTopic(this.currentWsDestination);
      this.currentWsDestination = null;
    }

    const destination = `/topic/title.${titleId}`;
    this.currentWsDestination = destination;

    this.ws.subscribe(destination, (_event: WsEvent<any>) => {
      if (this.selectedTopicDetail && Number((this.selectedTopicDetail as any).titleId) === titleId) {
        this.refreshCurrentTitle(false);
      }
    });
  }

  // private refreshCurrentTitle() {
  //   if (!this.selectedTopicDetail) return;
  //   const id = Number((this.selectedTopicDetail as any).titleId);
  //   if (!id) return;

  //   this.forum.getTitleWithPosts(id, 0, 10).subscribe(res => {
  //     this.selectedTopicDetail = this.toVM(res as any as TitleDto);
  //   });
  // }


  private refreshCurrentTitle(showLoading: boolean = true) {
  if (!this.selectedTopicDetail) return;
  const id = Number((this.selectedTopicDetail as any).titleId);
  if (!id) return;
  
  // 🟢 FIX: Only set isLoading = true if explicitly requested
  if (showLoading) {
      this.isLoading = true; 
  }

  this.forum.getTitleWithPosts(id, 0, 10).pipe(
    finalize(() => {
      // 🟢 FIX: Only set isLoading = false if it was set to true here
      if (showLoading) {
          this.isLoading = false;
      }
    })
  ).subscribe(res => {
    this.selectedTopicDetail = this.toVM(res as any as TitleDto);
    // The visual update happens here by replacing the old this.selectedTopicDetail 
    // with the new one from the REST call, instantly refreshing the view content.
  });
}

  onTopicSelected(item: TopicItem): void {
    const raw = (item as any).__raw ? (item as any).__raw : item;
    const titleId = Number((raw as any).titleId);
    if (!titleId) return;

    this.activeTopicId = titleId;
    this.isLoading = true;

    this.forum.getTitleWithPosts(titleId, 0, 10).pipe(
      finalize(() => (this.isLoading = false))
    ).subscribe({
      next: (res) => {
        this.selectedTopicDetail = this.toVM(res as any as TitleDto);
        this.subscribeToTitleWs(titleId);

        if (this.selectedTopicDetail.posts?.[0]) {
          this.forum.viewPost(this.selectedTopicDetail.posts[0].postId).subscribe((_count: number) => {
            this.selectedTopicDetail!.posts[0].viewsCount = _count;
          });
        }
      },
      error: (err) => { console.error("Error fetching topic detail:", err); }
    });
  }

  onBack(): void {
    if (this.currentWsDestination) {
      this.ws.unsubscribeFromTopic(this.currentWsDestination);
      this.currentWsDestination = null;
    }
    this.selectedTopicDetail = null;
    this.loadTitles(0);
  }

  onReplySubmitted(_e: { postId: number }): void {
    this.refreshCurrentTitle(false);
  }

  onTopicCreated(_created: TitleDto): void {
    this.showCreateModal = false;
    this.loadTitles(0);
  }

  onNotificationClicked(topicId: string): void {
    this.showNotifications = false;
    this.forum.listTitles().subscribe(page => {
      const topicItem = this.toTopicItems(page.content).find(t => String(t.titleId) === topicId);
      if (topicItem) {
        this.onTopicSelected(topicItem);
      } else {
        console.warn(`Topic with ID ${topicId} not found in current list.`);
      }
    });
  }
}




 // <app-notifications-panel
          //   *ngIf="showNotifications"
          //   (close)="onCloseNotifications()"
          //   (notificationClicked)="onNotificationClicked($any($event))">
          // </app-notifications-panel>