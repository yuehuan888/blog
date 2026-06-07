export const useUiStore = defineStore('ui', () => {
  const scrollPosition = ref(0)

  function setScrollPosition(pos: number) {
    scrollPosition.value = pos
  }

  return {
    scrollPosition,
    setScrollPosition,
  }
})
