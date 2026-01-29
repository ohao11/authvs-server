<template>
  <div class="login-wrapper">
    <div class="login-container">
      <div class="login-header">
      <h1>AuthVs 登录</h1>
    </div>

    <div v-if="error" class="alert alert-error">
      {{ error }}
    </div>

    <form @submit.prevent="handleSubmit">
      <div v-if="step === 'login'">
        <input type="hidden" v-model="form.captchaKey" />
        <div class="form-group">
          <label for="username">用户名</label>
          <input
            id="username"
            v-model="form.username"
            class="form-control"
            type="text"
            placeholder="请输入用户名"
            required
            autofocus
          />
        </div>
        <div class="form-group">
          <label for="password">密码</label>
          <input
            id="password"
            v-model="form.password"
            class="form-control"
            type="password"
            placeholder="请输入密码"
            required
          />
        </div>
        <div class="form-group">
          <label for="captcha">验证码</label>
          <div class="captcha-row">
            <input
              id="captcha"
              v-model="form.captcha"
              class="form-control"
              type="text"
              placeholder="请输入验证码"
              required
            />
            <img
              v-if="captchaImg"
              :src="captchaImg"
              class="captcha-img"
              alt="验证码"
              title="点击刷新验证码"
              @click="refreshCaptcha"
            />
          </div>
        </div>
      </div>

      <div v-else-if="step === 'select'">
        <div class="form-group text-center">
          <label class="select-label">请选择验证方式</label>
        </div>
        <div class="mfa-options-list">
          <div 
            v-for="opt in mfaOptions" 
            :key="opt.type" 
            class="mfa-option-item"
            @click="selectMfaOption(opt)"
          >
            <div class="mfa-icon">
              <span v-if="opt.type === 'TOTP'">🔐</span>
              <span v-else-if="opt.type === 'SMS'">📱</span>
              <span v-else-if="opt.type === 'EMAIL'">📧</span>
              <span v-else>🛡️</span>
            </div>
            <div class="mfa-info">
              <div class="mfa-label">{{ opt.label }}</div>
              <div v-if="opt.target" class="mfa-target">{{ opt.target }}</div>
            </div>
            <div class="mfa-arrow">›</div>
          </div>
        </div>
      </div>

      <div v-else-if="step === 'mfa'">
        <div class="form-group">
          <label for="mfaCode">{{ mfaLabel }}</label>
          <div class="input-group">
            <input
              id="mfaCode"
              v-model="form.mfaCode"
              class="form-control"
              type="text"
              placeholder="请输入6位验证码"
              maxlength="6"
              required
              autofocus
            />
            <button 
              v-if="selectedMfaType !== 'TOTP' && countdown > 0" 
              type="button" 
              class="btn-sm btn-disabled"
              disabled
            >
              {{ countdown }}s
            </button>
            <button 
              v-else-if="selectedMfaType !== 'TOTP'" 
              type="button" 
              class="btn-sm"
              @click="sendMfaCode"
              :disabled="sending"
            >
              {{ sending ? '发送中...' : '重新发送' }}
            </button>
          </div>
          <div class="hint">{{ mfaHint }}</div>
        </div>
      </div>

      <button v-if="step !== 'select'" class="btn" type="submit" :disabled="submitting">
        {{ submitting ? (step === 'login' ? "登录中..." : "验证中...") : (step === 'login' ? "登 录" : "验 证") }}
      </button>
      
      <div v-if="step === 'mfa' && mfaOptions.length > 1" class="text-center mt-2">
        <a href="#" @click.prevent="backToSelect">切换验证方式</a>
      </div>

      <div v-if="step === 'mfa' || step === 'select'" class="text-center mt-2">
        <a href="#" @click.prevent="resetLogin">返回登录</a>
      </div>
    </form>
  </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const step = ref('login') // login, select, mfa
const mfaId = ref('')
const mfaOptions = ref([])
const selectedMfaType = ref('')
const mfaLabel = ref('')
const mfaHint = ref('')
const countdown = ref(0)
const sending = ref(false)
let timer = null

const form = reactive({
  username: '',
  password: '',
  captcha: '',
  captchaKey: '',
  mfaCode: ''
})

const captchaImg = ref('')
const error = ref('')
const submitting = ref(false)

async function refreshCaptcha(clearError = true) {
  if (clearError !== false) {
    error.value = ''
  }
  try {
    const res = await fetch('/api/captcha', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ type: 'login' })
    })
    const data = await res.json()
    if (data.code === 200 && data.data) {
      form.captchaKey = data.data.key
      captchaImg.value = data.data.image
      form.captcha = ''
    } else {
      error.value = data.message || '验证码加载失败'
    }
  } catch (e) {
    error.value = '验证码加载失败，请稍后重试'
    console.error(e)
  }
}

function resetLogin() {
  step.value = 'login'
  form.password = ''
  form.captcha = ''
  form.mfaCode = ''
  error.value = ''
  mfaOptions.value = []
  selectedMfaType.value = ''
  clearInterval(timer)
  countdown.value = 0
  refreshCaptcha()
}

function backToSelect() {
  step.value = 'select'
  error.value = ''
  form.mfaCode = ''
  selectedMfaType.value = ''
  clearInterval(timer)
  countdown.value = 0
}

async function handleLogin() {
  const res = await fetch('/api/login', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      username: form.username,
      password: form.password,
      captcha: form.captcha,
      captchaKey: form.captchaKey
    })
  })
  const data = await res.json()
  if (data.code === 200) {
    const { state, token, redirectUrl, mfaId: id, options } = data.data
    
    if (state === 'MFA_REQUIRED') {
      mfaId.value = id
      error.value = ''
      
      if (options && options.length > 0) {
        if (options.length === 1) {
          selectMfaOption(options[0])
        } else {
          step.value = 'select'
          mfaOptions.value = options
        }
      } else {
         // Fallback default
         selectMfaOption({ type: 'TOTP', label: 'Google 验证器' })
      }
    } else {
      handleSuccess(token, redirectUrl)
    }
  } else {
    error.value = data.message || '登录失败'
    await refreshCaptcha(false)
  }
}

function selectMfaOption(option) {
  selectedMfaType.value = option.type
  step.value = 'mfa'
  form.mfaCode = ''
  error.value = ''
  
  if (option.type === 'TOTP') {
    mfaLabel.value = 'Google 验证码'
    mfaHint.value = '请输入您 Google Authenticator 中的 6 位数字'
  } else if (option.type === 'SMS') {
    mfaLabel.value = '短信验证码'
    mfaHint.value = `验证码已发送至 ${option.target}`
    sendMfaCode()
  } else if (option.type === 'EMAIL') {
    mfaLabel.value = '邮箱验证码'
    mfaHint.value = `验证码已发送至 ${option.target}`
    sendMfaCode()
  }
}

async function sendMfaCode() {
  if (sending.value || countdown.value > 0) return
  
  sending.value = true
  try {
    const res = await fetch('/api/login/mfa/send', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ mfaId: mfaId.value, type: selectedMfaType.value })
    })
    const data = await res.json()
    if (data.code === 200) {
      startCountdown()
    } else {
      error.value = data.message || '验证码发送失败'
    }
  } catch(e) {
    error.value = '发送请求失败'
  } finally {
    sending.value = false
  }
}

function startCountdown() {
  countdown.value = 60
  clearInterval(timer)
  timer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      clearInterval(timer)
    }
  }, 1000)
}

async function handleMfa() {
  const res = await fetch('/api/login/mfa', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      mfaId: mfaId.value,
      code: form.mfaCode,
      type: selectedMfaType.value
    })
  })
  const data = await res.json()
  if (data.code === 200) {
    const { token, redirectUrl } = data.data
    handleSuccess(token, redirectUrl)
  } else {
    error.value = data.message || '验证失败'
  }
}

function handleSuccess(token, redirectUrl) {
  if (token) {
    localStorage.setItem('auth_token', token)
  }
  
  if (redirectUrl) {
    window.location.href = redirectUrl
  } else {
    router.push('/profile')
  }
}

async function handleSubmit() {
  error.value = ''
  submitting.value = true
  try {
    if (step.value === 'login') {
      await handleLogin()
    } else {
      await handleMfa()
    }
  } catch (e) {
    error.value = '请求失败，请稍后重试'
    console.error(e)
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  if (localStorage.getItem('auth_token')) {
    router.push('/profile')
    return
  }
  refreshCaptcha()
})
</script>

<style scoped>
.login-wrapper {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f0f2f5;
}

.login-container {
  background-color: #ffffff;
  padding: 40px;
  border-radius: 8px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  width: 100%;
  max-width: 400px;
}

.login-header {
  text-align: center;
  margin-bottom: 30px;
}

.login-header h1 {
  margin: 0;
  color: #333333;
  font-size: 24px;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  color: #666666;
}

.form-control {
  width: 100%;
  padding: 10px;
  border: 1px solid #dddddd;
  border-radius: 4px;
  box-sizing: border-box;
  font-size: 16px;
}

.form-control:focus {
  border-color: #4a90e2;
  outline: none;
  box-shadow: 0 0 0 2px rgba(74, 144, 226, 0.2);
}

.captcha-row {
  display: flex;
  gap: 10px;
  align-items: center;
}

.captcha-img {
  cursor: pointer;
  height: 40px;
  border-radius: 4px;
}

.btn {
  width: 100%;
  padding: 12px;
  background-color: #4a90e2;
  color: #ffffff;
  border: none;
  border-radius: 4px;
  font-size: 16px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.btn:hover {
  background-color: #357abd;
}

.btn:disabled {
  background-color: #a0c4e8;
  cursor: not-allowed;
}

.alert {
  padding: 10px;
  border-radius: 4px;
  margin-bottom: 20px;
  font-size: 14px;
}

.alert-error {
  background-color: #fde8e8;
  color: #c53030;
  border: 1px solid #f8b4b4;
}

.hint {
  font-size: 12px;
  color: #888;
  margin-top: 5px;
}

.text-center {
  text-align: center;
}

.mt-2 {
  margin-top: 10px;
}

a {
  color: #4a90e2;
  text-decoration: none;
  font-size: 14px;
}

a:hover {
  text-decoration: underline;
}

.select-label {
  font-size: 16px;
  color: #333;
  font-weight: 500;
}

.mfa-options-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 20px;
}

.mfa-option-item {
  display: flex;
  align-items: center;
  padding: 15px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  background-color: #f9f9f9;
}

.mfa-option-item:hover {
  background-color: #fff;
  border-color: #4a90e2;
  box-shadow: 0 2px 8px rgba(74, 144, 226, 0.15);
  transform: translateY(-1px);
}

.mfa-icon {
  font-size: 24px;
  margin-right: 15px;
  width: 32px;
  text-align: center;
}

.mfa-info {
  flex: 1;
}

.mfa-label {
  font-weight: 500;
  color: #333;
  margin-bottom: 2px;
}

.mfa-target {
  font-size: 12px;
  color: #888;
}

.mfa-arrow {
  font-size: 20px;
  color: #ccc;
  font-weight: bold;
}

.input-group {
  display: flex;
  gap: 12px;
  align-items: center;
}

.input-group .form-control {
  flex: 1;
  width: auto;
}

.btn-sm {
  padding: 0 16px;
  height: 42px; /* Match form-control height */
  background-color: transparent;
  color: #4a90e2;
  border: 1px solid #4a90e2;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 100px;
}

.btn-sm:hover:not(:disabled) {
  background-color: #f0f9ff;
  box-shadow: 0 2px 4px rgba(74, 144, 226, 0.1);
}

.btn-sm:active:not(:disabled) {
  transform: translateY(1px);
}

.btn-sm:disabled, .btn-disabled {
  color: #999;
  border-color: #ddd;
  background-color: #f5f5f5;
  cursor: not-allowed;
  box-shadow: none;
}
</style>
