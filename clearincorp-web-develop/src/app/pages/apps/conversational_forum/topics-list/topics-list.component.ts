import {
  Component,
  Input,
  Output,
  EventEmitter,
  ChangeDetectionStrategy,
  OnChanges,
  SimpleChanges,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatMenuModule } from '@angular/material/menu';

import { TopicItem } from 'src/app/models/forum.model';

type TopicGroup = { label: string; sortKey: number; dayTime: number; topics: TopicItem[] };

@Component({
  selector: 'app-topics-list',
  standalone: true,
  imports: [CommonModule, MatIconModule, MatButtonModule, MatTooltipModule, MatMenuModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="topics-wrap">

      <div class="brandbar">
        <div class="brand-left">
          <div class="brand-logo" aria-hidden="true">
            <mat-icon>forum</mat-icon>
          </div>

          <div class="brand-text">
            <div class="brand-title">ClearInCorp Community</div>
            <div class="brand-sub">Ask • Learn • Share • Grow</div>
          </div>
        </div>

        <div class="brand-right" *ngIf="activeCategory">
          <span class="category-pill">
            <mat-icon>local_offer</mat-icon>
            <span class="category-text">{{ activeCategory }}</span>
          </span>
        </div>
      </div>

      <ng-container *ngIf="groupedList.length; else emptyState">
        <div class="group" *ngFor="let group of groupedList; trackBy: trackGroup">
          <div class="group-title">{{ group.label }}</div>

          <div
            class="topic"
            *ngFor="let topic of group.topics; trackBy: trackTopic"
            (click)="emitPick(topic)"
            [class.is-active]="isActive(topic)"
            tabindex="0"
            (keydown.enter)="emitPick(topic)"
            (keydown.space)="$event.preventDefault(); emitPick(topic)"
          >
            <div class="topic-row">
              <div class="topic-left">
                <div class="title">
                  <mat-icon
                    *ngIf="isPinned(topic)"
                    class="pin"
                    matTooltip="Pinned"
                    aria-label="Pinned"
                    >push_pin</mat-icon
                  >
                  <span class="title-text">{{ getTitle(topic) }}</span>
                </div>

                <div class="desc">
                  {{ getLimitedDescription(getTopicText(topic)) }}
                </div>

                <div class="meta">
                  <span class="meta-item">
                    <mat-icon>schedule</mat-icon>
                    <span>{{ getTimeAgo(getTopicWhen(topic)) }}</span>
                  </span>

                  <ng-container *ngIf="getAuthorName(topic) as author">
                    <span class="meta-item">
                      <mat-icon>person</mat-icon>
                      <span>{{ author }}</span>
                    </span>
                  </ng-container>

                  <!-- <ng-container *ngIf="getReplies(topic) !== null">
                    <span class="meta-item">
                      <mat-icon>chat_bubble_outline</mat-icon>
                      <span>{{ getReplies(topic) }}</span>
                    </span>
                  </ng-container> -->

                  <!-- <ng-container *ngIf="getViews(topic) !== null">
                    <span class="meta-item">
                      <mat-icon>visibility</mat-icon>
                      <span>{{ getViews(topic) }}</span>
                    </span>
                  </ng-container> -->
                </div>
              </div>

              <div class="topic-right">
                <button
                  mat-icon-button
                  [matMenuTriggerFor]="menu"
                  (click)="$event.stopPropagation()"
                  aria-label="Topic actions"
                >
                  <mat-icon>more_vert</mat-icon>
                </button>

                <mat-menu #menu="matMenu">
                  <button mat-menu-item (click)="emitPick(topic)">
                    <mat-icon>open_in_new</mat-icon>
                    <span>Open</span>
                  </button>
                </mat-menu>
              </div>
            </div>
          </div>
        </div>
      </ng-container>

      <ng-template #emptyState>
        <div class="empty">
          <mat-icon>forum</mat-icon>
          <div class="empty-title">No topics yet</div>
          <div class="empty-sub">Create a topic to start the conversation.</div>
        </div>
      </ng-template>
    </div>
  `,
  styles: [`
    :host{
  /* USA THEME */
  --usa-blue: #0A3161;
  --usa-red:  #B31942;
  --usa-white:#FFFFFF;

  /* RGB helpers (if you want rgb(var(--usa-blue-rgb)/.2) style) */
  --usa-blue-rgb: 10 49 97;
  --usa-red-rgb:  179 25 66;

  /* Forum base */
  --card: rgba(255,255,255,.92);
  --text: rgba(15,23,42,.92);
  --muted: rgba(15,23,42,.62);
  --border: rgba(15,23,42,.10);

  --shadow: 0 10px 26px rgba(2, 6, 23, 0.08);
  --shadow2: 0 18px 42px rgba(2, 6, 23, 0.12);

  --radius-xl: 22px;

  display:block;
  width:100%;
  color:var(--text);
}

/* page padding */
.topics-wrap{
  padding: 10px;
}

/* ===== Brand / header bar (optional if you have it) ===== */
.brandbar{
  padding: 14px 14px;
  border-radius: 18px;
  border: 1px solid rgba(15,23,42,.08);
  box-shadow: 0 10px 24px rgba(2,6,23,.06);

  background:
    radial-gradient(520px 220px at 18% 8%,  rgb(var(--usa-blue-rgb) / .18), transparent 58%),
    radial-gradient(520px 220px at 88% 0%,  rgb(var(--usa-red-rgb)  / .14), transparent 62%),
    rgba(255,255,255,.86);

  backdrop-filter: blur(10px);
}

.brand-left{
  display:flex;
  align-items:center;
  gap:12px;
  min-width:0;
}

.brand-logo{
  width:44px;
  height:44px;
  border-radius:16px;
  display:grid;
  place-items:center;
  color: var(--usa-white);
  background:
    radial-gradient(16px 16px at 30% 25%, rgba(255,255,255,.55), transparent 60%),
    linear-gradient(135deg, var(--usa-blue), var(--usa-red));
  box-shadow: 0 18px 40px rgb(var(--usa-blue-rgb) / .20);
  flex-shrink:0;
}

.brand-logo mat-icon{
  font-size:22px;
  width:22px;
  height:22px;
}

.brand-text{
  min-width:0;
  display:flex;
  flex-direction:column;
}

.brand-title{
  font-weight:1000;
  font-size:14px;
  letter-spacing:.2px;
  line-height:1.1;
  white-space:nowrap;
  overflow:hidden;
  text-overflow:ellipsis;
}

.brand-sub{
  margin-top:3px;
  font-size:12px;
  color: rgba(15,23,42,.60);
  white-space:nowrap;
  overflow:hidden;
  text-overflow:ellipsis;
}

.brand-right{ flex-shrink:0; }

.category-pill{
  display:inline-flex;
  align-items:center;
  gap:6px;
  padding:7px 12px;
  border-radius:999px;
  font-weight:900;
  font-size:12px;
  color: rgba(15,23,42,.74);
  background: rgba(255,255,255,.84);
  border: 1px solid rgba(15,23,42,.10);
  max-width: 240px;
}

.category-pill mat-icon{
  font-size:16px;
  width:16px;height:16px;
  color: rgb(var(--usa-blue-rgb) / .92);
}

.category-text{
  overflow:hidden;
  text-overflow:ellipsis;
  white-space:nowrap;
}

/* ===== Group Title (LAST 7 DAYS / etc) ===== */
.group{
  margin-bottom: 20px;
}

.group-title{
  font-weight: 1000;
  font-size: 12px;
  letter-spacing: .14em;
  text-transform: uppercase;
  color: rgba(15,23,42,.58);
  margin: 12px 6px;
  display:flex;
  align-items:center;
  gap: 10px;
}

.group-title::after{
  content:"";
  height: 1px;
  flex: 1;
  background: linear-gradient(
    to right,
    rgb(var(--usa-blue-rgb) / .22),
    rgb(var(--usa-red-rgb) / .12),
    transparent
  );
}

/* ===== Topic Card ===== */
.topic{
  position:relative;
  border-radius: var(--radius-xl);
  padding: 18px 18px;     /* BIGGER CARD */
  margin: 14px 0;         /* more spacing */
  cursor:pointer;

  background: var(--card);
  border: 1px solid rgba(15,23,42,.10);
  box-shadow: var(--shadow);

  transition: transform 160ms ease, box-shadow 160ms ease, border-color 160ms ease, background 160ms ease;
  outline:none;

  overflow:hidden;
}

/* glow layer */
.topic::before{
  content:"";
  position:absolute;
  inset:-2px;
  border-radius: calc(var(--radius-xl) + 2px);
  background: linear-gradient(135deg,
    rgb(var(--usa-blue-rgb) / .18),
    rgb(var(--usa-red-rgb)  / .12),
    rgb(var(--usa-blue-rgb) / .08)
  );
  opacity: 0;
  transition: opacity 160ms ease;
  pointer-events:none;
}

/* left accent stripe */
.topic::after{
  content:"";
  position:absolute;
  left:0;
  top:14px;
  bottom:14px;
  width: 5px;
  border-radius: 999px;
  background: linear-gradient(180deg, var(--usa-blue), var(--usa-red));
  opacity: .55;
}

.topic:hover{
  transform: translateY(-2px);
  border-color: rgb(var(--usa-blue-rgb) / .26);
  box-shadow: var(--shadow2);
}

.topic:hover::before{
  opacity: 1;
}

/* selected / active */
.topic.is-active{
  border-color: rgb(var(--usa-blue-rgb) / .55);
  background:
    linear-gradient(0deg, rgb(var(--usa-blue-rgb) / .06), rgb(var(--usa-blue-rgb) / .06)),
    var(--card);
  box-shadow: 0 22px 54px rgb(var(--usa-blue-rgb) / .14);
}

.topic-row{
  display:flex;
  gap: 14px;
  align-items:flex-start;
  position: relative;
  z-index: 1;
}

.topic-left{
  flex:1;
  min-width:0;
}

.topic-right{
  display:flex;
  align-items:center;
  gap: 8px;
  margin-top: -2px;
}

/* ===== Title ===== */
.title{
  display:flex;
  align-items:center;
  gap:10px;
  min-width:0;
}

/* BIG TITLE + highlight */
.title-text{
  font-size: 22px;        /* BIGGER */
  font-weight: 1000;
  line-height: 1.18;
  letter-spacing: .2px;
  overflow:hidden;
  white-space:nowrap;
  text-overflow:ellipsis;
  color: rgba(15,23,42,.95);
  position: relative;
}

/* subtle underline highlight */
.title-text::after{
  content:"";
  position:absolute;
  left:0;
  bottom:-6px;
  width: 120px;
  height: 3px;
  border-radius: 999px;
  background: linear-gradient(90deg, var(--usa-blue), var(--usa-red));
  opacity: .22;
}

.pin{
  color: var(--usa-red);
}

/* ===== Description ===== */
.desc{
  margin-top: 10px;
  color: rgba(15,23,42,.70);
  font-size: 14.5px;       /* bigger */
  line-height: 1.55;
  display:-webkit-box;
  -webkit-line-clamp: 3;   /* show more */
  -webkit-box-orient:vertical;
  overflow:hidden;
}

/* ===== Meta pills ===== */
.meta{
  margin-top: 12px;
  display:flex;
  flex-wrap:wrap;
  gap: 10px;
  align-items:center;
  font-size: 12.5px;
  color: rgba(15,23,42,.60);
}

.meta-item{
  display:inline-flex;
  align-items:center;
  gap:6px;
  padding: 7px 12px;       /* bigger pill */
  border-radius: 999px;
  background: rgba(15,23,42,.045);
  border: 1px solid rgba(15,23,42,.06);
  font-weight: 900;
}

.meta mat-icon{
  font-size: 16px;
  width: 16px;
  height: 16px;
  opacity: .95;
  color: rgb(var(--usa-blue-rgb) / .88);
}

/* ===== Menu button ===== */
.topic-right button[mat-icon-button]{
  border: 1px solid rgba(15,23,42,.10);
  border-radius: 14px;
  background: rgba(255,255,255,.86);
  transition: transform 140ms ease, background 140ms ease, border-color 140ms ease, box-shadow 140ms ease;
  box-shadow: 0 8px 18px rgba(2,6,23,.06);
}

.topic-right button[mat-icon-button]:hover{
  transform: translateY(-1px);
  background: rgba(255,255,255,.98);
  border-color: rgb(var(--usa-blue-rgb) / .22);
  box-shadow: 0 12px 26px rgba(2,6,23,.10);
}

/* ===== Empty state ===== */
.empty{
  text-align:center;
  padding: 48px 16px;
  border: 1px dashed rgba(15,23,42,.22);
  border-radius: 20px;
  background: rgba(255,255,255,.72);
  color: rgba(15,23,42,.72);
}

.empty mat-icon{
  font-size: 52px;
  width: 52px;
  height: 52px;
  color: rgb(var(--usa-blue-rgb) / .82);
}

.empty-title{
  margin-top: 12px;
  font-weight: 1000;
  color: rgba(15,23,42,.88);
}

.empty-sub{
  margin-top: 6px;
  color: rgba(15,23,42,.60);
  font-size: 13px;
}

/* ===== Mobile ===== */
@media (max-width: 640px){
  .topics-wrap{ padding: 6px; }
  .brandbar{ padding: 12px; border-radius: 16px; }

  .topic{
    padding: 14px;
    border-radius: 18px;
  }

  .title-text{
    font-size: 18px;   /* still bigger on mobile */
  }

  .desc{
    font-size: 13.5px;
    -webkit-line-clamp: 2;
  }

  .meta{
    gap: 8px;
  }

  .meta-item{
    padding: 6px 10px;
    font-size: 12px;
  }
}

  `],
})
export class TopicsListComponent implements OnChanges {
  @Input() topics: TopicItem[] = [];
  @Input() activeTopicId: number | string | null = null;

  @Input() activeCategory: any = null;

  // keep shape compatible (forum-main passes Record<number, ...>)
  @Input() usersByLoginId: Record<number, any> = {};

  // ✅ SUPPORT BOTH TEMPLATE STYLES:
  // forum-main currently uses (select)="..."
  @Output() select = new EventEmitter<TopicItem>();
  // some places may use (topicSelected)="..."
  @Output() topicSelected = new EventEmitter<TopicItem>();

  groupedList: TopicGroup[] = [];

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['topics']) this.groupedList = this.buildGroups(this.topics || []);
  }

  emitPick(topic: TopicItem) {
    this.select.emit(topic);
    this.topicSelected.emit(topic);
  }

  /* ================== SAFE GETTERS ================== */
  private getId(t: TopicItem): string {
    const anyT = t as any;
    return String(anyT?.id ?? anyT?.topicId ?? anyT?.titleId ?? anyT?._id ?? '');
  }

  isPinned(t: TopicItem): boolean {
    const anyT = t as any;
    return Boolean(anyT?.pinned ?? anyT?.isPinned ?? false);
  }

  getTitle(t: TopicItem): string {
    const anyT = t as any;
    return String(anyT?.title ?? anyT?.topicTitle ?? 'Untitled');
  }

  getAuthorName(t: TopicItem): string | null {
    const anyT = t as any;
    return (anyT?.authorName ?? anyT?.createdByName ?? anyT?.author?.name ?? null) || null;
  }

  getReplies(t: TopicItem): number | null {
    const anyT = t as any;
    const v = anyT?.repliesCount ?? anyT?.replyCount ?? anyT?.replies ?? null;
    return (v === null || v === undefined) ? null : Number(v);
  }

  getViews(t: TopicItem): number | null {
    const anyT = t as any;
    const v = anyT?.viewsCount ?? anyT?.viewCount ?? anyT?.views ?? null;
    return (v === null || v === undefined) ? null : Number(v);
  }

  getTopicText(t: TopicItem): string {
    const anyT = t as any;
    return String(anyT?.descriptionMd ?? anyT?.preview ?? anyT?.description ?? '');
  }

  getTopicWhen(t: TopicItem): Date | null {
    const anyT = t as any;
    return (
      this.toDate(anyT?.lastActivity ?? anyT?.lastActivityAt) ??
      this.toDate(anyT?.createdAt ?? anyT?.createdOn ?? anyT?.createdDate ?? anyT?.created_at) ??
      null
    );
  }

  isActive(t: TopicItem): boolean {
    if (this.activeTopicId === null || this.activeTopicId === undefined) return false;
    return this.getId(t) === String(this.activeTopicId);
  }

  trackGroup = (_: number, g: TopicGroup) => `${g.sortKey}|${g.dayTime}|${g.label}`;
  trackTopic = (_: number, t: TopicItem) => this.getId(t);

  /* ================== GROUPING ================== */
  private buildGroups(list: TopicItem[]): TopicGroup[] {
    const todayStart = this.startOfDay(new Date());
    const yesterdayStart = this.addDays(todayStart, -1);
    const last7Start = this.addDays(todayStart, -6);

    const map = new Map<string, TopicGroup>();

    const sorted = [...(list || [])].sort((a, b) => {
      const ad = this.getTopicWhen(a)?.getTime() ?? 0;
      const bd = this.getTopicWhen(b)?.getTime() ?? 0;
      return bd - ad;
    });

    for (const t of sorted) {
      const created = this.getTopicWhen(t);
      if (!created) continue;

      const day = this.startOfDay(created);
      let label = '';
      let sortKey = 0;

      if (day.getTime() === todayStart.getTime()) {
        label = 'Today';
        sortKey = 3;
      } else if (day.getTime() === yesterdayStart.getTime()) {
        label = 'Yesterday';
        sortKey = 2;
      } else if (day.getTime() >= last7Start.getTime()) {
        label = 'Last 7 days';
        sortKey = 1;
      } else {
        label = this.formatFullDate(day);
        sortKey = 0;
      }

      const key = `${sortKey}|${day.getTime()}|${label}`;
      if (!map.has(key)) map.set(key, { label, sortKey, dayTime: day.getTime(), topics: [] });
      map.get(key)!.topics.push(t);
    }

    const groups = Array.from(map.values()).sort((a, b) => {
      if (b.sortKey !== a.sortKey) return b.sortKey - a.sortKey;
      return b.dayTime - a.dayTime;
    });

    for (const g of groups) {
      g.topics.sort((a, b) => {
        const ad = this.getTopicWhen(a)?.getTime() ?? 0;
        const bd = this.getTopicWhen(b)?.getTime() ?? 0;
        return bd - ad;
      });
    }

    return groups;
  }

  /* ================== TEXT HELPERS ================== */
  getLimitedDescription(text: string, max = 140): string {
    const clean = this.stripMarkdown(text).replace(/\s+/g, ' ').trim();
    if (!clean) return '';
    return clean.length > max ? clean.slice(0, max).trimEnd() + '…' : clean;
  }

  getTimeAgo(input: Date | string | null | undefined): string {
    const d = input instanceof Date ? input : this.toDate(input);
    if (!d) return '';

    const diffMs = Date.now() - d.getTime();
    const s = Math.floor(diffMs / 1000);
    if (s < 10) return 'just now';
    if (s < 60) return `${s}s ago`;

    const m = Math.floor(s / 60);
    if (m < 60) return `${m}m ago`;

    const h = Math.floor(m / 60);
    if (h < 24) return `${h}h ago`;

    const days = Math.floor(h / 24);
    if (days < 7) return `${days}d ago`;

    return this.formatFullDate(d);
  }

  /* ================== DATE HELPERS ================== */
  private toDate(v: string | Date | null | undefined): Date | null {
    if (!v) return null;
    if (v instanceof Date) return isNaN(v.getTime()) ? null : v;
    const d = new Date(v);
    return isNaN(d.getTime()) ? null : d;
  }

  private startOfDay(d: Date): Date {
    return new Date(d.getFullYear(), d.getMonth(), d.getDate());
  }

  private addDays(d: Date, days: number): Date {
    const x = new Date(d);
    x.setDate(x.getDate() + days);
    return x;
  }

  private formatFullDate(d: Date): string {
    return d.toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' });
  }

  private stripMarkdown(md: string): string {
    if (!md) return '';
    return md
      .replace(/```[\s\S]*?```/g, '')
      .replace(/`([^`]+)`/g, '$1')
      .replace(/!\[.*?\]\(.*?\)/g, '')
      .replace(/\[([^\]]+)\]\([^)]+\)/g, '$1')
      .replace(/(^|\s)#+\s+/g, '$1')
      .replace(/(\*\*|__)(.*?)\1/g, '$2')
      .replace(/(\*|_)(.*?)\1/g, '$2')
      .replace(/>\s+/g, '')
      .replace(/[-*_]{3,}/g, '')
      .replace(/^\s*[-+*]\s+/gm, '')
      .replace(/^\s*\d+\.\s+/gm, '');
  }
}
