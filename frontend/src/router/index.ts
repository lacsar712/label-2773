import { createRouter, createWebHistory } from 'vue-router';
import type { RouteRecordRaw } from 'vue-router';
import { useAuthStore } from '../stores/auth';
import HomeView from '../views/HomeView.vue';
import EmployeeView from '../views/EmployeeView.vue';
import DepartmentView from '../views/DepartmentView.vue';
import LoginView from '../views/LoginView.vue';
import ChangePasswordView from '../views/ChangePasswordView.vue';
import AttendanceView from '../views/AttendanceView.vue';
import AttendanceRecordView from '../views/AttendanceRecordView.vue';
import AttendanceMakeUpView from '../views/AttendanceMakeUpView.vue';
import AttendanceApprovalView from '../views/AttendanceApprovalView.vue';
import AttendanceReportView from '../views/AttendanceReportView.vue';
import LeaveApplicationView from '../views/LeaveApplicationView.vue';
import LeaveApprovalView from '../views/LeaveApprovalView.vue';
import LeaveApprovalConfigView from '../views/LeaveApprovalConfigView.vue';
import SalaryRecordView from '../views/SalaryRecordView.vue';
import SalaryTemplateView from '../views/SalaryTemplateView.vue';
import MySalaryView from '../views/MySalaryView.vue';
import SalaryReportView from '../views/SalaryReportView.vue';
import OnboardingTemplateView from '../views/OnboardingTemplateView.vue';
import OnboardingChecklistListView from '../views/OnboardingChecklistListView.vue';
import OnboardingChecklistDetailView from '../views/OnboardingChecklistDetailView.vue';
import MyOnboardingView from '../views/MyOnboardingView.vue';
import AnnouncementView from '../views/AnnouncementView.vue';
import AnnouncementManageView from '../views/AnnouncementManageView.vue';
import AnnouncementEditView from '../views/AnnouncementEditView.vue';
import AnnouncementDetailView from '../views/AnnouncementDetailView.vue';
import LoginLogView from '../views/LoginLogView.vue';

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
    path: '/login-logs',
    name: 'login-logs',
    component: LoginLogView,
    meta: { title: '登录日志', requiresAuth: true, roles: ['ADMIN', 'HR', 'EMPLOYEE'] },
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
    path: '/attendance',
    name: 'attendance',
    component: AttendanceView,
    meta: { title: '考勤打卡', requiresAuth: true, roles: ['ADMIN', 'HR', 'EMPLOYEE'] },
  },
  {
    path: '/attendance/records',
    name: 'attendance-records',
    component: AttendanceRecordView,
    meta: { title: '考勤记录', requiresAuth: true, roles: ['ADMIN', 'HR', 'EMPLOYEE'] },
  },
  {
    path: '/attendance/makeup',
    name: 'attendance-makeup',
    component: AttendanceMakeUpView,
    meta: { title: '补卡申请', requiresAuth: true, roles: ['ADMIN', 'HR', 'EMPLOYEE'] },
  },
  {
    path: '/attendance/approval',
    name: 'attendance-approval',
    component: AttendanceApprovalView,
    meta: { title: '补卡审批', requiresAuth: true, roles: ['ADMIN', 'HR'] },
  },
  {
    path: '/attendance/reports',
    name: 'attendance-reports',
    component: AttendanceReportView,
    meta: { title: '月度报表', requiresAuth: true, roles: ['ADMIN', 'HR'] },
  },
  {
    path: '/leave/apply',
    name: 'leave-apply',
    component: LeaveApplicationView,
    meta: { title: '请假申请', requiresAuth: true, roles: ['ADMIN', 'HR', 'EMPLOYEE'] },
  },
  {
    path: '/leave/approval',
    name: 'leave-approval',
    component: LeaveApprovalView,
    meta: { title: '请假审批', requiresAuth: true, roles: ['ADMIN', 'HR', 'EMPLOYEE'] },
  },
  {
    path: '/leave/config',
    name: 'leave-config',
    component: LeaveApprovalConfigView,
    meta: { title: '审批流程配置', requiresAuth: true, roles: ['ADMIN', 'HR'] },
  },
  {
    path: '/salary/manage',
    name: 'salary-manage',
    component: SalaryRecordView,
    meta: { title: '薪资管理', requiresAuth: true, roles: ['ADMIN', 'HR'] },
  },
  {
    path: '/salary/templates',
    name: 'salary-templates',
    component: SalaryTemplateView,
    meta: { title: '薪资模板', requiresAuth: true, roles: ['ADMIN', 'HR'] },
  },
  {
    path: '/salary/my',
    name: 'salary-my',
    component: MySalaryView,
    meta: { title: '我的薪资', requiresAuth: true, roles: ['ADMIN', 'HR', 'EMPLOYEE'] },
  },
  {
    path: '/salary/report',
    name: 'salary-report',
    component: SalaryReportView,
    meta: { title: '人力成本报表', requiresAuth: true, roles: ['ADMIN', 'HR'] },
  },
  {
    path: '/onboarding/my',
    name: 'onboarding-my',
    component: MyOnboardingView,
    meta: { title: '我的入职清单', requiresAuth: true, roles: ['ADMIN', 'HR', 'EMPLOYEE'] },
  },
  {
    path: '/onboarding/templates',
    name: 'onboarding-templates',
    component: OnboardingTemplateView,
    meta: { title: '入职模板管理', requiresAuth: true, roles: ['ADMIN', 'HR'] },
  },
  {
    path: '/onboarding/manage',
    name: 'onboarding-manage',
    component: OnboardingChecklistListView,
    meta: { title: '入职清单管理', requiresAuth: true, roles: ['ADMIN', 'HR'] },
  },
  {
    path: '/onboarding/checklist/:id',
    name: 'onboarding-detail',
    component: OnboardingChecklistDetailView,
    meta: { title: '入职清单详情', requiresAuth: true, roles: ['ADMIN', 'HR', 'EMPLOYEE'] },
  },
  {
    path: '/announcements',
    name: 'announcements',
    component: AnnouncementView,
    meta: { title: '公告通知', requiresAuth: true, roles: ['ADMIN', 'HR', 'EMPLOYEE'] },
  },
  {
    path: '/announcements/:id',
    name: 'announcement-detail',
    component: AnnouncementDetailView,
    meta: { title: '公告详情', requiresAuth: true, roles: ['ADMIN', 'HR', 'EMPLOYEE'] },
  },
  {
    path: '/admin/announcements',
    name: 'admin-announcements',
    component: AnnouncementManageView,
    meta: { title: '公告管理', requiresAuth: true, roles: ['ADMIN', 'HR'] },
  },
  {
    path: '/admin/announcements/new',
    name: 'admin-announcement-new',
    component: AnnouncementEditView,
    meta: { title: '新建公告', requiresAuth: true, roles: ['ADMIN', 'HR'] },
  },
  {
    path: '/admin/announcements/edit/:id',
    name: 'admin-announcement-edit',
    component: AnnouncementEditView,
    meta: { title: '编辑公告', requiresAuth: true, roles: ['ADMIN', 'HR'] },
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

router.beforeEach(async (to, _from, next) => {
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

  if (authStore.userInfo?.isFirstLogin === true) {
    next('/change-password');
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
