import { createRouter, createWebHistory } from 'vue-router';
import HomeView from '../views/HomeView.vue';
import EmployeeView from '../views/EmployeeView.vue';
import DepartmentView from '../views/DepartmentView.vue';

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
      meta: { title: '数据概览' },
    },
    {
      path: '/employees',
      name: 'employees',
      component: EmployeeView,
      meta: { title: '员工管理' },
    },
    {
      path: '/departments',
      name: 'departments',
      component: DepartmentView,
      meta: { title: '部门管理' },
    },
  ],
});

export default router;
