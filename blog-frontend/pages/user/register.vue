<template>
  <div class="min-h-[80vh] flex items-center justify-center px-4">
    <NCard class="w-full max-w-md" title="注册 GreenRead">
      <NForm ref="formRef" :model="form" :rules="rules">
        <NFormItem label="用户名" path="username">
          <NInput v-model:value="form.username" placeholder="请输入用户名" size="large" />
        </NFormItem>
        <NFormItem label="昵称" path="nickname">
          <NInput v-model:value="form.nickname" placeholder="给自己起个昵称（选填，默认用户名为昵称）" size="large" />
        </NFormItem>
        <NFormItem label="头像">
          <div class="flex items-center gap-3">
            <!-- Preview / Placeholder -->
            <div
              class="w-12 h-12 rounded-full flex items-center justify-center text-white text-lg font-medium flex-shrink-0 cursor-pointer border-2 border-dashed border-gray-300 hover:border-primary transition-colors overflow-hidden"
              :style="{ background: avatarPreview ? 'transparent' : '#2D6A4F' }"
              @click="triggerFileInput"
            >
              <img v-if="avatarPreview" :src="avatarPreview" class="w-full h-full object-cover" />
              <span v-else>+</span>
            </div>
            <div class="flex-1">
              <NButton size="small" @click="triggerFileInput" :loading="uploading">
                {{ avatarPreview ? '更换头像' : '选择头像' }}
              </NButton>
              <p class="text-xs text-text-secondary mt-1">支持 JPG/PNG，不超过 2MB</p>
            </div>
            <input
              ref="fileInputRef"
              type="file"
              accept="image/*"
              class="hidden"
              @change="handleFileChange"
            />
          </div>
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
import { uploadAvatar } from '~/api/modules/user'

definePageMeta({
  middleware: 'guest',
})

const authStore = useAuthStore()
const message = useMessage()
const router = useRouter()

const formRef = ref<FormInst | null>(null)
const fileInputRef = ref<HTMLInputElement | null>(null)
const loading = ref(false)
const uploading = ref(false)
const avatarPreview = ref('')
const avatarUrl = ref('')
const form = reactive({
  username: '',
  nickname: '',
  password: '',
  confirmPassword: '',
})

function triggerFileInput() {
  fileInputRef.value?.click()
}

async function handleFileChange(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  if (file.size > 2 * 1024 * 1024) {
    message.warning('头像图片不能超过 2MB')
    return
  }

  // Show local preview immediately
  const reader = new FileReader()
  reader.onload = () => {
    avatarPreview.value = reader.result as string
  }
  reader.readAsDataURL(file)

  // Upload to server
  uploading.value = true
  try {
    avatarUrl.value = await uploadAvatar(file)
  } catch (err: any) {
    message.error(err.message || '头像上传失败')
    avatarPreview.value = ''
    input.value = ''
  } finally {
    uploading.value = false
  }
}

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
      nickname: form.nickname || undefined,
      avatar: avatarUrl.value || undefined,
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
