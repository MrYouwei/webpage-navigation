<script setup>
import { ref, watch, computed } from 'vue'
import { useNavStore } from '../stores/nav'

const navStore = useNavStore()

const form = ref({ name: '', url: '' })

const dialogTitle = computed(() => {
  const { type } = navStore.editDialog
  const titles = {
    addRootGroup: '新建根分组',
    addGroup: '新建子分组',
    addSite: '新建网站',
    rename: '重命名分组',
    editSite: '编辑网站'
  }
  return titles[type] || '编辑'
})

const showUrl = computed(() => {
  return ['addSite', 'editSite'].includes(navStore.editDialog.type)
})

// addSite 类型下名称可不填（可从网址自动解析）
const isNameOptional = computed(() => navStore.editDialog.type === 'addSite')
const isNameRequired = computed(() => !isNameOptional.value)

// 从网址中提取名称：取主域名首段并首字母大写
// 例: www.google.com -> Google, github.com -> GitHub
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

watch(() => navStore.editDialog.visible, (visible) => {
  if (visible) {
    const { type, node } = navStore.editDialog
    form.value = {
      name: node?.name || '',
      url: node?.url || ''
    }
  }
})

// 监听网址输入：当名称为空时自动从网址解析
watch(() => form.value.url, (newUrl) => {
  if (!isNameOptional.value) return
  if (!newUrl || !newUrl.trim()) return
  if (form.value.name && form.value.name.trim()) return
  const auto = extractNameFromUrl(newUrl)
  if (auto) {
    form.value.name = auto
  }
})

function handleConfirm() {
  // addSite 且名称为空时，自动从网址解析后再提交
  if (isNameOptional.value && !form.value.name.trim() && form.value.url.trim()) {
    const auto = extractNameFromUrl(form.value.url)
    if (auto) {
      form.value.name = auto
    }
  }
  navStore.confirmEditDialog(form.value.name, form.value.url)
}
</script>

<template>
  <el-dialog
    v-model="navStore.editDialog.visible"
    :title="dialogTitle"
    width="400px"
    @close="navStore.closeEditDialog"
    destroy-on-close
  >
    <el-form :model="form" label-width="80px">
      <el-form-item label="名称" :required="isNameRequired">
        <el-input
          v-model="form.name"
          :placeholder="navStore.editDialog.type === 'addSite' ? '请输入网站名称（留空将自动解析）' : '请输入分组名称'"
          @keyup.enter="handleConfirm"
        />
        <div v-if="isNameOptional" class="name-hint">留空将自动从网址解析</div>
      </el-form-item>
      <el-form-item v-if="showUrl" label="网址" required>
        <el-input
          v-model="form.url"
          placeholder="请输入完整网址（带http/https）"
          @keyup.enter="handleConfirm"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="navStore.closeEditDialog">取消</el-button>
      <el-button type="primary" @click="handleConfirm">确认</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.name-hint {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
  line-height: 1.4;
}
</style>
