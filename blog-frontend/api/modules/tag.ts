import { api } from '~/api/index'
import type { Tag, TagCloudItem } from '~/types'

export function createTag(name: string) {
  return api.post<Tag>('/api/tags', { name })
}

export function updateTag(id: number, name: string) {
  return api.put<Tag>(`/api/tags/${id}`, { name })
}

export function deleteTag(id: number) {
  return api.delete<void>(`/api/tags/${id}`)
}

export function getTagCloud(sort: 'count' | 'hot' = 'count') {
  return api.get<TagCloudItem[]>('/api/tags/cloud', { sort })
}
