<script setup lang="ts">
import { computed } from 'vue'
import { RouterView, useRoute } from 'vue-router'
import MainLayout from '@/layouts/MainLayout.vue'
import AuthLayout from '@/layouts/AuthLayout.vue'

const route = useRoute()

const layout = computed(() => {
  return route.meta.layout as string | undefined
})
</script>

<template>
  <MainLayout v-if="layout === 'main'">
    <RouterView />
  </MainLayout>
  <AuthLayout v-else-if="layout === 'auth'">
    <RouterView />
  </AuthLayout>
  <RouterView v-else />
</template>

<style scoped>
</style>
