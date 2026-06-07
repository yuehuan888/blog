<template>
  <NTag
    :type="clickable ? 'success' : 'default'"
    :bordered="false"
    :class="{ 'cursor-pointer hover:opacity-80': clickable }"
    @click="clickable && navigateTo(`/tag/${tag.id}`)"
  >
    {{ tag.name }}
    <template v-if="showCount">
      <span class="ml-1 opacity-60">({{ tag.articleCount }})</span>
    </template>
  </NTag>
</template>

<script setup lang="ts">
import { NTag } from 'naive-ui'
import type { Tag } from '~/types'

withDefaults(defineProps<{
  tag: Tag
  clickable?: boolean
  showCount?: boolean
}>(), {
  clickable: true,
  showCount: false,
})
</script>
