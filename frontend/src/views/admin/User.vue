<template>
  <div class="admin-user">
    <div class="sg-page-title">
      <div>
        <h1>用户管理</h1>
        <div class="sg-subtitle">用户状态、角色分配、重置密码与数据清理。</div>
      </div>
      <el-button type="primary" :disabled="!isSuperAdmin" @click="openCreateAdmin">新增管理员</el-button>
    </div>

    <el-card class="sg-card">
    <div class="bar">
      <el-input v-model="keyword" placeholder="用户名/手机/邮箱" class="kw" @keyup.enter="load" />
      <el-select v-model="statusFilter" placeholder="全部状态" class="st" @change="load">
        <el-option label="全部状态" :value="null" />
        <el-option label="正常" :value="1" />
        <el-option label="禁用" :value="0" />
      </el-select>
      <el-button @click="load">查询</el-button>
    </div>
    <el-table :data="list" v-loading="loading" class="table">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="username" label="用户名" width="120" />
      <el-table-column prop="roles" label="角色" min-width="200">
        <template #default="{ row }">
          <el-tag v-for="role in getRoleNames(row.roles)" :key="role" effect="plain" class="mr-5">{{ role }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="phone" label="手机" width="120" />
      <el-table-column prop="email" label="邮箱" width="180" />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">{{ row.status === 1 ? '正常' : '禁用' }}</template>
      </el-table-column>
      <el-table-column prop="createTime" label="注册时间" width="180" />
      <el-table-column label="操作" width="280">
        <template #default="{ row }">
          <el-button link type="primary" :disabled="!isSuperAdmin" @click="setStatus(row.id, row.status === 1 ? 0 : 1)">{{ row.status === 1 ? '禁用' : '启用' }}</el-button>
          <el-button link type="primary" :disabled="!isSuperAdmin" @click="openSetRole(row)">分配角色</el-button>
          <el-button link type="warning" :disabled="!isSuperAdmin" @click="removeAdminRoles(row)">去除权限</el-button>
          <el-button link type="primary" :disabled="!isSuperAdmin" @click="openResetPwd(row)">重置密码</el-button>
          <el-button link type="danger" :disabled="!isSuperAdmin" @click="deleteUser(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div class="pager">
      <el-pagination v-model:current-page="current" :page-size="size" :total="total" layout="prev, pager, next" @current-change="load" />
    </div>
    </el-card>
    <el-dialog v-model="resetVisible" title="重置密码" width="400px">
      <el-input v-model="newPwd" type="password" placeholder="新密码（至少6位）" />
      <template #footer>
        <el-button @click="resetVisible = false">取消</el-button>
        <el-button type="primary" @click="resetPwd">确定</el-button>
      </template>
    </el-dialog>
    <el-dialog v-model="roleVisible" title="分配角色" width="420px">
      <el-checkbox-group v-model="selectedRoles">
        <el-checkbox v-for="r in allRoles" :key="r.code" :label="r.code" style="display:block;margin-bottom:10px">{{ r.name }}</el-checkbox>
      </el-checkbox-group>
      <template #footer>
        <el-button @click="roleVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingRole" @click="saveRole">确定</el-button>
      </template>
    </el-dialog>
    <el-dialog v-model="createVisible" title="新增管理员" width="420px">
      <el-form :model="createForm" label-width="90px">
        <el-form-item label="用户名">
          <el-input v-model="createForm.username" placeholder="至少2位" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="createForm.password" type="password" placeholder="至少6位" show-password />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="createForm.roleCode" style="width:100%">
            <el-option v-for="r in allRoles.filter(x=>x.code!=='user')" :key="r.code" :label="r.name" :value="r.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="真实姓名">
          <el-input v-model="createForm.realName" />
        </el-form-item>
        <el-form-item label="手机">
          <el-input v-model="createForm.phone" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="createForm.email" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="createAdmin">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '../../utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '../../stores/user'

const list = ref([])
const loading = ref(false)
const current = ref(1)
const size = ref(10)
const total = ref(0)
const keyword = ref('')
const statusFilter = ref(null)
const resetVisible = ref(false)
const resetUserId = ref(null)
const newPwd = ref('')
const createVisible = ref(false)
const creating = ref(false)
const createForm = ref({ username: '', password: '', realName: '', phone: '', email: '', roleCode: 'admin' })
const allRoles = ref([])
const roleVisible = ref(false)
const roleUserId = ref(null)
const selectedRoles = ref([])
const savingRole = ref(false)
const userStore = useUserStore()
userStore.initFromStorage()
const isSuperAdmin = (userStore.user?.roles || []).includes('ROLE_SUPER_ADMIN')

function getRoleNames(roles) {
  if (!roles || !roles.length) return ['普通用户']
  const map = { SUPER_ADMIN: '超级管理员', CONTENT_ADMIN: '内容管理员', REVIEW_ADMIN: '审核管理员', ADMIN: '管理员', USER: '普通用户' }
  return roles.map(r => map[r.toUpperCase()] || r)
}

async function loadRoles() {
  const res = await request.get('/admin/user/roles')
  allRoles.value = res || []
}

async function load() {
  loading.value = true
  try {
    const res = await request.get('/admin/user/list', { params: { current: current.value, size: size.value, keyword: keyword.value || null, status: statusFilter.value } })
    list.value = (res.records || []).map(r => ({ ...r, createTime: r.createTime ? new Date(r.createTime).toLocaleString('zh-CN') : '' }))
    total.value = res.total || 0
  } finally { loading.value = false }
}
async function setStatus(id, status) {
  await request.put('/admin/user/status/' + id, null, { params: { status } })
  ElMessage.success('操作成功')
  load()
}
function openResetPwd(row) {
  resetUserId.value = row.id
  newPwd.value = ''
  resetVisible.value = true
}
async function resetPwd() {
  if (!newPwd.value || newPwd.value.length < 6) { ElMessage.warning('密码至少6位'); return }
  await request.put('/admin/user/resetPwd/' + resetUserId.value, { password: newPwd.value })
  ElMessage.success('已重置')
  resetVisible.value = false
  load()
}

async function deleteUser(row) {
  await ElMessageBox.confirm(`确定删除用户「${row.username}」？此操作不可恢复。`)
  await request.delete('/admin/user/' + row.id)
  ElMessage.success('已删除')
  load()
}

function openSetRole(row) {
  roleUserId.value = row.id
  selectedRoles.value = normalizeRoleCodes(row.roles || [])
  roleVisible.value = true
}
async function saveRole() {
  savingRole.value = true
  try {
    const roleIdMap = {}
    allRoles.value.forEach(r => { roleIdMap[(r.code || '').toLowerCase()] = r.id })
    const roleIds = selectedRoles.value.map(code => roleIdMap[(code || '').toLowerCase()]).filter(Boolean)
    await request.put('/admin/user/role/' + roleUserId.value, { roleIds })
    ElMessage.success('已保存')
    roleVisible.value = false
    load()
  } finally { savingRole.value = false }
}

function normalizeRoleCodes(roles) {
  const map = { USER: 'user', ADMIN: 'admin', SUPER_ADMIN: 'super_admin', CONTENT_ADMIN: 'content_admin', REVIEW_ADMIN: 'review_admin' }
  return (roles || []).map(r => map[(r || '').toUpperCase()] || (r || '').toLowerCase()).filter(Boolean)
}

async function removeAdminRoles(row) {
  await ElMessageBox.confirm(`确定去除用户「${row.username}」的管理员权限？将仅保留普通用户权限。`)
  if (userStore.user?.userId && row.id === userStore.user.userId) {
    ElMessage.warning('不能修改自己的角色')
    return
  }
  const roleIdMap = {}
  allRoles.value.forEach(r => { roleIdMap[(r.code || '').toLowerCase()] = r.id })
  const roleIds = [roleIdMap.user].filter(Boolean)
  await request.put('/admin/user/role/' + row.id, { roleIds })
  ElMessage.success('已去除')
  load()
}

function openCreateAdmin() {
  createForm.value = { username: '', password: '', realName: '', phone: '', email: '', roleCode: 'admin' }
  createVisible.value = true
}

async function createAdmin() {
  if (!createForm.value.username || createForm.value.username.length < 2) { ElMessage.warning('用户名至少2位'); return }
  if (!createForm.value.password || createForm.value.password.length < 6) { ElMessage.warning('密码至少6位'); return }
  creating.value = true
  try {
    await request.post('/admin/user/createAdmin', createForm.value)
    ElMessage.success('已创建')
    createVisible.value = false
    load()
  } finally { creating.value = false }
}
onMounted(() => { loadRoles(); load() })
</script>

<style lang="scss" scoped>
.bar {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.kw {
  width: 260px;
}

.st {
  width: 140px;
}

.table {
  width: 100%;
}

.pager {
  margin-top: 14px;
  display: flex;
  justify-content: center;
}

@media (max-width: 520px) {
  .kw,
  .st {
    width: 100%;
  }
}
</style>
