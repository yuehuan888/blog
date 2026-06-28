<template>
  <div class="max-w-4xl mx-auto px-4 py-8">
    <h1 class="text-2xl font-bold mb-8 text-center">🏷 标签云</h1>

    <!-- 加载骨架 -->
    <div v-if="loading" class="flex flex-wrap justify-center items-center gap-4 py-10">
      <div
        v-for="i in 12"
        :key="i"
        class="skeleton rounded-full"
        :style="{ width: `${60 + (i % 4) * 25}px`, height: `${60 + (i % 4) * 25}px` }"
      />
    </div>

    <NResult
      v-else-if="error"
      status="error"
      title="加载失败"
      :description="error"
    >
      <template #footer>
        <NButton type="primary" @click="fetchTags">重新加载</NButton>
      </template>
    </NResult>

    <EmptyState
      v-else-if="tags.length === 0"
      description="还没有标签，请联系管理员创建"
      action-label="标签管理（管理员）"
      @action="navigateTo('/admin/tags')"
    />

    <!-- 有机气泡云 -->
    <div
      v-else
      ref="cloudContainer"
      class="bubble-cloud"
      :style="{ height: cloudHeight + 'px' }"
    >
      <div
        v-for="(bubble, i) in bubbles"
        :key="bubble.tag.id"
        class="bubble"
        :style="bubbleStyle(bubble, i)"
        @click="navigateTo(`/tag/${bubble.tag.id}`)"
      >
        <span class="bubble-name">{{ bubble.tag.name }}</span>
        <span class="bubble-count">{{ bubble.tag.articleCount }} 篇</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { NButton, NResult } from 'naive-ui'
import { getTagCloud } from '~/api/modules/tag'
import type { TagCloudItem } from '~/types'

const tags = ref<TagCloudItem[]>([])
const loading = ref(true)
const error = ref<string | null>(null)
const cloudContainer = ref<HTMLElement | null>(null)
const cloudHeight = ref(600)

// ========== 配置 ==========

const PALETTE = [
  '#FF6B6B', '#FF9F43', '#FECA57', '#54A0FF',
  '#5F27CD', '#01A3A4', '#2ED573', '#FF6FB7',
]

const SIZE_RANGE = { min: 72, max: 160 }
const BUBBLE_PADDING = 8

interface BubbleData {
  tag: TagCloudItem
  x: number
  y: number
  radius: number
  color: string
  rotation: number
}

const bubbles = ref<BubbleData[]>([])

// ========== 工具函数 ==========

function seededRandom(seed: number): number {
  const x = Math.sin(seed * 9301 + 49297) * 233280
  return x - Math.floor(x)
}

function lighten(hex: string, amount: number): string {
  const r = parseInt(hex.slice(1, 3), 16)
  const g = parseInt(hex.slice(3, 5), 16)
  const b = parseInt(hex.slice(5, 7), 16)
  const l = (c: number) => Math.min(255, Math.round(c + (255 - c) * amount))
  return `rgb(${l(r)}, ${l(g)}, ${l(b)})`
}

// ========== 气泡布局算法 ==========

interface PlacedCircle {
  x: number
  y: number
  radius: number
}

function circlesOverlap(a: PlacedCircle, b: PlacedCircle, padding: number): boolean {
  const dx = a.x - b.x
  const dy = a.y - b.y
  const dist = Math.sqrt(dx * dx + dy * dy)
  return dist < a.radius + b.radius + padding
}

function placeBubbles(items: { tag: TagCloudItem; radius: number }[], containerWidth: number) {
  const placed: BubbleData[] = []
  const placedGeom: PlacedCircle[] = []

  const cx = containerWidth / 2
  const spiralStartY = 0

  for (const item of items) {
    const { tag, radius } = item
    let bx = cx
    let by = spiralStartY
    let found = false

    // 螺旋搜索：角度递增，半径递增
    const maxAttempts = 800
    for (let step = 0; step < maxAttempts && !found; step++) {
      const angle = step * 0.5
      const r = step * 1.8
      bx = cx + r * Math.cos(angle)
      by = spiralStartY + r * Math.sin(angle)

      // 确保不超出边界
      if (bx - radius < 0) bx = radius
      if (bx + radius > containerWidth) bx = containerWidth - radius

      const candidate: PlacedCircle = { x: bx, y: by, radius }
      const overlaps = placedGeom.some(p => circlesOverlap(candidate, p, BUBBLE_PADDING))

      if (!overlaps) {
        found = true
      }
    }

    // 如果螺旋找不到位置（极少情况），随机偏移
    if (!found) {
      bx = cx + (seededRandom(tag.id * 7 + 13) - 0.5) * containerWidth * 0.6
      by = placed.length * 40
    }

    const color = PALETTE[tag.id % PALETTE.length]
    const rotation = (seededRandom(tag.id) * 6 - 3)

    placed.push({ tag, x: bx, y: by, radius, color, rotation })
    placedGeom.push({ x: bx, y: by, radius })
  }

  // 计算包围盒
  let minY = Infinity
  let maxY = -Infinity
  for (const p of placed) {
    if (p.y - p.radius < minY) minY = p.y - p.radius
    if (p.y + p.radius > maxY) maxY = p.y + p.radius
  }

  const cloudHeight = maxY - minY
  // 容器高度 = 云团高度 + 上下各留 25% 呼吸空间
  const containerHeight = Math.max(cloudHeight * 1.5, 500)
  const verticalCenter = containerHeight / 2
  const cloudCenter = (minY + maxY) / 2
  const shiftY = verticalCenter - cloudCenter

  // 整体下移使云团居中
  for (const p of placed) {
    p.y += shiftY
  }

  return { placed, totalHeight: Math.round(containerHeight) }
}

// ========== 计算布局 ==========

function computeLayout() {
  if (tags.value.length === 0) return

  const containerWidth = cloudContainer.value
    ? cloudContainer.value.clientWidth
    : Math.min(window.innerWidth - 32, 896) // max-w-4xl - padding

  // 计算每个标签的半径
  const maxCount = Math.max(...tags.value.map(t => t.articleCount), 1)
  const items = tags.value.map(tag => {
    const safeCount = tag.articleCount || 1
    const ratio = maxCount <= 1 ? 0 : Math.log(safeCount) / Math.log(maxCount)
    const size = SIZE_RANGE.min + (SIZE_RANGE.max - SIZE_RANGE.min) * ratio
    return { tag, radius: size / 2 }
  })

  // 按半径降序排列（大圆圈优先占据中心位置）
  items.sort((a, b) => b.radius - a.radius)

  const result = placeBubbles(items, containerWidth)
  bubbles.value = result.placed
  cloudHeight.value = result.totalHeight
}

// ========== 样式生成 ==========

function bubbleStyle(bubble: BubbleData, index: number) {
  const { x, y, radius, color, rotation } = bubble
  const diameter = radius * 2

  const gradient = `radial-gradient(circle at 38% 32%, ${lighten(color, 0.25)}, ${color})`

  return {
    width: `${diameter}px`,
    height: `${diameter}px`,
    left: `${x - radius}px`,
    top: `${y - radius}px`,
    background: gradient,
    rotate: `${rotation.toFixed(1)}deg`,
    fontSize: `${Math.max(0.7, diameter / 80)}rem`,
    zIndex: Math.round(10 + rotation * 3),
  }
}

// ========== 数据加载 ==========

async function fetchTags() {
  loading.value = true
  error.value = null
  try {
    tags.value = await getTagCloud('count')
  } catch (err: any) {
    error.value = err.message || '加载标签失败'
  } finally {
    loading.value = false
    // 等 DOM 渲染出 bubble-cloud 容器后再计算布局
    await nextTick()
    if (tags.value.length > 0) {
      computeLayout()
    }
  }
}

// ========== 生命周期 ==========

function handleResize() {
  if (tags.value.length > 0) {
    computeLayout()
  }
}

onMounted(() => {
  fetchTags()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.bubble-cloud {
  position: relative;
  width: 100%;
  min-height: 400px;
  transition: height 0.3s ease;
}

.bubble {
  position: absolute;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  cursor: pointer;
  color: #fff;
  font-weight: 700;
  text-align: center;
  line-height: 1.25;
  box-shadow: 0 3px 12px rgba(0, 0, 0, 0.12);
  transition: scale 0.2s ease, box-shadow 0.2s ease;
  user-select: none;
  padding: 8px;
  overflow: hidden;
}

.bubble:hover {
  scale: 1.18;
  box-shadow: 0 8px 28px rgba(0, 0, 0, 0.2);
  z-index: 999 !important;
}

.bubble-name {
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
}

.bubble-count {
  margin-top: 2px;
  font-size: 0.62em;
  opacity: 0.85;
}
</style>
