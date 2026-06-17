<template>
  <div
    :style="avatarStyle"
    class="flex items-center justify-center font-medium select-none flex-shrink-0 overflow-hidden rounded-full"
    :title="username || ''"
  >
    <img
      v-if="realSrc"
      :src="realSrc"
      :style="{ width: px + 'px', height: px + 'px', objectFit: 'cover' }"
      alt=""
    />
    <span v-else :style="{ fontSize: fontSize + 'px', lineHeight: '1' }">{{ displayChar }}</span>
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

const config = useRuntimeConfig()

const sizeMap: Record<string, { px: number; fontSize: number }> = {
  small: { px: 28, fontSize: 10 },
  medium: { px: 36, fontSize: 13 },
  large: { px: 48, fontSize: 16 },
}

const px = computed(() => sizeMap[props.size]?.px ?? 36)
const fontSize = computed(() => sizeMap[props.size]?.fontSize ?? 13)

const realSrc = computed(() => {
  if (!props.src || props.src === 'null') return null
  if (props.src.startsWith('http')) return props.src
  return (config.public.apiBase as string) + props.src
})

const displayChar = computed(() => {
  return props.username?.charAt(0)?.toUpperCase() || '?'
})

const isImage = computed(() => !!realSrc.value)

const avatarStyle = computed(() => ({
  width: px.value + 'px',
  height: px.value + 'px',
  backgroundColor: isImage.value ? 'transparent' : '#2D6A4F',
  color: '#fff',
}))
</script>
