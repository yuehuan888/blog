<template>
  <div
    ref="containerRef"
    class="danmaku-overlay absolute inset-0 pointer-events-none overflow-hidden z-10"
    :style="{ display: visible ? 'block' : 'none' }"
  >
    <canvas ref="canvasRef" class="absolute inset-0 w-full h-full" />
  </div>
</template>

<script setup lang="ts">
import type { DanmakuItem } from '~/types'

const props = defineProps<{
  danmakuList: DanmakuItem[]
  currentTime: number
  playing: boolean
  visible: boolean
}>()

const containerRef = ref<HTMLElement | null>(null)
const canvasRef = ref<HTMLCanvasElement | null>(null)

const TRACK_COUNT = 3
const DANMAKU_SPEED = 120 // pixels per second
const DANMAKU_DURATION = 8000 // ms to cross screen

interface ActiveDanmaku {
  id: number
  text: string
  color: string
  mode: string
  x: number
  y: number
  track: number
  startTime: number
  timestampSec: number
}

const activeDanmaku: ActiveDanmaku[] = []
const emittedIds = new Set<number>()
let animationId = 0
let lastTime = 0

function getTrackY(track: number, height: number): number {
  const trackHeight = height / TRACK_COUNT
  return track * trackHeight + trackHeight * 0.7
}

function tick(now: number) {
  const canvas = canvasRef.value
  const container = containerRef.value
  if (!canvas || !container) return

  const ctx = canvas.getContext('2d')
  if (!ctx) return

  const w = container.clientWidth
  const h = container.clientHeight

  if (canvas.width !== w || canvas.height !== h) {
    canvas.width = w
    canvas.height = h
  }

  // Spawn new danmaku
  const ct = props.currentTime
  for (const d of props.danmakuList) {
    if (!emittedIds.has(d.id) && ct >= d.timestampSec) {
      emittedIds.add(d.id)
      const track = Math.floor(Math.random() * TRACK_COUNT)
      const text = d.content
      const fontSize = 20
      ctx.font = `${fontSize}px "PingFang SC", "Microsoft YaHei", sans-serif`
      const textWidth = ctx.measureText(text).width

      activeDanmaku.push({
        id: d.id,
        text,
        color: d.color || '#FFFFFF',
        mode: d.mode || 'scroll',
        x: w,
        y: getTrackY(track, h),
        track,
        startTime: now,
        timestampSec: d.timestampSec,
      })
    }
  }

  if (!props.playing) {
    // Still render but don't advance
  }

  const dt = lastTime ? (now - lastTime) / 1000 : 0
  lastTime = now

  // Clear
  ctx.clearRect(0, 0, w, h)

  // Draw shadow on text
  ctx.shadowColor = 'rgba(0,0,0,0.8)'
  ctx.shadowBlur = 3

  // Update & draw
  for (let i = activeDanmaku.length - 1; i >= 0; i--) {
    const dm = activeDanmaku[i]
    if (dm.mode === 'scroll') {
      if (props.playing) {
        dm.x -= DANMAKU_SPEED * dt
      }
      if (dm.x < -500) {
        activeDanmaku.splice(i, 1)
        continue
      }
      ctx.font = 'bold 20px "PingFang SC", "Microsoft YaHei", sans-serif'
      ctx.fillStyle = dm.color
      ctx.fillText(dm.text, dm.x, dm.y)
    } else if (dm.mode === 'top') {
      if (now - dm.startTime > 5000) {
        activeDanmaku.splice(i, 1)
        continue
      }
      ctx.font = 'bold 20px "PingFang SC", "Microsoft YaHei", sans-serif'
      ctx.fillStyle = dm.color
      const tw = ctx.measureText(dm.text).width
      ctx.fillText(dm.text, (w - tw) / 2, dm.y)
    }
  }

  ctx.shadowBlur = 0
  animationId = requestAnimationFrame(tick)
}

function startLoop() {
  if (animationId) return
  lastTime = 0
  animationId = requestAnimationFrame(tick)
}

function stopLoop() {
  if (animationId) {
    cancelAnimationFrame(animationId)
    animationId = 0
  }
}

watch(() => props.visible, (v) => {
  if (v) startLoop()
  else stopLoop()
})

onMounted(() => {
  if (props.visible) startLoop()
})

onUnmounted(() => {
  stopLoop()
})

// Expose clear for seeking
defineExpose({
  clearEmitted() { emittedIds.clear() },
})
</script>
