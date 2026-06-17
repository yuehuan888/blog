<template>
  <div
    class="user-avatar flex items-center justify-center font-medium text-white select-none flex-shrink-0 overflow-hidden"
    :class="[sizeClass, roundClass]"
    :style="avatarStyle"
    :title="username || ''"
  >
    <img v-if="realSrc" :src="realSrc" class="w-full h-full object-cover" alt="" />
    <span v-else class="leading-none">{{ displayChar }}</span>
  </div>
</template>

<script setup lang="ts">
const props = withDefaults(defineProps<{
  username?: string
  src?: string | null
  size?: 'small' | 'medium' | 'large'
}>(), {
  username: '',
  src: null,
  size: 'medium',
})

const sizeMap: Record<string, { cls: string; font: string }> = {
  small: { cls: 'w-7 h-7', font: 'text-xs' },
  medium: { cls: 'w-9 h-9', font: 'text-sm' },
  large: { cls: 'w-12 h-12', font: 'text-lg' },
}

const sizeClass = computed(() => sizeMap[props.size]?.cls || sizeMap.medium.cls)
const fontClass = computed(() => sizeMap[props.size]?.font || sizeMap.medium.font)

const realSrc = computed(() => {
  if (!props.src || props.src === 'null') return null
  return props.src
})

const roundClass = 'rounded-full'

const displayChar = computed(() => {
  return props.username?.charAt(0)?.toUpperCase() || '?'
})

const avatarStyle = computed(() => ({
  backgroundColor: '#2D6A4F',
  fontSize: undefined, // handled by Tailwind class
}))
</script>

<style scoped>
.user-avatar span {
  font-size: inherit;
}
.user-avatar.w-7 span { font-size: 10px; }
.user-avatar.w-9 span { font-size: 13px; }
.user-avatar.w-12 span { font-size: 16px; }
</style>
