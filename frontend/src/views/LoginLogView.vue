<template>
  <div class="page-container">
    <div class="content-wrapper">
      <a-card class="main-card" :bordered="false">
        <div class="header-section">
          <div class="title-group">
            <h1 class="page-title">登录日志</h1>
            <p class="subtitle">Login Logs</p>
          </div>
        </div>

        <a-form layout="inline" class="filter-form" @finish="handleSearch">
          <a-form-item v-if="isAdmin" label="用户名">
            <a-input
              v-model:value="filterUsername"
              placeholder="请输入用户名"
              style="width: 180px"
              allow-clear
            />
          </a-form-item>
          <a-form-item label="状态">
            <a-select
              v-model:value="filterStatus"
              allow-clear
              placeholder="全部状态"
              style="width: 140px"
            >
              <a-select-option :value="1">成功</a-select-option>
              <a-select-option :value="0">失败</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="操作类型">
            <a-select
              v-model:value="filterLoginType"
              allow-clear
              placeholder="全部类型"
              style="width: 140px"
            >
              <a-select-option value="1">登录</a-select-option>
              <a-select-option value="2">登出</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="时间范围">
            <a-range-picker
              v-model:value="dateRange"
              value-format="YYYY-MM-DD HH:mm:ss"
              style="width: 280px"
              show-time
            />
          </a-form-item>
          <a-form-item>
            <a-space>
              <a-button type="primary" html-type="submit" class="search-btn">
                <template #icon><SearchOutlined /></template>
                查询
              </a-button>
              <a-button @click="handleReset">重置</a-button>
            </a-space>
          </a-form-item>
        </a-form>

        <a-table
          :columns="columns"
          :data-source="records"
          :loading="loading"
          row-key="id"
          :pagination="paginationConfig"
          class="modern-table"
          :scroll="{ x: 1400 }"
          @change="handleTableChange"
        >
          <template #bodyCell="{ column, record, index }">
            <template v-if="column.key === 'index'">
              <span>{{ (pageNum - 1) * pageSize + index + 1 }}</span>
            </template>
            <template v-if="column.key === 'loginType'">
              <a-tag :color="getLoginTypeColor(record.loginType)">
                {{ getLoginTypeText(record.loginType) }}
              </a-tag>
            </template>
            <template v-if="column.key === 'status'">
              <a-tag :color="record.status === 1 ? 'green' : 'red'">
                {{ record.status === 1 ? '成功' : '失败' }}
              </a-tag>
            </template>
            <template v-if="column.key === 'loginTime'">
              <span>{{ record.loginTime }}</span>
            </template>
          </template>
          <template #emptyText>
            <a-empty description="暂无登录日志数据" />
          </template>
        </a-table>
      </a-card>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, computed, onMounted } from 'vue';
import request from '../utils/request';
import { useAuthStore } from '../stores/auth';
import { SearchOutlined } from '@ant-design/icons-vue';

const authStore = useAuthStore();

interface LoginLogRecord {
  id: number;
  userId: number;
  username: string;
  loginType: string;
  status: number;
  ipAddress: string;
  loginLocation?: string;
  browser: string;
  os: string;
  device: string;
  message: string;
  loginTime: string;
}

interface PageResult<T> {
  records: T[];
  total: number;
  size: number;
  current: number;
  pages: number;
}

interface Result<T> {
  code: number;
  message: string;
  data: T;
}

const isAdmin = computed(() => authStore.hasRole('ADMIN'));

const filterUsername = ref<string>('');
const filterStatus = ref<number | undefined>();
const filterLoginType = ref<string | undefined>();
const dateRange = ref<string[]>([]);
const pageNum = ref(1);
const pageSize = ref(10);
const loading = ref(false);
const records = ref<LoginLogRecord[]>([]);
const total = ref(0);

const paginationConfig = computed(() => ({
  current: pageNum.value,
  pageSize: pageSize.value,
  total: total.value,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (t: number) => `共 ${t} 条记录`,
}));

const columns = [
  { title: '序号', key: 'index', width: '7%' },
  { title: '用户名', dataIndex: 'username', key: 'username', width: '10%' },
  { title: '操作类型', dataIndex: 'loginType', key: 'loginType', width: '10%' },
  { title: '状态', dataIndex: 'status', key: 'status', width: '8%' },
  { title: 'IP地址', dataIndex: 'ipAddress', key: 'ipAddress', width: '12%' },
  { title: '浏览器', dataIndex: 'browser', key: 'browser', width: '12%' },
  { title: '操作系统', dataIndex: 'os', key: 'os', width: '12%' },
  { title: '设备', dataIndex: 'device', key: 'device', width: '10%' },
  { title: '提示信息', dataIndex: 'message', key: 'message', width: '15%' },
  { title: '登录时间', dataIndex: 'loginTime', key: 'loginTime', width: '14%' },
];

const getLoginTypeColor = (type?: string) => {
  switch (type) {
    case '1': return 'blue';
    case '2': return 'orange';
    default: return 'default';
  }
};

const getLoginTypeText = (type?: string) => {
  switch (type) {
    case '1': return '登录';
    case '2': return '登出';
    default: return type || '-';
  }
};

const getQueryParams = () => {
  const params: Record<string, any> = {
    pageNum: pageNum.value,
    pageSize: pageSize.value,
  };
  if (filterUsername.value) {
    params.username = filterUsername.value;
  }
  if (filterStatus.value !== undefined) {
    params.status = filterStatus.value;
  }
  if (filterLoginType.value) {
    params.loginType = filterLoginType.value;
  }
  if (dateRange.value?.[0]) {
    params.startTime = dateRange.value[0];
  }
  if (dateRange.value?.[1]) {
    params.endTime = dateRange.value[1];
  }
  return params;
};

const fetchData = async () => {
  loading.value = true;
  try {
    const res = await request.get<any, Result<PageResult<LoginLogRecord>>>('/login-logs', {
      params: getQueryParams(),
    });
    records.value = res.data.records || [];
    total.value = res.data.total || 0;
  } catch (e) {
    console.error('获取登录日志失败', e);
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  pageNum.value = 1;
  fetchData();
};

const handleTableChange = (pag: any) => {
  pageNum.value = pag.current;
  pageSize.value = pag.pageSize;
  fetchData();
};

const handleReset = () => {
  filterUsername.value = '';
  filterStatus.value = undefined;
  filterLoginType.value = '';
  dateRange.value = [];
  pageNum.value = 1;
  fetchData();
};

onMounted(() => {
  fetchData();
});
</script>

<style scoped>
.page-container {
  min-height: 100vh;
  padding: 24px 24px;
  display: flex;
  justify-content: center;
  align-items: flex-start;
}

.content-wrapper {
  width: 100%;
  max-width: 1400px;
}

.main-card {
  border-radius: 14px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
  transition: all 0.3s ease;
  overflow: hidden;
}

.main-card:hover {
  box-shadow: 0 6px 24px rgba(0, 0, 0, 0.08);
}

.header-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding: 0 8px;
}

.title-group {
  display: flex;
  flex-direction: column;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0;
  background: linear-gradient(135deg, #2c3e50 0%, #3498db 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.subtitle {
  color: #8c8c8c;
  margin: 4px 0 0 0;
  font-size: 13px;
}

.search-btn {
  height: 36px;
  padding: 0 20px;
  border-radius: 18px;
  font-weight: 600;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  box-shadow: 0 4px 12px rgba(118, 75, 162, 0.3);
  transition: all 0.3s ease;
}

.search-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 18px rgba(118, 75, 162, 0.4);
}

.filter-form {
  margin-bottom: 24px;
}

:deep(.ant-table-tbody > tr > td) {
  padding: 12px 16px;
}

@media (max-width: 768px) {
  .page-container {
    padding: 16px 12px;
  }

  .header-section {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .filter-form :deep(.ant-form-item) {
    margin-bottom: 12px;
  }
}
</style>
