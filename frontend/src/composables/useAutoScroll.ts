import { ref, onMounted, onUnmounted, type Ref } from 'vue'

interface UseAutoScrollOptions {
  threshold?: number // 距离底部多少像素内算"在底部"
  smoothScroll?: boolean
}

export function useAutoScroll(
  containerRef: Ref<HTMLElement | null>,
  options: UseAutoScrollOptions = {}
) {
  const { threshold = 100, smoothScroll = true } = options
  
  const isAtBottom = ref(true)
  const userScrolled = ref(false)

  /**
   * 检查是否在底部
   */
  function checkIsAtBottom() {
    const container = containerRef.value
    if (!container) return true
    
    const { scrollTop, scrollHeight, clientHeight } = container
    return scrollHeight - scrollTop - clientHeight <= threshold
  }

  /**
   * 滚动到底部
   */
  function scrollToBottom(force = false) {
    const container = containerRef.value
    if (!container) return

    // 如果用户手动滚动了且没有强制，则不自动滚动
    if (userScrolled.value && !force) return

    if (smoothScroll) {
      container.scrollTo({
        top: container.scrollHeight,
        behavior: 'smooth'
      })
    } else {
      container.scrollTop = container.scrollHeight
    }
  }

  /**
   * 处理滚动事件
   */
  function handleScroll() {
    const wasAtBottom = isAtBottom.value
    isAtBottom.value = checkIsAtBottom()
    
    // 如果从底部滚动离开，标记为用户手动滚动
    if (wasAtBottom && !isAtBottom.value) {
      userScrolled.value = true
    }
    
    // 如果滚动回底部，重置标记
    if (isAtBottom.value) {
      userScrolled.value = false
    }
  }

  /**
   * 重置滚动状态
   */
  function resetScroll() {
    userScrolled.value = false
    isAtBottom.value = true
  }

  // 监听容器变化
  let observer: MutationObserver | null = null

  onMounted(() => {
    const container = containerRef.value
    if (!container) return

    container.addEventListener('scroll', handleScroll, { passive: true })

    // 使用 MutationObserver 监听内容变化
    observer = new MutationObserver(() => {
      if (!userScrolled.value) {
        requestAnimationFrame(() => {
          scrollToBottom()
        })
      }
    })

    observer.observe(container, {
      childList: true,
      subtree: true,
      characterData: true
    })
  })

  onUnmounted(() => {
    const container = containerRef.value
    if (container) {
      container.removeEventListener('scroll', handleScroll)
    }
    if (observer) {
      observer.disconnect()
    }
  })

  return {
    isAtBottom,
    userScrolled,
    scrollToBottom,
    resetScroll
  }
}
