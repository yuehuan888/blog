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
      <!-- Back Button -->
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

      <!-- Stats Bar -->
      <ArticleStats
        :article-id="article.id"
        :stats="articleStats"
        class="mb-6"
      />

      <!-- Image Carousel -->
      <div
        v-if="article.images && article.images.length > 0"
        class="relative mb-6 rounded-xl overflow-hidden bg-gray-100"
        style="height: 360px"
      >
        <!-- Track -->
        <div
          ref="carouselTrack"
          class="carousel-track flex overflow-x-auto snap-x snap-mandatory h-full"
          style="scrollbar-width: none; -ms-overflow-style: none;"
          @scroll="onCarouselScroll"
        >
          <div
            v-for="(img, i) in article.images"
            :key="i"
            class="flex-shrink-0 w-full h-full snap-center flex items-center justify-center"
            @click="openLightbox(i)"
          >
            <img
              :src="carouselImageUrl(img)"
              class="max-w-full max-h-full object-contain cursor-pointer"
              :alt="`图片 ${i + 1}`"
              loading="lazy"
            />
          </div>
        </div>

        <!-- Prev button -->
        <button
          v-if="article.images.length > 1 && currentSlide > 0"
          class="absolute left-3 top-1/2 -translate-y-1/2 w-8 h-8 rounded-full bg-white/80 shadow flex items-center justify-center hover:bg-white transition-colors z-10"
          @click.stop="slideTo(currentSlide - 1)"
        >
          <NIcon size="18"><ChevronBackOutline /></NIcon>
        </button>

        <!-- Next button -->
        <button
          v-if="article.images.length > 1 && currentSlide < article.images.length - 1"
          class="absolute right-3 top-1/2 -translate-y-1/2 w-8 h-8 rounded-full bg-white/80 shadow flex items-center justify-center hover:bg-white transition-colors z-10"
          @click.stop="slideTo(currentSlide + 1)"
        >
          <NIcon size="18"><ChevronForwardOutline /></NIcon>
        </button>

        <!-- Dot indicators -->
        <div
          v-if="article.images.length > 1"
          class="absolute bottom-3 left-1/2 -translate-x-1/2 flex gap-1.5"
        >
          <span
            v-for="(_, i) in article.images"
            :key="i"
            class="rounded-full transition-all duration-200"
            :class="i === currentSlide ? 'bg-white w-4 h-2' : 'bg-white/50 w-2 h-2'"
            :style="{ width: i === currentSlide ? '16px' : '8px', height: '8px' }"
          />
        </div>
      </div>

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

  <!-- Lightbox -->
  <Teleport to="body">
    <div
      v-if="lightboxVisible"
      class="fixed inset-0 z-50 bg-black/90 flex items-center justify-center"
      @click="closeLightbox"
    >
      <!-- Close button -->
      <button
        class="absolute top-4 right-4 w-10 h-10 rounded-full bg-white/20 text-white text-xl flex items-center justify-center hover:bg-white/30 transition-colors z-20"
        @click="closeLightbox"
      >
        ✕
      </button>

      <!-- Prev -->
      <button
        v-if="article?.images && article.images.length > 1 && lightboxIndex > 0"
        class="absolute left-4 top-1/2 -translate-y-1/2 w-10 h-10 rounded-full bg-white/20 text-white flex items-center justify-center hover:bg-white/30 transition-colors z-20"
        @click.stop="lightboxIndex--"
      >
        <NIcon size="20"><ChevronBackOutline /></NIcon>
      </button>

      <!-- Image -->
      <img
        :src="carouselImageUrl(article?.images?.[lightboxIndex] || '')"
        class="max-w-[90vw] max-h-[90vh] object-contain select-none"
        @click.stop
      />

      <!-- Next -->
      <button
        v-if="article?.images && article.images.length > 1 && lightboxIndex < article.images.length - 1"
        class="absolute right-4 top-1/2 -translate-y-1/2 w-10 h-10 rounded-full bg-white/20 text-white flex items-center justify-center hover:bg-white/30 transition-colors z-20"
        @click.stop="lightboxIndex++"
      >
        <NIcon size="20"><ChevronForwardOutline /></NIcon>
      </button>

      <!-- Counter -->
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

// ========== Image Carousel ==========
const carouselTrack = ref<HTMLElement | null>(null)
const currentSlide = ref(0)
const API_BASE = 'http://localhost:8080'

function carouselImageUrl(src: string): string {
  if (!src) return ''
  if (src.startsWith('http://') || src.startsWith('https://')) return src
  return API_BASE + src
}

function onCarouselScroll() {
  if (!carouselTrack.value) return
  const track = carouselTrack.value
  const slideWidth = track.clientWidth
  if (slideWidth === 0) return
  currentSlide.value = Math.round(track.scrollLeft / slideWidth)
}

function slideTo(index: number) {
  if (!carouselTrack.value) return
  const slideWidth = carouselTrack.value.clientWidth
  carouselTrack.value.scrollTo({ left: slideWidth * index, behavior: 'smooth' })
  currentSlide.value = index
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

// Handle Escape key for lightbox
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

    // Fetch author profile (skip if article has no author)
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
.carousel-track::-webkit-scrollbar {
  display: none;
}
</style>
