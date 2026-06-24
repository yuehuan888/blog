<template>
  <NCard
    class="video-card overflow-hidden transition-all duration-300 hover:shadow-md hover:-translate-y-1 cursor-pointer"
    :bordered="false"
    size="small"
    @click="navigateTo(`/video/${video.id}`)"
  >
    <!-- Thumbnail with play overlay -->
    <div class="relative overflow-hidden bg-gray-900">
      <img
        v-if="video.thumbnailUrl && !imgFailed"
        :src="imageUrl(video.thumbnailUrl)"
        :alt="video.title"
        class="w-full block object-cover"
        :style="{ minHeight: '140px', maxHeight: '320px' }"
        loading="lazy"
        @error="imgFailed = true"
      />
      <div
        v-else
        class="h-36 bg-gradient-to-br from-gray-800 to-gray-900 flex items-center justify-center"
      >
        <span class="text-4xl opacity-50">🎬</span>
      </div>
      <!-- Play button overlay -->
      <div class="absolute inset-0 flex items-center justify-center">
        <div class="w-12 h-12 rounded-full bg-black/60 flex items-center justify-center transition-transform duration-200 group-hover:scale-110">
          <NIcon size="24" color="#fff"><PlayCircleOutline /></NIcon>
        </div>
      </div>
      <!-- Duration badge -->
      <span
        v-if="video.duration"
        class="absolute bottom-2 right-2 bg-black/70 text-white text-xs px-1.5 py-0.5 rounded"
      >
        {{ formatDuration(video.duration) }}
      </span>
      <!-- AI badge -->
      <span
        v-if="video.aiSummary"
        class="absolute top-2 left-2 bg-primary/80 text-white text-xs px-1.5 py-0.5 rounded"
      >
        AI 摘要
      </span>
    </div>

    <div class="p-3">
      <!-- Title -->
      <h3 class="text-sm font-bold leading-snug line-clamp-2 mb-1 text-text-primary">
        {{ video.title }}
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
        @click.stop="navigateTo(`/user/${video.authorId}`)"
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
            {{ formatCount(video.readCount) }}
          </span>
          <span class="flex items-center gap-1">
            <NIcon size="14"><HeartOutline /></NIcon>
            {{ formatCount(video.likeCount) }}
          </span>
          <span class="flex items-center gap-1">
            <NIcon size="14"><ChatbubbleOutline /></NIcon>
            {{ formatCount(video.commentCount) }}
          </span>
        </div>
      </div>
    </div>
  </NCard>
</template>

<script setup lang="ts">
import { NCard, NTag, NIcon } from 'naive-ui'
import { PlayCircleOutline, EyeOutline, HeartOutline, ChatbubbleOutline } from '@vicons/ionicons5'
import { getArticleTags } from '~/api/modules/article'
import { getUserProfile } from '~/api/modules/user'
import type { Video, Tag, UserProfile } from '~/types'

const API_BASE = 'http://localhost:8080'

const props = defineProps<{
  video: Video
}>()

const tags = ref<Tag[]>([])
const author = ref<UserProfile | null>(null)
const imgFailed = ref(false)

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

function formatDuration(seconds: number): string {
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return `${m}:${s.toString().padStart(2, '0')}`
}

async function fetchTags() {
  try {
    tags.value = await getArticleTags(props.video.id)
  } catch { /* non-critical */ }
}

async function fetchAuthor() {
  if (!props.video.authorId) return
  try {
    author.value = await getUserProfile(props.video.authorId)
  } catch { /* non-critical */ }
}

watch(() => props.video.thumbnailUrl, () => { imgFailed.value = false })

onMounted(() => { fetchTags(); fetchAuthor() })
</script>
