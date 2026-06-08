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
  proxyEmployeeId?: number;
  proxyEmployeeName?: string;
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

export interface LeaveApprovalConfigItem {
  id?: number;
  leaveType?: number;
  minDays?: number;
  maxDays?: number;
  departmentId?: number | null;
  nodeIndex?: number;
  nodeName?: string;
  approverRole?: string;
  approverId?: number | null;
  skipCondition?: string;
  enabled?: number;
  createTime?: string;
  updateTime?: string;
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
    approvalConfigList: [] as LeaveApprovalConfigItem[],
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

    async fetchMyApprovals(approverId?: number, pageNum = 1, pageSize = 10) {
      this.loading = true;
      try {
        const params: any = { pageNum, pageSize };
        if (approverId) params.approverId = approverId;
        const res = await request.get<any, Result<PageResult<LeaveApplication>>>(
          `${API_URL}/my-approvals`,
          { params }
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

    async fetchApprovalConfigs(params?: { leaveType?: number; departmentId?: number }) {
      this.loading = true;
      try {
        const res = await request.get<any, Result<LeaveApprovalConfigItem[]>>(`${API_URL}/config/list`, { params });
        this.approvalConfigList = res.data;
        return res.data;
      } catch (error) {
        return [];
      } finally {
        this.loading = false;
      }
    },

    async createApprovalConfig(data: LeaveApprovalConfigItem): Promise<LeaveApprovalConfigItem | null> {
      this.loading = true;
      try {
        const res = await request.post<any, Result<LeaveApprovalConfigItem>>(`${API_URL}/config`, data);
        message.success('创建成功');
        return res.data;
      } catch (error) {
        return null;
      } finally {
        this.loading = false;
      }
    },

    async updateApprovalConfig(data: LeaveApprovalConfigItem): Promise<LeaveApprovalConfigItem | null> {
      this.loading = true;
      try {
        const res = await request.put<any, Result<LeaveApprovalConfigItem>>(`${API_URL}/config`, data);
        message.success('更新成功');
        return res.data;
      } catch (error) {
        return null;
      } finally {
        this.loading = false;
      }
    },

    async deleteApprovalConfig(id: number): Promise<boolean> {
      this.loading = true;
      try {
        await request.delete<any, Result<void>>(`${API_URL}/config/${id}`);
        message.success('删除成功');
        return true;
      } catch (error) {
        return false;
      } finally {
        this.loading = false;
      }
    },
  },
});
