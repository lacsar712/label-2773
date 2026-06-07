import { defineStore } from 'pinia';
import request from '../utils/request';

const API_URL = '/onboarding/notifications';

interface Result<T> {
  code: number;
  message: string;
  data: T;
}

export interface OnboardingNotification {
  id: number;
  checklistId?: number;
  checklistItemId?: number;
  recipientId: number;
  recipientName: string;
  notificationType: number;
  title: string;
  content: string;
  isRead?: boolean;
  readTime?: string;
  createTime?: string;
}

export const useOnboardingNotificationStore = defineStore('onboardingNotification', {
  state: () => ({
    notifications: [] as OnboardingNotification[],
    unreadCount: 0,
    loading: false,
  }),
  actions: {
    async fetchMyNotifications() {
      this.loading = true;
      try {
        const res = await request.get<any, Result<OnboardingNotification[]>>(`${API_URL}/my`);
        this.notifications = res.data;
      } finally {
        this.loading = false;
      }
    },
    async fetchUnreadCount() {
      try {
        const res = await request.get<any, Result<number>>(`${API_URL}/unread-count`);
        this.unreadCount = res.data;
        return res.data;
      } catch (error) {
        return 0;
      }
    },
    async markAsRead(id: number) {
      try {
        await request.post<any, Result<boolean>>(`${API_URL}/${id}/read`);
        await this.fetchMyNotifications();
        await this.fetchUnreadCount();
        return true;
      } catch (error) {
        return false;
      }
    },
    async markAllAsRead() {
      try {
        await request.post<any, Result<boolean>>(`${API_URL}/read-all`);
        await this.fetchMyNotifications();
        await this.fetchUnreadCount();
        return true;
      } catch (error) {
        return false;
      }
    },
  },
});
