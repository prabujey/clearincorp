

import { Component, Input, Output, EventEmitter, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatMenuModule } from '@angular/material/menu';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { finalize } from 'rxjs/operators';
import { MatDialog } from '@angular/material/dialog';
import { ConfirmDialogComponent } from 'src/app/shared/confirm-dialog.component';
import { EditContentDialogComponent } from 'src/app/shared/edit-content-dialog.component';
import { ForumService, PostDto, ReplyDto, TitleDto, Page } from 'src/app/services/forum.service';
import { WsService } from 'src/app/services/ws.service';
import { SecureStorageService } from 'src/app/services/storage/secure-storage.service';

export type TitleWithPostsVM = Omit<TitleDto, 'posts'> & {
  postsPage?: Page<PostDto> | null;
  posts: PostDto[];
};

type ReplyNode = ReplyDto & { children?: ReplyNode[] | null; isLiked?: boolean };

@Component({
  selector: 'app-topic-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, MatMenuModule, MatIconModule, MatButtonModule, MatTooltipModule],
  templateUrl: './topic-detail.component.html',
  styleUrls: ['./topic-detail.component.scss'],
})
export class TopicDetailComponent implements OnInit, OnDestroy {
  private _topic!: TitleWithPostsVM;
  private readonly ADMIN_ID = 1;
  private readonly EDIT_WINDOW_MINUTES = 5;
  
  @Input()
  set topic(value: TitleWithPostsVM) { 
    this._topic = value;
    if (this._topic) this.setupWebSocket();
  }
  get topic(): TitleWithPostsVM { return this._topic; }

  @Output() back = new EventEmitter<void>();
  @Output() replySubmitted = new EventEmitter<{ postId: number }>();

  @Input() usersByLoginId: Record<number, any> = {};

  newPostText = '';
  inlineReplyText = '';
  replyingToPostId: number | null = null;
  replyingToReplyId: number | null = null;
  isBusy = false;
  
  private openRepliesPosts = new Set<number>();
  private openChildThreads = new Set<number>();
  private currentWsDest = '';

  constructor(
    private forum: ForumService, 
    private ws: WsService, 
    private dialog: MatDialog,
    private secureStorage: SecureStorageService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.setupWebSocket();
  }

  ngOnDestroy(): void {
    if (this.currentWsDest) {
      this.ws.unsubscribeFromTopic(this.currentWsDest);
    }
  }

  /**
   * Checks if the current user owns the item and if it's within the 5-minute window.
   * Admins bypass the time restriction.
   */
  // canModify(item: any): boolean {
  //   const currentUserId = this.getLoginUserId();
  //   const ownerId = item.loginUserId || item.createdByLoginId || (item === this.topic ? (this.topic as any).createdByLoginId : null);
    
  //   const isOwner = Number(ownerId) === currentUserId;
  //   const isAdmin = currentUserId === this.ADMIN_ID;

  //   if (isAdmin) return isOwner;
  //   if (!isOwner) return false;

  //   // Time Check: 5 Minutes
  //   const createdAt = item.createdAt || (item === this.topic ? (this.topic as any).createdAt : null);
  //   if (!createdAt) return false;

  //   const diffMs = Date.now() - new Date(createdAt).getTime();
  //   const diffMinutes = diffMs / (1000 * 60);

  //   return diffMinutes < this.EDIT_WINDOW_MINUTES;
  // }

  canModify(item: any, action: 'edit' | 'delete'): boolean {
  const currentUserId = Number(this.getLoginUserId());

  const ownerId = Number(
    item?.loginUserId ??
    item?.createdByLoginId ??
    (item === this.topic ? (this.topic as any)?.createdByLoginId : null)
  );

  const isOwner = ownerId === currentUserId;
  const isAdmin = currentUserId === Number(this.ADMIN_ID);

  // ✅ Admin rules
  if (isAdmin) {
    if (action === 'delete') return true;   // delete all
    return isOwner;                         // edit only own
  }

  // ✅ Normal user rules (edit/delete only own + within 5 minutes)
  if (!isOwner) return false;

  const createdAt =
    item?.createdAt ?? (item === this.topic ? (this.topic as any)?.createdAt : null);

  if (!createdAt) return false;

  const diffMinutes = (Date.now() - new Date(createdAt).getTime()) / (1000 * 60);
  return diffMinutes < this.EDIT_WINDOW_MINUTES;
}


  

 
  /**
   * Helper to check if a kebab menu should even be shown.
   * Useful if you have other actions like 'Report' that don't expire.
   */
  // hasActions(item: any): boolean {
  //   return this.canModify(item);
  // }

  private setupWebSocket(): void {
    if (!this.topic?.titleId) return;
    const dest = `/topic/title.${this.topic.titleId}`;
    if (this.currentWsDest === dest) return;

    if (this.currentWsDest) this.ws.unsubscribeFromTopic(this.currentWsDest);
    this.currentWsDest = dest;

    this.ws.subscribe<any>(dest, (evt) => {
      if (evt.type === 'POST_CREATED') {
        const p = evt.payload as PostDto;
        if (!this.topic.posts.find(x => x.postId === p.postId)) {
          this.topic.posts = [p, ...this.topic.posts];
          this.cdr.detectChanges();
        }
      } else if (evt.type === 'REPLY_CREATED') {
        const data = evt.payload as { postId: number; reply: ReplyDto };
        const post = this.topic.posts.find(p => p.postId === data.postId);
        if (post) {
          this.handleIncomingReply(post, data.reply);
        }
      }
    });
  }

  private handleIncomingReply(post: PostDto, reply: ReplyDto): void {
    const newNode = reply as ReplyNode;
    if (!post.replies) post.replies = [];
    
    if (this.findReplyNode(post.replies as ReplyNode[], reply.replyId)) return;

    if (!reply.parentReplyId) {
      post.replies = [...(post.replies as ReplyNode[]), newNode];
    } else {
      const parent = this.findReplyNode(post.replies as ReplyNode[], reply.parentReplyId);
      if (parent) {
        parent.children = [...(parent.children || []), newNode];
        this.openChildThreads.add(parent.replyId);
      } else {
        post.replies = [...(post.replies as ReplyNode[]), newNode];
      }
    }
    post.replyCount = (post.replyCount || 0) + 1;
    this.cdr.detectChanges();
  }

  private findReplyNode(list: ReplyNode[], id: number): ReplyNode | null {
    if (!list) return null;
    for (const r of list) {
      if (r.replyId === id) return r;
      if (r.children) {
        const found = this.findReplyNode(r.children as ReplyNode[], id);
        if (found) return found;
      }
    }
    return null;
  }

  getLoginUserId(): number {
    const uid = this.secureStorage.getLoginUserId();
    return uid ? parseInt(uid, 10) : 0;
  }

  getUserAvatar(loginUserId: number): string {
    const user = this.usersByLoginId[loginUserId];
    if (user?.profileImageUrl) {
      if (user.profileImageUrl.startsWith('http') || user.profileImageUrl.startsWith('data:')) return user.profileImageUrl;
      return `data:image/png;base64,${user.profileImageUrl}`;
    }
    return `assets/avatar-0.jpg`;
  }

  getTimeLabel(dateStr: string): string {
    if (!dateStr) return 'just now';
    const d = new Date(dateStr);
    const seconds = Math.floor((Date.now() - d.getTime()) / 1000);
    if (seconds < 60) return 'just now';
    const minutes = Math.floor(seconds / 60);
    if (minutes < 60) return `${minutes}m ago`;
    const hours = Math.floor(minutes / 60);
    if (hours < 24) return `${hours}h ago`;
    return d.toLocaleDateString();
  }

  getDateLabel(v: any): string {
    if (!v) return '';
    const d = new Date(v);
    return isNaN(d.getTime()) ? '' : d.toLocaleDateString();
  }

  getTotalReplies(t: TitleWithPostsVM): number {
    return t.posts.reduce((acc, p) => acc + (p.replyCount || 0), 0);
  }

  getTotalViews(t: TitleWithPostsVM): number {
    return t.posts.reduce((acc, p) => acc + (p.viewsCount || 0), 0);
  }

  onBack() { this.back.emit(); }

  toggleReplies(post: PostDto) {
    if (this.openRepliesPosts.has(post.postId)) this.openRepliesPosts.delete(post.postId);
    else this.openRepliesPosts.add(post.postId);
  }
  isRepliesOpen(post: PostDto) { return this.openRepliesPosts.has(post.postId); }

  toggleChildThread(reply: ReplyDto) {
    if (this.openChildThreads.has(reply.replyId)) this.openChildThreads.delete(reply.replyId);
    else this.openChildThreads.add(reply.replyId);
  }
  isChildThreadOpen(reply: ReplyDto) { return this.openChildThreads.has(reply.replyId); }

  onReplyToPost(post: PostDto) {
    this.replyingToPostId = post.postId;
    this.replyingToReplyId = null;
    this.inlineReplyText = '';
  }

  onReplyToReply(post: PostDto, reply: ReplyDto) {
    this.replyingToPostId = post.postId;
    this.replyingToReplyId = reply.replyId;
    this.inlineReplyText = '';
  }

  cancelInlineReply() {
    this.replyingToPostId = null;
    this.replyingToReplyId = null;
    this.inlineReplyText = '';
  }

  submitInlineReply(post: PostDto) {
    if (!this.inlineReplyText.trim()) return;
    this.isBusy = true;
    this.forum.createReply({
      postId: post.postId,
      parentReplyId: this.replyingToReplyId,
      contentMd: this.inlineReplyText.trim()
    }, {}).pipe(finalize(() => this.isBusy = false)).subscribe(res => {
      this.handleIncomingReply(post, res);
      this.cancelInlineReply();
      this.openRepliesPosts.add(post.postId);
    });
  }

  createNewPostForTitle(text: string) {
    if (!text.trim()) return;
    this.isBusy = true;
    this.forum.createPost({
      topicId: this.topic.topicId,
      titleId: this.topic.titleId,
      descriptionMd: text.trim()
    }).pipe(finalize(() => this.isBusy = false)).subscribe(res => {
      if (!this.topic.posts.find(x => x.postId === res.postId)) {
        this.topic.posts = [res, ...this.topic.posts];
      }
    });
  }

  isBlank(s: string) { return !s || !s.trim(); }

  trackPost(index: number, post: PostDto) { return post.postId; }
  trackReply(index: number, reply: ReplyDto) { return reply.replyId; }

  editItem(type: any, item: any) {
    if (!this.canModify(item, 'edit')) {
      alert('The edit window has closed for this item.');
      return;
    }
    this.dialog.open(EditContentDialogComponent, {
      width: '600px',
      data: { 
        title: `Edit ${type}`, 
        mainContent: item.content || item.descriptionMd, 
        secondaryContent: item.title,
        isTitle: type === 'title'
      }
    }).afterClosed().subscribe(res => {
        if (!res) return;
        this.isBusy = true;
        this.forum.edit({
            titleId: this.topic.titleId,
            postId: type === 'post' ? item.postId : undefined,
            replyId: type === 'reply' ? item.replyId : undefined,
            newContentMd: res.mainContent,
            newTitle: res.secondaryContent
        }).pipe(finalize(() => this.isBusy = false)).subscribe((updated: any) => {
            if (type === 'title') {
                this.topic.title = updated.title;
                this.topic.descriptionMd = updated.descriptionMd;
            } else {
                item.content = updated.content || item.content;
            }
        });
    });
  }

  deleteItem(type: any, item: any, parentPost?: PostDto) {
    this.dialog.open(ConfirmDialogComponent, {
      data: { title: `Delete ${type}`, message: `Are you sure you want to delete this ${type}?` }
    }).afterClosed().subscribe(ok => {
      if (!ok) return;
      this.isBusy = true;
      const deleteObs = type === 'title' ? this.forum.deleteTitle(item.titleId) :  type === 'post'
          ? this.forum.deleteItem({ postId: item.postId })
          : this.forum.deleteItem({ replyId: item.replyId });
      deleteObs.pipe(finalize(() => this.isBusy = false)).subscribe(() => {
          if (type === 'title') this.onBack();
          else if (type === 'post') this.topic.posts = this.topic.posts.filter(p => p.postId !== item.postId);
          else if (type === 'reply') {
              if (parentPost) {
                parentPost.replies = (parentPost.replies as ReplyDto[]).filter(r => r.replyId !== item.replyId);
              } else {
                 // Tree search if parent not explicitly provided
                 this.topic.posts.forEach(p => {
                    p.replies = (p.replies as ReplyDto[]).filter(r => r.replyId !== item.replyId);
                 });
              }
          }
      });
    });
  }

  onLikePost(post: any) { 
    this.forum.toggleLike({ postId: post.postId }).subscribe(c => {
        post.likesCount = c;
        post.isLiked = !post.isLiked;
    }); 
  }
  onLikeReply(reply: any) { 
    this.forum.toggleLike({ replyId: reply.replyId }).subscribe(c => {
        reply.likesCount = c;
        reply.isLiked = !reply.isLiked;
    }); 
  }

  getPostAuthorName(p: PostDto) { return p.loginUserName || 'User'; }
  getReplyAuthorName(r: ReplyDto) { return r.loginUserName || 'User'; }
}