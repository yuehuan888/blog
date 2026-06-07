import type { Config } from 'tailwindcss'
import typography from '@tailwindcss/typography'

export default {
  content: [
    './components/**/*.{vue,js,ts}',
    './pages/**/*.{vue,js,ts}',
    './layouts/**/*.{vue,js,ts}',
    './app.vue',
  ],
  theme: {
    extend: {
      colors: {
        primary: {
          DEFAULT: '#2D6A4F',
          light: '#52B788',
          pale: '#E8F5E9',
        },
        surface: '#FFFFFF',
        background: '#F0F7F4',
        'text-primary': '#1A1A2E',
        'text-secondary': '#6B7280',
        accent: '#FF6B6B',
      },
      borderRadius: {
        card: '12px',
        tag: '20px',
      },
      fontFamily: {
        sans: ['PingFang SC', 'Microsoft YaHei', 'sans-serif'],
      },
    },
  },
  plugins: [
    typography,
  ],
} satisfies Config
