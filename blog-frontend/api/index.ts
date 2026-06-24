import type { Result } from '~/types'
import { useAuthStore } from '~/stores/auth'
import { $fetch } from 'ofetch'

// 后端英文错误消息 → 中文翻译映射
const ERROR_ZH: Record<string, string> = {
  'Invalid username or password': '用户名或密码错误',
  'Username already exists': '用户名已存在',
  'Article not found': '文章不存在',
  'Authentication required': '需要登录',
  'Only the article author or admin can delete': '只有文章作者或管理员可以删除',
  'Only the article author or admin can set tags': '只有文章作者或管理员可以设置标签',
  'Admin access required': '需要管理员权限',
  'Tag name already exists': '标签名已存在',
  'Tag not found': '标签不存在',
  'Comment not found': '评论不存在',
  'Parent comment not found': '父评论不存在',
  'Not authorized to delete this comment': '无权删除该评论',
  'Comment contains sensitive word': '评论包含敏感词',
  'Cannot follow yourself': '不能关注自己',
  'User not found': '用户不存在',
  'History not found': '版本历史不存在',
  'History does not belong to this article': '版本历史不属于该文章',
  'Redis not available': 'Redis 服务不可用',
  'File is empty': '文件为空',
  'Only image files are allowed': '只允许上传图片文件',
  'File size must be less than 2MB': '文件大小不能超过 2MB',
  'File size must be less than 5MB': '文件大小不能超过 5MB',
  'File size must be less than 200MB': '文件大小不能超过 200MB',
  'Only video files are allowed': '只允许上传视频文件',
  'Failed to save file': '文件保存失败',
  'Chunk upload failed': '分片上传失败',
  'Invalid uploadId or upload session expired': '上传会话已过期，请重新上传',
  'Internal server error': '服务器内部错误',
  'Missing or invalid Authorization header': '缺少或无效的认证信息',
  'Token expired': '登录已过期，请重新登录',
  'Token has been revoked': '登录已失效，请重新登录',
  'Invalid token': '认证信息无效',
}

/** 将后端英文错误消息转为中文，带动态内容（如ID）的自动拼接 */
function translateError(msg: string): string {
  if (!msg) return ''
  // 精确匹配
  if (ERROR_ZH[msg]) return ERROR_ZH[msg]
  // 带动态后缀的匹配（如 "Article not found: 123"）
  for (const [en, zh] of Object.entries(ERROR_ZH)) {
    if (msg.startsWith(en + ': ')) {
      return zh + ': ' + msg.slice(en.length + 2)
    }
    if (msg.startsWith(en + ' ')) {
      return zh + ' ' + msg.slice(en.length + 1)
    }
  }
  // 无匹配则返回原文
  return msg
}

interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'
  body?: any
  params?: Record<string, any>
  signal?: AbortSignal
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
      signal: options.signal,
    })

    if (result.code !== 200) {
      if (result.code === 401) {
        authStore.clearAuth()
        await navigateTo('/user/login')
      }
      throw new Error(translateError(result.message) || '请求失败')
    }

    return result.data as T
  } catch (err: any) {
    // err.message comes from our own throw (line 98) or ofetch's FetchError
    // err.data.message comes from ofetch FetchError (when $fetch itself throws on non-JSON)
    const rawMessage: string = err?.data?.message || err?.message || ''
    const apiMessage = translateError(rawMessage)
    const statusCode = err?.statusCode || err?.response?.status

    if (statusCode === 401) {
      authStore.clearAuth()
      await navigateTo('/user/login')
      throw new Error(apiMessage || '登录已过期，请重新登录')
    }
    if (statusCode === 403) {
      throw new Error(apiMessage || '没有权限执行此操作')
    }
    throw new Error(apiMessage || '网络请求失败，请稍后重试')
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
