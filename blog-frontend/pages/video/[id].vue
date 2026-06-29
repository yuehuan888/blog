<template>
  <!-- Loading -->
  <div v-if="loading" class="max-w-3xl mx-auto px-4 py-6">
    <div class="skeleton h-8 w-3/4 mb-4" />
    <div class="skeleton h-4 w-1/4 mb-6" />
    <div class="skeleton h-80 rounded-card mb-6" />
    <div class="skeleton h-4 w-full mb-2" v-for="i in 5" :key="i" />
  </div>

  <!-- Error -->
  <div v-else-if="error" class="max-w-3xl mx-auto px-4 py-6">
    <NResult status="error" :title="error.includes('404') || error.includes('不存在') ? '视频不存在' : '加载失败'" :description="error">
      <template #footer>
        <NButton type="primary" @click="router.back()">返回</NButton>
      </template>
    </NResult>
  </div>

  <!-- ====== Two-column layout ====== -->
  <div v-else-if="video" class="flex flex-col lg:flex-row lg:h-[calc(100vh-56px)]">
    <!-- Left: Video Player Column -->
    <div class="lg:w-[40%] lg:flex-shrink-0 overflow-y-auto bg-[#f0f7f4] flex flex-col" style="scrollbar-width: none;">
      <!-- Back button -->
      <div class="px-4 py-2 border-b border-gray-200 bg-white">
        <NButton text size="small" @click="router.back()">
          <template #icon><NIcon size="18"><ArrowBackOutline /></NIcon></template>
          返回
        </NButton>
      </div>

      <!-- Video Player -->
      <div ref="playerContainerRef" class="relative bg-black" :style="{ minHeight: '300px' }">
        <video
          ref="videoRef"
          :src="videoUrl"
          class="w-full block"
          controls
          :poster="thumbnailSrc"
          @loadedmetadata="onMetaLoaded"
          @timeupdate="onTimeUpdate"
          @play="onPlay"
          @pause="onPause"
          @seeking="onSeeking"
        />

        <!-- Danmaku Overlay -->
        <DanmakuOverlay
          ref="danmakuRef"
          :danmaku-list="danmakuList"
          :current-time="currentTime"
          :playing="playing"
          :visible="danmakuVisible"
        />
      </div>

      <!-- Player Controls Bar -->
      <div class="bg-white px-4 py-2 flex items-center justify-between flex-wrap gap-2 border-b border-gray-200">
        <div class="flex items-center gap-3">
          <NButton size="small" @click="danmakuVisible = !danmakuVisible">
            <template #icon><NIcon size="16"><ChatbubbleOutline /></NIcon></template>
            {{ danmakuVisible ? '隐藏弹幕' : '显示弹幕' }}
          </NButton>
        </div>
        <div class="flex items-center gap-2">
          <NInput
            v-model:value="danmakuInput"
            size="tiny"
            placeholder="发送弹幕..."
            :style="{ width: '160px' }"
            @keyup.enter="sendDanmakuMsg"
          />
          <NButton size="tiny" type="primary" :disabled="!danmakuInput.trim()" @click="sendDanmakuMsg">
            发送
          </NButton>
        </div>
      </div>

      <!-- AI Summary Card -->
      <div v-if="video.aiSummary" class="bg-white px-4 py-3 border-b border-gray-200">
        <div class="flex items-center justify-between">
          <NButton size="tiny" text @click="showAiSummary = !showAiSummary">
            <template #icon><NIcon size="16"><BulbOutline /></NIcon></template>
            {{ showAiSummary ? '隐藏 AI 摘要' : '显示 AI 摘要' }}
          </NButton>
          <NButton v-if="showAiSummary" size="tiny" text type="warning" @click="handleAiSummary" :loading="analyzing">
            重新分析
          </NButton>
        </div>
        <p v-if="showAiSummary" class="text-sm text-text-secondary leading-relaxed mt-2">{{ video.aiSummary }}</p>
      </div>
      <div v-else-if="analyzing" class="bg-white px-4 py-3 border-b border-gray-200">
        <div class="flex items-center gap-2">
          <NSpin size="small" />
          <span class="text-sm text-text-secondary">AI 正在分析视频内容...</span>
        </div>
      </div>
      <div v-else class="bg-white px-4 py-3 border-b border-gray-200">
        <NButton size="small" @click="handleAiSummary" :loading="analyzing">
          <template #icon><NIcon size="16"><BulbOutline /></NIcon></template>
          AI 视频分析
        </NButton>
      </div>
    </div>

    <!-- Right: Content Column -->
    <div class="lg:w-[60%] overflow-y-auto px-6 py-6 border-l border-gray-100">
      <!-- Author Info -->
      <div v-if="video.authorId" class="flex items-center gap-3 mb-4 p-3 bg-gray-50 rounded-card">
        <div class="cursor-pointer flex items-center gap-3 flex-1" @click="navigateTo(`/user/${video.authorId}`)">
          <UserAvatar :username="authorProfile?.nickname || authorProfile?.username || '?'" :src="authorProfile?.avatar" size="medium" />
          <div>
            <div class="text-sm font-medium hover:text-primary transition-colors">
              {{ authorProfile?.nickname || authorProfile?.username || '用户' + video.authorId }}
            </div>
            <div class="text-xs text-text-secondary">
              {{ authorProfile?.followerCount || 0 }} 粉丝 · {{ authorProfile?.articleCount || 0 }} 篇
            </div>
          </div>
        </div>
        <NButton v-if="!isOwnVideo" :type="following ? 'default' : 'primary'" size="small" :loading="followLoading" @click="handleFollow">
          {{ following ? '已关注' : '+ 关注' }}
        </NButton>
      </div>

      <!-- Title + Delete -->
      <div class="flex items-start justify-between gap-4 mb-2">
        <h1 class="text-2xl md:text-3xl font-bold flex-1">{{ video.title }}</h1>
        <NPopconfirm v-if="canDelete" @positive-click="handleDelete">
          <template #trigger>
            <NButton size="small" type="error" :loading="deleting">
              <template #icon><NIcon size="16"><TrashOutline /></NIcon></template>
              删除
            </NButton>
          </template>
          确定删除此视频？所有评论、弹幕、点赞数据将被一并删除。
        </NPopconfirm>
      </div>

      <!-- Meta -->
      <div class="flex items-center gap-4 text-sm text-text-secondary mb-6 pb-6 border-b border-gray-100">
        <span>{{ video.category }}</span>
        <span>&middot;</span>
        <span>{{ formatDate(video.createdAt) }}</span>
        <span>&middot;</span>
        <span>👁 {{ video.readCount }} 播放</span>
        <span v-if="video.duration">&middot;</span>
        <span v-if="video.duration">{{ formatDuration(video.duration) }}</span>
      </div>

      <!-- Stats -->
      <ArticleStats :article-id="video.id" :stats="videoStats" class="mb-6" />

      <!-- Tags -->
      <div v-if="tags.length" class="flex gap-2 mb-6 flex-wrap">
        <TagBadge v-for="tag in tags" :key="tag.id" :tag="tag" />
      </div>

      <!-- Description -->
      <div v-if="video.content" class="prose max-w-none mb-6 text-sm text-text-secondary" v-html="renderedContent" />

      <!-- Comments -->
      <div class="border-t border-gray-100 pt-8">
        <h2 class="text-lg font-bold mb-4">评论 ({{ video.commentCount }})</h2>
        <CommentList :article-id="video.id" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { NResult, NButton, NIcon, NInput, NPopconfirm, NSpin, useMessage } from 'naive-ui'
import { ArrowBackOutline, TrashOutline, ChatbubbleOutline, BulbOutline } from '@vicons/ionicons5'
import { getArticleById, getArticleTags, getArticleStats, deleteArticle } from '~/api/modules/article'
import { getUserProfile, toggleFollow } from '~/api/modules/user'
import { getDanmaku, sendDanmaku, generateAiSummary } from '~/api/modules/video'
import { useAuthStore } from '~/stores/auth'
import DOMPurify from 'dompurify'
import DanmakuOverlay from '~/components/video/DanmakuOverlay.vue'
import type { Video, Tag, UserProfile, DanmakuItem } from '~/types'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const message = useMessage()

const video = ref<Video | null>(null)
const tags = ref<Tag[]>([])
const videoStats = ref<Record<string, any>>({})
const authorProfile = ref<UserProfile | null>(null)
const following = ref(false)
const followLoading = ref(false)
const loading = ref(true)
const deleting = ref(false)
const error = ref<string | null>(null)

const runtimeConfig = useRuntimeConfig()
const API_BASE = (runtimeConfig.public.apiBase as string) || ''

// Video player state
const videoRef = ref<HTMLVideoElement | null>(null)
const playerContainerRef = ref<HTMLElement | null>(null)
const danmakuRef = ref<InstanceType<typeof DanmakuOverlay> | null>(null)
const currentTime = ref(0)
const playing = ref(false)
const analyzing = ref(false)
const showAiSummary = ref(true)
const danmakuVisible = ref(true)
const danmakuInput = ref('')
const danmakuList = ref<DanmakuItem[]>([])
const ws = ref<WebSocket | null>(null)

const videoId = computed(() => Number(route.params.id))
const videoUrl = computed(() => {
  if (!video.value?.videoUrl) return ''
  const url = video.value.videoUrl
  if (url.startsWith('http://') || url.startsWith('https://')) return url
  return API_BASE + url
})
const thumbnailSrc = computed(() => {
  if (!video.value?.thumbnailUrl) return ''
  const url = video.value.thumbnailUrl
  if (url.startsWith('http://') || url.startsWith('https://')) return url
  return API_BASE + url
})

const canDelete = computed(() => {
  if (!video.value) return false
  return authStore.isAdmin || authStore.user?.userId === video.value.authorId
})
const isOwnVideo = computed(() => authStore.user?.userId === video.value?.authorId)

const renderedContent = computed(() => {
  if (!video.value?.content) return ''
  const c = video.value.content.trim()
  if (c.startsWith('<') && /<\/[a-z][\s\S]*>/i.test(c)) return DOMPurify.sanitize(c)
  return c.split('\n').map(line => line.trim() ? `<p>${line}</p>` : '').join('')
})

// ====== Video Events ======
function onMetaLoaded() {
  if (videoRef.value && video.value && !video.value.duration) {
    // Could update duration via patch, but skip for now
  }
}
function onTimeUpdate() {
  currentTime.value = videoRef.value?.currentTime || 0
}
function onPlay() { playing.value = true }
function onPause() { playing.value = false }
function onSeeking() {
  danmakuRef.value?.clearEmitted()
}

// ====== Danmaku ======
async function sendDanmakuMsg() {
  const content = danmakuInput.value.trim()
  if (!content || !video.value) return
  danmakuInput.value = ''

  try {
    // Send via HTTP (always works)
    const dm = await sendDanmaku(videoId.value, {
      content,
      timestampSec: currentTime.value,
      color: '#FFFFFF',
      mode: 'scroll',
    })
    // Add to local list immediately
    danmakuList.value.push(dm)
  } catch (err: any) {
    message.error(err.message || '发送弹幕失败')
  }
}

function connectWebSocket() {
  if (!authStore.token) return
  try {
    const protocol = typeof window !== 'undefined' && window.location.protocol === 'https:' ? 'wss' : 'ws'
    const host = typeof window !== 'undefined' ? window.location.hostname : 'localhost'
    const wsUrl = `${protocol}://${host}:8080/ws/video/${videoId.value}/danmaku?token=${authStore.token}`
    const socket = new WebSocket(wsUrl)
    socket.onmessage = (e) => {
      try {
        const msg = JSON.parse(e.data)
        if (msg.type === 'new') {
          danmakuList.value.push({
            id: msg.id,
            articleId: videoId.value,
            userId: msg.userId,
            nickname: msg.nickname,
            content: msg.content,
            timestampSec: msg.timestampSec,
            color: msg.color,
            mode: msg.mode,
            createdAt: new Date().toISOString(),
          })
        } else if (msg.type === 'history') {
          danmakuList.value = msg.danmakuList || []
        }
      } catch {}
    }
    socket.onerror = () => {} // Silently fall back to HTTP
    ws.value = socket
  } catch {}
}

// ====== Actions ======
async function handleFollow() {
  if (!authStore.isLoggedIn) { navigateTo('/user/login'); return }
  followLoading.value = true
  try {
    if (!video.value?.authorId) return
    const result = await toggleFollow(video.value.authorId)
    following.value = result.liked
  } catch (err: any) { message.error(err.message || '操作失败') }
  finally { followLoading.value = false }
}

async function handleAiSummary() {
  analyzing.value = true
  try {
    await generateAiSummary(videoId.value)
    message.success('AI 分析已启动，请稍后刷新查看结果')
    // Poll for result
    let attempts = 0
    const poll = setInterval(async () => {
      attempts++
      try {
        const updated = await getArticleById(videoId.value) as Video
        if (updated.aiSummary) {
          clearInterval(poll)
          video.value = updated
          analyzing.value = false
          message.success('AI 分析完成')
        } else if (attempts >= 20) {
          clearInterval(poll)
          analyzing.value = false
          message.warning('AI 分析超时，请稍后刷新页面查看')
        }
      } catch {
        clearInterval(poll)
        analyzing.value = false
      }
    }, 3000)
  } catch (err: any) {
    analyzing.value = false
    message.error(err.message || '启动分析失败')
  }
}

async function handleDelete() {
  deleting.value = true
  try {
    await deleteArticle(videoId.value)
    message.success('视频已删除')
    router.replace('/')
  } catch (err: any) { message.error(err.message || '删除失败') }
  finally { deleting.value = false }
}

async function fetchVideo() {
  loading.value = true
  error.value = null
  try {
    const [videoData, tagsData, statsData, danmakuData] = await Promise.all([
      getArticleById(videoId.value) as Promise<Video>,
      getArticleTags(videoId.value),
      getArticleStats(videoId.value),
      getDanmaku(videoId.value),
    ])
    video.value = videoData
    tags.value = tagsData
    videoStats.value = statsData
    danmakuList.value = danmakuData

    if (videoData.authorId) {
      try {
        authorProfile.value = await getUserProfile(videoData.authorId)
        following.value = authorProfile.value.followed
      } catch {}
    }
  } catch (err: any) {
    error.value = err.message || '加载视频失败'
  } finally {
    loading.value = false
  }
}

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

function formatDuration(seconds: number): string {
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return `${m}:${s.toString().padStart(2, '0')}`
}

onMounted(() => {
  fetchVideo()
  connectWebSocket()
})

onUnmounted(() => {
  ws.value?.close()
})

watch(videoId, () => {
  ws.value?.close()
  fetchVideo()
  connectWebSocket()
})
</script>
