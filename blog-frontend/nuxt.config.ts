// https://nuxt.com/docs/api/configuration/nuxt-config
export default defineNuxtConfig({
  devtools: { enabled: true },

  modules: [
    '@pinia/nuxt',
  ],

  css: [
    '~/assets/css/main.css',
  ],

  postcss: {
    plugins: {
      tailwindcss: {},
      autoprefixer: {},
    },
  },

  runtimeConfig: {
    public: {
      apiBase: 'http://localhost:8080',
    },
  },

  app: {
    head: {
      title: 'GreenRead - 发现好内容',
      meta: [
        { charset: 'utf-8' },
        { name: 'viewport', content: 'width=device-width, initial-scale=1' },
        { name: 'description', content: '一个开放的内容分享社区，发现好内容，记录生活美好' },
      ],
    },
  },

  typescript: {
    strict: true,
    typeCheck: true,
  },

  vite: {
    ssr: {
      noExternal: ['naive-ui', 'vueuc', 'date-fns', '@css-render/vue3-ssr'],
    },
  },

  compatibilityDate: '2024-11-01',
})
