import { defineStore } from 'pinia';
import request from '../utils/request';
import { message } from 'ant-design-vue';

const API_URL = '/leave';

interface Result<T> {
  code: number;
  message: string;
  data: T;
}

export interface LeaveBalance {
  id?: number;
  employeeId?: number;
  employeeName?: string;
  leaveType?: number;
  totalDays?: number;
  usedDays?: number;
  remainingDays?: number;
  year?: number;
  remark?: string;
  createTime?: string;
  updateTime?: string;
}

export interface LeaveApprovalNode {
  id?: number;
  applicationId?: number;
  nodeIndex?: number;
  nodeName?: string;
  approverId?: number;
  approverName?: string;
  nodeType?: number;
  status?: number;
  statusName?: string;
  approvalRemark?: string;
  approvalTime?: string;
  originalApproverId?: number;
  originalApproverName?: string;
  addSignApproverId?: number;
  addSignApproverName?: string;
  createTime?: string;
  updateTime?: string;
}

export interface LeaveApplication {
  id?: number;
  applicationNo?: string;
  employeeId?: number;
  employeeName?: string;
  departmentId?: number;
  departmentName?: string;
  leaveType?: number;
  leaveTypeName?: string;
  startDate?: string;
  endDate?: string;
  startHalf?: number;
  endHalf?: number;
  totalDays?: number;
  reason?: string;
  attachment?: string;
  status?: number;
  statusName?: string;
  currentNodeIndex?: number;
  currentApproverId?: number;
  currentApproverName?: string;
  submitTime?: string;
  approvalTime?: string;
  createTime?: string;
  updateTime?: string;
  approvalNodes?: LeaveApprovalNode[];
}

export interface LeaveApplyRequest {
  employeeId: number;
  leaveType: number;
  startDate: string;
  endDate: string;
  startHalf?: number;
  endHalf?: number;
  reason: string;
  attachment?: string;
}

export interface LeaveApprovalRequest {
  applicationId: number;
  status: number;
  approvalRemark?: string;
  transferToApproverId?: number;
  transferToApproverName?: string;
  addSignApproverId?: number;
  addSignApproverName?: string;
}

export interface LeaveQueryParams {
  employeeId?: number;
  approverId?: number;
  departmentId?: number;
  leaveType?: number;
  status?: number;
  startDate?: string;
  endDate?: string;
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

export const useLeaveStore = defineStore('leave', {
  state: () => ({
    applicationList: [] as LeaveApplication[],
    applicationListTotal: 0,
    myApprovalList: [] as LeaveApplication[],
    myApprovalTotal: 0,
    currentApplication: null as LeaveApplication | null,
    balanceList: [] as LeaveBalance[],
    loading: false,
  }),
  getters: {},
  actions: {
    async calculateDays(params: {
      employeeId: number;
      leaveType: number;
      startDate: string;
      endDate: string;
      startHalf?: number;
      endHalf?: number;
    }): Promise<number> {
      try {
        const res = await request.post<any, Result<{ workDays: number }>>(
          `${API_URL}/calculate-days`,
          params
        );
        return res.data.workDays;
      } catch (error) {
        return 0;
      }
    },

    async createDraft(data: LeaveApplyRequest): Promise<LeaveApplication | null> {
      this.loading = true;
      try {
        const res = await request.post<any, Result<LeaveApplication>>(`${API_URL}/draft`, data);
        message.success('草稿保存成功');
        return res.data;
      } catch (error) {
        return null;
      } finally {
        this.loading = false;
      }
    },

    async submitApplication(id: number): Promise<LeaveApplication | null> {
      this.loading = true;
      try {
        const res = await request.post<any, Result<LeaveApplication>>(`${API_URL}/submit/${id}`);
        message.success('提交成功');
        return res.data;
      } catch (error) {
        return null;
      } finally {
        this.loading = false;
      }
    },

    async approveApplication(data: LeaveApprovalRequest): Promise<LeaveApplication | null> {
      this.loading = true;
      try {
        const res = await request.post<any, Result<LeaveApplication>>(`${API_URL}/approve`, data);
        const actionText = data.transferToApproverId
          ? '转交成功'
          : data.addSignApproverId
          ? '加签成功'
          : data.status === 1
          ? '审批通过'
          : '已驳回';
        message.success(actionText);
        return res.data;
      } catch (error) {
        return null;
      } finally {
        this.loading = false;
      }
    },

    async cancelApplication(id: number): Promise<LeaveApplication | null> {
      this.loading = true;
      try {
        const res = await request.post<any, Result<LeaveApplication>>(`${API_URL}/cancel/${id}`);
        message.success('撤销成功');
        return res.data;
      } catch (error) {
        return null;
      } finally {
        this.loading = false;
      }
    },

    async fetchApplicationDetail(id: number) {
      this.loading = true;
      try {
        const res = await request.get<any, Result<LeaveApplication>>(`${API_URL}/${id}`);
        this.currentApplication = res.data;
        return res.data;
      } catch (error) {
        return null;
      } finally {
        this.loading = false;
      }
    },

    async fetchApplicationList(params: LeaveQueryParams) {
      this.loading = true;
      try {
        const res = await request.get<any, Result<PageResult<LeaveApplication>>>(
          `${API_URL}/list`,
          { params }
        );
        this.applicationList = res.data.records;
        this.applicationListTotal = res.data.total;
      } catch (error) {
      } finally {
        this.loading = false;
      }
    },

    async fetchMyApprovals(approverId: number, pageNum = 1, pageSize = 10) {
      this.loading = true;
      try {
        const res = await request.get<any, Result<PageResult<LeaveApplication>>>(
          `${API_URL}/my-approvals`,
          { params: { approverId, pageNum, pageSize } }
        );
        this.myApprovalList = res.data.records;
        this.myApprovalTotal = res.data.total;
      } catch (error) {
      } finally {
        this.loading = false;
      }
    },

    async fetchBalance(employeeId: number, year?: number) {
      this.loading = true;
      try {
        const res = await request.get<any, Result<LeaveBalance[]>>(`${API_URL}/balance/${employeeId}`, {
          params: { year },
        });
        this.balanceList = res.data;
        return res.data;
      } catch (error) {
        return [];
      } finally {
        this.loading = false;
      }
    },
  },
});
