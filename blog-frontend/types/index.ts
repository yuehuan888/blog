// ========== Entities ==========

export interface Article {
  id: number
  title: string
  content: string
  category: string
  status: 'draft' | 'published'
  likeCount: number
  favoriteCount: number
  readCount: number
  commentCount: number
  authorId: number | null
  createdAt: string
  updatedAt: string
  coverImage: string | null
  images: string[]
  imageCount: number
}

export interface User {
  id: number
  username: string
  nickname: string
  avatar: string
  password?: string
  role: 'admin' | 'user'
  createdAt: string
}

export interface Comment {
  id: number
  articleId: number
  userId: number
  parentId: number | null
  replyTo: number | null
  content: string
  likeCount: number
  status: 'visible' | 'hidden' | 'deleted'
  createdAt: string
  liked: boolean
  userNickname: string
  userAvatar: string
}

export interface CommentDTO extends Comment {
  replies: CommentDTO[]
  replyCount: number
}

export interface Tag {
  id: number
  name: string
  articleCount: number
  hotScore: number
}

export interface ArticleHistory {
  id: number
  articleId: number
  title: string
  content: string
  category: string
  versionNo: number
  changeType: 'UPDATE' | 'ROLLBACK'
  createdAt: string
}

// ========== DTOs ==========

export interface Result<T> {
  code: number
  message: string
  data: T
}

export interface ToggleResult {
  liked: boolean
  count: number
}

export interface LoginResponse {
  token: string
  userId: number
  username: string
  nickname: string
  avatar: string
  role: string
}

export interface LoginRequest {
  username: string
  password: string
  nickname?: string
  avatar?: string
}

export interface ArticleQueryParams {
  page?: number
  size?: number
  category?: string
  status?: string
  keyword?: string
  tagId?: number
  authorId?: number
}

export interface HotArticleDTO {
  articleId: number
  title: string
  readCount: number
}

export interface TagCloudItem {
  id: number
  name: string
  articleCount: number
  hotScore: number
}

// ========== Pagination ==========

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

// ========== Category Statistics ==========

export interface CategoryStat {
  [category: string]: number
}

// ========== Comment Form ==========

export interface UserProfile extends User {
  followerCount: number
  followingCount: number
  articleCount: number
  followed: boolean
}

export interface FollowToggleResult {
  liked: boolean
  count: number
}

export interface CommentForm {
  articleId: number
  parentId?: number | null
  replyTo?: number | null
  content: string
}

// ========== Video Types ==========

export interface Video extends Article {
  type: 'video'
  videoUrl: string
  thumbnailUrl: string | null
  duration: number | null
  aiSummary: string | null
  transcodeStatus: string | null
}

export type FeedItem = Article | Video

export interface DanmakuItem {
  id: number
  articleId: number
  userId: number
  nickname?: string
  content: string
  timestampSec: number
  color: string
  mode: 'scroll' | 'top' | 'bottom'
  createdAt: string
}

export interface ChunkedUploadState {
  uploadId: string
  fileHash: string
  fileName: string
  totalChunks: number
  completedChunks: number[]
  objectKey: string | null
  videoUrl: string | null
  timestamp: number
}

export interface ChunkedInitResponse {
  uploadId: string
  objectKey: string
  totalChunks: number
  chunkSize: number
  uploadedChunks: number[]
  uploaded: boolean
  videoUrl?: string
}
