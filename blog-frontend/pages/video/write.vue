<template>
  <div class="max-w-3xl mx-auto px-4 py-6">
    <div class="flex items-center gap-4 mb-6">
      <NButton text size="small" @click="router.back()">
        <template #icon><NIcon size="18"><ArrowBackOutline /></NIcon></template>
        返回
      </NButton>
      <h1 class="text-2xl font-bold">发布视频</h1>
    </div>

    <div class="space-y-5">
      <!-- Video Upload Area -->
      <div>
        <label class="block text-sm font-medium mb-2">视频文件 <span class="text-red-500">*</span></label>
        <div
          v-if="!uploadState.videoUrl"
          class="border-2 border-dashed rounded-xl p-8 text-center transition-colors"
          :class="dragOver ? 'border-primary bg-primary/5' : 'border-gray-300 hover:border-primary/50'"
          @dragover.prevent="dragOver = true"
          @dragleave.prevent="dragOver = false"
          @drop.prevent="handleDrop"
        >
          <NIcon size="48" class="text-gray-400 mb-3"><CloudUploadOutline /></NIcon>
          <p class="text-sm text-text-secondary mb-2">拖拽视频文件到此处，或点击下方按钮选择</p>
          <p class="text-xs text-text-secondary/60 mb-4">支持 MP4、MOV、AVI，最大 200MB</p>
          <input
            ref="fileInputRef"
            type="file"
            accept="video/*"
            class="hidden"
            @change="handleFileSelect"
          />
          <NButton type="primary" @click="fileInputRef?.click()">选择视频</NButton>
        </div>

        <!-- Upload progress -->
        <div v-if="uploadState.uploadId && !uploadState.videoUrl" class="mt-4 p-4 bg-gray-50 rounded-xl">
          <div class="flex items-center justify-between mb-3">
            <span class="text-sm font-medium">{{ uploadState.fileName }}</span>
            <span class="text-xs text-text-secondary">
              {{ uploadState.completedChunks.length }} / {{ uploadState.totalChunks }} 分片
            </span>
          </div>
          <!-- Progress bar -->
          <div class="h-2 bg-gray-200 rounded-full overflow-hidden mb-3">
            <div
              class="h-full bg-primary rounded-full transition-all duration-300"
              :style="{ width: uploadProgress + '%' }"
            />
          </div>
          <!-- Chunk grid -->
          <div class="flex flex-wrap gap-1">
            <div
              v-for="i in uploadState.totalChunks"
              :key="i"
              class="w-4 h-4 rounded-sm"
              :class="{
                'bg-green-500': uploadState.completedChunks.includes(i - 1),
                'bg-blue-400': uploadingChunks.has(i - 1),
                'bg-red-400': failedChunks.has(i - 1),
                'bg-gray-300': !uploadState.completedChunks.includes(i - 1) && !uploadingChunks.has(i - 1) && !failedChunks.has(i - 1),
              }"
              :title="'分片 ' + i"
            />
          </div>
          <p class="text-xs text-text-secondary/60 mt-2">
            {{ uploading ? '上传中...' : uploadComplete ? '上传完成，正在合并...' : '等待上传' }}
          </p>
        </div>

        <!-- Uploaded video preview -->
        <div v-if="uploadState.videoUrl" class="mt-3 p-3 bg-green-50 rounded-lg flex items-center gap-3">
          <NIcon size="24" color="#2D6A4F"><CheckmarkCircleOutline /></NIcon>
          <span class="text-sm text-primary font-medium">视频上传成功</span>
          <NButton size="tiny" @click="resetUpload">重新上传</NButton>
        </div>
      </div>

      <!-- Thumbnail -->
      <div>
        <label class="block text-sm font-medium mb-2">视频封面</label>
        <div class="flex items-center gap-4">
          <img
            v-if="thumbnailPreview"
            :src="thumbnailPreview"
            class="w-32 h-20 object-cover rounded-lg border"
          />
          <div
            v-else
            class="w-32 h-20 bg-gray-100 rounded-lg flex items-center justify-center text-2xl text-gray-400"
          >
            🎬
          </div>
          <input ref="thumbInputRef" type="file" accept="image/*" class="hidden" @change="handleThumbSelect" />
          <NButton size="small" @click="thumbInputRef?.click()">
            {{ thumbnailPreview ? '更换封面' : '选择封面' }}
          </NButton>
        </div>
      </div>

      <!-- Title -->
      <div>
        <label class="block text-sm font-medium mb-2">视频标题 <span class="text-red-500">*</span></label>
        <NInput v-model:value="form.title" placeholder="输入视频标题..." maxlength="100" show-count />
      </div>

      <!-- Tags -->
      <div>
        <label class="block text-sm font-medium mb-2">标签（最多 5 个）</label>
        <NSelect
          v-model:value="form.tagIds"
          :options="tagOptions"
          multiple
          placeholder="选择标签"
          :max-tag-count="5"
          filterable
        />
      </div>

      <!-- Description (simple textarea, not full RichEditor) -->
      <div>
        <label class="block text-sm font-medium mb-2">视频描述</label>
        <NInput
          v-model:value="form.description"
          type="textarea"
          placeholder="简要描述视频内容..."
          :autosize="{ minRows: 3, maxRows: 6 }"
          maxlength="500"
          show-count
        />
      </div>

      <!-- Category -->
      <div>
        <label class="block text-sm font-medium mb-2">分类</label>
        <NSelect
          v-model:value="form.category"
          :options="categoryOptions"
          placeholder="选择分类"
          filterable
        />
      </div>

      <!-- Actions -->
      <div class="flex gap-3 pt-4 border-t">
        <NButton
          type="primary"
          size="large"
          :loading="publishing"
          :disabled="!canPublish"
          @click="handlePublish"
        >
          发布视频
        </NButton>
        <NButton size="large" :loading="saving" :disabled="!canSave" @click="handleSaveDraft">
          保存草稿
        </NButton>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { NButton, NIcon, NInput, NSelect, useMessage } from 'naive-ui'
import { ArrowBackOutline, CloudUploadOutline, CheckmarkCircleOutline } from '@vicons/ionicons5'
import { createArticle, updateArticle, setArticleTags } from '~/api/modules/article'
import { getTagCloud } from '~/api/modules/tag'
import { uploadVideoThumbnail, computeFileHash, initChunkedUpload, uploadChunk, completeChunkedUpload } from '~/api/modules/video'
import type { ChunkedUploadState } from '~/types'

definePageMeta({ middleware: 'auth' })
const router = useRouter()
const message = useMessage()

// ====== Form ======
const form = reactive({
  title: '',
  description: '',
  category: '' as string,
  tagIds: [] as number[],
})

const tagOptions = ref<{ label: string; value: number }[]>([])
const categoryOptions = [
  { label: '科技', value: '科技' },
  { label: '生活', value: '生活' },
  { label: '美食', value: '美食' },
  { label: '旅行', value: '旅行' },
  { label: '音乐', value: '音乐' },
  { label: '游戏', value: '游戏' },
  { label: '知识', value: '知识' },
  { label: '运动', value: '运动' },
  { label: '其他', value: '其他' },
]

// ====== Upload State ======
const CHUNK_SIZE = 5 * 1024 * 1024 // 5MB
const MAX_SMALL_FILE = 10 * 1024 * 1024 // 10MB — small files upload directly
const MAX_CONCURRENT = 3

const fileInputRef = ref<HTMLInputElement | null>(null)
const thumbInputRef = ref<HTMLInputElement | null>(null)
const dragOver = ref(false)

const uploadState = reactive<ChunkedUploadState>({
  uploadId: '',
  fileHash: '',
  fileName: '',
  totalChunks: 0,
  completedChunks: [],
  objectKey: null,
  videoUrl: null,
  timestamp: 0,
})

const uploadingChunks = reactive(new Set<number>())
const failedChunks = reactive(new Set<number>())
const uploading = ref(false)
const uploadComplete = ref(false)
const thumbnailPreview = ref<string | null>(null)
const thumbnailUrl = ref<string | null>(null)
const editingArticleId = ref<number | null>(null)

const uploadProgress = computed(() => {
  if (uploadState.totalChunks === 0) return 0
  return Math.round((uploadState.completedChunks.length / uploadState.totalChunks) * 100)
})

const publishing = ref(false)
const saving = ref(false)

const canPublish = computed(() => {
  return form.title.trim() && uploadState.videoUrl && !uploading.value
})

const canSave = computed(() => {
  return (form.title.trim() || uploadState.videoUrl) && !uploading.value
})

// ====== File Handlers ======
function handleDrop(e: DragEvent) {
  dragOver.value = false
  const file = e.dataTransfer?.files?.[0]
  if (file?.type.startsWith('video/')) startUpload(file)
}

function handleFileSelect(e: Event) {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (file) startUpload(file)
}

function handleThumbSelect(e: Event) {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (!file) return
  const reader = new FileReader()
  reader.onload = () => { thumbnailPreview.value = reader.result as string }
  reader.readAsDataURL(file)
}

async function resetUpload() {
  uploadState.uploadId = ''
  uploadState.fileHash = ''
  uploadState.fileName = ''
  uploadState.totalChunks = 0
  uploadState.completedChunks = []
  uploadState.objectKey = null
  uploadState.videoUrl = null
  uploadState.timestamp = 0
  uploadingChunks.clear()
  failedChunks.clear()
  uploading.value = false
  uploadComplete.value = false
  saveLocalDraft()
}

// ====== Upload Logic ======
async function startUpload(file: File) {
  // Reset
  resetUpload()
  uploadState.fileName = file.name
  uploadState.timestamp = Date.now()

  // Compute hash
  message.info('正在计算文件哈希...')
  const fileHash = await computeFileHash(file)
  uploadState.fileHash = fileHash

  // Small file → direct upload
  if (file.size < MAX_SMALL_FILE) {
    uploading.value = true
    try {
      const { uploadVideo } = await import('~/api/modules/video')
      const result = await uploadVideo(file)
      uploadState.videoUrl = result.videoUrl
      uploadState.objectKey = result.objectKey
      uploadState.completedChunks = [0] // mark as complete
      message.success('视频上传成功')
    } catch (err: any) {
      message.error(err.message || '上传失败')
    } finally {
      uploading.value = false
    }
    saveLocalDraft()
    return
  }

  // Large file → chunked upload
  const totalChunks = Math.ceil(file.size / CHUNK_SIZE)
  uploadState.totalChunks = totalChunks
  saveLocalDraft()

  // Init
  try {
    const initResp = await initChunkedUpload({
      fileHash,
      fileName: file.name,
      totalSize: file.size,
      chunkSize: CHUNK_SIZE,
    })

    if (initResp.uploaded) {
      uploadState.videoUrl = initResp.videoUrl!
      uploadState.objectKey = initResp.objectKey
      uploadState.totalChunks = initResp.totalChunks
      uploadState.completedChunks = Array.from({ length: initResp.totalChunks }, (_, i) => i)
      message.success('文件已存在，秒传成功！')
      saveLocalDraft()
      return
    }

    uploadState.uploadId = initResp.uploadId
    uploadState.objectKey = initResp.objectKey
    uploadState.totalChunks = initResp.totalChunks
    uploadState.completedChunks = initResp.uploadedChunks
    saveLocalDraft()

    // Upload missing chunks
    const allChunks = Array.from({ length: totalChunks }, (_, i) => i)
    const missing = allChunks.filter(i => !uploadState.completedChunks.includes(i))

    await uploadMissingChunks(file, missing)

    // Complete
    uploadComplete.value = true
    const completeResp = await completeChunkedUpload(uploadState.uploadId, fileHash)
    uploadState.videoUrl = completeResp.videoUrl
    uploadState.objectKey = completeResp.objectKey
    uploadState.completedChunks = allChunks
    uploadComplete.value = false
    message.success('视频上传成功')
    saveLocalDraft()
  } catch (err: any) {
    message.error(err.message || '上传失败')
  }
}

async function uploadMissingChunks(file: File, chunks: number[]) {
  uploading.value = true
  const queue = [...chunks]
  const active: Promise<void>[] = []

  async function processNext(): Promise<void> {
    while (queue.length > 0) {
      const idx = queue.shift()!
      uploadingChunks.add(idx)
      const start = idx * CHUNK_SIZE
      const end = Math.min(start + CHUNK_SIZE, file.size)
      const blob = file.slice(start, end)

      // Retry up to 3 times
      for (let attempt = 1; attempt <= 3; attempt++) {
        try {
          await uploadChunk(uploadState.uploadId, idx, blob)
          uploadState.completedChunks.push(idx)
          uploadState.completedChunks = [...new Set(uploadState.completedChunks)]
          failedChunks.delete(idx)
          saveLocalDraft()
          break
        } catch (err) {
          if (attempt === 3) {
            failedChunks.add(idx)
            throw err
          }
          await new Promise(r => setTimeout(r, 1000 * attempt))
        }
      }
      uploadingChunks.delete(idx)
    }
  }

  // Run N concurrent uploaders
  const workers = Array.from({ length: Math.min(MAX_CONCURRENT, chunks.length) }, () => processNext())
  await Promise.all(workers)
  uploading.value = false
}

// ====== Publish / Save ======
async function handlePublish() {
  publishing.value = true
  try {
    // Upload thumbnail first
    if (thumbnailPreview.value && thumbInputRef.value?.files?.[0]) {
      thumbnailUrl.value = await uploadVideoThumbnail(thumbInputRef.value.files[0])
    }

    const articleData: any = {
      type: 'video',
      title: form.title,
      content: form.description || '',
      category: form.category || '其他',
      status: 'published',
      videoUrl: uploadState.videoUrl,
      thumbnailUrl: thumbnailUrl.value || null,
      duration: null, // Could extract from video metadata
    }

    let articleId: number

    if (editingArticleId.value) {
      await updateArticle(editingArticleId.value, articleData)
      articleId = editingArticleId.value
    } else {
      const created = await createArticle(articleData)
      articleId = created.id
      editingArticleId.value = created.id
    }

    // Set tags
    if (form.tagIds.length > 0) {
      await setArticleTags(articleId, form.tagIds)
    }

    // Clear local draft
    clearLocalDraft()
    message.success('视频发布成功！AI 摘要正在后台生成...')
    router.push(`/video/${articleId}`)
  } catch (err: any) {
    message.error(err.message || '发布失败')
  } finally {
    publishing.value = false
  }
}

async function handleSaveDraft() {
  saving.value = true
  try {
    const articleData: any = {
      type: 'video',
      title: form.title || '未命名视频',
      content: form.description || '',
      category: form.category || '其他',
      status: 'draft',
      videoUrl: uploadState.videoUrl || null,
      thumbnailUrl: thumbnailUrl.value || null,
    }

    if (editingArticleId.value) {
      await updateArticle(editingArticleId.value, articleData)
    } else {
      const created = await createArticle(articleData)
      editingArticleId.value = created.id
    }

    if (form.tagIds.length > 0 && editingArticleId.value) {
      try { await setArticleTags(editingArticleId.value, form.tagIds) } catch {}
    }

    saveLocalDraft()
    message.success('草稿已保存')
  } catch (err: any) {
    message.error(err.message || '保存失败')
  } finally {
    saving.value = false
  }
}

// ====== Local Draft ======
const DRAFT_KEY = 'video-draft'

function saveLocalDraft() {
  if (typeof window === 'undefined') return
  localStorage.setItem(DRAFT_KEY, JSON.stringify({
    form: { ...form },
    uploadState: { ...uploadState },
    thumbnailPreview: thumbnailPreview.value,
    thumbnailUrl: thumbnailUrl.value,
    editingArticleId: editingArticleId.value,
  }))
}

function loadLocalDraft() {
  if (typeof window === 'undefined') return
  const raw = localStorage.getItem(DRAFT_KEY)
  if (!raw) return
  try {
    const data = JSON.parse(raw)
    Object.assign(form, data.form || {})
    Object.assign(uploadState, data.uploadState || {})
    thumbnailPreview.value = data.thumbnailPreview || null
    thumbnailUrl.value = data.thumbnailUrl || null
    editingArticleId.value = data.editingArticleId || null
    // If there's an old upload state, verify it's still valid (< 24h)
    if (uploadState.timestamp && Date.now() - uploadState.timestamp > 24 * 3600 * 1000) {
      resetUpload()
    }
  } catch {}
}

function clearLocalDraft() {
  localStorage.removeItem(DRAFT_KEY)
}

// ====== Lifecycle ======
async function fetchTags() {
  try {
    const tags = await getTagCloud('count')
    tagOptions.value = tags.map(t => ({ label: t.name, value: t.id }))
  } catch {}
}

onMounted(() => {
  fetchTags()
  loadLocalDraft()
})

// beforeunload warning
if (typeof window !== 'undefined') {
  window.addEventListener('beforeunload', (e) => {
    if (uploading.value) {
      e.preventDefault()
      e.returnValue = '视频正在上传中，确定离开吗？'
    }
  })
}
</script>
