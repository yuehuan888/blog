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
  authorId: number
  createdAt: string
  updatedAt: string
}

export interface User {
  id: number
  username: string
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
  role: string
}

export interface LoginRequest {
  username: string
  password: string
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

export interface CommentForm {
  articleId: number
  parentId?: number | null
  replyTo?: number | null
  content: string
}
