import type { Result } from '~/types'
import { useAuthStore } from '~/stores/auth'

interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'
  body?: any
  params?: Record<string, any>
}

export async function apiRequest<T>(
  url: string,
  options: RequestOptions = {}
): Promise<T> {
  const config = useRuntimeConfig()
  const authStore = useAuthStore()

  const headers: Record<string, string> = {}

  if (options.body && typeof options.body === 'object' && !(options.body instanceof FormData)) {
    headers['Content-Type'] = 'application/json'
  }

  if (authStore.token) {
    headers['Authorization'] = `Bearer ${authStore.token}`
  }

  try {
    const result = await $fetch<Result<T>>(url, {
      baseURL: config.public.apiBase as string,
      method: options.method || 'GET',
      body: options.body,
      query: options.params,
      headers,
    })

    if (result.code !== 200) {
      if (result.code === 401) {
        authStore.clearAuth()
        await navigateTo('/user/login')
      }
      throw new Error(result.message || '请求失败')
    }

    return result.data as T
  } catch (err: any) {
    if (err?.response?.status === 401) {
      authStore.clearAuth()
      await navigateTo('/user/login')
      throw new Error('登录已过期，请重新登录')
    }
    if (err?.response?.status === 403) {
      throw new Error('没有权限执行此操作')
    }
    throw new Error(err?.message || '网络请求失败，请稍后重试')
  }
}

// Convenience methods
export const api = {
  get<T>(url: string, params?: Record<string, any>) {
    return apiRequest<T>(url, { method: 'GET', params })
  },
  post<T>(url: string, body?: any) {
    return apiRequest<T>(url, { method: 'POST', body })
  },
  put<T>(url: string, body?: any) {
    return apiRequest<T>(url, { method: 'PUT', body })
  },
  patch<T>(url: string, body?: any) {
    return apiRequest<T>(url, { method: 'PATCH', body })
  },
  delete<T>(url: string, params?: Record<string, any>) {
    return apiRequest<T>(url, { method: 'DELETE', params })
  },
}
