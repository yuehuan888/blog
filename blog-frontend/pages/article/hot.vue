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
