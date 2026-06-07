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
            <NIcon size="14"><EyeOutline /></NIcon>
            {{ formatCount(article.readCount) }}
          </span>
          <span class="flex items-center gap-1">
            <NIcon size="14"><HeartOutline /></NIcon>
            {{ formatCount(article.likeCount) }}
          </span>
          <span class="flex items-center gap-1">
            <NIcon size="14"><ChatbubbleOutline /></NIcon>
            {{ formatCount(article.commentCount) }}
          </span>
        </div>
      </div>
    </div>
  </NCard>
</template>

<script setup lang="ts">
import { NCard, NTag, NIcon } from 'naive-ui'
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
