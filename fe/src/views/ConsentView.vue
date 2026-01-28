<template>
  <div class="consent-container">
    <div class="consent-box">
      <div v-if="loading" class="loading">加载中...</div>
      <div v-else-if="error" class="error">{{ error }}</div>
      <div v-else>
        <h2>授权请求</h2>
        <div class="client-info">
          <p>
            应用 <strong>{{ clientInfo.clientName || clientId }}</strong> 请求访问您的账户。
          </p>
          <p class="desc" v-if="clientInfo.description">
            {{ clientInfo.description }}
          </p>
        </div>

        <form method="post" action="/oauth2/authorize" ref="consentForm">
          <input type="hidden" name="client_id" :value="clientId" />
          <input type="hidden" name="state" :value="state" />
          
          <div class="scopes-section">
            <p>该应用将获得以下权限：</p>
            <div v-for="scope in scopes" :key="scope" class="scope-item">
              <label>
                <input
                  type="checkbox"
                  name="scope"
                  :value="scope"
                  checked
                />
                <span class="scope-name">{{ scope }}</span>
                <span class="scope-desc">{{ getScopeDesc(scope) }}</span>
              </label>
            </div>
          </div>

          <div class="actions">
            <button
              type="submit"
              name="consent_action"
              value="cancel"
              class="btn btn-cancel"
            >
              取消
            </button>
            <button
              type="submit"
              name="consent_action"
              value="approve"
              class="btn btn-approve"
            >
              同意授权
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const clientId = ref('')
const state = ref('')
const scopes = ref([])
const clientInfo = ref({})
const loading = ref(true)
const error = ref('')

// 简单的 scope 描述映射
const scopeDescriptions = {
  openid: '验证您的身份 (OpenID Connect)',
  profile: '访问您的个人基本信息 (用户名、类型等)',
  email: '访问您的邮箱地址',
  phone: '访问您的手机号码',
  address: '访问您的地址信息'
}

function getScopeDesc(scope) {
  return scopeDescriptions[scope] || `权限: ${scope}`
}

onMounted(async () => {
  // 从 URL 获取参数
  clientId.value = route.query.client_id
  state.value = route.query.state
  const scopeStr = route.query.scope || ''
  scopes.value = scopeStr.split(' ').filter(s => s)

  if (!clientId.value || !state.value) {
    error.value = '无效的授权请求参数'
    loading.value = false
    return
  }

  // 获取 Client 信息
  try {
    const res = await fetch(`/api/oauth2/client/${clientId.value}`)
    const data = await res.json()
    if (data.code === 200) {
      clientInfo.value = data.data
    } else {
      console.warn('获取应用信息失败:', data.message)
    }
  } catch (e) {
    console.error('获取应用信息异常:', e)
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.consent-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background-color: #f5f7fa;
}

.consent-box {
  background: white;
  padding: 40px;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  width: 100%;
  max-width: 500px;
}

h2 {
  text-align: center;
  margin-top: 0;
  color: #333;
}

.client-info {
  text-align: center;
  margin-bottom: 30px;
  padding-bottom: 20px;
  border-bottom: 1px solid #eee;
}

.client-info strong {
  color: #409eff;
}

.desc {
  color: #666;
  font-size: 14px;
  margin-top: 5px;
}

.scopes-section {
  margin-bottom: 30px;
}

.scope-item {
  margin-bottom: 10px;
  padding: 10px;
  background: #f9f9f9;
  border-radius: 4px;
}

.scope-item label {
  display: flex;
  align-items: center;
  cursor: pointer;
}

.scope-item input {
  margin-right: 10px;
}

.scope-name {
  font-weight: bold;
  margin-right: 10px;
  color: #333;
}

.scope-desc {
  color: #888;
  font-size: 13px;
}

.actions {
  display: flex;
  gap: 20px;
}

.btn {
  flex: 1;
  padding: 12px;
  border: none;
  border-radius: 4px;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.3s;
}

.btn-approve {
  background-color: #409eff;
  color: white;
}

.btn-approve:hover {
  background-color: #66b1ff;
}

.btn-cancel {
  background-color: #f5f7fa;
  color: #606266;
  border: 1px solid #dcdfe6;
}

.btn-cancel:hover {
  background-color: #e6e8eb;
}

.loading, .error {
  text-align: center;
  padding: 20px;
  color: #666;
}

.error {
  color: #f56c6c;
}
</style>
