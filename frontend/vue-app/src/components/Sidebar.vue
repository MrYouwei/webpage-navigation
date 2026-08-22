<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useNavStore } from '../stores/nav'
import NavTreeNode from './NavTreeNode.vue'

const navStore = useNavStore()
const fileInputRef = ref(null)

const isNavModule = computed(() => navStore.activeModule === '导航')
const showManagementTools = computed(() => navStore.isLoggedIn && navStore.activeModule === '我的')
const showBatchActions = computed(() => showManagementTools.value && navStore.checkedLinkIds.length > 0)

// ========= 公共导航分类二级树（导航模块专用，左侧菜单） =========
const publicCategoryTree = [
  {
    id: 'cat-ai',
    type: 'group',
    name: 'AI',
    icon: '✨',
    badge: 'AI+',
    sectionId: 'ai-root',
    expanded: true,
    children: [
      { id: 'cat-ai-general',  type: 'group', name: '通用',  icon: '🧑', sectionId: 'ai-general',  children: [], expanded: false },
      { id: 'cat-ai-search',   type: 'group', name: '搜索',  icon: '🔎', sectionId: 'ai-search',   children: [], expanded: false },
      { id: 'cat-ai-image',    type: 'group', name: '图像',  icon: '🖼️', sectionId: 'ai-image',    children: [], expanded: false },
      { id: 'cat-ai-video',    type: 'group', name: '视频',  icon: '▶️', sectionId: 'ai-video',    children: [], expanded: false },
      { id: 'cat-ai-music',    type: 'group', name: '音乐',  icon: '🎵', sectionId: 'ai-music',    children: [], expanded: false },
      { id: 'cat-ai-note',     type: 'group', name: '笔记',  icon: '📒', sectionId: 'ai-note',     children: [], expanded: false },
      { id: 'cat-ai-ppt',      type: 'group', name: 'PPT',   icon: '📊', sectionId: 'ai-ppt',      children: [], expanded: false },
      { id: 'cat-ai-tools',    type: 'group', name: '工具',  icon: '🔧', sectionId: 'ai-tools',    children: [], expanded: false },
      { id: 'cat-ai-code',     type: 'group', name: '编程',  icon: '💻', sectionId: 'ai-code',     children: [], expanded: false },
      { id: 'cat-ai-aggregate',type: 'group', name: '聚合',  icon: '📦', sectionId: 'ai-aggregate',children: [], expanded: false }
    ]
  },
  {
    id: 'cat-life',
    type: 'group',
    name: '生活',
    icon: '🛍️',
    sectionId: 'life',
    expanded: false,
    children: []
  },
  {
    id: 'cat-movie',
    type: 'group',
    name: '影视',
    icon: '📺',
    sectionId: 'movie',
    expanded: false,
    children: []
  },
  {
    id: 'cat-search',
    type: 'group',
    name: '搜索',
    icon: '🔍',
    sectionId: 'search',
    expanded: false,
    children: []
  }
]

// ========= 侧栏树数据源 =========
const sidebarTreeData = computed(() => {
  // 导航模块：始终使用"公共分类二级树"，与登录状态、用户书签无关
  if (isNavModule.value) return publicCategoryTree
  // 我的模块：用户书签树
  if (navStore.isLoggedIn && navStore.bookmarkData.length > 0) return navStore.bookmarkData
  return []
})

// ========= 只读模式（导航模块使用，且二级叶子点击→滚动到对应分块） =========
const treeReadonly = computed(() => isNavModule.value)

// 侧栏树节点点击 → 设置滚动锚点
function handleTreeReadonlyClick(node) {
  if (!isNavModule.value) return
  const sid = node?.sectionId || node?.id
  if (sid) {
    navStore.navScrollAnchor = sid
    // 触发一次后立即清空，下次再点击同节点也能再次触发
    setTimeout(() => { navStore.navScrollAnchor = '' }, 200)
  }
}

function handleAddRootGroup() { navStore.openEditDialog('addRootGroup') }
function handleExpandAll() { navStore.expandAll() }
function handleCollapseAll() { navStore.collapseAll() }
function handleImportClick() { fileInputRef.value?.click() }
function handleFileChange(e) {
  const file = e.target.files?.[0]
  if (file) navStore.importData(file)
  e.target.value = ''
}
function handleExportClick() { navStore.openExportDialog() }
function handleBatchDelete() { navStore.batchDeleteCheckedLinks() }
function handleBatchCancel() { navStore.clearCheckedLinks() }
function handleToggleSidebar() { navStore.toggleSidebar() }

async function handleDragEnd(e) {
  const { draggedId, targetId, position } = e.detail || {}
  if (draggedId && targetId && position) {
    navStore.moveNode(draggedId, targetId, position)
  }
}

onMounted(() => window.addEventListener('drag-end', handleDragEnd))
onUnmounted(() => window.removeEventListener('drag-end', handleDragEnd))
</script>

<template>
  <div class="sidebar" :class="{ collapsed: navStore.sidebarCollapsed }">
    <!-- Header (只在未折叠时显示) -->
    <div class="sidebar-header" v-show="!navStore.sidebarCollapsed">
      <!-- ========= 导航模块：顶部只显示一个简洁标题，不显示用户信息/操作按钮 ========= -->
      <template v-if="isNavModule">
        <div class="nav-module-header">
          <el-icon :size="20" color="#63b3ed"><Guide /></el-icon>
          <span class="nav-module-title">公共导航</span>
        </div>
      </template>

      <!-- ========= 我的模块：保持原有用户信息 + 管理操作 ========= -->
      <template v-else>
        <div class="auth-panel">
          <template v-if="!navStore.isLoggedIn">
            <div class="guest-hint">
              <el-icon :size="32" color="#718096"><User /></el-icon>
              <p>登录后管理你的导航</p>
              <el-button type="primary" size="small" @click="navStore.openAuthDialog('login')">
                <el-icon><Right /></el-icon>
                <span>登录</span>
              </el-button>
            </div>
          </template>
          <template v-else>
            <div class="user-info">
              <el-icon :size="18"><User /></el-icon>
              <span class="username">{{ navStore.currentUser?.username }}</span>
            </div>
          </template>
        </div>

        <!-- 新建分组 (居中薄按钮) -->
        <div v-if="showManagementTools" class="add-group-btn">
          <el-tooltip content="新建分组" placement="bottom" :show-after="200">
            <el-button size="small" round @click="handleAddRootGroup">
              <el-icon><Plus /></el-icon>
              <span>新建分组</span>
            </el-button>
          </el-tooltip>
        </div>

        <!-- 主操作按钮：展开 / 折叠 / 导入 / 导出 / 清空 -->
        <div v-if="showManagementTools" class="main-actions">
          <el-tooltip content="展开全部" placement="bottom" :show-after="200">
            <el-button class="tool-btn" size="small" circle @click="handleExpandAll">
              <el-icon><Expand /></el-icon>
            </el-button>
          </el-tooltip>
          <el-tooltip content="折叠全部" placement="bottom" :show-after="200">
            <el-button class="tool-btn" size="small" circle @click="handleCollapseAll">
              <el-icon><Fold /></el-icon>
            </el-button>
          </el-tooltip>
          <el-tooltip content="导入" placement="bottom" :show-after="200">
            <el-button class="tool-btn" size="small" circle @click="handleImportClick">
              <el-icon><Upload /></el-icon>
            </el-button>
          </el-tooltip>
          <el-tooltip content="导出" placement="bottom" :show-after="200">
            <el-button class="tool-btn" size="small" circle @click="handleExportClick">
              <el-icon><Download /></el-icon>
            </el-button>
          </el-tooltip>
          <el-tooltip content="清空数据" placement="bottom" :show-after="200">
            <el-button class="tool-btn" size="small" circle @click="navStore.clearAll()">
              <el-icon><RefreshRight /></el-icon>
            </el-button>
          </el-tooltip>
        </div>

        <!-- 批量操作 -->
        <div v-if="showBatchActions" class="batch-actions">
          <span class="batch-count">已选 {{ navStore.selectedLinks.length }} 个</span>
          <div class="batch-buttons">
            <el-button type="danger" size="small" @click="handleBatchDelete">
              <el-icon><Delete /></el-icon>
              <span>删除</span>
            </el-button>
            <el-button size="small" @click="handleBatchCancel">取消</el-button>
          </div>
        </div>
      </template>
    </div>

    <!-- 树容器 -->
    <div class="tree-container" v-show="!navStore.sidebarCollapsed">
      <template v-if="isNavModule">
        <NavTreeNode
          v-for="node in sidebarTreeData"
          :key="node.id"
          :node="node"
          :depth="0"
          readonly
          @node-click="handleTreeReadonlyClick"
        />
      </template>
      <template v-else-if="navStore.isLoggedIn && navStore.bookmarkData.length > 0">
        <NavTreeNode
          v-for="node in sidebarTreeData"
          :key="node.id"
          :node="node"
          :depth="0"
        />
      </template>
      <div v-else-if="navStore.isLoggedIn" class="tree-empty">
        <el-empty description="暂无分组，点击上方'新建分组'开始" />
      </div>
      <div v-else class="tree-empty">
        <el-empty description="登录后管理书签" />
      </div>
    </div>

    <!-- 侧边栏折叠/展开按钮 (底部居中边界位置) -->
    <button
      class="sidebar-toggle"
      @click="handleToggleSidebar"
      :title="navStore.sidebarCollapsed ? '展开侧栏' : '收起侧栏'"
    >
      <el-icon :size="14">
        <DArrowRight v-if="navStore.sidebarCollapsed" />
        <DArrowLeft v-else />
      </el-icon>
    </button>

    <!-- 隐藏的文件选择 -->
    <input
      ref="fileInputRef"
      type="file"
      accept=".json,.xml,.html"
      style="display: none"
      @change="handleFileChange"
    />
  </div>
</template>

<style scoped>
.sidebar {
  width: 260px;
  height: 100%;
  background-color: #2d3748;
  color: #e2e8f0;
  display: flex;
  flex-direction: column;
  position: relative;
  transition: width 0.2s ease;
  flex-shrink: 0;
}

.sidebar.collapsed {
  width: 48px;
}

/* 侧栏切换按钮 */
.sidebar-toggle {
  position: absolute;
  top: 50%;
  right: -12px;
  z-index: 10;
  width: 24px;
  height: 48px;
  background-color: #4a5568;
  border: none;
  color: #e2e8f0;
  border-radius: 12px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transform: translateY(-50%);
  transition: background-color 0.2s ease, transform 0.2s ease, box-shadow 0.2s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
}
.sidebar-toggle:hover {
  background-color: #718096;
  transform: translateY(-50%) scale(1.08);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.4);
}

.sidebar-header {
  padding: 12px;
  border-bottom: 1px solid #4a5568;
  flex-shrink: 0;
}

/* ====== 导航模块顶部简洁标题 ====== */
.nav-module-header {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 10px 12px;
  background: linear-gradient(135deg, rgba(99, 179, 237, 0.12), rgba(90, 103, 216, 0.08));
  border: 1px solid rgba(99, 179, 237, 0.25);
  border-radius: 8px;
}
.nav-module-title {
  font-size: 15px;
  font-weight: 600;
  color: #e2e8f0;
  letter-spacing: 1px;
}

/* ====== 我的模块原有样式 ====== */
.auth-panel {
  margin-bottom: 10px;
}

.guest-hint {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 12px 8px;
}
.guest-hint p {
  font-size: 12px;
  color: #a0aec0;
  margin: 0;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  background-color: #4a5568;
  border-radius: 6px;
  font-size: 13px;
  color: #e2e8f0;
}
.username {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 新建分组按钮 */
.add-group-btn {
  display: flex;
  justify-content: center;
  margin-bottom: 10px;
}
.add-group-btn .el-button {
  background-color: transparent !important;
  border: 1px solid rgba(255, 255, 255, 0.15) !important;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2) !important;
  color: #e2e8f0 !important;
  border-radius: 16px !important;
  padding: 3px 18px !important;
  height: 26px !important;
}
.add-group-btn .el-button:hover {
  background-color: rgba(66, 153, 225, 0.15) !important;
  border-color: rgba(66, 153, 225, 0.4) !important;
  color: #63b3ed !important;
}

/* 操作按钮 */
.main-actions {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
}
.main-actions .tool-btn {
  margin-left: 0 !important;
  width: 30px;
  height: 30px;
  padding: 0;
  background-color: transparent !important;
  border: 1px solid rgba(255, 255, 255, 0.15) !important;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2) !important;
  color: #a0aec0 !important;
}
.main-actions .tool-btn:hover {
  background-color: rgba(255, 255, 255, 0.1) !important;
  border-color: rgba(255, 255, 255, 0.3) !important;
  color: #ffffff !important;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3) !important;
}

.batch-actions {
  margin-top: 10px;
  padding: 6px 8px;
  background-color: #4a5568;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.batch-count {
  font-size: 11px;
  color: #fbd38d;
}
.batch-buttons { display: flex; gap: 4px; }

/* 树容器 */
.tree-container {
  flex: 1;
  overflow-y: auto;
  padding: 6px 0;
}
.tree-container::-webkit-scrollbar { width: 6px; }
.tree-container::-webkit-scrollbar-track { background: #2d3748; }
.tree-container::-webkit-scrollbar-thumb { background: #4a5568; border-radius: 3px; }
.tree-container::-webkit-scrollbar-thumb:hover { background: #718096; }

.tree-empty { padding: 40px 20px; text-align: center; }
.tree-empty :deep(.el-empty__description p) { color: #718096; font-size: 13px; }
</style>
