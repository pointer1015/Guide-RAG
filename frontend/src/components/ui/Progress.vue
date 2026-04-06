<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  value: number
  max?: number
  size?: 'sm' | 'md' | 'lg'
  showLabel?: boolean
  variant?: 'default' | 'success' | 'warning' | 'error'
}

const props = withDefaults(defineProps<Props>(), {
  value: 0,
  max: 100,
  size: 'md',
  showLabel: false,
  variant: 'default'
})

const percentage = computed(() => 
  Math.min(Math.max((props.value / props.max) * 100, 0), 100)
)

const sizeClass = computed(() => `progress-${props.size}`)
const variantClass = computed(() => `progress-${props.variant}`)
</script>

<template>
  <div class="progress-wrapper">
    <div class="progress" :class="[sizeClass, variantClass]">
      <div 
        class="progress-bar"
        :style="{ width: `${percentage}%` }"
      />
    </div>
    <span v-if="showLabel" class="progress-label">
      {{ Math.round(percentage) }}%
    </span>
  </div>
</template>

<style scoped>
.progress-wrapper {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.progress {
  flex: 1;
  background: var(--bg-tertiary);
  border-radius: 100px;
  overflow: hidden;
}

.progress-bar {
  height: 100%;
  border-radius: 100px;
  transition: width 0.3s ease;
}

/* Sizes */
.progress-sm {
  height: 4px;
}

.progress-md {
  height: 8px;
}

.progress-lg {
  height: 12px;
}

/* Variants */
.progress-default .progress-bar {
  background: linear-gradient(90deg, #7c3aed, #2563eb);
}

.progress-success .progress-bar {
  background: #22c55e;
}

.progress-warning .progress-bar {
  background: #f59e0b;
}

.progress-error .progress-bar {
  background: #ef4444;
}

.progress-label {
  font-size: 0.8rem;
  color: var(--text-secondary);
  min-width: 40px;
  text-align: right;
}
</style>
