import { defineStore } from 'pinia';
import request from '../utils/request';

const API_URL = '/dashboard';
const CACHE_KEY = 'dashboard_query_cache';

export interface DepartmentStatDTO {
  departmentId: number;
  departmentName: string;
  employeeCount: number;
  headcountLimit: number;
  headcountUsageRate: number;
  percentage: number;
}

export interface TurnoverTrendDTO {
  month: string;
  hires: number;
  departures: number;
  attritionRate: number;
}

export interface MonthlyStatDTO {
  month: string;
  count: number;
}

export interface DashboardOverviewDTO {
  totalEmployees: number;
  totalHeadcount: number;
  headcountUsageRate: number;
  newHiresThisMonth: number;
  pendingProbation: number;
  contractExpiringSoon: number;
  pendingLeaveApproval: number;
  pendingOnboarding: number;
  totalEmployeesMoM: number;
  newHiresMoM: number;
  attritionRate: number;
  attritionRateMoM: number;
  departmentStats: DepartmentStatDTO[];
  turnoverTrend: TurnoverTrendDTO[];
  monthlyHireTrend: MonthlyStatDTO[];
}

export interface DashboardQuery {
  startDate?: string;
  endDate?: string;
  departmentId?: number | null;
}

interface Result<T> {
  code: number;
  message: string;
  data: T;
}

export const useDashboardStore = defineStore('dashboard', {
  state: () => ({
    overview: null as DashboardOverviewDTO | null,
    loading: false,
    query: {
      startDate: undefined as string | undefined,
      endDate: undefined as string | undefined,
      departmentId: null as number | null,
    } as DashboardQuery,
  }),
  actions: {
    loadCachedQuery() {
      try {
        const cached = localStorage.getItem(CACHE_KEY);
        if (cached) {
          const parsed = JSON.parse(cached);
          this.query = { ...this.query, ...parsed };
        }
      } catch (e) {
        console.error('读取缓存查询条件失败', e);
      }
    },
    saveQueryToCache() {
      try {
        localStorage.setItem(CACHE_KEY, JSON.stringify(this.query));
      } catch (e) {
        console.error('保存查询条件失败', e);
      }
    },
    setQuery(query: DashboardQuery) {
      this.query = { ...this.query, ...query };
      this.saveQueryToCache();
    },
    async fetchOverview() {
      this.loading = true;
      try {
        const params: Record<string, any> = {};
        if (this.query.startDate) params.startDate = this.query.startDate;
        if (this.query.endDate) params.endDate = this.query.endDate;
        if (this.query.departmentId) params.departmentId = this.query.departmentId;

        const res = await request.get<any, Result<DashboardOverviewDTO>>(
          `${API_URL}/overview`,
          { params }
        );
        this.overview = res.data;
      } catch (error) {
        console.error('获取仪表盘数据失败', error);
      } finally {
        this.loading = false;
      }
    },
    async refresh() {
      this.loadCachedQuery();
      await this.fetchOverview();
    },
  },
});
