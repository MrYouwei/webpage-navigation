<script setup>
import { computed, watch, nextTick, ref } from 'vue'
import { useNavStore } from '../stores/nav'

const navStore = useNavStore()

const searchScopeOptions = [
  { value: 'all', label: '全量' },
  { value: 'name', label: '名称' },
  { value: 'url', label: '网址' },
  { value: 'group', label: '分组' }
]

const hasSearch = computed(() => navStore.searchKeyword.trim().length > 0)
const currentModule = computed(() => navStore.activeModule)
const isMyModule = computed(() => currentModule.value === '我的')
const isHomeModule = computed(() => currentModule.value === '首页')
const isNavModule = computed(() => currentModule.value === '导航')
const isPlazaModule = computed(() => currentModule.value === '广场')

// ============ 我的模块 - 书签逻辑 ============
function collectLinksFromList(list, path = []) {
  const links = []
  list.forEach(node => {
    if (node.type === 'link') {
      links.push({
        ...node,
        groupPath: node.groupPath || path.filter(Boolean).join(' / ')
      })
    } else if (node.type === 'group' && node.children) {
      links.push(...collectLinksFromList(node.children, path.concat(node.name || '')))
    }
  })
  return links
}

function collectLinksFromGroup(node) {
  if (!node || node.type !== 'group') return []
  return collectLinksFromList(node.children || [], [node.name || ''])
}

const displayLinks = computed(() => {
  if (!navStore.isLoggedIn) return []
  if (hasSearch.value) return navStore.filteredLinks || []
  if (navStore.currentSelectNode?.type === 'group') {
    const links = []
    function collectLinksRecursive(node) {
      if (!node.children) return
      node.children.forEach(child => {
        if (child.type === 'link') links.push(child)
        else if (child.type === 'group') collectLinksRecursive(child)
      })
    }
    collectLinksRecursive(navStore.currentSelectNode)
    return links
  }
  const allLinks = []
  function collect(list) {
    list.forEach(node => {
      if (node.type === 'link') allLinks.push(node)
      else if (node.type === 'group' && node.children) collect(node.children)
    })
  }
  collect(navStore.bookmarkData)
  return allLinks
})

// ============ 导航模块 - 公共推荐导航网站（占位假数据，后续替换为接口） ============
const publicNavSections = [
  {
    sectionId: 'ai-general',
    groupTitle: 'AI · 通用',
    title: '通用',
    links: [
      { name: 'DeepSeek',          url: 'https://chat.deepseek.com',       icon: '🐙' },
      { name: 'QwenChat',          url: 'https://chat.qwen.aliyun.com',    icon: '💫' },
      { name: '豆包',              url: 'https://www.doubao.com',          icon: '🧑' },
      { name: 'Kimi',              url: 'https://kimi.moonshot.cn',        icon: '🅺' },
      { name: 'Google AI Studio',  url: 'https://aistudio.google.com',    icon: '🟦' },
      { name: 'Gemini',            url: 'https://gemini.google.com',       icon: '✦' },
      { name: 'ChatGPT',           url: 'https://chat.openai.com',         icon: '🌐' },
      { name: 'Grok',              url: 'https://grok.com',                icon: '𝕏' },
      { name: 'Claude',            url: 'https://claude.ai',               icon: '❋' },
      { name: '腾讯元宝',          url: 'https://yuanbao.tencent.com',     icon: '🟢' },
      { name: '通义千问',          url: 'https://tongyi.aliyun.com',       icon: '🔷' },
      { name: '秘塔AI搜索',        url: 'https://metaso.cn',               icon: '🗻' },
      { name: '文心一言',          url: 'https://yiyan.baidu.com',         icon: '⬢' },
      { name: 'LLM模型桌面站',     url: 'https://llm.dev',                 icon: '🐈‍⬛' },
      { name: '陆续新增中…',       url: '#',                               icon: '🔵', placeholder: true }
    ]
  },
  {
    sectionId: 'ai-search',
    groupTitle: 'AI · 搜索',
    title: '搜索',
    links: [
      { name: 'DeepSeek',       url: 'https://chat.deepseek.com',       icon: '🐙' },
      { name: 'Perplexity',     url: 'https://www.perplexity.ai',       icon: '⬛' },
      { name: '纳米AI搜索',     url: 'https://nano.ai',                 icon: '🎯' },
      { name: 'Bilin AI',       url: 'https://bilin.ai',                icon: '🫧' },
      { name: 'Felo AI',        url: 'https://felo.ai',                 icon: '🔮' },
      { name: '陆续新增中…',    url: '#',                               icon: '🔵', placeholder: true }
    ]
  },
  {
    sectionId: 'ai-image',
    groupTitle: 'AI · 图像',
    title: '图像',
    links: [
      { name: 'Lovart',          url: 'https://lovart.ai',              icon: '🌀' },
      { name: 'FLUX.1 Kontext',  url: 'https://fal.ai/models/flux-1',   icon: '⚡' },
      { name: '开源SD整合包',    url: 'https://github.com/AUTOMATIC1111/stable-diffusion-webui', icon: '🤖' },
      { name: 'Civitai Green',   url: 'https://civitai.com',            icon: '🟣' },
      { name: 'LiblibAI',        url: 'https://www.liblib.art',          icon: '🟦' },
      { name: 'Midjourney',      url: 'https://www.midjourney.com',     icon: '⛵' },
      { name: '可灵AI',          url: 'https://klingai.com',            icon: '🔯' },
      { name: '即梦',            url: 'https://jimeng.jianying.com',    icon: '💎' },
      { name: 'DALL·E',          url: 'https://openai.com/dall-e-3',    icon: '🧩' },
      { name: 'Janus-Pro',       url: 'https://januspro.deeping.ai',    icon: '🌓' },
      { name: '阿里万相',        url: 'https://tongyi.aliyun.com/wanxiang', icon: '🌌' },
      { name: '陆续新增中…',     url: '#',                              icon: '🔵', placeholder: true }
    ]
  },
  {
    sectionId: 'ai-video',
    groupTitle: 'AI · 视频',
    title: '视频',
    links: [
      { name: '可灵AI',      url: 'https://klingai.com',               icon: '🔯' },
      { name: '即梦',        url: 'https://jimeng.jianying.com',       icon: '💎' },
      { name: 'Sora',        url: 'https://openai.com/sora',           icon: '🌆' },
      { name: 'PixVerse',    url: 'https://pixverse.ai',               icon: '🎬' },
      { name: '阿里万相',    url: 'https://tongyi.aliyun.com/wanxiang', icon: '🌌' },
      { name: '陆续新增中…', url: '#',                                  icon: '🔵', placeholder: true }
    ]
  },
  {
    sectionId: 'ai-music',
    groupTitle: 'AI · 音乐',
    title: '音乐',
    links: [
      { name: 'suno',        url: 'https://suno.com',                  icon: '🎸' },
      { name: 'YuE (乐)',    url: 'https://yue.com',                    icon: '🎼' },
      { name: '陆续新增中…', url: '#',                                  icon: '🔵', placeholder: true }
    ]
  },
  {
    sectionId: 'ai-note',
    groupTitle: 'AI · 笔记',
    title: '笔记',
    links: [
      { name: 'Notion AI',   url: 'https://www.notion.so/product/ai',   icon: '📝' },
      { name: '飞书妙计',    url: 'https://www.feishu.cn/product/minutes', icon: '📒' },
      { name: '陆续新增中…', url: '#',                                  icon: '🔵', placeholder: true }
    ]
  },
  {
    sectionId: 'ai-ppt',
    groupTitle: 'AI · PPT',
    title: 'PPT',
    links: [
      { name: 'Gamma',       url: 'https://gamma.app',                 icon: '📊' },
      { name: 'Beautiful.ai', url: 'https://www.beautiful.ai',         icon: '🎨' },
      { name: '陆续新增中…', url: '#',                                  icon: '🔵', placeholder: true }
    ]
  },
  {
    sectionId: 'ai-tools',
    groupTitle: 'AI · 工具',
    title: '工具',
    links: [
      { name: 'Toolify',     url: 'https://www.toolify.ai',             icon: '🧰' },
      { name: 'Futurepedia', url: 'https://www.futurepedia.io',         icon: '🛠️' },
      { name: '陆续新增中…', url: '#',                                  icon: '🔵', placeholder: true }
    ]
  },
  {
    sectionId: 'ai-code',
    groupTitle: 'AI · 编程',
    title: '编程',
    links: [
      { name: 'GitHub Copilot', url: 'https://github.com/features/copilot', icon: '🐙' },
      { name: 'Cursor',          url: 'https://cursor.sh',                icon: '💻' },
      { name: '陆续新增中…',     url: '#',                                icon: '🔵', placeholder: true }
    ]
  },
  {
    sectionId: 'ai-aggregate',
    groupTitle: 'AI · 聚合',
    title: '聚合',
    links: [
      { name: 'FavAI',       url: 'https://favai.com',                 icon: '🎁' },
      { name: 'AI工具集',    url: 'https://ai-bot.cn',                  icon: '📦' },
      { name: '陆续新增中…', url: '#',                                  icon: '🔵', placeholder: true }
    ]
  },
  {
    sectionId: 'life',
    groupTitle: '生活',
    title: '生活',
    links: [
      { name: '内容接口待提供…', url: '#', icon: '🌱', placeholder: true }
    ]
  },
  {
    sectionId: 'movie',
    groupTitle: '影视',
    title: '影视',
    links: [
      { name: '内容接口待提供…', url: '#', icon: '🎬', placeholder: true }
    ]
  },
  {
    sectionId: 'search',
    groupTitle: '搜索',
    title: '搜索',
    links: [
      { name: '内容接口待提供…', url: '#', icon: '🔍', placeholder: true }
    ]
  }
]

// ============ 导航模块 - 滚动锚点联动 ============
const contentMainRef = ref(null)

// 在 content-main（嵌套滚动容器）内平滑滚动到指定元素
function scrollInContainer(container, targetEl, offset = 16) {
  if (!container || !targetEl) return
  const targetTop = targetEl.offsetTop - offset
  // 优先使用 CSS scroll-behavior 支持的 smooth scrollTo
  try {
    container.scrollTo({ top: targetTop, behavior: 'smooth' })
  } catch {
    container.scrollTop = targetTop
  }
}

watch(
  () => navStore.navScrollAnchor,
  async (anchor) => {
    if (!anchor || !isNavModule.value) return
    await nextTick()
    const container = contentMainRef.value || document.querySelector('.content-main')
    const resolveAnchor = (id) => document.getElementById('nav-section-' + id)
    let el = resolveAnchor(anchor)
    if (!el) {
      // 一级节点（如 AI 根）→ 映射到第一个子分块
      const map = {
        'ai-root': 'ai-general',
        'cat-ai': 'ai-general'
      }
      const fallback = map[anchor]
      if (fallback) el = resolveAnchor(fallback)
    }
    if (el) scrollInContainer(container, el)
  }
)

// ============ 我的模块 - 杂项 ============
const groupPathMap = computed(() => {
  return navStore.buildLinkGroupPathMap(navStore.bookmarkData)
})

function handleCardClick(link) {
  window.open(link.url, '_blank', 'noopener noreferrer')
}

function handleNavLinkClick(link, e) {
  if (link.placeholder || link.url === '#' || !link.url) {
    e.preventDefault()
    return
  }
  window.open(link.url, '_blank', 'noopener noreferrer')
}

function handleCardContextMenu(e, link) {
  e.preventDefault()
  e.stopPropagation()
  navStore.selectNode(link)
  navStore.showContextMenu(e.clientX, e.clientY, 'link', link.id)
}

function addSiteToSelectedGroup() {
  const node = navStore.currentSelectNode
  if (node && node.type === 'group') {
    navStore.openEditDialog('addSite', node)
  }
}

function getHighlightedField(value, field) {
  if (!hasSearch.value) return value
  return navStore.highlightField(value, navStore.searchKeyword, field, navStore.searchScope)
}
</script>

<template>
  <div class="content-area">
    <!-- ============ 搜索栏（导航模块不显示） ============ -->
    <div v-if="!isNavModule" class="search-bar">
      <div class="search-wrapper">
        <el-icon class="search-icon"><Search /></el-icon>
        <el-select
          v-model="navStore.searchScope"
          size="default"
          :disabled="!navStore.isLoggedIn"
          class="search-scope"
        >
          <el-option
            v-for="opt in searchScopeOptions"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
        <el-input
          v-model="navStore.searchKeyword"
          placeholder="搜索名称/网址/分组，回车确认"
          :disabled="!navStore.isLoggedIn"
          clearable
        />
      </div>
    </div>

    <!-- Loading Overlay -->
    <div v-if="navStore.isLoading" class="loading-overlay">
      <div class="loading-content">
        <el-icon class="is-loading" :size="24"><Loading /></el-icon>
        <span>{{ navStore.loadingText }}</span>
      </div>
    </div>

    <!-- Main Content -->
    <div ref="contentMainRef" class="content-main">
      <!-- ========== 首页（备用分支，实际首页由 App.vue 直接渲染） ========== -->
      <template v-if="isHomeModule">
        <div class="empty-state">
          <el-empty description="首页已移至顶栏下方全宽区域" />
        </div>
      </template>

      <!-- ========== 导航模块：分块文字链接布局（参考用户图1） ========== -->
      <template v-else-if="isNavModule">
        <div class="nav-sections-wrapper">
          <section
            v-for="sec in publicNavSections"
            :key="sec.sectionId"
            :id="'nav-section-' + sec.sectionId"
            class="nav-section"
          >
            <header class="nav-section-header">
              <div class="nav-section-title-row">
                <span class="nav-section-corner"></span>
                <h3 class="nav-section-title">{{ sec.title }}</h3>
              </div>
              <el-icon class="nav-section-search" :size="18" color="#a0aec0"><Search /></el-icon>
            </header>
            <div class="nav-links-card">
              <div
                v-for="(link, idx) in sec.links"
                :key="idx"
                class="nav-link-item"
                :class="{ placeholder: link.placeholder, clickable: !link.placeholder && link.url !== '#' }"
                @click="handleNavLinkClick(link, $event)"
              >
                <span class="nav-link-icon">{{ link.icon || '🌐' }}</span>
                <span class="nav-link-name">{{ link.name }}</span>
              </div>
            </div>
          </section>

          <!-- 底部接口占位提示 -->
          <div class="nav-footer-hint">
            <el-icon :size="16" color="#718096"><InfoFilled /></el-icon>
            <span>以上内容为前端占位，后续将通过接口返回公共推荐导航网站。</span>
          </div>
        </div>
      </template>

      <!-- ========== 广场 ========== -->
      <template v-else-if="isPlazaModule">
        <div class="plaza-blank"></div>
      </template>

      <!-- ========== 我的 ========== -->
      <template v-else-if="isMyModule">
        <div v-if="!navStore.isLoggedIn" class="guest-state">
          <el-icon :size="56" color="#cbd5e0"><User /></el-icon>
          <p>请登录后管理你的导航标签</p>
        </div>

        <div v-else-if="navStore.bookmarkData.length === 0" class="empty-state">
          <el-empty description="暂无分组，点击'新建分组'开始创建你的导航" />
        </div>

        <div v-else-if="displayLinks.length === 0" class="empty-state">
          <div v-if="hasSearch" style="text-align:center">
            <el-empty description="未找到匹配的网站" />
          </div>
          <div v-else-if="navStore.currentSelectNode && navStore.currentSelectNode.type === 'group'" style="text-align:center">
            <el-empty :description="'「' + navStore.currentSelectNode.name + '」分组下暂无网站'">
              <el-button type="primary" @click="addSiteToSelectedGroup">
                <el-icon><Link /></el-icon>
                <span>添加网站</span>
              </el-button>
            </el-empty>
          </div>
          <div v-else style="text-align:center">
            <el-empty description="暂无网站，请在左侧树中分组上右键添加网站">
              <el-button type="primary" @click="navStore.openEditDialog('addRootGroup')">
                <el-icon><FolderAdd /></el-icon>
                <span>新建分组</span>
              </el-button>
            </el-empty>
          </div>
        </div>

        <div v-else class="card-grid-wrapper">
          <div class="card-grid">
            <div
              v-for="link in displayLinks"
              :key="link.id"
              class="site-card"
              @click="handleCardClick(link)"
              @contextmenu="handleCardContextMenu($event, link)"
            >
              <div class="card-icon">{{ (link.name || '?').charAt(0).toUpperCase() }}</div>
              <div class="card-name" v-html="getHighlightedField(link.name, 'name')"></div>
              <div class="card-url" v-html="getHighlightedField(link.url, 'url')"></div>
              <div
                v-if="link.groupPath || groupPathMap[link.id]"
                class="card-group"
                :title="link.groupPath || groupPathMap[link.id]"
                v-html="getHighlightedField(link.groupPath || groupPathMap[link.id] || '', 'group')"
              ></div>
            </div>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped>
.content-area {
  flex: 1;
  height: 100%;
  display: flex;
  flex-direction: column;
  background-color: #f7fafc;
  position: relative;
  overflow: hidden;
}

/* ===== 搜索栏 ===== */
.search-bar {
  padding: 20px 24px;
  background-color: #ffffff;
  border-bottom: 1px solid #e2e8f0;
  flex-shrink: 0;
}
.search-wrapper {
  display: flex;
  align-items: center;
  gap: 12px;
  max-width: 880px;
  margin: 0 auto;
  padding: 10px 16px;
  background-color: #f7fafc;
  border: 1px solid #e2e8f0;
  border-radius: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: border-color 0.2s, box-shadow 0.2s;
}
.search-wrapper:focus-within {
  border-color: #4299e1;
  box-shadow: 0 2px 12px rgba(66, 153, 225, 0.15);
}
.search-icon {
  color: #4299e1;
  font-size: 22px;
  flex-shrink: 0;
}
.search-scope {
  width: 100px;
  flex-shrink: 0;
}
.search-wrapper .el-input { flex: 1; }
.search-wrapper .el-input :deep(.el-input__wrapper) {
  background-color: transparent;
  box-shadow: none !important;
}
.search-wrapper :deep(.el-select .el-select__wrapper) {
  background-color: transparent;
  box-shadow: none !important;
}

/* ===== 主内容区 ===== */
.content-main {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}
.content-main::-webkit-scrollbar { width: 8px; }
.content-main::-webkit-scrollbar-track { background: #edf2f7; }
.content-main::-webkit-scrollbar-thumb { background: #cbd5e0; border-radius: 4px; }

.guest-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  gap: 16px;
  color: #718096;
}
.guest-state p { font-size: 16px; margin: 0; }

.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
}

/* ============================================================
   导航模块：分块 + 文字链接行
   ============================================================ */
.nav-sections-wrapper {
  max-width: 1180px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 22px;
  padding-bottom: 40px;
}

.nav-section {
  scroll-margin-top: 16px;
}

.nav-section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
  padding: 0 2px;
}

.nav-section-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
  position: relative;
}

/* 橙色角标（参考图一分块标题左上角小三角） */
.nav-section-corner {
  display: inline-block;
  width: 0;
  height: 0;
  border-style: solid;
  border-width: 7px 0 7px 10px;
  border-color: transparent transparent transparent #f97316;
  transform: rotate(4deg);
  flex-shrink: 0;
  filter: drop-shadow(0 1px 2px rgba(249, 115, 22, 0.35));
}

.nav-section-title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #1a202c;
  letter-spacing: 0.5px;
}

.nav-section-search {
  cursor: pointer;
  transition: color 0.2s;
}
.nav-section-search:hover {
  color: #4299e1;
}

/* 链接行卡片（白色圆角底板，多行文字链接排列） */
.nav-links-card {
  background-color: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 20px 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.03);
  display: flex;
  flex-wrap: wrap;
  gap: 8px 4px;
}

/* 单个链接项：图标 + 文字（一行内多个） */
.nav-link-item {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  border-radius: 8px;
  min-width: 170px;
  transition: background-color 0.15s ease, transform 0.15s ease;
  line-height: 1.2;
}

.nav-link-item.clickable {
  cursor: pointer;
}
.nav-link-item.clickable:hover {
  background-color: #ebf8ff;
  transform: translateY(-1px);
}

.nav-link-item.placeholder {
  color: #a0aec0;
  cursor: default;
  opacity: 0.75;
}

.nav-link-icon {
  font-size: 18px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 22px;
  height: 22px;
}

.nav-link-name {
  font-size: 14px;
  color: #2d3748;
  font-weight: 500;
  white-space: nowrap;
}

.nav-link-item.placeholder .nav-link-name {
  color: #718096;
  font-weight: 400;
}

/* 底部提示 */
.nav-footer-hint {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 20px 0 8px;
  color: #a0aec0;
  font-size: 13px;
  border-top: 1px dashed #cbd5e0;
  margin-top: 12px;
}

/* ============================================================
   我的模块 - 卡片网格
   ============================================================ */
.card-grid-wrapper {
  max-width: 1200px;
  margin: 0 auto;
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
}

.site-card {
  background-color: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-height: 100px;
}
.site-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
  border-color: #4299e1;
}

.card-icon {
  width: 36px;
  height: 36px;
  background-color: #4299e1;
  color: #ffffff;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  font-size: 16px;
}

.card-name {
  font-size: 14px;
  font-weight: 600;
  color: #2d3748;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-url {
  font-size: 12px;
  color: #718096;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-group {
  font-size: 11px;
  color: #a0aec0;
  background-color: #edf2f7;
  padding: 2px 6px;
  border-radius: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* Loading */
.loading-overlay {
  position: absolute;
  inset: 0;
  background-color: rgba(255, 255, 255, 0.8);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
}
.loading-content {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  background-color: #ffffff;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  color: #2d3748;
  font-size: 14px;
}

:deep(mark) {
  background-color: #fef08a;
  color: #2d3748;
  padding: 0 2px;
  border-radius: 2px;
}

.module-header {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e2e8f0;
}
.module-header h2 {
  font-size: 22px;
  font-weight: 700;
  color: #2d3748;
  margin: 0;
}
.module-subtitle {
  font-size: 13px;
  color: #a0aec0;
}

.plaza-blank {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 60vh;
}
</style>
