<template>
  <div class="max-w-3xl mx-auto px-4 py-6">
    <NCard>
      <template #header>
        <div class="flex items-center justify-between">
          <div class="flex items-center gap-3">
            <NButton text size="small" @click="router.back()">
              <template #icon><NIcon size="18"><ArrowBackOutline /></NIcon></template>
            </NButton>
            <h2 class="text-lg font-bold">{{ isEdit ? '编辑文章' : '写文章' }}</h2>
          </div>
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

        <!-- Image Upload Area -->
        <div class="mb-4">
          <label class="block text-sm font-medium mb-2" style="color: #333">
            图片（最多9张）
          </label>
          <div class="flex gap-3 flex-wrap">
            <!-- Uploaded thumbnails -->
            <div
              v-for="(img, idx) in uploadedImages"
              :key="idx"
              class="relative w-20 h-20 rounded-lg overflow-hidden bg-gray-100 flex-shrink-0 group"
              :class="{ 'opacity-50': uploadingStates[idx] }"
            >
              <img :src="img" class="w-full h-full object-cover" />
              <NSpin v-if="uploadingStates[idx]" size="small" class="absolute inset-0 m-auto" />
              <button
                v-else
                class="absolute top-0.5 right-0.5 w-5 h-5 rounded-full bg-black/50 text-white text-xs flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity"
                @click="removeImage(idx)"
              >
                ✕
              </button>
              <div
                class="absolute bottom-0 left-0 right-0 h-5 bg-gradient-to-t from-black/30 to-transparent opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center"
              >
                <span class="text-white text-xs">{{ idx === 0 ? '封面' : idx + 1 }}</span>
              </div>
            </div>

            <!-- Add button -->
            <div
              v-if="uploadedImages.length < 9"
              class="w-20 h-20 rounded-lg border-2 border-dashed border-gray-300 flex flex-col items-center justify-center cursor-pointer hover:border-primary transition-colors bg-gray-50 flex-shrink-0"
              @click="triggerImageInput"
            >
              <span class="text-2xl text-gray-400">+</span>
              <span class="text-xs text-gray-400 mt-0.5">添加</span>
            </div>
          </div>
          <input
            ref="imageInputRef"
            type="file"
            accept="image/*"
            multiple
            class="hidden"
            @change="handleImageSelect"
          />
        </div>

        <NFormItem path="tags" label="分类标签">
          <NSelect
            v-model:value="selectedTagIds"
            :options="tagOptions"
            multiple
            placeholder="选择分类标签（必选，最多5个）"
            :max-tag-count="5"
            filterable
            clearable
          />
          <template #feedback>
            <span class="text-xs text-text-secondary">选中的第一个标签将作为文章分类</span>
          </template>
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
import { NCard, NForm, NFormItem, NInput, NButton, NIcon, NSelect, NSpace, NSpin, useMessage } from 'naive-ui'
import { ArrowBackOutline } from '@vicons/ionicons5'
import type { FormInst, FormRules } from 'naive-ui'
import { createArticle, updateArticle, getArticleById, getArticleTags, setArticleTags, uploadArticleImage } from '~/api/modules/article'
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
// 跟踪当前正在编辑的文章 ID：从路由 ?edit=id 初始化，或创建草稿后自动设置
const editingArticleId = ref<number | null>(null)
const isEdit = computed(() => editingArticleId.value !== null)

const form = reactive({
  title: '',
  content: '',
})

const selectedTagIds = ref<number[]>([])
const tagOptions = ref<{ label: string; value: number }[]>([])
// Map tag id to name for category derivation
const tagNameMap = ref<Record<number, string>>({})

// ========== 图片上传 ==========
const uploadedImages = ref<string[]>([])
const uploadingStates = ref<boolean[]>([])
const imageInputRef = ref<HTMLInputElement | null>(null)

function triggerImageInput() {
  imageInputRef.value?.click()
}

async function handleImageSelect(e: Event) {
  const input = e.target as HTMLInputElement
  const files = input.files
  if (!files || files.length === 0) return

  const remaining = 9 - uploadedImages.value.length
  if (files.length > remaining) {
    message.warning(`最多再添加 ${remaining} 张图片`)
    input.value = ''
    return
  }

  // Validate size first
  const overSize = Array.from(files).filter(f => f.size > 5 * 1024 * 1024)
  if (overSize.length > 0) {
    message.warning(`图片 "${overSize.map(f => f.name).join(', ')}" 超过 5MB 限制`)
    input.value = ''
    return
  }

  // Reserve slots and show local preview immediately
  const startIdx = uploadedImages.value.length
  const fileList = Array.from(files)
  for (const file of fileList) {
    uploadedImages.value.push(URL.createObjectURL(file))
    uploadingStates.value.push(true)
  }

  // Upload all in parallel
  const results = await Promise.all(
    fileList.map((file, i) => {
      const idx = startIdx + i
      return uploadArticleImage(file)
        .then(url => {
          uploadedImages.value[idx] = url.startsWith('http')
            ? url
            : `http://localhost:8080${url}`
          uploadingStates.value[idx] = false
        })
        .catch(err => {
          message.error(`上传失败: ${err.message || '未知错误'}`)
          uploadedImages.value[idx] = '' // mark as failed
          uploadingStates.value[idx] = false
        })
    })
  )

  // Remove any failed uploads (marked as empty string)
  for (let i = uploadedImages.value.length - 1; i >= 0; i--) {
    if (uploadedImages.value[i] === '' && !uploadingStates.value[i]) {
      uploadedImages.value.splice(i, 1)
      uploadingStates.value.splice(i, 1)
    }
  }

  input.value = ''
}

function removeImage(idx: number) {
  uploadedImages.value.splice(idx, 1)
  uploadingStates.value.splice(idx, 1)
}

function getRelativeUrls(): string[] {
  return uploadedImages.value.map(u => {
    if (u.startsWith('http://localhost:8080')) return u.replace('http://localhost:8080', '')
    if (u.startsWith('blob:')) return ''
    return u
  }).filter(u => u && !u.startsWith('blob:'))
}

const rules: FormRules = {
  title: [
    { required: true, message: '请输入标题', trigger: 'blur' },
    { min: 2, message: '标题至少2个字符', trigger: 'blur' },
  ],
  content: [
    { required: true, message: '请输入内容', trigger: 'blur' },
  ],
}

// 从路由 query 读取初始编辑 ID（仅用于 onMounted 初始化）
const routeEditId = computed(() => {
  const id = route.query.edit
  return id ? Number(id) : null
})

// User-specific draft key prevents leaking drafts between users
const draftKey = computed(() => `article_draft_${authStore.user?.userId || 'anonymous'}`)

// Derive category from first selected tag
function getCategory(): string {
  if (selectedTagIds.value.length === 0) return ''
  return tagNameMap.value[selectedTagIds.value[0]] || ''
}

async function fetchTags() {
  try {
    const tags = await getTagCloud('count')
    tagOptions.value = tags.map(t => {
      tagNameMap.value[t.id] = t.name
      return { label: t.name, value: t.id }
    })
  } catch {
    // non-critical
  }
}

function loadDraft() {
  if (import.meta.client) {
    const draft = localStorage.getItem(draftKey.value)
    if (draft) {
      try {
        const data = JSON.parse(draft)
        form.title = data.title || ''
        form.content = data.content || ''
        if (data.tagIds) selectedTagIds.value = data.tagIds
        if (data.images) {
          uploadedImages.value = data.images
          uploadingStates.value = data.images.map(() => false)
        }
        if (data.articleId) editingArticleId.value = data.articleId
      } catch {}
    }
  }
}

function saveDraftToLocal() {
  if (import.meta.client && authStore.user?.userId) {
    localStorage.setItem(draftKey.value, JSON.stringify({
      title: form.title,
      content: form.content,
      tagIds: selectedTagIds.value,
      images: uploadedImages.value.filter(u => !u.startsWith('blob:')),
      articleId: editingArticleId.value, // 持久化编辑中的文章 ID，刷新后可恢复
    }))
  }
}

function clearDraft() {
  if (import.meta.client) {
    localStorage.removeItem(draftKey.value)
  }
}

let autoSaveTimer: ReturnType<typeof setInterval> | null = null

onMounted(async () => {
  await fetchTags()
  if (routeEditId.value) {
    editingArticleId.value = routeEditId.value
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
    const id = editingArticleId.value!
    const article = await getArticleById(id)
    form.title = article.title
    form.content = article.content
    // Load existing tags
    const tags = await getArticleTags(id)
    selectedTagIds.value = tags.map(t => t.id)
    // Restore images
    if (article.images && article.images.length > 0) {
      uploadedImages.value = article.images.map((u: string) =>
        u.startsWith('http') ? u : `http://localhost:8080${u}`
      )
    }
  } catch (err: any) {
    message.error('加载文章失败')
  }
}

function resetForm() {
  form.title = ''
  form.content = ''
  selectedTagIds.value = []
  uploadedImages.value = []
  uploadingStates.value = []
  editingArticleId.value = null
  clearDraft()
}

async function saveArticle(status: 'draft' | 'published') {
  saving.value = true
  try {
    // Wait for any still-uploading images
    const stillUploading = uploadingStates.value.some(s => s)
    if (stillUploading) {
      message.warning('图片还在上传中，请稍候...')
      saving.value = false
      return
    }

    const imageUrls = getRelativeUrls()
    const category = getCategory()
    let articleId: number

    if (editingArticleId.value) {
      await updateArticle(editingArticleId.value, { title: form.title, category, content: form.content, status, images: imageUrls })
      articleId = editingArticleId.value
    } else {
      const article = await createArticle({ title: form.title, category, content: form.content, status, images: imageUrls })
      articleId = article.id
      // 创建成功后立即进入编辑模式，后续保存走更新逻辑
      editingArticleId.value = article.id
    }

    // Assign tags
    if (selectedTagIds.value.length > 0) {
      await setArticleTags(articleId, selectedTagIds.value)
    }

    if (status === 'draft') {
      message.success('草稿已保存到服务器')
      saveDraftToLocal()
    } else {
      resetForm()
      message.success('文章发布成功！')
      router.push(`/article/${articleId}`)
    }
  } catch (err: any) {
    message.error(err.message || '操作失败')
  } finally {
    saving.value = false
  }
}

async function saveDraft() {
  if (!form.title.trim()) {
    message.warning('请至少输入标题')
    return
  }
  await saveArticle('draft')
}

async function publish() {
  try {
    await formRef.value?.validate()
  } catch {
    message.warning('请填写必填项')
    return
  }
  await saveArticle('published')
}
</script>
