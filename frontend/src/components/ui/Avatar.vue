<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  name?: string
  size?: 'sm' | 'md' | 'lg' | 'xl'
  src?: string
}

const props = withDefaults(defineProps<Props>(), {
  name: '',
  size: 'md',
  src: ''
})

const initials = computed(() => {
  if (!props.name) return '?'
  const names = props.name.trim().split(' ')
  if (names.length === 1) {
    return names[0].charAt(0).toUpperCase()
  }
  return (names[0].charAt(0) + names[names.length - 1].charAt(0)).toUpperCase()
})

const bgColor = computed(() => {
  if (!props.name) return '#7c3aed'
  // 根据名字生成一个稳定的颜色
  const colors = ['#7c3aed', '#2563eb', '#0891b2', '#059669', '#d97706', '#dc2626']
  let hash = 0
  for (let i = 0; i < props.name.length; i++) {
    hash = props.name.charCodeAt(i) + ((hash << 5) - hash)
  }
  return colors[Math.abs(hash) % colors.length]
})

const sizeClass = computed(() => `avatar-${props.size}`)
</script>

<template>
  <div class="avatar" :class="sizeClass">
    <img 
      v-if="src" 
      :src="src" 
      :alt="name"
      class="avatar-img"
    />
    <span 
      v-else 
      class="avatar-initials"
      :style="{ backgroundColor: bgColor }"
    >
      {{ initials }}
    </span>
  </div>
</template>

<style scoped>
.avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
}

.avatar-sm {
  width: 28px;
  height: 28px;
  font-size: 0.7rem;
}

.avatar-md {
  width: 36px;
  height: 36px;
  font-size: 0.85rem;
}

.avatar-lg {
  width: 48px;
  height: 48px;
  font-size: 1rem;
}

.avatar-xl {
  width: 64px;
  height: 64px;
  font-size: 1.25rem;
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-initials {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: 600;
}
</style>
