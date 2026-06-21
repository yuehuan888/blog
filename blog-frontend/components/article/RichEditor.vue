<template>
  <div class="rich-editor rounded-card overflow-hidden bg-white" style="border: 1px solid #e5e7eb;">
    <!-- Toolbar -->
    <div class="toolbar flex flex-wrap items-center gap-1 px-3 py-2 border-b border-gray-200 bg-gray-50/80 sticky top-0 z-10">
      <!-- Undo / Redo -->
      <div class="flex items-center gap-0.5 mr-1">
        <button
          class="tool-btn" title="撤销 (Ctrl+Z)"
          :class="{ 'opacity-40': !editor?.can().undo() }"
          @click="editor?.chain().focus().undo().run()"
        >
          <svg class="w-4 h-4" viewBox="0 0 24 24"><path fill="currentColor" d="M12.5 8c-2.65 0-5.05.99-6.9 2.6L2 7v9h9l-3.62-3.62c1.39-1.16 3.16-1.88 5.12-1.88c3.54 0 6.55 2.31 7.6 5.5l2.37-.78C21.08 11.03 17.15 8 12.5 8z"/></svg>
        </button>
        <button
          class="tool-btn" title="重做 (Ctrl+Y)"
          :class="{ 'opacity-40': !editor?.can().redo() }"
          @click="editor?.chain().focus().redo().run()"
        >
          <svg class="w-4 h-4" viewBox="0 0 24 24"><path fill="currentColor" d="M18.4 10.6C16.55 8.99 14.15 8 11.5 8c-4.65 0-8.58 3.03-9.96 7.22L3.9 16c1.05-3.19 4.05-5.5 7.6-5.5c1.95 0 3.73.72 5.12 1.88L13 16h9V7l-3.6 3.6z"/></svg>
        </button>
      </div>

      <div class="w-px h-5 bg-gray-300 mx-0.5" />

      <!-- Font Family -->
      <select
        class="tool-select text-xs"
        title="字体"
        @change="setFontFamily(($event.target as HTMLSelectElement).value)"
      >
        <option value="">字体</option>
        <option value="SimSun, 宋体, serif">宋体</option>
        <option value="SimHei, 黑体, sans-serif">黑体</option>
        <option value="KaiTi, 楷体, serif">楷体</option>
        <option value="Microsoft YaHei, 微软雅黑, sans-serif">微软雅黑</option>
        <option value="Arial, sans-serif">Arial</option>
        <option value="Georgia, serif">Georgia</option>
      </select>

      <!-- Font Size -->
      <select
        class="tool-select text-xs"
        title="字号"
        @change="setFontSize(($event.target as HTMLSelectElement).value)"
      >
        <option value="">字号</option>
        <option value="12px">12px</option>
        <option value="14px">14px</option>
        <option value="16px">16px</option>
        <option value="18px">18px</option>
        <option value="20px">20px</option>
        <option value="24px">24px</option>
        <option value="30px">30px</option>
      </select>

      <div class="w-px h-5 bg-gray-300 mx-0.5" />

      <!-- Text Color -->
      <div class="relative" ref="colorPickerRef">
        <button class="tool-btn" title="文字颜色" @click="showColorPicker = !showColorPicker">
          <svg class="w-4 h-4" viewBox="0 0 24 24"><path fill="currentColor" d="M9.62 12L12 5.67L14.37 12M11 3L5.5 17h2.25l1.12-3h6.25l1.13 3h2.25L13 3h-2z"/></svg>
          <div class="w-3 h-0.5 rounded-full" :style="{ background: currentTextColor || '#333' }" />
        </button>
        <div v-if="showColorPicker" class="color-picker-dropdown absolute top-full left-0 mt-1 p-2 bg-white rounded-lg shadow-lg border border-gray-200 z-50 grid grid-cols-5 gap-1.5">
          <button
            v-for="c in presetColors"
            :key="c"
            class="w-6 h-6 rounded-full border border-gray-200 hover:scale-110 transition-transform"
            :style="{ backgroundColor: c }"
            :title="c"
            @click="setColor(c); showColorPicker = false"
          />
          <button
            class="w-6 h-6 rounded-full border border-gray-200 flex items-center justify-center text-xs text-gray-400 hover:scale-110 transition-transform"
            title="清除颜色"
            @click="setColor(undefined); showColorPicker = false"
          >
            ✕
          </button>
        </div>
      </div>

      <!-- Highlight Color -->
      <div class="relative" ref="highlightPickerRef">
        <button class="tool-btn" title="背景色" @click="showHighlightPicker = !showHighlightPicker">
          <svg class="w-4 h-4" viewBox="0 0 24 24"><path fill="currentColor" d="M12.37,20.27C11.67,19.74 7.82,16.68 6.1,15.08C5.88,14.88 5.68,14.66 5.5,14.44L7.76,12.18L10.46,9.48L13.71,6.24L13.71,6.24C14.5,5.46 15.75,5.46 16.54,6.24C17.33,7.03 17.33,8.28 16.54,9.07L9.07,16.54C8.28,17.33 7.03,17.33 6.24,16.54C5.85,16.15 5.64,15.65 5.64,15.14C5.64,14.63 5.85,14.13 6.24,13.74L9.52,10.46L10.87,11.8L8.36,14.31L13.24,19.19L14.6,17.84L15.95,19.19L15.26,19.88C14.46,20.68 13.17,20.68 12.37,20.27Z"/></svg>
          <div class="w-3 h-0.5 rounded-full" :style="{ background: currentHighlightColor || '#ff0' }" />
        </button>
        <div v-if="showHighlightPicker" class="color-picker-dropdown absolute top-full left-0 mt-1 p-2 bg-white rounded-lg shadow-lg border border-gray-200 z-50 grid grid-cols-5 gap-1.5">
          <button
            v-for="c in highlightColors"
            :key="c"
            class="w-6 h-6 rounded-full border border-gray-200 hover:scale-110 transition-transform"
            :style="{ backgroundColor: c }"
            :title="c"
            @click="setHighlight(c); showHighlightPicker = false"
          />
          <button
            class="w-6 h-6 rounded-full border border-gray-200 flex items-center justify-center text-xs text-gray-400 hover:scale-110 transition-transform"
            title="清除背景"
            @click="setHighlight(undefined); showHighlightPicker = false"
          >
            ✕
          </button>
        </div>
      </div>

      <div class="w-px h-5 bg-gray-300 mx-0.5" />

      <!-- Bold / Italic / Underline / Strike -->
      <button class="tool-btn font-bold" :class="{ 'is-active': editor?.isActive('bold') }" title="粗体 (Ctrl+B)" @click="editor?.chain().focus().toggleBold().run()">
        <strong>B</strong>
      </button>
      <button class="tool-btn italic" :class="{ 'is-active': editor?.isActive('italic') }" title="斜体 (Ctrl+I)" @click="editor?.chain().focus().toggleItalic().run()">
        <em>I</em>
      </button>
      <button class="tool-btn underline" :class="{ 'is-active': editor?.isActive('underline') }" title="下划线 (Ctrl+U)" @click="editor?.chain().focus().toggleUnderline().run()">
        <u>U</u>
      </button>
      <button class="tool-btn line-through" :class="{ 'is-active': editor?.isActive('strike') }" title="删除线" @click="editor?.chain().focus().toggleStrike().run()">
        <s>S</s>
      </button>

      <div class="w-px h-5 bg-gray-300 mx-0.5" />

      <!-- Headings -->
      <button
        class="tool-btn text-sm font-bold"
        :class="{ 'is-active': editor?.isActive('heading', { level: 1 }) }"
        title="标题1"
        @click="editor?.chain().focus().toggleHeading({ level: 1 }).run()"
      >H1</button>
      <button
        class="tool-btn text-sm font-bold"
        :class="{ 'is-active': editor?.isActive('heading', { level: 2 }) }"
        title="标题2"
        @click="editor?.chain().focus().toggleHeading({ level: 2 }).run()"
      >H2</button>
      <button
        class="tool-btn text-sm font-bold"
        :class="{ 'is-active': editor?.isActive('heading', { level: 3 }) }"
        title="标题3"
        @click="editor?.chain().focus().toggleHeading({ level: 3 }).run()"
      >H3</button>

      <div class="w-px h-5 bg-gray-300 mx-0.5" />

      <!-- Paragraph -->
      <button class="tool-btn text-xs" :class="{ 'is-active': editor?.isActive('paragraph') }" title="正文" @click="editor?.chain().focus().setParagraph().run()">
        ¶
      </button>

      <!-- Bullet List / Ordered List -->
      <button class="tool-btn" :class="{ 'is-active': editor?.isActive('bulletList') }" title="无序列表" @click="editor?.chain().focus().toggleBulletList().run()">
        <svg class="w-4 h-4" viewBox="0 0 24 24"><path fill="currentColor" d="M7,5H21V7H7V5M7,13V11H21V13H7M4,4.5A1.5,1.5 0 0,1 5.5,6A1.5,1.5 0 0,1 4,7.5A1.5,1.5 0 0,1 2.5,6A1.5,1.5 0 0,1 4,4.5M4,10.5A1.5,1.5 0 0,1 5.5,12A1.5,1.5 0 0,1 4,13.5A1.5,1.5 0 0,1 2.5,12A1.5,1.5 0 0,1 4,10.5M7,19V17H21V19H7M4,16.5A1.5,1.5 0 0,1 5.5,18A1.5,1.5 0 0,1 4,19.5A1.5,1.5 0 0,1 2.5,18A1.5,1.5 0 0,1 4,16.5Z"/></svg>
      </button>
      <button class="tool-btn" :class="{ 'is-active': editor?.isActive('orderedList') }" title="有序列表" @click="editor?.chain().focus().toggleOrderedList().run()">
        <svg class="w-4 h-4" viewBox="0 0 24 24"><path fill="currentColor" d="M7,13V11H21V13H7M7,19V17H21V19H7M7,7V5H21V7H7M3,8V5H2V4H4V8H3M2,17V16H5V20H2V19H4V18.5H3V17.5H4V17H2M4.25,10A0.75,0.75 0 0,1 5,10.75C5,10.95 4.92,11.14 4.79,11.27L3.12,13H5V14H2V13.08L4,11H2V10H4.25Z"/></svg>
      </button>

      <div class="w-px h-5 bg-gray-300 mx-0.5" />

      <!-- Blockquote -->
      <button class="tool-btn" :class="{ 'is-active': editor?.isActive('blockquote') }" title="引用" @click="editor?.chain().focus().toggleBlockquote().run()">
        <svg class="w-4 h-4" viewBox="0 0 24 24"><path fill="currentColor" d="M14,17H17L19,13V7H13V13H16M6,17H9L11,13V7H5V13H8L6,17Z"/></svg>
      </button>

      <!-- Horizontal Rule -->
      <button class="tool-btn" title="分隔线" @click="editor?.chain().focus().setHorizontalRule().run()">
        <svg class="w-4 h-4" viewBox="0 0 24 24"><path fill="currentColor" d="M2,11H4V13H2V11M6,11H18V13H6V11M20,11H22V13H20V11Z"/></svg>
      </button>
    </div>

    <!-- Editor Content -->
    <TiptapEditorContent :editor="editor" class="editor-content px-4 py-3 min-h-[240px] prose max-w-none" />
  </div>
</template>

<script setup lang="ts">
import { useEditor, EditorContent as TiptapEditorContent } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import { TextStyle } from '@tiptap/extension-text-style'
import Color from '@tiptap/extension-color'
import FontFamily from '@tiptap/extension-font-family'
import Underline from '@tiptap/extension-underline'
import TextAlign from '@tiptap/extension-text-align'
import Highlight from '@tiptap/extension-highlight'
import { Mark } from '@tiptap/core'
import { watch, ref, onBeforeUnmount, onMounted, nextTick } from 'vue'

// ============================
// Props & Emits
// ============================
const props = defineProps<{
  modelValue: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

// ============================
// Custom FontSize Mark
// ============================
const FontSize = Mark.create({
  name: 'fontSize',
  addAttributes() {
    return {
      size: {
        default: null,
        parseHTML: el => el.style.fontSize || null,
      },
    }
  },
  parseHTML() {
    return [{ style: 'font-size' }]
  },
  renderHTML({ mark }) {
    if (!mark.attrs.size) return ['span']
    return ['span', { style: `font-size: ${mark.attrs.size}` }, 0]
  },
  addCommands() {
    return {
      setFontSize: (size: string) => ({ chain }) => {
        return chain().setMark('fontSize', { size }).run()
      },
      unsetFontSize: () => ({ chain }) => {
        return chain().unsetMark('fontSize').run()
      },
    }
  },
})

// ============================
// Editor instance
// ============================
const isUpdatingFromProp = ref(false)

const editor = useEditor({
  content: props.modelValue || '',
  extensions: [
    StarterKit.configure({
      heading: { levels: [1, 2, 3] },
    }),
    TextStyle,
    Color,
    FontFamily,
    Underline,
    TextAlign.configure({ types: ['heading', 'paragraph'] }),
    Highlight.configure({ multicolor: true }),
    FontSize,
  ],
  onUpdate: ({ editor }) => {
    if (isUpdatingFromProp.value) return
    const html = editor.getHTML()
    emit('update:modelValue', html)
  },
  editorProps: {
    attributes: {
      class: 'prose prose-sm max-w-none focus:outline-none',
    },
  },
})

// v-model: sync external value into editor
watch(
  () => props.modelValue,
  (newVal) => {
    if (!editor.value) return
    const currentHtml = editor.value.getHTML()
    // Only update if different to avoid cursor jumping
    if (newVal !== currentHtml) {
      isUpdatingFromProp.value = true
      editor.value.commands.setContent(newVal, { emitUpdate: false })
      nextTick(() => {
        isUpdatingFromProp.value = false
      })
    }
  }
)

onBeforeUnmount(() => {
  editor.value?.destroy()
})

// ============================
// Color Pickers
// ============================
const showColorPicker = ref(false)
const showHighlightPicker = ref(false)
const colorPickerRef = ref<HTMLElement | null>(null)
const highlightPickerRef = ref<HTMLElement | null>(null)

const presetColors = [
  '#333333', '#666666', '#999999',
  '#E60000', '#FF4D4F', '#FA541C',
  '#FA8C16', '#FADB14', '#52C41A',
  '#2D6A4F', '#1890FF', '#722ED1',
]

const highlightColors = [
  '#FFF566', '#FFD591', '#FFADD2',
  '#FF9C6E', '#B7EB8F', '#87E8DE',
  '#91D5FF', '#ADC6FF', '#D3ADF7',
]

// Close color pickers when clicking outside
function handleClickOutside(e: MouseEvent) {
  if (colorPickerRef.value && !colorPickerRef.value.contains(e.target as Node)) {
    showColorPicker.value = false
  }
  if (highlightPickerRef.value && !highlightPickerRef.value.contains(e.target as Node)) {
    showHighlightPicker.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', handleClickOutside)
})

// ============================
// Computed
// ============================
const currentTextColor = computed(() => {
  if (!editor.value) return null
  const attrs = editor.value.getAttributes('textStyle')
  return attrs?.color || null
})

const currentHighlightColor = computed(() => {
  if (!editor.value) return null
  const attrs = editor.value.getAttributes('highlight')
  return attrs?.color || null
})

// ============================
// Toolbar actions
// ============================
function setColor(color: string | undefined) {
  if (!color) {
    editor.value?.chain().focus().unsetColor().run()
    return
  }
  editor.value?.chain().focus().setColor(color).run()
}

function setHighlight(color: string | undefined) {
  if (!color) {
    editor.value?.chain().focus().unsetHighlight().run()
    return
  }
  editor.value?.chain().focus().toggleHighlight({ color }).run()
}

function setFontFamily(family: string) {
  if (!family) {
    editor.value?.chain().focus().unsetFontFamily().run()
    return
  }
  editor.value?.chain().focus().setFontFamily(family).run()
}

function setFontSize(size: string) {
  if (!size) {
    editor.value?.chain().focus().unsetFontSize().run()
    return
  }
  editor.value?.chain().focus().setFontSize(size).run()
}
</script>

<script lang="ts">
// Needed for component name resolution
export default { name: 'RichEditor' }
</script>

<style scoped>
.tool-btn {
  @apply w-7 h-7 flex items-center justify-center rounded text-gray-600;
  @apply hover:bg-gray-200 hover:text-gray-800;
  @apply transition-colors text-xs;
  border: none;
  background: transparent;
  cursor: pointer;
}

.tool-btn.is-active {
  @apply bg-primary/15 text-primary font-semibold;
}

.tool-select {
  @apply h-7 px-1.5 rounded border border-gray-200 bg-white text-gray-600;
  @apply focus:outline-none focus:border-primary;
  cursor: pointer;
  min-width: 0;
}

/* Editor content styling */
.editor-content :deep(.ProseMirror) {
  min-height: 220px;
  outline: none;
  font-size: 15px;
  line-height: 1.75;
  color: #333;
}

.editor-content :deep(.ProseMirror p.is-editor-empty:first-child::before) {
  content: attr(data-placeholder);
  color: #adb5bd;
  pointer-events: none;
  float: left;
  height: 0;
}

.editor-content :deep(.ProseMirror h1) {
  font-size: 1.8em;
  font-weight: 700;
  margin: 0.5em 0 0.3em;
  line-height: 1.3;
}

.editor-content :deep(.ProseMirror h2) {
  font-size: 1.5em;
  font-weight: 600;
  margin: 0.5em 0 0.3em;
  line-height: 1.3;
}

.editor-content :deep(.ProseMirror h3) {
  font-size: 1.25em;
  font-weight: 600;
  margin: 0.4em 0 0.2em;
  line-height: 1.3;
}

.editor-content :deep(.ProseMirror ul),
.editor-content :deep(.ProseMirror ol) {
  padding-left: 1.5em;
}

.editor-content :deep(.ProseMirror blockquote) {
  border-left: 3px solid #2D6A4F;
  padding-left: 1em;
  color: #666;
  margin: 0.5em 0;
}

.editor-content :deep(.ProseMirror hr) {
  border: none;
  border-top: 1px solid #e5e7eb;
  margin: 1em 0;
}

.editor-content :deep(.ProseMirror p) {
  margin: 0.3em 0;
}
</style>
