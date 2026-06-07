<script setup lang="ts">
import { ref } from 'vue';
import { RouterView, useRoute } from 'vue-router';
import { TeamOutlined, UserOutlined } from '@ant-design/icons-vue';

const route = useRoute();
const selectedKey = ref([route.path]);

const menuItems = [
  {
    key: '/',
    icon: UserOutlined,
    label: '员工管理',
  },
  {
    key: '/departments',
    icon: TeamOutlined,
    label: '部门管理',
  },
];
</script>

<template>
  <a-layout style="min-height: 100vh">
    <a-layout-sider width="220" theme="dark" :trigger="null" collapsible>
      <div class="logo">
        <div class="logo-icon">EM</div>
        <div class="logo-text">员工管理系统</div>
      </div>
      <a-menu
        v-model:selectedKeys="selectedKey"
        theme="dark"
        mode="inline"
        :items="menuItems"
        router
      >
        <template #icon="{ icon }">
          <component :is="icon" />
        </template>
      </a-menu>
    </a-layout-sider>
    <a-layout>
      <a-layout-header class="header">
        <span class="header-title">{{ route.meta?.title || '管理系统' }}</span>
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

.content {
  background: #f5f7fa;
}
</style>
