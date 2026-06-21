<template>
  <NCard
    class="article-card overflow-hidden transition-all duration-300 hover:shadow-md hover:-translate-y-1 cursor-pointer"
    :bordered="false"
    size="small"
    @click="navigateTo(`/article/${article.id}`)"
  >
    <!-- Cover Image -->
    <div v-if="article.coverImage && !imgFailed" class="relative overflow-hidden bg-gray-100">
      <img
        :src="imageUrl(article.coverImage)"
        :alt="article.title"
        class="w-full block object-cover"
        :style="{ minHeight: '120px', maxHeight: '320px' }"
        loading="lazy"
        @error="imgFailed = true"
      />
      <!-- Multi-image count badge -->
      <span
        v-if="article.imageCount && article.imageCount > 1"
        class="absolute bottom-2 right-2 bg-black/60 text-white text-xs px-1.5 py-0.5 rounded"
      >
        +{{ article.imageCount - 1 }}
      </span>
    </div>

    <!-- No-image fallback -->
    <div
      v-else
      class="h-24 bg-gradient-to-br from-primary-pale to-primary/10 flex items-center justify-center text-3xl"
    >
      {{ coverEmoji }}
    </div>

    <div class="p-3">
      <!-- Title -->
      <h3 class="text-sm font-bold leading-snug line-clamp-2 mb-1 text-text-primary">
        {{ article.title }}
      </h3>

      <!-- Tags -->
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

      <!-- Author Row -->
      <div
        v-if="author"
        class="flex items-center gap-2 mt-2 pt-2 border-t border-gray-100 cursor-pointer"
        @click.stop="navigateTo(`/user/${article.authorId}`)"
      >
        <UserAvatar :username="author.nickname || author.username" :src="author.avatar" size="small" />
        <span class="text-xs text-text-secondary hover:text-primary transition-colors truncate">
          {{ author.nickname || author.username }}
        </span>
      </div>

      <!-- Meta -->
      <div class="flex items-center justify-between text-xs text-text-secondary mt-1">
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
import { getUserProfile } from '~/api/modules/user'
import type { Article, Tag, UserProfile } from '~/types'

const API_BASE = 'http://localhost:8080'

const props = defineProps<{
  article: Article
}>()

const tags = ref<Tag[]>([])
const author = ref<UserProfile | null>(null)
const imgFailed = ref(false)

const emojis = ['📝', '🌿', '📷', '🎨', '🍃', '✨', '📖', '🌸', '🌲', '🖋️']

const coverEmoji = computed(() => {
  const hash = props.article.title.split('').reduce((a, c) => a + c.charCodeAt(0), 0)
  return emojis[hash % emojis.length]
})

function imageUrl(src: string): string {
  if (!src) return ''
  if (src.startsWith('http://') || src.startsWith('https://')) return src
  return API_BASE + src
}

function formatCount(n: number): string {
  if (n >= 10000) return (n / 10000).toFixed(1) + 'w'
  if (n >= 1000) return (n / 1000).toFixed(1) + 'k'
  return String(n)
}

async function fetchTags() {
  try {
    tags.value = await getArticleTags(props.article.id)
  } catch {
    // Non-critical, silently fail
  }
}

async function fetchAuthor() {
  if (!props.article.authorId) return
  try {
    author.value = await getUserProfile(props.article.authorId)
  } catch {
    // Non-critical
  }
}

// Reset imgFailed when coverImage changes
watch(() => props.article.coverImage, () => {
  imgFailed.value = false
})

onMounted(() => {
  fetchTags()
  fetchAuthor()
})
</script>
