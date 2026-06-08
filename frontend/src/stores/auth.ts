import { defineStore } from 'pinia';
import request from '../utils/request';
import { message } from 'ant-design-vue';

const TOKEN_KEY = 'access_token';
const REFRESH_TOKEN_KEY = 'refresh_token';
const USER_INFO_KEY = 'user_info';

interface LoginRequest {
  username: string;
  password: string;
  captcha: string;
  uuid: string;
}

interface CaptchaResponse {
  uuid: string;
  img: string;
}

interface UserInfo {
  userId: number;
  employeeId?: number;
  username: string;
  nickname: string;
  email?: string;
  phone?: string;
  avatar?: string;
  roleId: number;
  roleCode: string;
  roleName: string;
}

interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  userId: number;
  employeeId?: number;
  username: string;
  nickname: string;
  roleCode: string;
  roleName: string;
  avatar?: string;
  isFirstLogin: boolean;
}

interface ChangePasswordRequest {
  oldPassword: string;
  newPassword: string;
}

interface Result<T> {
  code: number;
  message: string;
  data: T;
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    accessToken: localStorage.getItem(TOKEN_KEY) || '',
    refreshToken: localStorage.getItem(REFRESH_TOKEN_KEY) || '',
    userInfo: null as UserInfo | null,
  }),
  getters: {
    isLoggedIn(state): boolean {
      return !!state.accessToken;
    },
  },
  actions: {
    hasRole(roles: string | string[]): boolean {
      if (!this.userInfo?.roleCode) return false;
      const roleList = Array.isArray(roles) ? roles : [roles];
      return roleList.includes(this.userInfo.roleCode);
    },
    async getCaptcha(): Promise<CaptchaResponse> {
      const res = await request.get<any, Result<CaptchaResponse>>('/auth/captcha');
      return res.data;
    },
    async login(loginData: LoginRequest): Promise<LoginResponse> {
      const res = await request.post<any, Result<LoginResponse>>('/auth/login', loginData);
      const data = res.data;
      this.accessToken = data.accessToken;
      this.refreshToken = data.refreshToken;
      localStorage.setItem(TOKEN_KEY, data.accessToken);
      localStorage.setItem(REFRESH_TOKEN_KEY, data.refreshToken);
      this.userInfo = {
        userId: data.userId,
        employeeId: data.employeeId,
        username: data.username,
        nickname: data.nickname,
        avatar: data.avatar,
        roleId: 0,
        roleCode: data.roleCode,
        roleName: data.roleName,
      };
      localStorage.setItem(USER_INFO_KEY, JSON.stringify(this.userInfo));
      return data;
    },
    async fetchUserInfo() {
      const res = await request.get<any, Result<UserInfo>>('/auth/user-info');
      this.userInfo = res.data;
      localStorage.setItem(USER_INFO_KEY, JSON.stringify(res.data));
      return res.data;
    },
    async changePassword(data: ChangePasswordRequest) {
      await request.post<any, Result<void>>('/auth/change-password', data);
      message.success('密码修改成功');
    },
    async logout() {
      try {
        await request.post<any, Result<void>>('/auth/logout');
      } catch (e) {
      }
      this.clearAuth();
    },
    clearAuth() {
      this.accessToken = '';
      this.refreshToken = '';
      this.userInfo = null;
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(REFRESH_TOKEN_KEY);
      localStorage.removeItem(USER_INFO_KEY);
    },
    async refreshAccessToken(): Promise<string> {
      const res = await request.post<any, Result<LoginResponse>>('/auth/refresh-token', {
        refreshToken: this.refreshToken,
      }, { skipAuth: true } as any);
      const data = res.data;
      this.accessToken = data.accessToken;
      this.refreshToken = data.refreshToken;
      localStorage.setItem(TOKEN_KEY, data.accessToken);
      localStorage.setItem(REFRESH_TOKEN_KEY, data.refreshToken);
      return data.accessToken;
    },
    loadUserInfoFromCache() {
      try {
        const cached = localStorage.getItem(USER_INFO_KEY);
        if (cached) {
          this.userInfo = JSON.parse(cached);
        }
      } catch (e) {
        console.error('读取用户信息缓存失败', e);
      }
    },
  },
});
