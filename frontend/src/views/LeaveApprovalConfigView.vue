<template>
  <div class="page-container">
    <div class="content-wrapper">
      <a-card class="main-card" :bordered="false">
        <div class="header-section">
          <div class="title-group">
            <h1 class="page-title">审批流程配置</h1>
            <p class="subtitle">Leave Approval Configuration</p>
          </div>
          <a-button type="primary" @click="openCreateModal">
            <PlusOutlined /> 新增配置
          </a-button>
        </div>

        <a-row :gutter="16" class="filter-row">
          <a-col :span="6">
            <a-select
              v-model:value="filterLeaveType"
              placeholder="假期类型（全部）"
              allow-clear
              style="width: 100%"
              @change="fetchData"
            >
              <a-select-option :value="0">通用（所有类型）</a-select-option>
              <a-select-option :value="1">年假</a-select-option>
              <a-select-option :value="2">事假</a-select-option>
              <a-select-option :value="3">病假</a-select-option>
              <a-select-option :value="4">调休</a-select-option>
            </a-select>
          </a-col>
          <a-col :span="6">
            <a-select
              v-model:value="filterDepartmentId"
              placeholder="部门（全部）"
              allow-clear
              style="width: 100%"
              :loading="departmentStore.loading"
              @change="fetchData"
            >
              <a-select-option v-for="d in departmentStore.departmentsFlat" :key="d.id" :value="d.id">
                {{ d.name }}
              </a-select-option>
            </a-select>
          </a-col>
        </a-row>

        <a-table
          :columns="columns"
          :data-source="leaveStore.approvalConfigList"
          :loading="leaveStore.loading"
          :pagination="false"
          :scroll="{ x: 1100 }"
          row-key="id"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'leaveType'">
              <a-tag :color="getLeaveTypeColor(record.leaveType)">
                {{ getLeaveTypeName(record.leaveType) }}
              </a-tag>
            </template>
            <template v-if="column.key === 'department'">
              {{ getDepartmentName(record.departmentId) }}
            </template>
            <template v-if="column.key === 'daysRange'">
              {{ record.minDays }} ~ {{ record.maxDays }} 天
            </template>
            <template v-if="column.key === 'approverRole'">
              <a-tag color="blue">{{ getApproverRoleName(record.approverRole) }}</a-tag>
              <span v-if="record.approverRole === 'SPECIFIC' && record.approverId" class="muted-text">
                （{{ getEmployeeName(record.approverId) }}）
              </span>
            </template>
            <template v-if="column.key === 'enabled'">
              <a-tag :color="record.enabled === 1 ? 'green' : 'default'">
                {{ record.enabled === 1 ? '启用' : '禁用' }}
              </a-tag>
            </template>
            <template v-if="column.key === 'action'">
              <a-space>
                <a-button type="link" size="small" @click="openEditModal(record)">
                  <EditOutlined /> 编辑
                </a-button>
                <a-popconfirm
                  title="确定删除该审批配置？"
                  ok-text="确定"
                  cancel-text="取消"
                  ok-type="danger"
                  @confirm="handleDelete(record)"
                >
                  <a-button type="link" size="small" danger>
                    <DeleteOutlined /> 删除
                  </a-button>
                </a-popconfirm>
              </a-space>
            </template>
          </template>
        </a-table>
      </a-card>
    </div>

    <a-modal
      v-model:open="modalOpen"
      :title="editingConfig?.id ? '编辑审批配置' : '新增审批配置'"
      :width="600"
      :footer="null"
      @cancel="modalOpen = false"
    >
      <a-form ref="formRef" :model="formState" layout="vertical" @finish="handleSubmit">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="假期类型" name="leaveType" :rules="[{ required: true, message: '请选择假期类型!' }]">
              <a-select v-model:value="formState.leaveType" style="width: 100%">
                <a-select-option :value="0">通用（所有类型）</a-select-option>
                <a-select-option :value="1">年假</a-select-option>
                <a-select-option :value="2">事假</a-select-option>
                <a-select-option :value="3">病假</a-select-option>
                <a-select-option :value="4">调休</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="适用部门" name="departmentId">
              <a-select v-model:value="formState.departmentId" style="width: 100%" allow-clear placeholder="全部门">
                <a-select-option v-for="d in departmentStore.departmentsFlat" :key="d.id" :value="d.id">
                  {{ d.name }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>

        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="最小天数" name="minDays" :rules="[{ required: true, message: '请输入最小天数!' }]">
              <a-input-number v-model:value="formState.minDays" :min="0" :precision="1" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="最大天数" name="maxDays" :rules="[{ required: true, message: '请输入最大天数!' }]">
              <a-input-number v-model:value="formState.maxDays" :min="0" :precision="1" style="width: 100%" />
            </a-form-item>
          </a-col>
        </a-row>

        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="节点顺序" name="nodeIndex" :rules="[{ required: true, message: '请输入节点顺序!' }]">
              <a-input-number v-model:value="formState.nodeIndex" :min="0" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="节点名称" name="nodeName" :rules="[{ required: true, message: '请输入节点名称!' }]">
              <a-input v-model:value="formState.nodeName" placeholder="如：直属主管审批" />
            </a-form-item>
          </a-col>
        </a-row>

        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="审批角色" name="approverRole" :rules="[{ required: true, message: '请选择审批角色!' }]">
              <a-select v-model:value="formState.approverRole" style="width: 100%">
                <a-select-option value="DIRECT_MANAGER">直属主管</a-select-option>
                <a-select-option value="DEPARTMENT_HEAD">部门负责人</a-select-option>
                <a-select-option value="HR">HR</a-select-option>
                <a-select-option value="SPECIFIC">指定人员</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item
              v-if="formState.approverRole === 'SPECIFIC'"
              label="指定审批人"
              name="approverId"
              :rules="[{ required: true, message: '请选择审批人!' }]"
            >
              <a-select
                v-model:value="formState.approverId"
                style="width: 100%"
                show-search
                :filter-option="filterEmployeeOption"
              >
                <a-select-option v-for="e in employeeStore.employees" :key="e.id" :value="e.id">
                  {{ e.name }}（{{ e.departmentName }}）
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>

        <a-form-item label="跳过条件" name="skipCondition">
          <a-textarea
            v-model:value="formState.skipCondition"
            :rows="2"
            placeholder="可选。例如：天数 ≤ 3 天跳过当前节点（系统当前未自动执行，可作为参考说明）"
          />
        </a-form-item>

        <a-form-item label="是否启用" name="enabled">
          <a-switch v-model:checked="enabledChecked" />
        </a-form-item>

        <div class="modal-footer">
          <a-button @click="modalOpen = false">取消</a-button>
          <a-button type="primary" html-type="submit" :loading="leaveStore.loading">
            确定
          </a-button>
        </div>
      </a-form>
    </a-modal>
  </div>
</template>

<script lang="ts" setup>
import { ref, computed, reactive, onMounted } from 'vue';
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons-vue';
import { useLeaveStore, type LeaveApprovalConfigItem } from '../stores/leave';
import { useDepartmentStore } from '../stores/department';
import { useEmployeeStore } from '../stores/employee';

const leaveStore = useLeaveStore();
const departmentStore = useDepartmentStore();
const employeeStore = useEmployeeStore();

const filterLeaveType = ref<number | undefined>();
const filterDepartmentId = ref<number | undefined>();
const modalOpen = ref(false);
const formRef = ref();
const editingConfig = ref<LeaveApprovalConfigItem | null>(null);

const enabledChecked = computed({
  get: () => formState.enabled === 1,
  set: (val: boolean) => (formState.enabled = val ? 1 : 0),
});

const formState = reactive({
  id: undefined as number | undefined,
  leaveType: 0,
  departmentId: undefined as number | undefined,
  minDays: 0,
  maxDays: 999,
  nodeIndex: 0,
  nodeName: '',
  approverRole: 'DIRECT_MANAGER',
  approverId: undefined as number | undefined,
  skipCondition: '',
  enabled: 1,
});

const columns = [
  { title: '假期类型', dataIndex: 'leaveType', key: 'leaveType', width: '12%' },
  { title: '适用部门', dataIndex: 'department', key: 'department', width: '12%' },
  { title: '天数范围', dataIndex: 'daysRange', key: 'daysRange', width: '12%' },
  { title: '节点顺序', dataIndex: 'nodeIndex', key: 'nodeIndex', width: '8%' },
  { title: '节点名称', dataIndex: 'nodeName', key: 'nodeName', width: '14%' },
  { title: '审批角色', dataIndex: 'approverRole', key: 'approverRole', width: '16%' },
  { title: '跳过条件', dataIndex: 'skipCondition', key: 'skipCondition', width: '14%' },
  { title: '状态', dataIndex: 'enabled', key: 'enabled', width: '8%' },
  { title: '操作', dataIndex: 'action', key: 'action', width: '12%', fixed: 'right' as const },
];

const getLeaveTypeName = (type?: number) => {
  const map: Record<number, string> = { 0: '通用', 1: '年假', 2: '事假', 3: '病假', 4: '调休' };
  return type !== undefined ? map[type] || '-' : '-';
};

const getLeaveTypeColor = (type?: number) => {
  const map: Record<number, string> = { 0: 'default', 1: 'blue', 2: 'orange', 3: 'red', 4: 'purple' };
  return type !== undefined ? map[type] || 'default' : 'default';
};

const getApproverRoleName = (role?: string) => {
  const map: Record<string, string> = {
    DIRECT_MANAGER: '直属主管',
    DEPARTMENT_HEAD: '部门负责人',
    HR: 'HR',
    SPECIFIC: '指定人员',
  };
  return role ? map[role] || role : '-';
};

const getDepartmentName = (deptId?: number | null) => {
  if (!deptId) return '全部门';
  const dept = departmentStore.departmentsFlat.find((d) => d.id === deptId);
  return dept ? dept.name : '-';
};

const getEmployeeName = (empId?: number | null) => {
  if (!empId) return '-';
  const emp = employeeStore.employees.find((e: any) => e.id === empId);
  return emp ? emp.name : '-';
};

const filterEmployeeOption = (input: string, option: any) => {
  const value = (option?.label || option?.children || '').toString();
  return value.toLowerCase().includes(input.toLowerCase());
};

const fetchData = () => {
  leaveStore.fetchApprovalConfigs({
    leaveType: filterLeaveType.value,
    departmentId: filterDepartmentId.value,
  });
};

const resetForm = () => {
  formState.id = undefined;
  formState.leaveType = 0;
  formState.departmentId = undefined;
  formState.minDays = 0;
  formState.maxDays = 999;
  formState.nodeIndex = 0;
  formState.nodeName = '';
  formState.approverRole = 'DIRECT_MANAGER';
  formState.approverId = undefined;
  formState.skipCondition = '';
  formState.enabled = 1;
  formRef.value?.resetFields();
};

const openCreateModal = () => {
  editingConfig.value = null;
  resetForm();
  modalOpen.value = true;
};

const openEditModal = (record: LeaveApprovalConfigItem) => {
  editingConfig.value = record;
  formState.id = record.id;
  formState.leaveType = record.leaveType ?? 0;
  formState.departmentId = record.departmentId ?? undefined;
  formState.minDays = record.minDays ?? 0;
  formState.maxDays = record.maxDays ?? 999;
  formState.nodeIndex = record.nodeIndex ?? 0;
  formState.nodeName = record.nodeName ?? '';
  formState.approverRole = record.approverRole ?? 'DIRECT_MANAGER';
  formState.approverId = record.approverId ?? undefined;
  formState.skipCondition = record.skipCondition ?? '';
  formState.enabled = record.enabled ?? 1;
  modalOpen.value = true;
};

const handleSubmit = async () => {
  const payload: LeaveApprovalConfigItem = { ...formState };
  let result: LeaveApprovalConfigItem | null;
  if (formState.id) {
    result = await leaveStore.updateApprovalConfig(payload);
  } else {
    result = await leaveStore.createApprovalConfig(payload);
  }
  if (result) {
    modalOpen.value = false;
    fetchData();
  }
};

const handleDelete = async (record: LeaveApprovalConfigItem) => {
  if (!record.id) return;
  const ok = await leaveStore.deleteApprovalConfig(record.id);
  if (ok) fetchData();
};

onMounted(async () => {
  await Promise.all([departmentStore.fetchDepartmentsFlat(), employeeStore.fetchEmployees()]);
  fetchData();
});
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
  padding: 8px 4px;
}

.header-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding: 0 16px;
}

.title-group {
  display: flex;
  flex-direction: column;
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

.filter-row {
  margin-bottom: 16px;
  padding: 0 16px;
}

.muted-text {
  color: #8c8c8c;
  font-size: 12px;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 8px;
}
</style>
