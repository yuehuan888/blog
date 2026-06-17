<template>
  <NAvatar :size="avatarSize" round :style="avatarStyle" :src="src || undefined">
    {{ displayChar }}
  </NAvatar>
</template>

<script setup lang="ts">
import { NAvatar } from 'naive-ui'

const props = withDefaults(defineProps<{
  username?: string
  src?: string
  size?: 'small' | 'medium' | 'large'
}>(), {
  username: '',
  src: '',
  size: 'medium',
})

const sizeMap: Record<string, number> = {
  small: 28,
  medium: 36,
  large: 48,
}

const avatarSize = computed(() => sizeMap[props.size])

const displayChar = computed(() => {
  return props.username?.charAt(0)?.toUpperCase() || '?'
})

const avatarStyle = computed(() => ({
  backgroundColor: props.src ? 'transparent' : '#2D6A4F',
  color: '#fff',
}))
</script>
