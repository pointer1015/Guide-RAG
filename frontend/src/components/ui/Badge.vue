<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  variant?: 'default' | 'success' | 'warning' | 'error' | 'info'
  size?: 'sm' | 'md'
  dot?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  variant: 'default',
  size: 'md',
  dot: false
})

const classes = computed(() => [
  'badge',
  `badge-${props.variant}`,
  `badge-${props.size}`,
  { 'badge-dot': props.dot }
])
</script>

<template>
  <span :class="classes">
    <span v-if="dot" class="badge-dot-indicator" />
    <slot />
  </span>
</template>

<style scoped>
.badge {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  border-radius: 100px;
  font-weight: 500;
}

/* Sizes */
.badge-sm {
  padding: 0.125rem 0.5rem;
  font-size: 0.7rem;
}

.badge-md {
  padding: 0.25rem 0.75rem;
  font-size: 0.8rem;
}

/* Variants */
.badge-default {
  background: var(--bg-tertiary);
  color: var(--text-secondary);
}

.badge-success {
  background: rgba(34, 197, 94, 0.1);
  color: #22c55e;
}

.badge-warning {
  background: rgba(245, 158, 11, 0.1);
  color: #f59e0b;
}

.badge-error {
  background: rgba(239, 68, 68, 0.1);
  color: #ef4444;
}

.badge-info {
  background: rgba(59, 130, 246, 0.1);
  color: #3b82f6;
}

/* Dot */
.badge-dot-indicator {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}
</style>
