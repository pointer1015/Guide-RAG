<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  type?: 'text' | 'password' | 'email'
  placeholder?: string
  modelValue?: string
  disabled?: boolean
  error?: string
  size?: 'sm' | 'md' | 'lg'
}

const props = withDefaults(defineProps<Props>(), {
  type: 'text',
  placeholder: '',
  modelValue: '',
  disabled: false,
  error: '',
  size: 'md'
})

const emit = defineEmits<{
  'update:modelValue': [value: string]
  blur: [event: FocusEvent]
  focus: [event: FocusEvent]
}>()

const classes = computed(() => [
  'input',
  `input-${props.size}`,
  {
    'input-error': !!props.error,
    'input-disabled': props.disabled
  }
])

function handleInput(event: Event) {
  const target = event.target as HTMLInputElement
  emit('update:modelValue', target.value)
}
</script>

<template>
  <div class="input-wrapper">
    <input
      :type="type"
      :class="classes"
      :placeholder="placeholder"
      :value="modelValue"
      :disabled="disabled"
      @input="handleInput"
      @blur="emit('blur', $event)"
      @focus="emit('focus', $event)"
    />
    <span v-if="error" class="input-error-text">{{ error }}</span>
  </div>
</template>

<style scoped>
.input-wrapper {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.input {
  width: 100%;
  background: var(--bg-secondary);
  border: 1px solid var(--border-primary);
  border-radius: 8px;
  color: var(--text-primary);
  outline: none;
  transition: all 0.2s ease;
}

.input::placeholder {
  color: var(--text-tertiary);
}

.input:focus {
  border-color: var(--brand-primary);
  box-shadow: 0 0 0 3px rgba(124, 58, 237, 0.1);
}

/* Sizes */
.input-sm {
  padding: 0.5rem 0.75rem;
  font-size: 0.875rem;
}

.input-md {
  padding: 0.75rem 1rem;
  font-size: 0.95rem;
}

.input-lg {
  padding: 1rem 1.25rem;
  font-size: 1rem;
}

/* States */
.input-error {
  border-color: var(--error);
}

.input-error:focus {
  border-color: var(--error);
  box-shadow: 0 0 0 3px var(--error-bg);
}

.input-disabled {
  opacity: 0.5;
  cursor: not-allowed;
  background: var(--bg-tertiary);
}

.input-error-text {
  font-size: 0.8rem;
  color: var(--error);
}
</style>
