<template>
  <footer class="border-t border-gray-100 bg-white mt-12">
    <div class="max-w-6xl mx-auto px-4 py-8">
      <div class="flex flex-col md:flex-row items-center justify-between gap-4">
        <div class="flex items-center gap-2 text-primary font-bold">
          <span>🌿</span>
          <span>GreenRead</span>
        </div>
        <p class="text-xs text-text-secondary">
          © {{ new Date().getFullYear() }} GreenRead. 记录生活，发现美好。
        </p>
        <div class="flex gap-4 text-xs text-text-secondary">
          <NuxtLink to="/" class="hover:text-primary">首页</NuxtLink>
          <NuxtLink to="/tag" class="hover:text-primary">标签</NuxtLink>
          <NuxtLink to="/article/hot" class="hover:text-primary">热门</NuxtLink>
        </div>
      </div>
    </div>
  </footer>
</template>
