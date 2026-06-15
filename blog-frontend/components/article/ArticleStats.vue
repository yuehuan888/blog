<template>
  <div class="flex items-center gap-6 text-sm text-text-secondary">
    <span class="flex items-center gap-1">
      <NIcon size="16"><EyeOutline /></NIcon>
      {{ stats.readCount ?? 0 }}
    </span>
    <NButton
      text
      :type="likeState.liked ? 'error' : 'default'"
      @click.stop="handleLike"
    >
      <template #icon>
        <NIcon size="16">
          <Heart v-if="likeState.liked" />
          <HeartOutline v-else />
        </NIcon>
      </template>
      {{ likeState.count }}
    </NButton>
    <NButton
      text
      :type="favoriteState.liked ? 'warning' : 'default'"
      @click.stop="handleFavorite"
    >
      <template #icon>
        <NIcon size="16">
          <Star v-if="favoriteState.liked" />
          <StarOutline v-else />
        </NIcon>
      </template>
      {{ favoriteState.count }}
    </NButton>
    <span class="flex items-center gap-1">
      <NIcon size="16"><ChatbubbleOutline /></NIcon>
      {{ stats.commentCount ?? 0 }}
    </span>
  </div>
</template>

<script setup lang="ts">
import { NButton, NIcon, useMessage } from 'naive-ui'
import { EyeOutline, HeartOutline, Heart, StarOutline, Star, ChatbubbleOutline } from '@vicons/ionicons5'
import { toggleLike, toggleFavorite } from '~/api/modules/article'

const props = defineProps<{
  articleId: number
  stats: Record<string, any>
}>()

const message = useMessage()

const likeState = reactive({
  liked: (props.stats.liked as boolean) ?? false,
  count: (props.stats.likeCount as number) ?? 0,
})
const favoriteState = reactive({
  liked: (props.stats.favorited as boolean) ?? false,
  count: (props.stats.favoriteCount as number) ?? 0,
})

async function handleLike() {
  const prevLiked = likeState.liked
  const prevCount = likeState.count
  likeState.liked = !likeState.liked
  likeState.count += likeState.liked ? 1 : -1

  try {
    const result = await toggleLike(props.articleId)
    likeState.liked = result.liked
    likeState.count = result.count
  } catch {
    likeState.liked = prevLiked
    likeState.count = prevCount
    message.error('操作失败，请稍后重试')
  }
}

async function handleFavorite() {
  const prevLiked = favoriteState.liked
  const prevCount = favoriteState.count
  favoriteState.liked = !favoriteState.liked
  favoriteState.count += favoriteState.liked ? 1 : -1

  try {
    const result = await toggleFavorite(props.articleId)
    favoriteState.liked = result.liked
    favoriteState.count = result.count
  } catch {
    favoriteState.liked = prevLiked
    favoriteState.count = prevCount
    message.error('操作失败，请稍后重试')
  }
}
</script>
