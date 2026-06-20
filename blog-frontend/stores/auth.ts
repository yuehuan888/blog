import { defineStore } from 'pinia'
import * as authApi from '~/api/modules/auth'
import type { LoginRequest } from '~/types'

interface AuthUser {
  userId: number
  username: string
  nickname: string
  avatar: string
  role: string
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(null)
  const user = ref<AuthUser | null>(null)

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => user.value?.role === 'admin')

  // ========== sessionStorage 封装 ==========
  // 用 sessionStorage 实现多窗口独立登录：
  // - 同一标签页刷新 → 保持登录
  // - 新开窗口/复制 URL → 独立 session，需重新登录
  function getStore(): Storage | null {
    if (!import.meta.client) return null
    return window.sessionStorage
  }

  function setItem(key: string, value: string) {
    getStore()?.setItem(key, value)
  }

  function getItem(key: string): string | null {
    return getStore()?.getItem(key) ?? null
  }

  function removeItem(key: string) {
    getStore()?.removeItem(key)
  }

  // ========== 核心方法 ==========

  function setAuth(t: string, u: AuthUser) {
    token.value = t
    user.value = u
    setItem('token', t)
    setItem('user', JSON.stringify(u))
  }

  function clearAuth() {
    token.value = null
    user.value = null
    removeItem('token')
    removeItem('user')
  }

  function restoreFromStorage() {
    const t = getItem('token')
    const u = getItem('user')
    if (t && u) {
      token.value = t
      try {
        user.value = JSON.parse(u)
      } catch {
        clearAuth()
      }
    }
  }

  async function login(data: LoginRequest) {
    const res = await authApi.login(data)
    setAuth(res.token, {
      userId: res.userId,
      username: res.username,
      nickname: res.nickname,
      avatar: res.avatar,
      role: res.role,
    })
    return res
  }

  async function register(data: LoginRequest) {
    const res = await authApi.register(data)
    setAuth(res.token, {
      userId: res.userId,
      username: res.username,
      nickname: res.nickname,
      avatar: res.avatar,
      role: res.role,
    })
    return res
  }

  async function logout() {
    // 清除该用户的本地草稿（localStorage 跨标签共享，保留 localStorage 读写）
    if (import.meta.client && user.value?.userId) {
      localStorage.removeItem(`article_draft_${user.value.userId}`)
    }
    try {
      await authApi.logout()
    } catch {
      // Even if logout API fails, clear local state
    }
    clearAuth()
  }

  /** 更新当前用户资料（昵称/头像），同步更新 sessionStorage */
  function updateUser(partial: { nickname?: string; avatar?: string }) {
    if (!user.value) return
    if (partial.nickname !== undefined) user.value.nickname = partial.nickname
    if (partial.avatar !== undefined) user.value.avatar = partial.avatar
    setItem('user', JSON.stringify(user.value))
  }

  // 客户端启动时恢复 sessionStorage 中的登录态
  restoreFromStorage()

  return {
    token,
    user,
    isLoggedIn,
    isAdmin,
    login,
    register,
    logout,
    clearAuth,
    restoreFromStorage,
    updateUser,
  }
})
