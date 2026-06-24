import { api, apiRequest } from '~/api/index'
import type { DanmakuItem, ChunkedInitResponse } from '~/types'

/** Trigger AI video analysis. Returns immediately; analysis runs async. */
export function generateAiSummary(articleId: number): Promise<{ message: string; articleId: string }> {
  return api.post(`/api/articles/${articleId}/ai-summary`)
}

/** Upload complete video file (small files < 10MB). */
export async function uploadVideo(file: File, signal?: AbortSignal): Promise<{ videoUrl: string; objectKey: string }> {
  const formData = new FormData()
  formData.append('file', file)
  return apiRequest('/api/upload/video', { method: 'POST', body: formData, signal })
}

/** Upload video thumbnail. */
export async function uploadVideoThumbnail(file: File): Promise<string> {
  const formData = new FormData()
  formData.append('file', file)
  const result = await apiRequest<{ url: string }>('/api/upload/video-thumbnail', { method: 'POST', body: formData })
  return result.url
}

/** Init chunked upload — check dedup + get uploaded parts for resume. */
export async function initChunkedUpload(params: {
  fileHash: string
  fileName: string
  totalSize: number
  chunkSize: number
}): Promise<ChunkedInitResponse> {
  return apiRequest('/api/upload/video/init', { method: 'POST', body: params })
}

/** Upload a single chunk. */
export async function uploadChunk(
  uploadId: string,
  chunkIndex: number,
  chunk: Blob,
  signal?: AbortSignal,
): Promise<{ chunkIndex: number; etag: string }> {
  const formData = new FormData()
  formData.append('uploadId', uploadId)
  formData.append('chunkIndex', String(chunkIndex))
  formData.append('chunk', chunk)
  return apiRequest('/api/upload/video/chunk', { method: 'POST', body: formData, signal })
}

/** Complete chunked upload — merge all parts. */
export async function completeChunkedUpload(uploadId: string, fileHash: string): Promise<{ videoUrl: string; objectKey: string }> {
  return apiRequest('/api/upload/video/complete', { method: 'POST', body: { uploadId, fileHash } })
}

/** Get danmaku history for a video. */
export function getDanmaku(videoId: number, since: number = 0) {
  return api.get<DanmakuItem[]>(`/api/videos/${videoId}/danmaku`, { since })
}

/** Send a danmaku (HTTP fallback). */
export function sendDanmaku(
  videoId: number,
  data: { content: string; timestampSec: number; color?: string; mode?: string },
) {
  return api.post<DanmakuItem>(`/api/videos/${videoId}/danmaku`, {
    content: data.content,
    timestampSec: data.timestampSec,
    color: data.color || '#FFFFFF',
    mode: data.mode || 'scroll',
  })
}

/** Compute SHA-256 hash of a file.
 * Uses the entire file in one pass — Web Crypto handles up to ~500MB safely. */
export async function computeFileHash(file: File): Promise<string> {
  const buffer = await file.arrayBuffer()
  const hash = await crypto.subtle.digest('SHA-256', buffer)
  return Array.from(new Uint8Array(hash))
    .map(b => b.toString(16).padStart(2, '0'))
    .join('')
}
