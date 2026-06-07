<template>
  <div class="max-w-6xl mx-auto px-4 py-6">
    <div class="flex items-center gap-3 mb-6">
      <NButton text @click="router.back()">
        <template #icon><NIcon><ArrowBackOutline /></NIcon></template>
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
import { NButton, NPagination, NIcon } from 'naive-ui'
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
