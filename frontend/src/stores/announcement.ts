import { defineStore } from 'pinia';
import request from '../utils/request';

const API_URL = '/announcements';

interface Result<T> {
  code: number;
  message: string;
  data: T;
}

export interface VisibilityTarget {
  targetType: number;
  targetId: number;
  targetName?: string;
}

export interface Announcement {
  id?: number;
  title: string;
  content?: string;
  coverImage?: string;
  visibilityType: number;
  effectiveTime?: string;
  expireTime?: string;
  isPinned?: boolean;
  isImportant?: boolean;
  status?: number;
  publishTime?: string;
  creatorId?: number;
  creatorName?: string;
  readCount?: number;
  totalTargetCount?: number;
  createTime?: string;
  updateTime?: string;
  visibilityList?: VisibilityTarget[];
  visibilityTargets?: VisibilityTarget[];
  hasRead?: boolean;
}

export interface AnnouncementRead {
  id: number;
  announcementId: number;
  userId: number;
  userName: string;
  employeeId?: number;
  departmentId?: number;
  readTime: string;
}

export interface AnnouncementPage {
  records: Announcement[];
  total: number;
  size: number;
  current: number;
  pages: number;
}

export interface ReadStats {
  readList: AnnouncementRead[];
  unreadList: any[];
  readCount: number;
  unreadCount: number;
}

export const useAnnouncementStore = defineStore('announcement', {
  state: () => ({
    announcementList: [] as Announcement[],
    announcementListTotal: 0,
    myAnnouncementList: [] as Announcement[],
    myAnnouncementListTotal: 0,
    currentAnnouncement: null as Announcement | null,
    readStats: null as ReadStats | null,
    loading: false,
  }),
  actions: {
    async fetchAdminList(params: any) {
      this.loading = true;
      try {
        const res = await request.get<any, Result<AnnouncementPage>>(`${API_URL}/admin`, { params });
        this.announcementList = res.data.records;
        this.announcementListTotal = res.data.total;
        return res.data;
      } finally {
        this.loading = false;
      }
    },

    async fetchMyList(params: any) {
      this.loading = true;
      try {
        const res = await request.get<any, Result<AnnouncementPage>>(`${API_URL}/my`, { params });
        this.myAnnouncementList = res.data.records;
        this.myAnnouncementListTotal = res.data.total;
        return res.data;
      } finally {
        this.loading = false;
      }
    },

    async fetchAdminDetail(id: number) {
      const res = await request.get<any, Result<Announcement>>(`${API_URL}/admin/${id}`);
      this.currentAnnouncement = res.data;
      return res.data;
    },

    async fetchMyDetail(id: number) {
      const res = await request.get<any, Result<Announcement>>(`${API_URL}/my/${id}`);
      this.currentAnnouncement = res.data;
      return res.data;
    },

    async createDraft(data: Announcement) {
      const res = await request.post<any, Result<Announcement>>(`${API_URL}`, data);
      return res.data;
    },

    async update(id: number, data: Announcement) {
      const res = await request.put<any, Result<Announcement>>(`${API_URL}/${id}`, data);
      return res.data;
    },

    async publish(id: number) {
      const res = await request.post<any, Result<boolean>>(`${API_URL}/${id}/publish`);
      return res.data;
    },

    async togglePin(id: number, pinned: boolean) {
      const res = await request.post<any, Result<boolean>>(`${API_URL}/${id}/pin`, null, {
        params: { pinned },
      });
      return res.data;
    },

    async archive(id: number) {
      const res = await request.post<any, Result<boolean>>(`${API_URL}/${id}/archive`);
      return res.data;
    },

    async delete(id: number) {
      const res = await request.delete<any, Result<boolean>>(`${API_URL}/${id}`);
      return res.data;
    },

    async markAsRead(id: number) {
      const res = await request.post<any, Result<boolean>>(`${API_URL}/my/${id}/read`);
      return res.data;
    },

    async fetchReadStats(id: number) {
      const res = await request.get<any, Result<ReadStats>>(`${API_URL}/admin/${id}/stats`);
      this.readStats = res.data;
      return res.data;
    },

    async uploadImage(file: File) {
      const formData = new FormData();
      formData.append('file', file);
      const res = await request.post<any, Result<{ url: string; filename: string; size: string }>>(
        '/upload/image',
        formData,
        {
          headers: {
            'Content-Type': 'multipart/form-data',
          },
        }
      );
      return res.data;
    },
  },
});
