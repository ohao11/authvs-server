<template>
  <div class="container">
    <div class="header">
      <h1>用户详情</h1>
      <button @click="handleLogout" class="btn-logout">退出登录</button>
    </div>

    <div v-if="loading" class="text-center">
      正在加载用户信息...
    </div>

    <div v-else-if="error" class="text-center">
      {{ error }}
    </div>

    <div v-else-if="user" class="profile-info">
      <div class="label">用户ID</div>
      <div class="value">{{ user.id }}</div>

      <div class="label">用户名</div>
      <div class="value">{{ user.username }}</div>

      <div class="label">邮箱</div>
      <div class="value">{{ user.email || '未设置' }}</div>

      <div class="label">手机号</div>
      <div class="value">{{ user.phone || '未设置' }}</div>

      <div class="label">用户类型</div>
      <div class="value">{{ userTypeText }}</div>

      <div class="label">状态</div>
      <div class="value">
        <span
          class="status-badge"
          :class="user.enabled === 1 ? 'status-active' : 'status-inactive'"
        >
          {{ user.enabled === 1 ? '启用' : '禁用' }}
        </span>
      </div>

      <div class="label">双因子认证</div>
      <div class="value totp-row">
        <span
          class="status-badge"
          :class="user.totpEnabled === 1 ? 'status-active' : 'status-inactive'"
        >
          {{ user.totpEnabled === 1 ? '已开启' : '未开启' }}
        </span>
        <button 
          v-if="user.totpEnabled !== 1" 
          @click="startEnableTotp" 
          class="btn-sm btn-primary"
        >
          开启
        </button>
        <button 
          v-else 
          @click="startDisableTotp" 
          class="btn-sm btn-danger"
        >
          关闭
        </button>
      </div>

      <div class="label">注册时间</div>
      <div class="value">{{ user.createdAt || '' }}</div>
    </div>

    <!-- Enable TOTP Modal -->
    <div v-if="showEnableModal" class="modal-overlay">
      <div class="modal">
        <h3>开启双因子认证</h3>
        <div class="modal-content">
           <p>1. 请使用 Google Authenticator 扫描下方二维码</p>
           <div class="qr-container">
             <img v-if="qrCodeUrl" :src="qrCodeUrl" alt="QR Code" />
           </div>
           <p class="secret-text">或手动输入密钥: <code class="secret-code">{{ totpSecret }}</code></p>
           
           <p>2. 输入6位验证码</p>
           <input 
             v-model="totpCode" 
             class="form-control" 
             placeholder="6位验证码" 
             maxlength="6" 
             @keyup.enter="confirmEnableTotp"
           />
           <div v-if="modalError" class="error-msg">{{ modalError }}</div>
           
           <div class="modal-actions">
             <button @click="confirmEnableTotp" class="btn-primary" :disabled="modalLoading">
               {{ modalLoading ? '验证中...' : '验证并开启' }}
             </button>
             <button @click="closeEnableModal" class="btn-secondary" :disabled="modalLoading">取消</button>
           </div>
        </div>
      </div>
    </div>

    <!-- Disable TOTP Modal -->
    <div v-if="showDisableModal" class="modal-overlay">
      <div class="modal">
        <h3>关闭双因子认证</h3>
        <div class="modal-content">
          <p>为了您的账号安全，请输入登录密码进行确认。</p>
          <input 
            type="password" 
            v-model="password" 
            class="form-control" 
            placeholder="请输入密码" 
            @keyup.enter="confirmDisableTotp"
          />
          <div v-if="modalError" class="error-msg">{{ modalError }}</div>

          <div class="modal-actions">
               <button @click="confirmDisableTotp" class="btn-danger" :disabled="modalLoading">
                 {{ modalLoading ? '处理中...' : '确认关闭' }}
               </button>
               <button @click="closeDisableModal" class="btn-secondary" :disabled="modalLoading">取消</button>
          </div>
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
.container {
  max-width: 600px;
  margin: 40px auto;
  padding: 20px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
}

.header h1 {
  margin: 0;
  font-size: 24px;
}

.btn-logout {
  padding: 8px 16px;
  background-color: #f56c6c;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.btn-logout:hover {
  background-color: #f78989;
}

.profile-info {
  display: grid;
  grid-template-columns: 100px 1fr;
  gap: 15px;
  align-items: center;
}

.label {
  font-weight: bold;
  color: #606266;
  text-align: right;
}

.value {
  color: #303133;
}

.status-badge {
  display: inline-block;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
}

.status-active {
  background-color: #f0f9eb;
  color: #67c23a;
}

.status-inactive {
  background-color: #f4f4f5;
  color: #909399;
}

.text-center {
  text-align: center;
}

.totp-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.btn-sm {
  padding: 4px 10px;
  font-size: 12px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.btn-primary {
  background-color: #409eff;
  color: white;
  border: none;
  border-radius: 4px;
  padding: 8px 16px;
  cursor: pointer;
}
.btn-primary:hover {
  background-color: #66b1ff;
}
.btn-primary:disabled {
  background-color: #a0cfff;
  cursor: not-allowed;
}

.btn-secondary {
  background-color: #909399;
  color: white;
  border: none;
  border-radius: 4px;
  padding: 8px 16px;
  cursor: pointer;
}
.btn-secondary:hover {
  background-color: #a6a9ad;
}

.btn-danger {
  background-color: #f56c6c;
  color: white;
  border: none;
  border-radius: 4px;
  padding: 8px 16px; /* Added standard padding for consistency in modals */
  cursor: pointer;
}
.btn-danger:hover {
  background-color: #f78989;
}

/* Modal Styles */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal {
  background: white;
  padding: 20px;
  border-radius: 8px;
  width: 90%;
  max-width: 400px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.modal h3 {
  margin-top: 0;
  text-align: center;
}

.modal-content {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.qr-container {
  text-align: center;
  margin: 10px 0;
}

.qr-container img {
  width: 150px;
  height: 150px;
}

.secret-text {
  font-size: 12px;
  color: #606266;
  text-align: center;
}

.secret-code {
  background: #f4f4f5;
  padding: 2px 4px;
  border-radius: 4px;
  color: #e6a23c;
  font-weight: bold;
}

.form-control {
  width: 100%;
  padding: 8px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  box-sizing: border-box;
}

.modal-actions {
  display: flex;
  justify-content: center;
  gap: 15px;
  margin-top: 10px;
}

.error-msg {
  color: #f56c6c;
  font-size: 12px;
  text-align: center;
}
</style>
