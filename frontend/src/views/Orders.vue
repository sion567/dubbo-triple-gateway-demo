<template>
  <el-card>
    <template #header>
      <div class="card-header">
        <span>订单管理</span>
        <el-button type="primary" @click="dialog = true">下单</el-button>
      </div>
    </template>

    <el-table :data="orders" v-loading="loading" border stripe>
      <el-table-column prop="orderId" label="ID" width="70" />
      <el-table-column prop="userId" label="用户ID" width="80" />
      <el-table-column prop="productCode" label="商品编码" width="130" />
      <el-table-column prop="product" label="商品" min-width="120" />
      <el-table-column prop="count" label="数量" width="70" />
      <el-table-column prop="money" label="金额" width="100" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'SUCCESS' ? 'success' : row.status === 'INIT' ? 'warning' : 'info'">
            {{ row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column v-if="auth.isAdmin" label="操作" width="220">
        <template #default="{ row }">
          <el-button size="small" type="success" @click="setStatus(row, 'SUCCESS')">完成</el-button>
          <el-button size="small" type="warning" @click="setStatus(row, 'CANCELLED')">取消</el-button>
          <el-button size="small" type="danger" @click="del(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <el-dialog v-model="dialog" title="下单（Seata 分布式事务）" width="480">
    <el-form :model="form" label-width="90px">
      <el-form-item label="用户ID">
        <el-input-number v-model="form.userId" :min="1" :max="3" />
      </el-form-item>
      <el-form-item label="商品编码">
        <el-select v-model="form.productCode">
          <el-option label="iPhone15" value="iPhone15" />
          <el-option label="MacBookPro" value="MacBookPro" />
        </el-select>
      </el-form-item>
      <el-form-item label="商品名">
        <el-input v-model="form.product" />
      </el-form-item>
      <el-form-item label="数量">
        <el-input-number v-model="form.count" :min="1" />
      </el-form-item>
      <el-form-item label="单价">
        <el-input-number v-model="form.price" :min="0.01" :precision="2" />
      </el-form-item>
      <el-form-item label="触发回滚">
        <el-switch v-model="fail" active-text="status=FAIL（验证 Seata 全局回滚）" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialog = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="save">提交订单</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../api/request'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const orders = ref([])
const loading = ref(false)
const dialog = ref(false)
const saving = ref(false)
const fail = ref(false)
const form = reactive({ userId: 1, productCode: 'iPhone15', product: 'iPhone 15', count: 1, price: 6999 })

async function load() {
  loading.value = true
  try {
    orders.value = await request.get('/order/list')
  } finally {
    loading.value = false
  }
}

async function save() {
  saving.value = true
  try {
    const body = { ...form, status: fail.value ? 'FAIL' : 'OK' }
    const res = await request.post('/order/createOrder', body)
    ElMessage.success(res)
    dialog.value = false
    load()
  } catch {
    // 拦截器已提示（余额/库存不足、回滚等），刷新看最终状态
    load()
  } finally {
    saving.value = false
  }
}

async function setStatus(row, status) {
  await request.post('/order/updateStatus', { id: row.orderId, status })
  ElMessage.success('已更新')
  load()
}

async function del(row) {
  await ElMessageBox.confirm(`确认删除订单 ${row.orderId}？`, '提示', { type: 'warning' })
  await request.delete(`/order/delete/${row.orderId}`)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
