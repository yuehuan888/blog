import { useAuthStore } from '~/stores/auth'

/**
 * Client-only plugin: restores auth state from sessionStorage after SSR hydration.
 * SSR serializes Pinia state (token=null from server) into __NUXT__,
 * which overwrites localStorage during hydration. This runs after hydration
 * to reload the real auth state.
 */
export default defineNuxtPlugin(() => {
  const authStore = useAuthStore()
  authStore.restoreFromStorage()
})
