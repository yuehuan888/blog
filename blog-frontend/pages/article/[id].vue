<template>
  <!-- Loading -->
  <div v-if="loading" class="max-w-3xl mx-auto px-4 py-6">
    <div class="skeleton h-8 w-3/4 mb-4" />
    <div class="skeleton h-4 w-1/4 mb-6" />
    <div class="skeleton h-64 rounded-card mb-6" />
    <div class="skeleton h-4 w-full mb-2" v-for="i in 8" :key="i" />
  </div>

  <!-- Error -->
  <div v-else-if="error" class="max-w-3xl mx-auto px-4 py-6">
    <NResult
      status="error"
      :title="error.includes('404') || error.includes('不存在') ? '文章不存在' : '加载失败'"
      :description="error"
    >
      <template #footer>
        <NButton type="primary" @click="router.back()">返回</NButton>
      </template>
    </NResult>
  </div>

  <!-- ====== Two-column layout (with images) ====== -->
  <div
    v-else-if="article && hasImages"
    class="flex flex-col lg:flex-row lg:h-[calc(100vh-56px)]"
  >
    <!-- Left: Image Column -->
    <div
      class="lg:w-[35%] lg:flex-shrink-0 overflow-y-auto flex flex-col items-center"
      style="scrollbar-width: none; -ms-overflow-style: none; background-color: #F0F7F4;"
    >
      <div class="sticky top-0 z-10 w-full flex items-center justify-between px-4 py-3" style="background: linear-gradient(to bottom, rgba(240,247,244,0.95), rgba(240,247,244,0));">
        <NButton text size="small" @click="router.back()" class="!text-[#2D6A4F]">
          <template #icon>
            <NIcon size="18"><ArrowBackOutline /></NIcon>
          </template>
          返回
        </NButton>
        <span v-if="article.images.length > 1" class="text-text-secondary text-sm">
          {{ article.images.length }} 张图片
        </span>
      </div>
      <div class="px-6 pb-6 space-y-5 w-full max-w-sm mx-auto">
        <img
          v-for="(img, i) in article.images"
          :key="i"
          :src="imageUrl(img)"
          class="w-full rounded-xl cursor-pointer hover:opacity-95 transition-opacity"
          :alt="`图片 ${i + 1}`"
          loading="lazy"
          @click="openLightbox(i)"
        />
      </div>
    </div>

    <!-- Right: Content Column (desktop) -->
    <div class="hidden lg:block lg:w-[65%] overflow-y-auto px-6 py-6 border-l border-gray-100">
      <!-- Author Info -->
      <div v-if="article.authorId" class="flex items-center gap-3 mb-4 p-3 bg-gray-50 rounded-card">
        <div class="cursor-pointer flex items-center gap-3 flex-1" @click="navigateTo(`/user/${article.authorId}`)">
          <UserAvatar
            :username="authorProfile?.nickname || authorProfile?.username || '?'"
            :src="authorProfile?.avatar"
            size="medium"
          />
          <div>
            <div class="text-sm font-medium hover:text-primary transition-colors">
              {{ authorProfile?.nickname || authorProfile?.username || '用户' + article.authorId }}
            </div>
            <div class="text-xs text-text-secondary">
              {{ authorProfile?.followerCount || 0 }} 粉丝 · {{ authorProfile?.articleCount || 0 }} 篇
            </div>
          </div>
        </div>
        <NButton
          v-if="!isOwnArticle"
          :type="following ? 'default' : 'primary'"
          size="small"
          :loading="followLoading"
          @click="handleFollow"
        >
          {{ following ? '已关注' : '+ 关注' }}
        </NButton>
      </div>

      <!-- Title + Delete -->
      <div class="flex items-start justify-between gap-4 mb-2">
        <h1 class="text-2xl md:text-3xl font-bold flex-1">{{ article.title }}</h1>
        <NPopconfirm v-if="canDelete" @positive-click="handleDelete">
          <template #trigger>
            <NButton size="small" type="error" :loading="deleting">
              <template #icon><NIcon size="16"><TrashOutline /></NIcon></template>
              删除
            </NButton>
          </template>
          确定删除此文章？所有评论、点赞数据将被一并删除。
        </NPopconfirm>
      </div>

      <!-- Meta -->
      <div class="flex items-center gap-4 text-sm text-text-secondary mb-6 pb-6 border-b border-gray-100">
        <span>{{ article.category }}</span>
        <span>&middot;</span>
        <span>{{ formatDate(article.createdAt) }}</span>
        <span>&middot;</span>
        <span>&#x1F441; {{ article.readCount }} 阅读</span>
      </div>

      <!-- Stats Bar -->
      <ArticleStats :article-id="article.id" :stats="articleStats" class="mb-6" />

      <!-- Tags -->
      <div v-if="tags.length" class="flex gap-2 mb-6 flex-wrap">
        <TagBadge v-for="tag in tags" :key="tag.id" :tag="tag" />
      </div>

      <!-- Content Body -->
      <div class="prose max-w-none mb-12" v-html="renderedContent" />

      <!-- Comments -->
      <div class="border-t border-gray-100 pt-8">
        <h2 class="text-lg font-bold mb-4">评论 ({{ article.commentCount }})</h2>
        <CommentList :article-id="article.id" />
      </div>
    </div>

    <!-- Mobile: content below images -->
    <div class="lg:hidden px-4 py-6">
      <!-- Author Info -->
      <div v-if="article.authorId" class="flex items-center gap-3 mb-4 p-3 bg-gray-50 rounded-card">
        <div class="cursor-pointer flex items-center gap-3 flex-1" @click="navigateTo(`/user/${article.authorId}`)">
          <UserAvatar
            :username="authorProfile?.nickname || authorProfile?.username || '?'"
            :src="authorProfile?.avatar"
            size="medium"
          />
          <div>
            <div class="text-sm font-medium hover:text-primary transition-colors">
              {{ authorProfile?.nickname || authorProfile?.username || '用户' + article.authorId }}
            </div>
            <div class="text-xs text-text-secondary">
              {{ authorProfile?.followerCount || 0 }} 粉丝 · {{ authorProfile?.articleCount || 0 }} 篇
            </div>
          </div>
        </div>
        <NButton
          v-if="!isOwnArticle"
          :type="following ? 'default' : 'primary'"
          size="small"
          :loading="followLoading"
          @click="handleFollow"
        >
          {{ following ? '已关注' : '+ 关注' }}
        </NButton>
      </div>

      <h1 class="text-2xl font-bold mb-2">{{ article.title }}</h1>
      <div class="flex items-center gap-4 text-sm text-text-secondary mb-6 pb-6 border-b border-gray-100">
        <span>{{ article.category }}</span>
        <span>&middot;</span>
        <span>{{ formatDate(article.createdAt) }}</span>
        <span>&middot;</span>
        <span>&#x1F441; {{ article.readCount }} 阅读</span>
      </div>

      <ArticleStats :article-id="article.id" :stats="articleStats" class="mb-6" />
      <div v-if="tags.length" class="flex gap-2 mb-6 flex-wrap">
        <TagBadge v-for="tag in tags" :key="tag.id" :tag="tag" />
      </div>
      <div class="prose max-w-none mb-12" v-html="renderedContent" />
      <div class="border-t border-gray-100 pt-8">
        <h2 class="text-lg font-bold mb-4">评论 ({{ article.commentCount }})</h2>
        <CommentList :article-id="article.id" />
      </div>
    </div>
  </div>

  <!-- ====== Single-column layout (no images) ====== -->
  <div v-else-if="article && !hasImages" class="max-w-3xl mx-auto px-4 py-6">
    <NButton text size="small" @click="router.back()" class="mb-4">
      <template #icon>
        <NIcon size="18"><ArrowBackOutline /></NIcon>
      </template>
      返回
    </NButton>

    <!-- Author Info -->
    <div v-if="article.authorId" class="flex items-center gap-3 mb-4 p-3 bg-gray-50 rounded-card">
      <div class="cursor-pointer flex items-center gap-3 flex-1" @click="navigateTo(`/user/${article.authorId}`)">
        <UserAvatar
          :username="authorProfile?.nickname || authorProfile?.username || '?'"
          :src="authorProfile?.avatar"
          size="medium"
        />
        <div>
          <div class="text-sm font-medium hover:text-primary transition-colors">
            {{ authorProfile?.nickname || authorProfile?.username || '用户' + article.authorId }}
          </div>
          <div class="text-xs text-text-secondary">
            {{ authorProfile?.followerCount || 0 }} 粉丝 · {{ authorProfile?.articleCount || 0 }} 篇
          </div>
        </div>
      </div>
      <NButton
        v-if="!isOwnArticle"
        :type="following ? 'default' : 'primary'"
        size="small"
        :loading="followLoading"
        @click="handleFollow"
      >
        {{ following ? '已关注' : '+ 关注' }}
      </NButton>
    </div>

    <!-- Title + Delete -->
    <div class="flex items-start justify-between gap-4 mb-2">
      <h1 class="text-2xl md:text-3xl font-bold flex-1">{{ article.title }}</h1>
      <NPopconfirm v-if="canDelete" @positive-click="handleDelete">
        <template #trigger>
          <NButton size="small" type="error" :loading="deleting">
            <template #icon><NIcon size="16"><TrashOutline /></NIcon></template>
            删除
          </NButton>
        </template>
        确定删除此文章？所有评论、点赞数据将被一并删除。
      </NPopconfirm>
    </div>

    <!-- Meta -->
    <div class="flex items-center gap-4 text-sm text-text-secondary mb-6 pb-6 border-b border-gray-100">
      <span>{{ article.category }}</span>
      <span>&middot;</span>
      <span>{{ formatDate(article.createdAt) }}</span>
      <span>&middot;</span>
      <span>&#x1F441; {{ article.readCount }} 阅读</span>
    </div>

    <ArticleStats :article-id="article.id" :stats="articleStats" class="mb-6" />
    <div v-if="tags.length" class="flex gap-2 mb-6 flex-wrap">
      <TagBadge v-for="tag in tags" :key="tag.id" :tag="tag" />
    </div>
    <div class="prose max-w-none mb-12" v-html="renderedContent" />
    <div class="border-t border-gray-100 pt-8">
      <h2 class="text-lg font-bold mb-4">评论 ({{ article.commentCount }})</h2>
      <CommentList :article-id="article.id" />
    </div>
  </div>

  <!-- ====== Lightbox ====== -->
  <Teleport to="body">
    <div
      v-if="lightboxVisible"
      class="fixed inset-0 z-50 bg-black/95 flex items-center justify-center"
      @click="closeLightbox"
    >
      <button
        class="absolute top-4 right-4 w-10 h-10 rounded-full bg-white/20 text-white text-xl flex items-center justify-center hover:bg-white/30 transition-colors z-20"
        @click="closeLightbox"
      >
        ✕
      </button>

      <button
        v-if="article?.images && article.images.length > 1 && lightboxIndex > 0"
        class="absolute left-4 top-1/2 -translate-y-1/2 w-10 h-10 rounded-full bg-white/20 text-white flex items-center justify-center hover:bg-white/30 transition-colors z-20"
        @click.stop="lightboxIndex--"
      >
        <NIcon size="20"><ChevronBackOutline /></NIcon>
      </button>

      <img
        :src="imageUrl(article?.images?.[lightboxIndex] || '')"
        class="max-w-[90vw] max-h-[90vh] object-contain select-none"
        @click.stop
      />

      <button
        v-if="article?.images && article.images.length > 1 && lightboxIndex < article.images.length - 1"
        class="absolute right-4 top-1/2 -translate-y-1/2 w-10 h-10 rounded-full bg-white/20 text-white flex items-center justify-center hover:bg-white/30 transition-colors z-20"
        @click.stop="lightboxIndex++"
      >
        <NIcon size="20"><ChevronForwardOutline /></NIcon>
      </button>

      <div
        v-if="article?.images && article.images.length > 1"
        class="absolute bottom-6 left-1/2 -translate-x-1/2 text-white text-sm bg-black/40 px-3 py-1 rounded-full"
      >
        {{ lightboxIndex + 1 }} / {{ article.images.length }}
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { NResult, NButton, NIcon, NPopconfirm, useMessage } from 'naive-ui'
import { ArrowBackOutline, TrashOutline, ChevronBackOutline, ChevronForwardOutline } from '@vicons/ionicons5'
import { getArticleById, getArticleTags, getArticleStats, deleteArticle } from '~/api/modules/article'
import { getUserProfile, toggleFollow } from '~/api/modules/user'
import { useAuthStore } from '~/stores/auth'
import DOMPurify from 'dompurify'
import type { Article, Tag, UserProfile } from '~/types'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const message = useMessage()

const article = ref<Article | null>(null)
const tags = ref<Tag[]>([])
const articleStats = ref<Record<string, any>>({})
const authorProfile = ref<UserProfile | null>(null)
const following = ref(false)
const followLoading = ref(false)
const loading = ref(true)
const deleting = ref(false)
const error = ref<string | null>(null)

const API_BASE = 'http://localhost:8080'

const hasImages = computed(() => {
  return !!(article.value?.images && article.value.images.length > 0)
})

function imageUrl(src: string): string {
  if (!src) return ''
  if (src.startsWith('http://') || src.startsWith('https://')) return src
  return API_BASE + src
}

// ========== Lightbox ==========
const lightboxVisible = ref(false)
const lightboxIndex = ref(0)

function openLightbox(index: number) {
  lightboxIndex.value = index
  lightboxVisible.value = true
}

function closeLightbox() {
  lightboxVisible.value = false
}

function onLightboxKeydown(e: KeyboardEvent) {
  if (e.key === 'Escape') closeLightbox()
}

watch(lightboxVisible, (visible) => {
  if (visible) {
    document.addEventListener('keydown', onLightboxKeydown)
  } else {
    document.removeEventListener('keydown', onLightboxKeydown)
  }
})

// ========== Computed ==========
const articleId = computed(() => Number(route.params.id))
const canDelete = computed(() => {
  if (!article.value) return false
  return authStore.isAdmin || authStore.user?.userId === article.value.authorId
})
const isOwnArticle = computed(() => {
  return authStore.user?.userId === article.value?.authorId
})
const renderedContent = computed(() => {
  if (!article.value?.content) return ''
  const c = article.value.content.trim()
  // Detect HTML (from rich editor) vs plain text (legacy articles)
  let html: string
  if (c.startsWith('<') && /<\/[a-z][\s\S]*>/i.test(c)) {
    html = c // Rich text HTML
  } else {
    // Legacy plain text: \n → <p> wrapping
    html = c.split('\n')
      .map(line => line.trim() ? `<p>${line}</p>` : '')
      .join('')
  }
  return DOMPurify.sanitize(html)
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

// ========== Actions ==========
async function handleFollow() {
  if (!authStore.isLoggedIn) {
    navigateTo('/user/login')
    return
  }
  followLoading.value = true
  try {
    if (!article.value?.authorId) return
    const result = await toggleFollow(article.value.authorId)
    following.value = result.liked
  } catch (err: any) {
    message.error(err.message || '操作失败')
  } finally {
    followLoading.value = false
  }
}

async function handleDelete() {
  deleting.value = true
  try {
    await deleteArticle(articleId.value)
    message.success('文章已删除')
    router.replace('/')
  } catch (err: any) {
    message.error(err.message || '删除失败')
  } finally {
    deleting.value = false
  }
}

async function fetchArticle() {
  loading.value = true
  error.value = null
  try {
    const [articleData, tagsData, statsData] = await Promise.all([
      getArticleById(articleId.value),
      getArticleTags(articleId.value),
      getArticleStats(articleId.value),
    ])
    article.value = articleData
    tags.value = tagsData
    articleStats.value = statsData

    if (articleData.authorId) {
      try {
        authorProfile.value = await getUserProfile(articleData.authorId)
        following.value = authorProfile.value.followed
      } catch {
        // Non-critical
      }
    }
  } catch (err: any) {
    error.value = err.message || '加载文章失败'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchArticle()
})

onUnmounted(() => {
  document.removeEventListener('keydown', onLightboxKeydown)
})

watch(articleId, () => {
  fetchArticle()
})
</script>

<style scoped>
[style*="scrollbar-width: none"]::-webkit-scrollbar {
  display: none;
}
</style>
