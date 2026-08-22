<script setup>
import { onMounted, onUnmounted, computed, ref, watch, nextTick } from 'vue'
import { useNavStore } from './stores/nav'
import Sidebar from './components/Sidebar.vue'
import ContentArea from './components/ContentArea.vue'
import AuthDialog from './components/AuthDialog.vue'
import EditDialog from './components/EditDialog.vue'
import ContextMenu from './components/ContextMenu.vue'
import ExportDialog from './components/ExportDialog.vue'

const navStore = useNavStore()

const moduleTabs = [
  { value: '首页', label: '首页', icon: 'Home' },
  { value: '我的', label: '我的', icon: 'User' },
  { value: '导航', label: '导航', icon: 'MapLocation' },
  { value: '广场', label: '广场', icon: 'ChatDotRound' }
]

const currentModule = computed({
  get: () => navStore.activeModule,
  set: (v) => {
    navStore.activeModule = v
    navStore.selectNode(null)
    navStore.clearCheckedLinks()
  }
})

const isHomeModule = computed(() => currentModule.value === '首页')

// ========= 首页专属数据 & 逻辑 =========
const activeHomeLinkId = ref('f1')
let autoRotateTimer = null
let typewriterTimer = null
const cardsMounted = ref(false)

// 生活化 & 大众化站点（替换 GitHub/程序员向内容）
const featuredLinks = [
  // 视频娱乐
  { id: 'f1',  name: '哔哩哔哩',   url: 'https://www.bilibili.com',    tagline: '年轻人的潮流文化娱乐视频社区',         groupPath: '视频娱乐 / 弹幕视频' },
  { id: 'f2',  name: '抖音',       url: 'https://www.douyin.com',       tagline: '记录美好生活的短视频社交平台',         groupPath: '视频娱乐 / 短视频' },
  { id: 'f3',  name: '爱奇艺',     url: 'https://www.iqiyi.com',        tagline: '在线影视综、动漫纪录片一站式观看',     groupPath: '视频娱乐 / 长视频' },
  { id: 'f4',  name: '腾讯视频',   url: 'https://v.qq.com',             tagline: '海量高清正版影视内容随心看',           groupPath: '视频娱乐 / 长视频' },
  // 电商购物
  { id: 'f5',  name: '淘宝',       url: 'https://www.taobao.com',       tagline: '万能的淘宝，好物应有尽有',             groupPath: '电商购物 / 综合' },
  { id: 'f6',  name: '京东',       url: 'https://www.jd.com',           tagline: '多快好省，品质生活放心购',             groupPath: '电商购物 / 综合' },
  { id: 'f7',  name: '什么值得买', url: 'https://www.smzdm.com',         tagline: '折扣爆料与真实评测，网购不花冤枉钱',   groupPath: '电商购物 / 导购' },
  // 社交 & 知识
  { id: 'f8',  name: '微博',       url: 'https://weibo.com',            tagline: '随时随地发现新鲜事与热点话题',         groupPath: '社交资讯 / 微博' },
  { id: 'f9',  name: '知乎',       url: 'https://www.zhihu.com',         tagline: '有问题，就会有答案的知识问答社区',     groupPath: '社交资讯 / 问答' },
  { id: 'f10', name: '小红书',     url: 'https://www.xiaohongshu.com',   tagline: '年轻人的生活方式分享与种草社区',       groupPath: '社交资讯 / 生活分享' },
  // 效率 & 工具
  { id: 'f11', name: 'Canva可画',  url: 'https://www.canva.cn',          tagline: '零基础做海报、PPT、简历的在线设计神器', groupPath: '效率工具 / 设计' },
  { id: 'f12', name: 'WPS在线',    url: 'https://www.kdocs.cn',           tagline: '在线文档、表格、演示多人实时协作',     groupPath: '效率工具 / 办公' }
]

// ========= Hero 背景光点（动态生成） =========
const heroParticles = Array.from({ length: 28 }, (_, i) => ({
  id: i,
  size: 2 + Math.random() * 5,              // 2~7px
  left: Math.random() * 100,                // 0~100%
  top: Math.random() * 100,                 // 0~100%
  delay: Math.random() * 8,                 // 0~8s
  duration: 10 + Math.random() * 10,        // 10~20s
  opacity: 0.3 + Math.random() * 0.5,       // 0.3~0.8
  hue: 190 + Math.random() * 80             // 蓝→紫 (190~270)
}))

// ========= 打字机效果（Showcase 名称 & 网址） =========
const typedName = ref('')
const typedUrl = ref('')
const showCursor = ref(true)
const showcaseKey = ref(0) // 用于强制重新触发扫描条动画

function runTypewriter(name, url) {
  if (typewriterTimer) { clearInterval(typewriterTimer); typewriterTimer = null }
  typedName.value = ''
  typedUrl.value = ''
  let i = 0, j = 0
  typewriterTimer = setInterval(() => {
    if (i < name.length) {
      typedName.value = name.substring(0, i + 1)
      i++
    } else if (i < name.length + 4) {
      i++
    } else if (j < url.length) {
      typedUrl.value = url.substring(0, j + 1)
      j++
    } else {
      clearInterval(typewriterTimer)
      typewriterTimer = null
    }
    showCursor.value = Math.floor(Date.now() / 280) % 2 === 0
  }, 55)
}

// 光标持续闪烁（即使打字完成）
let cursorTimer = null
function startCursorBlink() {
  cursorTimer = setInterval(() => { showCursor.value = !showCursor.value }, 520)
}

// ========= Showcase 自动轮播 =========
function rotateToNext() {
  const list = homeDisplayLinks.value
  if (!list.length) return
  const idx = list.findIndex(l => l.id === activeHomeLinkId.value)
  const next = (idx < 0 || idx >= list.length - 1) ? 0 : idx + 1
  activeHomeLinkId.value = list[next].id
}

function startAutoRotate() {
  stopAutoRotate()
  autoRotateTimer = setInterval(rotateToNext, 7000)
}
function stopAutoRotate() {
  if (autoRotateTimer) { clearInterval(autoRotateTimer); autoRotateTimer = null }
}

const homeLinks = computed(() => {
  if (!navStore.isLoggedIn) return []
  const all = []
  function collect(list) {
    list.forEach(n => {
      if (n.type === 'link') all.push(n)
      else if (n.type === 'group' && n.children) collect(n.children)
    })
  }
  collect(navStore.bookmarkData)
  const kw = navStore.searchKeyword.trim().toLowerCase()
  if (kw) {
    return all.filter(l =>
      l.name.toLowerCase().includes(kw) ||
      l.url.toLowerCase().includes(kw)
    )
  }
  return all
})

const homeDisplayLinks = computed(() => {
  // 首页永远展示公共推荐内容（featuredLinks），不展示用户个人书签
  return featuredLinks
})

const selectedHomeLink = computed(() => {
  return homeDisplayLinks.value.find(link => link.id === activeHomeLinkId.value) || homeDisplayLinks.value[0] || null
})

// selected 变化时重新触发打字机 + 重置轮播间隔 + 重启扫描条
watch(selectedHomeLink, (sel) => {
  if (sel) {
    showcaseKey.value++
    nextTick(() => runTypewriter(sel.name, sel.url.replace(/^https?:\/\//, '')))
    startAutoRotate()
  }
}, { immediate: true })

function handleCardClick(link) {
  window.open(link.url, '_blank', 'noopener noreferrer')
}

function handleHomeCardClick(link) {
  activeHomeLinkId.value = link.id
  startAutoRotate()
}

onMounted(() => {
  navStore.init()
  startCursorBlink()
  startAutoRotate()
  // 延迟一点再给卡片加 stagger 入场动画（确保 DOM 已挂）
  nextTick(() => {
    setTimeout(() => { cardsMounted.value = true }, 80)
  })
})

onUnmounted(() => {
  stopAutoRotate()
  if (typewriterTimer) clearInterval(typewriterTimer)
  if (cursorTimer) clearInterval(cursorTimer)
})
</script>

<template>
  <div class="app-root">
    <!-- Full-width top bar: logo + module nav -->
    <header class="app-topbar">
      <div class="topbar-logo">
        <el-icon :size="22" color="#4299e1"><Compass /></el-icon>
        <span class="logo-text">网站管理工具</span>
      </div>

      <nav class="module-nav">
        <div
          v-for="tab in moduleTabs"
          :key="tab.value"
          class="module-tab"
          :class="{ active: currentModule === tab.value }"
          @click="currentModule = tab.value"
        >
          <el-icon class="module-tab-icon"><component :is="tab.icon" /></el-icon>
          <span class="module-tab-label">{{ tab.label }}</span>
        </div>
      </nav>

      <div class="topbar-right">
        <template v-if="navStore.isLoggedIn">
          <div v-if="currentModule === '导航'" class="topbar-user-static">
            <el-icon><User /></el-icon>
            <span>{{ navStore.currentUser?.username }}</span>
          </div>
          <el-dropdown v-else trigger="click" placement="bottom" @command="(cmd) => {
            if (cmd === 'logout') navStore.logout()
          }">
            <el-button class="topbar-user-btn" size="small" round>
              <el-icon><User /></el-icon>
              <span>{{ navStore.currentUser?.username }}</span>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
      </div>
    </header>

    <!-- 首页：全宽独立布局，无侧边栏、无内容区框架（AI风格左右分栏） -->
    <main v-if="isHomeModule" class="app-home">
      <div class="home-inner">

        <!-- ============== Hero AI 风格（通义灵码式：左文案 右产品大图） ============== -->
        <section class="home-hero ai-hero">
          <!-- 背景光晕 + 光点 -->
          <div class="hero-bg" aria-hidden="true">
            <div class="glow-blob glow-blob-1"></div>
            <div class="glow-blob glow-blob-2"></div>
            <div class="glow-blob glow-blob-3"></div>
            <span
              v-for="p in heroParticles"
              :key="'hp-'+p.id"
              class="hero-dot"
              :style="{
                width: p.size + 'px',
                height: p.size + 'px',
                left: p.left + '%',
                top: p.top + '%',
                opacity: p.opacity,
                animationDelay: p.delay + 's',
                animationDuration: p.duration + 's',
                background: 'hsl(' + p.hue + ', 90%, 72%)',
                boxShadow: '0 0 ' + (p.size * 3) + 'px hsl(' + p.hue + ', 95%, 70%)'
              }"
            ></span>
            <div class="hero-shimmer" aria-hidden="true"></div>
          </div>

          <div class="hero-grid">
            <!-- 左侧文案区 -->
            <div class="hero-copy">
              <div class="hero-eyebrow">
                <el-icon><MagicStick /></el-icon>
                <span>AI 智能导航  ·  精选推荐  ·  高效管理</span>
              </div>
              <h1 class="hero-mega-title">
                <span class="line-1">你的</span>
                <span class="line-1 gradient-text">智能网站导航</span>
                <span class="line-2">一站式书签管理助手</span>
              </h1>
              <p class="hero-desc">
                多层级分组收纳 · 全文实时搜索 · Chrome/Edge 书签一键迁移 · 精选公共推荐导航，
                让你告别收藏夹杂乱，秒级找到想去的网站。
              </p>
              <div class="hero-cta">
                <el-button
                  class="btn-primary"
                  size="large"
                  type="primary"
                  round
                  @click="currentModule = navStore.isLoggedIn ? '我的' : '导航'"
                >
                  <el-icon><Compass /></el-icon>
                  <span>{{ navStore.isLoggedIn ? '去管理我的书签' : '立即开始使用' }}</span>
                </el-button>
                <el-button
                  class="btn-ghost"
                  size="large"
                  round
                  @click="currentModule = '导航'"
                >
                  <el-icon><MapLocation /></el-icon>
                  <span>浏览公共导航广场</span>
                </el-button>
              </div>
              <div class="hero-stats">
                <div class="stat-item">
                  <div class="stat-num">{{ homeDisplayLinks.length }}+</div>
                  <div class="stat-label">精选推荐</div>
                </div>
                <div class="stat-divider"></div>
                <div class="stat-item">
                  <div class="stat-num">4</div>
                  <div class="stat-label">核心能力</div>
                </div>
                <div class="stat-divider"></div>
                <div class="stat-item">
                  <div class="stat-num">99%</div>
                  <div class="stat-label">秒级定位</div>
                </div>
              </div>
            </div>

            <!-- 右侧 AI 搜索面板 Mockup -->
            <div class="hero-visual" aria-hidden="true">
              <div class="ai-panel">
                <div class="ai-panel-header">
                  <div class="ai-avatar">
                    <el-icon :size="20"><MagicStick /></el-icon>
                  </div>
                  <div class="ai-header-text">
                    <div class="ai-title">智能推荐</div>
                    <div class="ai-subtitle">根据你的浏览习惯 · 推荐热门网站</div>
                  </div>
                  <span class="ai-badge-dot"></span>
                </div>
                <!-- AI 搜索框 -->
                <div class="ai-search-box">
                  <el-icon :size="18" color="#9f7aea"><Search /></el-icon>
                  <input
                    class="ai-search-input"
                    value="我想看个视频放松一下"
                    readonly
                  />
                  <div class="ai-search-btn">
                    <el-icon :size="16"><Promotion /></el-icon>
                    <span>搜索</span>
                  </div>
                </div>
                <!-- AI 结果卡片（气泡浮动） -->
                <div class="ai-results">
                  <div class="ai-result-card float-card-a">
                    <div class="arc-icon arc-vid">🎬</div>
                    <div class="arc-body">
                      <div class="arc-name">哔哩哔哩</div>
                      <div class="arc-desc">年轻人的潮流文化视频社区</div>
                    </div>
                    <span class="arc-arrow">→</span>
                  </div>
                  <div class="ai-result-card float-card-b">
                    <div class="arc-icon arc-shop">🛒</div>
                    <div class="arc-body">
                      <div class="arc-name">淘宝 / 京东</div>
                      <div class="arc-desc">万能购物 · 品质生活好物</div>
                    </div>
                    <span class="arc-arrow">→</span>
                  </div>
                  <div class="ai-result-card float-card-c">
                    <div class="arc-icon arc-soc">💬</div>
                    <div class="arc-body">
                      <div class="arc-name">微博 / 知乎</div>
                      <div class="arc-desc">随时随地发现新鲜事与知识</div>
                    </div>
                    <span class="arc-arrow">→</span>
                  </div>
                </div>
                <!-- 扫描条 -->
                <div class="ai-scan-line" aria-hidden="true"></div>
              </div>
              <!-- 悬浮装饰圆环 -->
              <div class="ring-deco ring-1" aria-hidden="true"></div>
              <div class="ring-deco ring-2" aria-hidden="true"></div>
            </div>
          </div>
        </section>

        <!-- ============== 核心能力 4 卡片（通义灵码式大图介绍） ============== -->
        <section class="core-section">
          <div class="section-head">
            <div class="section-eyebrow">
              <el-icon><Collection /></el-icon>
              <span>CORE FEATURES</span>
            </div>
            <h2 class="section-title">
              四大核心能力
              <span class="title-gradient">让网站管理更智能</span>
            </h2>
          </div>

          <div class="core-grid">
            <div class="core-card core-card-1">
              <div class="core-ico-wrap ico-blue">
                <el-icon :size="26"><Collection /></el-icon>
              </div>
              <h3>智能分组管理</h3>
              <p>多层级树形分类，支持拖拽移动、批量重命名，上万书签也能井井有条。</p>
              <div class="core-visual cv-tree" aria-hidden="true">
                <div class="tree-node tn-1"></div>
                <div class="tree-node tn-2"></div>
                <div class="tree-node tn-3"></div>
                <div class="tree-node tn-4"></div>
              </div>
            </div>

            <div class="core-card core-card-2">
              <div class="core-ico-wrap ico-green">
                <el-icon :size="26"><Search /></el-icon>
              </div>
              <h3>实时全文搜索</h3>
              <p>名称、网址、分组、标签全字段匹配，毫秒级响应，一秒直达目标网站。</p>
              <div class="core-visual cv-search" aria-hidden="true">
                <div class="search-bar-mock"></div>
                <div class="search-hit-mock hm-1"></div>
                <div class="search-hit-mock hm-2"></div>
                <div class="search-hit-mock hm-3"></div>
              </div>
            </div>

            <div class="core-card core-card-3">
              <div class="core-ico-wrap ico-orange">
                <el-icon :size="26"><Upload /></el-icon>
              </div>
              <h3>一键迁移导入</h3>
              <p>支持 Chrome / Edge / Firefox HTML 书签文件批量导入导出，无缝切换零负担。</p>
              <div class="core-visual cv-import" aria-hidden="true">
                <div class="import-arrow"></div>
                <div class="import-blob ib-1"></div>
                <div class="import-blob ib-2"></div>
                <div class="import-blob ib-3"></div>
              </div>
            </div>

            <div class="core-card core-card-4">
              <div class="core-ico-wrap ico-purple">
                <el-icon :size="26"><Share /></el-icon>
              </div>
              <h3>公共导航广场</h3>
              <p>精选视频、电商、社交、学习等 12 大类热门公共站点，全网推荐实时更新。</p>
              <div class="core-visual cv-square" aria-hidden="true">
                <div class="sq-tile sq-1"></div>
                <div class="sq-tile sq-2"></div>
                <div class="sq-tile sq-3"></div>
                <div class="sq-tile sq-4"></div>
                <div class="sq-tile sq-5"></div>
                <div class="sq-tile sq-6"></div>
              </div>
            </div>
          </div>
        </section>

        <!-- ============== 精选推荐 12 站点（左小标签+右大标题排版） ============== -->
        <section class="featured-section">
          <div class="section-head">
            <div class="section-eyebrow">
              <el-icon><Star /></el-icon>
              <span>RECOMMENDED</span>
            </div>
            <h2 class="section-title">
              精选常用网站
              <span class="title-gradient">覆盖娱乐/购物/社交/工具</span>
            </h2>
            <p class="section-desc">精心挑选大众最常用的 12 个优质网站，点击可预览详情，一键直达</p>
          </div>

          <div class="home-card-grid">
            <div
              v-for="(link, index) in homeDisplayLinks"
              :key="link.id"
              class="home-site-card"
              :class="{
                active: selectedHomeLink?.id === link.id,
                'card-in': cardsMounted
              }"
              :style="cardsMounted ? { animationDelay: (index * 0.06) + 's' } : {}"
              @click="handleHomeCardClick(link)"
            >
              <div class="card-icon">
                <span class="card-icon-letter">{{ (link.name || '?').charAt(0).toUpperCase() }}</span>
                <span class="card-icon-shine" aria-hidden="true"></span>
              </div>
              <div class="card-name">{{ link.name }}</div>
              <div class="card-url">{{ link.url }}</div>
              <div
                v-if="link.groupPath"
                class="card-group"
                :title="link.groupPath"
              >{{ link.groupPath }}</div>
            </div>
          </div>
        </section>

        <!-- ============== Showcase 场景展示（保留打字机 + 浏览器Mockup） ============== -->
        <section v-if="selectedHomeLink" class="showcase-section">
          <div class="section-head">
            <div class="section-eyebrow">
              <el-icon><View /></el-icon>
              <span>SHOWCASE</span>
            </div>
            <h2 class="section-title">
              场景深度预览
              <span class="title-gradient">{{ selectedHomeLink.name }} · 详情展示</span>
            </h2>
          </div>

          <div class="site-showcase" :key="'sc-'+showcaseKey">
            <div class="showcase-copy">
              <div class="showcase-kicker">{{ selectedHomeLink.groupPath || '精选站点' }}</div>
              <h2 class="showcase-typewriter">
                <span>{{ typedName }}</span>
                <span class="type-cursor" :class="{ blink: showCursor }">|</span>
              </h2>
              <p class="showcase-tagline">{{ selectedHomeLink.tagline || '集中收纳常用入口，减少查找和切换成本。' }}</p>
              <div class="showcase-actions">
                <el-button type="primary" size="large" @click="handleCardClick(selectedHomeLink)">
                  <el-icon><Position /></el-icon>
                  <span>立即打开网站</span>
                </el-button>
                <span class="showcase-url typewriter-url">
                  <span>{{ typedUrl }}</span>
                  <span v-if="!typedUrl" class="type-cursor small" :class="{ blink: showCursor }">_</span>
                </span>
              </div>
            </div>
            <div class="showcase-visual" aria-hidden="true">
              <div class="browser-shell">
                <div class="browser-bar">
                  <span class="dot-r"></span>
                  <span class="dot-y"></span>
                  <span class="dot-g"></span>
                  <div class="browser-address-bar">
                    <span class="lock-icon">🔒</span>
                    <span class="address-text">{{ selectedHomeLink.url.replace(/^https?:\/\//, '') }}</span>
                  </div>
                </div>
                <div class="browser-body">
                  <div class="scan-progress" aria-hidden="true"></div>
                  <div class="preview-line strong"></div>
                  <div class="preview-line"></div>
                  <div class="preview-grid">
                    <div class="preview-tile active">
                      <span>{{ (selectedHomeLink.name || '?').charAt(0).toUpperCase() }}</span>
                      <div class="tile-glow" aria-hidden="true"></div>
                    </div>
                    <div class="preview-tile"></div>
                    <div class="preview-tile"></div>
                  </div>
                  <div class="terminal-panel">
                    <span class="term-prompt">$</span>
                    <span class="term-name">{{ typedName || selectedHomeLink.name }}</span>
                    <span class="term-url">{{ typedUrl || selectedHomeLink.url.replace(/^https?:\/\//, '') }}</span>
                    <span class="term-cursor" :class="{ blink: showCursor }">▊</span>
                  </div>
                </div>
                <div class="browser-glow" aria-hidden="true"></div>
              </div>
            </div>
          </div>
        </section>

        <!-- ============== 底部大 CTA（通义灵码式最终号召） ============== -->
        <section class="final-cta-section">
          <div class="final-cta-card">
            <div class="glow-cta-bg" aria-hidden="true"></div>
            <div class="cta-content">
              <h2 class="cta-title">
                <span>现在就开始管理你的</span>
                <span class="gradient-text">专属网站导航</span>
              </h2>
              <p class="cta-desc">
                {{ navStore.isLoggedIn ? '继续在书签中添加你的专属网站，开启高效浏览之旅！' : '登录后即可创建你的第一个分组，导入 Chrome 书签，完全免费使用' }}
              </p>
              <div class="cta-buttons">
                <el-button
                  size="large"
                  round
                  class="btn-primary"
                  type="primary"
                  @click="currentModule = navStore.isLoggedIn ? '我的' : '导航'"
                >
                  <el-icon><Compass /></el-icon>
                  <span>{{ navStore.isLoggedIn ? '立即管理我的书签' : '免费开始使用' }}</span>
                </el-button>
                <el-button
                  size="large"
                  round
                  class="btn-ghost"
                  @click="currentModule = '广场'"
                >
                  <el-icon><ChatDotRound /></el-icon>
                  <span>去广场看看</span>
                </el-button>
              </div>
            </div>
            <div class="cta-art" aria-hidden="true">
              <div class="cta-blob cta-blob-1"></div>
              <div class="cta-blob cta-blob-2"></div>
            </div>
          </div>
        </section>

      </div>
    </main>

    <!-- 我的 / 导航 / 广场：保持原有 sidebar + content 两栏布局 -->
    <div v-else class="app-main">
      <Sidebar />
      <ContentArea />
    </div>

    <AuthDialog />
    <EditDialog />
    <ContextMenu />
    <ExportDialog />
  </div>
</template>

<style scoped>
/* ================= 全局布局 ================= */
.app-root {
  width: 100vw;
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.app-topbar {
  height: 56px;
  background-color: #1a202c;
  border-bottom: 1px solid #2d3748;
  display: flex;
  align-items: center;
  padding: 0 24px;
  gap: 16px;
  flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
  z-index: 20;
}

.topbar-logo {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}
.logo-text {
  font-size: 18px;
  font-weight: 700;
  color: #ffffff;
}

.module-nav {
  display: flex;
  align-items: center;
  gap: 4px;
  flex: 1;
  justify-content: center;
}

.module-tab {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  cursor: pointer;
  font-size: 16px;
  color: #a0aec0;
  border-radius: 6px;
  transition: color 0.2s, background-color 0.2s;
  user-select: none;
}
.module-tab:hover {
  color: #ffffff;
  background-color: #2d3748;
}
.module-tab.active {
  color: #63b3ed;
  background-color: #2c5282;
  font-weight: 600;
}
.module-tab-icon { font-size: 18px; }
.module-tab-label { line-height: 1; }

.topbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}
.topbar-user-btn {
  background-color: transparent !important;
  border-color: #4a5568 !important;
  color: #e2e8f0 !important;
  padding: 6px 14px !important;
}
.topbar-user-btn:hover {
  background-color: #2d3748 !important;
  color: #ffffff !important;
  border-color: #63b3ed !important;
}
.topbar-user-static {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 30px;
  padding: 5px 14px;
  border: 1px solid #4a5568;
  border-radius: 16px;
  color: #e2e8f0;
  font-size: 13px;
}

/* ====== 两栏布局（我的/导航/广场） ====== */
.app-main {
  flex: 1;
  display: flex;
  overflow: hidden;
}

/* ====== 首页：全宽布局 ====== */
.app-home {
  flex: 1;
  overflow-y: auto;
  background:
    radial-gradient(ellipse 80% 50% at 50% -20%, rgba(66, 153, 225, 0.12), transparent 60%),
    #f8fafc;
}
.app-home::-webkit-scrollbar { width: 10px; }
.app-home::-webkit-scrollbar-track { background: #edf2f7; }
.app-home::-webkit-scrollbar-thumb {
  background: linear-gradient(180deg, #4299e1, #9f7aea);
  border-radius: 5px;
}

.home-inner {
  max-width: 1360px;
  margin: 0 auto;
  padding: 32px 32px 80px;
  display: flex;
  flex-direction: column;
  gap: 96px;
}

/* ============ 通用工具类：渐变文字、section标题 ============ */
.gradient-text {
  background: linear-gradient(90deg, #4299e1 0%, #9f7aea 45%, #ed64a6 100%);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  background-size: 200% 200%;
  animation: gradientShift 8s ease infinite;
}
@keyframes gradientShift {
  0%, 100% { background-position: 0% 50%; }
  50%      { background-position: 100% 50%; }
}

/* 通用 section 头部（通义灵码式：小标签+两行大标题+渐变字） */
.section-head {
  text-align: center;
  margin-bottom: 48px;
}
.section-eyebrow {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 16px;
  border-radius: 999px;
  background: rgba(66, 153, 225, 0.1);
  color: #2b6cb0;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 1.5px;
  margin-bottom: 16px;
  border: 1px solid rgba(66, 153, 225, 0.2);
}
.section-title {
  margin: 0;
  font-size: 34px;
  font-weight: 800;
  color: #0f172a;
  line-height: 1.3;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}
.section-title .title-gradient {
  font-size: 30px;
  font-weight: 700;
}
.section-desc {
  margin: 14px auto 0;
  max-width: 640px;
  color: #64748b;
  font-size: 15px;
  line-height: 1.8;
}

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(24px); }
  to   { opacity: 1; transform: translateY(0); }
}

/* 通用按钮（CTA 大按钮） */
.btn-primary {
  background: linear-gradient(135deg, #3182ce 0%, #667eea 100%) !important;
  border: none !important;
  color: #ffffff !important;
  font-weight: 600;
  padding: 18px 28px !important;
  box-shadow: 0 10px 25px -8px rgba(66, 153, 225, 0.55) !important;
  transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1), box-shadow 0.3s !important;
}
.btn-primary:hover {
  transform: translateY(-3px) !important;
  box-shadow: 0 16px 32px -8px rgba(66, 153, 225, 0.7) !important;
}
.btn-ghost {
  background: rgba(255, 255, 255, 0.15) !important;
  color: #ffffff !important;
  border: 1px solid rgba(255, 255, 255, 0.35) !important;
  backdrop-filter: blur(6px);
  font-weight: 600;
  padding: 17px 26px !important;
  transition: all 0.3s !important;
}
.btn-ghost:hover {
  background: rgba(255, 255, 255, 0.28) !important;
  border-color: #ffffff !important;
  transform: translateY(-2px) !important;
}

/* ========================================================= */
/* ====== 1. Hero AI 左右分栏（通义灵码风格） ====== */
/* ========================================================= */
.ai-hero {
  position: relative;
  border-radius: 28px;
  overflow: hidden;
  color: #ffffff;
  background:
    linear-gradient(135deg, #1e3a8a 0%, #2b6cb0 30%, #4c51bf 65%, #553c9a 100%);
  padding: 64px 56px;
  margin: 0;
}

/* 巨型模糊光晕 Blob（灵码风格） */
.hero-bg {
  position: absolute;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
  z-index: 0;
}
.glow-blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(60px);
  opacity: 0.55;
  will-change: transform;
  animation: blobFloat 18s ease-in-out infinite;
}
.glow-blob-1 {
  width: 380px; height: 380px;
  background: #60a5fa;
  top: -120px; left: -80px;
}
.glow-blob-2 {
  width: 420px; height: 420px;
  background: #a78bfa;
  bottom: -140px; right: -100px;
  animation-delay: -6s;
}
.glow-blob-3 {
  width: 280px; height: 280px;
  background: #ec4899;
  top: 40%; left: 50%;
  opacity: 0.28;
  animation-delay: -12s;
}
@keyframes blobFloat {
  0%, 100% { transform: translate(0, 0) scale(1); }
  33%      { transform: translate(30px, -20px) scale(1.08); }
  66%      { transform: translate(-20px, 25px) scale(0.94); }
}

/* 光点粒子 */
.hero-dot {
  position: absolute;
  border-radius: 50%;
  transform: translateZ(0);
  animation: dotFloat ease-in-out infinite, dotPulse ease-in-out infinite;
  will-change: transform, opacity;
}
@keyframes dotFloat {
  0%   { transform: translate(0, 0) scale(1); }
  25%  { transform: translate(24px, -18px) scale(1.15); }
  50%  { transform: translate(-12px, -36px) scale(0.9); }
  75%  { transform: translate(-28px, -10px) scale(1.1); }
  100% { transform: translate(0, 0) scale(1); }
}
@keyframes dotPulse {
  0%, 100% { filter: brightness(0.85); }
  50%      { filter: brightness(1.3); }
}

/* 横向流动的扫光带（斜向划过） */
.hero-shimmer {
  position: absolute;
  inset: -10%;
  background: linear-gradient(
    115deg,
    transparent 30%,
    rgba(255, 255, 255, 0.08) 48%,
    rgba(255, 255, 255, 0.22) 50%,
    rgba(255, 255, 255, 0.08) 52%,
    transparent 70%
  );
  transform: translateX(-40%) skewX(-18deg);
  animation: shimmerMove 9s cubic-bezier(0.4, 0, 0.2, 1) infinite;
}
@keyframes shimmerMove {
  0%   { transform: translateX(-60%) skewX(-18deg); opacity: 0; }
  10%  { opacity: 1; }
  60%  { transform: translateX(60%) skewX(-18deg); opacity: 1; }
  70%  { opacity: 0; }
  100% { transform: translateX(60%) skewX(-18deg); opacity: 0; }
}

/* Hero Grid：左右分栏 */
.hero-grid {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: 1.1fr 1fr;
  gap: 64px;
  align-items: center;
}

/* 左：文案区 */
.hero-copy {
  display: flex;
  flex-direction: column;
  gap: 24px;
  animation: fadeInUp 0.8s ease both;
}
.hero-eyebrow {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 18px;
  background: rgba(255, 255, 255, 0.15);
  border: 1px solid rgba(255, 255, 255, 0.25);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  border-radius: 999px;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.5px;
  align-self: flex-start;
  color: rgba(255, 255, 255, 0.92);
}

/* 巨型标题（三行式，第一行+渐变字+第二行） */
.hero-mega-title {
  margin: 0;
  font-size: 54px;
  font-weight: 900;
  line-height: 1.12;
  letter-spacing: -0.5px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.hero-mega-title .line-1 {
  display: inline-flex;
  gap: 12px;
  align-items: baseline;
}
.hero-mega-title .line-2 {
  font-size: 46px;
  font-weight: 800;
  color: #e2e8f0;
}

.hero-desc {
  margin: 0;
  font-size: 17px;
  line-height: 1.85;
  color: rgba(255, 255, 255, 0.82);
  max-width: 560px;
}

.hero-cta {
  display: flex;
  gap: 16px;
  margin-top: 8px;
  flex-wrap: wrap;
}

/* 信任数据 */
.hero-stats {
  display: flex;
  align-items: center;
  gap: 28px;
  margin-top: 12px;
  padding-top: 24px;
  border-top: 1px solid rgba(255, 255, 255, 0.15);
  max-width: 520px;
}
.stat-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.stat-num {
  font-size: 28px;
  font-weight: 800;
  background: linear-gradient(135deg, #fff, #c4b5fd);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  line-height: 1;
}
.stat-label {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.72);
}
.stat-divider {
  width: 1px;
  height: 36px;
  background: rgba(255, 255, 255, 0.18);
}

/* 右：AI 产品视觉 Mockup 面板 */
.hero-visual {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  animation: fadeInUp 0.9s ease 0.15s both;
}

/* 装饰圆环 */
.ring-deco {
  position: absolute;
  border-radius: 50%;
  border: 1px dashed rgba(255, 255, 255, 0.25);
  pointer-events: none;
}
.ring-1 {
  width: 120%; height: 120%;
  animation: ringSpin 20s linear infinite;
}
.ring-2 {
  width: 92%; height: 92%;
  border-color: rgba(196, 181, 253, 0.4);
  animation: ringSpin 14s linear infinite reverse;
}
@keyframes ringSpin {
  to { transform: rotate(360deg); }
}

/* AI 面板主卡片 */
.ai-panel {
  width: 100%;
  max-width: 460px;
  background: rgba(255, 255, 255, 0.96);
  border-radius: 20px;
  padding: 22px 20px 26px;
  box-shadow:
    0 30px 70px -12px rgba(15, 23, 42, 0.35),
    0 0 0 1px rgba(255, 255, 255, 0.6) inset;
  position: relative;
  overflow: hidden;
  animation: panelFloat 6s ease-in-out infinite;
}
@keyframes panelFloat {
  0%, 100% { transform: translateY(0) rotateX(2deg); }
  50%      { transform: translateY(-10px) rotateX(2deg); }
}

.ai-panel-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 18px;
}
.ai-avatar {
  width: 40px; height: 40px;
  border-radius: 12px;
  background: linear-gradient(135deg, #4299e1, #9f7aea);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 6px 14px rgba(159, 122, 234, 0.4);
}
.ai-header-text { flex: 1; }
.ai-title {
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
}
.ai-subtitle {
  font-size: 12px;
  color: #64748b;
  margin-top: 2px;
}
.ai-badge-dot {
  width: 8px; height: 8px;
  border-radius: 50%;
  background: #48bb78;
  box-shadow: 0 0 0 4px rgba(72, 187, 120, 0.25);
  animation: pulseDot 1.8s ease-in-out infinite;
}
@keyframes pulseDot {
  0%, 100% { box-shadow: 0 0 0 2px rgba(72, 187, 120, 0.25); }
  50%      { box-shadow: 0 0 0 7px rgba(72, 187, 120, 0.08); }
}

/* 搜索框 */
.ai-search-box {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 10px 10px 14px;
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  margin-bottom: 18px;
}
.ai-search-input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  font-size: 14px;
  color: #334155;
  font-family: inherit;
}
.ai-search-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 8px 14px;
  background: linear-gradient(135deg, #4299e1, #667eea);
  color: #fff;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 600;
  box-shadow: 0 4px 10px rgba(66, 153, 225, 0.35);
}

/* 搜索结果浮动卡片 */
.ai-results {
  display: flex;
  flex-direction: column;
  gap: 10px;
  position: relative;
}
.ai-result-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.05);
  transition: transform 0.3s;
  position: relative;
}
.float-card-a { animation: resultFloatA 5s ease-in-out infinite; }
.float-card-b { animation: resultFloatB 5s ease-in-out infinite; animation-delay: 0.3s; }
.float-card-c { animation: resultFloatC 5s ease-in-out infinite; animation-delay: 0.6s; }
@keyframes resultFloatA {
  0%, 100% { transform: translateX(0); }
  50%      { transform: translateX(4px); }
}
@keyframes resultFloatB {
  0%, 100% { transform: translateX(0); }
  50%      { transform: translateX(-4px); }
}
@keyframes resultFloatC {
  0%, 100% { transform: translateX(0); }
  50%      { transform: translateX(6px); }
}

.arc-icon {
  width: 36px; height: 36px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
}
.arc-vid  { background: #fef3c7; }
.arc-shop { background: #fee2e2; }
.arc-soc  { background: #dbeafe; }
.arc-body { flex: 1; min-width: 0; }
.arc-name {
  font-size: 13px;
  font-weight: 700;
  color: #0f172a;
}
.arc-desc {
  font-size: 11px;
  color: #64748b;
  margin-top: 2px;
}
.arc-arrow {
  color: #94a3b8;
  font-weight: 700;
  transition: transform 0.3s, color 0.3s;
}
.ai-result-card:hover {
  border-color: #4299e1;
  transform: translateY(-2px);
}
.ai-result-card:hover .arc-arrow {
  color: #4299e1;
  transform: translateX(4px);
}

/* AI 面板扫描线 */
.ai-scan-line {
  position: absolute;
  left: 0; top: 0; right: 0;
  height: 2px;
  background: linear-gradient(90deg, transparent, #4299e1, #9f7aea, transparent);
  box-shadow: 0 0 12px rgba(66, 153, 225, 0.7);
  animation: aiScan 3s ease-in-out infinite;
}
@keyframes aiScan {
  0%   { top: 0; opacity: 0; }
  15%  { opacity: 1; }
  85%  { opacity: 1; }
  100% { top: 100%; opacity: 0; }
}

/* ========================================================= */
/* ====== 2. 核心能力 4 卡片 ====== */
/* ========================================================= */
.core-section {
  scroll-margin-top: 40px;
}
.core-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 22px;
}
.core-card {
  position: relative;
  padding: 28px 24px 24px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 20px;
  box-shadow: 0 2px 10px rgba(15, 23, 42, 0.04);
  transition: transform 0.4s cubic-bezier(0.34, 1.56, 0.64, 1),
              box-shadow 0.4s, border-color 0.3s;
  overflow: hidden;
  animation: fadeInUp 0.7s ease both;
}
.core-card::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(66, 153, 225, 0.04), transparent 60%);
  opacity: 0;
  transition: opacity 0.4s;
  pointer-events: none;
}
.core-card:hover {
  transform: translateY(-8px);
  border-color: #4299e1;
  box-shadow: 0 20px 40px -12px rgba(66, 153, 225, 0.25);
}
.core-card:hover::before { opacity: 1; }
.core-card-1 { animation-delay: 0.05s; }
.core-card-2 { animation-delay: 0.15s; }
.core-card-3 { animation-delay: 0.25s; }
.core-card-4 { animation-delay: 0.35s; }

.core-ico-wrap {
  width: 54px; height: 54px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ffffff;
  margin-bottom: 16px;
  box-shadow: 0 8px 18px rgba(0, 0, 0, 0.12);
  position: relative;
  overflow: hidden;
  transition: transform 0.4s;
}
.core-card:hover .core-ico-wrap {
  transform: scale(1.08) rotate(-4deg);
}
.ico-blue   { background: linear-gradient(135deg, #3182ce, #63b3ed); }
.ico-green  { background: linear-gradient(135deg, #38a169, #68d391); }
.ico-orange { background: linear-gradient(135deg, #dd6b20, #f6ad55); }
.ico-purple { background: linear-gradient(135deg, #6b46c1, #b794f4); }

.core-card h3 {
  margin: 0 0 8px;
  font-size: 18px;
  font-weight: 800;
  color: #0f172a;
}
.core-card p {
  margin: 0;
  font-size: 13.5px;
  line-height: 1.75;
  color: #64748b;
}

/* 卡片底部的装饰性视觉 */
.core-visual {
  margin-top: 18px;
  height: 80px;
  border-radius: 12px;
  background: #f8fafc;
  border: 1px dashed #e2e8f0;
  position: relative;
  overflow: hidden;
}

/* 视觉1：树形结构 */
.cv-tree { }
.tree-node {
  position: absolute;
  background: linear-gradient(135deg, #4299e1, #63b3ed);
  border-radius: 6px;
  box-shadow: 0 3px 8px rgba(66, 153, 225, 0.3);
}
.tn-1 { top: 14px; left: 50%; transform: translateX(-50%); width: 60%; height: 18px; }
.tn-2 { top: 44px; left: 10%; width: 34%; height: 14px; background: linear-gradient(135deg, #667eea, #b794f4); }
.tn-3 { top: 44px; left: 56%; width: 34%; height: 14px; background: linear-gradient(135deg, #667eea, #b794f4); }
.tn-4 { top: 64px; left: 20%; width: 22%; height: 10px; background: #cbd5e1; }

/* 视觉2：搜索结果 */
.cv-search { padding: 12px; }
.search-bar-mock {
  height: 18px;
  background: linear-gradient(90deg, #4299e1, #63b3ed);
  border-radius: 6px;
  width: 70%;
  margin: 2px auto 12px;
}
.search-hit-mock {
  height: 12px;
  border-radius: 4px;
  background: #e2e8f0;
  margin-bottom: 6px;
}
.hm-1 { width: 88%; }
.hm-2 { width: 60%; margin-left: 8px; background: #dbeafe; }
.hm-3 { width: 75%; margin-left: 8px; }

/* 视觉3：导入箭头 */
.cv-import { display: flex; align-items: center; justify-content: center; }
.import-arrow {
  position: absolute;
  top: 50%; left: 30%;
  width: 40%;
  height: 4px;
  background: linear-gradient(90deg, transparent, #dd6b20, transparent);
  transform: translateY(-50%);
  animation: importSlide 2.2s ease-in-out infinite;
}
@keyframes importSlide {
  0%, 100% { left: 20%; opacity: 0; }
  50%      { left: 45%; opacity: 1; }
}
.import-blob {
  position: absolute;
  border-radius: 10px;
}
.ib-1 { top: 26px; left: 16px; width: 36px; height: 36px; background: #f6ad55; animation: blobPulse 2s ease-in-out infinite; }
.ib-2 { top: 22px; right: 22px; width: 44px; height: 44px; background: linear-gradient(135deg, #ed8936, #f6e05e); }
.ib-3 { top: 52px; right: 60px; width: 30px; height: 30px; background: #68d391; }
@keyframes blobPulse {
  0%, 100% { transform: scale(1); }
  50%      { transform: scale(1.12); }
}

/* 视觉4：方格 */
.cv-square {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 6px;
  padding: 12px;
}
.sq-tile {
  aspect-ratio: 1 / 0.6;
  border-radius: 6px;
  background: #e2e8f0;
  animation: sqFlash 3.5s ease-in-out infinite;
}
.sq-1 { background: linear-gradient(135deg, #667eea, #9f7aea); animation-delay: 0s; }
.sq-2 { animation-delay: 0.4s; }
.sq-3 { background: linear-gradient(135deg, #4299e1, #63b3ed); animation-delay: 0.8s; }
.sq-4 { animation-delay: 1.2s; }
.sq-5 { background: linear-gradient(135deg, #ed64a6, #f687b3); animation-delay: 1.6s; }
.sq-6 { animation-delay: 2s; }
@keyframes sqFlash {
  0%, 100% { opacity: 0.45; }
  50%      { opacity: 1; }
}

/* ========================================================= */
/* ====== 3. 精选推荐卡片 ====== */
/* ========================================================= */
.featured-section { scroll-margin-top: 40px; }

.home-card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 18px;
}

.home-site-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  padding: 18px 16px;
  cursor: pointer;
  transition: transform 0.35s cubic-bezier(0.34, 1.56, 0.64, 1),
              box-shadow 0.35s, border-color 0.3s, background 0.3s;
  display: flex;
  flex-direction: column;
  gap: 6px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  position: relative;
  overflow: hidden;
  opacity: 0;
  transform: translateY(28px) scale(0.92);
}
.home-site-card.card-in {
  animation: cardIn 0.6s cubic-bezier(0.34, 1.56, 0.64, 1) forwards;
}
@keyframes cardIn {
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}
.home-site-card::after {
  content: '';
  position: absolute;
  top: 0; left: -60%;
  width: 50%; height: 100%;
  background: linear-gradient(
    120deg,
    transparent,
    rgba(66, 153, 225, 0.12),
    transparent
  );
  transform: skewX(-20deg);
  transition: left 0.7s ease;
}
.home-site-card:hover {
  transform: translateY(-6px) scale(1.025);
  border-color: #4299e1;
  box-shadow: 0 14px 32px rgba(66, 153, 225, 0.18),
              0 4px 10px rgba(0, 0, 0, 0.06);
}
.home-site-card:hover::after { left: 130%; }
.home-site-card.active {
  border-color: #4299e1;
  background: linear-gradient(180deg, #ebf8ff 0%, #ffffff 65%);
  box-shadow: 0 6px 20px rgba(66, 153, 225, 0.22),
              inset 0 0 0 1px rgba(66, 153, 225, 0.15);
}

.card-icon {
  width: 44px; height: 44px;
  border-radius: 12px;
  background: linear-gradient(135deg, #4299e1, #667eea);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 4px;
  position: relative;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(66, 153, 225, 0.35);
}
.card-icon-letter {
  font-size: 19px;
  font-weight: 700;
  position: relative;
  z-index: 1;
}
.card-icon-shine {
  position: absolute;
  top: -40%; left: -40%;
  width: 50%; height: 180%;
  background: linear-gradient(
    110deg,
    transparent 40%,
    rgba(255, 255, 255, 0.55) 50%,
    transparent 60%
  );
  transform: rotate(20deg);
  animation: iconShine 3.2s ease-in-out infinite;
}
@keyframes iconShine {
  0%   { transform: translateX(0) rotate(20deg); opacity: 0; }
  15%  { opacity: 1; }
  55%  { transform: translateX(240%) rotate(20deg); opacity: 1; }
  70%  { opacity: 0; }
  100% { transform: translateX(240%) rotate(20deg); opacity: 0; }
}

.card-name {
  font-size: 15px;
  font-weight: 600;
  color: #1a202c;
  line-height: 1.3;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.card-url {
  font-size: 12px;
  color: #94a3b8;
  line-height: 1.3;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-family: 'Consolas', 'Monaco', monospace;
}
.card-group {
  margin-top: 4px;
  padding: 2px 8px;
  background: #eff6ff;
  color: #2b6cb0;
  font-size: 11px;
  border-radius: 4px;
  align-self: flex-start;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 100%;
  font-weight: 600;
}

/* ========================================================= */
/* ====== 4. Showcase 场景深度预览（保留打字机+浏览器） ====== */
/* ========================================================= */
.showcase-section { scroll-margin-top: 40px; }

.site-showcase {
  display: grid;
  grid-template-columns: 1fr 1.2fr;
  gap: 32px;
  align-items: center;
  padding: 48px 44px;
  border-radius: 24px;
  background-color: #ffffff;
  border: 1px solid rgba(66, 153, 225, 0.18);
  background-image:
    linear-gradient(135deg, rgba(66, 153, 225, 0.05) 0%, rgba(159, 122, 234, 0.04) 100%),
    linear-gradient(90deg, rgba(66, 153, 225, 0.06) 1px, transparent 1px),
    linear-gradient(rgba(66, 153, 225, 0.06) 1px, transparent 1px);
  background-size: auto, 32px 32px, 32px 32px;
  animation: showcaseIn 0.65s cubic-bezier(0.34, 1.56, 0.64, 1) both;
  position: relative;
  overflow: hidden;
}
@keyframes showcaseIn {
  from { opacity: 0; transform: translateY(20px) scale(0.98); }
  to   { opacity: 1; transform: translateY(0) scale(1); }
}

.showcase-copy {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.showcase-kicker {
  display: inline-block;
  align-self: flex-start;
  padding: 6px 14px;
  background: rgba(66, 153, 225, 0.12);
  color: #2b6cb0;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  animation: fadeInUp 0.5s ease both;
}

.showcase-typewriter {
  margin: 0;
  font-size: 40px;
  font-weight: 900;
  color: #0f172a;
  letter-spacing: -0.5px;
  min-height: 52px;
  line-height: 1.2;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
}
.type-cursor {
  display: inline-block;
  margin-left: 3px;
  color: #4299e1;
  font-weight: 400;
  line-height: 1;
  opacity: 1;
}
.type-cursor.small { font-size: 12px; }
.type-cursor.blink { animation: cursorBlink 1s steps(2, start) infinite; }
@keyframes cursorBlink {
  to { visibility: hidden; }
}

.showcase-tagline {
  margin: 0;
  color: #64748b;
  font-size: 15.5px;
  line-height: 1.9;
  animation: fadeInUp 0.55s ease 0.15s both;
}

.showcase-actions {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 8px;
  animation: fadeInUp 0.55s ease 0.25s both;
  flex-wrap: wrap;
}
.showcase-actions .el-button--primary {
  padding: 14px 22px !important;
  font-weight: 600;
  box-shadow: 0 8px 18px -8px rgba(66, 153, 225, 0.6);
}
.showcase-url {
  color: #64748b;
  font-size: 13px;
  font-family: 'Consolas', 'Monaco', monospace;
}
.typewriter-url {
  padding: 5px 12px;
  background: #f1f5f9;
  border-radius: 6px;
  border: 1px dashed #cbd5e0;
}

.showcase-visual {
  display: flex;
  justify-content: center;
  align-items: center;
}

/* 浏览器 mockup */
.browser-shell {
  width: 100%;
  max-width: 540px;
  background: #0f172a;
  border-radius: 18px;
  overflow: hidden;
  position: relative;
  box-shadow:
    0 30px 70px -16px rgba(15, 23, 42, 0.55),
    0 8px 24px rgba(66, 153, 225, 0.2);
  transform: perspective(1200px) rotateY(-4deg);
  transition: transform 0.6s cubic-bezier(0.34, 1.56, 0.64, 1);
  animation: browserFloat 7s ease-in-out infinite;
}
.browser-shell:hover {
  transform: perspective(1200px) rotateY(0) scale(1.03);
}
@keyframes browserFloat {
  0%, 100% { transform: perspective(1200px) rotateY(-4deg) translateY(0); }
  50%      { transform: perspective(1200px) rotateY(-4deg) translateY(-8px); }
}

/* 浏览器外发光 */
.browser-glow {
  position: absolute;
  inset: -2px;
  border-radius: 20px;
  padding: 2px;
  background: linear-gradient(
    135deg,
    rgba(66, 153, 225, 0.7),
    rgba(159, 122, 234, 0.7),
    rgba(236, 72, 153, 0.5),
    rgba(66, 153, 225, 0.7)
  );
  background-size: 300% 300%;
  animation: borderGlow 6s ease infinite;
  z-index: -1;
  -webkit-mask:
    linear-gradient(#fff 0 0) content-box,
    linear-gradient(#fff 0 0);
  -webkit-mask-composite: xor;
          mask-composite: exclude;
  pointer-events: none;
  opacity: 0.85;
}
@keyframes borderGlow {
  0%   { background-position: 0% 50%; }
  50%  { background-position: 100% 50%; }
  100% { background-position: 0% 50%; }
}

.browser-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px 16px;
  background: #1e293b;
  border-bottom: 1px solid rgba(255,255,255,0.05);
}
.browser-bar span[class^="dot-"] {
  width: 12px; height: 12px;
  border-radius: 50%;
  flex-shrink: 0;
  transition: transform 0.2s;
}
.browser-bar .dot-r { background: #ef4444; }
.browser-bar .dot-y { background: #f59e0b; }
.browser-bar .dot-g { background: #22c55e; }
.browser-shell:hover .dot-r,
.browser-shell:hover .dot-y,
.browser-shell:hover .dot-g { transform: scale(1.2); }

.browser-address-bar {
  margin-left: 10px;
  flex: 1;
  height: 28px;
  background: #0f172a;
  border-radius: 8px;
  padding: 0 12px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 11px;
  color: #94a3b8;
  font-family: 'Consolas', monospace;
  overflow: hidden;
  border: 1px solid rgba(255,255,255,0.06);
}
.lock-icon { font-size: 10px; }
.address-text {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.browser-body {
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  position: relative;
}

/* 扫描进度条 */
.scan-progress {
  position: absolute;
  left: 0; top: 0;
  height: 2px;
  width: 0;
  background: linear-gradient(90deg,
    #4299e1, #9f7aea, #ed64a6, #48bb78, #4299e1
  );
  background-size: 200% 100%;
  box-shadow: 0 0 10px rgba(66, 153, 225, 0.8);
  animation: scanMove 2.2s cubic-bezier(0.65, 0, 0.35, 1) forwards,
             scanHue 1.2s linear infinite;
  z-index: 2;
}
@keyframes scanMove {
  0%   { width: 0; opacity: 1; }
  70%  { width: 100%; opacity: 1; }
  100% { width: 100%; opacity: 0; }
}
@keyframes scanHue {
  0%   { background-position: 0% 0; }
  100% { background-position: 200% 0; }
}

.preview-line {
  height: 10px;
  border-radius: 5px;
  background: #334155;
  width: 85%;
  position: relative;
  overflow: hidden;
}
.preview-line::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg,
    transparent 0%,
    rgba(255,255,255,0.1) 50%,
    transparent 100%
  );
  background-size: 200% 100%;
  animation: lineShimmer 2.4s ease-in-out infinite;
}
@keyframes lineShimmer {
  0%   { background-position: 100% 0; }
  100% { background-position: -100% 0; }
}
.preview-line.strong {
  height: 14px;
  width: 55%;
  background: linear-gradient(90deg, #4299e1, #63b3ed);
}

.preview-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  margin-top: 4px;
}
.preview-tile {
  aspect-ratio: 1 / 1;
  background: #1e293b;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #64748b;
  font-weight: 700;
  font-size: 28px;
  position: relative;
  overflow: hidden;
  transition: transform 0.3s;
}
.preview-tile:hover { transform: scale(1.04); }
.preview-tile.active {
  background: linear-gradient(135deg, #48bb78 0%, #4299e1 100%);
  color: #ffffff;
  box-shadow: 0 6px 20px rgba(72, 187, 120, 0.4);
  animation: tilePulse 3s ease-in-out infinite;
}
@keyframes tilePulse {
  0%, 100% { box-shadow: 0 6px 20px rgba(72, 187, 120, 0.4); }
  50% {
    box-shadow: 0 6px 28px rgba(72, 187, 120, 0.6),
                0 0 0 3px rgba(72, 187, 120, 0.22);
  }
}
.tile-glow {
  position: absolute;
  inset: -40%;
  background: conic-gradient(
    from 0deg,
    transparent,
    rgba(255,255,255,0.35),
    transparent 40%
  );
  animation: tileGlowSpin 4s linear infinite;
}
@keyframes tileGlowSpin {
  to { transform: rotate(360deg); }
}

/* Terminal 面板 */
.terminal-panel {
  margin-top: 8px;
  padding: 12px 14px;
  background: #020617;
  border-radius: 10px;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 12px;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px 8px;
  color: #4ade80;
  position: relative;
  overflow: hidden;
  border: 1px solid rgba(255,255,255,0.05);
}
.terminal-panel::before {
  content: '';
  position: absolute;
  top: 0; left: 0; right: 0;
  height: 100%;
  background: repeating-linear-gradient(
    to bottom,
    rgba(255,255,255,0.03) 0,
    rgba(255,255,255,0.03) 1px,
    transparent 1px,
    transparent 3px
  );
  pointer-events: none;
}
.term-prompt { color: #c084fc; font-weight: 700; }
.term-name   { color: #fbbf24; }
.term-url    { color: #7dd3fc; word-break: break-all; }
.term-cursor {
  color: #4ade80;
  font-size: 10px;
  display: inline-block;
  margin-left: 1px;
}
.term-cursor.blink { animation: cursorBlink 1s steps(2, start) infinite; }

/* ========================================================= */
/* ====== 5. 底部大 CTA（通义灵码式最终号召卡） ====== */
/* ========================================================= */
.final-cta-section { scroll-margin-top: 40px; }
.final-cta-card {
  position: relative;
  padding: 72px 64px;
  border-radius: 28px;
  overflow: hidden;
  background: linear-gradient(135deg, #1e3a8a 0%, #4338ca 50%, #5b21b6 100%);
  color: #ffffff;
  display: grid;
  grid-template-columns: 1.4fr 1fr;
  gap: 48px;
  align-items: center;
}
.glow-cta-bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background:
    radial-gradient(circle at 20% 30%, rgba(96, 165, 250, 0.35), transparent 50%),
    radial-gradient(circle at 80% 70%, rgba(236, 72, 153, 0.3), transparent 55%);
}
.cta-content { position: relative; z-index: 1; }
.cta-title {
  margin: 0 0 16px;
  font-size: 38px;
  font-weight: 900;
  line-height: 1.25;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.cta-desc {
  margin: 0 0 28px;
  font-size: 16px;
  line-height: 1.85;
  color: rgba(255, 255, 255, 0.82);
  max-width: 560px;
}
.cta-buttons {
  display: flex;
  gap: 14px;
  flex-wrap: wrap;
}
.final-cta-card .btn-ghost {
  background: rgba(255, 255, 255, 0.12) !important;
  padding: 17px 26px !important;
}

.cta-art {
  position: relative;
  height: 200px;
  z-index: 1;
}
.cta-blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(30px);
  opacity: 0.6;
  animation: blobFloat 14s ease-in-out infinite;
}
.cta-blob-1 {
  width: 220px; height: 220px;
  background: #60a5fa;
  top: 10%; right: 10%;
}
.cta-blob-2 {
  width: 160px; height: 160px;
  background: #ec4899;
  bottom: 5%; left: 15%;
  animation-delay: -6s;
}

/* ========================================================= */
/* ====== 响应式适配 ====== */
/* ========================================================= */
@media (max-width: 1200px) {
  .core-grid { grid-template-columns: repeat(2, 1fr); }
  .hero-mega-title { font-size: 44px; }
  .hero-mega-title .line-2 { font-size: 38px; }
}

@media (max-width: 960px) {
  .home-inner {
    padding: 24px 20px 64px;
    gap: 72px;
  }

  /* Hero 堆叠 */
  .ai-hero { padding: 48px 28px; }
  .hero-grid {
    grid-template-columns: 1fr;
    gap: 40px;
  }
  .hero-mega-title { font-size: 38px; }
  .hero-mega-title .line-2 { font-size: 32px; }
  .hero-cta .btn-primary,
  .hero-cta .btn-ghost { padding: 14px 22px !important; }

  /* Showcase */
  .site-showcase {
    grid-template-columns: 1fr;
    padding: 28px 22px;
  }
  .showcase-typewriter { font-size: 30px; min-height: 40px; }

  .browser-shell {
    transform: none;
    animation: none;
    max-width: 480px;
    margin: 0 auto;
  }
  .browser-shell:hover { transform: none; }

  /* Final CTA */
  .final-cta-card {
    grid-template-columns: 1fr;
    padding: 56px 32px;
  }
  .cta-title { font-size: 30px; }
  .cta-art { height: 140px; max-width: 300px; margin: 0 auto; width: 100%; }

  /* Section 标题缩小 */
  .section-title { font-size: 28px; }
  .section-title .title-gradient { font-size: 24px; }
}

@media (max-width: 600px) {
  .core-grid { grid-template-columns: 1fr; }
  .hero-stats { gap: 18px; }
  .stat-num { font-size: 22px; }
  .home-card-grid { grid-template-columns: repeat(2, 1fr); }
}
</style>
