import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { api } from '../api/index'
import { ElMessage, ElMessageBox } from 'element-plus'

const SIDEBAR_COLLAPSED_KEY = 'webpage-navigation:sidebar-collapsed'

function generateId() {
  return Date.now() + '-' + Math.random().toString(36).substr(2, 9)
}

function cloneNavGroups(groups) {
  if (!Array.isArray(groups)) return []
  return groups.map(cloneNavNode)
}

function cloneNavNode(node) {
  const cloned = {
    id: node.id,
    name: node.name,
    type: node.type
  }
  if (node.type === 'link') {
    cloned.url = node.url || ''
  }
  if (node.type === 'group') {
    cloned.children = cloneNavGroups(node.children || [])
    cloned.expanded = node.expanded || false
  }
  return cloned
}

function serializeNavData(groups) {
  return JSON.stringify(cloneNavGroups(groups))
}

function normalizeNavGroups(groups) {
  return cloneNavGroups(groups).map(node => {
    if (node.type === 'group') {
      node.expanded = false
      node.children = normalizeNavGroups(node.children || [])
    }
    return node
  })
}

function escapeHtml(str) {
  return String(str || '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;')
}

function escapeRegExp(str) {
  return String(str || '').replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

function escapeXml(str) {
  return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, '&apos;')
}

function formatDate(date) {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}${m}${d}`
}

// 从网址中提取名称：取主域名首段并首字母大写
// 例: www.google.com -> Google, github.com -> GitHub, http://example.com -> Example
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

export const useNavStore = defineStore('nav', () => {
  const bookmarkData = ref([])
  const currentSelectNode = ref(null)
  const currentUser = ref(null)
  const isDirty = ref(false)
  const isSaving = ref(false)
  const isLoading = ref(false)
  const loadingText = ref('加载中')
  const searchKeyword = ref('')
  const searchScope = ref('all')
  const sidebarCollapsed = ref(false)
  const rightClickMenu = ref({ visible: false, x: 0, y: 0, nodeType: '', nodeId: null })
  const editDialog = ref({ visible: false, type: '', node: null })
  const exportDialog = ref({ visible: false })
  const authDialog = ref({ visible: false, mode: 'login' })
  const authLoading = ref(false)
  const authError = ref('')
  const checkedLinkIds = ref([])
  const saveTimer = ref(null)
  const activeModule = ref('我的')
  const navScrollAnchor = ref('') // 导航模块左侧树点击 → 右侧分块滚动锚点

  const lastSavedSnapshot = ref('[]')

  const isLoggedIn = computed(() => !!currentUser.value)
  const hasUnsavedChanges = computed(() => isDirty.value)
  const selectedLinks = computed(() => checkedLinkIds.value)

  async function init() {
    loadSidebarState()
    await loadData()
  }

  function loadSidebarState() {
    sidebarCollapsed.value = localStorage.getItem(SIDEBAR_COLLAPSED_KEY) === '1'
  }

  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
    localStorage.setItem(SIDEBAR_COLLAPSED_KEY, sidebarCollapsed.value ? '1' : '0')
  }

  function setLoading(loading, text = '加载中') {
    isLoading.value = loading
    loadingText.value = text
  }

  async function loadData() {
    setLoading(true, '正在检查登录状态')
    try {
      const response = await api.getCurrentUser()
      applyAuthenticatedUser(response.data)
    } catch (e) {
      applyGuestState()
    } finally {
      setLoading(false)
    }
  }

  function applyAuthenticatedUser(data) {
    currentUser.value = {
      userId: data.userId,
      username: data.username
    }
    const navData = data.navData
    const groups = Array.isArray(navData) ? navData : (navData?.groups || [])
    bookmarkData.value = normalizeNavGroups(groups)
    applyExpandedStatesFromStorage()
    lastSavedSnapshot.value = serializeNavData(bookmarkData.value)
    isDirty.value = false
    currentSelectNode.value = null
    checkedLinkIds.value = []
  }

  function applyGuestState() {
    currentUser.value = null
    bookmarkData.value = []
    currentSelectNode.value = null
    rightClickMenu.value = { visible: false, x: 0, y: 0, nodeType: '', nodeId: null }
    lastSavedSnapshot.value = '[]'
    isDirty.value = false
    saveTimer.value && clearTimeout(saveTimer.value)
    checkedLinkIds.value = []
  }

  function getExpandedStorageKey() {
    const userKey = currentUser.value ? currentUser.value.userId : 'guest'
    return `webpage-navigation:expanded:${userKey}`
  }

  function saveExpandedStatesToStorage() {
    if (!currentUser.value) return
    const states = {}
    collectExpandedStates(bookmarkData.value, states)
    localStorage.setItem(getExpandedStorageKey(), JSON.stringify(states))
  }

  function collectExpandedStates(list, states) {
    list.forEach(node => {
      if (node.type === 'group') {
        states[node.id] = Boolean(node.expanded)
        collectExpandedStates(node.children || [], states)
      }
    })
  }

  function loadExpandedStatesFromStorage() {
    if (!currentUser.value) return {}
    try {
      return JSON.parse(localStorage.getItem(getExpandedStorageKey()) || '{}') || {}
    } catch {
      return {}
    }
  }

  function applyExpandedStatesFromStorage() {
    const states = loadExpandedStatesFromStorage()
    restoreExpandedStates(bookmarkData.value, states)
  }

  function restoreExpandedStates(list, states) {
    list.forEach(node => {
      if (states.hasOwnProperty(node.id)) {
        node.expanded = states[node.id]
      }
      if (node.type === 'group' && node.children) {
        restoreExpandedStates(node.children, states)
      }
    })
  }

  function markDirty() {
    isDirty.value = serializeNavData(bookmarkData.value) !== lastSavedSnapshot.value
  }

  function scheduleAutoSave() {
    saveTimer.value && clearTimeout(saveTimer.value)
    saveTimer.value = setTimeout(() => {
      persistData(false)
    }, 800)
  }

  async function persistData(showSuccess = false) {
    if (!currentUser.value || isSaving.value || !isDirty.value) return
    isSaving.value = true
    try {
      const navPayload = buildNavPayload()
      await api.saveNavData(navPayload)
      lastSavedSnapshot.value = serializeNavData(bookmarkData.value)
      isDirty.value = false
      if (showSuccess) ElMessage.success('保存成功')
    } catch (e) {
      isDirty.value = true
    } finally {
      isSaving.value = false
    }
  }

  function buildNavPayload() {
    return {
      version: '1.0',
      updatedAt: new Date().toISOString(),
      groups: cloneNavGroups(bookmarkData.value)
    }
  }

  function notifyChange() {
    if (!currentUser.value) return
    markDirty()
    scheduleAutoSave()
  }

  function findNodeById(nodes, targetId) {
    for (const node of nodes) {
      if (node.id === targetId) return node
      if (node.type === 'group' && node.children) {
        const found = findNodeById(node.children, targetId)
        if (found) return found
      }
    }
    return null
  }

  function findNodeAndParent(list, id) {
    for (let i = 0; i < list.length; i++) {
      if (list[i].id === id) {
        return { node: list[i], parent: list, index: i }
      }
      if (list[i].type === 'group' && list[i].children) {
        const result = findNodeAndParent(list[i].children, id)
        if (result) return result
      }
    }
    return null
  }

  function isAncestor(ancestorId, descendantId) {
    function containsId(list, id) {
      for (const node of list) {
        if (node.id === id) return true
        if (node.type === 'group' && node.children && containsId(node.children, id)) return true
      }
      return false
    }
    function findInTree(list) {
      for (const node of list) {
        if (node.id === ancestorId && node.type === 'group' && node.children) {
          if (containsId(node.children, descendantId)) return true
        }
        if (node.type === 'group' && node.children) {
          if (findInTree(node.children)) return true
        }
      }
      return false
    }
    return findInTree(bookmarkData.value)
  }

  function selectNode(node) {
    currentSelectNode.value = node
  }

  function addRootGroup(name) {
    const newGroup = {
      id: generateId(),
      name,
      type: 'group',
      children: [],
      expanded: true
    }
    bookmarkData.value.push(newGroup)
    notifyChange()
  }

  function addGroup(parentNode, name) {
    const newGroup = {
      id: generateId(),
      name,
      type: 'group',
      children: [],
      expanded: true
    }
    parentNode.children = parentNode.children || []
    parentNode.children.push(newGroup)
    parentNode.expanded = true
    notifyChange()
  }

  function addSite(parentNode, name, url) {
    let fullUrl = url
    if (!/^[a-zA-Z][a-zA-Z\d+\-.]*:\/\//.test(url)) {
      fullUrl = 'http://' + url
    }
    // 名称留空时，自动从网址解析；解析失败则使用兜底名
    let finalName = name
    if (!finalName || !finalName.trim()) {
      finalName = extractNameFromUrl(fullUrl) || '未命名网站'
    }
    const newSite = {
      id: generateId(),
      name: finalName,
      type: 'link',
      url: fullUrl
    }
    parentNode.children = parentNode.children || []
    parentNode.children.push(newSite)
    notifyChange()
  }

  function renameNode(node, name) {
    node.name = name
    notifyChange()
  }

  function editSite(node, name, url) {
    let fullUrl = url
    if (!/^[a-zA-Z][a-zA-Z\d+\-.]*:\/\//.test(url)) {
      fullUrl = 'http://' + url
    }
    node.name = name
    node.url = fullUrl
    notifyChange()
  }

  function deleteNodeById(nodeId) {
    const expandedStates = {}
    collectExpandedStates(bookmarkData.value, expandedStates)

    function removeNode(list, targetId) {
      for (let i = 0; i < list.length; i++) {
        if (list[i].id === targetId) {
          list.splice(i, 1)
          return true
        }
        if (list[i].children && list[i].children.length > 0) {
          if (removeNode(list[i].children, targetId)) return true
        }
      }
      return false
    }

    removeNode(bookmarkData.value, nodeId)
    notifyChange()
    restoreExpandedStates(bookmarkData.value, expandedStates)
    if (currentSelectNode.value && currentSelectNode.value.id === nodeId) {
      currentSelectNode.value = null
    }
  }

  function deleteFolderOnlyById(nodeId) {
    const node = findNodeById(bookmarkData.value, nodeId)
    if (!node || node.type !== 'group') return

    const expandedStates = {}
    collectExpandedStates(bookmarkData.value, expandedStates)

    function removeFolderOnly(list, targetId) {
      for (let i = 0; i < list.length; i++) {
        if (list[i].id === targetId) {
          if (list[i].children && list[i].children.length > 0) {
            list.splice(i, 1, ...list[i].children)
          } else {
            list.splice(i, 1)
          }
          return true
        }
        if (list[i].children && list[i].children.length > 0) {
          if (removeFolderOnly(list[i].children, targetId)) return true
        }
      }
      return false
    }

    removeFolderOnly(bookmarkData.value, nodeId)
    notifyChange()
    restoreExpandedStates(bookmarkData.value, expandedStates)
  }

  function moveNode(draggedId, targetId, position) {
    if (isAncestor(draggedId, targetId)) return

    const expandedStates = {}
    collectExpandedStates(bookmarkData.value, expandedStates)

    const draggedInfo = findNodeAndParent(bookmarkData.value, draggedId)
    const targetInfo = findNodeAndParent(bookmarkData.value, targetId)

    if (!draggedInfo || !targetInfo) return

    const draggedData = draggedInfo.node

    if (position === 'inside') {
      draggedInfo.parent.splice(draggedInfo.index, 1)
      targetInfo.node.children = targetInfo.node.children || []
      targetInfo.node.children.push(draggedData)
      targetInfo.node.expanded = true
    } else {
      const targetParent = targetInfo.parent
      let targetIndex = targetParent.findIndex(n => n.id === targetId)

      if (position === 'after') targetIndex += 1

      if (draggedInfo.parent === targetParent && draggedInfo.index < targetIndex) {
        targetIndex -= 1
      }

      draggedInfo.parent.splice(draggedInfo.index, 1)
      targetParent.splice(targetIndex, 0, draggedData)
    }

    notifyChange()
    saveExpandedStatesToStorage()
    restoreExpandedStates(bookmarkData.value, expandedStates)
  }

  function toggleGroupExpand(node) {
    node.expanded = !node.expanded
    saveExpandedStatesToStorage()
  }

  function expandAll() {
    function walk(list) {
      list.forEach(node => {
        if (node.type === 'group') {
          node.expanded = true
          walk(node.children || [])
        }
      })
    }
    walk(bookmarkData.value)
    saveExpandedStatesToStorage()
  }

  function collapseAll() {
    function walk(list) {
      list.forEach(node => {
        if (node.type === 'group') {
          node.expanded = false
          walk(node.children || [])
        }
      })
    }
    walk(bookmarkData.value)
    saveExpandedStatesToStorage()
  }

  function clearAll() {
    ElMessageBox.confirm('确定要清空所有数据吗？此操作不可恢复！', '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(() => {
      bookmarkData.value = []
      notifyChange()
      currentSelectNode.value = null
      checkedLinkIds.value = []
      ElMessage.success('已清空所有数据')
    }).catch(() => {})
  }

  const filteredLinks = computed(() => {
    const keyword = searchKeyword.value.trim().toLowerCase()
    if (!keyword) return null

    const scope = searchScope.value
    const allLinks = []
    const groupPathMap = buildLinkGroupPathMap(bookmarkData.value)

    function matchesField(value) {
      return String(value || '').toLowerCase().includes(keyword)
    }

    function matchesLink(node) {
      const groupPath = groupPathMap[node.id] || ''
      if (scope === 'name') return matchesField(node.name)
      if (scope === 'url') return matchesField(node.url)
      if (scope === 'group') return matchesField(groupPath)
      return matchesField(node.name) || matchesField(node.url) || matchesField(groupPath)
    }

    function searchNodes(list) {
      list.forEach(node => {
        if (node.type === 'link') {
          if (matchesLink(node)) {
            allLinks.push({ ...node, groupPath: groupPathMap[node.id] || '' })
          }
        } else if (node.type === 'group' && node.children) {
          searchNodes(node.children)
        }
      })
    }

    searchNodes(bookmarkData.value)
    return allLinks
  })

  function buildLinkGroupPathMap(list, path = [], map = {}) {
    list.forEach(node => {
      if (node.type === 'group') {
        const nextPath = path.concat(node.name || '')
        buildLinkGroupPathMap(node.children || [], nextPath, map)
      } else if (node.type === 'link') {
        map[node.id] = path.filter(Boolean).join(' / ')
      }
    })
    return map
  }

  function highlightField(value, keyword, field, scope) {
    const safeValue = escapeHtml(value || '')
    if (!keyword || (scope !== 'all' && scope !== field)) return safeValue
    const reg = new RegExp(`(${escapeRegExp(escapeHtml(keyword))})`, 'gi')
    return safeValue.replace(reg, '<mark>$1</mark>')
  }

  function showContextMenu(x, y, nodeType, nodeId) {
    let menuX = x
    let menuY = y
    rightClickMenu.value = { visible: true, x: menuX, y: menuY, nodeType, nodeId }
  }

  function hideContextMenu() {
    rightClickMenu.value = { visible: false, x: 0, y: 0, nodeType: '', nodeId: null }
  }

  function openEditDialog(type, node = null) {
    editDialog.value = { visible: true, type, node }
  }

  function closeEditDialog() {
    editDialog.value = { visible: false, type: '', node: null }
  }

  function confirmEditDialog(name, url) {
    const { type, node } = editDialog.value
    // addSite 类型名称可不填（将自动从网址解析）
    if (type !== 'addSite' && !name.trim()) {
      ElMessage.warning('请输入名称')
      return
    }

    const expandedStates = {}
    collectExpandedStates(bookmarkData.value, expandedStates)

    switch (type) {
      case 'addRootGroup':
        addRootGroup(name.trim())
        break
      case 'addGroup':
        if (!node) return
        addGroup(node, name.trim())
        break
      case 'addSite':
        if (!node) return
        if (!url.trim()) {
          ElMessage.warning('请输入完整网址')
          return
        }
        addSite(node, name.trim(), url.trim())
        break
      case 'rename':
        if (!node) return
        renameNode(node, name.trim())
        break
      case 'editSite':
        if (!node) return
        if (!url.trim()) {
          ElMessage.warning('请输入完整网址')
          return
        }
        editSite(node, name.trim(), url.trim())
        break
    }

    restoreExpandedStates(bookmarkData.value, expandedStates)
    saveExpandedStatesToStorage()
    closeEditDialog()
  }

  function openExportDialog() {
    exportDialog.value = { visible: true }
  }

  function closeExportDialog() {
    exportDialog.value = { visible: false }
  }

  function exportData(format) {
    let content = ''
    let fileName = `网站书签_${formatDate(new Date())}`
    let mimeType = ''

    switch (format) {
      case 'json':
        content = JSON.stringify(bookmarkData.value, null, 2)
        fileName += '.json'
        mimeType = 'application/json'
        break
      case 'xml':
        content = convertToXML(bookmarkData.value)
        fileName += '.xml'
        mimeType = 'text/xml'
        break
      case 'html':
        content = convertToHTMLBookmark(bookmarkData.value)
        fileName += '.html'
        mimeType = 'text/html'
        break
    }

    const blob = new Blob([content], { type: mimeType })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = fileName
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
    closeExportDialog()
  }

  function convertToXML(data) {
    let xml = '<?xml version="1.0" encoding="UTF-8"?>\n<bookmarks>\n'
    function buildXML(nodes, indent = '  ') {
      let xmlStr = ''
      nodes.forEach(node => {
        if (node.type === 'group') {
          xmlStr += `${indent}<group name="${escapeXml(node.name)}">\n`
          if (node.children && node.children.length > 0) {
            xmlStr += buildXML(node.children, indent + '  ')
          }
          xmlStr += `${indent}</group>\n`
        } else if (node.type === 'link') {
          xmlStr += `${indent}<site name="${escapeXml(node.name)}" url="${escapeXml(node.url)}"/>\n`
        }
      })
      return xmlStr
    }
    xml += buildXML(data)
    xml += '</bookmarks>'
    return xml
  }

  function convertToHTMLBookmark(data) {
    let html = `<!DOCTYPE NETSCAPE-Bookmark-file-1>\n<META HTTP-EQUIV="Content-Type" CONTENT="text/html; charset=UTF-8">\n<TITLE>网站书签</TITLE>\n<DL><p>\n`
    function buildHTML(nodes, indent = '  ') {
      let htmlStr = ''
      nodes.forEach(node => {
        if (node.type === 'group') {
          htmlStr += `${indent}<DT><H3>${escapeHtml(node.name)}</H3>\n`
          htmlStr += `${indent}<DL><p>\n`
          if (node.children && node.children.length > 0) {
            htmlStr += buildHTML(node.children, indent + '  ')
          }
          htmlStr += `${indent}</DL><p>\n`
        } else if (node.type === 'link') {
          htmlStr += `${indent}<DT><A HREF="${escapeHtml(node.url)}">${escapeHtml(node.name)}</A>\n`
        }
      })
      return htmlStr
    }
    html += buildHTML(data)
    html += '</DL><p>'
    return html
  }

  async function importData(file) {
    if (!currentUser.value || !file) return

    try {
      const content = await file.text()
      const fileName = file.name.toLowerCase()
      let importedData = []

      if (fileName.endsWith('.json')) {
        importedData = JSON.parse(content)
      } else if (fileName.endsWith('.xml')) {
        importedData = parseXML(content)
      } else if (fileName.endsWith('.html')) {
        importedData = parseHTMLBookmark(content)
      }

      if (!Array.isArray(importedData)) {
        ElMessage.error('导入失败：数据格式不正确')
        return
      }

      const isMerge = await ElMessageBox.confirm(
        '是否合并到当前数据？取消则替换全部数据',
        '导入确认',
        { confirmButtonText: '合并', cancelButtonText: '替换', type: 'info' }
      ).catch(() => false)

      if (isMerge) {
        importedData = handleDuplicateNames(bookmarkData.value, importedData)
        bookmarkData.value = bookmarkData.value.concat(importedData)
      } else {
        bookmarkData.value = importedData
      }

      let stats = `导入成功！\n\n解析到 ${importedData.length} 个顶级节点\n`
      const totalStats = countNodes(importedData)
      stats += `总计：${totalStats.groups} 个分组，${totalStats.links} 个网站`
      ElMessage.success(stats)

      notifyChange()
    } catch (err) {
      console.error('导入失败', err)
      ElMessage.error('导入失败：文件解析错误，请检查文件格式')
    }
  }

  function countNodes(nodes) {
    let count = { groups: 0, links: 0 }
    nodes.forEach(node => {
      if (node.type === 'group') {
        count.groups++
        if (node.children) {
          const childCount = countNodes(node.children)
          count.groups += childCount.groups
          count.links += childCount.links
        }
      } else if (node.type === 'link') {
        count.links++
      }
    })
    return count
  }

  function parseXML(xmlStr) {
    const parser = new DOMParser()
    const xmlDoc = parser.parseFromString(xmlStr, 'text/xml')
    const root = xmlDoc.querySelector('bookmarks')
    if (!root) return []

    function parseNodes(parentElement) {
      const nodes = []
      const groups = parentElement.querySelectorAll(':scope > group')
      groups.forEach(group => {
        const name = group.getAttribute('name')
        const children = parseNodes(group)
        nodes.push({ id: generateId(), name, type: 'group', children })
      })
      const sites = parentElement.querySelectorAll(':scope > site')
      sites.forEach(site => {
        const name = site.getAttribute('name')
        const url = site.getAttribute('url')
        nodes.push({ id: generateId(), name, type: 'link', url })
      })
      return nodes
    }
    return parseNodes(root)
  }

  function parseHTMLBookmark(htmlStr) {
    if (!htmlStr || typeof htmlStr !== 'string') return []

    try {
      htmlStr = htmlStr.replace(/<!--[\s\S]*?-->/g, '')
      htmlStr = htmlStr.replace(/<p>/gi, '')
      htmlStr = htmlStr.replace(/<\/p>/gi, '')
    } catch {
      return []
    }

    const result = []
    const stack = [result]
    let currentGroup = null
    const lines = htmlStr.split('\n')

    for (let i = 0; i < lines.length; i++) {
      const line = lines[i] ? lines[i].trim() : ''

      const h3Match = line.match(/<DT><H3[^>]*>([\s\S]*?)<\/H3>/i)
      if (h3Match) {
        const groupName = h3Match[1].trim()
        const groupNode = { id: generateId(), name: groupName, type: 'group', children: [] }
        stack[stack.length - 1].push(groupNode)
        currentGroup = groupNode
        continue
      }

      const aMatch = line.match(/<DT><A[^>]+HREF="([^"]+)"[^>]*>([\s\S]*?)<\/A>/i)
      if (aMatch) {
        stack[stack.length - 1].push({
          id: generateId(),
          name: aMatch[2].trim(),
          type: 'link',
          url: aMatch[1]
        })
        continue
      }

      if (line.indexOf('<DL>') !== -1) {
        if (currentGroup) {
          stack.push(currentGroup.children)
        }
        continue
      }

      if (line.indexOf('</DL>') !== -1) {
        if (stack.length > 1) stack.pop()
        currentGroup = stack[stack.length - 1][stack[stack.length - 1].length - 1]
        if (currentGroup && currentGroup.type !== 'group') currentGroup = null
        continue
      }
    }
    return result
  }

  function handleDuplicateNames(existingData, newData) {
    const existingNames = new Set()
    function collectNames(list) {
      list.forEach(node => {
        existingNames.add(node.name)
        if (node.type === 'group' && node.children) collectNames(node.children)
      })
    }
    collectNames(existingData)

    function processNodes(nodes) {
      return nodes.map(node => {
        let newName = node.name
        let counter = 1
        while (existingNames.has(newName)) {
          newName = `${node.name} (${counter})`
          counter++
        }
        existingNames.add(newName)
        const processedNode = { ...node, name: newName }
        if (node.type === 'group' && node.children) {
          processedNode.children = processNodes(node.children)
        }
        return processedNode
      })
    }
    return processNodes(newData)
  }

  function openAuthDialog(mode = 'login') {
    authDialog.value = { visible: true, mode }
    authError.value = ''
  }

  function closeAuthDialog() {
    authDialog.value = { visible: false, mode: 'login' }
    authError.value = ''
  }

  async function submitAuth(formData) {
    if (authLoading.value) return
    authLoading.value = true
    authError.value = ''

    try {
      let response
      if (authDialog.value.mode === 'login') {
        response = await api.login({ username: formData.username, password: formData.password })
      } else {
        response = await api.register({
          username: formData.username,
          password: formData.password,
          confirmPassword: formData.confirmPassword
        })
      }
      applyAuthenticatedUser(response.data)
      closeAuthDialog()
      ElMessage.success(authDialog.value.mode === 'login' ? '登录成功' : '注册成功')
    } catch (e) {
      authError.value = e.message || '操作失败'
    } finally {
      authLoading.value = false
    }
  }

  async function logout() {
    if (isDirty.value) {
      try {
        await ElMessageBox.confirm('当前有未保存的数据，确定要退出登录吗？', '提示', {
          confirmButtonText: '确定退出',
          cancelButtonText: '取消',
          type: 'warning'
        })
      } catch {
        return
      }
    }
    try {
      await api.logout()
    } catch {}
    applyGuestState()
    ElMessage.success('已退出登录')
  }

  function toggleLinkChecked(linkId) {
    const idx = checkedLinkIds.value.indexOf(linkId)
    if (idx >= 0) {
      checkedLinkIds.value.splice(idx, 1)
    } else {
      checkedLinkIds.value.push(linkId)
    }
  }

  // 收集某分组下（递归）所有链接 ID
  function collectLinkIdsInGroup(node) {
    const linkIds = []
    function walk(list) {
      (list || []).forEach(n => {
        if (n.type === 'link') {
          linkIds.push(n.id)
        } else if (n.type === 'group' && n.children) {
          walk(n.children)
        }
      })
    }
    if (node && node.type === 'group') {
      walk(node.children)
    }
    return linkIds
  }

  // 分组复选框：若分组下所有链接均已选中则取消全部，否则选中全部
  function toggleGroupChecked(node) {
    if (!node || node.type !== 'group') return
    const linkIds = collectLinkIdsInGroup(node)
    if (linkIds.length === 0) return
    const allChecked = linkIds.every(id => checkedLinkIds.value.includes(id))
    const checkedSet = new Set(checkedLinkIds.value)
    if (allChecked) {
      linkIds.forEach(id => checkedSet.delete(id))
    } else {
      linkIds.forEach(id => checkedSet.add(id))
    }
    checkedLinkIds.value = Array.from(checkedSet)
  }

  function clearCheckedLinks() {
    checkedLinkIds.value = []
  }

  function batchDeleteCheckedLinks() {
    const ids = [...checkedLinkIds.value]
    if (ids.length === 0) return

    ElMessageBox.confirm(`确定要删除选中的 ${ids.length} 个网站吗？删除后无法恢复！`, '警告', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(() => {
      const expandedStates = {}
      collectExpandedStates(bookmarkData.value, expandedStates)

      function removeSelected(list) {
        for (let i = list.length - 1; i >= 0; i--) {
          if (list[i].type === 'link' && ids.includes(list[i].id)) {
            list.splice(i, 1)
          } else if (list[i].type === 'group' && list[i].children) {
            removeSelected(list[i].children)
          }
        }
      }

      removeSelected(bookmarkData.value)
      notifyChange()
      restoreExpandedStates(bookmarkData.value, expandedStates)
      checkedLinkIds.value = []
      ElMessage.success('删除成功')
    }).catch(() => {})
  }

  return {
    bookmarkData,
    currentSelectNode,
    currentUser,
    isDirty,
    isSaving,
    isLoading,
    loadingText,
    searchKeyword,
    searchScope,
    sidebarCollapsed,
    rightClickMenu,
    editDialog,
    exportDialog,
    authDialog,
    authLoading,
    authError,
    checkedLinkIds,
    activeModule,
    navScrollAnchor,
    filteredLinks,
    selectedLinks,
    isLoggedIn,
    hasUnsavedChanges,
    init,
    toggleSidebar,
    loadData,
    selectNode,
    addRootGroup,
    addGroup,
    addSite,
    renameNode,
    editSite,
    deleteNodeById,
    deleteFolderOnlyById,
    moveNode,
    toggleGroupExpand,
    expandAll,
    collapseAll,
    clearAll,
    showContextMenu,
    hideContextMenu,
    openEditDialog,
    closeEditDialog,
    confirmEditDialog,
    openExportDialog,
    closeExportDialog,
    exportData,
    importData,
    openAuthDialog,
    closeAuthDialog,
    submitAuth,
    logout,
    persistData,
    toggleLinkChecked,
    toggleGroupChecked,
    collectLinkIdsInGroup,
    clearCheckedLinks,
    batchDeleteCheckedLinks,
    buildLinkGroupPathMap,
    highlightField,
    findNodeById,
    findNodeAndParent,
    generateId
  }
})
