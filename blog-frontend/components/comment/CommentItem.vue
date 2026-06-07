<template>
  <div class="py-3">
    <div class="flex gap-3">
      <UserAvatar :username="comment.userId?.toString() || '?'" size="small" />
      <div class="flex-1 min-w-0">
        <!-- Header -->
        <div class="flex items-center gap-2 mb-1">
          <span class="text-sm font-medium">用户{{ comment.userId }}</span>
          <span class="text-xs text-text-secondary">{{ formatDate(comment.createdAt) }}</span>
          <NTag v-if="comment.status === 'deleted'" size="tiny" type="error">已删除</NTag>
          <NTag v-if="comment.status === 'hidden'" size="tiny" type="warning">已隐藏</NTag>
        </div>

        <!-- Content -->
        <p
          class="text-sm text-text-primary leading-relaxed mb-2"
          :class="{ 'italic text-text-secondary': comment.status !== 'visible' }"
        >
          {{ comment.status === 'deleted' ? '该评论已被删除' : comment.content }}
        </p>

        <!-- Actions -->
        <div v-if="comment.status === 'visible'" class="flex items-center gap-4 text-xs text-text-secondary">
          <NButton text size="tiny" @click="handleLike">
            <template #icon>
              <NIcon size="14"><HeartOutline /></NIcon>
            </template>
            {{ comment.likeCount }}
          </NButton>
          <NButton
            text
            size="tiny"
            @click="$emit('reply', comment.id)"
            v-if="!isReply"
          >
            回复
          </NButton>
          <NButton
            v-if="canDelete"
            text
            size="tiny"
            type="error"
            @click="handleDelete"
          >
            删除
          </NButton>
        </div>

        <!-- Nested Replies -->
        <div v-if="comment.replies && comment.replies.length > 0" class="mt-2 pl-4 border-l-2 border-gray-100">
          <CommentItem
            v-for="reply in comment.replies"
            :key="reply.id"
            :comment="reply"
            is-reply
            @reply="$emit('reply', $event)"
            @deleted="$emit('deleted', $event)"
          />
        </div>

        <!-- Show more replies link -->
        <NButton
          v-if="comment.replyCount > (comment.replies?.length || 0)"
          text
          size="tiny"
          type="primary"
          class="mt-2"
          @click="$emit('loadReplies', comment.id)"
        >
          查看全部 {{ comment.replyCount }} 条回复
        </NButton>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { NButton, NTag, NIcon, useMessage } from 'naive-ui'
import { HeartOutline } from '@vicons/ionicons5'
import { toggleCommentLike, deleteComment } from '~/api/modules/comment'
import { useAuthStore } from '~/stores/auth'
import type { CommentDTO } from '~/types'

const props = withDefaults(defineProps<{
  comment: CommentDTO
  isReply?: boolean
}>(), {
  isReply: false,
})

const emit = defineEmits<{
  reply: [commentId: number]
  deleted: [commentId: number]
  loadReplies: [commentId: number]
}>()

const authStore = useAuthStore()
const message = useMessage()

const canDelete = computed(() =>
  authStore.isAdmin || authStore.user?.userId === props.comment.userId
)

function formatDate(dateStr: string): string {
  const d = new Date(dateStr)
  const now = new Date()
  const diff = now.getTime() - d.getTime()
  const mins = Math.floor(diff / 60000)
  if (mins < 1) return '刚刚'
  if (mins < 60) return `${mins}分钟前`
  const hours = Math.floor(mins / 60)
  if (hours < 24) return `${hours}小时前`
  return d.toLocaleDateString('zh-CN')
}

async function handleLike() {
  try {
    await toggleCommentLike(props.comment.id)
    props.comment.likeCount++
  } catch {
    message.error('操作失败')
  }
}

async function handleDelete() {
  try {
    await deleteComment(props.comment.id)
    emit('deleted', props.comment.id)
    message.success('评论已删除')
  } catch (err: any) {
    message.error(err.message || '删除失败')
  }
}
</script>
