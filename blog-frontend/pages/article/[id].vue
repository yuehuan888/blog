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
import { NResult, NButton, NIcon, NPopconfirm, useMessage } from 'naive-ui'
import { ArrowBackOutline, TrashOutline } from '@vicons/ionicons5'
import { getArticleById, getArticleTags, getArticleStats, deleteArticle } from '~/api/modules/article'
import { useAuthStore } from '~/stores/auth'
import type { Article, Tag } from '~/types'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const message = useMessage()

const article = ref<Article | null>(null)
const tags = ref<Tag[]>([])
const articleStats = ref<Record<string, any>>({})
const loading = ref(true)
const deleting = ref(false)
const error = ref<string | null>(null)

const articleId = computed(() => Number(route.params.id))
const canDelete = computed(() => {
  if (!article.value) return false
  return authStore.isAdmin || authStore.user?.userId === article.value.authorId
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
