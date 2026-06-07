export default defineNuxtRouteMiddleware(() => {
  const { isAdmin } = useAuthStore()

  if (!isAdmin) {
    throw createError({ statusCode: 403, message: '需要管理员权限' })
  }
})
