<template>
  <div class="page-container">
    <div class="content-wrapper">
      <a-row :gutter="24">
        <a-col :xs="24" :md="10">
          <a-card class="main-card" :bordered="false">
            <div class="header-section">
              <div class="title-group">
                <h1 class="page-title">请假申请</h1>
                <p class="subtitle">Leave Application</p>
              </div>
              <div class="header-actions">
                <a-select
                  v-model:value="selectedEmployeeId"
                  placeholder="选择员工"
                  style="width: 200px"
                  :loading="employeeStore.loading"
                  @change="handleEmployeeChange"
                >
                  <a-select-option
                    v-for="emp in employeeStore.employees"
                    :key="emp.id"
                    :value="emp.id"
                  >
                    {{ emp.name }}
                  </a-select-option>
                </a-select>
              </div>
            </div>

            <a-alert
              v-if="isProxyMode"
              class="proxy-alert"
              type="warning"
              show-icon
              message="代申请模式"
              description="当前正在以他人身份提交请假申请，系统将记录您为代申请人"
            />

            <div v-if="selectedEmployeeId" class="balance-section">
              <a-row :gutter="12">
                <a-col :span="12" v-for="bal in displayBalances" :key="bal.leaveType">
                  <div class="balance-card">
                    <div class="balance-name">{{ getLeaveTypeName(bal.leaveType) }}</div>
                    <div class="balance-value">
                      <span class="remaining">{{ bal.remainingDays ?? 0 }}</span>
                      <span class="total"> / {{ bal.totalDays ?? 0 }}天</span>
                    </div>
                    <a-progress
                      :percent="calculatePercent(bal)"
                      :stroke-color="getBalanceColor(bal)"
                      size="small"
                      :show-info="false"
                    />
                  </div>
                </a-col>
              </a-row>
            </div>

            <a-form ref="formRef" :model="formState" layout="vertical" class="modern-form" @finish="handleSubmit">
              <a-form-item label="假期类型" name="leaveType" :rules="[{ required: true, message: '请选择假期类型!' }]">
                <a-select v-model:value="formState.leaveType" style="width: 100%" placeholder="请选择假期类型">
                  <a-select-option :value="1">年假</a-select-option>
                  <a-select-option :value="2">事假</a-select-option>
                  <a-select-option :value="3">病假</a-select-option>
                  <a-select-option :value="4">调休</a-select-option>
                </a-select>
              </a-form-item>

              <a-row :gutter="12">
                <a-col :span="12">
                  <a-form-item label="开始日期" name="startDate" :rules="[{ required: true, message: '请选择开始日期!' }]">
                    <a-date-picker
                      v-model:value="formState.startDate"
                      style="width: 100%"
                      format="YYYY-MM-DD"
                      value-format="YYYY-MM-DD"
                      @change="recalculateDays"
                    />
                  </a-form-item>
                </a-col>
                <a-col :span="12">
                  <a-form-item label="开始时段" name="startHalf">
                    <a-select v-model:value="formState.startHalf" style="width: 100%" @change="recalculateDays">
                      <a-select-option :value="0">全天</a-select-option>
                      <a-select-option :value="1">上午</a-select-option>
                      <a-select-option :value="2">下午</a-select-option>
                    </a-select>
                  </a-form-item>
                </a-col>
              </a-row>

              <a-row :gutter="12">
                <a-col :span="12">
                  <a-form-item label="结束日期" name="endDate" :rules="[{ required: true, message: '请选择结束日期!' }]">
                    <a-date-picker
                      v-model:value="formState.endDate"
                      style="width: 100%"
                      format="YYYY-MM-DD"
                      value-format="YYYY-MM-DD"
                      @change="recalculateDays"
                    />
                  </a-form-item>
                </a-col>
                <a-col :span="12">
                  <a-form-item label="结束时段" name="endHalf">
                    <a-select v-model:value="formState.endHalf" style="width: 100%" @change="recalculateDays">
                      <a-select-option :value="0">全天</a-select-option>
                      <a-select-option :value="1">上午</a-select-option>
                      <a-select-option :value="2">下午</a-select-option>
                    </a-select>
                  </a-form-item>
                </a-col>
              </a-row>

              <a-form-item label="请假天数">
                <a-input-number
                  :value="calculatedDays"
                  :min="0"
                  :step="0.5"
                  :precision="1"
                  disabled
                  style="width: 100%"
                  addon-after="工作日（已自动扣除节假日）"
                />
              </a-form-item>

              <a-form-item label="请假事由" name="reason" :rules="[{ required: true, message: '请输入请假事由!' }]">
                <a-textarea
                  v-model:value="formState.reason"
                  :rows="4"
                  placeholder="请详细说明请假事由"
                  show-count
                  :max-length="500"
                />
              </a-form-item>

              <a-form-item label="附件（病假条等）">
                <a-upload
                  :before-upload="handleBeforeUpload"
                  @change="handleFileChange"
                  :file-list="fileList"
                  :max-count="3"
                >
                  <a-button>
                    <template #icon><UploadOutlined /></template>
                    点击上传
                  </a-button>
                </a-upload>
              </a-form-item>

              <a-form-item>
                <a-space style="width: 100%">
                  <a-button
                    type="primary"
                    html-type="submit"
                    class="submit-btn"
                    :loading="leaveStore.loading"
                    style="flex: 1"
                  >
                    <template #icon><SendOutlined /></template>
                    提交申请
                  </a-button>
                  <a-button
                    :loading="leaveStore.loading"
                    style="flex: 1"
                    @click="handleSaveDraft"
                  >
                    <template #icon><SaveOutlined /></template>
                    保存草稿
                  </a-button>
                </a-space>
              </a-form-item>
            </a-form>
          </a-card>
        </a-col>

        <a-col :xs="24" :md="14">
          <a-card class="main-card" :bordered="false">
            <div class="header-section">
              <div class="title-group">
                <h2 class="page-title">我的请假记录</h2>
                <p class="subtitle">My Leave Records</p>
              </div>
              <div class="filter-actions">
                <a-select
                  v-model:value="filterStatus"
                  placeholder="状态筛选"
                  allow-clear
                  style="width: 120px"
                  @change="fetchList"
                >
                  <a-select-option :value="0">待提交</a-select-option>
                  <a-select-option :value="1">审批中</a-select-option>
                  <a-select-option :value="2">已通过</a-select-option>
                  <a-select-option :value="3">已驳回</a-select-option>
                  <a-select-option :value="4">已撤销</a-select-option>
                </a-select>
              </div>
            </div>

            <a-table
              :columns="columns"
              :data-source="leaveStore.applicationList"
              :loading="leaveStore.loading"
              row-key="id"
              :pagination="paginationConfig"
              class="modern-table"
              :scroll="{ x: 900 }"
              @change="handleTableChange"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'leaveType'">
                  <a-tag :color="getLeaveTypeColor(record.leaveType)">
                    {{ record.leaveTypeName || getLeaveTypeName(record.leaveType) }}
                  </a-tag>
                </template>
                <template v-if="column.key === 'dateRange'">
                  <span>{{ record.startDate }} ~ {{ record.endDate }}</span>
                </template>
                <template v-if="column.key === 'totalDays'">
                  <span>{{ record.totalDays }} 天</span>
                </template>
                <template v-if="column.key === 'status'">
                  <a-tag :color="getStatusColor(record.status)">{{ record.statusName || getStatusText(record.status) }}</a-tag>
                </template>
                <template v-if="column.key === 'currentApprover'">
                  <span v-if="record.status === 1">{{ record.currentApproverName || '-' }}</span>
                  <span v-else>-</span>
                </template>
                <template v-if="column.key === 'action'">
                  <a-space>
                    <a-button type="link" size="small" @click="viewDetail(record)">
                      <EyeOutlined /> 详情
                    </a-button>
                    <a-button
                      v-if="record.status === 0"
                      type="link"
                      size="small"
                      @click="submitDraft(record)"
                    >
                      <SendOutlined /> 提交
                    </a-button>
                    <a-button
                      v-if="record.status === 0 || record.status === 1 || record.status === 2"
                      type="link"
                      size="small"
                      danger
                      @click="handleCancel(record)"
                    >
                      <CloseOutlined /> 撤销
                    </a-button>
                  </a-space>
                </template>
              </template>
            </a-table>
          </a-card>
        </a-col>
      </a-row>
    </div>

    <a-modal
      v-model:open="detailModalOpen"
      title="请假详情"
      :width="700"
      :footer="null"
    >
      <div v-if="leaveStore.currentApplication" class="detail-content">
        <a-descriptions :column="2" bordered size="small">
          <a-descriptions-item label="申请单号">{{ leaveStore.currentApplication.applicationNo }}</a-descriptions-item>
          <a-descriptions-item label="申请人">{{ leaveStore.currentApplication.employeeName }}</a-descriptions-item>
          <a-descriptions-item v-if="leaveStore.currentApplication.proxyEmployeeName" label="代申请人" :span="2">
            <a-tag color="purple">{{ leaveStore.currentApplication.proxyEmployeeName }}</a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="部门">{{ leaveStore.currentApplication.departmentName }}</a-descriptions-item>
          <a-descriptions-item label="假期类型">
            <a-tag :color="getLeaveTypeColor(leaveStore.currentApplication.leaveType)">
              {{ leaveStore.currentApplication.leaveTypeName }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="请假时间" :span="2">
            {{ leaveStore.currentApplication.startDate }} ~ {{ leaveStore.currentApplication.endDate }}
            ({{ leaveStore.currentApplication.totalDays }}天)
          </a-descriptions-item>
          <a-descriptions-item label="当前状态">
            <a-tag :color="getStatusColor(leaveStore.currentApplication.status)">
              {{ leaveStore.currentApplication.statusName }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="当前审批人">{{ leaveStore.currentApplication.currentApproverName || '-' }}</a-descriptions-item>
          <a-descriptions-item label="请假事由" :span="2">{{ leaveStore.currentApplication.reason }}</a-descriptions-item>
        </a-descriptions>

        <div class="timeline-section">
          <h4>审批轨迹</h4>
          <a-steps
            v-if="leaveStore.currentApplication.approvalNodes && leaveStore.currentApplication.approvalNodes.length > 0"
            direction="vertical"
            :current="getCurrentStepIndex()"
            :status="getTimelineStatus()"
          >
            <a-step
              v-for="node in leaveStore.currentApplication.approvalNodes"
              :key="node.id"
              :title="node.nodeName"
              :description="getNodeDescription(node)"
              :status="getNodeStatus(node)"
              :sub-title="node.approvalTime"
            />
          </a-steps>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script lang="ts" setup>
import { ref, reactive, computed, onMounted, watch } from 'vue';
import { message, Modal } from 'ant-design-vue';
import type { UploadProps } from 'ant-design-vue';
import { useLeaveStore, type LeaveApplication } from '../stores/leave';
import { useEmployeeStore } from '../stores/employee';
import { useAuthStore } from '../stores/auth';
import {
  UploadOutlined,
  SendOutlined,
  SaveOutlined,
  EyeOutlined,
  CloseOutlined,
} from '@ant-design/icons-vue';

const leaveStore = useLeaveStore();
const employeeStore = useEmployeeStore();
const authStore = useAuthStore();

const isProxyMode = computed(() => {
  if (!selectedEmployeeId.value || !authStore.userInfo?.employeeId) return false;
  return selectedEmployeeId.value !== authStore.userInfo.employeeId;
});

const formRef = ref();
const selectedEmployeeId = ref<number | undefined>();
const pageNum = ref(1);
const pageSize = ref(10);
const filterStatus = ref<number | undefined>();
const calculatedDays = ref(0);
const detailModalOpen = ref(false);
const fileList = ref<any[]>([]);

const formState = reactive({
  employeeId: 0,
  leaveType: undefined as number | undefined,
  startDate: '',
  endDate: '',
  startHalf: 0,
  endHalf: 0,
  reason: '',
  attachment: '',
});

const displayBalances = computed(() => {
  if (!selectedEmployeeId.value) return [];
  const balances = leaveStore.balanceList;
  if (balances.length > 0) return balances;
  return [
    { leaveType: 1, totalDays: 0, usedDays: 0, remainingDays: 0 },
    { leaveType: 4, totalDays: 0, usedDays: 0, remainingDays: 0 },
  ];
});

const paginationConfig = computed(() => ({
  current: pageNum.value,
  pageSize: pageSize.value,
  total: leaveStore.applicationListTotal,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条记录`,
}));

const columns = [
  { title: '申请单号', dataIndex: 'applicationNo', key: 'applicationNo', width: '15%' },
  { title: '假期类型', dataIndex: 'leaveType', key: 'leaveType', width: '10%' },
  { title: '请假时间', dataIndex: 'dateRange', key: 'dateRange', width: '25%' },
  { title: '天数', dataIndex: 'totalDays', key: 'totalDays', width: '8%' },
  { title: '状态', dataIndex: 'status', key: 'status', width: '10%' },
  { title: '当前审批人', dataIndex: 'currentApprover', key: 'currentApprover', width: '10%' },
  { title: '操作', dataIndex: 'action', key: 'action', width: '22%', fixed: 'right' as const },
];

const getLeaveTypeName = (type?: number) => {
  const map: Record<number, string> = { 1: '年假', 2: '事假', 3: '病假', 4: '调休' };
  return type ? map[type] || '-' : '-';
};

const getLeaveTypeColor = (type?: number) => {
  const map: Record<number, string> = { 1: 'blue', 2: 'orange', 3: 'red', 4: 'purple' };
  return type ? map[type] || 'default' : 'default';
};

const getStatusColor = (status?: number) => {
  const map: Record<number, string> = {
    0: 'default',
    1: 'processing',
    2: 'success',
    3: 'error',
    4: 'warning',
  };
  return status !== undefined ? map[status] || 'default' : 'default';
};

const getStatusText = (status?: number) => {
  const map: Record<number, string> = {
    0: '待提交',
    1: '审批中',
    2: '已通过',
    3: '已驳回',
    4: '已撤销',
  };
  return status !== undefined ? map[status] || '-' : '-';
};

const calculatePercent = (bal: any) => {
  if (!bal.totalDays || bal.totalDays === 0) return 0;
  return Math.min(100, Math.round(((bal.usedDays ?? 0) / bal.totalDays) * 100));
};

const getBalanceColor = (bal: any) => {
  const percent = calculatePercent(bal);
  if (percent >= 90) return '#f5222d';
  if (percent >= 70) return '#faad14';
  return '#52c41a';
};

const getNodeStatus = (node: any) => {
  const map: Record<number, string> = {
    0: 'process',
    1: 'finish',
    2: 'error',
    3: 'wait',
    4: 'wait',
  };
  return map[node.status ?? 0] || 'wait';
};

const getCurrentStepIndex = () => {
  const nodes = leaveStore.currentApplication?.approvalNodes || [];
  const idx = nodes.findIndex((n: any) => n.status === 0 || n.status === 3);
  return idx >= 0 ? idx : nodes.length;
};

const getTimelineStatus = () => {
  const status = leaveStore.currentApplication?.status;
  if (status === 3) return 'error';
  if (status === 2) return 'finish';
  return 'process';
};

const getNodeDescription = (node: any) => {
  let desc = `审批人：${node.approverName || '-'}`;
  if (node.statusName) {
    desc += `（${node.statusName}）`;
  }
  if (node.approvalRemark) {
    desc += `\n意见：${node.approvalRemark}`;
  }
  if (node.originalApproverName) {
    desc += `\n（转交自：${node.originalApproverName}）`;
  }
  if (node.addSignApproverName) {
    desc += `\n（加签人：${node.addSignApproverName}）`;
  }
  return desc;
};

const handleBeforeUpload: UploadProps['beforeUpload'] = (file) => {
  const isLt10M = file.size / 1024 / 1024 < 10;
  if (!isLt10M) {
    message.error('文件大小不能超过 10MB!');
  }
  return false;
};

const handleFileChange = (info: any) => {
  fileList.value = info.fileList;
};

const recalculateDays = async () => {
  if (!selectedEmployeeId.value || !formState.leaveType || !formState.startDate || !formState.endDate) {
    calculatedDays.value = 0;
    return;
  }
  calculatedDays.value = await leaveStore.calculateDays({
    employeeId: selectedEmployeeId.value,
    leaveType: formState.leaveType,
    startDate: formState.startDate,
    endDate: formState.endDate,
    startHalf: formState.startHalf,
    endHalf: formState.endHalf,
  });
};

const handleEmployeeChange = () => {
  pageNum.value = 1;
  if (selectedEmployeeId.value) {
    leaveStore.fetchBalance(selectedEmployeeId.value);
  }
  fetchList();
};

const handleTableChange = (pag: any) => {
  pageNum.value = pag.current;
  pageSize.value = pag.pageSize;
  fetchList();
};

const fetchList = () => {
  if (!selectedEmployeeId.value) return;
  leaveStore.fetchApplicationList({
    employeeId: selectedEmployeeId.value,
    status: filterStatus.value,
    pageNum: pageNum.value,
    pageSize: pageSize.value,
  });
};

const resetForm = () => {
  formState.leaveType = undefined;
  formState.startDate = '';
  formState.endDate = '';
  formState.startHalf = 0;
  formState.endHalf = 0;
  formState.reason = '';
  formState.attachment = '';
  calculatedDays.value = 0;
  fileList.value = [];
  formRef.value?.resetFields();
};

const validateForm = async () => {
  if (!selectedEmployeeId.value) {
    message.warning('请先选择员工');
    return false;
  }
  try {
    await formRef.value.validate();
    if (calculatedDays.value <= 0) {
      message.warning('请假天数必须大于0');
      return false;
    }
    return true;
  } catch {
    return false;
  }
};

const handleSubmit = async () => {
  if (!(await validateForm())) return;
  formState.employeeId = selectedEmployeeId.value!;
  const draft = await leaveStore.createDraft(formState as any);
  if (draft && draft.id) {
    const result = await leaveStore.submitApplication(draft.id);
    if (result) {
      resetForm();
      pageNum.value = 1;
      fetchList();
      if (selectedEmployeeId.value) {
        leaveStore.fetchBalance(selectedEmployeeId.value);
      }
    }
  }
};

const handleSaveDraft = async () => {
  if (!(await validateForm())) return;
  formState.employeeId = selectedEmployeeId.value!;
  const result = await leaveStore.createDraft(formState as any);
  if (result) {
    resetForm();
    pageNum.value = 1;
    fetchList();
  }
};

const submitDraft = async (record: LeaveApplication) => {
  if (!record.id) return;
  Modal.confirm({
    title: '确认提交',
    content: '确定要提交这份请假申请吗？',
    okText: '确定',
    cancelText: '取消',
    onOk: async () => {
      const result = await leaveStore.submitApplication(record.id!);
      if (result) {
        fetchList();
        if (selectedEmployeeId.value) {
          leaveStore.fetchBalance(selectedEmployeeId.value);
        }
      }
    },
  });
};

const handleCancel = async (record: LeaveApplication) => {
  if (!record.id) return;
  Modal.confirm({
    title: '确认撤销',
    content: '确定要撤销这份请假申请吗？',
    okText: '确定',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      const result = await leaveStore.cancelApplication(record.id!);
      if (result) {
        fetchList();
        if (selectedEmployeeId.value) {
          leaveStore.fetchBalance(selectedEmployeeId.value);
        }
      }
    },
  });
};

const viewDetail = async (record: LeaveApplication) => {
  if (!record.id) return;
  await leaveStore.fetchApplicationDetail(record.id);
  detailModalOpen.value = true;
};

watch(
  () => [formState.leaveType, formState.startDate, formState.endDate, formState.startHalf, formState.endHalf],
  () => {
    recalculateDays();
  }
);

watch(
  () => employeeStore.employees,
  (emps) => {
    if (emps.length > 0 && !selectedEmployeeId.value) {
      if (authStore.userInfo?.employeeId) {
        const employeeId = authStore.userInfo.employeeId;
        const match = emps.find((e: any) => e.id === employeeId);
        if (match?.id) {
          selectedEmployeeId.value = match.id;
        }
      }
      if (!selectedEmployeeId.value) {
        selectedEmployeeId.value = emps[0]!.id;
      }
      handleEmployeeChange();
    }
  },
  { immediate: true }
);

onMounted(async () => {
  await employeeStore.fetchEmployees();
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
  max-width: 1600px;
}

.main-card {
  border-radius: 14px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
  transition: all 0.3s ease;
  overflow: hidden;
  margin-bottom: 24px;
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

.submit-btn {
  height: 44px;
  border-radius: 22px;
  font-weight: 600;
  font-size: 15px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  box-shadow: 0 4px 12px rgba(118, 75, 162, 0.3);
  transition: all 0.3s ease;
}

.submit-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 18px rgba(118, 75, 162, 0.4);
}

.balance-section {
  margin-bottom: 24px;
  padding: 16px;
  background: #fafafa;
  border-radius: 12px;
}

.balance-card {
  background: white;
  padding: 16px;
  border-radius: 10px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.balance-name {
  font-size: 13px;
  color: #8c8c8c;
  margin-bottom: 8px;
}

.balance-value {
  margin-bottom: 12px;
}

.balance-value .remaining {
  font-size: 28px;
  font-weight: 700;
  color: #1a1a1a;
}

.balance-value .total {
  font-size: 14px;
  color: #8c8c8c;
}

.filter-actions {
  display: flex;
  gap: 12px;
}

.detail-content {
  padding: 8px 0;
}

.timeline-section {
  margin-top: 24px;
}

.timeline-section h4 {
  margin: 0 0 16px 0;
  font-size: 16px;
  font-weight: 600;
}

.modern-form :deep(.ant-form-item-label > label) {
  font-weight: 500;
  color: #2c3e50;
}

.modern-table :deep(.ant-table-thead > tr > th) {
  background: #fafafa;
  font-weight: 600;
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

  .header-actions,
  .filter-actions {
    width: 100%;
  }

  .header-actions .ant-select,
  .filter-actions .ant-select {
    width: 100% !important;
  }
}
</style>
