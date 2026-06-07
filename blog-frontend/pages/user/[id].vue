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
