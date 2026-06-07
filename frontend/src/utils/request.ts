import axios, { AxiosRequestConfig } from 'axios';
import { message, Modal } from 'ant-design-vue';
import { useAuthStore } from '../stores/auth';
import router from '../router';

interface CustomAxiosRequestConfig extends AxiosRequestConfig {
  skipAuth?: boolean;
}

const service = axios.create({
  baseURL: '/api',
  timeout: 10000,
});

let isReloginModalShown = false;

service.interceptors.request.use(
  (config: CustomAxiosRequestConfig) => {
    const authStore = useAuthStore();
    if (!config.skipAuth && authStore.accessToken) {
      config.headers = config.headers || {};
      config.headers['Authorization'] = `Bearer ${authStore.accessToken}`;
    }
    return config;
  },
  (error) => {
    console.error('Request error:', error);
    return Promise.reject(error);
  }
);

service.interceptors.response.use(
  (response) => {
    const res = response.data;
    if (res.code !== 200) {
      if (res.code === 401 || res.code === 1006 || res.code === 1007 || res.code === 1009) {
        if (!isReloginModalShown) {
          isReloginModalShown = true;
          const authStore = useAuthStore();
          authStore.clearAuth();
          Modal.confirm({
            title: '提示',
            content: res.code === 1009 ? '您已在其他地方登录，请重新登录' : '登录状态已过期，请重新登录',
            okText: '重新登录',
            cancelText: '取消',
            onOk: () => {
              isReloginModalShown = false;
              router.push('/login');
            },
            onCancel: () => {
              isReloginModalShown = false;
            },
          });
        }
        return Promise.reject(new Error(res.message || 'Unauthorized'));
      }
      message.error(res.message || 'Error');
      return Promise.reject(new Error(res.message || 'Error'));
    } else {
      return res;
    }
  },
  async (error) => {
    const originalRequest = error.config;
    if (error.response?.status === 401 && !originalRequest._retry) {
      const authStore = useAuthStore();
      if (authStore.refreshToken) {
        originalRequest._retry = true;
        try {
          const newToken = await authStore.refreshAccessToken();
          originalRequest.headers['Authorization'] = `Bearer ${newToken}`;
          return service(originalRequest);
        } catch {
          authStore.clearAuth();
          router.push('/login');
          return Promise.reject(error);
        }
      }
    }
    const msg = error.response?.data?.message || error.message || 'Network Error';
    message.error(msg);
    return Promise.reject(error);
  }
);

export default service;
