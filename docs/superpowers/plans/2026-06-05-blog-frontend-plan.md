# Blog Frontend Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a complete Nuxt 3 frontend for the blog backend — a public social content platform (Xiaohongshu-style) with article browsing, user auth, comments, likes, tags, hot rankings, and admin management.

**Architecture:** Nuxt 3 with SSR/SSG for public pages and CSR for authenticated pages. Naive UI component library with Tailwind CSS for layout utilities. Pinia stores for auth and UI state. API layer wraps `$fetch` with JWT interceptor and unified error handling. Four implementation phases: P1 (foundation + browsing), P2 (creation + interaction), P3 (discovery), P4 (advanced features).

**Tech Stack:** Nuxt 3, TypeScript, Naive UI, Tailwind CSS, Pinia, $fetch (ofetch)

**Backend API Base:** `http://localhost:8080` (configurable via `nuxt.config.ts`)

---

### Task 1: Project Scaffolding

**Files:**
- Create: `blog-frontend/package.json`
- Create: `blog-frontend/nuxt.config.ts`
- Create: `blog-frontend/tailwind.config.ts`
- Create: `blog-frontend/tsconfig.json`
- Create: `blog-frontend/app.vue`
- Create: `blog-frontend/.gitignore`

- [ ] **Step 1: Initialize Nuxt 3 project**

Run: `npx nuxi@latest init blog-frontend`

When prompted, choose: TypeScript = Yes, package manager = npm. This creates the base project structure.

- [ ] **Step 2: Install additional dependencies**

Run:
```bash
cd blog-frontend
npm install naive-ui @vicons/ionicons5 pinia @pinia/nuxt
npm install -D tailwindcss @tailwindcss/typography autoprefixer @css-poodle/skeleton
```

- [ ] **Step 3: Write `package.json` final state**

```json
{
  "name": "blog-frontend",
  "private": true,
  "type": "module",
  "scripts": {
    "build": "nuxt build",
    "dev": "nuxt dev",
    "generate": "nuxt generate",
    "preview": "nuxt preview",
    "postinstall": "nuxt prepare"
  },
  "dependencies": {
    "@pinia/nuxt": "^0.5.4",
    "@vicons/ionicons5": "^0.12.0",
    "naive-ui": "^2.39.0",
    "nuxt": "^3.13.0",
    "pinia": "^2.2.4",
    "vue": "latest"
  },
  "devDependencies": {
    "@tailwindcss/typography": "^0.5.15",
    "autoprefixer": "^10.4.20",
    "tailwindcss": "^3.4.13",
    "typescript": "^5.6.0"
  }
}
```

- [ ] **Step 4: Write `nuxt.config.ts`**

```typescript
// https://nuxt.com/docs/api/configuration/nuxt-config
export default defineNuxtConfig({
  devtools: { enabled: true },
  
  modules: [
    '@pinia/nuxt',
  ],

  css: [
    '~/assets/css/main.css',
  ],

  postcss: {
    plugins: {
      tailwindcss: {},
      autoprefixer: {},
    },
  },

  runtimeConfig: {
    public: {
      apiBase: 'http://localhost:8080',
    },
  },

  app: {
    head: {
      title: 'GreenRead - 发现好内容',
      meta: [
        { charset: 'utf-8' },
        { name: 'viewport', content: 'width=device-width, initial-scale=1' },
        { name: 'description', content: '一个开放的内容分享社区，发现好内容，记录生活美好' },
      ],
    },
  },

  typescript: {
    strict: true,
    typeCheck: true,
  },

  compatibilityDate: '2024-11-01',
})
```

- [ ] **Step 5: Write `tailwind.config.ts`**

```typescript
import type { Config } from 'tailwindcss'

export default {
  content: [
    './components/**/*.{vue,js,ts}',
    './pages/**/*.{vue,js,ts}',
    './layouts/**/*.{vue,js,ts}',
    './app.vue',
  ],
  theme: {
    extend: {
      colors: {
        primary: {
          DEFAULT: '#2D6A4F',
          light: '#52B788',
          pale: '#E8F5E9',
        },
        surface: '#FFFFFF',
        background: '#F0F7F4',
        'text-primary': '#1A1A2E',
        'text-secondary': '#6B7280',
        accent: '#FF6B6B',
      },
      borderRadius: {
        card: '12px',
        tag: '20px',
      },
      fontFamily: {
        sans: ['PingFang SC', 'Microsoft YaHei', 'sans-serif'],
      },
    },
  },
  plugins: [
    require('@tailwindcss/typography'),
  ],
} satisfies Config
```

- [ ] **Step 6: Write `tsconfig.json`**

```json
{
  "extends": "./.nuxt/tsconfig.json",
  "compilerOptions": {
    "strict": true,
    "types": ["naive-ui/volar"]
  }
}
```

- [ ] **Step 7: Write `app.vue` (Naive UI wrapper)**

```vue
<template>
  <NConfigProvider :theme-overrides="themeOverrides" :locale="zhCN" :date-locale="dateZhCN">
    <NMessageProvider>
      <NDialogProvider>
        <NuxtLayout>
          <NuxtPage />
        </NuxtLayout>
      </NDialogProvider>
    </NMessageProvider>
  </NConfigProvider>
</template>

<script setup lang="ts">
import { NConfigProvider, NMessageProvider, NDialogProvider, zhCN, dateZhCN } from 'naive-ui'
import type { GlobalThemeOverrides } from 'naive-ui'

const themeOverrides: GlobalThemeOverrides = {
  common: {
    primaryColor: '#2D6A4F',
    primaryColorHover: '#52B788',
    primaryColorPressed: '#1B4332',
    primaryColorSuppl: '#52B788',
    borderRadius: '8px',
  },
  Button: {
    borderRadiusMedium: '8px',
  },
  Card: {
    borderRadius: '12px',
  },
  Tag: {
    borderRadius: '20px',
  },
}
</script>
```

- [ ] **Step 8: Write `.gitignore`**

```
# Nuxt
.output/
.data/
.nuxt/
dist/

# Node
node_modules/

# IDE
.idea/
*.swp
*.swo

# OS
.DS_Store
Thumbs.db

# Env
.env
.env.local

# Logs
*.log
```

- [ ] **Step 9: Install and verify**

Run:
```bash
cd blog-frontend
npm install
npx nuxt dev
```

Expected: Dev server starts, visit http://localhost:3000 shows a blank page with no errors. Naive UI config provider wraps the app.

---

### Task 2: TypeScript Types

**Files:**
- Create: `blog-frontend/types/index.ts`

- [ ] **Step 1: Write all TypeScript interfaces**

Create `blog-frontend/types/index.ts`:

```typescript
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
```

- [ ] **Step 2: Verify types compile**

Run: `cd blog-frontend && npx nuxi typecheck`
Expected: No type errors.

---

### Task 3: Theme CSS Variables & Global Styles

**Files:**
- Create: `blog-frontend/assets/css/main.css`

- [ ] **Step 1: Write global CSS with Forest Green theme**

Create `blog-frontend/assets/css/main.css`:

```css
@tailwind base;
@tailwind components;
@tailwind utilities;

:root {
  --color-primary: #2D6A4F;
  --color-primary-light: #52B788;
  --color-primary-pale: #E8F5E9;
  --color-background: #F0F7F4;
  --color-surface: #FFFFFF;
  --color-text-primary: #1A1A2E;
  --color-text-secondary: #6B7280;
  --color-accent: #FF6B6B;
  --radius-card: 12px;
  --radius-button: 8px;
  --radius-tag: 20px;
  --radius-input: 8px;
  --font-sans: 'PingFang SC', 'Microsoft YaHei', -apple-system, BlinkMacSystemFont, sans-serif;
}

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html {
  font-family: var(--font-sans);
  color: var(--color-text-primary);
  background-color: var(--color-background);
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

body {
  min-height: 100vh;
}

a {
  color: inherit;
  text-decoration: none;
}

/* Prose overrides for article content */
.prose {
  max-width: 100% !important;
}

.prose img {
  border-radius: var(--radius-card);
}

/* Scrollbar styling */
::-webkit-scrollbar {
  width: 6px;
}

::-webkit-scrollbar-track {
  background: transparent;
}

::-webkit-scrollbar-thumb {
  background: #c0c0c0;
  border-radius: 3px;
}

::-webkit-scrollbar-thumb:hover {
  background: #a0a0a0;
}

/* Skeleton animation for loading states */
@keyframes shimmer {
  0% { background-position: -200% 0; }
  100% { background-position: 200% 0; }
}

.skeleton {
  background: linear-gradient(90deg, #e8f5e9 25%, #f0f7f4 50%, #e8f5e9 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  border-radius: var(--radius-card);
}
```

- [ ] **Step 2: Verify CSS loads**

Run: `cd blog-frontend && npx nuxt dev`
Visit http://localhost:3000, inspect page: `<html>` tag should have `font-family: PingFang SC...`.
Expected: Page background is #F0F7F4 green-tinted, no console errors.

---

### Task 4: Layout Components (Header, Footer, Default Layout)

**Files:**
- Create: `blog-frontend/layouts/default.vue`
- Create: `blog-frontend/components/layout/AppHeader.vue`
- Create: `blog-frontend/components/layout/AppFooter.vue`

- [ ] **Step 1: Write `AppHeader.vue`**

```vue
<template>
  <header class="sticky top-0 z-50 bg-white/80 backdrop-blur-md border-b border-gray-100">
    <div class="max-w-6xl mx-auto px-4 h-14 flex items-center justify-between">
      <!-- Logo -->
      <NuxtLink to="/" class="flex items-center gap-2 text-primary font-bold text-lg no-underline">
        <span class="text-xl">🌿</span>
        <span>GreenRead</span>
      </NuxtLink>

      <!-- Nav Links -->
      <nav class="hidden md:flex items-center gap-6">
        <NuxtLink to="/" class="text-sm text-text-secondary hover:text-primary transition-colors">
          首页
        </NuxtLink>
        <NuxtLink to="/tag/index" class="text-sm text-text-secondary hover:text-primary transition-colors">
          标签云
        </NuxtLink>
        <NuxtLink to="/article/hot" class="text-sm text-text-secondary hover:text-primary transition-colors">
          🔥 热门
        </NuxtLink>
      </nav>

      <!-- Right Actions -->
      <div class="flex items-center gap-3">
        <template v-if="authStore.isLoggedIn">
          <NButton text @click="navigateTo('/article/write')">
            <template #icon>
              <Icon><PencilOutline /></Icon>
            </template>
            写文章
          </NButton>
          <NDropdown trigger="click" :options="userMenuOptions" @select="handleUserMenu">
            <NButton quaternary circle>
              <template #icon>
                <Icon size="20"><PersonCircleOutline /></Icon>
              </template>
            </NButton>
          </NDropdown>
        </template>
        <template v-else>
          <NButton text @click="navigateTo('/user/login')">登录</NButton>
          <NButton type="primary" size="small" @click="navigateTo('/user/register')">注册</NButton>
        </template>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { NButton, NDropdown, Icon } from 'naive-ui'
import { PencilOutline, PersonCircleOutline } from '@vicons/ionicons5'
import { useAuthStore } from '~/stores/auth'

const authStore = useAuthStore()

const userMenuOptions = computed(() => {
  const options: any[] = [
    {
      label: '个人主页',
      key: 'profile',
    },
  ]
  if (authStore.isAdmin) {
    options.push(
      { label: '标签管理', key: 'admin-tags' },
      { label: '评论管理', key: 'admin-comments' },
    )
  }
  options.push(
    { type: 'divider' as const, key: 'd1' },
    { label: '退出登录', key: 'logout' },
  )
  return options
})

function handleUserMenu(key: string) {
  switch (key) {
    case 'profile':
      navigateTo(`/user/${authStore.user!.userId}`)
      break
    case 'admin-tags':
      navigateTo('/admin/tags')
      break
    case 'admin-comments':
      navigateTo('/admin/comments')
      break
    case 'logout':
      authStore.logout()
      navigateTo('/')
      break
  }
}
</script>
```

- [ ] **Step 2: Write `AppFooter.vue`**

```vue
<template>
  <footer class="border-t border-gray-100 bg-white mt-12">
    <div class="max-w-6xl mx-auto px-4 py-8">
      <div class="flex flex-col md:flex-row items-center justify-between gap-4">
        <div class="flex items-center gap-2 text-primary font-bold">
          <span>🌿</span>
          <span>GreenRead</span>
        </div>
        <p class="text-xs text-text-secondary">
          © {{ new Date().getFullYear() }} GreenRead. 记录生活，发现美好。
        </p>
        <div class="flex gap-4 text-xs text-text-secondary">
          <NuxtLink to="/" class="hover:text-primary">首页</NuxtLink>
          <NuxtLink to="/tag/index" class="hover:text-primary">标签</NuxtLink>
          <NuxtLink to="/article/hot" class="hover:text-primary">热门</NuxtLink>
        </div>
      </div>
    </div>
  </footer>
</template>
```

- [ ] **Step 3: Write `layouts/default.vue`**

```vue
<template>
  <div class="min-h-screen flex flex-col">
    <AppHeader />
    <main class="flex-1">
      <slot />
    </main>
    <AppFooter />
  </div>
</template>
```

- [ ] **Step 4: Test layout renders**

Run: `cd blog-frontend && npx nuxt dev`
Visit http://localhost:3000
Expected: Header with logo and login/register buttons visible (no auth yet). Footer visible. No console errors.

---

### Task 5: API Layer — Request Wrapper

**Files:**
- Create: `blog-frontend/api/index.ts`

- [ ] **Step 1: Write the API request wrapper**

Create `blog-frontend/api/index.ts`:

```typescript
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

  if (options.body && typeof options.body === 'object') {
    headers['Content-Type'] = 'application/json'
  }

  if (authStore.token) {
    headers['Authorization'] = `Bearer ${authStore.token}`
  }

  try {
    const result = await $fetch<Result<T>>(url, {
      baseURL: config.public.apiBase as string,
      method: options.method || 'GET',
      body: options.body ? JSON.stringify(options.body) : undefined,
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
    // Handle network errors (err.response doesn't exist for network failures)
    if (err?.response?.status === 401) {
      authStore.clearAuth()
      await navigateTo('/user/login')
      throw new Error('登录已过期，请重新登录')
    }
    if (err?.response?.status === 403) {
      throw new Error('没有权限执行此操作')
    }
    // Re-throw with user-friendly message
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
```

---

### Task 6: API Modules — Auth, Article, Comment, Tag

**Files:**
- Create: `blog-frontend/api/modules/auth.ts`
- Create: `blog-frontend/api/modules/article.ts`
- Create: `blog-frontend/api/modules/comment.ts`
- Create: `blog-frontend/api/modules/tag.ts`

- [ ] **Step 1: Write `api/modules/auth.ts`**

```typescript
import { api } from '~/api/index'
import type { LoginRequest, LoginResponse } from '~/types'

export function register(data: LoginRequest) {
  return api.post<LoginResponse>('/api/auth/register', data)
}

export function login(data: LoginRequest) {
  return api.post<LoginResponse>('/api/auth/login', data)
}

export function logout() {
  return api.post<void>('/api/auth/logout')
}
```

- [ ] **Step 2: Write `api/modules/article.ts`**

```typescript
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
```

- [ ] **Step 3: Write `api/modules/comment.ts`**

```typescript
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
```

- [ ] **Step 4: Write `api/modules/tag.ts`**

```typescript
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
```

---

### Task 7: Auth Store (Pinia)

**Files:**
- Create: `blog-frontend/stores/auth.ts`

- [ ] **Step 1: Write the auth Pinia store**

Create `blog-frontend/stores/auth.ts`:

```typescript
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
```

- [ ] **Step 2: Verify store works**

Add a temporary test to `pages/index.vue`:
```vue
<template>
  <div>
    <p>登录状态: {{ auth.isLoggedIn }}</p>
    <p>管理员: {{ auth.isAdmin }}</p>
  </div>
</template>

<script setup lang="ts">
const auth = useAuthStore()
</script>
```

Run: `cd blog-frontend && npx nuxt dev`
Visit http://localhost:3000
Expected: Shows "登录状态: false", "管理员: false". No errors. Remove test code after verification.

---

### Task 8: Auth Middleware

**Files:**
- Create: `blog-frontend/middleware/auth.ts`
- Create: `blog-frontend/middleware/guest.ts`
- Create: `blog-frontend/middleware/admin.ts`

- [ ] **Step 1: Write `middleware/auth.ts` (require login)**

```typescript
export default defineNuxtRouteMiddleware(() => {
  const { isLoggedIn } = useAuthStore()

  if (!isLoggedIn.value) {
    return navigateTo('/user/login')
  }
})
```

- [ ] **Step 2: Write `middleware/guest.ts` (only for non-logged-in users)**

```typescript
export default defineNuxtRouteMiddleware(() => {
  const { isLoggedIn } = useAuthStore()

  if (isLoggedIn.value) {
    return navigateTo('/')
  }
})
```

- [ ] **Step 3: Write `middleware/admin.ts` (require admin role)**

```typescript
export default defineNuxtRouteMiddleware(() => {
  const { isAdmin } = useAuthStore()

  if (!isAdmin.value) {
    throw createError({ statusCode: 403, message: '需要管理员权限' })
  }
})
```

---

### Task 9: Auth Pages (Login & Register)

**Files:**
- Create: `blog-frontend/pages/user/login.vue`
- Create: `blog-frontend/pages/user/register.vue`

- [ ] **Step 1: Write `pages/user/login.vue`**

```vue
<template>
  <div class="min-h-[80vh] flex items-center justify-center px-4">
    <NCard class="w-full max-w-md" title="登录 GreenRead">
      <NForm ref="formRef" :model="form" :rules="rules" @submit.prevent="handleLogin">
        <NFormItem label="用户名" path="username">
          <NInput v-model:value="form.username" placeholder="请输入用户名" size="large" />
        </NFormItem>
        <NFormItem label="密码" path="password">
          <NInput
            v-model:value="form.password"
            type="password"
            placeholder="请输入密码"
            size="large"
            @keyup.enter="handleLogin"
          />
        </NFormItem>
        <NButton
          type="primary"
          block
          size="large"
          :loading="loading"
          @click="handleLogin"
        >
          登录
        </NButton>
      </NForm>
      <template #footer>
        <div class="text-center text-sm text-text-secondary">
          还没有账号？
          <NuxtLink to="/user/register" class="text-primary font-medium">立即注册</NuxtLink>
        </div>
      </template>
    </NCard>
  </div>
</template>

<script setup lang="ts">
import { NCard, NForm, NFormItem, NInput, NButton, useMessage } from 'naive-ui'
import type { FormInst, FormRules } from 'naive-ui'
import { useAuthStore } from '~/stores/auth'

definePageMeta({
  middleware: 'guest',
})

const authStore = useAuthStore()
const message = useMessage()
const router = useRouter()

const formRef = ref<FormInst | null>(null)
const loading = ref(false)
const form = reactive({
  username: '',
  password: '',
})

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, message: '用户名至少2个字符', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6个字符', trigger: 'blur' },
  ],
}

async function handleLogin() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }

  loading.value = true
  try {
    await authStore.login({
      username: form.username,
      password: form.password,
    })
    message.success('登录成功')
    router.push('/')
  } catch (err: any) {
    message.error(err.message || '登录失败，请检查用户名和密码')
  } finally {
    loading.value = false
  }
}
</script>
```

- [ ] **Step 2: Write `pages/user/register.vue`**

```vue
<template>
  <div class="min-h-[80vh] flex items-center justify-center px-4">
    <NCard class="w-full max-w-md" title="注册 GreenRead">
      <NForm ref="formRef" :model="form" :rules="rules" @submit.prevent="handleRegister">
        <NFormItem label="用户名" path="username">
          <NInput v-model:value="form.username" placeholder="请输入用户名" size="large" />
        </NFormItem>
        <NFormItem label="密码" path="password">
          <NInput
            v-model:value="form.password"
            type="password"
            placeholder="请设置密码（至少6位）"
            size="large"
          />
        </NFormItem>
        <NFormItem label="确认密码" path="confirmPassword">
          <NInput
            v-model:value="form.confirmPassword"
            type="password"
            placeholder="请再次输入密码"
            size="large"
            @keyup.enter="handleRegister"
          />
        </NFormItem>
        <NButton
          type="primary"
          block
          size="large"
          :loading="loading"
          @click="handleRegister"
        >
          注册
        </NButton>
      </NForm>
      <template #footer>
        <div class="text-center text-sm text-text-secondary">
          已有账号？
          <NuxtLink to="/user/login" class="text-primary font-medium">立即登录</NuxtLink>
        </div>
      </template>
    </NCard>
  </div>
</template>

<script setup lang="ts">
import { NCard, NForm, NFormItem, NInput, NButton, useMessage } from 'naive-ui'
import type { FormInst, FormRules } from 'naive-ui'
import { useAuthStore } from '~/stores/auth'

definePageMeta({
  middleware: 'guest',
})

const authStore = useAuthStore()
const message = useMessage()
const router = useRouter()

const formRef = ref<FormInst | null>(null)
const loading = ref(false)
const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
})

function validateConfirmPassword(_rule: any, value: string) {
  if (value !== form.password) {
    return new Error('两次输入的密码不一致')
  }
  return true
}

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, message: '用户名至少2个字符', trigger: 'blur' },
    { max: 20, message: '用户名最多20个字符', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请设置密码', trigger: 'blur' },
    { min: 6, message: '密码至少6个字符', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' },
  ],
}

async function handleRegister() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }

  loading.value = true
  try {
    await authStore.register({
      username: form.username,
      password: form.password,
    })
    message.success('注册成功！欢迎加入 GreenRead')
    router.push('/')
  } catch (err: any) {
    message.error(err.message || '注册失败，请稍后重试')
  } finally {
    loading.value = false
  }
}
</script>
```

---

### Task 10: UI Store & Common Components

**Files:**
- Create: `blog-frontend/stores/ui.ts`
- Create: `blog-frontend/components/common/EmptyState.vue`
- Create: `blog-frontend/components/common/UserAvatar.vue`
- Create: `blog-frontend/components/common/TagBadge.vue`

- [ ] **Step 1: Write `stores/ui.ts`**

```typescript
export const useUiStore = defineStore('ui', () => {
  const scrollPosition = ref(0)

  function setScrollPosition(pos: number) {
    scrollPosition.value = pos
  }

  return {
    scrollPosition,
    setScrollPosition,
  }
})
```

- [ ] **Step 2: Write `components/common/EmptyState.vue`**

```vue
<template>
  <NEmpty :description="description" size="large">
    <template #extra>
      <NButton v-if="actionLabel" type="primary" @click="$emit('action')">
        {{ actionLabel }}
      </NButton>
    </template>
  </NEmpty>
</template>

<script setup lang="ts">
import { NEmpty, NButton } from 'naive-ui'

defineProps<{
  description?: string
  actionLabel?: string
}>()

defineEmits<{
  action: []
}>()
</script>
```

- [ ] **Step 3: Write `components/common/UserAvatar.vue`**

```vue
<template>
  <NAvatar :size="size" round :style="style">
    {{ username?.charAt(0)?.toUpperCase() || '?' }}
  </NAvatar>
</template>

<script setup lang="ts">
import { NAvatar } from 'naive-ui'

const props = withDefaults(defineProps<{
  username?: string
  size?: 'small' | 'medium' | 'large'
}>(), {
  username: '',
  size: 'medium',
})

const sizeMap: Record<string, number> = {
  small: 28,
  medium: 36,
  large: 48,
}

const style = computed(() => ({
  backgroundColor: '#2D6A4F',
  color: '#fff',
  width: `${sizeMap[props.size]}px`,
  height: `${sizeMap[props.size]}px`,
  fontSize: props.size === 'small' ? '12px' : '14px',
}))
</script>
```

- [ ] **Step 4: Write `components/common/TagBadge.vue`**

```vue
<template>
  <NTag
    :type="clickable ? 'success' : 'default'"
    :bordered="false"
    :class="{ 'cursor-pointer hover:opacity-80': clickable }"
    @click="clickable && navigateTo(`/tag/${tag.id}`)"
  >
    {{ tag.name }}
    <template v-if="showCount">
      <span class="ml-1 opacity-60">({{ tag.articleCount }})</span>
    </template>
  </NTag>
</template>

<script setup lang="ts">
import { NTag } from 'naive-ui'
import type { Tag } from '~/types'

withDefaults(defineProps<{
  tag: Tag
  clickable?: boolean
  showCount?: boolean
}>(), {
  clickable: true,
  showCount: false,
})
</script>
```

---

### Task 11: ArticleCard Component

**Files:**
- Create: `blog-frontend/components/article/ArticleCard.vue`

- [ ] **Step 1: Write `ArticleCard.vue`**

```vue
<template>
  <NCard
    class="article-card overflow-hidden transition-all duration-300 hover:shadow-md hover:-translate-y-1"
    :bordered="false"
    size="small"
    @click="navigateTo(`/article/${article.id}`)"
  >
    <!-- Cover Image Placeholder -->
    <div
      class="h-32 bg-gradient-to-br from-primary-pale to-primary/10 flex items-center justify-center text-4xl"
    >
      {{ coverEmoji }}
    </div>

    <div class="p-3">
      <!-- Title -->
      <h3 class="text-sm font-bold leading-snug line-clamp-2 mb-1 text-text-primary">
        {{ article.title }}
      </h3>

      <!-- Category Tag -->
      <NTag
        v-if="article.category"
        size="tiny"
        :bordered="false"
        class="mb-2"
      >
        {{ article.category }}
      </NTag>

      <!-- Meta -->
      <div class="flex items-center justify-between text-xs text-text-secondary">
        <div class="flex items-center gap-3">
          <span class="flex items-center gap-1">
            <Icon size="14"><EyeOutline /></Icon>
            {{ formatCount(article.readCount) }}
          </span>
          <span class="flex items-center gap-1">
            <Icon size="14"><HeartOutline /></Icon>
            {{ formatCount(article.likeCount) }}
          </span>
          <span class="flex items-center gap-1">
            <Icon size="14"><ChatbubbleOutline /></Icon>
            {{ formatCount(article.commentCount) }}
          </span>
        </div>
      </div>
    </div>
  </NCard>
</template>

<script setup lang="ts">
import { NCard, NTag, Icon } from 'naive-ui'
import { EyeOutline, HeartOutline, ChatbubbleOutline } from '@vicons/ionicons5'
import type { Article } from '~/types'

const props = defineProps<{
  article: Article
}>()

const emojis = ['📝', '🌿', '📷', '🎨', '🍃', '✨', '📖', '🌸', '🌲', '🖋️']

const coverEmoji = computed(() => {
  const hash = props.article.title.split('').reduce((a, c) => a + c.charCodeAt(0), 0)
  return emojis[hash % emojis.length]
})

function formatCount(n: number): string {
  if (n >= 10000) return (n / 10000).toFixed(1) + 'w'
  if (n >= 1000) return (n / 1000).toFixed(1) + 'k'
  return String(n)
}
</script>
```

---

### Task 12: Home Page (index.vue)

**Files:**
- Create: `blog-frontend/pages/index.vue`
- Create: `blog-frontend/components/article/HotScroll.vue`

- [ ] **Step 1: Write `components/article/HotScroll.vue`**

```vue
<template>
  <div v-if="articles.length > 0" class="mb-6">
    <div class="flex items-center gap-2 mb-3">
      <span class="text-lg">🔥</span>
      <h3 class="text-base font-bold">热门推荐</h3>
      <NuxtLink to="/article/hot" class="text-xs text-primary ml-auto hover:underline">
        查看全部 →
      </NuxtLink>
    </div>
    <div class="flex gap-3 overflow-x-auto pb-2 scrollbar-hide">
      <NCard
        v-for="item in articles"
        :key="item.articleId"
        size="small"
        :bordered="false"
        class="flex-shrink-0 w-40 cursor-pointer border-l-4 border-primary-light hover:shadow-md transition-shadow"
        @click="navigateTo(`/article/${item.articleId}`)"
      >
        <p class="text-sm font-medium line-clamp-2 text-text-primary">{{ item.title }}</p>
        <p class="text-xs text-text-secondary mt-1">👁 {{ item.readCount }} 次阅读</p>
      </NCard>
    </div>
  </div>
</template>

<script setup lang="ts">
import { NCard } from 'naive-ui'
import type { HotArticleDTO } from '~/types'

defineProps<{
  articles: HotArticleDTO[]
}>()
</script>

<style scoped>
.scrollbar-hide::-webkit-scrollbar {
  display: none;
}
.scrollbar-hide {
  -ms-overflow-style: none;
  scrollbar-width: none;
}
</style>
```

- [ ] **Step 2: Write `pages/index.vue`**

```vue
<template>
  <div class="max-w-6xl mx-auto px-4 py-6">
    <!-- Banner -->
    <div
      class="rounded-2xl p-8 mb-6 text-white"
      style="background: linear-gradient(135deg, #2D6A4F 0%, #52B788 100%);"
    >
      <h1 class="text-2xl md:text-3xl font-bold mb-2">发现好内容 🌿</h1>
      <p class="text-white/80 text-sm md:text-base">
        每日精选 · 热门话题 · 创作你的故事
      </p>
    </div>

    <!-- Hot Articles Horizontal Scroll -->
    <HotScroll :articles="hotArticles" />

    <!-- Tag Filter Row -->
    <div class="flex items-center gap-2 mb-6 flex-wrap">
      <NTag
        :type="activeTagId === null ? 'success' : 'default'"
        :bordered="false"
        class="cursor-pointer"
        @click="activeTagId = null"
      >
        全部
      </NTag>
      <NTag
        v-for="tag in tags"
        :key="tag.id"
        :type="activeTagId === tag.id ? 'success' : 'default'"
        :bordered="false"
        class="cursor-pointer"
        @click="activeTagId = tag.id"
      >
        {{ tag.name }}
      </NTag>
      <NuxtLink
        to="/tag/index"
        class="text-xs text-primary ml-1 hover:underline flex-shrink-0"
      >
        更多标签 →
      </NuxtLink>
    </div>

    <!-- Article Waterfall Grid -->
    <div v-if="loading" class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
      <div v-for="i in 8" :key="i" class="skeleton h-48 rounded-card" />
    </div>

    <div v-else-if="error" class="flex flex-col items-center py-20">
      <NResult status="error" title="加载失败" :description="error">
        <template #footer>
          <NButton type="primary" @click="fetchArticles()">重新加载</NButton>
        </template>
      </NResult>
    </div>

    <div v-else-if="articles.length === 0" class="py-20">
      <EmptyState
        description="还没有文章，成为第一个创作者吧～"
        action-label="写文章"
        @action="navigateTo('/article/write')"
      />
    </div>

    <template v-else>
      <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
        <ArticleCard v-for="article in articles" :key="article.id" :article="article" />
      </div>

      <!-- Load More -->
      <div v-if="hasMore" class="flex justify-center mt-8">
        <NButton
          :loading="loadingMore"
          secondary
          @click="loadMore"
        >
          加载更多
        </NButton>
      </div>
      <p v-else class="text-center text-text-secondary text-sm mt-8">
        — 已经到底了 —
      </p>
    </template>
  </div>
</template>

<script setup lang="ts">
import { NTag, NButton, NResult } from 'naive-ui'
import { getArticles, getHotArticles } from '~/api/modules/article'
import { getTagCloud } from '~/api/modules/tag'
import type { Article, HotArticleDTO, TagCloudItem } from '~/types'

definePageMeta({
  // SSG for home page — but we need fresh data, so use CSR for initial load
  // then SSG for subsequent visits via generate
})

const articles = ref<Article[]>([])
const hotArticles = ref<HotArticleDTO[]>([])
const tags = ref<TagCloudItem[]>([])
const activeTagId = ref<number | null>(null)
const loading = ref(true)
const loadingMore = ref(false)
const error = ref<string | null>(null)
const currentPage = ref(1)
const hasMore = ref(true)
const pageSize = 12

// Fetch hot articles on mount
async function fetchHotArticles() {
  try {
    const result = await getHotArticles()
    hotArticles.value = result.records || []
  } catch {
    // Non-critical, silently fail
  }
}

// Fetch articles with current filters
async function fetchArticles(page = 1) {
  loading.value = true
  error.value = null

  try {
    const params: Record<string, any> = {
      page,
      size: pageSize,
      status: 'published',
    }
    if (activeTagId.value) {
      params.tagId = activeTagId.value
    }

    const result = await getArticles(params)
    if (page === 1) {
      articles.value = result.records || []
    } else {
      articles.value.push(...(result.records || []))
    }
    currentPage.value = page
    hasMore.value = result.records && result.records.length === pageSize
  } catch (err: any) {
    error.value = err.message || '加载文章失败'
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

async function loadMore() {
  loadingMore.value = true
  await fetchArticles(currentPage.value + 1)
}

// Fetch tags for filter bar
async function fetchTags() {
  try {
    tags.value = (await getTagCloud('count')).slice(0, 8)
  } catch {
    // Non-critical
  }
}

// Watch active tag changes
watch(activeTagId, () => {
  fetchArticles(1)
})

// On mount
onMounted(() => {
  fetchHotArticles()
  fetchTags()
  fetchArticles()
})
</script>
```

---

### Task 13: Article Detail Page

**Files:**
- Create: `blog-frontend/pages/article/[id].vue`
- Create: `blog-frontend/components/article/ArticleStats.vue`

- [ ] **Step 1: Write `components/article/ArticleStats.vue`**

```vue
<template>
  <div class="flex items-center gap-6 text-sm text-text-secondary">
    <span class="flex items-center gap-1">
      <Icon size="16"><EyeOutline /></Icon>
      {{ stats.readCount ?? 0 }}
    </span>
    <NButton
      text
      :type="likeState.liked ? 'error' : 'default'"
      @click.stop="handleLike"
    >
      <template #icon>
        <Icon size="16">
          <Heart v-if="likeState.liked" />
          <HeartOutline v-else />
        </Icon>
      </template>
      {{ likeState.count }}
    </NButton>
    <NButton
      text
      :type="favoriteState.liked ? 'warning' : 'default'"
      @click.stop="handleFavorite"
    >
      <template #icon>
        <Icon size="16">
          <Star v-if="favoriteState.liked" />
          <StarOutline v-else />
        </Icon>
      </template>
      {{ favoriteState.count }}
    </NButton>
    <span class="flex items-center gap-1">
      <Icon size="16"><ChatbubbleOutline /></Icon>
      {{ stats.commentCount ?? 0 }}
    </span>
  </div>
</template>

<script setup lang="ts">
import { NButton, Icon, useMessage } from 'naive-ui'
import { EyeOutline, HeartOutline, Heart, StarOutline, Star, ChatbubbleOutline } from '@vicons/ionicons5'
import { toggleLike, toggleFavorite } from '~/api/modules/article'

const props = defineProps<{
  articleId: number
  stats: Record<string, number>
}>()

const emit = defineEmits<{
  statsUpdate: [stats: Record<string, number>]
}>()

const message = useMessage()

const likeState = reactive({ liked: false, count: props.stats.likeCount ?? 0 })
const favoriteState = reactive({ liked: false, count: props.stats.favoriteCount ?? 0 })

async function handleLike() {
  // Optimistic update
  const prevLiked = likeState.liked
  const prevCount = likeState.count
  likeState.liked = !likeState.liked
  likeState.count += likeState.liked ? 1 : -1

  try {
    const result = await toggleLike(props.articleId)
    likeState.liked = result.liked
    likeState.count = result.count
  } catch {
    // Rollback on failure
    likeState.liked = prevLiked
    likeState.count = prevCount
    message.error('操作失败，请稍后重试')
  }
}

async function handleFavorite() {
  const prevLiked = favoriteState.liked
  const prevCount = favoriteState.count
  favoriteState.liked = !favoriteState.liked
  favoriteState.count += favoriteState.liked ? 1 : -1

  try {
    const result = await toggleFavorite(props.articleId)
    favoriteState.liked = result.liked
    favoriteState.count = result.count
  } catch {
    favoriteState.liked = prevLiked
    favoriteState.count = prevCount
    message.error('操作失败，请稍后重试')
  }
}
</script>
```

- [ ] **Step 2: Write `pages/article/[id].vue`**

```vue
<template>
  <div class="max-w-3xl mx-auto px-4 py-6">
    <!-- Loading -->
    <template v-if="loading">
      <div class="skeleton h-8 w-3/4 mb-4" />
      <div class="skeleton h-4 w-1/4 mb-6" />
      <div class="skeleton h-64 rounded-card mb-6" />
      <div class="skeleton h-4 w-full mb-2" v-for="i in 8" :key="i" />
    </template>

    <!-- Error -->
    <NResult
      v-else-if="error"
      status="error"
      :title="error.includes('404') || error.includes('不存在') ? '文章不存在' : '加载失败'"
      :description="error"
    >
      <template #footer>
        <NButton type="primary" @click="router.back()">返回</NButton>
      </template>
    </NResult>

    <!-- Content -->
    <template v-else-if="article">
      <!-- Title -->
      <h1 class="text-2xl md:text-3xl font-bold mb-2">{{ article.title }}</h1>

      <!-- Meta -->
      <div class="flex items-center gap-4 text-sm text-text-secondary mb-6 pb-6 border-b border-gray-100">
        <span>{{ article.category }}</span>
        <span>·</span>
        <span>{{ formatDate(article.createdAt) }}</span>
        <span>·</span>
        <span>👁 {{ article.readCount }} 阅读</span>
      </div>

      <!-- Stats Bar -->
      <ArticleStats
        :article-id="article.id"
        :stats="{
          readCount: article.readCount,
          likeCount: article.likeCount,
          favoriteCount: article.favoriteCount,
          commentCount: article.commentCount,
        }"
        class="mb-6"
      />

      <!-- Tags -->
      <div v-if="tags.length" class="flex gap-2 mb-6 flex-wrap">
        <TagBadge v-for="tag in tags" :key="tag.id" :tag="tag" />
      </div>

      <!-- Content Body -->
      <div class="prose max-w-none mb-12" v-html="renderedContent" />

      <!-- Comments Section -->
      <div class="border-t border-gray-100 pt-8">
        <h2 class="text-lg font-bold mb-4">
          评论 ({{ article.commentCount }})
        </h2>
        <CommentList :article-id="article.id" />
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { NResult, NButton } from 'naive-ui'
import { getArticleById, getArticleTags } from '~/api/modules/article'
import type { Article, Tag } from '~/types'

const route = useRoute()
const router = useRouter()

const article = ref<Article | null>(null)
const tags = ref<Tag[]>([])
const loading = ref(true)
const error = ref<string | null>(null)

const articleId = computed(() => Number(route.params.id))

const renderedContent = computed(() => {
  if (!article.value?.content) return ''
  // Simple newline-to-paragraph for plain text; Markdown would use a renderer
  return article.value.content
    .split('\n')
    .map(line => line.trim() ? `<p>${line}</p>` : '')
    .join('')
})

function formatDate(dateStr: string): string {
  const d = new Date(dateStr)
  const now = new Date()
  const diff = now.getTime() - d.getTime()
  const days = Math.floor(diff / 86400000)
  if (days === 0) return '今天'
  if (days === 1) return '昨天'
  if (days < 7) return `${days}天前`
  return d.toLocaleDateString('zh-CN')
}

async function fetchArticle() {
  loading.value = true
  error.value = null
  try {
    article.value = await getArticleById(articleId.value)
    tags.value = await getArticleTags(articleId.value)
  } catch (err: any) {
    error.value = err.message || '加载文章失败'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchArticle()
})

watch(articleId, () => {
  fetchArticle()
})
</script>
```

---

### Task 14: Article Editor (Write Page)

**Files:**
- Create: `blog-frontend/pages/article/write.vue`

- [ ] **Step 1: Write `pages/article/write.vue`**

```vue
<template>
  <div class="max-w-3xl mx-auto px-4 py-6">
    <NCard>
      <template #header>
        <div class="flex items-center justify-between">
          <h2 class="text-lg font-bold">{{ isEdit ? '编辑文章' : '写文章' }}</h2>
          <NSpace>
            <NButton @click="saveDraft" :loading="saving">保存草稿</NButton>
            <NButton type="primary" @click="publish" :loading="saving">发布</NButton>
          </NSpace>
        </div>
      </template>

      <NForm ref="formRef" :model="form" :rules="rules">
        <NFormItem path="title" label="标题">
          <NInput
            v-model:value="form.title"
            placeholder="请输入文章标题"
            size="large"
          />
        </NFormItem>

        <NFormItem path="category" label="分类">
          <NInput
            v-model:value="form.category"
            placeholder="例如：旅行、美食、技术..."
          />
        </NFormItem>

        <NFormItem path="content" label="内容">
          <NInput
            v-model:value="form.content"
            type="textarea"
            placeholder="写下你想分享的内容..."
            :autosize="{ minRows: 12, maxRows: 30 }"
          />
        </NFormItem>
      </NForm>
    </NCard>
  </div>
</template>

<script setup lang="ts">
import { NCard, NForm, NFormItem, NInput, NButton, NSpace, useMessage } from 'naive-ui'
import type { FormInst, FormRules } from 'naive-ui'
import { createArticle, updateArticle, getArticleById } from '~/api/modules/article'

definePageMeta({
  middleware: 'auth',
})

const message = useMessage()
const router = useRouter()
const route = useRoute()

const formRef = ref<FormInst | null>(null)
const saving = ref(false)
const isEdit = ref(false)

const form = reactive({
  title: '',
  category: '',
  content: '',
})

const rules: FormRules = {
  title: [
    { required: true, message: '请输入标题', trigger: 'blur' },
    { min: 2, message: '标题至少2个字符', trigger: 'blur' },
  ],
  content: [
    { required: true, message: '请输入内容', trigger: 'blur' },
  ],
}

// Check if editing existing article
const editId = computed(() => {
  const id = route.query.edit
  return id ? Number(id) : null
})

// Load draft from localStorage
const DRAFT_KEY = 'article_draft'

function loadDraft() {
  if (import.meta.client) {
    const draft = localStorage.getItem(DRAFT_KEY)
    if (draft) {
      try {
        const data = JSON.parse(draft)
        form.title = data.title || ''
        form.category = data.category || ''
        form.content = data.content || ''
      } catch {}
    }
  }
}

function saveDraftToStorage() {
  if (import.meta.client) {
    localStorage.setItem(DRAFT_KEY, JSON.stringify({
      title: form.title,
      category: form.category,
      content: form.content,
    }))
  }
}

function clearDraft() {
  if (import.meta.client) {
    localStorage.removeItem(DRAFT_KEY)
  }
}

// Auto-save draft every 30 seconds
let autoSaveTimer: ReturnType<typeof setInterval> | null = null

onMounted(() => {
  if (editId.value) {
    isEdit.value = true
    fetchArticleForEdit()
  } else {
    loadDraft()
  }
  autoSaveTimer = setInterval(saveDraftToStorage, 30000)
})

onUnmounted(() => {
  if (autoSaveTimer) clearInterval(autoSaveTimer)
})

async function fetchArticleForEdit() {
  try {
    const article = await getArticleById(editId.value!)
    form.title = article.title
    form.category = article.category || ''
    form.content = article.content
  } catch (err: any) {
    message.error('加载文章失败')
  }
}

async function saveDraft() {
  saveDraftToStorage()
  message.success('草稿已保存到本地')
}

async function publish() {
  try {
    await formRef.value?.validate()
  } catch {
    message.warning('请填写必填项')
    return
  }

  saving.value = true
  try {
    if (isEdit.value && editId.value) {
      await updateArticle(editId.value, {
        title: form.title,
        category: form.category,
        content: form.content,
        status: 'published',
      })
      message.success('文章已更新')
      router.push(`/article/${editId.value}`)
    } else {
      const article = await createArticle({
        title: form.title,
        category: form.category,
        content: form.content,
        status: 'published',
      })
      clearDraft()
      message.success('文章发布成功！')
      router.push(`/article/${article.id}`)
    }
  } catch (err: any) {
    message.error(err.message || '发布失败，请稍后重试')
  } finally {
    saving.value = false
  }
}
</script>
```

---

### Task 15: Comment System (CommentList + CommentItem)

**Files:**
- Create: `blog-frontend/components/comment/CommentItem.vue`
- Create: `blog-frontend/components/comment/CommentList.vue`

- [ ] **Step 1: Write `components/comment/CommentItem.vue`**

```vue
<template>
  <div class="py-3">
    <div class="flex gap-3">
      <UserAvatar :username="comment.userId?.toString() || '?'" size="small" />
      <div class="flex-1 min-w-0">
        <!-- Header -->
        <div class="flex items-center gap-2 mb-1">
          <span class="text-sm font-medium">用户{{ comment.userId }}</span>
          <span class="text-xs text-text-secondary">{{ formatDate(comment.createdAt) }}</span>
          <NTag v-if="comment.status === 'deleted'" size="tiny" type="error">已删除</NTag>
          <NTag v-if="comment.status === 'hidden'" size="tiny" type="warning">已隐藏</NTag>
        </div>

        <!-- Content -->
        <p
          class="text-sm text-text-primary leading-relaxed mb-2"
          :class="{ 'italic text-text-secondary': comment.status !== 'visible' }"
        >
          {{ comment.status === 'deleted' ? '该评论已被删除' : comment.content }}
        </p>

        <!-- Actions -->
        <div v-if="comment.status === 'visible'" class="flex items-center gap-4 text-xs text-text-secondary">
          <NButton text size="tiny" @click="handleLike">
            <template #icon>
              <Icon size="14"><HeartOutline /></Icon>
            </template>
            {{ comment.likeCount }}
          </NButton>
          <NButton
            text
            size="tiny"
            @click="$emit('reply', comment.id)"
            v-if="!isReply"
          >
            回复
          </NButton>
          <NButton
            v-if="canDelete"
            text
            size="tiny"
            type="error"
            @click="handleDelete"
          >
            删除
          </NButton>
        </div>

        <!-- Nested Replies -->
        <div v-if="comment.replies && comment.replies.length > 0" class="mt-2 pl-4 border-l-2 border-gray-100">
          <CommentItem
            v-for="reply in comment.replies"
            :key="reply.id"
            :comment="reply"
            is-reply
            @reply="$emit('reply', $event)"
            @deleted="$emit('deleted', $event)"
          />
        </div>

        <!-- Show more replies link -->
        <NButton
          v-if="comment.replyCount > (comment.replies?.length || 0)"
          text
          size="tiny"
          type="primary"
          class="mt-2"
          @click="$emit('loadReplies', comment.id)"
        >
          查看全部 {{ comment.replyCount }} 条回复
        </NButton>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { NButton, NTag, Icon, useMessage } from 'naive-ui'
import { HeartOutline } from '@vicons/ionicons5'
import { toggleCommentLike, deleteComment } from '~/api/modules/comment'
import { useAuthStore } from '~/stores/auth'
import type { CommentDTO } from '~/types'

const props = withDefaults(defineProps<{
  comment: CommentDTO
  isReply?: boolean
}>(), {
  isReply: false,
})

const emit = defineEmits<{
  reply: [commentId: number]
  deleted: [commentId: number]
  loadReplies: [commentId: number]
}>()

const authStore = useAuthStore()
const message = useMessage()

const canDelete = computed(() =>
  authStore.isAdmin || authStore.user?.userId === props.comment.userId
)

function formatDate(dateStr: string): string {
  const d = new Date(dateStr)
  const now = new Date()
  const diff = now.getTime() - d.getTime()
  const mins = Math.floor(diff / 60000)
  if (mins < 1) return '刚刚'
  if (mins < 60) return `${mins}分钟前`
  const hours = Math.floor(mins / 60)
  if (hours < 24) return `${hours}小时前`
  return d.toLocaleDateString('zh-CN')
}

async function handleLike() {
  try {
    await toggleCommentLike(props.comment.id)
    props.comment.likeCount++
  } catch {
    message.error('操作失败')
  }
}

async function handleDelete() {
  try {
    await deleteComment(props.comment.id)
    emit('deleted', props.comment.id)
    message.success('评论已删除')
  } catch (err: any) {
    message.error(err.message || '删除失败')
  }
}
</script>
```

- [ ] **Step 2: Write `components/comment/CommentList.vue`**

```vue
<template>
  <div>
    <!-- Comment Input -->
    <div v-if="authStore.isLoggedIn" class="mb-6">
      <NInput
        v-model:value="commentText"
        type="textarea"
        :placeholder="replyTarget ? `回复 用户${replyTarget}` : '写下你的评论...'"
        :autosize="{ minRows: 2, maxRows: 4 }"
      />
      <div class="flex items-center justify-between mt-2">
        <NButton
          v-if="replyTarget"
          text
          size="small"
          type="tertiary"
          @click="cancelReply"
        >
          取消回复
        </NButton>
        <span v-else />
        <NButton
          type="primary"
          size="small"
          :loading="posting"
          :disabled="!commentText.trim()"
          @click="postComment"
        >
          发表评论
        </NButton>
      </div>
    </div>
    <NuxtLink v-else to="/user/login" class="text-primary text-sm">
      登录后参与评论 →
    </NuxtLink>

    <!-- Comment List -->
    <div v-if="loading">
      <div v-for="i in 3" :key="i" class="py-3">
        <div class="skeleton h-4 w-1/4 mb-2" />
        <div class="skeleton h-3 w-full" />
      </div>
    </div>

    <EmptyState
      v-else-if="comments.length === 0"
      description="暂无评论，来发表第一条评论吧～"
    />

    <template v-else>
      <CommentItem
        v-for="comment in comments"
        :key="comment.id"
        :comment="comment"
        @reply="startReply"
        @deleted="handleCommentDeleted"
        @load-replies="loadReplies"
      />

      <!-- Pagination -->
      <div v-if="totalPages > 1" class="flex justify-center mt-6">
        <NPagination
          v-model:page="currentPage"
          :page-count="totalPages"
          @update:page="fetchComments"
        />
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { NInput, NButton, NPagination, useMessage } from 'naive-ui'
import { getTopLevelComments, createComment, getReplies } from '~/api/modules/comment'
import { useAuthStore } from '~/stores/auth'
import type { CommentDTO } from '~/types'

const props = defineProps<{
  articleId: number
}>()

const authStore = useAuthStore()
const message = useMessage()

const comments = ref<CommentDTO[]>([])
const loading = ref(true)
const posting = ref(false)
const commentText = ref('')
const replyTarget = ref<number | null>(null)
const currentPage = ref(1)
const totalPages = ref(1)

function startReply(commentId: number) {
  replyTarget.value = commentId
  commentText.value = ''
}

function cancelReply() {
  replyTarget.value = null
  commentText.value = ''
}

async function postComment() {
  if (!commentText.value.trim()) return
  posting.value = true
  try {
    await createComment({
      articleId: props.articleId,
      content: commentText.value,
      parentId: replyTarget.value,
    })
    message.success('评论发表成功')
    commentText.value = ''
    replyTarget.value = null
    await fetchComments(1)
  } catch (err: any) {
    message.error(err.message || '评论发表失败')
  } finally {
    posting.value = false
  }
}

async function fetchComments(page = 1) {
  loading.value = true
  try {
    const result = await getTopLevelComments(props.articleId, page)
    comments.value = result.records || []
    currentPage.value = page
    totalPages.value = result.pages || 1
  } catch (err: any) {
    message.error(err.message || '加载评论失败')
  } finally {
    loading.value = false
  }
}

async function loadReplies(commentId: number) {
  try {
    const result = await getReplies(commentId)
    const comment = comments.value.find(c => c.id === commentId)
    if (comment) {
      comment.replies = result.records || []
      comment.replyCount = comment.replies.length
    }
  } catch {
    message.error('加载回复失败')
  }
}

function handleCommentDeleted(commentId: number) {
  comments.value = comments.value.filter(c => c.id !== commentId)
}

onMounted(() => {
  fetchComments()
})
</script>
```

---

### Task 16: Tag Pages (Cloud & Filtered Listing)

**Files:**
- Create: `blog-frontend/pages/tag/index.vue`
- Create: `blog-frontend/pages/tag/[id].vue`

- [ ] **Step 1: Write `pages/tag/index.vue`**

```vue
<template>
  <div class="max-w-4xl mx-auto px-4 py-8">
    <h1 class="text-2xl font-bold mb-6">🏷 标签云</h1>

    <div v-if="loading" class="flex flex-wrap gap-3">
      <div v-for="i in 12" :key="i" class="skeleton h-8 w-20 rounded-tag" />
    </div>

    <NResult
      v-else-if="error"
      status="error"
      title="加载失败"
      :description="error"
    >
      <template #footer>
        <NButton type="primary" @click="fetchTags">重新加载</NButton>
      </template>
    </NResult>

    <div v-else class="flex flex-wrap gap-3">
      <NTag
        v-for="tag in tags"
        :key="tag.id"
        size="large"
        :bordered="false"
        class="cursor-pointer hover:shadow-md transition-shadow px-4 py-2"
        :style="{ fontSize: getTagSize(tag.articleCount) }"
        @click="navigateTo(`/tag/${tag.id}`)"
      >
        {{ tag.name }}
        <span class="ml-1 opacity-50" :style="{ fontSize: '0.75em' }">
          {{ tag.articleCount }}
        </span>
      </NTag>
    </div>
  </div>
</template>

<script setup lang="ts">
import { NTag, NButton, NResult } from 'naive-ui'
import { getTagCloud } from '~/api/modules/tag'
import type { TagCloudItem } from '~/types'

const tags = ref<TagCloudItem[]>([])
const loading = ref(true)
const error = ref<string | null>(null)

function getTagSize(count: number): string {
  if (count >= 20) return '1.4rem'
  if (count >= 10) return '1.1rem'
  if (count >= 5) return '0.95rem'
  return '0.8rem'
}

async function fetchTags() {
  loading.value = true
  error.value = null
  try {
    tags.value = await getTagCloud('count')
  } catch (err: any) {
    error.value = err.message || '加载标签失败'
  } finally {
    loading.value = false
  }
}

onMounted(fetchTags)
</script>
```

- [ ] **Step 2: Write `pages/tag/[id].vue`**

```vue
<template>
  <div class="max-w-6xl mx-auto px-4 py-6">
    <div class="flex items-center gap-3 mb-6">
      <NButton text @click="router.back()">
        <template #icon><Icon><ArrowBackOutline /></Icon></template>
      </NButton>
      <h1 class="text-xl font-bold">标签：{{ tagName }}</h1>
      <span class="text-sm text-text-secondary">{{ totalArticles }} 篇文章</span>
    </div>

    <div v-if="loading" class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
      <div v-for="i in 8" :key="i" class="skeleton h-48 rounded-card" />
    </div>

    <EmptyState
      v-else-if="articles.length === 0"
      description="该标签下还没有文章"
    />

    <template v-else>
      <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
        <ArticleCard v-for="article in articles" :key="article.id" :article="article" />
      </div>

      <NPagination
        v-if="totalPages > 1"
        v-model:page="currentPage"
        :page-count="totalPages"
        class="mt-8 justify-center"
        @update:page="fetchArticles"
      />
    </template>
  </div>
</template>

<script setup lang="ts">
import { NButton, NPagination, Icon } from 'naive-ui'
import { ArrowBackOutline } from '@vicons/ionicons5'
import { getArticles } from '~/api/modules/article'
import type { Article } from '~/types'

const route = useRoute()
const router = useRouter()

const tagId = computed(() => Number(route.params.id))
const tagName = ref('')
const articles = ref<Article[]>([])
const loading = ref(true)
const currentPage = ref(1)
const totalPages = ref(1)
const totalArticles = ref(0)

async function fetchArticles(page = 1) {
  loading.value = true
  try {
    const result = await getArticles({ page, size: 12, tagId: tagId.value })
    articles.value = result.records || []
    currentPage.value = page
    totalPages.value = result.pages || 1
    totalArticles.value = result.total || 0
  } catch {
    // handled by error boundary
  } finally {
    loading.value = false
  }
}

onMounted(fetchArticles)
watch(tagId, () => fetchArticles())
</script>
```

---

### Task 17: Hot Articles Page & User Profile Page

**Files:**
- Create: `blog-frontend/pages/article/hot.vue`
- Create: `blog-frontend/pages/user/[id].vue`

- [ ] **Step 1: Write `pages/article/hot.vue`**

```vue
<template>
  <div class="max-w-4xl mx-auto px-4 py-6">
    <h1 class="text-2xl font-bold mb-6">🔥 热门文章</h1>

    <div class="flex gap-2 mb-6">
      <NButton
        :type="days === 7 ? 'primary' : 'default'"
        @click="days = 7; fetchHot()"
      >
        近7天
      </NButton>
      <NButton
        :type="days === 30 ? 'primary' : 'default'"
        @click="days = 30; fetchHot()"
      >
        近30天
      </NButton>
    </div>

    <div v-if="loading" class="space-y-3">
      <div v-for="i in 5" :key="i" class="skeleton h-20 rounded-card" />
    </div>

    <EmptyState v-else-if="articles.length === 0" description="暂无热门文章" />

    <template v-else>
      <div class="space-y-4">
        <NCard
          v-for="(item, index) in articles"
          :key="item.articleId"
          :bordered="false"
          class="cursor-pointer hover:shadow-md transition-shadow"
          @click="navigateTo(`/article/${item.articleId}`)"
        >
          <div class="flex items-center gap-4">
            <span
              class="text-2xl font-bold w-8 text-center"
              :class="index < 3 ? 'text-accent' : 'text-text-secondary'"
            >
              {{ index + 1 }}
            </span>
            <div class="flex-1">
              <h3 class="font-bold mb-1">{{ item.title }}</h3>
              <p class="text-sm text-text-secondary">👁 {{ item.readCount }} 次阅读</p>
            </div>
          </div>
        </NCard>
      </div>

      <NPagination
        v-if="totalPages > 1"
        v-model:page="currentPage"
        :page-count="totalPages"
        class="mt-6 justify-center"
        @update:page="(p: number) => { currentPage = p; fetchHot() }"
      />
    </template>
  </div>
</template>

<script setup lang="ts">
import { NCard, NButton, NPagination } from 'naive-ui'
import { getHotArticles } from '~/api/modules/article'
import type { HotArticleDTO } from '~/types'

const articles = ref<HotArticleDTO[]>([])
const loading = ref(true)
const days = ref(7)
const currentPage = ref(1)
const totalPages = ref(1)

async function fetchHot() {
  loading.value = true
  try {
    const result = await getHotArticles(days.value, currentPage.value)
    articles.value = result.records || []
    totalPages.value = result.pages || 1
  } catch {
    // handled
  } finally {
    loading.value = false
  }
}

onMounted(fetchHot)
</script>
```

- [ ] **Step 2: Write `pages/user/[id].vue`**

```vue
<template>
  <div class="max-w-6xl mx-auto px-4 py-6">
    <!-- Profile Header -->
    <div class="bg-white rounded-card p-6 mb-6 shadow-sm">
      <div class="flex items-center gap-4">
        <UserAvatar :username="username" size="large" />
        <div>
          <h1 class="text-xl font-bold">{{ username || '用户' + userId }}</h1>
          <p class="text-sm text-text-secondary">{{ totalArticles }} 篇文章</p>
        </div>
      </div>
    </div>

    <!-- Articles -->
    <h2 class="text-lg font-bold mb-4">发布的文章</h2>

    <div v-if="loading" class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
      <div v-for="i in 8" :key="i" class="skeleton h-48 rounded-card" />
    </div>

    <EmptyState
      v-else-if="articles.length === 0"
      description="该用户还没有发布文章"
    />

    <template v-else>
      <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
        <ArticleCard v-for="article in articles" :key="article.id" :article="article" />
      </div>

      <NPagination
        v-if="totalPages > 1"
        v-model:page="currentPage"
        :page-count="totalPages"
        class="mt-8 justify-center"
        @update:page="(p: number) => { currentPage = p; fetchArticles() }"
      />
    </template>
  </div>
</template>

<script setup lang="ts">
import { NPagination } from 'naive-ui'
import { getArticles } from '~/api/modules/article'
import type { Article } from '~/types'

const route = useRoute()

const userId = computed(() => route.params.id)
const username = ref('')
const articles = ref<Article[]>([])
const loading = ref(true)
const currentPage = ref(1)
const totalPages = ref(1)
const totalArticles = ref(0)

async function fetchArticles() {
  loading.value = true
  try {
    // Note: the current backend doesn't have a filter by authorId directly.
    // Using keyword search as a workaround, or we filter client-side.
    // For now, fetch all published and filter.
    const result = await getArticles({ page: currentPage.value, size: 12, status: 'published' })
    articles.value = (result.records || []).filter(a => a.authorId === Number(userId.value))
    totalArticles.value = articles.value.length
    totalPages.value = 1
  } catch {
    // handled
  } finally {
    loading.value = false
  }
}

onMounted(fetchArticles)
watch(userId, fetchArticles)
</script>
```

---

### Task 18: Admin Pages (Tag Management & Comment Management)

**Files:**
- Create: `blog-frontend/pages/admin/tags.vue`
- Create: `blog-frontend/pages/admin/comments.vue`

- [ ] **Step 1: Write `pages/admin/tags.vue`**

```vue
<template>
  <div class="max-w-2xl mx-auto px-4 py-6">
    <h1 class="text-2xl font-bold mb-6">🏷 标签管理</h1>

    <!-- Create Tag -->
    <div class="flex gap-3 mb-6">
      <NInput
        v-model:value="newTagName"
        placeholder="输入新标签名"
        @keyup.enter="handleCreate"
      />
      <NButton type="primary" :loading="creating" @click="handleCreate">创建</NButton>
    </div>

    <!-- Tag List -->
    <div v-if="loading" class="space-y-2">
      <div v-for="i in 5" :key="i" class="skeleton h-10 rounded" />
    </div>

    <NResult
      v-else-if="error"
      status="error"
      title="加载失败"
      :description="error"
    >
      <template #footer>
        <NButton type="primary" @click="fetchTags">重新加载</NButton>
      </template>
    </NResult>

    <div v-else class="space-y-2">
      <NCard
        v-for="tag in tags"
        :key="tag.id"
        size="small"
        :bordered="false"
      >
        <div class="flex items-center justify-between">
          <div>
            <span class="font-medium">{{ tag.name }}</span>
            <span class="text-xs text-text-secondary ml-2">
              {{ tag.articleCount }} 篇文章 · 热度 {{ tag.hotScore }}
            </span>
          </div>
          <div class="flex gap-2">
            <NButton
              text
              size="small"
              type="primary"
              @click="startEdit(tag)"
            >
              编辑
            </NButton>
            <NPopconfirm @positive-click="handleDelete(tag.id)">
              <template #trigger>
                <NButton text size="small" type="error">删除</NButton>
              </template>
              确定删除标签 "{{ tag.name }}"？此操作不可撤销。
            </NPopconfirm>
          </div>
        </div>
      </NCard>
    </div>

    <!-- Edit Modal -->
    <NModal v-model:show="showEdit" title="编辑标签">
      <NCard style="width: 400px;" :bordered="false" title="编辑标签">
        <NInput v-model:value="editName" placeholder="标签名" />
        <template #footer>
          <NButton type="primary" @click="handleUpdate">保存</NButton>
        </template>
      </NCard>
    </NModal>
  </div>
</template>

<script setup lang="ts">
import { NCard, NInput, NButton, NModal, NPopconfirm, NResult, useMessage } from 'naive-ui'
import { getTagCloud, createTag, updateTag, deleteTag } from '~/api/modules/tag'
import type { TagCloudItem } from '~/types'

definePageMeta({
  middleware: ['auth', 'admin'],
})

const message = useMessage()

const tags = ref<TagCloudItem[]>([])
const loading = ref(true)
const error = ref<string | null>(null)
const creating = ref(false)
const newTagName = ref('')

// Edit state
const showEdit = ref(false)
const editingId = ref<number | null>(null)
const editName = ref('')

async function fetchTags() {
  loading.value = true
  error.value = null
  try {
    tags.value = await getTagCloud('count')
  } catch (err: any) {
    error.value = err.message || '加载标签失败'
  } finally {
    loading.value = false
  }
}

async function handleCreate() {
  if (!newTagName.value.trim()) return
  creating.value = true
  try {
    await createTag(newTagName.value.trim())
    message.success('标签创建成功')
    newTagName.value = ''
    await fetchTags()
  } catch (err: any) {
    message.error(err.message || '创建失败')
  } finally {
    creating.value = false
  }
}

function startEdit(tag: TagCloudItem) {
  editingId.value = tag.id
  editName.value = tag.name
  showEdit.value = true
}

async function handleUpdate() {
  if (!editingId.value || !editName.value.trim()) return
  try {
    await updateTag(editingId.value, editName.value.trim())
    message.success('标签更新成功')
    showEdit.value = false
    await fetchTags()
  } catch (err: any) {
    message.error(err.message || '更新失败')
  }
}

async function handleDelete(id: number) {
  try {
    await deleteTag(id)
    message.success('标签已删除')
    await fetchTags()
  } catch (err: any) {
    message.error(err.message || '删除失败')
  }
}

onMounted(fetchTags)
</script>
```

- [ ] **Step 2: Write `pages/admin/comments.vue`**

```vue
<template>
  <div class="max-w-4xl mx-auto px-4 py-6">
    <h1 class="text-2xl font-bold mb-6">💬 评论管理</h1>

    <NResult
      v-if="error"
      status="error"
      title="加载失败"
      :description="error"
    >
      <template #footer>
        <NButton type="primary" @click="fetchComments()">重新加载</NButton>
      </template>
    </NResult>

    <EmptyState v-else-if="comments.length === 0" description="暂无评论需要管理" />

    <div v-else class="space-y-2">
      <NCard
        v-for="comment in comments"
        :key="comment.id"
        size="small"
        :bordered="false"
      >
        <div class="flex items-start justify-between gap-4">
          <div class="flex-1 min-w-0">
            <div class="flex items-center gap-2 mb-1">
              <span class="text-sm font-medium">用户{{ comment.userId }}</span>
              <NTag size="tiny" :type="statusType(comment.status)">
                {{ statusLabel(comment.status) }}
              </NTag>
            </div>
            <p class="text-sm">{{ comment.content }}</p>
          </div>
          <div class="flex gap-2 flex-shrink-0">
            <NButton
              v-if="comment.status === 'visible'"
              text
              size="small"
              type="warning"
              @click="handleHide(comment.id)"
            >
              隐藏
            </NButton>
            <NPopconfirm @positive-click="handleDelete(comment.id)">
              <template #trigger>
                <NButton text size="small" type="error">删除</NButton>
              </template>
              确定要删除这条评论吗？
            </NPopconfirm>
          </div>
        </div>
      </NCard>
    </div>
  </div>
</template>

<script setup lang="ts">
import { NCard, NTag, NButton, NPopconfirm, NResult, useMessage } from 'naive-ui'
import { hideComment, deleteComment } from '~/api/modules/comment'
import type { Comment } from '~/types'

definePageMeta({
  middleware: ['auth', 'admin'],
})

const message = useMessage()
const comments = ref<Comment[]>([])
const error = ref<string | null>(null)

function statusType(status: string) {
  switch (status) {
    case 'visible': return 'success'
    case 'hidden': return 'warning'
    case 'deleted': return 'error'
    default: return 'default'
  }
}

function statusLabel(status: string) {
  switch (status) {
    case 'visible': return '正常'
    case 'hidden': return '已隐藏'
    case 'deleted': return '已删除'
    default: return status
  }
}

// Note: This page would need a dedicated admin endpoint to list all comments.
// The current backend doesn't have a "list all comments" endpoint, so this is
// a placeholder for when that endpoint is added or for articles' comments.

async function handleHide(id: number) {
  try {
    await hideComment(id)
    message.success('评论已隐藏')
    const c = comments.value.find(c => c.id === id)
    if (c) c.status = 'hidden'
  } catch (err: any) {
    message.error(err.message || '操作失败')
  }
}

async function handleDelete(id: number) {
  try {
    await deleteComment(id)
    message.success('评论已删除')
    comments.value = comments.value.filter(c => c.id !== id)
  } catch (err: any) {
    message.error(err.message || '删除失败')
  }
}
</script>
```

---

### Task 19: Global Error Page

**Files:**
- Create: `blog-frontend/layouts/error.vue`

- [ ] **Step 1: Write `layouts/error.vue`**

```vue
<template>
  <div class="min-h-screen flex items-center justify-center bg-background px-4">
    <NResult
      :status="errorStatus"
      :title="errorTitle"
      :description="errorDescription"
    >
      <template #footer>
        <NSpace justify="center">
          <NButton type="primary" @click="handleBack">
            {{ error.statusCode === 404 ? '返回首页' : '重新加载' }}
          </NButton>
          <NButton v-if="error.statusCode !== 404" @click="handleBack">
            返回首页
          </NButton>
        </NSpace>
      </template>
    </NResult>
  </div>
</template>

<script setup lang="ts">
import { NResult, NButton, NSpace } from 'naive-ui'

const props = defineProps<{
  error: {
    statusCode?: number
    message?: string
  }
}>()

const errorStatus = computed(() => {
  switch (props.error?.statusCode) {
    case 404: return '404' as const
    case 403: return '403' as const
    case 500: return '500' as const
    default: return 'error' as const
  }
})

const errorTitle = computed(() => {
  switch (props.error?.statusCode) {
    case 404: return '页面不存在'
    case 403: return '无权限访问'
    case 500: return '服务器错误'
    default: return '出错了'
  }
})

const errorDescription = computed(() => {
  return props.error?.message || '请稍后再试'
})

function handleBack() {
  clearError()
  if (props.error?.statusCode === 404) {
    navigateTo('/')
  } else {
    // Reload current page
    location.reload()
  }
}
</script>
```

---

### Task 20: Final Integration & Polish

**Tasks:**
- SEO meta tags for key pages
- Verify CORS configuration with backend
- Test all flows end-to-end

- [ ] **Step 1: Configure CORS proxy for development**

Add to `nuxt.config.ts` the dev proxy:

```typescript
// Inside defineNuxtConfig({...}), add:
nitro: {
  devProxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true,
    },
  },
},
```

- [ ] **Step 2: Update `runtimeConfig.public.apiBase` for production**

Change default `apiBase` to empty string when using same-origin proxy:
```typescript
runtimeConfig: {
  public: {
    apiBase: process.env.NODE_ENV === 'production' ? '' : 'http://localhost:8080',
  },
},
```

- [ ] **Step 3: Verify all pages compile and run**

Run:
```bash
cd blog-frontend
npx nuxi typecheck
npx nuxt build
```

Expected: Type check passes, build succeeds with no errors. All routes are generated.

- [ ] **Step 4: Start backend and test end-to-end**

```bash
# Terminal 1: Start backend
cd blog-backend
mvn spring-boot:run

# Terminal 2: Start frontend
cd blog-frontend
npm run dev
```

Verify flows:
1. Visit http://localhost:3000 → Home page loads with articles (or empty state)
2. Click Login → Login page renders
3. Register new user → Auto-login, header shows user menu
4. Click "写文章" → Auth-protected, editor renders
5. Write and publish article → Redirects to article detail
6. Article detail shows content, stats, tags, comments
7. Like/Favorite toggle works (optimistic)
8. Post comment → Comment appears
9. Visit Tag Cloud → Tags display
10. Click tag → Filtered article list
11. Visit Hot → Ranked articles
12. Admin user: visit /admin/tags → Manage tags
