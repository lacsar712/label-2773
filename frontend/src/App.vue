<script setup lang="ts">
import { ref, computed, watch, h } from 'vue';
import { RouterView, useRoute, useRouter } from 'vue-router';
import { TeamOutlined, UserOutlined, DashboardOutlined, LogoutOutlined, KeyOutlined, ClockCircleOutlined, CalendarOutlined, FormOutlined, AuditOutlined, BarChartOutlined, RestOutlined, CheckCircleOutlined, MoneyCollectOutlined, FileTextOutlined, WalletOutlined, PieChartOutlined, SolutionOutlined, UserAddOutlined, ProfileOutlined, NotificationOutlined, BellOutlined, SafetyOutlined } from '@ant-design/icons-vue';
import { Modal, message } from 'ant-design-vue';
import { useAuthStore } from './stores/auth';

type MenuItem = {
  key: string;
  icon: typeof DashboardOutlined;
  label: string;
  roles?: string[];
};

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const selectedKey = ref([route.path]);

watch(() => route.path, (newPath) => {
  selectedKey.value = [newPath];
});

const showLayout = computed(() => {
  return route.path !== '/login' && route.path !== '/change-password' && authStore.isLoggedIn;
});

const menuItems = computed(() => {
  const items: MenuItem[] = [
    {
      key: '/',
      icon: DashboardOutlined,
      label: '数据概览',
      roles: ['ADMIN', 'HR', 'EMPLOYEE'],
    },
  ];
  items.push({
    key: '/attendance',
    icon: ClockCircleOutlined,
    label: '考勤打卡',
    roles: ['ADMIN', 'HR', 'EMPLOYEE'],
  });
  items.push({
    key: '/attendance/records',
    icon: CalendarOutlined,
    label: '考勤记录',
    roles: ['ADMIN', 'HR', 'EMPLOYEE'],
  });
  items.push({
    key: '/attendance/makeup',
    icon: FormOutlined,
    label: '补卡申请',
    roles: ['ADMIN', 'HR', 'EMPLOYEE'],
  });
  items.push({
    key: '/leave/apply',
    icon: RestOutlined,
    label: '请假申请',
    roles: ['ADMIN', 'HR', 'EMPLOYEE'],
  });
  if (authStore.hasRole(['ADMIN', 'HR', 'EMPLOYEE'])) {
    items.push({
      key: '/leave/approval',
      icon: CheckCircleOutlined,
      label: '请假审批',
      roles: ['ADMIN', 'HR', 'EMPLOYEE'],
    });
  }
  items.push({
    key: '/salary/my',
    icon: WalletOutlined,
    label: '我的薪资',
    roles: ['ADMIN', 'HR', 'EMPLOYEE'],
  });
  items.push({
    key: '/onboarding/my',
    icon: ProfileOutlined,
    label: '我的入职清单',
    roles: ['ADMIN', 'HR', 'EMPLOYEE'],
  });
  items.push({
    key: '/announcements',
    icon: BellOutlined,
    label: '公告通知',
    roles: ['ADMIN', 'HR', 'EMPLOYEE'],
  });
  items.push({
    key: '/login-logs',
    icon: SafetyOutlined,
    label: '登录日志',
    roles: ['ADMIN', 'HR', 'EMPLOYEE'],
  });
  if (authStore.hasRole(['ADMIN', 'HR'])) {
    items.push(
      { key: '/employees', icon: UserOutlined, label: '员工管理', roles: ['ADMIN', 'HR'] },
      { key: '/departments', icon: TeamOutlined, label: '部门管理', roles: ['ADMIN', 'HR'] },
      { key: '/attendance/approval', icon: AuditOutlined, label: '补卡审批', roles: ['ADMIN', 'HR'] },
      { key: '/attendance/reports', icon: BarChartOutlined, label: '月度报表', roles: ['ADMIN', 'HR'] },
      { key: '/salary/manage', icon: MoneyCollectOutlined, label: '薪资管理', roles: ['ADMIN', 'HR'] },
      { key: '/salary/templates', icon: FileTextOutlined, label: '薪资模板', roles: ['ADMIN', 'HR'] },
      { key: '/salary/report', icon: PieChartOutlined, label: '人力成本', roles: ['ADMIN', 'HR'] },
      { key: '/onboarding/templates', icon: SolutionOutlined, label: '入职模板管理', roles: ['ADMIN', 'HR'] },
      { key: '/onboarding/manage', icon: UserAddOutlined, label: '入职清单管理', roles: ['ADMIN', 'HR'] },
      { key: '/admin/announcements', icon: NotificationOutlined, label: '公告管理', roles: ['ADMIN', 'HR'] },
    );
  }
  return items;
});

const antdMenuItems = computed(() =>
  menuItems.value.map((item) => ({
    key: item.key,
    label: item.label,
    icon: () => h(item.icon),
  }))
);

const userMenuItems = [
  {
    key: 'changePassword',
    icon: () => h(KeyOutlined),
    label: '修改密码',
  },
  {
    key: 'logout',
    icon: () => h(LogoutOutlined),
    label: '退出登录',
  },
];

async function handleUserMenuClick({ key }: { key: string }) {
  if (key === 'logout') {
    Modal.confirm({
      title: '提示',
      content: '确定要退出登录吗？',
      okText: '确定',
      cancelText: '取消',
      onOk: async () => {
        await authStore.logout();
        message.success('已退出登录');
        router.push('/login');
      },
    });
  } else if (key === 'changePassword') {
    router.push('/change-password');
  }
}

function handleMenuClick({ key }: { key: string }) {
  router.push(key);
}
</script>

<template>
  <RouterView v-if="!showLayout" />
  <a-layout v-else style="min-height: 100vh">
    <a-layout-sider width="220" theme="dark" :trigger="null" collapsible>
      <div class="logo">
        <div class="logo-icon">EM</div>
        <div class="logo-text">员工管理系统</div>
      </div>
      <a-menu
        v-model:selectedKeys="selectedKey"
        theme="dark"
        mode="inline"
        :items="antdMenuItems"
        @click="handleMenuClick"
      />
    </a-layout-sider>
    <a-layout>
      <a-layout-header class="header">
        <span class="header-title">{{ route.meta?.title || '管理系统' }}</span>
        <div class="header-right">
          <a-dropdown :menu="{ items: userMenuItems, onClick: handleUserMenuClick }" placement="bottomRight">
            <div class="user-info">
              <a-avatar :style="{ backgroundColor: '#7265e6', verticalAlign: 'middle' }" size="small">
                {{ authStore.userInfo?.nickname?.charAt(0) || authStore.userInfo?.username?.charAt(0) || 'U' }}
              </a-avatar>
              <span class="user-name">
                {{ authStore.userInfo?.nickname || authStore.userInfo?.username }}
              </span>
              <a-tag color="blue" class="role-tag">
                {{ authStore.userInfo?.roleName }}
              </a-tag>
            </div>
          </a-dropdown>
        </div>
      </a-layout-header>
      <a-layout-content class="content">
        <RouterView />
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<style scoped>
.logo {
  height: 64px;
  display: flex;
  align-items: center;
  padding: 0 20px;
  gap: 12px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.logo-icon {
  width: 36px;
  height: 36px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: bold;
  font-size: 14px;
}

.logo-text {
  color: white;
  font-size: 16px;
  font-weight: 600;
}

.header {
  background: white;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-title {
  font-size: 18px;
  font-weight: 600;
  color: #2c3e50;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 12px;
  border-radius: 20px;
  transition: background-color 0.2s;
}

.user-info:hover {
  background-color: #f5f7fa;
}

.user-name {
  font-size: 14px;
  color: #2c3e50;
  font-weight: 500;
}

.role-tag {
  margin-left: 4px;
}

.content {
  background: #f5f7fa;
  padding: 24px;
}
</style>
