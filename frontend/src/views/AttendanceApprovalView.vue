<template>
  <div class="page-container">
    <div class="content-wrapper">
      <a-card class="main-card" :bordered="false">
        <div class="header-section">
          <div class="title-group">
            <h1 class="page-title">补卡审批</h1>
            <p class="subtitle">Make-up Approval</p>
          </div>
          <div class="header-actions">
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
          </div>
        </div>

        <a-table
          :columns="columns"
          :data-source="attendanceStore.pendingApprovalList"
          :loading="attendanceStore.loading || deptStore.loading"
          row-key="id"
          :pagination="paginationConfig"
          class="modern-table"
          :scroll="{ x: 1100 }"
          @change="handleTableChange"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'employeeName'">
              <div class="user-info">
                <a-avatar :style="{ backgroundColor: '#7265e6', verticalAlign: 'middle' }" size="small">
                  {{ record.employeeName?.charAt(0) }}
                </a-avatar>
                <span class="user-name">{{ record.employeeName }}</span>
              </div>
            </template>
            <template v-if="column.key === 'departmentName'">
              <a-tag color="blue">{{ record.departmentName || '-' }}</a-tag>
            </template>
            <template v-if="column.key === 'attendanceDate'">
              <span>{{ record.attendanceDate }}</span>
            </template>
            <template v-if="column.key === 'makeupType'">
              <a-tag :color="record.makeupType === 1 ? 'green' : 'orange'">
                {{ record.makeupType === 1 ? '上班补卡' : '下班补卡' }}
              </a-tag>
            </template>
            <template v-if="column.key === 'makeupTime'">
              <span>{{ formatDateTime(record.makeupTime) }}</span>
            </template>
            <template v-if="column.key === 'reason'">
              <span class="reason-text">{{ record.reason }}</span>
            </template>
            <template v-if="column.key === 'createTime'">
              <span>{{ formatDateTime(record.createTime) }}</span>
            </template>
            <template v-if="column.key === 'action'">
              <a-space size="small">
                <a-popconfirm
                  title="确认通过该补卡申请?"
                  ok-text="确定"
                  cancel-text="取消"
                  placement="topRight"
                  @confirm="handleApprove(record)"
                >
                  <a-button type="link" class="action-btn approve">通过</a-button>
                </a-popconfirm>
                <a-divider type="vertical" />
                <a-button type="link" danger class="action-btn reject" @click="showRejectModal(record)">
                  拒绝
                </a-button>
              </a-space>
            </template>
          </template>
        </a-table>
      </a-card>

      <a-modal
        v-model:open="rejectVisible"
        title="拒绝补卡申请"
        @ok="handleRejectOk"
        ok-text="确认拒绝"
        cancel-text="取消"
        centered
        :mask-style="{ backdropFilter: 'blur(4px)' }"
        class="modern-modal"
      >
        <a-form ref="rejectFormRef" :model="rejectFormState" layout="vertical">
          <a-form-item label="拒绝原因" name="approvalRemark" :rules="[{ required: true, message: '请输入拒绝原因!' }]">
            <a-textarea
              v-model:value="rejectFormState.approvalRemark"
              :rows="4"
              placeholder="请输入拒绝原因"
              show-count
              :max-length="200"
            />
          </a-form-item>
        </a-form>
      </a-modal>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, reactive, computed, onMounted } from 'vue';
import dayjs from 'dayjs';
import { useAttendanceStore, type AttendanceMakeUp, type MakeUpApproval } from '../stores/attendance';
import { useDepartmentStore } from '../stores/department';

const attendanceStore = useAttendanceStore();
const deptStore = useDepartmentStore();

const rejectVisible = ref(false);
const rejectFormRef = ref();
const currentRecord = ref<AttendanceMakeUp | null>(null);
const deptFilterId = ref<number | null>(null);
const pageNum = ref(1);
const pageSize = ref(10);

const rejectFormState = reactive({
  approvalRemark: '',
});

const deptFilterOptions = computed(() => {
  return [{ id: null as any, name: '全部部门', children: deptStore.departmentsTree }];
});

const paginationConfig = computed(() => ({
  current: pageNum.value,
  pageSize: pageSize.value,
  total: attendanceStore.pendingApprovalTotal,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条待审批`,
}));

const columns = [
  { title: '申请人', dataIndex: 'employeeName', key: 'employeeName', width: '12%' },
  { title: '部门', dataIndex: 'departmentName', key: 'departmentName', width: '10%' },
  { title: '补卡日期', dataIndex: 'attendanceDate', key: 'attendanceDate', width: '11%' },
  { title: '类型', dataIndex: 'makeupType', key: 'makeupType', width: '9%' },
  { title: '补卡时间', dataIndex: 'makeupTime', key: 'makeupTime', width: '13%' },
  { title: '补卡原因', dataIndex: 'reason', key: 'reason', width: '22%' },
  { title: '提交时间', dataIndex: 'createTime', key: 'createTime', width: '13%' },
  { title: '操作', key: 'action', width: '10%', align: 'center' as const, fixed: 'right' as const },
];

const formatDateTime = (time?: string) => {
  if (!time) return '-';
  return dayjs(time).format('YYYY-MM-DD HH:mm');
};

const handleDeptFilterChange = () => {
  pageNum.value = 1;
  fetchPendingApprovals();
};

const handleTableChange = (pag: any) => {
  pageNum.value = pag.current;
  pageSize.value = pag.pageSize;
  fetchPendingApprovals();
};

const fetchPendingApprovals = () => {
  attendanceStore.fetchPendingApprovals({
    departmentId: deptFilterId.value || undefined,
    pageNum: pageNum.value,
    pageSize: pageSize.value,
  });
};

const handleApprove = async (record: AttendanceMakeUp) => {
  if (!record.id) return;
  const data: MakeUpApproval = {
    id: record.id,
    status: 1,
    approvalRemark: undefined,
  };
  await attendanceStore.approveMakeUp(data);
  fetchPendingApprovals();
};

const showRejectModal = (record: AttendanceMakeUp) => {
  currentRecord.value = record;
  rejectFormState.approvalRemark = '';
  rejectVisible.value = true;
};

const handleRejectOk = async () => {
  try {
    await rejectFormRef.value.validate();
    if (!currentRecord.value?.id) return;
    const data: MakeUpApproval = {
      id: currentRecord.value.id,
      status: 2,
      approvalRemark: rejectFormState.approvalRemark,
    };
    await attendanceStore.approveMakeUp(data);
    rejectVisible.value = false;
    fetchPendingApprovals();
  } catch (error) {
  }
};

onMounted(async () => {
  await deptStore.refreshAll();
  fetchPendingApprovals();
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

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-name {
  font-weight: 500;
  color: #2c3e50;
}

.reason-text {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  color: #595959;
}

.action-btn {
  padding: 0;
  font-weight: 500;
}

.action-btn.approve {
  color: #52c41a;
}

.action-btn.reject {
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

  .header-actions .ant-tree-select {
    width: 100% !important;
  }
}
</style>
