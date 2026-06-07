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
