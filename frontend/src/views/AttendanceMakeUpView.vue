<template>
  <div class="page-container">
    <div class="content-wrapper">
      <a-row :gutter="24">
        <a-col :xs="24" :md="10">
          <a-card class="main-card" :bordered="false">
            <div class="header-section">
              <div class="title-group">
                <h1 class="page-title">补卡申请</h1>
                <p class="subtitle">Make-up Application</p>
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

            <a-form ref="formRef" :model="formState" layout="vertical" class="modern-form" @finish="handleSubmit">
              <a-form-item label="补卡日期" name="attendanceDate" :rules="[{ required: true, message: '请选择补卡日期!' }]">
                <a-date-picker
                  v-model:value="formState.attendanceDate"
                  style="width: 100%"
                  format="YYYY-MM-DD"
                  value-format="YYYY-MM-DD"
                  :disabled-date="disabledDate"
                />
              </a-form-item>
              <a-form-item label="补卡类型" name="makeupType" :rules="[{ required: true, message: '请选择补卡类型!' }]">
                <a-select v-model:value="formState.makeupType" style="width: 100%" placeholder="请选择补卡类型">
                  <a-select-option :value="1">上班补卡</a-select-option>
                  <a-select-option :value="2">下班补卡</a-select-option>
                </a-select>
              </a-form-item>
              <a-form-item label="补卡时间" name="makeupTime" :rules="[{ required: true, message: '请选择补卡时间!' }]">
                <a-date-picker
                  v-model:value="formState.makeupTime"
                  show-time
                  style="width: 100%"
                  format="YYYY-MM-DD HH:mm:ss"
                  value-format="YYYY-MM-DD HH:mm:ss"
                />
              </a-form-item>
              <a-form-item label="补卡地点" name="location">
                <a-input v-model:value="formState.location" placeholder="请输入补卡地点（选填）" />
              </a-form-item>
              <a-form-item label="补卡原因" name="reason" :rules="[{ required: true, message: '请输入补卡原因!' }]">
                <a-textarea
                  v-model:value="formState.reason"
                  :rows="4"
                  placeholder="请详细说明补卡原因"
                  show-count
                  :max-length="200"
                />
              </a-form-item>
              <a-form-item>
                <a-button type="primary" html-type="submit" class="submit-btn" :loading="attendanceStore.loading" block>
                  <template #icon><form-outlined /></template>
                  提交申请
                </a-button>
              </a-form-item>
            </a-form>
          </a-card>
        </a-col>

        <a-col :xs="24" :md="14">
          <a-card class="main-card" :bordered="false">
            <div class="header-section">
              <div class="title-group">
                <h2 class="page-title">我的补卡申请</h2>
                <p class="subtitle">My Applications</p>
              </div>
            </div>

            <a-table
              :columns="columns"
              :data-source="attendanceStore.makeupList"
              :loading="attendanceStore.loading"
              row-key="id"
              :pagination="paginationConfig"
              class="modern-table"
              :scroll="{ x: 900 }"
              @change="handleTableChange"
            >
              <template #bodyCell="{ column, record }">
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
                <template v-if="column.key === 'status'">
                  <a-tag :color="getStatusColor(record.status)">{{ getStatusText(record.status) }}</a-tag>
                </template>
                <template v-if="column.key === 'approvalRemark'">
                  <span class="approval-text">{{ record.approvalRemark || '-' }}</span>
                </template>
                <template v-if="column.key === 'createTime'">
                  <span>{{ formatDateTime(record.createTime) }}</span>
                </template>
              </template>
            </a-table>
          </a-card>
        </a-col>
      </a-row>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, reactive, computed, onMounted, watch } from 'vue';
import dayjs from 'dayjs';
import { useAttendanceStore, type MakeUpRequest } from '../stores/attendance';
import { useEmployeeStore } from '../stores/employee';
import { FormOutlined } from '@ant-design/icons-vue';

const attendanceStore = useAttendanceStore();
const employeeStore = useEmployeeStore();

const formRef = ref();
const selectedEmployeeId = ref<number | undefined>();
const pageNum = ref(1);
const pageSize = ref(10);

const formState = reactive<MakeUpRequest>({
  employeeId: 0,
  attendanceDate: '',
  makeupType: 0 as any,
  makeupTime: '',
  reason: '',
  location: '',
});

const paginationConfig = computed(() => ({
  current: pageNum.value,
  pageSize: pageSize.value,
  total: attendanceStore.makeupListTotal,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条记录`,
}));

const columns = [
  { title: '补卡日期', dataIndex: 'attendanceDate', key: 'attendanceDate', width: '12%' },
  { title: '类型', dataIndex: 'makeupType', key: 'makeupType', width: '10%' },
  { title: '补卡时间', dataIndex: 'makeupTime', key: 'makeupTime', width: '15%' },
  { title: '原因', dataIndex: 'reason', key: 'reason', width: '23%' },
  { title: '状态', dataIndex: 'status', key: 'status', width: '10%' },
  { title: '审批意见', dataIndex: 'approvalRemark', key: 'approvalRemark', width: '15%' },
  { title: '提交时间', dataIndex: 'createTime', key: 'createTime', width: '15%' },
];

const disabledDate = (current: dayjs.Dayjs) => {
  return current && current > dayjs().endOf('day');
};

const formatDateTime = (time?: string) => {
  if (!time) return '-';
  return dayjs(time).format('YYYY-MM-DD HH:mm');
};

const getStatusColor = (status?: number) => {
  switch (status) {
    case 0: return 'orange';
    case 1: return 'green';
    case 2: return 'red';
    default: return 'default';
  }
};

const getStatusText = (status?: number) => {
  switch (status) {
    case 0: return '待审批';
    case 1: return '已通过';
    case 2: return '已拒绝';
    default: return '-';
  }
};

const resetForm = () => {
  formState.attendanceDate = '';
  formState.makeupType = 0 as any;
  formState.makeupTime = '';
  formState.reason = '';
  formState.location = '';
};

const handleEmployeeChange = () => {
  pageNum.value = 1;
  fetchMakeUpList();
};

const handleTableChange = (pag: any) => {
  pageNum.value = pag.current;
  pageSize.value = pag.pageSize;
  fetchMakeUpList();
};

const fetchMakeUpList = () => {
  if (!selectedEmployeeId.value) return;
  attendanceStore.fetchMyMakeUpList({
    employeeId: selectedEmployeeId.value,
    pageNum: pageNum.value,
    pageSize: pageSize.value,
  });
};

const handleSubmit = async () => {
  try {
    await formRef.value.validate();
    if (!selectedEmployeeId.value) return;
    formState.employeeId = selectedEmployeeId.value;
    const result = await attendanceStore.submitMakeUp(formState);
    if (result) {
      resetForm();
      pageNum.value = 1;
      fetchMakeUpList();
    }
  } catch (error) {
  }
};

watch(
  () => employeeStore.employees,
  (emps) => {
    if (emps.length > 0 && !selectedEmployeeId.value) {
      selectedEmployeeId.value = emps[0]!.id;
      fetchMakeUpList();
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
  max-width: 1400px;
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

.reason-text {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  color: #595959;
}

.approval-text {
  color: #8c8c8c;
  font-size: 12px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
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

  .header-actions .ant-select {
    width: 100% !important;
  }
}
</style>
