<template>
  <el-card>
    <template #header>
      <span>用户管理（管理员）</span>
    </template>

    <el-table :data="users" v-loading="loading" border stripe>
      <el-table-column prop="userId" label="用户ID" width="80" />
      <el-table-column prop="name" label="姓名" width="120" />
      <el-table-column prop="username" label="登录账号" width="120" />
      <el-table-column label="角色" width="220">
        <template #default="{ row }">
          <el-tag v-for="r in row.roles || []" :key="r" size="small"
                  :type="r === 'ROLE_ADMIN' ? 'danger' : 'success'" style="margin-right: 6px">
            {{ r }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="余额（元）" width="140">
        <template #default="{ row }">
          {{ row.money != null ? row.money.toFixed(2) : '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="perms" label="权限点">
        <template #default="{ row }">
          <el-tag v-for="p in row.perms || []" :key="p" type="info" size="small"
                  style="margin-right: 4px">{{ p }}</el-tag>
        </template>
      </el-table-column>
    </el-table>
    <el-alert style="margin-top: 12px" type="info" :closable="false"
              title="余额在账户库（seata_account），下单时经 Seata 全局事务扣减" />
  </el-card>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import request from '../api/request'

const users = ref([])
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    const res = await request.get('/user/list')
    users.value = res.users || []
  } finally {
    loading.value = false
  }
})
</script>
