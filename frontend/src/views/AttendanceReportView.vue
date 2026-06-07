<template>
  <div class="page-container">
    <div class="content-wrapper">
      <a-card class="main-card" :bordered="false">
        <div class="header-section">
          <div class="title-group">
            <h1 class="page-title">月度考勤报表</h1>
            <p class="subtitle">Monthly Attendance Report</p>
          </div>
          <div class="header-actions">
            <a-button type="primary" class="export-btn" @click="handleExport">
              <template #icon><download-outlined /></template>
              导出报表
            </a-button>
          </div>
        </div>

        <a-form layout="inline" class="filter-form" @finish="handleSearch">
          <a-form-item label="员工">
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
          <a-form-item label="部门">
            <a-tree-select
              v-model:value="filterDepartmentId"
              :tree-data="deptSelectOptions"
              :field-names="{ label: 'name', value: 'id', children: 'children' }"
              placeholder="选择部门"
              allow-clear
              tree-default-expand-all
              style="width: 220px"
            />
          </a-form-item>
          <a-form-item label="年月">
            <a-month-picker
              v-model:value="reportMonth"
              format="YYYY年MM月"
              value-format="YYYY-MM"
              style="width: 180px"
            />
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
          :data-source="attendanceStore.monthlyReports"
          :loading="attendanceStore.loading || employeeStore.loading || deptStore.loading"
          row-key="id"
          :pagination="paginationConfig"
          class="modern-table"
          :scroll="{ x: 1300 }"
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
            <template v-if="column.key === 'statPeriod'">
              <span>{{ record.statYear }}年{{ record.statMonth }}月</span>
            </template>
            <template v-if="column.key === 'workDays'">
              <span class="text-default">{{ record.workDays ?? 0 }}</span>
            </template>
            <template v-if="column.key === 'actualDays'">
              <span class="text-primary">{{ record.actualDays ?? 0 }}</span>
            </template>
            <template v-if="column.key === 'lateCount'">
              <span :class="{ 'text-warning': (record.lateCount ?? 0) > 0 }">{{ record.lateCount ?? 0 }}</span>
            </template>
            <template v-if="column.key === 'earlyCount'">
              <span :class="{ 'text-warning': (record.earlyCount ?? 0) > 0 }">{{ record.earlyCount ?? 0 }}</span>
            </template>
            <template v-if="column.key === 'absentCount'">
              <span :class="{ 'text-danger': (record.absentCount ?? 0) > 0 }">{{ record.absentCount ?? 0 }}</span>
            </template>
            <template v-if="column.key === 'exceptionCount'">
              <span :class="{ 'text-warning': (record.exceptionCount ?? 0) > 0 }">{{ record.exceptionCount ?? 0 }}</span>
            </template>
            <template v-if="column.key === 'exceptionRate'">
              <a-progress
                :percent="Number(record.exceptionRate ?? 0) * 100"
                :show-info="false"
                size="small"
                :stroke-color="getRateColor(record.exceptionRate)"
                style="width: 100px; display: inline-block; vertical-align: middle"
              />
              <span class="rate-text" :style="{ color: getRateColor(record.exceptionRate) }">
                {{ (Number(record.exceptionRate ?? 0) * 100).toFixed(2) }}%
              </span>
            </template>
          </template>
        </a-table>
      </a-card>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, computed, onMounted } from 'vue';
import dayjs from 'dayjs';
import { useAttendanceStore, type QueryParams } from '../stores/attendance';
import { useEmployeeStore } from '../stores/employee';
import { useDepartmentStore } from '../stores/department';
import { SearchOutlined, DownloadOutlined } from '@ant-design/icons-vue';

const attendanceStore = useAttendanceStore();
const employeeStore = useEmployeeStore();
const deptStore = useDepartmentStore();

const filterEmployeeId = ref<number | undefined>();
const filterDepartmentId = ref<number | null>(null);
const reportMonth = ref(dayjs().format('YYYY-MM'));
const pageNum = ref(1);
const pageSize = ref(10);

const deptSelectOptions = computed(() => {
  const filterEnabled = (items: any[]): any[] => {
    return items
      .filter((d) => d.enabled)
      .map((d) => ({
        ...d,
        children: d.children ? filterEnabled(d.children) : undefined,
      }));
  };
  return [{ id: null as any, name: '全部部门', children: filterEnabled(deptStore.departmentsTree) }];
});

const paginationConfig = computed(() => ({
  current: pageNum.value,
  pageSize: pageSize.value,
  total: attendanceStore.monthlyReportsTotal,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条记录`,
}));

const columns = [
  { title: '员工', dataIndex: 'employeeName', key: 'employeeName', width: '12%', fixed: 'left' as const },
  { title: '部门', dataIndex: 'departmentName', key: 'departmentName', width: '12%' },
  { title: '年份', dataIndex: 'statYear', key: 'statYear', width: '7%' },
  { title: '月份', dataIndex: 'statMonth', key: 'statMonth', width: '7%' },
  { title: '应出勤天数', dataIndex: 'workDays', key: 'workDays', width: '9%' },
  { title: '实际出勤天数', dataIndex: 'actualDays', key: 'actualDays', width: '10%' },
  { title: '迟到次数', dataIndex: 'lateCount', key: 'lateCount', width: '8%' },
  { title: '早退次数', dataIndex: 'earlyCount', key: 'earlyCount', width: '8%' },
  { title: '缺卡次数', dataIndex: 'absentCount', key: 'absentCount', width: '8%' },
  { title: '异常次数', dataIndex: 'exceptionCount', key: 'exceptionCount', width: '8%' },
  { title: '异常率(%)', dataIndex: 'exceptionRate', key: 'exceptionRate', width: '11%', fixed: 'right' as const },
];

const getRateColor = (rate?: number) => {
  const r = Number(rate ?? 0);
  if (r === 0) return '#52c41a';
  if (r <= 0.05) return '#1890ff';
  if (r <= 0.1) return '#faad14';
  return '#ff4d4f';
};

const getQueryParams = (): QueryParams => {
  const params: QueryParams = {
    pageNum: pageNum.value,
    pageSize: pageSize.value,
  };
  if (filterEmployeeId.value) {
    params.employeeId = filterEmployeeId.value;
  }
  if (filterDepartmentId.value) {
    params.departmentId = filterDepartmentId.value;
  }
  if (reportMonth.value) {
    const [year, month] = reportMonth.value.split('-').map(Number);
    params.year = year;
    params.month = month;
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
  attendanceStore.fetchMonthlyReports(params);
};

const handleReset = () => {
  filterEmployeeId.value = undefined;
  filterDepartmentId.value = null;
  reportMonth.value = dayjs().format('YYYY-MM');
  pageNum.value = 1;
  fetchData();
};

const handleExport = () => {
  const params = getQueryParams();
  attendanceStore.exportMonthlyReports(params);
};

onMounted(async () => {
  await Promise.all([
    employeeStore.fetchEmployees(),
    deptStore.refreshAll(),
  ]);
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

.text-default {
  color: #8c8c8c;
}

.text-primary {
  color: #1890ff;
  font-weight: 600;
}

.text-warning {
  color: #fa8c16;
  font-weight: 600;
}

.text-danger {
  color: #ff4d4f;
  font-weight: 600;
}

.rate-text {
  margin-left: 8px;
  font-weight: 600;
  font-size: 13px;
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
