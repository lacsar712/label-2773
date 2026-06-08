<template>
  <div class="page-container">
    <div class="content-wrapper">
      <a-card class="main-card" :bordered="false">
        <div class="header-section">
          <div class="title-group">
            <h1 class="page-title">考勤打卡</h1>
            <p class="subtitle">Attendance Check-in</p>
          </div>
          <div class="header-actions">
            <a-select
              v-model:value="selectedEmployeeId"
              placeholder="选择员工"
              style="width: 240px"
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

        <a-row :gutter="24">
          <a-col :xs="24" :md="10">
            <div class="clock-section">
              <div class="current-time">
                <div class="date">{{ currentDate }}</div>
                <div class="time">{{ currentTime }}</div>
              </div>

              <a-card class="today-status-card" :bordered="false">
                <div class="status-header">
                  <span class="status-title">今日打卡状态</span>
                  <a-tag v-if="todayStatus" :color="getStatusColor(attendanceStore.todayRecord?.status)">
                    {{ getStatusText(attendanceStore.todayRecord?.status) }}
                  </a-tag>
                  <a-tag v-else color="default">未打卡</a-tag>
                </div>
                <a-divider />
                <div class="status-grid">
                  <div class="status-item">
                    <span class="status-label">上班打卡时间</span>
                    <span class="status-value">
                      {{ formatTime(attendanceStore.todayRecord?.checkInTime) || '未打卡' }}
                    </span>
                  </div>
                  <div class="status-item">
                    <span class="status-label">上班打卡地点</span>
                    <span class="status-value">
                      {{ attendanceStore.todayRecord?.checkInLocation || '-' }}
                    </span>
                  </div>
                  <div class="status-item">
                    <span class="status-label">下班打卡时间</span>
                    <span class="status-value">
                      {{ formatTime(attendanceStore.todayRecord?.checkOutTime) || '未打卡' }}
                    </span>
                  </div>
                  <div class="status-item">
                    <span class="status-label">下班打卡地点</span>
                    <span class="status-value">
                      {{ attendanceStore.todayRecord?.checkOutLocation || '-' }}
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

            <div class="legend">
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
import { LoginOutlined, LogoutOutlined } from '@ant-design/icons-vue';

const attendanceStore = useAttendanceStore();
const employeeStore = useEmployeeStore();

const currentTime = ref('');
const currentDate = ref('');
const selectedDate = ref(dayjs());
const selectedEmployeeId = ref<number | undefined>();
let timer: number | null = null;

const todayStatus = computed(() => attendanceStore.todayRecord?.status !== undefined && attendanceStore.todayRecord?.status !== null);

const updateTime = () => {
  const now = dayjs();
  currentTime.value = now.format('HH:mm:ss');
  currentDate.value = now.format('YYYY年MM月DD日 dddd');
};

const formatTime = (time?: string) => {
  if (!time) return '';
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

const getCalendarDotColor = (value: dayjs.Dayjs): string | undefined => {
  const dateStr = value.format('YYYY-MM-DD');
  const item = attendanceStore.calendarData.find((d) => d.attendanceDate === dateStr);
  if (!item || item.status === null || item.status === undefined) return undefined;
  switch (item.status) {
    case 0: return '#52c41a';
    case 1:
    case 2:
    case 4: return '#fa8c16';
    case 3: return '#ff4d4f';
    case 5:
    case 6: return '#1890ff';
    default: return '#bfbfbf';
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

const handleClockIn = async () => {
  if (!selectedEmployeeId.value) return;
  await attendanceStore.clockIn({
    employeeId: selectedEmployeeId.value,
    clockType: 1,
  });
};

const handleClockOut = async () => {
  if (!selectedEmployeeId.value) return;
  await attendanceStore.clockOut({
    employeeId: selectedEmployeeId.value,
    clockType: 2,
  });
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

watch(
  () => employeeStore.employees,
  (emps) => {
    if (emps.length > 0 && !selectedEmployeeId.value) {
      selectedEmployeeId.value = emps[0]!.id;
      loadEmployeeData();
    }
  },
  { immediate: true }
);

onMounted(async () => {
  updateTime();
  timer = window.setInterval(updateTime, 1000);
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

.clock-section {
  text-align: center;
  padding: 8px 0;
}

.current-time {
  margin-bottom: 24px;
}

.current-time .date {
  font-size: 16px;
  color: #8c8c8c;
  margin-bottom: 8px;
}

.current-time .time {
  font-size: 48px;
  font-weight: 700;
  color: #2c3e50;
  letter-spacing: 2px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.today-status-card {
  background: linear-gradient(135deg, #f8f9ff 0%, #fff5f5 100%);
  border-radius: 12px;
  margin-bottom: 28px;
  padding: 8px 16px;
}

.status-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.status-title {
  font-size: 15px;
  font-weight: 600;
  color: #2c3e50;
}

.status-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px 24px;
}

.status-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  text-align: left;
}

.status-label {
  font-size: 13px;
  color: #8c8c8c;
}

.status-value {
  font-size: 14px;
  font-weight: 500;
  color: #2c3e50;
}

.clock-buttons {
  display: flex;
  justify-content: center;
}

.clock-btn {
  min-width: 150px;
  height: 50px;
  border-radius: 25px;
  font-weight: 600;
  font-size: 16px;
}

.clock-in-btn {
  background: linear-gradient(135deg, #52c41a 0%, #389e0d 100%);
  border: none;
  box-shadow: 0 4px 12px rgba(82, 196, 26, 0.3);
  transition: all 0.3s ease;
}

.clock-in-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 18px rgba(82, 196, 26, 0.45);
}

.clock-out-btn {
  background: linear-gradient(135deg, #fa8c16 0%, #d46b08 100%);
  border: none;
  color: white;
  box-shadow: 0 4px 12px rgba(250, 140, 22, 0.3);
  transition: all 0.3s ease;
}

.clock-out-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 18px rgba(250, 140, 22, 0.45);
  color: white;
}

.attendance-calendar {
  background: #fafafa;
  border-radius: 12px;
  padding: 8px;
}

.calendar-cell {
  position: relative;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 4px 0;
}

.cell-day {
  font-size: 14px;
  color: #2c3e50;
}

.cell-status-dot {
  position: absolute;
  bottom: 6px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.legend {
  display: flex;
  justify-content: center;
  gap: 24px;
  margin-top: 16px;
  padding: 12px;
  background: #fafafa;
  border-radius: 8px;
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
  font-size: 13px;
  color: #595959;
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

  .current-time .time {
    font-size: 36px;
  }

  .status-grid {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .legend {
    flex-wrap: wrap;
    gap: 12px 20px;
  }
}
</style>
