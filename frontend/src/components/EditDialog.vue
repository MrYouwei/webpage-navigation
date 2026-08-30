<script setup>
import { ref, watch, computed } from 'vue'
import { useNavStore } from '../stores/nav'
import { api } from '../api/index'

const navStore = useNavStore()

const form = ref({ name: '', url: '', folderId: '' })
const isFetchingTitle = ref(false)
let titleFetchTimer = null
let lastFetchedUrl = ''
let confirming = false

const dialogTitle = computed(() => {
  const { type } = navStore.editDialog
  const titles = {
    addRootGroup: '新建根分组',
    addGroup: '新建子分组',
    addSite: '新建网站',
    quickAddSite: '快速新增网站',
    rename: '重命名分组',
    editSite: '编辑网站'
  }
  return titles[type] || '编辑'
})

const showUrl = computed(() => {
  return ['addSite', 'quickAddSite', 'editSite'].includes(navStore.editDialog.type)
})

const showFolderPath = computed(() => ['quickAddSite', 'editSite'].includes(navStore.editDialog.type))
const isAddSiteDialog = computed(() => ['addSite', 'quickAddSite'].includes(navStore.editDialog.type))
const dialogWidth = computed(() => showFolderPath.value ? '520px' : '400px')
const formLabelWidth = computed(() => showFolderPath.value ? '96px' : '80px')

// 新建网站类型下名称可不填（可从网址自动解析）
const isNameOptional = computed(() => isAddSiteDialog.value)
const isNameRequired = computed(() => !isNameOptional.value)

const folderTreeData = computed(() => buildGroupTreeOptions(navStore.bookmarkData))

function buildGroupTreeOptions(list, path = []) {
  if (!Array.isArray(list)) return []
  return list
    .filter(node => node.type === 'group')
    .map(node => {
      const label = node.name || '未命名分组'
      const fullPath = path.concat(label).join(' / ')
      const option = {
        id: node.id,
        label,
        fullPath,
        searchText: `${label} ${fullPath}`.toLowerCase()
      }
      const children = buildGroupTreeOptions(node.children || [], path.concat(label))
      if (children.length > 0) {
        option.children = children
      }
      return option
    })
}

function filterFolderNode(keyword, data) {
  const value = String(keyword || '').trim().toLowerCase()
  if (!value) return true
  return (data.searchText || '').includes(value)
}

// 从网址中提取名称：取主域名首段并首字母大写（兜底方案）
function extractNameFromUrl(url) {
  if (!url) return ''
  let urlStr = String(url).trim()
  if (!urlStr) return ''
  if (!/^https?:\/\//i.test(urlStr)) {
    urlStr = 'http://' + urlStr
  }
  try {
    const parsed = new URL(urlStr)
    let host = (parsed.hostname || '').toLowerCase()
    host = host.replace(/^www\./, '')
    if (!host) return ''
    const parts = host.split('.')
    const domain = parts[0] || host
    if (!domain) return ''
    return domain.charAt(0).toUpperCase() + domain.slice(1)
  } catch {
    return ''
  }
}

// 通过后端接口获取网页标题
async function fetchWebsiteTitle(url) {
  let urlStr = String(url).trim()
  if (!urlStr) return ''
  if (!/^https?:\/\//i.test(urlStr)) {
    urlStr = 'http://' + urlStr
  }

  try {
    const result = await api.getWebsiteTitle(urlStr)
    if (result.code >= 200 && result.code < 300 && result.data?.title) {
      return result.data.title
    }
  } catch {
    // 后端接口失败，回退到域名提取
  }

  // 回退：从域名提取
  return extractNameFromUrl(urlStr)
}

watch(() => navStore.editDialog.visible, (visible) => {
  if (visible) {
    const { type, node } = navStore.editDialog
    // 新建操作：名称和网址都留空，不预填父节点信息
    const isAdd = ['addRootGroup', 'addGroup', 'addSite', 'quickAddSite'].includes(type)
    form.value = {
      name: isAdd ? '' : (node?.name || ''),
      url: isAdd ? '' : (node?.url || ''),
      folderId: getInitialFolderId(type, node)
    }
    lastFetchedUrl = ''
    confirming = false
  }
})

// 监听网址输入：防抖获取网站标题
watch(() => form.value.url, (newUrl) => {
  if (!isNameOptional.value) return
  if (!newUrl || !newUrl.trim()) {
    isFetchingTitle.value = false
    return
  }

  // 用户已手动输入名称则不覆盖
  if (form.value.name && form.value.name.trim()) return

  // 防抖：输入停止 800ms 后再请求
  clearTimeout(titleFetchTimer)
  titleFetchTimer = setTimeout(async () => {
    // 避免重复请求
    if (lastFetchedUrl === newUrl.trim()) return
    lastFetchedUrl = newUrl.trim()

    isFetchingTitle.value = true
    const title = await fetchWebsiteTitle(newUrl)
    isFetchingTitle.value = false

    // 再次检查：用户可能在请求期间手动输入了名称
    if (form.value.name && form.value.name.trim()) return
    if (title) {
      form.value.name = title
    }
  }, 800)
})

async function handleConfirm() {
  if (confirming) return
  confirming = true
  // 新建网站且名称为空时，尝试获取网站标题
  try {
    if (isNameOptional.value && !form.value.name.trim() && form.value.url.trim()) {
      isFetchingTitle.value = true
      const title = await fetchWebsiteTitle(form.value.url)
      isFetchingTitle.value = false
      if (title) {
        form.value.name = title
      }
    }
    navStore.confirmEditDialog(form.value.name, form.value.url, form.value.folderId)
  } finally {
    isFetchingTitle.value = false
    confirming = false
  }
}

function getInitialFolderId(type, node) {
  if (type === 'quickAddSite') return node?.id || ''
  if (type === 'editSite') return navStore.getGroupForNode(node)?.id || ''
  return ''
}
</script>

<template>
  <el-dialog
    v-model="navStore.editDialog.visible"
    :title="dialogTitle"
    :width="dialogWidth"
    @close="navStore.closeEditDialog"
    destroy-on-close
  >
    <el-form class="edit-form" :model="form" :label-width="formLabelWidth" @submit.prevent.stop="handleConfirm">
      <el-form-item label="名称" :required="isNameRequired">
        <el-input
          v-model="form.name"
          :placeholder="isAddSiteDialog ? '请输入网站名称（留空将自动获取网站标题）' : '请输入分组名称'"
          @keydown.enter.prevent.stop="handleConfirm"
        />
      </el-form-item>
      <el-form-item v-if="showUrl" label="网址" required>
        <el-input
          v-model="form.url"
          placeholder="请输入完整网址（带http/https）"
          @keydown.enter.prevent.stop="handleConfirm"
        />
      </el-form-item>
      <el-form-item v-if="showFolderPath" label="文件夹路径" required>
        <el-tree-select
          v-model="form.folderId"
          :data="folderTreeData"
          node-key="id"
          :props="{ label: 'label', children: 'children' }"
          placeholder="请选择文件夹路径"
          check-strictly
          default-expand-all
          filterable
          :filter-node-method="filterFolderNode"
          class="folder-select"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="navStore.closeEditDialog">取消</el-button>
      <el-button type="primary" :loading="isFetchingTitle" @click="handleConfirm">确认</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.edit-form :deep(.el-form-item) {
  margin-bottom: 18px;
}
.edit-form :deep(.el-form-item:last-child) {
  margin-bottom: 0;
}
.folder-select {
  width: 100%;
}
</style>
