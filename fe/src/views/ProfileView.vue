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

      <div class="label">注册时间</div>
      <div class="value">{{ user.createdAt || '' }}</div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const user = ref(null)
const loading = ref(true)
const error = ref('')

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
    await fetch('/logout', { method: 'POST' })
  } catch (e) {
    console.error(e)
  }
  router.push('/login')
}

onMounted(() => {
  loadUser()
})
</script>

<style scoped>
.container {
  background-color: #ffffff;
  padding: 40px;
  border-radius: 8px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  width: 100%;
  max-width: 600px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
  border-bottom: 1px solid #eeeeee;
  padding-bottom: 20px;
}

.header h1 {
  margin: 0;
  color: #333333;
  font-size: 24px;
}

.profile-info {
  display: grid;
  grid-template-columns: 120px 1fr;
  gap: 20px;
}

.label {
  color: #666666;
  font-weight: 500;
}

.value {
  color: #333333;
}

.btn-logout {
  padding: 8px 16px;
  background-color: #e53e3e;
  color: #ffffff;
  border: none;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
  text-decoration: none;
}

.btn-logout:hover {
  background-color: #c53030;
}

.status-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: bold;
}

.status-active {
  background-color: #def7ec;
  color: #03543f;
}

.status-inactive {
  background-color: #fde8e8;
  color: #c53030;
}

.text-center {
  text-align: center;
  color: #666666;
  margin-top: 20px;
}
</style>
