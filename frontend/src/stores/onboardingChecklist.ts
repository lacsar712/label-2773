import { defineStore } from 'pinia';
import request from '../utils/request';
import { message } from 'ant-design-vue';

const API_URL = '/onboarding/checklists';

interface Result<T> {
  code: number;
  message: string;
  data: T;
}

export interface OnboardingChecklistItem {
  id?: number;
  checklistId?: number;
  employeeId?: number;
  templateItemId?: number;
  itemName: string;
  itemDescription?: string;
  stage: number;
  sortOrder?: number;
  isTemporary?: boolean;
  responsibleUserId?: number;
  responsibleUserName?: string;
  dueDate: string;
  status?: number;
  completedUserId?: number;
  completedUserName?: string;
  completedTime?: string;
  remark?: string;
  notificationSent?: boolean;
  createTime?: string;
  updateTime?: string;
}

export interface OnboardingChecklist {
  id?: number;
  employeeId: number;
  employeeName?: string;
  departmentId?: number;
  departmentName?: string;
  position?: string;
  hireDate: string;
  templateId?: number;
  templateName?: string;
  mentorId?: number;
  mentorName?: string;
  status?: number;
  progress?: number;
  preJoinProgress?: number;
  firstDayProgress?: number;
  firstWeekProgress?: number;
  firstMonthProgress?: number;
  archivedTime?: string;
  createTime?: string;
  updateTime?: string;
  items?: OnboardingChecklistItem[];
}

export interface OnboardingProgressDTO {
  checklistId: number;
  employeeId: number;
  employeeName: string;
  overallProgress: number;
  preJoinProgress: number;
  firstDayProgress: number;
  firstWeekProgress: number;
  firstMonthProgress: number;
  totalItems: number;
  completedItems: number;
  pendingItems: number;
  overdueItems: number;
}

export interface ChecklistGenerateDTO {
  employeeId: number;
  templateId?: number;
  mentorId?: number;
  mentorName?: string;
  hireDate: string;
}

export interface ItemCompleteDTO {
  itemId: number;
  remark?: string;
}

export interface TemporaryItemDTO {
  checklistId: number;
  itemName: string;
  itemDescription?: string;
  stage: number;
  sortOrder?: number;
  responsibleUserId?: number;
  responsibleUserName?: string;
  dueDate: string;
}

export interface ItemDueDateDTO {
  itemId: number;
  dueDate: string;
}

export const useOnboardingChecklistStore = defineStore('onboardingChecklist', {
  state: () => ({
    checklists: [] as OnboardingChecklist[],
    currentChecklist: null as OnboardingChecklist | null,
    currentProgress: null as OnboardingProgressDTO | null,
    loading: false,
  }),
  actions: {
    async fetchChecklists() {
      this.loading = true;
      try {
        const res = await request.get<any, Result<OnboardingChecklist[]>>(API_URL);
        this.checklists = res.data;
      } finally {
        this.loading = false;
      }
    },
    async fetchChecklist(id: number) {
      this.loading = true;
      try {
        const res = await request.get<any, Result<OnboardingChecklist>>(`${API_URL}/${id}`);
        this.currentChecklist = res.data;
        return res.data;
      } finally {
        this.loading = false;
      }
    },
    async fetchByEmployeeId(employeeId: number) {
      this.loading = true;
      try {
        const res = await request.get<any, Result<OnboardingChecklist>>(`${API_URL}/employee/${employeeId}`);
        this.currentChecklist = res.data;
        return res.data;
      } finally {
        this.loading = false;
      }
    },
    async fetchProgress(id: number) {
      try {
        const res = await request.get<any, Result<OnboardingProgressDTO>>(`${API_URL}/${id}/progress`);
        this.currentProgress = res.data;
        return res.data;
      } catch (error) {
        return null;
      }
    },
    async generateChecklist(dto: ChecklistGenerateDTO) {
      try {
        const res = await request.post<any, Result<OnboardingChecklist>>(`${API_URL}/generate`, dto);
        message.success('入职清单生成成功');
        await this.fetchChecklists();
        return res.data;
      } catch (error) {
        return null;
      }
    },
    async completeItem(dto: ItemCompleteDTO) {
      try {
        await request.post<any, Result<boolean>>(`${API_URL}/complete`, dto);
        message.success('标记完成成功');
        if (this.currentChecklist?.id) {
          await this.fetchChecklist(this.currentChecklist.id);
        }
        return true;
      } catch (error) {
        return false;
      }
    },
    async addTemporaryItem(dto: TemporaryItemDTO) {
      try {
        await request.post<any, Result<boolean>>(`${API_URL}/temporary`, dto);
        message.success('添加临时待办成功');
        if (this.currentChecklist?.id) {
          await this.fetchChecklist(this.currentChecklist.id);
        }
        return true;
      } catch (error) {
        return false;
      }
    },
    async updateDueDate(dto: ItemDueDateDTO) {
      try {
        await request.put<any, Result<boolean>>(`${API_URL}/due-date`, dto);
        message.success('截止日期更新成功');
        if (this.currentChecklist?.id) {
          await this.fetchChecklist(this.currentChecklist.id);
        }
        return true;
      } catch (error) {
        return false;
      }
    },
    async deleteItem(itemId: number) {
      try {
        await request.delete<any, Result<boolean>>(`${API_URL}/items/${itemId}`);
        message.success('删除成功');
        if (this.currentChecklist?.id) {
          await this.fetchChecklist(this.currentChecklist.id);
        }
        return true;
      } catch (error) {
        return false;
      }
    },
    async archiveChecklist(id: number) {
      try {
        await request.post<any, Result<boolean>>(`${API_URL}/${id}/archive`);
        message.success('归档成功');
        await this.fetchChecklists();
        return true;
      } catch (error) {
        return false;
      }
    },
  },
});
