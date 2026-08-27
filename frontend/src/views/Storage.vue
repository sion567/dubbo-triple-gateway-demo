<template>
  <el-card>
    <template #header>
      <div class="card-header">
        <span>库存管理（管理员）</span>
        <el-button type="primary" @click="openSave()">新增 / 补货</el-button>
      </div>
    </template>

    <el-table :data="rows" v-loading="loading" border stripe>
      <el-table-column prop="productCode" label="商品编码" width="200" />
      <el-table-column label="库存数量" width="140">
        <template #default="{ row }">
          <el-tag :type="row.count > 10 ? 'success' : 'danger'">{{ row.count }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button size="small" type="primary" @click="openSave(row)">补货</el-button>
          <el-button size="small" type="danger" @click="del(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <el-dialog v-model="dialog" :title="editCode ? `补货：${editCode}` : '新增商品'" width="420">
    <el-form :model="form" label-width="90px">
      <el-form-item label="商品编码">
        <el-input v-model="form.productCode" :disabled="!!editCode" placeholder="如 iPhone15" />
      </el-form-item>
      <el-form-item :label="editCode ? '补货数量' : '初始库存'">
        <el-input-number v-model="form.count" :min="1" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialog = false">取消</el-button>
      <el-button type="primary" @click="save">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../api/request'

const rows = ref([])
const loading = ref(false)
const dialog = ref(false)
const editCode = ref('')
const form = reactive({ productCode: '', count: 10 })

async function load() {
  loading.value = true
  try {
    rows.value = await request.get('/storage/list')
  } finally {
    loading.value = false
  }
}

function openSave(row) {
  editCode.value = row?.productCode || ''
  form.productCode = row?.productCode || ''
  form.count = 10
  dialog.value = true
}

async function save() {
  await request.post('/storage/save', { ...form })
  ElMessage.success('已保存')
  dialog.value = false
  load()
}

async function del(row) {
  await ElMessageBox.confirm(`确认删除 ${row.productCode}？`, '提示', { type: 'warning' })
  await request.delete(`/storage/delete/${row.productCode}`)
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
