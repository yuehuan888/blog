<template>
  <NAvatar :size="avatarSize" round :style="avatarStyle">
    {{ username?.charAt(0)?.toUpperCase() || '?' }}
  </NAvatar>
</template>

<script setup lang="ts">
import { NAvatar } from 'naive-ui'

const props = withDefaults(defineProps<{
  username?: string
  size?: 'small' | 'medium' | 'large'
}>(), {
  username: '',
  size: 'medium',
})

const sizeMap: Record<string, number> = {
  small: 28,
  medium: 36,
  large: 48,
}

const avatarSize = computed(() => sizeMap[props.size])

const avatarStyle = computed(() => ({
  backgroundColor: '#2D6A4F',
  color: '#fff',
}))
</script>
