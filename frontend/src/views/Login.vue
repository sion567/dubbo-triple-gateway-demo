<template>
  <div class="login-page">
    <el-card class="login-card">
      <h2 style="text-align: center; margin-top: 0">Dubbo Triple Gateway Demo</h2>
      <el-form :model="form" label-width="70px" @keyup.enter="doLogin">
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="user / admin" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password
                    placeholder="123456 / admin123" />
        </el-form-item>
        <el-button type="primary" style="width: 100%" :loading="loading" @click="doLogin">
          登 录
        </el-button>
      </el-form>
      <el-alert style="margin-top: 16px" type="info" :closable="false"
                title="user/123456（普通用户）  admin/admin123（管理员）" />
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '../api/request'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const form = reactive({ username: 'user', password: '123456' })

async function doLogin() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    const res = await request.post('/user/login', form)
    if (res.code === 0 && res.token) {
      auth.login(res)
      ElMessage.success(`欢迎，${res.username}`)
      router.push('/orders')
    } else {
      ElMessage.error(res.message || '登录失败')
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea, #764ba2);
}
.login-card {
  width: 380px;
  padding: 12px 8px;
}
</style>
