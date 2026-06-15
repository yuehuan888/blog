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

      <!-- Article Tags (replaces standalone category tag) -->
      <div v-if="tags.length > 0" class="flex gap-1 mb-2 flex-wrap">
        <NTag
          v-for="tag in tags"
          :key="tag.id"
          size="tiny"
          :bordered="false"
          type="success"
          class="cursor-pointer"
          @click.stop="navigateTo(`/tag/${tag.id}`)"
        >
          {{ tag.name }}
        </NTag>
      </div>

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
import { getArticleTags } from '~/api/modules/article'
import type { Article, Tag } from '~/types'

const props = defineProps<{
  article: Article
}>()

const tags = ref<Tag[]>([])

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

// Fetch tags for this article
async function fetchTags() {
  try {
    tags.value = await getArticleTags(props.article.id)
  } catch {
    // Non-critical, silently fail
  }
}

onMounted(fetchTags)
</script>
