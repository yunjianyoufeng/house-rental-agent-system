import { computed, ref, watch } from 'vue'

export function usePagination(listRef, defaultPageSize = 5) {
  const currentPage = ref(1)
  const pageSize = ref(defaultPageSize)

  const total = computed(() => listRef.value?.length || 0)
  const pagedList = computed(() => {
    const source = listRef.value || []
    const start = (currentPage.value - 1) * pageSize.value
    return source.slice(start, start + pageSize.value)
  })

  watch(total, (value) => {
    const maxPage = Math.max(1, Math.ceil(value / pageSize.value))
    if (currentPage.value > maxPage) {
      currentPage.value = maxPage
    }
  })

  watch(pageSize, () => {
    currentPage.value = 1
  })

  return {
    currentPage,
    pageSize,
    total,
    pagedList,
  }
}
