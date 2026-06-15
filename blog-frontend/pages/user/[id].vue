<template>
  <div class="max-w-6xl mx-auto px-4 py-6">
    <!-- Profile Header -->
    <div class="bg-white rounded-card p-6 mb-6 shadow-sm">
      <div class="flex items-center gap-4">
        <UserAvatar :username="profileUsername" size="large" />
        <div>
          <h1 class="text-xl font-bold">{{ profileUsername || '用户' + profileUserId }}</h1>
          <p class="text-sm text-text-secondary">{{ publishedCount }} 篇发布 · {{ draftArticles.length }} 篇草稿</p>
        </div>
        <div v-if="isOwnProfile" class="ml-auto">
          <NButton type="primary" size="small" @click="navigateTo('/article/write')">
            写文章
          </NButton>
        </div>
      </div>
    </div>

    <!-- Drafts (own profile only) -->
    <template v-if="isOwnProfile && draftArticles.length > 0">
      <h2 class="text-lg font-bold mb-4">📝 草稿</h2>
      <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4 mb-8">
        <NCard
          v-for="article in draftArticles"
          :key="article.id"
          size="small"
          :bordered="false"
          class="hover:shadow-md transition-shadow opacity-70"
        >
          <div class="p-2">
            <div class="flex items-start justify-between gap-2">
              <h3
                class="text-sm font-bold line-clamp-2 mb-1 flex-1 cursor-pointer"
                @click="navigateTo(`/article/write?edit=${article.id}`)"
              >
                {{ article.title || '未命名草稿' }}
              </h3>
              <NPopconfirm @positive-click="handleDelete(article.id)">
                <template #trigger>
                  <NButton text size="tiny" type="error" @click.stop>✕</NButton>
                </template>
                确定删除此草稿？此操作不可撤销。
              </NPopconfirm>
            </div>
            <p class="text-xs text-text-secondary">{{ formatDate(article.updatedAt) }} 更新</p>
          </div>
        </NCard>
      </div>
    </template>

    <!-- Published Articles -->
    <h2 class="text-lg font-bold mb-4">发布的文章</h2>

    <div v-if="loading" class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
      <div v-for="i in 8" :key="i" class="skeleton h-48 rounded-card" />
    </div>

    <EmptyState
      v-else-if="publishedArticles.length === 0"
      :description="isOwnProfile ? '你还没有发布文章，快去写一篇吧！' : '该用户还没有发布文章'"
      :action-label="isOwnProfile ? '写文章' : undefined"
      @action="navigateTo('/article/write')"
    />

    <template v-else>
      <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
        <div v-for="article in publishedArticles" :key="article.id" class="relative group">
          <ArticleCard :article="article" />
          <!-- Delete button (own articles or admin) -->
          <NPopconfirm
            v-if="canDelete"
            @positive-click="handleDelete(article.id)"
          >
            <template #trigger>
              <NButton
                size="tiny"
                type="error"
                class="absolute top-1 right-1 opacity-0 group-hover:opacity-100 transition-opacity"
                @click.stop
              >
                ✕
              </NButton>
            </template>
            确定删除此文章？所有评论、点赞数据将被一并删除。
          </NPopconfirm>
        </div>
      </div>

      <NPagination
        v-if="totalPages > 1"
        v-model:page="currentPage"
        :page-count="totalPages"
        class="mt-8 justify-center"
        @update:page="(p: number) => { currentPage = p; fetchArticles() }"
      />
    </template>
  </div>
</template>

<script setup lang="ts">
import { NCard, NButton, NPagination, NPopconfirm, useMessage } from 'naive-ui'
import { getArticles, deleteArticle } from '~/api/modules/article'
import { useAuthStore } from '~/stores/auth'
import type { Article } from '~/types'

const route = useRoute()
const authStore = useAuthStore()
const message = useMessage()

const profileUserId = computed(() => Number(route.params.id))
const profileUsername = ref('')
const publishedArticles = ref<Article[]>([])
const draftArticles = ref<Article[]>([])
const publishedCount = ref(0)
const loading = ref(true)
const currentPage = ref(1)
const totalPages = ref(1)

const isOwnProfile = computed(() => authStore.user?.userId === profileUserId.value)
const canDelete = computed(() => isOwnProfile.value || authStore.isAdmin)

function formatDate(dateStr: string): string {
  const d = new Date(dateStr)
  const now = new Date()
  const diff = now.getTime() - d.getTime()
  const days = Math.floor(diff / 86400000)
  if (days === 0) return '今天'
  if (days === 1) return '昨天'
  if (days < 7) return `${days}天前`
  return d.toLocaleDateString('zh-CN')
}

async function fetchArticles() {
  loading.value = true
  try {
    const published = await getArticles({
      page: currentPage.value,
      size: 12,
      status: 'published',
      authorId: profileUserId.value,
    })
    publishedArticles.value = published.records || []
    publishedCount.value = published.total || 0
    totalPages.value = published.pages || 1

    if (isOwnProfile.value) {
      const drafts = await getArticles({
        page: 1,
        size: 50,
        status: 'draft',
        authorId: profileUserId.value,
      })
      draftArticles.value = drafts.records || []
    }
  } catch {
    // handled
  } finally {
    loading.value = false
  }
}

async function handleDelete(articleId: number) {
  try {
    await deleteArticle(articleId)
    message.success('已删除')
    await fetchArticles()
  } catch (err: any) {
    message.error(err.message || '删除失败')
  }
}

onMounted(fetchArticles)
watch(profileUserId, fetchArticles)
</script>
