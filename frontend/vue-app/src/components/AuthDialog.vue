<script setup>
import { ref, computed, watch } from 'vue'
import { useNavStore } from '../stores/nav'

const navStore = useNavStore()

const loginForm = ref({ username: '', password: '' })
const registerForm = ref({ username: '', password: '', confirmPassword: '' })

const activeTab = computed({
  get: () => navStore.authDialog.mode,
  set: (val) => { navStore.authDialog.mode = val }
})

const isLoginMode = computed(() => activeTab.value === 'login')

watch(() => navStore.authDialog.visible, (visible) => {
  if (visible) {
    loginForm.value = { username: '', password: '' }
    registerForm.value = { username: '', password: '', confirmPassword: '' }
  }
})

async function handleSubmit() {
  if (isLoginMode.value) {
    if (!loginForm.value.username || !loginForm.value.password) return
    await navStore.submitAuth(loginForm.value)
  } else {
    if (!registerForm.value.username || !registerForm.value.password) return
    if (registerForm.value.password !== registerForm.value.confirmPassword) {
      navStore.authError = '两次输入的密码不一致'
      return
    }
    await navStore.submitAuth(registerForm.value)
  }
}

function handleClose() {
  navStore.closeAuthDialog()
}
</script>

<template>
  <el-dialog
    v-model="navStore.authDialog.visible"
    :title="isLoginMode ? '登录' : '注册'"
    width="400px"
    :close-on-click-modal="true"
    @close="handleClose"
    destroy-on-close
  >
    <el-tabs v-model="activeTab">
      <el-tab-pane label="登录" name="login" />
      <el-tab-pane label="注册" name="register" />
    </el-tabs>

    <div v-if="navStore.authError" class="auth-error">
      <el-icon><WarningFilled /></el-icon>
      {{ navStore.authError }}
    </div>

    <!-- Login Form -->
    <el-form
      v-if="isLoginMode"
      :model="loginForm"
      label-width="80px"
      style="margin-top: 16px"
    >
      <el-form-item label="用户名">
        <el-input v-model="loginForm.username" placeholder="请输入用户名" autocomplete="username" />
      </el-form-item>
      <el-form-item label="密码">
        <el-input
          v-model="loginForm.password"
          type="password"
          placeholder="请输入密码"
          autocomplete="current-password"
          show-password
          @keyup.enter="handleSubmit"
        />
      </el-form-item>
    </el-form>

    <!-- Register Form -->
    <el-form
      v-else
      :model="registerForm"
      label-width="80px"
      style="margin-top: 16px"
    >
      <el-form-item label="用户名">
        <el-input v-model="registerForm.username" placeholder="3-20 个字符" autocomplete="username" />
      </el-form-item>
      <el-form-item label="密码">
        <el-input
          v-model="registerForm.password"
          type="password"
          placeholder="6-20 个字符"
          autocomplete="new-password"
          show-password
        />
      </el-form-item>
      <el-form-item label="确认密码">
        <el-input
          v-model="registerForm.confirmPassword"
          type="password"
          placeholder="请再次输入密码"
          autocomplete="new-password"
          show-password
          @keyup.enter="handleSubmit"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" :loading="navStore.authLoading" @click="handleSubmit">
        {{ navStore.authLoading ? '处理中...' : (isLoginMode ? '登录' : '注册') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.auth-error {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  background-color: #fef0f0;
  color: #f56c6c;
  border-radius: 4px;
  font-size: 13px;
  margin-bottom: 12px;
}
</style>
