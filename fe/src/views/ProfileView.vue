<template>
  <div class="portal-layout">
    <!-- Navbar -->
    <header class="navbar">
      <div class="container navbar-content">
        <div class="brand">
          <span class="logo-icon">🛡️</span>
          <span class="brand-name">AuthVs Portal</span>
        </div>
        <div class="user-menu" v-if="user">
          <div class="user-info">
            <span class="avatar">{{ user.username?.charAt(0).toUpperCase() }}</span>
            <span class="username">{{ user.username }}</span>
          </div>
          <button @click="handleLogout" class="btn-logout-nav">退出</button>
        </div>
      </div>
    </header>

    <!-- Main Content -->
    <main class="main-content container">
      <div v-if="loading" class="loading-state">
        <div class="spinner"></div>
        <p>正在加载用户信息...</p>
      </div>

      <div v-else-if="error" class="error-state">
        <p>{{ error }}</p>
        <button @click="loadUser" class="btn-retry">重试</button>
      </div>

      <div v-else-if="user" class="content-wrapper">
        <div class="page-header">
          <h1>个人中心</h1>
          <p class="subtitle">管理您的账户信息和安全设置</p>
        </div>

        <div class="cards-grid">
          <!-- User Profile Card -->
          <div class="card profile-card">
            <div class="card-header">
              <h3>基本资料</h3>
            </div>
            <div class="card-body">
              <div class="info-row">
                <label>用户ID</label>
                <div class="value">{{ user.id }}</div>
              </div>
              <div class="info-row">
                <label>用户名</label>
                <div class="value">{{ user.username }}</div>
              </div>
              <div class="info-row">
                <label>用户类型</label>
                <div class="value">{{ userTypeText }}</div>
              </div>
              <div class="info-row">
                <label>注册时间</label>
                <div class="value">{{ user.createdAt || '-' }}</div>
              </div>
              <div class="info-row">
                <label>邮箱</label>
                <div class="value">{{ user.email || '未绑定' }}</div>
              </div>
              <div class="info-row">
                <label>手机号</label>
                <div class="value">{{ user.phone || '未绑定' }}</div>
              </div>
            </div>
          </div>

          <!-- Security Card -->
          <div class="card security-card">
            <div class="card-header">
              <h3>安全设置</h3>
            </div>
            <div class="card-body">
              <div class="security-item">
                <div class="sec-icon">🔒</div>
                <div class="sec-info">
                  <div class="sec-title">账号状态</div>
                  <div class="sec-desc">当前账号状态正常</div>
                </div>
                <div class="sec-action">
                   <span class="badge" :class="user.enabled === 1 ? 'badge-success' : 'badge-danger'">
                     {{ user.enabled === 1 ? '正常' : '禁用' }}
                   </span>
                </div>
              </div>
              
              <div class="divider"></div>

              <div class="security-item">
                <div class="sec-icon">🔐</div>
                <div class="sec-info">
                  <div class="sec-title">双因子认证 (2FA)</div>
                  <div class="sec-desc">绑定 Google Authenticator 提高安全性</div>
                </div>
                <div class="sec-action">
                  <button 
                    v-if="user.totpEnabled !== 1" 
                    @click="startEnableTotp" 
                    class="btn btn-primary btn-sm"
                  >
                    开启
                  </button>
                  <button 
                    v-else 
                    @click="startDisableTotp" 
                    class="btn btn-danger btn-sm"
                  >
                    关闭
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>

    <!-- Enable TOTP Modal -->
    <div v-if="showEnableModal" class="modal-overlay">
      <div class="modal">
        <div class="modal-header">
          <h3>开启双因子认证</h3>
          <button class="close-btn" @click="closeEnableModal">×</button>
        </div>
        <div class="modal-body">
           <div class="step">
             <span class="step-num">1</span>
             <p>请使用 Google Authenticator 扫描下方二维码</p>
           </div>
           <div class="qr-container">
             <img v-if="qrCodeUrl" :src="qrCodeUrl" alt="QR Code" />
           </div>
           <p class="secret-text">或手动输入密钥: <code class="secret-code">{{ totpSecret }}</code></p>
           
           <div class="step mt-4">
             <span class="step-num">2</span>
             <p>输入6位验证码以确认</p>
           </div>
           <input 
             v-model="totpCode" 
             class="form-control" 
             placeholder="6位验证码" 
             maxlength="6" 
             @keyup.enter="confirmEnableTotp"
           />
           <div v-if="modalError" class="error-msg">{{ modalError }}</div>
        </div>
        <div class="modal-footer">
           <button @click="closeEnableModal" class="btn btn-text" :disabled="modalLoading">取消</button>
           <button @click="confirmEnableTotp" class="btn btn-primary" :disabled="modalLoading">
             {{ modalLoading ? '验证中...' : '验证并开启' }}
           </button>
        </div>
      </div>
    </div>

    <!-- Disable TOTP Modal -->
    <div v-if="showDisableModal" class="modal-overlay">
      <div class="modal">
        <div class="modal-header">
          <h3>关闭双因子认证</h3>
          <button class="close-btn" @click="closeDisableModal">×</button>
        </div>
        <div class="modal-body">
          <p class="confirm-text">为了您的账号安全，请输入登录密码进行确认。</p>
          <input 
            type="password" 
            v-model="password" 
            class="form-control" 
            placeholder="请输入密码" 
            @keyup.enter="confirmDisableTotp"
          />
          <div v-if="modalError" class="error-msg">{{ modalError }}</div>
        </div>
        <div class="modal-footer">
             <button @click="closeDisableModal" class="btn btn-text" :disabled="modalLoading">取消</button>
             <button @click="confirmDisableTotp" class="btn btn-danger" :disabled="modalLoading">
               {{ modalLoading ? '处理中...' : '确认关闭' }}
             </button>
        </div>
      </div>
    </div>

  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import QRCode from 'qrcode'

const router = useRouter()
const user = ref(null)
const loading = ref(true)
const error = ref('')

// Modal State
const showEnableModal = ref(false)
const showDisableModal = ref(false)
const modalLoading = ref(false)
const modalError = ref('')

// TOTP Setup Data
const qrCodeUrl = ref('')
const totpSecret = ref('')
const totpCode = ref('')

// Disable Data
const password = ref('')

const userTypeText = computed(() => {
  if (!user.value) return '未知'
  if (user.value.userType === 1) return '门户用户'
  if (user.value.userType === 2) return '后台管理员'
  return '未知'
})

async function loadUser() {
  loading.value = true
  error.value = ''
  try {
    const res = await fetch('/api/user/profile', {
      headers: {
        'X-Auth-Token': localStorage.getItem('auth_token') || ''
      }
    })

    if (res.status === 401) {
      localStorage.removeItem('auth_token')
      router.push('/login')
      return
    }

    const data = await res.json()
    if (data.code === 200 && data.data) {
      user.value = data.data
    } else {
      error.value = data.message || '未找到用户信息'
    }
  } catch (e) {
    error.value = '加载失败'
    console.error(e)
  } finally {
    loading.value = false
  }
}

async function handleLogout() {
  localStorage.removeItem('auth_token')
  try {
    await fetch('/api/logout', { method: 'POST' })
  } catch (e) {
    console.error(e)
  }
  router.push('/login')
}

// Enable TOTP Logic
async function startEnableTotp() {
  modalError.value = ''
  totpCode.value = ''
  modalLoading.value = true
  
  try {
    const res = await fetch('/api/totp/setup', {
      headers: { 'X-Auth-Token': localStorage.getItem('auth_token') || '' }
    })
    const data = await res.json()
    if (data.code === 200) {
      totpSecret.value = data.data.secret
      // Generate QR Code
      qrCodeUrl.value = await QRCode.toDataURL(data.data.otpAuthUrl)
      showEnableModal.value = true
    } else {
      alert(data.message || '获取密钥失败')
    }
  } catch (e) {
    console.error(e)
    alert('请求失败')
  } finally {
    modalLoading.value = false
  }
}

function closeEnableModal() {
  showEnableModal.value = false
  qrCodeUrl.value = ''
  totpSecret.value = ''
  totpCode.value = ''
}

async function confirmEnableTotp() {
  if (!totpCode.value || totpCode.value.length !== 6) {
    modalError.value = '请输入6位验证码'
    return
  }
  
  modalLoading.value = true
  modalError.value = ''
  
  try {
    const res = await fetch('/api/totp/enable', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Auth-Token': localStorage.getItem('auth_token') || ''
      },
      body: JSON.stringify({
        secret: totpSecret.value,
        code: totpCode.value
      })
    })
    const data = await res.json()
    if (data.code === 200) {
      alert('双因子认证已开启')
      closeEnableModal()
      loadUser() // Refresh user info
    } else {
      modalError.value = data.message || '验证失败'
    }
  } catch (e) {
    modalError.value = '请求失败'
  } finally {
    modalLoading.value = false
  }
}

// Disable TOTP Logic
function startDisableTotp() {
  modalError.value = ''
  password.value = ''
  showDisableModal.value = true
}

function closeDisableModal() {
  showDisableModal.value = false
  password.value = ''
}

async function confirmDisableTotp() {
  if (!password.value) {
    modalError.value = '请输入密码'
    return
  }
  
  modalLoading.value = true
  modalError.value = ''
  
  try {
    const res = await fetch('/api/totp/disable', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Auth-Token': localStorage.getItem('auth_token') || ''
      },
      body: JSON.stringify({
        password: password.value
      })
    })
    const data = await res.json()
    if (data.code === 200) {
      alert('双因子认证已关闭')
      closeDisableModal()
      loadUser() // Refresh user info
    } else {
      modalError.value = data.message || '操作失败'
    }
  } catch (e) {
    modalError.value = '请求失败'
  } finally {
    modalLoading.value = false
  }
}

onMounted(() => {
  loadUser()
})
</script>

<style scoped>
.portal-layout {
  min-height: 100vh;
  background-color: #f5f7fa;
  display: flex;
  flex-direction: column;
}

.navbar {
  background-color: #fff;
  border-bottom: 1px solid #e5e7eb;
  padding: 0;
  height: 64px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.05);
  position: sticky;
  top: 0;
  z-index: 100;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
  width: 100%;
  box-sizing: border-box;
}

.navbar-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 100%;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 20px;
  font-weight: 600;
  color: #333;
}

.logo-icon {
  font-size: 24px;
}

.user-menu {
  display: flex;
  align-items: center;
  gap: 20px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.avatar {
  width: 36px;
  height: 36px;
  background: linear-gradient(135deg, #4a90e2 0%, #357abd 100%);
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 16px;
}

.username {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.btn-logout-nav {
  padding: 6px 16px;
  border: 1px solid #e0e0e0;
  background: white;
  border-radius: 6px;
  color: #666;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-logout-nav:hover {
  background: #f5f5f5;
  color: #333;
  border-color: #d0d0d0;
}

.main-content {
  flex: 1;
  padding-top: 40px;
  padding-bottom: 60px;
}

.page-header {
  margin-bottom: 30px;
}

.page-header h1 {
  font-size: 28px;
  color: #1a1a1a;
  margin: 0 0 8px 0;
}

.subtitle {
  color: #666;
  font-size: 15px;
  margin: 0;
}

.cards-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(400px, 1fr));
  gap: 24px;
}

.card {
  background: white;
  border-radius: 12px;
  border: 1px solid #e5e7eb;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
  overflow: hidden;
  transition: box-shadow 0.2s;
}

.card:hover {
  box-shadow: 0 8px 24px rgba(0,0,0,0.08);
}

.card-header {
  padding: 20px 24px;
  border-bottom: 1px solid #f0f0f0;
  background: #fafafa;
}

.card-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.card-body {
  padding: 24px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  padding: 12px 0;
  border-bottom: 1px solid #f5f5f5;
}

.info-row:last-child {
  border-bottom: none;
}

.info-row label {
  color: #888;
  font-size: 14px;
}

.info-row .value {
  color: #333;
  font-weight: 500;
  font-size: 14px;
}

.security-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 8px 0;
}

.sec-icon {
  font-size: 24px;
  width: 40px;
  height: 40px;
  background: #f0f7ff;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.sec-info {
  flex: 1;
}

.sec-title {
  font-size: 15px;
  font-weight: 600;
  color: #333;
  margin-bottom: 4px;
}

.sec-desc {
  font-size: 13px;
  color: #888;
}

.badge {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
}

.badge-success {
  background: #e6f7ff;
  color: #1890ff;
}

.badge-danger {
  background: #fff1f0;
  color: #f5222d;
}

.divider {
  height: 1px;
  background: #f0f0f0;
  margin: 20px 0;
}

.btn-sm {
  padding: 6px 16px;
  font-size: 13px;
}

/* Modal styles */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.6);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
  backdrop-filter: blur(4px);
}

.modal {
  background: white;
  border-radius: 16px;
  width: 90%;
  max-width: 480px;
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.2);
  display: flex;
  flex-direction: column;
  max-height: 90vh;
  animation: slideIn 0.3s ease;
}

@keyframes slideIn {
  from { transform: translateY(20px); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}

.modal-header {
  padding: 20px 24px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.modal-header h3 {
  margin: 0;
  font-size: 18px;
  color: #333;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  color: #999;
  cursor: pointer;
  padding: 0;
  line-height: 1;
}

.close-btn:hover {
  color: #333;
}

.modal-body {
  padding: 24px;
  overflow-y: auto;
}

.modal-footer {
  padding: 20px 24px;
  border-top: 1px solid #f0f0f0;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  background: #fafafa;
  border-radius: 0 0 16px 16px;
}

/* Loading & Error States */
.loading-state, .error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  color: #666;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #4a90e2;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 15px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.btn-retry {
  margin-top: 10px;
  padding: 8px 20px;
  background-color: #4a90e2;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

/* TOTP Setup specific */
.step {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.step-num {
  width: 24px;
  height: 24px;
  background: #4a90e2;
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: bold;
}

.step p {
  margin: 0;
  font-size: 14px;
  color: #333;
}

.qr-container {
  text-align: center;
  margin: 15px 0;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 12px;
  border: 1px solid #eee;
}

.qr-container img {
  width: 180px;
  height: 180px;
  display: block;
  margin: 0 auto;
}

.secret-text {
  font-size: 13px;
  color: #666;
  text-align: center;
  margin-bottom: 20px;
}

.secret-code {
  background: #fff7e6;
  padding: 4px 8px;
  border-radius: 4px;
  color: #d46b08;
  font-weight: bold;
  font-family: monospace;
  font-size: 14px;
  border: 1px solid #ffd591;
}

.form-control {
  width: 100%;
  padding: 12px;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  box-sizing: border-box;
  font-size: 16px;
  transition: border-color 0.2s;
}

.form-control:focus {
  border-color: #4a90e2;
  outline: none;
  box-shadow: 0 0 0 3px rgba(74, 144, 226, 0.1);
}

.btn {
  padding: 10px 20px;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
  border: none;
  font-weight: 500;
  transition: all 0.2s;
}

.btn-primary {
  background-color: #4a90e2;
  color: white;
}

.btn-primary:hover {
  background-color: #357abd;
  box-shadow: 0 2px 6px rgba(74, 144, 226, 0.25);
}

.btn-danger {
  background-color: #ff4d4f;
  color: white;
}

.btn-danger:hover {
  background-color: #ff2a2d;
  box-shadow: 0 2px 6px rgba(255, 77, 79, 0.25);
}

.btn-text {
  background: none;
  color: #666;
}

.btn-text:hover {
  background-color: #f5f5f5;
  color: #333;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.error-msg {
  color: #ff4d4f;
  font-size: 13px;
  margin-top: 8px;
}
</style>
