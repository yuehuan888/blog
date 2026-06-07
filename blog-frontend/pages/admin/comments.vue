<template>
  <div class="max-w-4xl mx-auto px-4 py-6">
    <h1 class="text-2xl font-bold mb-6">💬 评论管理</h1>

    <NResult
      v-if="error"
      status="error"
      title="加载失败"
      :description="error"
    >
      <template #footer>
        <NButton type="primary" @click="fetchComments()">重新加载</NButton>
      </template>
    </NResult>

    <EmptyState v-else-if="comments.length === 0" description="暂无评论需要管理" />

    <div v-else class="space-y-2">
      <NCard
        v-for="comment in comments"
        :key="comment.id"
        size="small"
        :bordered="false"
      >
        <div class="flex items-start justify-between gap-4">
          <div class="flex-1 min-w-0">
            <div class="flex items-center gap-2 mb-1">
              <span class="text-sm font-medium">用户{{ comment.userId }}</span>
              <NTag size="tiny" :type="statusType(comment.status)">
                {{ statusLabel(comment.status) }}
              </NTag>
            </div>
            <p class="text-sm">{{ comment.content }}</p>
          </div>
          <div class="flex gap-2 flex-shrink-0">
            <NButton
              v-if="comment.status === 'visible'"
              text
              size="small"
              type="warning"
              @click="handleHide(comment.id)"
            >
              隐藏
            </NButton>
            <NPopconfirm @positive-click="handleDelete(comment.id)">
              <template #trigger>
                <NButton text size="small" type="error">删除</NButton>
              </template>
              确定要删除这条评论吗？
            </NPopconfirm>
          </div>
        </div>
      </NCard>
    </div>
  </div>
</template>

<script setup lang="ts">
import { NCard, NTag, NButton, NPopconfirm, NResult, useMessage } from 'naive-ui'
import { hideComment, deleteComment } from '~/api/modules/comment'
import type { Comment } from '~/types'

definePageMeta({
  middleware: ['auth', 'admin'],
})

const message = useMessage()
const comments = ref<Comment[]>([])
const error = ref<string | null>(null)

async function fetchComments() {
  error.value = null
}

function statusType(status: string) {
  switch (status) {
    case 'visible': return 'success'
    case 'hidden': return 'warning'
    case 'deleted': return 'error'
    default: return 'default'
  }
}

function statusLabel(status: string) {
  switch (status) {
    case 'visible': return '正常'
    case 'hidden': return '已隐藏'
    case 'deleted': return '已删除'
    default: return status
  }
}

async function handleHide(id: number) {
  try {
    await hideComment(id)
    message.success('评论已隐藏')
    const c = comments.value.find(c => c.id === id)
    if (c) c.status = 'hidden'
  } catch (err: any) {
    message.error(err.message || '操作失败')
  }
}

async function handleDelete(id: number) {
  try {
    await deleteComment(id)
    message.success('评论已删除')
    comments.value = comments.value.filter(c => c.id !== id)
  } catch (err: any) {
    message.error(err.message || '删除失败')
  }
}
</script>
