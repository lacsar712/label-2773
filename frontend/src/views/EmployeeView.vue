<template>
  <div class="page-container">
    <div class="content-wrapper">
      <a-card class="main-card" :bordered="false">
        <div class="header-section">
          <div class="title-group">
            <h1 class="page-title">员工管理</h1>
            <p class="subtitle">Employee Management</p>
          </div>
          <div class="header-actions">
            <a-space>
              <a-tree-select
                v-model:value="deptFilterId"
                :tree-data="deptFilterOptions"
                :field-names="{ label: 'name', value: 'id', children: 'children' }"
                placeholder="按部门筛选"
                allow-clear
                tree-default-expand-all
                style="width: 240px"
                @change="handleDeptFilterChange"
              />
              <a-button v-permission="['ADMIN']" type="primary" class="add-btn" @click="showModal()">
                <template #icon><plus-outlined /></template>
                添加员工
              </a-button>
            </a-space>
          </div>
        </div>

        <a-table
          :columns="columns"
          :data-source="employeeStore.filteredEmployees"
          :loading="employeeStore.loading || deptStore.loading"
          row-key="id"
          :pagination="{ pageSize: 10 }"
          class="modern-table"
          :scroll="{ x: 1000 }"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'name'">
              <div class="user-info">
                <a-avatar :style="{ backgroundColor: '#7265e6', verticalAlign: 'middle' }" size="small">
                  {{ record.name.charAt(0) }}
                </a-avatar>
                <span class="user-name">{{ record.name }}</span>
              </div>
            </template>

            <template v-if="column.key === 'department'">
              <a-tag color="blue">{{ record.departmentName || '-' }}</a-tag>
            </template>

            <template v-if="column.key === 'role'">
              <a-tag :color="getRoleColor(record.role)">{{ record.role }}</a-tag>
            </template>

            <template v-if="column.key === 'hireDate'">
              <span>{{ record.hireDate || '-' }}</span>
            </template>

            <template v-if="column.key === 'status'">
              <a-tag :color="getStatusColor(record.status)">{{ getStatusText(record.status) }}</a-tag>
            </template>

            <template v-if="column.key === 'action'">
              <a-space size="small">
                <a-button v-permission="['ADMIN']" type="link" class="action-btn edit" @click="showModal(record)">编辑</a-button>
                <a-divider v-permission="['ADMIN']" type="vertical" />
                <a-popconfirm v-permission="['ADMIN']"
                  title="确定要删除吗?"
                  @confirm="handleDelete(record.id)"
                  ok-text="确定"
                  cancel-text="取消"
                  placement="topRight"
                >
                  <a-button type="link" danger class="action-btn delete">删除</a-button>
                </a-popconfirm>
              </a-space>
            </template>
          </template>
        </a-table>
      </a-card>

      <a-modal
        v-model:open="visible"
        :title="isEdit ? '编辑员工' : '添加员工'"
        @ok="handleOk"
        ok-text="保存"
        cancel-text="取消"
        centered
        :mask-style="{ backdropFilter: 'blur(4px)' }"
        class="modern-modal"
      >
        <a-form ref="formRef" :model="formState" layout="vertical" class="modern-form">
          <a-form-item label="姓名" name="name" :rules="[{ required: true, message: '请输入姓名!' }]">
            <a-input v-model:value="formState.name" placeholder="请输入员工姓名" />
          </a-form-item>
          <a-form-item label="邮箱" name="email" :rules="[{ required: true, message: '请输入邮箱!', type: 'email' }]">
            <a-input v-model:value="formState.email" placeholder="example@company.com" />
          </a-form-item>
          <a-form-item label="部门" name="departmentId" :rules="[{ required: true, message: '请选择部门!' }]">
            <a-tree-select
              v-model:value="formState.departmentId"
              :tree-data="deptSelectOptions"
              :field-names="{ label: 'name', value: 'id', children: 'children' }"
              placeholder="请选择所属部门"
              allow-clear
              tree-default-expand-all
              style="width: 100%"
            />
          </a-form-item>
          <a-form-item label="职位" name="role" :rules="[{ required: true, message: '请输入职位!' }]">
            <a-input v-model:value="formState.role" placeholder="例如：高级工程师" />
          </a-form-item>
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="入职日期" name="hireDate">
                <a-date-picker v-model:value="formState.hireDate" style="width: 100%" format="YYYY-MM-DD" value-format="YYYY-MM-DD" />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="合同到期日" name="contractEndDate">
                <a-date-picker v-model:value="formState.contractEndDate" style="width: 100%" format="YYYY-MM-DD" value-format="YYYY-MM-DD" />
              </a-form-item>
            </a-col>
          </a-row>
          <a-form-item label="状态" name="status">
            <a-select v-model:value="formState.status" style="width: 100%" placeholder="请选择状态">
              <a-select-option :value="1">在职</a-select-option>
              <a-select-option :value="2">试用期</a-select-option>
              <a-select-option :value="3">待转正</a-select-option>
              <a-select-option :value="4">已离职</a-select-option>
            </a-select>
          </a-form-item>
        </a-form>
      </a-modal>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { onMounted, ref, reactive, computed } from 'vue';
import { useEmployeeStore, type Employee } from '../stores/employee';
import { useDepartmentStore } from '../stores/department';
import { PlusOutlined } from '@ant-design/icons-vue';

const employeeStore = useEmployeeStore();
const deptStore = useDepartmentStore();

const visible = ref(false);
const isEdit = ref(false);
const formRef = ref();
const deptFilterId = ref<number | null>(null);

const formState = reactive<Employee>({
  name: '',
  email: '',
  departmentId: 0 as any,
  role: '',
  hireDate: undefined,
  contractEndDate: undefined,
  status: 1,
});

const columns = [
  { title: '姓名', dataIndex: 'name', key: 'name', width: '18%' },
  { title: '邮箱', dataIndex: 'email', key: 'email', width: '20%' },
  { title: '部门', dataIndex: 'departmentName', key: 'department', width: '15%' },
  { title: '职位', dataIndex: 'role', key: 'role', width: '15%' },
  { title: '入职日期', dataIndex: 'hireDate', key: 'hireDate', width: '12%' },
  { title: '状态', dataIndex: 'status', key: 'status', width: '8%' },
  { title: '操作', key: 'action', width: '12%', align: 'center' },
];

const deptFilterOptions = computed(() => {
  return [{ id: null as any, name: '全部部门', children: deptStore.departmentsTree }];
});

const deptSelectOptions = computed(() => {
  const filterEnabled = (items: any[]): any[] => {
    return items
      .filter((d) => d.enabled)
      .map((d) => ({
        ...d,
        children: d.children ? filterEnabled(d.children) : undefined,
      }));
  };
  return filterEnabled(deptStore.departmentsTree);
});

const getRoleColor = (role: string) => {
  const colors = ['blue', 'cyan', 'green', 'purple', 'geekblue', 'magenta'];
  let hash = 0;
  for (let i = 0; i < role.length; i++) {
    hash = role.charCodeAt(i) + ((hash << 5) - hash);
  }
  return colors[Math.abs(hash) % colors.length];
};

const getStatusColor = (status?: number) => {
  switch (status) {
    case 1: return 'green';
    case 2: return 'orange';
    case 3: return 'gold';
    case 4: return 'red';
    default: return 'default';
  }
};

const getStatusText = (status?: number) => {
  switch (status) {
    case 1: return '在职';
    case 2: return '试用期';
    case 3: return '待转正';
    case 4: return '已离职';
    default: return '-';
  }
};

onMounted(async () => {
  await deptStore.refreshAll();
  await employeeStore.fetchEmployees();
});

const handleDeptFilterChange = (value: number | null) => {
  if (!value) {
    employeeStore.setDepartmentFilter([]);
    return;
  }
  const descendantIds = deptStore.collectDescendantIds(
    deptStore.flattenDepartments(deptStore.departmentsTree),
    value
  );
  employeeStore.setDepartmentFilter(descendantIds);
};

const showModal = (record?: Employee) => {
  if (record) {
    isEdit.value = true;
    Object.assign(formState, record);
  } else {
    isEdit.value = false;
    formState.id = undefined;
    formState.name = '';
    formState.email = '';
    formState.departmentId = 0 as any;
    formState.role = '';
    formState.hireDate = undefined;
    formState.contractEndDate = undefined;
    formState.status = 1;
  }
  visible.value = true;
};

const handleOk = async () => {
  try {
    await formRef.value.validate();
    if (isEdit.value) {
      await employeeStore.updateEmployee(formState);
    } else {
      await employeeStore.createEmployee(formState);
    }
    visible.value = false;
  } catch (error) {
    // validation failed
  }
};

const handleDelete = async (id: number) => {
  await employeeStore.deleteEmployee(id);
};
</script>

<style scoped>
.page-container {
  min-height: 100vh;
  padding: 24px 24px;
  display: flex;
  justify-content: center;
  align-items: flex-start;
}

.content-wrapper {
  width: 100%;
  max-width: 1400px;
}

.main-card {
  border-radius: 14px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
  transition: all 0.3s ease;
  overflow: hidden;
}

.main-card:hover {
  box-shadow: 0 6px 24px rgba(0, 0, 0, 0.08);
}

.header-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding: 0 8px;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0;
  background: linear-gradient(135deg, #2c3e50 0%, #3498db 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.subtitle {
  color: #8c8c8c;
  margin: 4px 0 0 0;
  font-size: 13px;
}

.add-btn {
  height: 36px;
  padding: 0 20px;
  border-radius: 18px;
  font-weight: 600;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  box-shadow: 0 4px 12px rgba(118, 75, 162, 0.3);
  transition: all 0.3s ease;
}

.add-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 18px rgba(118, 75, 162, 0.4);
  background: linear-gradient(135deg, #764ba2 0%, #667eea 100%);
}

.add-btn:active {
  transform: translateY(0);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-name {
  font-weight: 500;
  color: #2c3e50;
}

.action-btn {
  padding: 0;
  font-weight: 500;
}

.action-btn.edit {
  color: #3498db;
}

.action-btn.delete {
  color: #ff4d4f;
}

@media (max-width: 768px) {
  .page-container {
    padding: 16px 12px;
  }

  .header-section {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .header-actions {
    width: 100%;
  }

  .header-actions .ant-space {
    width: 100%;
    flex-wrap: wrap;
  }

  .add-btn {
    width: 100%;
  }
}
</style>
