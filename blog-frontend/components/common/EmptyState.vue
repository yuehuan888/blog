<template>
  <NEmpty :description="description" size="large">
    <template #extra>
      <NButton v-if="actionLabel" type="primary" @click="$emit('action')">
        {{ actionLabel }}
      </NButton>
    </template>
  </NEmpty>
</template>

<script setup lang="ts">
import { NEmpty, NButton } from 'naive-ui'

defineProps<{
  description?: string
  actionLabel?: string
}>()

defineEmits<{
  action: []
}>()
</script>
