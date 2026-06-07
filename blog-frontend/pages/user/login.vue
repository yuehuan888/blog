<template>
  <div class="min-h-[80vh] flex items-center justify-center px-4">
    <NCard class="w-full max-w-md" title="登录 GreenRead">
      <NForm ref="formRef" :model="form" :rules="rules">
        <NFormItem label="用户名" path="username">
          <NInput v-model:value="form.username" placeholder="请输入用户名" size="large" />
        </NFormItem>
        <NFormItem label="密码" path="password">
          <NInput
            v-model:value="form.password"
            type="password"
            placeholder="请输入密码"
            size="large"
            @keyup.enter="handleLogin"
          />
        </NFormItem>
        <NButton
          type="primary"
          block
          size="large"
          :loading="loading"
          @click="handleLogin"
        >
          登录
        </NButton>
      </NForm>
      <template #footer>
        <div class="text-center text-sm text-text-secondary">
          还没有账号？
          <NuxtLink to="/user/register" class="text-primary font-medium">立即注册</NuxtLink>
        </div>
      </template>
    </NCard>
  </div>
</template>

<script setup lang="ts">
import { NCard, NForm, NFormItem, NInput, NButton, useMessage } from 'naive-ui'
import type { FormInst, FormRules } from 'naive-ui'
import { useAuthStore } from '~/stores/auth'

definePageMeta({
  middleware: 'guest',
})

const authStore = useAuthStore()
const message = useMessage()
const router = useRouter()

const formRef = ref<FormInst | null>(null)
const loading = ref(false)
const form = reactive({
  username: '',
  password: '',
})

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, message: '用户名至少2个字符', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6个字符', trigger: 'blur' },
  ],
}

async function handleLogin() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }

  loading.value = true
  try {
    await authStore.login({
      username: form.username,
      password: form.password,
    })
    message.success('登录成功')
    router.push('/')
  } catch (err: any) {
    message.error(err.message || '登录失败，请检查用户名和密码')
  } finally {
    loading.value = false
  }
}
</script>
