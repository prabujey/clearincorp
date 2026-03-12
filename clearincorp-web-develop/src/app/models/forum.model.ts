// // src/app/models/forum.model.ts

// export interface User {
//   id: string;
//   name: string;
//   avatar: string;
// }

// export interface Reply {
//   id: string;
//   content: string;
//   author: User;
//   createdAt: Date;
//   likes: string[];
// }

// export interface Post {
//   id: string;
//   content: string;
//   author: User;
//   createdAt: Date;
//   likes: string[];
//   replies: Reply[];
// }

// // Simplified version for list views
// export interface Topic {
//   id: string;
//   title: string;
//   category: string;
//   author: User;
//   replyCount: number;
//   viewCount: number;
//   lastActivity: Date;
//   createdAt: Date;
//   isPinned?: boolean;
//   preview?: string; // FIXED: Added preview property for the topic list
// }

// // Complete model used as the single source of truth
// export interface TopicDetail extends Topic {
//   posts: Post[];
// }

// export interface Notification {
//   id: string;
//   type: 'reply' | 'mention' | 'like'; // Add 'like' here
//   topicId: string;
//   topicTitle: string;
//   author: User;
//   message: string;
//   createdAt: Date;
//   isRead: boolean;
// }

// export type Category = 'LLC Formation' | 'Tax & Legal' | 'Business Growth' | 'Success Stories' | 'General';



// src/app/models/forum.model.ts (FINAL FIXED VERSION)

// export interface User {
//   id: string;
//   name: string;
//   avatar: string;
// }

// export interface Reply {
//   id: string;
//   content: string;
//   author: User;
//   createdAt: Date;
//   likes: string[];
//   // ✅ FIX: Added likesCount from backend DTO
//   likesCount?: number; 
// }

// export interface Post {
//   id: string;
//   content: string;
//   author: User;
//   createdAt: Date;
//   likes: string[];
//   replies: Reply[];
//   // ✅ FIX: Added likesCount from backend DTO
//   likesCount?: number; 
// }

// // Simplified version for list views
// export interface Topic {
//   id: string;
//   title: string;
//   category: string;
//   author: User;
//   replyCount: number;
//   viewCount: number;
//   lastActivity: Date;
//   createdAt: Date;
//   isPinned?: boolean;
//   preview?: string; // FIXED: Added preview property for the topic list
// }

// // Complete model used as the single source of truth
// export interface TopicDetail extends Topic {
//   posts: Post[];
// }

// export interface Notification {
//   id: string;
//   type: 'reply' | 'mention' | 'like'; // Add 'like' here
//   topicId: string;
//   topicTitle: string;
//   author: User;
//   message: string;
//   createdAt: Date;
//   isRead: boolean;
// }

// export type Category = 'LLC Formation' | 'Tax & Legal' | 'Business Growth' | 'Success Stories' | 'General';

// export interface ForumTopicDto {
//   topicId: number;
//   topicName: string;
// }

// export interface ReplyDto {
//   replyId: number;
//   postId: number;
//   parentReplyId?: number;
//   loginUserId: number;
//   loginUserName: string;
//   content: string;
//   depth: number;
//   likesCount: number;
//   deleted: boolean;
//   createdAt: string;
//   editedAt?: string;
// }

// export interface PostDto {
//   postId: number;
//   loginUserId: number;
//   loginUserName: string;
//   topicId: number;
//   topicName: string;
//   titleId: number;
//   title: string;
//   content: string;
//   pinned: boolean;
//   pinnedById?: number;
//   likesCount: number;
//   viewsCount: number;
//   replyCount: number;
//   deleted: boolean;
//   createdAt: string;
//   editedAt?: string;
//   lastActivityAt?: string;
//   replies?: ReplyDto[];
// }

// export interface TitleDto {
//   titleId: number;
//   topicId: number;
//   title: string;
//   descriptionMd: string;
//   createdAt: string;
//   createdById: number;
//   createdByName: string;
//   posts: PostDto[];
// }

// export interface Page<T> {
//   content: T[];
//   totalElements: number;
//   totalPages: number;
//   size: number;
//   number: number;
//   numberOfElements: number;
//   first: boolean;
//   last: boolean;
//   empty: boolean;
// }
// export interface Notification {
//   id: string;
//   message: string;
//   topicId: string;
//   topicTitle: string;
//   isRead: boolean;
//   createdAt: Date | string;
//   author?: {
//     name: string;
//     avatar?: string;
//   };
// }
// export interface TopicDetail {
//   topicId: number;
//   title: string;
//   createdById: number;
//   createdByName: string;
//   createdAt: string;
//   totalPosts: number;
//   totalReplies: number;
//   lastUpdatedAt: string;
// }
// export interface Post {
//   postId: number;
//   topicId: number;
//   content: string;
//   createdById: number;
//   createdByName: string;
//   createdAt: string;
//   replies: Reply[];
// }
// export interface Reply {
//   replyId: number;
//   postId: number;
//   content: string;
//   createdById: number;
//   createdByName: string;
//   createdAt: string;
// }
// export interface User {
//   id: number;
//   name: string;
//   avatarUrl?: string;
//   email?: string;
// }


// export interface ForumTopicDto {
// first: boolean;
// last: boolean;
// empty: boolean;
// }


export interface Notification {
id: string;
message: string;
topicId: string;
topicTitle: string;
isRead: boolean;
createdAt: Date | string;
author?: {
name: string;
avatar?: string;
};
}
export interface ReplyDto {
  replyId: number;
  postId: number;
  parentReplyId: number | null;
  loginUserId: number;
  loginUserName: string;
  content: string;
  depth: number;
  likesCount: number;
  deleted: boolean;
  createdAt: string;
  editedAt: string | null;
  children?: ReplyDto[];
}

export interface PostDto {
  postId: number;
  loginUserId: number;
  loginUserName: string;
  topicId: number;
  topicName: string;
  titleId: number;
  title: string;
  content: string;
  pinned: boolean;
  pinnedById: number | null;
  likesCount: number;
  viewsCount: number;
  replyCount: number;
  deleted: boolean;
  createdAt: string;
  editedAt: string | null;
  lastActivityAt: string | null;
  replies: ReplyDto[] | null;
}
// export interface TopicDetail {
// topicId: number;
// title: string;
// createdById: number;
// createdByName: string;
// createdAt: string;
// totalPosts: number;
// totalReplies: number;
// lastUpdatedAt: string;
// posts: Post[];
// }


// export interface Post {
// id: number;
// topicId: number;
// content: string;
// createdById: number;
// createdByName: string;
// createdAt: string;
// replies: Reply[];
// likes: number[];
// likesCount?: number;
// author: {
//     name: string;
//     avatar: string;
//   };
// }


// export interface Reply {
// replyId: number;
// postId: number;
// content: string;
// createdById: number;
// createdByName: string;
// createdAt: string;
// likes?: number[];
// likesCount?: number;
// }


// export interface User {
// id: number;
// name: string;
// avatarUrl?: string;
// email?: string;
// avatar: string;
// }

// export interface TopicDetail {
//   topicId: number;
//   title: string;
//   createdById: number;
//   createdByName: string;
//   createdAt: string;
//   totalPosts: number;
//   totalReplies: number;
//   lastUpdatedAt: string;
// }
// export interface Post {
//   postId: number;
//   topicId: number;
//   content: string;
//   createdById: number;
//   createdByName: string;
//   createdAt: string;
//   replies: Reply[];
// }
// export interface Reply {
//   replyId: number;
//   postId: number;
//   content: string;
//   createdById: number;
//   createdByName: string;
//   createdAt: string;
// }
// export interface Topic {
//   id: string;
//   title: string;
//   category: string;
//   author: User;
//   replyCount: number;
//   viewCount: number;
//   lastActivity: Date;
//   createdAt: Date;
//   isPinned?: boolean;
//   preview?: string; // FIXED: Added preview property for the topic list
// }
// src/app/models/forum.model.ts
export interface User {
  id: number;
  name: string;
  avatarUrl?: string;
  email?: string;
    author: User; // 👈 This is critical
    avatar: string;
}

export interface Reply {
  replyId: number;
  postId: number;
  parentReplyId: number | null;
  loginUserId: number;
  loginUserName: string;
  content: string;
  depth: number;
  likesCount: number;
  deleted: boolean;
  createdAt: string;          // ISO string
  editedAt: string | null;
  children: Reply[];          // nested tree replies
}
export interface Post {
  postId: number;
  loginUserId: number;
  loginUserName: string;
  topicId: number;
  topicName: string;
  titleId: number;
  title: string;
  content: string;
  pinned: boolean;
  pinnedById: number | null;
  likesCount: number;         // API sends a number count
  viewsCount: number;
  replyCount: number;
  deleted: boolean;
  createdAt: string;          // ISO string
  editedAt: string | null;
  lastActivityAt: string | null;
  replies: Reply[];           // tree roots (depth 0)
}

export interface TopicDetail {
  titleId: number;
  topicId: number;
  title: string;
  descriptionMd: string;
  createdAt: string;          // ISO string from API
  createdById: number;
  createdByName: string;
  posts: Post[];
}

/**
 * Topic (list view) — lightweight summary used by topics list.
 * Keep topicId (so it maps to TopicDetail.topicId).
 * Provide optional / fallback fields that the list UI expects.
 */
export interface Topic {
  topicId: number;
  title: string;
  preview?: string;          // short excerpt (optional)
  category?: string;
  isPinned?: boolean;
  author?: {
    name: string;
    avatar?: string;
  };
  replyCount?: number;
  viewCount?: number;
  likeCount?: number;
  lastActivity?: string | Date;
    
}
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  numberOfElements: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

export interface TitleDto {
  // identity
  titleId: number;
  topicId: number;

  // main content
  title: string;
  descriptionMd: string;

  // category / pin
  category?: string | null;        // if backend sends category name
  isPinned?: boolean | null;

  // creator
  createdByLoginId?: number | null;
  createdByName?: string | null;

  // activity / stats
  createdAt?: string | null;
  editedAt?: string | null;
  lastActivity?: string | null;     // used by your list UI
  replyCount?: number | null;
  viewCount?: number | null;

  // detail view loading (your toVM handles both array or page)
  posts?: Page<PostDto> | PostDto[] | null;
}
export interface TopicDetail {
  topicId: number;
  title: string;
  posts: Post[];
  // Added properties to resolve TS2339 errors
  category: string;
  isPinned: boolean;
  replyCount: number;
  viewCount: number;
  lastActivity: Date | string;
}
export interface TopicAuthor {
  name?: string;
  avatar?: string;
}
export interface TopicItem {
  topicId: number;
  titleId: number;
  title: string;
  descriptionMd: string;
  category?: string;
  isPinned?: boolean;
  lastActivity?: string | Date | null;
  replyCount?: number;
  viewCount?: number;
  author?: TopicAuthor;
  createdByName: string;
  createdByLoginId: number;
  preview?: string;
  __raw: any; // keep any to avoid circular imports
}


export type Category = 'LLC Formation' | 'Tax & Legal' | 'Business Growth' | 'Success Stories' | 'General';
