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
  deputyLeader?: string;
  parentId?: number | null;
  headcountLimit: number;
  levelType?: number;
  enabled: boolean;
  createTime?: string;
  updateTime?: string;
  children?: Department[];
  employeeCount?: number;
  activeEmployeeCount?: number;
  overHeadcount?: boolean;
  parentName?: string;
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

export interface LeaderChangeHistory {
  id: number;
  departmentId: number;
  departmentName: string;
  changeType: number;
  oldLeader?: string;
  newLeader?: string;
  operatorId?: number;
  operatorName?: string;
  remark?: string;
  createTime: string;
}

export interface VersionSnapshot {
  id: number;
  snapshotName: string;
  snapshotType: number;
  treeSnapshot: string;
  description?: string;
  operatorId?: number;
  operatorName?: string;
  createTime: string;
}

export interface TreeState {
  treeKey: string;
  expandedIds: number[];
  selectedId?: number;
}

export interface DepartmentDetail {
  department: Department;
  activeEmployees: any[];
  subDepartments: Department[];
  leaderChangeHistory: LeaderChangeHistory[];
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
    currentDetail: null as DepartmentDetail | null,
    snapshots: [] as VersionSnapshot[],
    treeState: { treeKey: 'department', expandedIds: [], selectedId: undefined } as TreeState,
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
    async getDepartmentDetail(id: number): Promise<DepartmentDetail> {
      const res = await request.get<any, Result<DepartmentDetail>>(`${API_URL}/${id}/detail`);
      this.currentDetail = res.data;
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
      if (this.currentDetail && this.currentDetail.department.id === dept.id) {
        await this.getDepartmentDetail(dept.id as number);
      }
    },
    async moveDepartment(deptId: number, targetParentId: number | null) {
      await request.put<any, Result<boolean>>(`${API_URL}/move`, {
        deptId,
        targetParentId: targetParentId || null,
      });
      message.success('移动部门成功');
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
      if (this.currentDetail && this.currentDetail.department.id === id) {
        this.currentDetail = null;
        this.treeState.selectedId = undefined;
      }
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
    async fetchSnapshots() {
      try {
        const res = await request.get<any, Result<VersionSnapshot[]>>(`${API_URL}/snapshots`);
        this.snapshots = res.data;
      } catch (error) {
        console.error('获取快照列表失败', error);
      }
    },
    async createSnapshot(snapshotName: string, description?: string) {
      const res = await request.post<any, Result<VersionSnapshot>>(`${API_URL}/snapshots`, {
        snapshotName,
        description,
      });
      message.success('创建快照成功');
      await this.fetchSnapshots();
      return res.data;
    },
    async getSnapshot(id: number): Promise<VersionSnapshot> {
      const res = await request.get<any, Result<VersionSnapshot>>(`${API_URL}/snapshots/${id}`);
      return res.data;
    },
    async restoreSnapshot(id: number): Promise<Department[]> {
      const res = await request.get<any, Result<Department[]>>(`${API_URL}/snapshots/${id}/restore`);
      return res.data;
    },
    async deleteSnapshot(id: number) {
      await request.delete<any, Result<boolean>>(`${API_URL}/snapshots/${id}`);
      message.success('删除快照成功');
      await this.fetchSnapshots();
    },
    async fetchTreeState(treeKey: string = 'department') {
      try {
        const res = await request.get<any, Result<TreeState>>(`${API_URL}/tree-state`, {
          params: { treeKey },
        });
        if (res.data) {
          this.treeState = {
            treeKey: res.data.treeKey || treeKey,
            expandedIds: res.data.expandedIds || [],
            selectedId: res.data.selectedId,
          };
        }
      } catch (error) {
        console.error('获取树状态失败', error);
      }
    },
    async saveTreeState(expandedIds: number[], selectedId?: number, treeKey: string = 'department') {
      try {
        this.treeState = { treeKey, expandedIds, selectedId };
        await request.post<any, Result<boolean>>(`${API_URL}/tree-state`, {
          treeKey,
          expandedIds,
          selectedId,
        });
      } catch (error) {
        console.error('保存树状态失败', error);
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
