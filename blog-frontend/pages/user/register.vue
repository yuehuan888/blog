<template>
  <div class="min-h-[80vh] flex items-center justify-center px-4">
    <NCard class="w-full max-w-md" title="注册 GreenRead">
      <NForm ref="formRef" :model="form" :rules="rules">
        <NFormItem label="用户名" path="username">
          <NInput v-model:value="form.username" placeholder="请输入用户名" size="large" />
        </NFormItem>
        <NFormItem label="密码" path="password">
          <NInput
            v-model:value="form.password"
            type="password"
            placeholder="请设置密码（至少6位）"
            size="large"
          />
        </NFormItem>
        <NFormItem label="确认密码" path="confirmPassword">
          <NInput
            v-model:value="form.confirmPassword"
            type="password"
            placeholder="请再次输入密码"
            size="large"
            @keyup.enter="handleRegister"
          />
        </NFormItem>
        <NButton
          type="primary"
          block
          size="large"
          :loading="loading"
          @click="handleRegister"
        >
          注册
        </NButton>
      </NForm>
      <template #footer>
        <div class="text-center text-sm text-text-secondary">
          已有账号？
          <NuxtLink to="/user/login" class="text-primary font-medium">立即登录</NuxtLink>
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
  confirmPassword: '',
})

function validateConfirmPassword(_rule: any, value: string) {
  if (value !== form.password) {
    return new Error('两次输入的密码不一致')
  }
  return true
}

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, message: '用户名至少2个字符', trigger: 'blur' },
    { max: 20, message: '用户名最多20个字符', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请设置密码', trigger: 'blur' },
    { min: 6, message: '密码至少6个字符', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' },
  ],
}

async function handleRegister() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }

  loading.value = true
  try {
    await authStore.register({
      username: form.username,
      password: form.password,
    })
    message.success('注册成功！欢迎加入 GreenRead')
    router.push('/')
  } catch (err: any) {
    message.error(err.message || '注册失败，请稍后重试')
  } finally {
    loading.value = false
  }
}
</script>
