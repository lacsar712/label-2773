import { defineStore } from 'pinia';
import request from '../utils/request';
import { message } from 'ant-design-vue';

const API_URL = '/salary';

interface Result<T> {
  code: number;
  message: string;
  data: T;
}

export interface SalaryTemplate {
  id?: number;
  templateName?: string;
  jobLevel?: string;
  baseSalary?: number;
  postAllowance?: number;
  performanceCoefficient?: number;
  performanceBonus?: number;
  socialInsurancePersonalRate?: number;
  socialInsuranceCompanyRate?: number;
  housingFundPersonalRate?: number;
  housingFundCompanyRate?: number;
  socialInsuranceBase?: number | null;
  description?: string;
  enabled?: number;
  createTime?: string;
  updateTime?: string;
}

export interface SalaryAdjustLog {
  id?: number;
  salaryRecordId?: number;
  employeeId?: number;
  employeeName?: string;
  fieldName?: string;
  fieldLabel?: string;
  oldValue?: number;
  newValue?: number;
  adjustReason?: string;
  operatorId?: number;
  operatorName?: string;
  createTime?: string;
}

export interface SalaryRecord {
  id?: number;
  recordNo?: string;
  employeeId?: number;
  employeeName?: string;
  departmentId?: number;
  departmentName?: string;
  jobLevel?: string;
  templateId?: number;
  salaryYear?: number;
  salaryMonth?: number;
  baseSalary?: number;
  postAllowance?: number;
  performanceCoefficient?: number;
  performanceBonus?: number;
  overtimePay?: number;
  otherAllowance?: number;
  socialInsurancePersonal?: number;
  socialInsuranceCompany?: number;
  housingFundPersonal?: number;
  housingFundCompany?: number;
  incomeTax?: number;
  otherDeduction?: number;
  grossSalary?: number;
  totalDeduction?: number;
  netSalary?: number;
  totalCompanyCost?: number;
  status?: number;
  statusName?: string;
  issueTime?: string;
  remark?: string;
  createTime?: string;
  updateTime?: string;
  adjustLogs?: SalaryAdjustLog[];
}

export interface SalaryCostSummary {
  salaryYear?: number;
  salaryMonth?: number;
  departmentId?: number;
  departmentName?: string;
  employeeCount?: number;
  totalBaseSalary?: number;
  totalPostAllowance?: number;
  totalPerformanceBonus?: number;
  totalOvertimePay?: number;
  totalGrossSalary?: number;
  totalSocialInsurancePersonal?: number;
  totalSocialInsuranceCompany?: number;
  totalHousingFundPersonal?: number;
  totalHousingFundCompany?: number;
  totalIncomeTax?: number;
  totalNetSalary?: number;
  totalCompanyCost?: number;
  avgNetSalary?: number;
  momRate?: number;
  yoyRate?: number;
}

export interface SalaryCostReport {
  salaryYear?: number;
  salaryMonth?: number;
  totalCompanyCost?: number;
  totalNetSalary?: number;
  totalEmployeeCount?: number;
  avgNetSalary?: number;
  momRate?: number;
  yoyRate?: number;
  departmentSummaries?: SalaryCostSummary[];
  monthlyTrend?: SalaryCostSummary[];
}

interface PageResult<T> {
  records: T[];
  total: number;
  size: number;
  current: number;
  pages: number;
}

export const useSalaryStore = defineStore('salary', {
  state: () => ({
    templates: [] as SalaryTemplate[],
    enabledTemplates: [] as SalaryTemplate[],
    recordList: [] as SalaryRecord[],
    recordListTotal: 0,
    currentRecord: null as SalaryRecord | null,
    myRecordList: [] as SalaryRecord[],
    myRecordListTotal: 0,
    myCurrentRecord: null as SalaryRecord | null,
    adjustLogs: [] as SalaryAdjustLog[],
    costReport: null as SalaryCostReport | null,
    fieldLabels: [] as { field: string; label: string }[],
    loading: false,
  }),
  getters: {},
  actions: {
    async fetchTemplates(keyword?: string) {
      this.loading = true;
      try {
        const res = await request.get<any, Result<SalaryTemplate[]>>(`${API_URL}/template/list`, {
          params: { keyword },
        });
        this.templates = res.data;
        return res.data;
      } catch (e) {
        return [];
      } finally {
        this.loading = false;
      }
    },

    async fetchEnabledTemplates() {
      try {
        const res = await request.get<any, Result<SalaryTemplate[]>>(`${API_URL}/template/enabled`);
        this.enabledTemplates = res.data;
        return res.data;
      } catch (e) {
        return [];
      }
    },

    async createTemplate(data: SalaryTemplate) {
      this.loading = true;
      try {
        const res = await request.post<any, Result<SalaryTemplate>>(`${API_URL}/template`, data);
        message.success('模板创建成功');
        return res.data;
      } catch (e) {
        return null;
      } finally {
        this.loading = false;
      }
    },

    async updateTemplate(data: SalaryTemplate) {
      this.loading = true;
      try {
        const res = await request.put<any, Result<SalaryTemplate>>(`${API_URL}/template`, data);
        message.success('模板更新成功');
        return res.data;
      } catch (e) {
        return null;
      } finally {
        this.loading = false;
      }
    },

    async toggleTemplate(id: number) {
      try {
        await request.post<any, Result<void>>(`${API_URL}/template/toggle/${id}`);
        message.success('状态已切换');
      } catch (e) {}
    },

    async batchGenerate(params: {
      salaryYear: number;
      salaryMonth: number;
      departmentId?: number;
      employeeIds?: number[];
      defaultTemplateId?: number;
    }) {
      this.loading = true;
      try {
        const res = await request.post<any, Result<SalaryRecord[]>>(
          `${API_URL}/batch-generate`,
          params
        );
        message.success(`已生成 ${res.data.length} 条薪资记录`);
        return res.data;
      } catch (e) {
        return [];
      } finally {
        this.loading = false;
      }
    },

    async applyTemplate(params: {
      templateId: number;
      employeeIds?: number[];
      departmentId?: number;
      salaryYear: number;
      salaryMonth: number;
    }) {
      this.loading = true;
      try {
        await request.post<any, Result<void>>(`${API_URL}/apply-template`, params);
        message.success('模板套用成功');
        return true;
      } catch (e) {
        return false;
      } finally {
        this.loading = false;
      }
    },

    async fetchRecords(params: {
      employeeId?: number;
      departmentId?: number;
      salaryYear?: number;
      salaryMonth?: number;
      status?: number;
      keyword?: string;
      pageNum?: number;
      pageSize?: number;
    }) {
      this.loading = true;
      try {
        const res = await request.get<any, Result<PageResult<SalaryRecord>>>(
          `${API_URL}/records`,
          { params }
        );
        this.recordList = res.data.records;
        this.recordListTotal = res.data.total;
        return res.data;
      } catch (e) {
        return null;
      } finally {
        this.loading = false;
      }
    },

    async fetchRecordDetail(id: number) {
      this.loading = true;
      try {
        const res = await request.get<any, Result<SalaryRecord>>(`${API_URL}/record/${id}`);
        this.currentRecord = res.data;
        return res.data;
      } catch (e) {
        return null;
      } finally {
        this.loading = false;
      }
    },

    async adjustField(params: {
      salaryRecordId: number;
      fieldName: string;
      fieldLabel: string;
      oldValue?: number;
      newValue: number;
      adjustReason?: string;
    }) {
      this.loading = true;
      try {
        const res = await request.post<any, Result<SalaryRecord>>(`${API_URL}/adjust`, params);
        message.success('调整成功');
        this.currentRecord = res.data;
        return res.data;
      } catch (e) {
        return null;
      } finally {
        this.loading = false;
      }
    },

    async confirmRecord(id: number) {
      try {
        const res = await request.post<any, Result<SalaryRecord>>(`${API_URL}/confirm/${id}`);
        message.success('已确认');
        return res.data;
      } catch (e) {
        return null;
      }
    },

    async batchConfirm(ids: number[]) {
      this.loading = true;
      try {
        const res = await request.post<any, Result<SalaryRecord[]>>(
          `${API_URL}/batch-confirm`,
          ids
        );
        message.success(`已确认 ${res.data.length} 条`);
        return res.data;
      } catch (e) {
        return [];
      } finally {
        this.loading = false;
      }
    },

    async issueRecord(id: number) {
      try {
        const res = await request.post<any, Result<SalaryRecord>>(`${API_URL}/issue/${id}`);
        message.success('已发放');
        return res.data;
      } catch (e) {
        return null;
      }
    },

    async batchIssue(ids: number[]) {
      this.loading = true;
      try {
        const res = await request.post<any, Result<SalaryRecord[]>>(
          `${API_URL}/batch-issue`,
          ids
        );
        message.success(`已发放 ${res.data.length} 条`);
        return res.data;
      } catch (e) {
        return [];
      } finally {
        this.loading = false;
      }
    },

    async fetchAdjustLogs(salaryRecordId: number) {
      try {
        const res = await request.get<any, Result<SalaryAdjustLog[]>>(
          `${API_URL}/adjust-logs/${salaryRecordId}`
        );
        this.adjustLogs = res.data;
        return res.data;
      } catch (e) {
        return [];
      }
    },

    async fetchFieldLabels() {
      try {
        const res = await request.get<any, Result<{ field: string; label: string }[]>>(
          `${API_URL}/field-labels`
        );
        this.fieldLabels = res.data;
        return res.data;
      } catch (e) {
        return [];
      }
    },

    async fetchMyRecords(pageNum = 1, pageSize = 10) {
      this.loading = true;
      try {
        const res = await request.get<any, Result<PageResult<SalaryRecord>>>(
          `${API_URL}/my-records`,
          { params: { pageNum, pageSize } }
        );
        this.myRecordList = res.data.records;
        this.myRecordListTotal = res.data.total;
        return res.data;
      } catch (e) {
        return null;
      } finally {
        this.loading = false;
      }
    },

    async fetchMyRecordDetail(id: number) {
      this.loading = true;
      try {
        const res = await request.get<any, Result<SalaryRecord>>(`${API_URL}/my-record/${id}`);
        this.myCurrentRecord = res.data;
        return res.data;
      } catch (e) {
        return null;
      } finally {
        this.loading = false;
      }
    },

    async fetchCostReport(year: number, month: number, departmentId?: number) {
      this.loading = true;
      try {
        const res = await request.get<any, Result<SalaryCostReport>>(`${API_URL}/cost-report`, {
          params: { year, month, departmentId },
        });
        this.costReport = res.data;
        return res.data;
      } catch (e) {
        return null;
      } finally {
        this.loading = false;
      }
    },
  },
});
