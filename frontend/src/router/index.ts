import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router';
import { useAuthStore } from '../stores/auth';
import HomeView from '../views/HomeView.vue';
import EmployeeView from '../views/EmployeeView.vue';
import DepartmentView from '../views/DepartmentView.vue';
import LoginView from '../views/LoginView.vue';
import ChangePasswordView from '../views/ChangePasswordView.vue';

declare module 'vue-router' {
  interface RouteMeta {
    title?: string;
    requiresAuth?: boolean;
    roles?: string[];
  }
}

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: LoginView,
    meta: { title: '登录', requiresAuth: false },
  },
  {
    path: '/change-password',
    name: 'change-password',
    component: ChangePasswordView,
    meta: { title: '修改密码', requiresAuth: true },
  },
  {
    path: '/',
    name: 'home',
    component: HomeView,
    meta: { title: '数据概览', requiresAuth: true, roles: ['ADMIN', 'HR', 'EMPLOYEE'] },
  },
  {
    path: '/employees',
    name: 'employees',
    component: EmployeeView,
    meta: { title: '员工管理', requiresAuth: true, roles: ['ADMIN', 'HR'] },
  },
  {
    path: '/departments',
    name: 'departments',
    component: DepartmentView,
    meta: { title: '部门管理', requiresAuth: true, roles: ['ADMIN', 'HR'] },
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/',
  },
];

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
});

router.beforeEach(async (to, from, next) => {
  document.title = `${to.meta.title || '员工管理系统'} - 员工管理系统`;
  const authStore = useAuthStore();

  if (to.meta.requiresAuth === false) {
    if (authStore.isLoggedIn && to.path === '/login') {
      next('/');
    } else {
      next();
    }
    return;
  }

  if (!authStore.isLoggedIn) {
    next({ path: '/login', query: { redirect: to.fullPath } });
    return;
  }

  if (!authStore.userInfo) {
    try {
      await authStore.fetchUserInfo();
    } catch {
      authStore.clearAuth();
      next({ path: '/login', query: { redirect: to.fullPath } });
      return;
    }
  }

  if (to.path === '/change-password') {
    next();
    return;
  }

  if (authStore.userInfo && authStore.isLoggedIn) {
    try {
      const fullUserInfo = await authStore.fetchUserInfo();
      if (fullUserInfo && fullUserInfo.roleCode && !authStore.userInfo.roleCode) {
        authStore.userInfo.roleCode = fullUserInfo.roleCode;
        authStore.userInfo.roleName = fullUserInfo.roleName;
        localStorage.setItem('user_info', JSON.stringify(authStore.userInfo));
      }
    } catch {}
  }

  if (authStore.userInfo && !authStore.userInfo.roleCode) {
    try {
      await authStore.fetchUserInfo();
    } catch {}
  }

  if (to.meta.roles && to.meta.roles.length > 0) {
    if (!authStore.hasRole(to.meta.roles)) {
      next('/');
      return;
    }
  }

  next();
});

export default router;
