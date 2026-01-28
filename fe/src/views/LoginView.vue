<template>
  <div class="login-container">
    <div class="login-header">
      <h1>AuthVs 登录</h1>
    </div>

    <div v-if="error" class="alert alert-error">
      {{ error }}
    </div>

    <form @submit.prevent="handleSubmit">
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
      <button class="btn" type="submit" :disabled="submitting">
        {{ submitting ? "登录中..." : "登 录" }}
      </button>
    </form>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const form = reactive({
  username: '',
  password: '',
  captcha: '',
  captchaKey: ''
})

const captchaImg = ref('')
const error = ref('')
const submitting = ref(false)

async function refreshCaptcha() {
  error.value = ''
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

async function handleSubmit() {
  error.value = ''
  submitting.value = true
  try {
    const res = await fetch('/api/login', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(form)
    })
    const data = await res.json()
    if (data.code === 200) {
      const { token, redirectUrl } = data.data
      if (token) {
        localStorage.setItem('auth_token', token)
      }
      
      if (redirectUrl) {
        window.location.href = redirectUrl
      } else {
        router.push('/profile')
      }
    } else {
      error.value = data.message || '登录失败'
      await refreshCaptcha()
    }
  } catch (e) {
    error.value = '登录请求失败，请稍后重试'
    console.error(e)
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  refreshCaptcha()
})
</script>

<style scoped>
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

.alert {
  padding: 12px;
  margin-bottom: 20px;
  border-radius: 4px;
  font-size: 14px;
}

.alert-error {
  background-color: #fde8e8;
  color: #c53030;
  border: 1px solid #fbd5d5;
}
</style>
