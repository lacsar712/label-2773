<template>
  <div class="page-container">
    <div class="content-wrapper">
      <a-card class="main-card" :bordered="false">
        <div class="header-section">
          <div class="title-group">
            <h1 class="page-title">考勤记录</h1>
            <p class="subtitle">Attendance Records</p>
          </div>
          <div class="header-actions">
            <a-button type="primary" class="export-btn" @click="handleExport" :disabled="!canExport">
              <template #icon><download-outlined /></template>
              导出
            </a-button>
          </div>
        </div>

        <a-form layout="inline" class="filter-form" @finish="handleSearch">
          <a-form-item v-permission="['ADMIN', 'HR']" label="员工">
            <a-select
              v-model:value="filterEmployeeId"
              placeholder="选择员工"
              style="width: 200px"
              allow-clear
              :loading="employeeStore.loading"
            >
              <a-select-option
                v-for="emp in employeeStore.employees"
                :key="emp.id"
                :value="emp.id"
              >
                {{ emp.name }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="日期范围">
            <a-range-picker
              v-model:value="dateRange"
              value-format="YYYY-MM-DD"
              style="width: 280px"
            />
          </a-form-item>
          <a-form-item label="状态">
            <a-select
              v-model:value="filterStatus"
              allow-clear
              placeholder="全部状态"
              style="width: 160px"
            >
              <a-select-option :value="0">正常</a-select-option>
              <a-select-option :value="1">迟到</a-select-option>
              <a-select-option :value="2">早退</a-select-option>
              <a-select-option :value="3">缺卡</a-select-option>
              <a-select-option :value="5">请假</a-select-option>
              <a-select-option :value="6">出差</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item>
            <a-space>
              <a-button type="primary" html-type="submit" class="search-btn">
                <template #icon><search-outlined /></template>
                查询
              </a-button>
              <a-button @click="handleReset">重置</a-button>
            </a-space>
          </a-form-item>
        </a-form>

        <a-table
          :columns="columns"
          :data-source="attendanceStore.records"
          :loading="attendanceStore.loading"
          row-key="id"
          :pagination="paginationConfig"
          class="modern-table"
          :scroll="{ x: 1300 }"
          @change="handleTableChange"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'attendanceDate'">
              <span>{{ record.attendanceDate }}</span>
            </template>
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
            <template v-if="column.key === 'checkInTime'">
              <span>{{ formatTime(record.checkInTime) }}</span>
            </template>
            <template v-if="column.key === 'checkOutTime'">
              <span>{{ formatTime(record.checkOutTime) }}</span>
            </template>
            <template v-if="column.key === 'workMinutes'">
              <span class="text-primary">{{ record.workMinutes ? record.workMinutes + ' 分钟' : '-' }}</span>
            </template>
            <template v-if="column.key === 'lateMinutes'">
              <span :class="{ 'text-warning': record.lateMinutes && record.lateMinutes > 0 }">
                {{ record.lateMinutes ? record.lateMinutes + ' 分钟' : '-' }}
              </span>
            </template>
            <template v-if="column.key === 'earlyMinutes'">
              <span :class="{ 'text-warning': record.earlyMinutes && record.earlyMinutes > 0 }">
                {{ record.earlyMinutes ? record.earlyMinutes + ' 分钟' : '-' }}
              </span>
            </template>
            <template v-if="column.key === 'status'">
              <a-tag :color="getStatusColor(record.status)">{{ getStatusText(record.status) }}</a-tag>
            </template>
            <template v-if="column.key === 'location'">
              <span class="location-text">
                {{ record.checkInLocation || '-' }}
                <template v-if="record.checkInLocation && record.checkOutLocation && record.checkInLocation !== record.checkOutLocation">
                  <br />{{ record.checkOutLocation }}
                </template>
              </span>
            </template>
          </template>
        </a-table>
      </a-card>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, computed, onMounted, watch } from 'vue';
import dayjs from 'dayjs';
import { useAttendanceStore, type QueryParams } from '../stores/attendance';
import { useEmployeeStore } from '../stores/employee';
import { useAuthStore } from '../stores/auth';
import { SearchOutlined, DownloadOutlined } from '@ant-design/icons-vue';

const attendanceStore = useAttendanceStore();
const employeeStore = useEmployeeStore();
const authStore = useAuthStore();

const filterEmployeeId = ref<number | undefined>();
const dateRange = ref<string[]>([]);
const filterStatus = ref<number | undefined>();
const pageNum = ref(1);
const pageSize = ref(10);

const canExport = computed(() => authStore.hasRole(['ADMIN', 'HR']));

const paginationConfig = computed(() => ({
  current: pageNum.value,
  pageSize: pageSize.value,
  total: attendanceStore.recordsTotal,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条记录`,
}));

const columns = [
  { title: '日期', dataIndex: 'attendanceDate', key: 'attendanceDate', width: '10%', fixed: 'left' as const },
  { title: '员工', dataIndex: 'employeeName', key: 'employeeName', width: '12%' },
  { title: '部门', dataIndex: 'departmentName', key: 'departmentName', width: '10%' },
  { title: '上班时间', dataIndex: 'checkInTime', key: 'checkInTime', width: '10%' },
  { title: '下班时间', dataIndex: 'checkOutTime', key: 'checkOutTime', width: '10%' },
  { title: '工作时长', dataIndex: 'workMinutes', key: 'workMinutes', width: '10%' },
  { title: '迟到分钟', dataIndex: 'lateMinutes', key: 'lateMinutes', width: '9%' },
  { title: '早退分钟', dataIndex: 'earlyMinutes', key: 'earlyMinutes', width: '9%' },
  { title: '状态', dataIndex: 'status', key: 'status', width: '8%' },
  { title: '地点', dataIndex: 'location', key: 'location', width: '12%' },
];

const formatTime = (time?: string) => {
  if (!time) return '-';
  return dayjs(time).format('HH:mm:ss');
};

const getStatusColor = (status?: number) => {
  switch (status) {
    case 0: return 'green';
    case 1:
    case 2:
    case 4: return 'orange';
    case 3: return 'red';
    case 5: return 'blue';
    case 6: return 'cyan';
    default: return 'default';
  }
};

const getStatusText = (status?: number) => {
  switch (status) {
    case 0: return '正常';
    case 1: return '迟到';
    case 2: return '早退';
    case 3: return '缺卡';
    case 4: return '迟到早退';
    case 5: return '请假';
    case 6: return '出差';
    default: return '-';
  }
};

const getQueryParams = (): QueryParams => {
  const params: QueryParams = {
    pageNum: pageNum.value,
    pageSize: pageSize.value,
  };
  if (filterEmployeeId.value) {
    params.employeeId = filterEmployeeId.value;
  }
  if (dateRange.value?.[0]) {
    params.startDate = dateRange.value[0];
  }
  if (dateRange.value?.[1]) {
    params.endDate = dateRange.value[1];
  }
  if (filterStatus.value !== undefined) {
    params.status = filterStatus.value;
  }
  return params;
};

const handleSearch = () => {
  pageNum.value = 1;
  fetchData();
};

const handleTableChange = (pag: any) => {
  pageNum.value = pag.current;
  pageSize.value = pag.pageSize;
  fetchData();
};

const fetchData = () => {
  const params = getQueryParams();
  if (canExport.value && params.employeeId) {
    attendanceStore.fetchRecords(params);
  } else if (canExport.value && !params.employeeId) {
    attendanceStore.fetchRecords(params);
  } else if (filterEmployeeId.value) {
    attendanceStore.fetchMyRecords({ ...params, employeeId: filterEmployeeId.value });
  }
};

const handleReset = () => {
  filterEmployeeId.value = undefined;
  dateRange.value = [];
  filterStatus.value = undefined;
  pageNum.value = 1;
  fetchData();
};

const handleExport = () => {
  const params = getQueryParams();
  attendanceStore.exportRecords(params);
};

watch(
  () => employeeStore.employees,
  (emps) => {
    if (emps.length > 0 && !filterEmployeeId.value && !canExport.value) {
      filterEmployeeId.value = emps[0].id;
      fetchData();
    }
  },
  { immediate: true }
);

onMounted(async () => {
  await employeeStore.fetchEmployees();
  if (canExport.value) {
    fetchData();
  }
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

.export-btn,
.search-btn {
  height: 36px;
  padding: 0 20px;
  border-radius: 18px;
  font-weight: 600;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  box-shadow: 0 4px 12px rgba(118, 75, 162, 0.3);
  transition: all 0.3s ease;
}

.export-btn:hover,
.search-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 18px rgba(118, 75, 162, 0.4);
}

.filter-form {
  margin-bottom: 24px;
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

.text-primary {
  color: #1890ff;
  font-weight: 500;
}

.text-warning {
  color: #fa8c16;
  font-weight: 500;
}

.location-text {
  font-size: 12px;
  color: #595959;
  line-height: 1.5;
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

  .filter-form :deep(.ant-form-item) {
    margin-bottom: 12px;
  }
}
</style>
