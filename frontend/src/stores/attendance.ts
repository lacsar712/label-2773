import { defineStore } from 'pinia';
import request from '../utils/request';
import { message } from 'ant-design-vue';
import dayjs from 'dayjs';

const API_URL = '/attendance';

interface Result<T> {
  code: number;
  message: string;
  data: T;
}

export interface AttendanceRule {
  id?: number;
  ruleName?: string;
  ruleType?: number;
  workStartTime?: string;
  workEndTime?: string;
  flexStartTime?: string;
  flexEndTime?: string;
  workMinutes?: number;
  lateGraceMinutes?: number;
  earlyGraceMinutes?: number;
  allowedIpRanges?: string;
  allowedGpsRadius?: number;
  workLocationName?: string;
  workLocationLng?: number;
  workLocationLat?: number;
  enabled?: number;
  isDefault?: number;
  createTime?: string;
  updateTime?: string;
}

export interface AttendanceRecord {
  id?: number;
  employeeId?: number;
  employeeName?: string;
  departmentId?: number;
  departmentName?: string;
  attendanceDate?: string;
  checkInTime?: string;
  checkOutTime?: string;
  checkInIp?: string;
  checkOutIp?: string;
  checkInLocation?: string;
  checkOutLocation?: string;
  checkInLng?: number;
  checkInLat?: number;
  checkOutLng?: number;
  checkOutLat?: number;
  checkInType?: number;
  checkOutType?: number;
  makeupInId?: number;
  makeupOutId?: number;
  workMinutes?: number;
  lateMinutes?: number;
  earlyMinutes?: number;
  status?: number;
  exceptionFlag?: number;
  ruleId?: number;
  remark?: string;
  createTime?: string;
  updateTime?: string;
}

export interface AttendanceMakeUp {
  id?: number;
  employeeId?: number;
  employeeName?: string;
  departmentId?: number;
  departmentName?: string;
  attendanceDate?: string;
  makeupType?: number;
  makeupTime?: string;
  ipAddress?: string;
  location?: string;
  lng?: number;
  lat?: number;
  reason?: string;
  approverId?: number;
  approverName?: string;
  status?: number;
  approvalTime?: string;
  approvalRemark?: string;
  createTime?: string;
  updateTime?: string;
}

export interface AttendanceCalendarItem {
  attendanceDate?: string;
  status?: number | null;
  lateMinutes?: number;
  earlyMinutes?: number;
}

export interface AttendanceMonthlyReport {
  employeeId?: number;
  employeeName?: string;
  departmentId?: number;
  departmentName?: string;
  statYear?: number;
  statMonth?: number;
  workDays?: number;
  actualDays?: number;
  lateCount?: number;
  earlyCount?: number;
  absentCount?: number;
  exceptionCount?: number;
  exceptionRate?: number;
  totalWorkMinutes?: number;
  totalLateMinutes?: number;
  totalEarlyMinutes?: number;
}

export interface ClockInRequest {
  employeeId: number;
  clockType: number;
  ipAddress?: string;
  location?: string;
  lng?: number;
  lat?: number;
}

export interface ClockInResponse {
  success: boolean;
  message: string;
  record?: AttendanceRecord;
}

export interface MakeUpRequest {
  employeeId: number;
  attendanceDate: string;
  makeupType: number;
  makeupTime: string;
  reason: string;
  location?: string;
  ipAddress?: string;
}

export interface MakeUpApproval {
  id: number;
  status: number;
  approvalRemark?: string;
}

export interface QueryParams {
  employeeId?: number;
  departmentId?: number;
  startDate?: string;
  endDate?: string;
  status?: number;
  exceptionFlag?: number;
  year?: number;
  month?: number;
  pageNum?: number;
  pageSize?: number;
}

interface PageResult<T> {
  records: T[];
  total: number;
  size: number;
  current: number;
  pages: number;
}

export const useAttendanceStore = defineStore('attendance', {
  state: () => ({
    todayRecord: null as AttendanceRecord | null,
    calendarData: [] as AttendanceCalendarItem[],
    records: [] as AttendanceRecord[],
    recordsTotal: 0,
    makeupList: [] as AttendanceMakeUp[],
    makeupListTotal: 0,
    pendingApprovalList: [] as AttendanceMakeUp[],
    pendingApprovalTotal: 0,
    monthlyReports: [] as AttendanceMonthlyReport[],
    monthlyReportsTotal: 0,
    rules: [] as AttendanceRule[],
    loading: false,
  }),
  getters: {},
  actions: {
    async fetchTodayRecord(employeeId: number) {
      this.loading = true;
      try {
        const res = await request.get<any, Result<AttendanceRecord>>(
          `${API_URL}/record/today`,
          { params: { employeeId } }
        );
        this.todayRecord = res.data;
      } catch (error) {
      } finally {
        this.loading = false;
      }
    },
    async clockIn(data: ClockInRequest): Promise<ClockInResponse | null> {
      try {
        const res = await request.post<any, Result<ClockInResponse>>(
          `${API_URL}/clock-in`,
          data
        );
        if (res.data.success) {
          message.success(res.data.message);
        } else {
          message.warning(res.data.message);
        }
        if (res.data.record) {
          this.todayRecord = res.data.record;
        }
        return res.data;
      } catch (error) {
        return null;
      }
    },
    async clockOut(data: ClockInRequest): Promise<ClockInResponse | null> {
      try {
        const res = await request.post<any, Result<ClockInResponse>>(
          `${API_URL}/clock-out`,
          data
        );
        if (res.data.success) {
          message.success(res.data.message);
        } else {
          message.warning(res.data.message);
        }
        if (res.data.record) {
          this.todayRecord = res.data.record;
        }
        return res.data;
      } catch (error) {
        return null;
      }
    },
    async fetchCalendarData(employeeId: number, year: number, month: number) {
      this.loading = true;
      try {
        const res = await request.get<any, Result<AttendanceCalendarItem[]>>(
          `${API_URL}/calendar`,
          { params: { employeeId, year, month } }
        );
        this.calendarData = res.data;
      } catch (error) {
      } finally {
        this.loading = false;
      }
    },
    async fetchRecords(params: QueryParams) {
      this.loading = true;
      try {
        const res = await request.get<any, Result<PageResult<AttendanceRecord>>>(
          `${API_URL}/records`,
          { params }
        );
        this.records = res.data.records;
        this.recordsTotal = res.data.total;
      } catch (error) {
      } finally {
        this.loading = false;
      }
    },
    async fetchMyRecords(params: QueryParams) {
      this.loading = true;
      try {
        const res = await request.get<any, Result<PageResult<AttendanceRecord>>>(
          `${API_URL}/records/my`,
          { params }
        );
        this.records = res.data.records;
        this.recordsTotal = res.data.total;
      } catch (error) {
      } finally {
        this.loading = false;
      }
    },
    async exportRecords(params: QueryParams) {
      try {
        const res = await request.get<any, Blob>(`${API_URL}/records/export`, {
          params,
          responseType: 'blob',
        });
        const url = window.URL.createObjectURL(res);
        const link = document.createElement('a');
        link.href = url;
        link.download = `考勤记录_${dayjs().format('YYYYMMDD')}.csv`;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        window.URL.revokeObjectURL(url);
        message.success('导出成功');
      } catch (error) {
      }
    },
    async submitMakeUp(data: MakeUpRequest): Promise<AttendanceMakeUp | null> {
      try {
        const res = await request.post<any, Result<AttendanceMakeUp>>(`${API_URL}/makeup`, data);
        message.success('提交成功');
        if (data.employeeId) {
          await this.fetchMyMakeUpList({ employeeId: data.employeeId, pageNum: 1, pageSize: 10 });
        }
        return res.data;
      } catch (error) {
        return null;
      }
    },
    async fetchMyMakeUpList(params: QueryParams) {
      this.loading = true;
      try {
        const res = await request.get<any, Result<PageResult<AttendanceMakeUp>>>(
          `${API_URL}/makeup/my`,
          { params: { employeeId: params.employeeId, pageNum: params.pageNum || 1, pageSize: params.pageSize || 10 } }
        );
        this.makeupList = res.data.records;
        this.makeupListTotal = res.data.total;
      } catch (error) {
      } finally {
        this.loading = false;
      }
    },
    async fetchPendingApprovals(params: QueryParams) {
      this.loading = true;
      try {
        const res = await request.get<any, Result<PageResult<AttendanceMakeUp>>>(
          `${API_URL}/makeup/pending`,
          { params: { departmentId: params.departmentId, pageNum: params.pageNum || 1, pageSize: params.pageSize || 10 } }
        );
        this.pendingApprovalList = res.data.records;
        this.pendingApprovalTotal = res.data.total;
      } catch (error) {
      } finally {
        this.loading = false;
      }
    },
    async approveMakeUp(data: MakeUpApproval): Promise<AttendanceMakeUp | null> {
      try {
        const res = await request.post<any, Result<AttendanceMakeUp>>(
          `${API_URL}/makeup/approve`,
          data
        );
        message.success(data.status === 1 ? '审批通过' : '已拒绝');
        return res.data;
      } catch (error) {
        return null;
      }
    },
    async fetchMonthlyReports(params: QueryParams) {
      this.loading = true;
      try {
        const res = await request.get<any, Result<PageResult<AttendanceMonthlyReport>>>(
          `${API_URL}/monthly/reports`,
          { params }
        );
        this.monthlyReports = res.data.records;
        this.monthlyReportsTotal = res.data.total;
      } catch (error) {
      } finally {
        this.loading = false;
      }
    },
    async exportMonthlyReports(params: QueryParams) {
      try {
        const res = await request.get<any, Blob>(
          `${API_URL}/monthly/export`,
          { params, responseType: 'blob' }
        );
        const url = window.URL.createObjectURL(res);
        const link = document.createElement('a');
        link.href = url;
        link.download = `月度考勤报表_${dayjs().format('YYYYMMDD')}.csv`;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        window.URL.revokeObjectURL(url);
        message.success('导出成功');
      } catch (error) {
      }
    },
    async fetchRules() {
      this.loading = true;
      try {
        const res = await request.get<any, Result<AttendanceRule[]>>(
          `${API_URL}/rules`
        );
        this.rules = res.data;
      } catch (error) {
      } finally {
        this.loading = false;
      }
    },
  },
});
