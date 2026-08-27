<template>
  <el-container style="height: 100vh">
    <el-aside width="200px" style="background: #304156">
      <div class="logo">Triple Demo</div>
      <el-menu :default-active="$route.path" router background-color="#304156"
               text-color="#bfcbd9" active-text-color="#409eff">
        <el-menu-item index="/orders">📦 订单管理</el-menu-item>
        <el-menu-item v-if="auth.isAdmin" index="/users">👥 用户管理</el-menu-item>
        <el-menu-item v-if="auth.isAdmin" index="/storage">🏬 库存管理</el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <span />
        <div class="user-box">
          <el-tag :type="auth.isAdmin ? 'danger' : 'success'" size="small">
            {{ auth.user?.username }}（{{ auth.isAdmin ? '管理员' : '普通用户' }}）
          </el-tag>
          <el-button size="small" @click="logout">退出</el-button>
        </div>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const router = useRouter()

function logout() {
  auth.logout()
  router.push('/login')
}
</script>

<style scoped>
.logo {
  height: 60px;
  line-height: 60px;
  text-align: center;
  color: #fff;
  font-weight: bold;
  letter-spacing: 2px;
}
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #e6e6e6;
}
.user-box {
  display: flex;
  align-items: center;
  gap: 12px;
}
</style>
