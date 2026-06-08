<template>
  <div class="page-container">
    <div class="content-wrapper">
      <a-card class="main-card" :bordered="false">
        <div class="header-section">
          <div class="title-group">
            <h1 class="page-title">请假审批</h1>
            <p class="subtitle">Leave Approval</p>
          </div>
          <div class="filter-actions">
            <a-select
              v-model:value="filterStatus"
              placeholder="状态筛选"
              allow-clear
              style="width: 120px"
              @change="fetchList"
            >
              <a-select-option :value="1">审批中</a-select-option>
              <a-select-option :value="2">已通过</a-select-option>
              <a-select-option :value="3">已驳回</a-select-option>
              <a-select-option :value="4">已撤销</a-select-option>
            </a-select>
          </div>
        </div>

        <a-table
          :columns="columns"
          :data-source="approvalList"
          :loading="leaveStore.loading"
          row-key="id"
          :pagination="paginationConfig"
          class="modern-table"
          :scroll="{ x: 1200 }"
          @change="handleTableChange"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'applicant'">
              <div class="applicant-info">
                <a-avatar :style="{ backgroundColor: '#7265e6' }" size="small">
                  {{ record.employeeName?.charAt(0) }}
                </a-avatar>
                <div class="applicant-name">{{ record.employeeName }}</div>
              </div>
            </template>
            <template v-if="column.key === 'leaveType'">
              <a-tag :color="getLeaveTypeColor(record.leaveType)">
                {{ record.leaveTypeName || getLeaveTypeName(record.leaveType) }}
              </a-tag>
            </template>
            <template v-if="column.key === 'dateRange'">
              <span>{{ record.startDate }} ~ {{ record.endDate }}</span>
              <div class="days-badge">{{ record.totalDays }}天</div>
            </template>
            <template v-if="column.key === 'status'">
              <a-tag :color="getStatusColor(record.status)">
                {{ record.statusName || getStatusText(record.status) }}
              </a-tag>
            </template>
            <template v-if="column.key === 'action'">
              <a-space>
                <a-button type="link" size="small" @click="viewDetail(record)">
                  <EyeOutlined /> 详情
                </a-button>
                <a-button
                  v-if="record.status === 1 && isMyApproval(record)"
                  type="link"
                  size="small"
                  @click="openApprovalModal(record, 1)"
                >
                  <CheckOutlined /> 通过
                </a-button>
                <a-button
                  v-if="record.status === 1 && isMyApproval(record)"
                  type="link"
                  size="small"
                  danger
                  @click="openApprovalModal(record, 2)"
                >
                  <CloseOutlined /> 驳回
                </a-button>
                <a-button
                  v-if="record.status === 1 && isMyApproval(record)"
                  type="link"
                  size="small"
                  @click="openTransferModal(record)"
                >
                  <SwapOutlined /> 转交
                </a-button>
                <a-button
                  v-if="record.status === 1 && isMyApproval(record)"
                  type="link"
                  size="small"
                  @click="openAddSignModal(record)"
                >
                  <UserAddOutlined /> 加签
                </a-button>
              </a-space>
            </template>
          </template>
        </a-table>
      </a-card>
    </div>

    <a-modal
      v-model:open="detailModalOpen"
      title="请假详情与审批"
      :width="800"
      :footer="null"
    >
      <div v-if="leaveStore.currentApplication" class="detail-content">
        <a-descriptions :column="2" bordered size="small">
          <a-descriptions-item label="申请单号">{{ leaveStore.currentApplication.applicationNo }}</a-descriptions-item>
          <a-descriptions-item label="申请人">{{ leaveStore.currentApplication.employeeName }}</a-descriptions-item>
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
          <a-descriptions-item label="当前审批人">
            {{ leaveStore.currentApplication.currentApproverName || '-' }}
          </a-descriptions-item>
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

        <div v-if="leaveStore.currentApplication.status === 1 && isMyApproval(leaveStore.currentApplication)" class="approval-actions">
          <a-divider />
          <h4>审批操作</h4>
          <a-form :model="approvalForm" layout="vertical">
            <a-form-item label="审批意见">
              <a-textarea
              v-model:value="approvalForm.approvalRemark"
              :rows="3"
              placeholder="请输入审批意见（选填）"
            />
            </a-form-item>
            <a-space>
              <a-button
                type="primary"
                @click="handleApprove(1)">
                <template #icon><CheckOutlined /></template>
                通过
              </a-button>
              <a-button danger @click="handleApprove(2)">
                <template #icon><CloseOutlined /></template>
                驳回
              </a-button>
              <a-button @click="openTransferModal(leaveStore.currentApplication)">
                <template #icon><SwapOutlined /></template>
                转交
              </a-button>
              <a-button @click="openAddSignModal(leaveStore.currentApplication)">
                <template #icon><UserAddOutlined /></template>
                加签
              </a-button>
            </a-space>
          </a-form>
        </div>
      </div>
    </a-modal>

    <a-modal
      v-model:open="transferModalOpen"
      title="转交审批"
      @ok="handleTransfer"
      :confirm-loading="leaveStore.loading"
    >
      <a-form :model="transferForm" layout="vertical">
        <a-form-item label="转交审批人" :rules="[{ required: true, message: '请选择转交对象' }]">
          <a-select
            v-model:value="transferForm.approverId"
            placeholder="请选择转交审批人"
            show-search
            :filter-option="filterEmployeeOption"
          >
            <a-select-option
              v-for="emp in employeeStore.employees"
              :key="emp.id"
              :value="emp.id"
            >
              {{ emp.name }} - {{ emp.role }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="转交说明">
          <a-textarea
            v-model:value="transferForm.remark"
            :rows="3"
            placeholder="请输入转交说明"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="addSignModalOpen"
      title="加签审批"
      @ok="handleAddSign"
      :confirm-loading="leaveStore.loading"
    >
      <a-form :model="addSignForm" layout="vertical">
        <a-form-item label="加签审批人" :rules="[{ required: true, message: '请选择加签对象' }]">
          <a-select
            v-model:value="addSignForm.approverId"
            placeholder="请选择加签审批人"
            show-search
            :filter-option="filterEmployeeOption"
          >
            <a-select-option
              v-for="emp in employeeStore.employees"
              :key="emp.id"
              :value="emp.id"
            >
              {{ emp.name }} - {{ emp.role }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="加签说明">
          <a-textarea
            v-model:value="addSignForm.remark"
            :rows="3"
            placeholder="请输入加签说明"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script lang="ts" setup>
import { ref, reactive, computed, onMounted } from 'vue';
import { message } from 'ant-design-vue';
import { useLeaveStore, type LeaveApplication } from '../stores/leave';
import { useEmployeeStore } from '../stores/employee';
import { useAuthStore } from '../stores/auth';
import {
  EyeOutlined,
  CheckOutlined,
  CloseOutlined,
  SwapOutlined,
  UserAddOutlined,
} from '@ant-design/icons-vue';

const leaveStore = useLeaveStore();
const employeeStore = useEmployeeStore();
const authStore = useAuthStore();

const pageNum = ref(1);
const pageSize = ref(10);
const filterStatus = ref<number | undefined>(1);
const detailModalOpen = ref(false);
const transferModalOpen = ref(false);
const addSignModalOpen = ref(false);

const currentRecord = ref<LeaveApplication | null>(null);

const approvalForm = reactive({
  approvalRemark: '',
});

const transferForm = reactive({
  approverId: undefined as number | undefined,
  remark: '',
});

const addSignForm = reactive({
  approverId: undefined as number | undefined,
  remark: '',
});

const approvalList = computed(() => {
  if (filterStatus.value === 1) {
    return leaveStore.myApprovalList;
  }
  return leaveStore.applicationList;
});

const paginationConfig = computed(() => ({
  current: pageNum.value,
  pageSize: pageSize.value,
  total: filterStatus.value === 1 ? leaveStore.myApprovalTotal : leaveStore.applicationListTotal,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条记录`,
}));

const columns = [
  { title: '申请单号', dataIndex: 'applicationNo', key: 'applicationNo', width: '14%' },
  { title: '申请人', dataIndex: 'applicant', key: 'applicant', width: '12%' },
  { title: '部门', dataIndex: 'departmentName', key: 'departmentName', width: '12%' },
  { title: '假期类型', dataIndex: 'leaveType', key: 'leaveType', width: '10%' },
  { title: '请假时间', dataIndex: 'dateRange', key: 'dateRange', width: '22%' },
  { title: '状态', dataIndex: 'status', key: 'status', width: '10%' },
  { title: '操作', dataIndex: 'action', key: 'action', width: '20%', fixed: 'right' as const },
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

const isMyApproval = (record: LeaveApplication) => {
  const approverId = authStore.userInfo?.employeeId;
  if (!approverId) return false;
  return record.currentApproverId === approverId;
};

const filterEmployeeOption = (input: string, option: any) => {
  const value = (option?.label || option?.children || '').toString();
  return value.toLowerCase().includes(input.toLowerCase());
};

const fetchList = () => {
  if (filterStatus.value === 1 || filterStatus.value === undefined) {
    const approverId = authStore.userInfo?.employeeId;
    if (approverId) {
      leaveStore.fetchMyApprovals(approverId, pageNum.value, pageSize.value);
    }
  } else {
    leaveStore.fetchApplicationList({
      status: filterStatus.value,
      pageNum: pageNum.value,
      pageSize: pageSize.value,
    });
  }
};

const handleTableChange = (pag: any) => {
  pageNum.value = pag.current;
  pageSize.value = pag.pageSize;
  fetchList();
};

const viewDetail = async (record: LeaveApplication) => {
  if (!record.id) return;
  await leaveStore.fetchApplicationDetail(record.id);
  approvalForm.approvalRemark = '';
  detailModalOpen.value = true;
};

const openApprovalModal = (record: LeaveApplication, status: number) => {
  currentRecord.value = record;
  approvalForm.approvalRemark = '';
  handleApprove(status);
};

const openTransferModal = async (record: LeaveApplication) => {
  currentRecord.value = record;
  transferForm.approverId = undefined;
  transferForm.remark = '';
  detailModalOpen.value = false;
  transferModalOpen.value = true;
};

const openAddSignModal = async (record: LeaveApplication) => {
  currentRecord.value = record;
  addSignForm.approverId = undefined;
  addSignForm.remark = '';
  detailModalOpen.value = false;
  addSignModalOpen.value = true;
};

const handleApprove = async (status: number) => {
  if (!currentRecord.value?.id) return;
  const result = await leaveStore.approveApplication({
    applicationId: currentRecord.value.id,
    status,
    approvalRemark: approvalForm.approvalRemark,
  });
  if (result) {
    detailModalOpen.value = false;
    approvalForm.approvalRemark = '';
    fetchList();
  }
};

const handleTransfer = async () => {
  if (!transferForm.approverId || !currentRecord.value?.id) {
    message.warning('请选择转交审批人');
    return;
  }
  const approver = employeeStore.employees.find((e: any) => e.id === transferForm.approverId);
  const result = await leaveStore.approveApplication({
    applicationId: currentRecord.value!.id!,
    status: 1,
    approvalRemark: transferForm.remark,
    transferToApproverId: transferForm.approverId,
    transferToApproverName: approver?.name,
  });
  if (result) {
    transferModalOpen.value = false;
    fetchList();
  }
};

const handleAddSign = async () => {
  if (!addSignForm.approverId || !currentRecord.value?.id) {
    message.warning('请选择加签审批人');
    return;
  }
  const approver = employeeStore.employees.find((e: any) => e.id === addSignForm.approverId);
  const result = await leaveStore.approveApplication({
    applicationId: currentRecord.value!.id!,
    status: 1,
    approvalRemark: addSignForm.remark,
    addSignApproverId: addSignForm.approverId,
    addSignApproverName: approver?.name,
  });
  if (result) {
    addSignModalOpen.value = false;
    fetchList();
  }
};

onMounted(async () => {
  await employeeStore.fetchEmployees();
  fetchList();
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

.filter-actions {
  display: flex;
  gap: 12px;
}

.applicant-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.applicant-name {
  font-weight: 500;
}

.days-badge {
  display: inline-block;
  margin-left: 8px;
  padding: 2px 8px;
  background: #f0f5ff;
  color: #2f54eb;
  border-radius: 10px;
  font-size: 12px;
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

.approval-actions {
  margin-top: 24px;
}

.approval-actions h4 {
  margin: 0 0 16px 0;
  font-size: 16px;
  font-weight: 600;
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
}
</style>
