<template>
  <div class="max-w-2xl mx-auto px-4 py-6">
    <h1 class="text-2xl font-bold mb-6">🏷 标签管理</h1>

    <!-- Create Tag -->
    <div class="flex gap-3 mb-6">
      <NInput
        v-model:value="newTagName"
        placeholder="输入新标签名"
        @keyup.enter="handleCreate"
      />
      <NButton type="primary" :loading="creating" @click="handleCreate">创建</NButton>
    </div>

    <!-- Tag List -->
    <div v-if="loading" class="space-y-2">
      <div v-for="i in 5" :key="i" class="skeleton h-10 rounded" />
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

    <div v-else class="space-y-2">
      <NCard
        v-for="tag in tags"
        :key="tag.id"
        size="small"
        :bordered="false"
      >
        <div class="flex items-center justify-between">
          <div>
            <span class="font-medium">{{ tag.name }}</span>
            <span class="text-xs text-text-secondary ml-2">
              {{ tag.articleCount }} 篇文章 · 热度 {{ tag.hotScore }}
            </span>
          </div>
          <div class="flex gap-2">
            <NButton
              text
              size="small"
              type="primary"
              @click="startEdit(tag)"
            >
              编辑
            </NButton>
            <NPopconfirm @positive-click="handleDelete(tag.id)">
              <template #trigger>
                <NButton text size="small" type="error">删除</NButton>
              </template>
              确定删除标签 "{{ tag.name }}"？此操作不可撤销。
            </NPopconfirm>
          </div>
        </div>
      </NCard>
    </div>

    <!-- Edit Modal -->
    <NModal v-model:show="showEdit" title="编辑标签">
      <NCard style="width: 400px;" :bordered="false" title="编辑标签">
        <NInput v-model:value="editName" placeholder="标签名" />
        <template #footer>
          <NButton type="primary" @click="handleUpdate">保存</NButton>
        </template>
      </NCard>
    </NModal>
  </div>
</template>

<script setup lang="ts">
import { NCard, NInput, NButton, NModal, NPopconfirm, NResult, useMessage } from 'naive-ui'
import { getTagCloud, createTag, updateTag, deleteTag } from '~/api/modules/tag'
import type { TagCloudItem } from '~/types'

definePageMeta({
  middleware: ['auth', 'admin'],
})

const message = useMessage()

const tags = ref<TagCloudItem[]>([])
const loading = ref(true)
const error = ref<string | null>(null)
const creating = ref(false)
const newTagName = ref('')

const showEdit = ref(false)
const editingId = ref<number | null>(null)
const editName = ref('')

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

async function handleCreate() {
  if (!newTagName.value.trim()) return
  creating.value = true
  try {
    await createTag(newTagName.value.trim())
    message.success('标签创建成功')
    newTagName.value = ''
    await fetchTags()
  } catch (err: any) {
    message.error(err.message || '创建失败')
  } finally {
    creating.value = false
  }
}

function startEdit(tag: TagCloudItem) {
  editingId.value = tag.id
  editName.value = tag.name
  showEdit.value = true
}

async function handleUpdate() {
  if (!editingId.value || !editName.value.trim()) return
  try {
    await updateTag(editingId.value, editName.value.trim())
    message.success('标签更新成功')
    showEdit.value = false
    await fetchTags()
  } catch (err: any) {
    message.error(err.message || '更新失败')
  }
}

async function handleDelete(id: number) {
  try {
    await deleteTag(id)
    message.success('标签已删除')
    await fetchTags()
  } catch (err: any) {
    message.error(err.message || '删除失败')
  }
}

onMounted(fetchTags)
</script>
