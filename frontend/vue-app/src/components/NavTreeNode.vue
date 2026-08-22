<script setup>
import { ref, computed } from 'vue'
import { useNavStore } from '../stores/nav'
import { ElMessageBox } from 'element-plus'

const props = defineProps({
  node: { type: Object, required: true },
  depth: { type: Number, default: 0 },
  readonly: { type: Boolean, default: false }
})

const emit = defineEmits(['node-click', 'node-context-menu', 'node-drag-start', 'node-drag-end'])

const navStore = useNavStore()

const isDragging = ref(false)
const dropPosition = ref('')
const dragOver = ref(false)

const isGroup = computed(() => props.node.type === 'group')
const isExpanded = computed(() => props.node.expanded)
const isSelected = computed(() => navStore.currentSelectNode?.id === props.node.id)
const isChecked = computed(() => navStore.checkedLinkIds.includes(props.node.id))

// 分组复选框状态：基于分组下所有链接（递归）的选中情况
const groupLinkIds = computed(() => {
  if (!isGroup.value) return []
  return navStore.collectLinkIdsInGroup(props.node)
})

const groupCheckedCount = computed(() => {
  return groupLinkIds.value.filter(id => navStore.checkedLinkIds.includes(id)).length
})

const isGroupChecked = computed(() => {
  const total = groupLinkIds.value.length
  return total > 0 && groupCheckedCount.value === total
})

const isGroupIndeterminate = computed(() => {
  const total = groupLinkIds.value.length
  const checked = groupCheckedCount.value
  return checked > 0 && checked < total
})

const toggleExpand = (e) => {
  e.stopPropagation()
  navStore.toggleGroupExpand(props.node)
}

const handleNodeClick = () => {
  navStore.selectNode(props.node)
  // readonly 模式（导航模块左侧菜单）→ 向父组件派发节点，用于内容区滚动
  emit('node-click', props.node)
}

const handleContextMenu = (e) => {
  e.preventDefault()
  if (props.readonly) return
  navStore.selectNode(props.node)
  navStore.showContextMenu(e.clientX, e.clientY, props.node.type, props.node.id)
}

const handleCheckboxChange = () => {
  navStore.toggleLinkChecked(props.node.id)
}

const handleCheckboxClick = (e) => {
  e.stopPropagation()
}

const handleGroupCheckboxChange = () => {
  navStore.toggleGroupChecked(props.node)
}

const handleGroupCheckboxClick = (e) => {
  e.stopPropagation()
}

// Inline fallback action handlers (right-click alternative)
const onInlineAddGroup = () => {
  navStore.openEditDialog('addGroup', props.node)
}
const onInlineAddSite = () => {
  navStore.openEditDialog('addSite', props.node)
}
const onInlineEdit = () => {
  if (isGroup.value) {
    navStore.openEditDialog('rename', props.node)
  } else {
    navStore.openEditDialog('editSite', props.node)
  }
}
const onInlineDelete = async () => {
  try {
    await ElMessageBox.confirm(`确定要删除「${props.node.name}」吗？删除后无法恢复！`, '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    navStore.deleteNodeById(props.node.id)
  } catch {}
}

const handleDragStart = (e) => {
  if (props.readonly) return
  if (!navStore.isLoggedIn) return
  isDragging.value = true
  e.dataTransfer.effectAllowed = 'move'
  e.dataTransfer.setData('text/plain', props.node.id)
  window.dispatchEvent(new CustomEvent('drag-start', {
    detail: { nodeId: props.node.id }
  }))
}

const handleDragEnd = (e) => {
  isDragging.value = false
  dropPosition.value = ''
  dragOver.value = false
  window.dispatchEvent(new CustomEvent('drag-end', { detail: {} }))
}

const handleDragOver = (e) => {
  if (props.readonly) return
  if (!navStore.isLoggedIn) return
  e.preventDefault()
  e.dataTransfer.dropEffect = 'move'
  if (isGroup.value) {
    dragOver.value = true
    const rect = e.currentTarget.getBoundingClientRect()
    const y = e.clientY - rect.top
    const h = rect.height
    if (y < h * 0.25) {
      dropPosition.value = 'before'
    } else if (y > h * 0.75) {
      dropPosition.value = 'after'
    } else {
      dropPosition.value = 'inside'
    }
  }
}

const handleDragLeave = (e) => {
  dragOver.value = false
  dropPosition.value = ''
}

const handleDrop = (e) => {
  e.preventDefault()
  e.stopPropagation()
  if (props.readonly) return
  if (!isGroup.value) return
  const draggedId = e.dataTransfer.getData('text/plain')
  const position = dropPosition.value || 'inside'
  dragOver.value = false
  dropPosition.value = ''
  if (draggedId && position) {
    navStore.moveNode(draggedId, props.node.id, position)
  }
}
</script>

<template>
  <div
    class="tree-node"
    :data-id="node.id"
    :data-type="node.type"
    :class="{
      'dragging': isDragging,
      'drop-inside': dropPosition === 'inside',
      'drop-before': dropPosition === 'before',
      'drop-after': dropPosition === 'after'
    }"
  >
    <div
      class="tree-node-header"
      :class="{
        active: isSelected,
        selected: isChecked,
        'readonly-root': readonly && depth === 0
      }"
      @click="handleNodeClick"
      @contextmenu="handleContextMenu"
      :draggable="!readonly"
      @dragstart="handleDragStart"
      @dragend="handleDragEnd"
      @dragover="handleDragOver"
      @dragleave="handleDragLeave"
      @drop="handleDrop"
    >
      <span v-if="isGroup" class="tree-toggle" :class="{ open: isExpanded }" @click="toggleExpand">
        <el-icon :size="12">
          <ArrowRight />
        </el-icon>
      </span>
      <span v-else class="tree-toggle-placeholder"></span>

      <!-- 分组节点复选框：支持半选/全选，批量选择组内所有链接 -->
      <el-checkbox
        v-if="isGroup && !readonly"
        :model-value="isGroupChecked"
        :indeterminate="isGroupIndeterminate"
        @change="handleGroupCheckboxChange"
        @click="handleGroupCheckboxClick"
        @contextmenu.stop.prevent
        class="tree-checkbox tree-checkbox-group"
      />
      <!-- 链接节点复选框：单个选中 -->
      <el-checkbox
        v-else-if="!readonly"
        :model-value="isChecked"
        @change="handleCheckboxChange"
        @click="handleCheckboxClick"
        @contextmenu.stop.prevent
        class="tree-checkbox"
      />

      <span class="tree-icon" :class="{ 'tree-icon-custom': node.icon }">
        <template v-if="node.icon">
          <span class="tree-icon-emoji">{{ node.icon }}</span>
        </template>
        <template v-else>
          <el-icon v-if="isGroup"><Folder /></el-icon>
          <el-icon v-else><Link /></el-icon>
        </template>
      </span>

      <span class="tree-name" :title="node.name">{{ node.name }}</span>

      <span v-if="node.badge" class="tree-badge">{{ node.badge }}</span>

      <!-- Inline fallback action buttons (appear on hover) -->
      <span v-if="!readonly" class="tree-actions">
        <template v-if="isGroup">
          <button class="act-btn" title="新建子分组" @click.stop="onInlineAddGroup">
            <el-icon :size="14"><FolderAdd /></el-icon>
          </button>
          <button class="act-btn" title="新建网站" @click.stop="onInlineAddSite">
            <el-icon :size="14"><Plus /></el-icon>
          </button>
        </template>
        <button class="act-btn" :title="isGroup ? '重命名' : '编辑网站'" @click.stop="onInlineEdit">
          <el-icon :size="14"><Edit /></el-icon>
        </button>
        <button class="act-btn danger" title="删除" @click.stop="onInlineDelete">
          <el-icon :size="14"><Delete /></el-icon>
        </button>
      </span>
    </div>

    <div v-if="isGroup && isExpanded" class="tree-children">
      <NavTreeNode
        v-for="child in node.children"
        :key="child.id"
        :node="child"
        :depth="depth + 1"
        :readonly="readonly"
        @node-click="(n) => emit('node-click', n)"
        @node-context-menu="(n, x, y) => emit('node-context-menu', n, x, y)"
      />
    </div>
  </div>
</template>

<style scoped>
.tree-node {
  position: relative;
}

.tree-node-header {
  display: flex;
  align-items: center;
  padding: 4px 8px;
  gap: 6px;
  cursor: pointer;
  user-select: none;
  transition: background-color 0.15s;
  border-radius: 4px;
}

.tree-node-header:hover {
  background-color: #4a5568;
}

.tree-node-header.active {
  background-color: #4299e1;
}

.tree-node-header.selected {
  background-color: rgba(66, 153, 225, 0.3);
}

.tree-toggle {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  transition: transform 0.2s;
  flex-shrink: 0;
}

.tree-toggle.open {
  transform: rotate(90deg);
}

.tree-toggle-placeholder {
  display: inline-block;
  width: 16px;
  height: 16px;
  flex-shrink: 0;
}

.tree-checkbox {
  margin-right: 0 !important;
  margin-left: 0;
  flex-shrink: 0;
  line-height: 1;
}

.tree-checkbox :deep(.el-checkbox__inner) {
  width: 14px;
  height: 14px;
  border-radius: 2px;
}

.tree-checkbox :deep(.el-checkbox__input) {
  line-height: 1;
}

/* 分组复选框：默认更隐蔽，hover 或选中/半选时高亮 */
.tree-checkbox-group {
  opacity: 0.55;
  transition: opacity 0.15s;
}

.tree-node-header:hover .tree-checkbox-group,
.tree-checkbox-group.is-checked,
.tree-checkbox-group.is-indeterminate {
  opacity: 1;
}

.tree-icon {
  display: inline-flex;
  align-items: center;
  color: #a0aec0;
  flex-shrink: 0;
  margin-right: 2px;
  width: 18px;
  height: 18px;
  justify-content: center;
}
.tree-icon-custom {
  width: auto;
  min-width: 20px;
}
.tree-icon-emoji {
  font-size: 15px;
  line-height: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.tree-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  color: #e2e8f0;
}

/* readonly 模式下一级根节点：加粗、略大 */
.tree-node-header.readonly-root .tree-name {
  font-weight: 700;
  font-size: 14px;
  letter-spacing: 0.4px;
}

/* 节点徽章（橙色 AI+ 角标） */
.tree-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 18px;
  padding: 0 6px;
  margin-left: 4px;
  font-size: 11px;
  font-weight: 700;
  color: #ffffff;
  background: linear-gradient(135deg, #f97316, #ea580c);
  border-radius: 4px;
  letter-spacing: 0.3px;
  flex-shrink: 0;
  box-shadow: 0 1px 3px rgba(249, 115, 22, 0.4);
}

/* Inline action buttons – appear on hover */
.tree-actions {
  display: none;
  align-items: center;
  gap: 2px;
  margin-left: auto;
  flex-shrink: 0;
}

.tree-node-header:hover .tree-actions,
.tree-node-header.active .tree-actions {
  display: inline-flex;
}

.act-btn {
  width: 22px;
  height: 22px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  color: #a0aec0;
  border: 1px solid transparent;
  border-radius: 3px;
  cursor: pointer;
  padding: 0;
  line-height: 1;
}

.act-btn:hover {
  background-color: rgba(255, 255, 255, 0.15);
  color: #ffffff;
  border-color: rgba(255, 255, 255, 0.2);
}

.act-btn.danger:hover {
  background-color: rgba(245, 101, 101, 0.25);
  color: #feb2b2;
  border-color: rgba(245, 101, 101, 0.4);
}

.tree-children {
  padding-left: 20px;
}

.dragging {
  opacity: 0.4;
}

.drop-inside {
  background-color: rgba(66, 153, 225, 0.3);
  outline: 2px dashed #4299e1;
  outline-offset: -2px;
}

.drop-before::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background-color: #4299e1;
}

.drop-after::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 3px;
  background-color: #4299e1;
}
</style>
