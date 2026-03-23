<template>
  <div id="app">
    <BasicLayout />
  </div>
</template>

<script setup lang="ts">
import { healthUsingGet } from '@/api/mainController.ts'
import BasicLayout from '@/layouts/BasicLayout.vue'

const res = healthUsingGet()
console.log(res)
</script>

<style scoped></style>
