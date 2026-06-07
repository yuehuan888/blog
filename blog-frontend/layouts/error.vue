<template>
  <div class="min-h-screen flex items-center justify-center bg-background px-4">
    <NResult
      :status="errorStatus"
      :title="errorTitle"
      :description="errorDescription"
    >
      <template #footer>
        <NSpace justify="center">
          <NButton type="primary" @click="handleBack">
            {{ error.statusCode === 404 ? '返回首页' : '重新加载' }}
          </NButton>
          <NButton v-if="error.statusCode !== 404" @click="handleBack">
            返回首页
          </NButton>
        </NSpace>
      </template>
    </NResult>
  </div>
</template>

<script setup lang="ts">
import { NResult, NButton, NSpace } from 'naive-ui'

const props = defineProps<{
  error: {
    statusCode?: number
    message?: string
  }
}>()

const errorStatus = computed(() => {
  switch (props.error?.statusCode) {
    case 404: return '404' as const
    case 403: return '403' as const
    case 500: return '500' as const
    default: return 'error' as const
  }
})

const errorTitle = computed(() => {
  switch (props.error?.statusCode) {
    case 404: return '页面不存在'
    case 403: return '无权限访问'
    case 500: return '服务器错误'
    default: return '出错了'
  }
})

const errorDescription = computed(() => {
  return props.error?.message || '请稍后再试'
})

function handleBack() {
  clearError()
  if (props.error?.statusCode === 404) {
    navigateTo('/')
  } else {
    location.reload()
  }
}
</script>
