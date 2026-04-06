<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  variant?: 'primary' | 'secondary' | 'outline' | 'ghost' | 'danger'
  size?: 'sm' | 'md' | 'lg'
  disabled?: boolean
  loading?: boolean
  block?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  variant: 'primary',
  size: 'md',
  disabled: false,
  loading: false,
  block: false
})

const emit = defineEmits<{
  click: [event: MouseEvent]
}>()

const classes = computed(() => [
  'btn',
  `btn-${props.variant}`,
  `btn-${props.size}`,
  {
    'btn-block': props.block,
    'btn-loading': props.loading,
    'btn-disabled': props.disabled || props.loading
  }
])

function handleClick(event: MouseEvent) {
  if (props.disabled || props.loading) {
    event.preventDefault()
    return
  }
  emit('click', event)
}
</script>

<template>
  <button
    :class="classes"
    :disabled="disabled || loading"
    @click="handleClick"
  >
    <span v-if="loading" class="btn-spinner">
      <svg class="animate-spin" viewBox="0 0 24 24" fill="none">
        <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="3" opacity="0.25"/>
        <path d="M12 2a10 10 0 0 1 10 10" stroke="currentColor" stroke-width="3" stroke-linecap="round"/>
      </svg>
    </span>
    <span class="btn-content">
      <slot />
    </span>
  </button>
</template>

<style scoped>
.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  border-radius: 8px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid transparent;
  outline: none;
}

.btn:focus-visible {
  outline: 2px solid var(--primary, #7c3aed);
  outline-offset: 2px;
}

/* Sizes */
.btn-sm {
  padding: 0.375rem 0.75rem;
  font-size: 0.875rem;
}

.btn-md {
  padding: 0.625rem 1.25rem;
  font-size: 0.95rem;
}

.btn-lg {
  padding: 0.875rem 1.75rem;
  font-size: 1.1rem;
}

/* Variants */
.btn-primary {
  background: var(--brand-gradient);
  color: white;
}

.btn-primary:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 20px rgba(124, 58, 237, 0.4);
}

.btn-secondary {
  background: var(--bg-tertiary);
  color: var(--text-primary);
  border-color: var(--border-primary);
}

.btn-secondary:hover:not(:disabled) {
  background: var(--bg-hover);
  border-color: var(--border-secondary);
}

.btn-outline {
  background: transparent;
  color: var(--text-primary);
  border-color: var(--border-primary);
}

.btn-outline:hover:not(:disabled) {
  background: var(--bg-hover);
  border-color: var(--brand-primary);
}

.btn-ghost {
  background: transparent;
  color: var(--text-secondary);
}

.btn-ghost:hover:not(:disabled) {
  background: var(--bg-hover);
  color: var(--text-primary);
}

.btn-danger {
  background: var(--error);
  color: white;
}

.btn-danger:hover:not(:disabled) {
  background: #b91c1c;
}

/* States */
.btn-block {
  width: 100%;
}

.btn-disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-loading .btn-content {
  opacity: 0.7;
}

/* Spinner */
.btn-spinner {
  display: flex;
  align-items: center;
  justify-content: center;
}

.btn-spinner svg {
  width: 1em;
  height: 1em;
}

.animate-spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
</style>
