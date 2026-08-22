<script setup>
import { useNavStore } from '../stores/nav'

const navStore = useNavStore()

function handleExport(format) {
  navStore.exportData(format)
}
</script>

<template>
  <el-dialog
    v-model="navStore.exportDialog.visible"
    title="选择导出格式"
    width="320px"
    @close="navStore.closeExportDialog"
  >
    <div class="export-options">
      <el-button type="primary" size="large" @click="handleExport('json')">
        <el-icon><Document /></el-icon>
        <span>导出 JSON 格式</span>
      </el-button>
      <el-button type="primary" size="large" @click="handleExport('xml')">
        <el-icon><Files /></el-icon>
        <span>导出 XML 格式</span>
      </el-button>
      <el-button type="primary" size="large" @click="handleExport('html')">
        <el-icon><Collection /></el-icon>
        <span>导出 HTML 书签格式</span>
      </el-button>
    </div>

    <template #footer>
      <el-button @click="navStore.closeExportDialog">取消</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.export-options {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.export-options .el-button {
  justify-content: flex-start;
}
</style>
