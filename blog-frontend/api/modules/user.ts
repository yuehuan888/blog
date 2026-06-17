import { api } from '~/api/index'
import type { UserProfile, FollowToggleResult, PageResult } from '~/types'

export function getUserProfile(userId: number) {
  return api.get<UserProfile>(`/api/users/${userId}`)
}

export function toggleFollow(userId: number) {
  return api.post<FollowToggleResult>(`/api/users/${userId}/follow`)
}

export function getFollowers(userId: number, page = 1, size = 20) {
  return api.get<PageResult<UserProfile>>(`/api/users/${userId}/followers`, { page, size })
}

export function getFollowing(userId: number, page = 1, size = 20) {
  return api.get<PageResult<UserProfile>>(`/api/users/${userId}/following`, { page, size })
}
