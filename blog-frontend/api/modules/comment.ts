import { api } from '~/api/index'
import type { Comment, CommentDTO, CommentForm, PageResult, ToggleResult } from '~/types'

export function createComment(data: CommentForm) {
  return api.post<Comment>('/api/comments', {
    articleId: data.articleId,
    parentId: data.parentId || null,
    replyTo: data.replyTo || null,
    content: data.content,
  })
}

export function getTopLevelComments(articleId: number, page: number = 1, size: number = 20, sort: string = 'time') {
  return api.get<PageResult<CommentDTO>>(`/api/articles/${articleId}/comments`, { page, size, sort })
}

export function getReplies(commentId: number, page: number = 1, size: number = 10) {
  return api.get<PageResult<CommentDTO>>(`/api/comments/${commentId}/replies`, { page, size })
}

export function toggleCommentLike(commentId: number) {
  return api.post<ToggleResult>(`/api/comments/${commentId}/like`)
}

export function deleteComment(commentId: number) {
  return api.delete<void>(`/api/comments/${commentId}`)
}

export function hideComment(commentId: number) {
  return api.put<void>(`/api/comments/${commentId}/hide`)
}
