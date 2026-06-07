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
        <span>&middot;</span>
        <span>{{ formatDate(article.createdAt) }}</span>
        <span>&middot;</span>
        <span>&#x1F441; {{ article.readCount }} 阅读</span>
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
