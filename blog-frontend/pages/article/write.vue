<template>
  <div class="max-w-3xl mx-auto px-4 py-6">
    <NCard>
      <template #header>
        <div class="flex items-center justify-between">
          <h2 class="text-lg font-bold">{{ isEdit ? '编辑文章' : '写文章' }}</h2>
          <NSpace>
            <NButton @click="saveDraft" :loading="saving">保存草稿</NButton>
            <NButton type="primary" @click="publish" :loading="saving">发布</NButton>
          </NSpace>
        </div>
      </template>

      <NForm ref="formRef" :model="form" :rules="rules">
        <NFormItem path="title" label="标题">
          <NInput
            v-model:value="form.title"
            placeholder="请输入文章标题"
            size="large"
          />
        </NFormItem>

        <NFormItem path="category" label="分类">
          <NInput
            v-model:value="form.category"
            placeholder="例如：旅行、美食、技术..."
          />
        </NFormItem>

        <NFormItem path="tags" label="标签">
          <NSelect
            v-model:value="selectedTagIds"
            :options="tagOptions"
            multiple
            placeholder="选择标签（可选，最多5个）"
            :max-tag-count="5"
            filterable
            clearable
          />
        </NFormItem>

        <NFormItem path="content" label="内容">
          <NInput
            v-model:value="form.content"
            type="textarea"
            placeholder="写下你想分享的内容..."
            :autosize="{ minRows: 12, maxRows: 30 }"
          />
        </NFormItem>
      </NForm>
    </NCard>
  </div>
</template>

<script setup lang="ts">
import { NCard, NForm, NFormItem, NInput, NButton, NSelect, NSpace, useMessage } from 'naive-ui'
import type { FormInst, FormRules } from 'naive-ui'
import { createArticle, updateArticle, getArticleById, getArticleTags, setArticleTags } from '~/api/modules/article'
import { getTagCloud } from '~/api/modules/tag'
import { useAuthStore } from '~/stores/auth'

definePageMeta({
  middleware: 'auth',
})

const message = useMessage()
const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const formRef = ref<FormInst | null>(null)
const saving = ref(false)
const isEdit = ref(false)

const form = reactive({
  title: '',
  category: '',
  content: '',
})

const selectedTagIds = ref<number[]>([])
const tagOptions = ref<{ label: string; value: number }[]>([])

const rules: FormRules = {
  title: [
    { required: true, message: '请输入标题', trigger: 'blur' },
    { min: 2, message: '标题至少2个字符', trigger: 'blur' },
  ],
  content: [
    { required: true, message: '请输入内容', trigger: 'blur' },
  ],
}

const editId = computed(() => {
  const id = route.query.edit
  return id ? Number(id) : null
})

async function fetchTags() {
  try {
    const tags = await getTagCloud('count')
    tagOptions.value = tags.map(t => ({ label: t.name, value: t.id }))
  } catch {
    // non-critical
  }
}

function loadDraft() {
  if (import.meta.client) {
    const draft = localStorage.getItem('article_draft')
    if (draft) {
      try {
        const data = JSON.parse(draft)
        form.title = data.title || ''
        form.category = data.category || ''
        form.content = data.content || ''
        if (data.tagIds) selectedTagIds.value = data.tagIds
      } catch {}
    }
  }
}

function saveDraftToLocal() {
  if (import.meta.client) {
    localStorage.setItem('article_draft', JSON.stringify({
      title: form.title,
      category: form.category,
      content: form.content,
      tagIds: selectedTagIds.value,
    }))
  }
}

function clearDraft() {
  if (import.meta.client) {
    localStorage.removeItem('article_draft')
  }
}

let autoSaveTimer: ReturnType<typeof setInterval> | null = null

onMounted(async () => {
  await fetchTags()
  if (editId.value) {
    isEdit.value = true
    await fetchArticleForEdit()
  } else {
    loadDraft()
  }
  autoSaveTimer = setInterval(saveDraftToLocal, 30000)
})

onUnmounted(() => {
  if (autoSaveTimer) clearInterval(autoSaveTimer)
})

async function fetchArticleForEdit() {
  try {
    const article = await getArticleById(editId.value!)
    form.title = article.title
    form.category = article.category || ''
    form.content = article.content
    // Load existing tags
    const tags = await getArticleTags(editId.value!)
    selectedTagIds.value = tags.map(t => t.id)
  } catch (err: any) {
    message.error('加载文章失败')
  }
}

async function saveDraft() {
  saving.value = true
  try {
    if (isEdit.value && editId.value) {
      await updateArticle(editId.value, {
        title: form.title || '未命名草稿',
        category: form.category,
        content: form.content,
        status: 'draft',
      })
      if (selectedTagIds.value.length > 0) {
        await setArticleTags(editId.value, selectedTagIds.value)
      }
      message.success('草稿已保存')
    } else {
      const article = await createArticle({
        title: form.title || '未命名草稿',
        category: form.category,
        content: form.content,
        status: 'draft',
      })
      if (selectedTagIds.value.length > 0) {
        await setArticleTags(article.id, selectedTagIds.value)
      }
      message.success('草稿已保存到服务器')
    }
    saveDraftToLocal()
  } catch (err: any) {
    message.error(err.message || '保存草稿失败')
  } finally {
    saving.value = false
  }
}

async function publish() {
  try {
    await formRef.value?.validate()
  } catch {
    message.warning('请填写必填项')
    return
  }

  saving.value = true
  try {
    if (isEdit.value && editId.value) {
      await updateArticle(editId.value, {
        title: form.title,
        category: form.category,
        content: form.content,
        status: 'published',
      })
      if (selectedTagIds.value.length > 0) {
        await setArticleTags(editId.value, selectedTagIds.value)
      }
      message.success('文章已更新')
      router.push(`/article/${editId.value}`)
    } else {
      const article = await createArticle({
        title: form.title,
        category: form.category,
        content: form.content,
        status: 'published',
      })
      if (selectedTagIds.value.length > 0) {
        await setArticleTags(article.id, selectedTagIds.value)
      }
      clearDraft()
      message.success('文章发布成功！')
      router.push(`/article/${article.id}`)
    }
  } catch (err: any) {
    message.error(err.message || '发布失败，请稍后重试')
  } finally {
    saving.value = false
  }
}
</script>
