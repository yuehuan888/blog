import { api } from '~/api/index'
import type {
  Article,
  ArticleHistory,
  ArticleQueryParams,
  CategoryStat,
  HotArticleDTO,
  PageResult,
  Tag,
  ToggleResult,
} from '~/types'

export function createArticle(data: Partial<Article>) {
  return api.post<Article>('/api/articles', data)
}

export function createArticleBatch(data: Partial<Article>[]) {
  return api.post<void>('/api/articles/batch', data)
}

export function getArticles(params: ArticleQueryParams) {
  return api.get<PageResult<Article>>('/api/articles', params as Record<string, any>)
}

export function getArticleById(id: number) {
  return api.get<Article>(`/api/articles/${id}`)
}

export function updateArticle(id: number, data: Partial<Article>) {
  return api.put<Article>(`/api/articles/${id}`, data)
}

export function patchArticle(id: number, data: Partial<Article>) {
  return api.patch<Article>(`/api/articles/${id}`, data)
}

export function deleteArticle(id: number) {
  return api.delete<void>(`/api/articles/${id}`)
}

export function deleteArticles(ids: number[]) {
  return api.delete<void>('/api/articles', { ids: ids.join(',') })
}

export function getCategoryStatistics() {
  return api.get<CategoryStat[]>('/api/articles/statistics/category')
}

export function toggleLike(id: number) {
  return api.post<ToggleResult>(`/api/articles/${id}/like`)
}

export function toggleFavorite(id: number) {
  return api.post<ToggleResult>(`/api/articles/${id}/favorite`)
}

export function getArticleStats(id: number) {
  return api.get<Record<string, number>>(`/api/articles/${id}/stats`)
}

export function getArticleTags(id: number) {
  return api.get<Tag[]>(`/api/articles/${id}/tags`)
}

export function setArticleTags(id: number, tagIds: number[]) {
  return api.put<void>(`/api/articles/${id}/tags`, tagIds)
}

export function getHotArticles(days: number = 7, page: number = 1, size: number = 10) {
  return api.get<PageResult<HotArticleDTO>>('/api/articles/hot', { days, page, size })
}

export function getArticleHistory(id: number, page: number = 1, size: number = 10) {
  return api.get<PageResult<ArticleHistory>>(`/api/articles/${id}/history`, { page, size })
}

export function getArticleHistoryDetail(articleId: number, historyId: number) {
  return api.get<ArticleHistory>(`/api/articles/${articleId}/history/${historyId}`)
}

export function rollbackArticle(articleId: number, historyId: number) {
  return api.post<Article>(`/api/articles/${articleId}/rollback/${historyId}`)
}
