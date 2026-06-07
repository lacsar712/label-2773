import { defineStore } from 'pinia';
import request from '../utils/request';
import { message } from 'ant-design-vue';

const API_URL = '/onboarding/templates';

interface Result<T> {
  code: number;
  message: string;
  data: T;
}

export interface OnboardingTemplateItem {
  id?: number;
  templateId?: number;
  itemName: string;
  itemDescription?: string;
  stage: number;
  sortOrder?: number;
  dueDays: number;
  responsibleRole: string;
  responsibleUserId?: number;
  responsibleUserName?: string;
  createTime?: string;
  updateTime?: string;
}

export interface OnboardingTemplate {
  id?: number;
  templateName: string;
  departmentId?: number;
  departmentName?: string;
  position?: string;
  description?: string;
  enabled?: boolean;
  isDefault?: boolean;
  createTime?: string;
  updateTime?: string;
  items?: OnboardingTemplateItem[];
}

export const useOnboardingTemplateStore = defineStore('onboardingTemplate', {
  state: () => ({
    templates: [] as OnboardingTemplate[],
    enabledTemplates: [] as OnboardingTemplate[],
    loading: false,
    currentTemplate: null as OnboardingTemplate | null,
  }),
  actions: {
    async fetchTemplates() {
      this.loading = true;
      try {
        const res = await request.get<any, Result<OnboardingTemplate[]>>(API_URL);
        this.templates = res.data;
      } finally {
        this.loading = false;
      }
    },
    async fetchEnabledTemplates() {
      try {
        const res = await request.get<any, Result<OnboardingTemplate[]>>(`${API_URL}/enabled`);
        this.enabledTemplates = res.data;
      } catch (error) {
        // handled by interceptor
      }
    },
    async fetchTemplate(id: number) {
      try {
        const res = await request.get<any, Result<OnboardingTemplate>>(`${API_URL}/${id}`);
        this.currentTemplate = res.data;
        return res.data;
      } catch (error) {
        return null;
      }
    },
    async createTemplate(template: OnboardingTemplate) {
      try {
        await request.post<any, Result<boolean>>(API_URL, template);
        message.success('创建成功');
        await this.fetchTemplates();
        return true;
      } catch (error) {
        return false;
      }
    },
    async updateTemplate(template: OnboardingTemplate) {
      try {
        await request.put<any, Result<boolean>>(API_URL, template);
        message.success('更新成功');
        await this.fetchTemplates();
        return true;
      } catch (error) {
        return false;
      }
    },
    async deleteTemplate(id: number) {
      try {
        await request.delete<any, Result<boolean>>(`${API_URL}/${id}`);
        message.success('删除成功');
        await this.fetchTemplates();
        return true;
      } catch (error) {
        return false;
      }
    },
  },
});
