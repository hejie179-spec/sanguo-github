<template>
  <div class="admin-resource">
    <div class="sg-page-title">
      <div>
        <h1>{{ typeLabel }}管理</h1>
        <div class="sg-subtitle">新增、编辑与排序资源内容；支持上传图片。</div>
      </div>
      <el-button type="primary" @click="openEdit()">新增</el-button>
    </div>

    <el-card class="sg-card">
      <el-table :data="list" v-loading="loading" class="table">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column :prop="type === 'person' ? 'name' : 'title'" label="名称" min-width="180" show-overflow-tooltip />
        <el-table-column label="图片" width="90">
          <template #default="{ row }">
            <el-image v-if="row.imageUrl" :src="row.imageUrl" fit="contain" class="cover" />
            <div v-else class="cover ph">无图</div>
          </template>
        </el-table-column>
        <el-table-column prop="dynasty" label="朝代" width="110" v-if="type==='person'||type==='event'" />
        <el-table-column prop="sort" label="排序" width="90" />
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="del(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination v-model:current-page="current" :page-size="size" :total="total" layout="prev, pager, next" @current-change="load" />
      </div>
    </el-card>
    <el-dialog v-model="editVisible" :title="editId ? '编辑' : '新增'" width="600px" :destroy-on-close="true" @close="resetEdit()">
      <el-form :model="editForm" label-width="80px">
        <template v-if="type==='person'">
          <el-form-item label="姓名"><el-input v-model="editForm.name" /></el-form-item>
          <el-form-item label="别名"><el-input v-model="editForm.alias" /></el-form-item>
          <el-form-item label="朝代"><el-input v-model="editForm.dynasty" /></el-form-item>
          <el-form-item label="简介"><el-input v-model="editForm.introduction" type="textarea" /></el-form-item>
          <el-form-item label="图片"><ImageUpload v-model="editForm.imageUrl" /></el-form-item>
          <el-form-item label="排序"><el-input-number v-model="editForm.sort" /></el-form-item>
        </template>
        <template v-else-if="type==='event'">
          <el-form-item label="标题"><el-input v-model="editForm.title" /></el-form-item>
          <el-form-item label="类型"><el-input v-model="editForm.type" /></el-form-item>
          <el-form-item label="朝代"><el-input v-model="editForm.dynasty" /></el-form-item>
          <el-form-item label="内容"><el-input v-model="editForm.content" type="textarea" /></el-form-item>
          <el-form-item label="历史意义"><el-input v-model="editForm.historicalValue" type="textarea" /></el-form-item>
          <el-form-item label="图片"><ImageUpload v-model="editForm.imageUrl" /></el-form-item>
          <el-form-item label="排序"><el-input-number v-model="editForm.sort" /></el-form-item>
        </template>
        <template v-else-if="type==='literature'">
          <el-form-item label="标题"><el-input v-model="editForm.title" /></el-form-item>
          <el-form-item label="作者"><el-input v-model="editForm.author" /></el-form-item>
          <el-form-item label="年份"><el-input v-model="editForm.publishYear" /></el-form-item>
          <el-form-item label="类别"><el-input v-model="editForm.category" /></el-form-item>
          <el-form-item label="来源"><el-input v-model="editForm.source" /></el-form-item>
          <el-form-item label="图片"><ImageUpload v-model="editForm.imageUrl" /></el-form-item>
          <el-form-item label="排序"><el-input-number v-model="editForm.sort" /></el-form-item>
        </template>
        <template v-else>
          <el-form-item label="名称"><el-input v-model="editForm.title" /></el-form-item>
          <el-form-item label="来源"><el-input v-model="editForm.source" /></el-form-item>
          <el-form-item label="内容"><el-input v-model="editForm.content" type="textarea" /></el-form-item>
          <el-form-item label="图片"><ImageUpload v-model="editForm.imageUrl" /></el-form-item>
          <el-form-item label="排序"><el-input-number v-model="editForm.sort" /></el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'
import request from '../../utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import ImageUpload from '../../components/ImageUpload.vue'

// 管理后台 - 资源管理（路由：/admin/person|event|literature|allusion）
// - 该页面复用同一份代码，通过 props.type 区分资源类型
// - 列表：GET /admin/{type}/list
// - 新增/编辑：POST/PUT /admin/{type}
// - 删除：DELETE /admin/{type}/:id
const props = defineProps({ type: String })
const typeLabel = computed(() => ({ person: '人物', event: '事件', literature: '史料', allusion: '典故' }[props.type]))
const list = ref([])
const loading = ref(false)
const current = ref(1)
const size = ref(10)
const total = ref(0)
const editVisible = ref(false)
const editId = ref(null)
const saving = ref(false)
const editForm = reactive({})

function getEmptyForm() {
  if (props.type === 'person') return { name: '', alias: '', dynasty: '', introduction: '', imageUrl: '', sort: 0 }
  if (props.type === 'event') return { title: '', type: '', dynasty: '', content: '', historicalValue: '', imageUrl: '', sort: 0 }
  if (props.type === 'literature') return { title: '', author: '', publishYear: '', category: '', source: '', imageUrl: '', sort: 0 }
  return { title: '', source: '', content: '', imageUrl: '', sort: 0 }
}

async function load() {
  loading.value = true
  try {
    const r = await request.get('/' + props.type + '/list', { params: { current: current.value, size: size.value } })
    list.value = r.records || []
    total.value = r.total || 0
  } finally { loading.value = false }
}
function openEdit(row) {
  resetEdit()
  editId.value = row?.id || null
  Object.assign(editForm, row ? { ...getEmptyForm(), ...row } : getEmptyForm())
  editVisible.value = true
}
function resetEdit() {
  editId.value = null
  Object.assign(editForm, getEmptyForm())
}
async function save() {
  saving.value = true
  try {
    if (editId.value) {
      await request.put('/' + props.type + '/update', editForm)
    } else {
      await request.post('/' + props.type + '/save', editForm)
    }
    ElMessage.success('保存成功')
    editVisible.value = false
    load()
  } finally { saving.value = false }
}
async function del(id) {
  await ElMessageBox.confirm('确定删除？')
  await request.delete('/' + props.type + '/' + id)
  ElMessage.success('已删除')
  load()
}

watch(
  () => props.type,
  () => {
    current.value = 1
    list.value = []
    total.value = 0
    editVisible.value = false
    resetEdit()
    load()
  },
  { immediate: true }
)
</script>

<style lang="scss" scoped>
.table {
  width: 100%;
}

.cover {
  width: 56px;
  height: 56px;
  border-radius: 10px;
  border: 1px solid var(--sanguo-border);
  background: #f4f0e8;
}

.ph {
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--sanguo-text-secondary);
  font-weight: 800;
  background: #f4f0e8;
}

.pager {
  margin-top: 14px;
  display: flex;
  justify-content: center;
}
</style>
