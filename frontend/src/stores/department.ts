import { defineStore } from 'pinia';
import request from '../utils/request';
import { message } from 'ant-design-vue';

const API_URL = '/departments';

export interface Department {
  id?: number;
  name: string;
  code: string;
  description?: string;
  leader?: string;
  parentId?: number | null;
  headcountLimit: number;
  enabled: boolean;
  children?: Department[];
  employeeCount?: number;
  overHeadcount?: boolean;
}

export interface DepartmentNotification {
  id: number;
  departmentId: number;
  departmentName: string;
  oldLeader?: string;
  newLeader?: string;
  content: string;
  createTime: string;
}

interface Result<T> {
  code: number;
  message: string;
  data: T;
}

export const useDepartmentStore = defineStore('department', {
  state: () => ({
    departmentsTree: [] as Department[],
    departmentsFlat: [] as Department[],
    enabledDepartments: [] as Department[],
    loading: false,
    notifications: [] as DepartmentNotification[],
  }),
  actions: {
    async fetchDepartmentsTree() {
      this.loading = true;
      try {
        const res = await request.get<any, Result<Department[]>>(`${API_URL}/tree`);
        this.departmentsTree = res.data;
      } finally {
        this.loading = false;
      }
    },
    async fetchDepartmentsFlat() {
      this.loading = true;
      try {
        const res = await request.get<any, Result<Department[]>>(`${API_URL}/list`);
        this.departmentsFlat = res.data;
      } finally {
        this.loading = false;
      }
    },
    async fetchEnabledDepartments() {
      try {
        const res = await request.get<any, Result<Department[]>>(`${API_URL}/enabled`);
        this.enabledDepartments = res.data;
      } catch (error) {
        console.error('获取启用部门失败', error);
      }
    },
    async searchDepartments(keyword: string) {
      const res = await request.get<any, Result<Department[]>>(`${API_URL}/search`, {
        params: { keyword },
      });
      return res.data;
    },
    async checkCodeUnique(code: string, excludeId?: number): Promise<boolean> {
      const res = await request.get<any, Result<boolean>>(`${API_URL}/check-code`, {
        params: { code, excludeId },
      });
      return res.data;
    },
    async getDepartment(id: number) {
      const res = await request.get<any, Result<Department>>(`${API_URL}/${id}`);
      return res.data;
    },
    async createDepartment(dept: Department) {
      await request.post<any, Result<boolean>>(API_URL, dept);
      message.success('创建部门成功');
      await this.refreshAll();
    },
    async updateDepartment(dept: Department) {
      await request.put<any, Result<boolean>>(API_URL, dept);
      message.success('更新部门成功');
      await this.refreshAll();
    },
    async toggleEnabled(id: number, enabled: boolean) {
      await request.put<any, Result<boolean>>(`${API_URL}/${id}/toggle`, null, {
        params: { enabled },
      });
      message.success(enabled ? '部门已启用' : '部门已停用，子部门已同步停用');
      await this.refreshAll();
    },
    async deleteDepartment(id: number) {
      await request.delete<any, Result<boolean>>(`${API_URL}/${id}`);
      message.success('删除部门成功');
      await this.refreshAll();
    },
    async fetchNotifications() {
      try {
        const res = await request.get<any, Result<DepartmentNotification[]>>(
          `${API_URL}/notifications`
        );
        this.notifications = res.data;
      } catch (error) {
        console.error('获取通知失败', error);
      }
    },
    async refreshAll() {
      await Promise.all([
        this.fetchDepartmentsTree(),
        this.fetchDepartmentsFlat(),
        this.fetchEnabledDepartments(),
      ]);
    },
    flattenDepartments(tree: Department[]): Department[] {
      const result: Department[] = [];
      const walk = (items: Department[]) => {
        items.forEach((item) => {
          result.push(item);
          if (item.children && item.children.length > 0) {
            walk(item.children);
          }
        });
      };
      walk(tree);
      return result;
    },
    getDepartmentName(id?: number | null): string {
      if (!id) return '';
      const all = this.flattenDepartments(this.departmentsTree);
      const dept = all.find((d) => d.id === id);
      return dept ? dept.name : '';
    },
    filterDepartmentsByParent(departments: Department[], parentId?: number | null): Department[] {
      if (!parentId) return departments;
      const allDescendantIds = this.collectDescendantIds(departments, parentId);
      return departments.filter((d) => allDescendantIds.includes(d.id as number));
    },
    collectDescendantIds(departments: Department[], parentId: number): number[] {
      const ids: number[] = [parentId];
      const walk = (items: Department[]) => {
        items.forEach((item) => {
          if (item.parentId === parentId || ids.includes(item.parentId as number)) {
            if (!ids.includes(item.id as number)) {
              ids.push(item.id as number);
            }
          }
          if (item.children) {
            walk(item.children);
          }
        });
      };
      walk(departments);
      return ids;
    },
  },
});
