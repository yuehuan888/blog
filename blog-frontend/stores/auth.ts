import { defineStore } from 'pinia'
import * as authApi from '~/api/modules/auth'
import type { LoginRequest } from '~/types'

interface AuthUser {
  userId: number
  username: string
  role: string
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(null)
  const user = ref<AuthUser | null>(null)

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => user.value?.role === 'admin')

  function setAuth(t: string, u: AuthUser) {
    token.value = t
    user.value = u
    if (import.meta.client) {
      localStorage.setItem('token', t)
      localStorage.setItem('user', JSON.stringify(u))
    }
  }

  function clearAuth() {
    token.value = null
    user.value = null
    if (import.meta.client) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
    }
  }

  function restoreFromStorage() {
    if (import.meta.client) {
      const t = localStorage.getItem('token')
      const u = localStorage.getItem('user')
      if (t && u) {
        token.value = t
        try {
          user.value = JSON.parse(u)
        } catch {
          clearAuth()
        }
      }
    }
  }

  async function login(data: LoginRequest) {
    const res = await authApi.login(data)
    setAuth(res.token, {
      userId: res.userId,
      username: res.username,
      role: res.role,
    })
    return res
  }

  async function register(data: LoginRequest) {
    const res = await authApi.register(data)
    setAuth(res.token, {
      userId: res.userId,
      username: res.username,
      role: res.role,
    })
    return res
  }

  async function logout() {
    // Clear user's local draft before logging out
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

  // Restore on store creation (client-side only)
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
  }
})
