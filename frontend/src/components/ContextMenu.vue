<script setup>
import { computed, onMounted, onUnmounted, ref, watch, nextTick } from 'vue'
import { useNavStore } from '../stores/nav'
import { ElMessageBox } from 'element-plus'

const navStore = useNavStore()
const menuRef = ref(null)

const menu = computed(() => navStore.rightClickMenu)
const nodeId = computed(() => menu.value.nodeId)
const nodeType = computed(() => menu.value.nodeType)

const showAddGroup = computed(() => nodeType.value !== 'link')
const showAddSite = computed(() => nodeType.value !== 'link')
const showRename = computed(() => true)
const showEditSite = computed(() => nodeType.value === 'link')
const showDeleteFolderOnly = computed(() => nodeType.value === 'group')
const showDelete = computed(() => true)

function getNode() {
  const id = nodeId.value
  const data = navStore.bookmarkData
  return navStore.findNodeById(data, id)
}

function runAction(fn) {
  navStore.hideContextMenu()
  setTimeout(fn, 30)
}

function handleAddGroup() {
  const node = getNode()
  if (!node) return
  runAction(() => navStore.openEditDialog('addGroup', node))
}

function handleAddSite() {
  const node = getNode()
  if (!node) return
  runAction(() => navStore.openEditDialog('addSite', node))
}

function handleRename() {
  const node = getNode()
  if (!node) return
  runAction(() => navStore.openEditDialog('rename', node))
}

function handleEditSite() {
  const node = getNode()
  if (!node) return
  runAction(() => navStore.openEditDialog('editSite', node))
}

async function handleDelete() {
  const node = getNode()
  if (!node) return
  navStore.hideContextMenu()
  try {
    await ElMessageBox.confirm(`确定要删除「${node.name}」吗？删除后无法恢复！`, '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    navStore.deleteNodeById(nodeId.value)
  } catch {}
}

async function handleDeleteFolderOnly() {
  const node = getNode()
  if (!node || node.type !== 'group') return
  navStore.hideContextMenu()
  try {
    await ElMessageBox.confirm(`确定要仅删除文件夹「${node.name}」吗？其内容将保留到上级目录！`, '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    navStore.deleteFolderOnlyById(nodeId.value)
  } catch {}
}

let closeTimer = null

function onDocMouseDown(e) {
  if (closeTimer) clearTimeout(closeTimer)
  if (!menu.value.visible) return
  if (menuRef.value && menuRef.value.contains(e.target)) return
  navStore.hideContextMenu()
}

function onDocContextMenu(e) {
  if (!menu.value.visible) return
  // Don't close if right-clicked inside menu
  if (menuRef.value && menuRef.value.contains(e.target)) return
  // Close menu, but let the new context menu from nodes show first
  navStore.hideContextMenu()
}

onMounted(() => {
  document.addEventListener('mousedown', onDocMouseDown)
  document.addEventListener('contextmenu', onDocContextMenu)
  window.addEventListener('blur', () => navStore.hideContextMenu())
})

onUnmounted(() => {
  document.removeEventListener('mousedown', onDocMouseDown)
  document.removeEventListener('contextmenu', onDocContextMenu)
  if (closeTimer) clearTimeout(closeTimer)
})
</script>

<template>
  <Teleport to="body">
    <div
      v-if="menu.visible"
      ref="menuRef"
      class="context-menu"
      :style="{ left: menu.x + 'px', top: menu.y + 'px' }"
      @contextmenu.prevent
      @mousedown.stop
      @click.stop
    >
      <div v-if="showAddGroup" class="menu-item" @click="handleAddGroup">
        <el-icon><FolderAdd /></el-icon>
        <span>新建子分组</span>
      </div>
      <div v-if="showAddSite" class="menu-item" @click="handleAddSite">
        <el-icon><Link /></el-icon>
        <span>新建网站</span>
      </div>
      <template v-if="showAddGroup && showAddSite">
        <div class="menu-divider"></div>
      </template>
      <div v-if="showRename" class="menu-item" @click="handleRename">
        <el-icon><Edit /></el-icon>
        <span>重命名</span>
      </div>
      <div v-if="showEditSite" class="menu-item" @click="handleEditSite">
        <el-icon><EditPen /></el-icon>
        <span>编辑网站</span>
      </div>
      <template v-if="(showRename || showEditSite) && (showDeleteFolderOnly || showDelete)">
        <div class="menu-divider"></div>
      </template>
      <div v-if="showDeleteFolderOnly" class="menu-item danger" @click="handleDeleteFolderOnly">
        <el-icon><FolderDelete /></el-icon>
        <span>仅删除文件夹</span>
      </div>
      <div v-if="showDelete" class="menu-item danger" @click="handleDelete">
        <el-icon><Delete /></el-icon>
        <span>删除</span>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.context-menu {
  position: fixed;
  background-color: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  box-shadow: 0 6px 24px rgba(0, 0, 0, 0.15);
  padding: 4px 0;
  min-width: 160px;
  z-index: 2147483647;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px;
  font-size: 13px;
  color: #2d3748;
  cursor: pointer;
  transition: background-color 0.12s;
}

.menu-item:hover {
  background-color: #ebf4ff;
  color: #2b6cb0;
}

.menu-item:active {
  background-color: #e2e8f0;
}

.menu-item.danger {
  color: #f56565;
}

.menu-item.danger:hover {
  background-color: #fff5f5;
  color: #e53e3e;
}

.menu-divider {
  height: 1px;
  background-color: #edf2f7;
  margin: 4px 0;
}
</style>
