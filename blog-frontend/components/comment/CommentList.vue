<template>
  <div>
    <!-- Comment Input -->
    <div v-if="authStore.isLoggedIn" class="mb-6">
      <NInput
        v-model:value="commentText"
        type="textarea"
        :placeholder="replyTarget ? `回复 用户${replyTarget}` : '写下你的评论...'"
        :autosize="{ minRows: 2, maxRows: 4 }"
      />
      <div class="flex items-center justify-between mt-2">
        <NButton
          v-if="replyTarget"
          text
          size="small"
          type="tertiary"
          @click="cancelReply"
        >
          取消回复
        </NButton>
        <span v-else />
        <NButton
          type="primary"
          size="small"
          :loading="posting"
          :disabled="!commentText.trim()"
          @click="postComment"
        >
          发表评论
        </NButton>
      </div>
    </div>
    <NuxtLink v-else to="/user/login" class="text-primary text-sm">
      登录后参与评论 →
    </NuxtLink>

    <!-- Comment List -->
    <div v-if="loading">
      <div v-for="i in 3" :key="i" class="py-3">
        <div class="skeleton h-4 w-1/4 mb-2" />
        <div class="skeleton h-3 w-full" />
      </div>
    </div>

    <EmptyState
      v-else-if="comments.length === 0"
      description="暂无评论，来发表第一条评论吧～"
    />

    <template v-else>
      <CommentItem
        v-for="comment in comments"
        :key="comment.id"
        :comment="comment"
        @reply="startReply"
        @deleted="handleCommentDeleted"
        @load-replies="loadReplies"
      />

      <!-- Pagination -->
      <div v-if="totalPages > 1" class="flex justify-center mt-6">
        <NPagination
          v-model:page="currentPage"
          :page-count="totalPages"
          @update:page="fetchComments"
        />
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { NInput, NButton, NPagination, useMessage } from 'naive-ui'
import { getTopLevelComments, createComment, getReplies } from '~/api/modules/comment'
import { useAuthStore } from '~/stores/auth'
import type { CommentDTO } from '~/types'

const props = defineProps<{
  articleId: number
}>()

const authStore = useAuthStore()
const message = useMessage()

const comments = ref<CommentDTO[]>([])
const loading = ref(true)
const posting = ref(false)
const commentText = ref('')
const replyTarget = ref<number | null>(null)
const currentPage = ref(1)
const totalPages = ref(1)

function startReply(commentId: number) {
  replyTarget.value = commentId
  commentText.value = ''
}

function cancelReply() {
  replyTarget.value = null
  commentText.value = ''
}

async function postComment() {
  if (!commentText.value.trim()) return
  posting.value = true
  try {
    await createComment({
      articleId: props.articleId,
      content: commentText.value,
      parentId: replyTarget.value,
    })
    message.success('评论发表成功')
    commentText.value = ''
    replyTarget.value = null
    await fetchComments(1)
  } catch (err: any) {
    message.error(err.message || '评论发表失败')
  } finally {
    posting.value = false
  }
}

async function fetchComments(page = 1) {
  loading.value = true
  try {
    const result = await getTopLevelComments(props.articleId, page)
    comments.value = result.records || []
    currentPage.value = page
    totalPages.value = result.pages || 1
  } catch (err: any) {
    message.error(err.message || '加载评论失败')
  } finally {
    loading.value = false
  }
}

async function loadReplies(commentId: number) {
  try {
    const result = await getReplies(commentId)
    const comment = comments.value.find(c => c.id === commentId)
    if (comment) {
      comment.replies = result.records || []
      comment.replyCount = comment.replies.length
    }
  } catch {
    message.error('加载回复失败')
  }
}

function handleCommentDeleted(commentId: number) {
  comments.value = comments.value.filter(c => c.id !== commentId)
}

onMounted(() => {
  fetchComments()
})
</script>
