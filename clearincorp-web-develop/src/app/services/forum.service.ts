// src/app/services/forum.service.ts
import { Injectable } from "@angular/core";
import {
  HttpClient,
  HttpParams,
  HttpHeaders,
  HttpContext,
} from "@angular/common/http";
import { Observable } from "rxjs";
import { environment } from "src/environments/environment";
import { SecureStorageService } from "./storage/secure-storage.service";

// ---------- Shared Types ----------
export interface Page<T> {
  content: T[];
  pageable: {
    pageNumber: number;
    pageSize: number;
    sort?: any;
    offset?: number;
    paged?: boolean;
    unpaged?: boolean;
  };
  totalElements: number;
  totalPages: number;
  last: boolean;
  size: number;
  number: number;
  sort?: any;
  numberOfElements: number;
  first: boolean;
  empty: boolean;
}

// ---------- Request DTOs ----------
export interface PostCreateRequestDto {
  topicId: number;
  titleId: number;
  descriptionMd: string; // backend expects descriptionMd in body DTO
}
export interface ReplyCreateRequestDto {
  postId: number;
  parentReplyId: number | null;
  contentMd: string;
}
export interface TitleCreateRequest {
  topicId: number;
  title: string;
  descriptionMd: string;
}

// ---------- Response DTOs ----------
export interface ForumTopicDto {
  topicId: number;
  topicName: string;
}

export interface TitleCheckResponse {
  titleId: number | null;
  topicId: number | null;
  title: string;
  exists: boolean;
  available: boolean;
}

export interface UserModel {
  loginUserId: number;
  firstName?: string;
  lastName?: string;
  email?: string;
  phoneNumber?: string;
  avatarId?: number;
  profileImageUrl?: string; // base64 or URL
}

export interface ReplyDto {
  replyId: number;
  postId: number;
  parentReplyId?: number | null;
  loginUserId: number;
  loginUserName: string;
  content: string;
  depth: number;
  likesCount: number;
  deleted: boolean;
  createdAt: string;
  editedAt?: string | null;
}

export interface PostDto {
isLiked: any;
  postId: number;
  loginUserId: number;
  loginUserName: string;
  topicId: number;
  topicName: string;
  titleId: number;
  title: string;
  content: string;
  pinned: boolean;
  pinnedById?: number | null;
  likesCount: number;
  viewsCount: number;
  replyCount: number;
  deleted: boolean;
  createdAt: string;
  editedAt?: string | null;
  lastActivityAt?: string | null;
  replies: ReplyDto[];
}

export interface TitleDto {
  titleId: number;
  topicId: number;
  title: string;
  descriptionMd: string;

  // optional fields if your mapper sends them
  createdAt?: string | null;
  lastActivityAt?: string | null;
  replyCount?: number;
  viewCount?: number;

  // ✅ allow these if backend includes them (safe for compilation)
  createdByLoginId?: number | null;
  createdByName?: string | null;

  posts?: Page<PostDto> | null;
}
type HttpOptions = {
  headers?: HttpHeaders | { [header: string]: string | string[] };
  context?: HttpContext;
  observe?: "body";
  params?:
    | HttpParams
    | {
        [param: string]:
          | string
          | number
          | boolean
          | readonly (string | number | boolean)[];
      };
  reportProgress?: boolean;
  responseType?: "json";
  withCredentials?: boolean;
};

@Injectable({ providedIn: "root" })
export class UserProfileService {
  private readonly base = this.join(environment.apiBaseUrl, "loginUser"); // example: /loginUser/{id}

  constructor(
    private http: HttpClient,
    private secureStorage: SecureStorageService
  ) {}

  getUserByLoginUserId(loginUserId: number): Observable<UserModel> {
    return this.http.get<UserModel>(this.join(this.base, String(loginUserId)));
  }

  private join(a: string, b: string): string {
    if (!a.endsWith("/")) a += "/";
    return a + b.replace(/^\//, "");
  }
}

@Injectable({ providedIn: "root" })
export class ForumService {
  private readonly base = this.join(environment.apiBaseUrl, "forum");

  constructor(
    private http: HttpClient,
    private secureStorage: SecureStorageService
  ) {}

  // ====== LOGIN USER HELPER ======
  private getLoginUserIdFromSession(): number {
    const s = this.secureStorage.getLoginUserId();
    if (s && !isNaN(+s)) return +s;
    const l = this.secureStorage.getLoginUserId();
    if (l && !isNaN(+l)) return +l;

    try {
      const blob = this.secureStorage.getLoggedInUserData();
      if (blob) {
        const parsed = JSON.parse(blob);
        if (parsed?.loginUserId && !isNaN(+parsed.loginUserId))
          return +parsed.loginUserId;
      }
    } catch {}

    return 1; // fallback
  }

  private withLogin(params?: HttpParams): HttpParams {
    const id = String(this.getLoginUserIdFromSession());
    const p = params ?? new HttpParams();
    return p.set("login_user_id", id);
  }

  private params(
    obj: Record<string, string | number | boolean | null | undefined>
  ): HttpParams {
    let p = new HttpParams();
    Object.entries(obj).forEach(([k, v]) => {
      if (v !== null && v !== undefined && v !== "") p = p.set(k, String(v));
    });
    return p;
  }

  private join(a: string, b: string): string {
    if (!a.endsWith("/")) a += "/";
    return a + b.replace(/^\//, "");
  }

  // ====== POSTS ======
  createPost(req: PostCreateRequestDto): Observable<PostDto> {
    const url = this.join(this.base, "posts");
    return this.http.post<PostDto>(url, req, { params: this.withLogin() });
  }

  listByTopic(topicId: number, page = 0, size = 20): Observable<Page<PostDto>> {
    const url = this.join(this.base, "posts/by-topic");
    let params = this.params({ topic_id: topicId, page, size });
    params = this.withLogin(params);
    return this.http.get<Page<PostDto>>(url, { params });
  }

  listByAuthor(page = 0, size = 20): Observable<Page<PostDto>> {
    const url = this.join(this.base, "posts/by-user");
    let params = this.params({ page, size });
    params = this.withLogin(params);
    return this.http.get<Page<PostDto>>(url, { params });
  }

  listAllPosts(page = 0, size = 20): Observable<Page<PostDto>> {
    const url = this.join(this.base, "all-posts");
    let params = this.params({ page, size });
    params = this.withLogin(params);
    return this.http.get<Page<PostDto>>(url, { params });
  }

  getPost(postId: number): Observable<PostDto> {
    const url = this.join(this.base, "post");
    let params = this.params({ post_id: postId });
    params = this.withLogin(params);
    return this.http.get<PostDto>(url, { params });
  }

  edit(options: {
    titleId?: number;
    postId?: number;
    replyId?: number;
    newContentMd?: string;
    newTitle?: string;
    newDescriptionMd?: string;
  }): Observable<PostDto | ReplyDto | TitleDto | { message: string }> {
    const url = this.join(this.base, "edit");
    let params = this.params({
      title_id: options.titleId,
      post_id: options.postId,
      reply_id: options.replyId,
      new_content_md: options.newContentMd,
      new_title: options.newTitle,
      new_description_md: options.newDescriptionMd,
    });
    params = this.withLogin(params);
    return this.http.put<PostDto | ReplyDto | TitleDto | { message: string }>(
      url,
      null,
      { params }
    );
  }

  // ✅ BACKEND accepts ONLY post_id + login_user_id (no reply_id)
  togglePinned(postId: number): Observable<PostDto> {
    const url = this.join(this.base, "post/pin-toggle");
    let params = this.params({ post_id: postId });
    params = this.withLogin(params);
    return this.http.post<PostDto>(url, null, { params });
  }

  // ✅ BACKEND accepts ONLY post_id + login_user_id (no reply_id)
  viewPost(postId: number): Observable<number> {
    const url = this.join(this.base, "post/view");
    let params = this.params({ post_id: postId });
    params = this.withLogin(params);
    return this.http.post<number>(url, null, { params });
  }

  toggleLike(opts: { postId?: number; replyId?: number }): Observable<number> {
    const url = this.join(this.base, "likes");
    let params = this.params({ post_id: opts.postId, reply_id: opts.replyId });
    params = this.withLogin(params);
    return this.http.post<number>(url, null, { params });
  }

  deleteItem(opts: { postId?: number; replyId?: number }): Observable<string> {
    const url = this.join(this.base, "delete");
    let params = this.params({ post_id: opts.postId, reply_id: opts.replyId });
    params = this.withLogin(params);
    return this.http.delete(url, { params, responseType: "text" });
  }

  // ====== REPLIES ======
  createReply(
    req: ReplyCreateRequestDto,
    skipLoadingOptions: unknown
  ): Observable<ReplyDto> {
    const headers = new HttpHeaders().set("X-Skip-Loading", "true");
    const url = this.join(this.base, "replies");
    return this.http.post<ReplyDto>(url, req, { params: this.withLogin() });
  }

  listTopReplies(
    postId: number,
    page = 0,
    size = 50
  ): Observable<Page<ReplyDto>> {
    const headers = new HttpHeaders().set("X-Skip-Loading", "true");
    const url = this.join(this.base, "post/replies");
    let params = this.params({ post_id: postId, page, size });
    params = this.withLogin(params);
    return this.http.get<Page<ReplyDto>>(url, { params, headers });
  }

  listChildReplies(
    replyId: number,
    page = 0,
    size = 50
  ): Observable<Page<ReplyDto>> {
    const headers = new HttpHeaders().set("X-Skip-Loading", "true");
    const url = this.join(this.base, "reply/children");
    let params = this.params({ reply_id: replyId, page, size });
    params = this.withLogin(params);
    return this.http.get<Page<ReplyDto>>(url, { params, headers });
  }

  // ====== SEARCH / TOPICS ======
  // NOTE: backend search returns posts, not titles
  search(query: string, page = 0, size = 50): Observable<PostDto[]> {
    const url = this.join(this.base, "search");
    const params = this.params({ query, page, size });
    return this.http.get<PostDto[]>(url, { params });
  }

  getAllTopicIdsAndNames(): Observable<ForumTopicDto[]> {
    const url = this.join(this.base, "topics");
    return this.http.get<ForumTopicDto[]>(url);
  }

  // ====== TITLES ======
  // backend expects request params, not JSON body
  createTitle(req: TitleCreateRequest): Observable<TitleDto> {
    const url = this.join(this.base, "titles");
    let params = this.params({
      topic_id: req.topicId,
      title: req.title,
      descriptionMd: req.descriptionMd,
    });
    params = this.withLogin(params);
    return this.http.post<TitleDto>(url, null, { params });
  }

  getTitleWithPosts(
    titleId: number,
    page = 0,
    size = 10
  ): Observable<TitleDto> {
    const url = this.join(this.base, "title-posts");
    const params = this.params({ title_id: titleId, page, size });
    return this.http.get<TitleDto>(url, { params });
  }

  // backend listTitles supports only topic_id + page/size
  listTitles(
    topicId?: number,
    page = 0,
    size = 20
  ): Observable<Page<TitleDto>> {
    const url = this.join(this.base, "titles");
    const params = this.params({ topic_id: topicId, page, size });
    return this.http.get<Page<TitleDto>>(url, { params });
  }

  deleteTitle(titleId: number): Observable<string> {
    const url = this.join(this.base, "titles/delete");
    let params = this.params({ title_id: titleId });
    params = this.withLogin(params);
    return this.http.delete(url, { params, responseType: "text" });
  }

  checkTitleAvailableGlobally(
    title: string,
    excludeId?: number
  ): Observable<TitleCheckResponse> {
    const url = this.join(this.base, "titles/check");
    const params = this.params({ title, exclude_title_id: excludeId });
    return this.http.get<TitleCheckResponse>(url, { params });
  }

  // ====== REPORTS / ADMIN ======
  // ✅ backend supports title_id also
  reportItem(opts: {
    titleId?: number;
    postId?: number;
    replyId?: number;
  }): Observable<any> {
    const url = this.join(this.base, "report");
    let params = this.params({
      title_id: opts.titleId,
      post_id: opts.postId,
      reply_id: opts.replyId,
    });
    params = this.withLogin(params);
    return this.http.post<any>(url, null, { params });
  }

  adminSoftDeleteItem(opts: {
    titleId?: number;
    postId?: number;
    replyId?: number;
  }): Observable<any> {
    const url = this.join(this.base, "admin/soft-delete");
    let params = this.params({
      title_id: opts.titleId,
      post_id: opts.postId,
      reply_id: opts.replyId,
    });
    params = this.withLogin(params);
    return this.http.post<any>(url, null, { params });
  }
}
