<template>
  <div class="page-container">
    <div class="content-wrapper">
      <div class="header-section">
        <div class="title-group">
          <h1 class="page-title">考勤打卡</h1>
          <p class="subtitle">记录每日出勤，查看考勤状态</p>
        </div>
      </div>

      <a-card class="main-card">
        <div class="employee-selector">
          <a-form layout="inline">
            <a-form-item label="选择员工">
              <a-select
                v-model:value="selectedEmployeeId"
                style="width: 260px"
                :disabled="isEmployeeRole || !employeeStore.employees.length"
                placeholder="请选择员工"
                show-search
                :filter-option="(input, option) => (option?.children as string)?.toLowerCase().includes(input.toLowerCase())"
                @change="handleEmployeeChange"
              >
                <a-select-option
                  v-for="emp in employeeStore.employees"
                  :key="emp.id"
                  :value="emp.id"
                >
                  {{ emp.name }} - {{ emp.departmentName || '未分配部门' }}
                </a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item v-if="locationLoading">
              <a-tag color="processing">
                <template #icon><loading-outlined /></template>
                正在获取定位...
              </a-tag>
            </a-form-item>
            <a-form-item v-else-if="gpsLocation">
              <a-tag color="green">
                <template #icon><environment-outlined /></template>
                GPS: {{ gpsLocation }}
              </a-tag>
            </a-form-item>
            <a-form-item v-else-if="ipAddress">
              <a-tag color="blue">
                <template #icon><global-outlined /></template>
                IP: {{ ipAddress }}
              </a-tag>
            </a-form-item>
            <a-form-item v-else-if="!locationLoading">
              <a-tag color="orange">
                <template #icon><warning-outlined /></template>
                未获取定位，将记录网络IP
              </a-tag>
            </a-form-item>
          </a-form>
        </div>

        <a-row :gutter="[24, 24]">
          <a-col :xs="24" :md="10">
            <div class="clock-section">
              <a-card class="clock-card">
                <div class="clock-display">
                  <div class="clock-time">{{ currentTime }}</div>
                  <div class="clock-date">{{ currentDate }}</div>
                </div>
              </a-card>

              <a-card class="status-card" :title="'今日打卡状态'">
                <div class="status-grid">
                  <div class="status-item">
                    <span class="status-label">上班时间</span>
                    <span class="status-value">
                      {{ formatTime(attendanceStore.todayRecord?.checkInTime) || '-' }}
                    </span>
                  </div>
                  <div class="status-item">
                    <span class="status-label">上班地点</span>
                    <span class="status-value">
                      {{ formatLocation(attendanceStore.todayRecord?.checkInLocation, attendanceStore.todayRecord?.checkInIp) || '-' }}
                    </span>
                  </div>
                  <div class="status-item">
                    <span class="status-label">下班时间</span>
                    <span class="status-value">
                      {{ formatTime(attendanceStore.todayRecord?.checkOutTime) || '-' }}
                    </span>
                  </div>
                  <div class="status-item">
                    <span class="status-label">下班地点</span>
                    <span class="status-value">
                      {{ formatLocation(attendanceStore.todayRecord?.checkOutLocation, attendanceStore.todayRecord?.checkOutIp) || '-' }}
                    </span>
                  </div>
                  <div class="status-item full-width">
                    <span class="status-label">工作时长</span>
                    <span class="status-value">
                      {{ attendanceStore.todayRecord?.workMinutes ? formatWorkMinutes(attendanceStore.todayRecord.workMinutes) : '-' }}
                    </span>
                  </div>
                  <div class="status-item full-width">
                    <span class="status-label">今日状态</span>
                    <span class="status-value">
                      <a-tag v-if="todayStatus" :color="getStatusColor(attendanceStore.todayRecord?.status)">
                        {{ getStatusText(attendanceStore.todayRecord?.status) }}
                        <span v-if="attendanceStore.todayRecord?.lateMinutes > 0" style="margin-left: 4px;">
                          (迟到{{ attendanceStore.todayRecord.lateMinutes }}分钟)
                        </span>
                        <span v-if="attendanceStore.todayRecord?.earlyMinutes > 0" style="margin-left: 4px;">
                          (早退{{ attendanceStore.todayRecord.earlyMinutes }}分钟)
                        </span>
                      </a-tag>
                      <a-tag v-else color="default">未打卡</a-tag>
                    </span>
                  </div>
                </div>
              </a-card>

              <div class="clock-buttons">
                <a-space size="large">
                  <a-button
                    type="primary"
                    size="large"
                    class="clock-btn clock-in-btn"
                    :loading="attendanceStore.loading"
                    :disabled="!selectedEmployeeId"
                    @click="handleClockIn"
                  >
                    <template #icon><login-outlined /></template>
                    上班打卡
                  </a-button>
                  <a-button
                    size="large"
                    class="clock-btn clock-out-btn"
                    :loading="attendanceStore.loading"
                    :disabled="!selectedEmployeeId"
                    @click="handleClockOut"
                  >
                    <template #icon><logout-outlined /></template>
                    下班打卡
                  </a-button>
                </a-space>
              </div>
            </div>
          </a-col>

          <a-col :xs="24" :md="14">
            <a-calendar
              v-model:value="selectedDate"
              @panelChange="handlePanelChange"
              class="attendance-calendar"
            >
              <template #dateFullCell="{ value }">
                <div class="calendar-cell">
                  <div class="cell-day">{{ value.date() }}</div>
                  <div
                    v-if="getCalendarDotColor(value)"
                    class="cell-status-dot"
                    :style="{ backgroundColor: getCalendarDotColor(value) }"
                  ></div>
                </div>
              </template>
            </a-calendar>

            <div class="calendar-legend">
              <div class="legend-item">
                <span class="legend-dot" style="background-color: #52c41a"></span>
                <span class="legend-text">正常</span>
              </div>
              <div class="legend-item">
                <span class="legend-dot" style="background-color: #fa8c16"></span>
                <span class="legend-text">迟到/早退</span>
              </div>
              <div class="legend-item">
                <span class="legend-dot" style="background-color: #ff4d4f"></span>
                <span class="legend-text">缺卡</span>
              </div>
              <div class="legend-item">
                <span class="legend-dot" style="background-color: #1890ff"></span>
                <span class="legend-text">请假/出差</span>
              </div>
              <div class="legend-item">
                <span class="legend-dot" style="background-color: #bfbfbf"></span>
                <span class="legend-text">无记录</span>
              </div>
            </div>
          </a-col>
        </a-row>
      </a-card>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, onMounted, onUnmounted, computed, watch } from 'vue';
import dayjs from 'dayjs';
import { useAttendanceStore } from '../stores/attendance';
import { useEmployeeStore } from '../stores/employee';
import { useAuthStore } from '../stores/auth';
import {
  LoginOutlined,
  LogoutOutlined,
  LoadingOutlined,
  EnvironmentOutlined,
  GlobalOutlined,
  WarningOutlined,
} from '@ant-design/icons-vue';
import { message } from 'ant-design-vue';

const attendanceStore = useAttendanceStore();
const employeeStore = useEmployeeStore();
const authStore = useAuthStore();

const currentTime = ref('');
const currentDate = ref('');
const selectedDate = ref(dayjs());
const selectedEmployeeId = ref<number | undefined>();
let timer: number | null = null;

const gpsLocation = ref<string>('');
const gpsLng = ref<number | undefined>();
const gpsLat = ref<number | undefined>();
const ipAddress = ref<string>('');
const locationLoading = ref(false);

const isEmployeeRole = computed(() => {
  return authStore.hasRole('EMPLOYEE') && !authStore.hasRole(['ADMIN', 'HR']);
});

const todayStatus = computed(
  () =>
    attendanceStore.todayRecord?.status !== undefined &&
    attendanceStore.todayRecord?.status !== null
);

const updateTime = () => {
  const now = dayjs();
  currentTime.value = now.format('HH:mm:ss');
  currentDate.value = now.format('YYYY年MM月DD日 dddd');
};

const formatTime = (time?: string) => {
  if (!time) return '';
  return dayjs(time).format('HH:mm:ss');
};

const formatLocation = (location?: string, ip?: string) => {
  if (location) return location;
  if (ip) return `IP: ${ip}`;
  return '';
};

const formatWorkMinutes = (minutes: number) => {
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  if (h > 0) return `${h}小时${m}分钟`;
  return `${m}分钟`;
};

const getStatusColor = (status?: number) => {
  switch (status) {
    case 0:
      return 'green';
    case 1:
    case 2:
    case 4:
      return 'orange';
    case 3:
      return 'red';
    case 5:
      return 'blue';
    case 6:
      return 'cyan';
    default:
      return 'default';
  }
};

const getStatusText = (status?: number) => {
  switch (status) {
    case 0:
      return '正常';
    case 1:
      return '迟到';
    case 2:
      return '早退';
    case 3:
      return '缺卡';
    case 4:
      return '迟到早退';
    case 5:
      return '请假';
    case 6:
      return '出差';
    default:
      return '-';
  }
};

const getCalendarDotColor = (value: dayjs.Dayjs): string | undefined => {
  const dateStr = value.format('YYYY-MM-DD');
  const item = attendanceStore.calendarData.find(
    (d) => (d.attendanceDate as unknown as string) === dateStr
  );
  if (!item || item.status === null || item.status === undefined) return undefined;
  switch (item.status as number) {
    case 0:
      return '#52c41a';
    case 1:
    case 2:
    case 4:
      return '#fa8c16';
    case 3:
      return '#ff4d4f';
    case 5:
    case 6:
      return '#1890ff';
    default:
      return '#bfbfbf';
  }
};

const getGpsLocation = () => {
  if (!('geolocation' in navigator)) {
    message.warning('浏览器不支持GPS定位');
    return;
  }
  locationLoading.value = true;
  navigator.geolocation.getCurrentPosition(
    (position) => {
      gpsLng.value = position.coords.longitude;
      gpsLat.value = position.coords.latitude;
      gpsLocation.value = `经度 ${position.coords.longitude.toFixed(5)}, 纬度 ${position.coords.latitude.toFixed(5)}`;
      locationLoading.value = false;
    },
    (error) => {
      console.warn('GPS定位获取失败:', error.message);
      locationLoading.value = false;
    },
    {
      enableHighAccuracy: true,
      timeout: 10000,
      maximumAge: 0,
    }
  );
};

const getIpAddress = async () => {
  try {
    const response = await fetch('https://api.ipify.org?format=json');
    if (response.ok) {
      const data = await response.json();
      ipAddress.value = data.ip;
    }
  } catch (e) {
    console.warn('获取公网IP失败');
  }
};

const handleEmployeeChange = () => {
  loadEmployeeData();
};

const handlePanelChange = (value: dayjs.Dayjs) => {
  if (selectedEmployeeId.value) {
    attendanceStore.fetchCalendarData(
      selectedEmployeeId.value,
      value.year(),
      value.month() + 1
    );
  }
};

const buildClockRequest = (clockType: number) => {
  const req = {
    employeeId: selectedEmployeeId.value as number,
    clockType,
    location: gpsLocation.value || undefined,
    lng: gpsLng.value,
    lat: gpsLat.value,
    ipAddress: ipAddress.value || undefined,
  };
  return req;
};

const handleClockIn = async () => {
  if (!selectedEmployeeId.value) return;
  await attendanceStore.clockIn(buildClockRequest(1));
};

const handleClockOut = async () => {
  if (!selectedEmployeeId.value) return;
  await attendanceStore.clockOut(buildClockRequest(2));
};

const loadEmployeeData = () => {
  if (!selectedEmployeeId.value) return;
  attendanceStore.fetchTodayRecord(selectedEmployeeId.value);
  attendanceStore.fetchCalendarData(
    selectedEmployeeId.value,
    selectedDate.value.year(),
    selectedDate.value.month() + 1
  );
};

const initSelectedEmployee = () => {
  if (isEmployeeRole.value) {
    const myEmployeeId = authStore.userInfo?.employeeId;
    if (myEmployeeId) {
      selectedEmployeeId.value = myEmployeeId;
      loadEmployeeData();
      return;
    }
  }
  if (employeeStore.employees.length > 0 && !selectedEmployeeId.value) {
    selectedEmployeeId.value = employeeStore.employees[0]!.id;
    loadEmployeeData();
  }
};

watch(
  () => employeeStore.employees,
  () => {
    initSelectedEmployee();
  },
  { immediate: true }
);

onMounted(async () => {
  updateTime();
  timer = window.setInterval(updateTime, 1000);
  getGpsLocation();
  getIpAddress();
  await employeeStore.fetchEmployees();
});

onUnmounted(() => {
  if (timer) {
    clearInterval(timer);
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

.employee-selector {
  padding: 0 8px 16px 8px;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 24px;
}

.clock-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.clock-card {
  border-radius: 12px;
  overflow: hidden;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
}

.clock-card :deep(.ant-card-body) {
  padding: 28px 20px;
}

.clock-display {
  text-align: center;
  color: #fff;
}

.clock-time {
  font-size: 52px;
  font-weight: 700;
  letter-spacing: 3px;
  font-variant-numeric: tabular-nums;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
  line-height: 1.2;
}

.clock-date {
  font-size: 15px;
  margin-top: 10px;
  opacity: 0.9;
}

.status-card {
  border-radius: 12px;
}

.status-card :deep(.ant-card-head) {
  border-bottom: 1px solid #f0f0f0;
  font-weight: 600;
}

.status-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px 20px;
}

.status-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.status-item.full-width {
  grid-column: 1 / -1;
}

.status-label {
  font-size: 12px;
  color: #8c8c8c;
}

.status-value {
  font-size: 15px;
  color: #262626;
  font-weight: 500;
  word-break: break-all;
}

.clock-buttons {
  display: flex;
  justify-content: center;
  padding: 8px 0;
}

.clock-btn {
  min-width: 160px;
  height: 52px;
  border-radius: 26px;
  font-size: 16px;
  font-weight: 600;
  border: none;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
}

.clock-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.15);
}

.clock-in-btn {
  background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
}

.clock-out-btn {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  color: #fff;
}

.attendance-calendar {
  border-radius: 12px;
  padding: 8px;
}

.calendar-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: 6px 0;
  min-height: 60px;
}

.cell-day {
  font-size: 14px;
  color: #262626;
}

.cell-status-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  margin-top: 2px;
}

.calendar-legend {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  justify-content: center;
  padding: 12px 8px 4px 8px;
  margin-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
}

.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.legend-text {
  font-size: 12px;
  color: #595959;
}
</style>
