<template>
  <div class="page-container">
    <div class="content-wrapper">
      <a-card class="main-card" :bordered="false">
        <div class="header-section">
          <div class="title-group">
            <h1 class="page-title">公告管理</h1>
            <p class="subtitle">Announcement Management</p>
          </div>
          <div class="header-actions">
            <a-button type="primary" class="create-btn" @click="goCreate">
              <template #icon><PlusOutlined /></template>
              新建公告
            </a-button>
          </div>
        </div>

        <div class="filter-section">
          <a-space :size="16" wrap>
            <a-input-search
              v-model:value="searchKeyword"
              placeholder="搜索公告标题"
              style="width: 240px"
              allow-clear
              @search="handleSearch"
              @change="handleSearchChange"
            />
            <a-select
              v-model:value="filterStatus"
              placeholder="状态筛选"
              style="width: 160px"
              allow-clear
              @change="handleFilterChange"
            >
              <a-select-option :value="0">草稿</a-select-option>
              <a-select-option :value="1">待发布</a-select-option>
              <a-select-option :value="2">已发布</a-select-option>
              <a-select-option :value="3">已归档</a-select-option>
            </a-select>
          </a-space>
        </div>

        <a-table
          :columns="columns"
          :data-source="announcementStore.announcementList"
          :loading="announcementStore.loading"
          row-key="id"
          :pagination="paginationConfig"
          class="modern-table"
          :scroll="{ x: 1000 }"
          @change="handleTableChange"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'title'">
              <div class="title-cell">
                <a-tag v-if="record.isPinned" color="red">
                  <PushpinOutlined />
                </a-tag>
                <a-tag v-if="record.isImportant" color="orange">!</a-tag>
                <span class="title-text" @click="viewDetail(record)">{{ record.title }}</span>
              </div>
            </template>
            <template v-if="column.key === 'status'">
              <a-tag :color="getStatusColor(record.status)">{{ getStatusText(record.status) }}</a-tag>
            </template>
            <template v-if="column.key === 'visibilityType'">
              <span>{{ getVisibilityText(record.visibilityType) }}</span>
            </template>
            <template v-if="column.key === 'readStats'">
              <span v-if="record.totalTargetCount">
                <a-progress
                  :percent="Math.round(((record.readCount || 0) / record.totalTargetCount) * 100)"
                  size="small"
                  style="width: 120px"
                />
                <span class="read-count-text">{{ record.readCount || 0 }}/{{ record.totalTargetCount }}</span>
              </span>
              <span v-else>-</span>
            </template>
            <template v-if="column.key === 'publishTime'">
              <span>{{ formatTime(record.publishTime || record.createTime) }}</span>
            </template>
            <template v-if="column.key === 'action'">
              <a-space size="small">
                <a-button type="link" size="small" @click="viewDetail(record)">
                  <EyeOutlined /> 详情
                </a-button>
                <a-button
                  v-if="record.status === 0"
                  type="link"
                  size="small"
                  @click="editAnnouncement(record)"
                >
                  <EditOutlined /> 编辑
                </a-button>
                <a-button
                  v-if="record.status === 0 || record.status === 1"
                  type="link"
                  size="small"
                  @click="handlePublish(record)"
                >
                  <SendOutlined /> 发布
                </a-button>
                <a-button
                  type="link"
                  size="small"
                  @click="handleTogglePin(record)"
                >
                  <template v-if="record.isPinned"><PushpinOutlined /> 取消置顶</template>
                  <template v-else><PushpinOutlined /> 置顶</template>
                </a-button>
                <a-button
                  v-if="record.status === 2"
                  type="link"
                  size="small"
                  @click="handleArchive(record)"
                >
                  <InboxOutlined /> 归档
                </a-button>
                <a-button
                  v-if="record.status === 0"
                  type="link"
                  size="small"
                  danger
                  @click="handleDelete(record)"
                >
                  <DeleteOutlined /> 删除
                </a-button>
              </a-space>
            </template>
          </template>
        </a-table>
      </a-card>

      <a-modal
        v-model:open="statsModalOpen"
        title="阅读统计"
        :width="900"
        :footer="null"
      >
        <div v-if="announcementStore.readStats">
          <a-row :gutter="24">
            <a-col :span="12">
              <a-statistic title="已读人数" :value="announcementStore.readStats.readCount" />
              <a-divider />
              <div class="stats-list-title">已读名单</div>
              <a-table
                :columns="readColumns"
                :data-source="announcementStore.readStats.readList"
                row-key="id"
                size="small"
                :pagination="{ pageSize: 5 }"
                :scroll="{ y: 300 }"
              >
                <template #bodyCell="{ column, record }">
                  <template v-if="column.key === 'readTime'">
                    {{ formatTime(record.readTime) }}
                  </template>
                </template>
              </a-table>
            </a-col>
            <a-col :span="12">
              <a-statistic title="未读人数" :value="announcementStore.readStats.unreadCount" />
              <a-divider />
              <div class="stats-list-title">未读名单</div>
              <a-table
                :columns="unreadColumns"
                :data-source="announcementStore.readStats.unreadList"
                row-key="id"
                size="small"
                :pagination="{ pageSize: 5 }"
                :scroll="{ y: 300 }"
              />
            </a-col>
          </a-row>
        </div>
      </a-modal>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { Modal, message } from 'ant-design-vue';
import {
  PlusOutlined,
  PushpinOutlined,
  EyeOutlined,
  EditOutlined,
  SendOutlined,
  InboxOutlined,
  DeleteOutlined,
} from '@ant-design/icons-vue';
import dayjs from 'dayjs';
import { useAnnouncementStore, type Announcement } from '../stores/announcement';

const router = useRouter();
const announcementStore = useAnnouncementStore();

const searchKeyword = ref('');
const filterStatus = ref<number | undefined>();
const pageNum = ref(1);
const pageSize = ref(10);
const statsModalOpen = ref(false);

const paginationConfig = computed(() => ({
  current: pageNum.value,
  pageSize: pageSize.value,
  total: announcementStore.announcementListTotal,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条公告`,
}));

const columns = [
  { title: '标题', dataIndex: 'title', key: 'title', width: '25%' },
  { title: '状态', dataIndex: 'status', key: 'status', width: '10%' },
  { title: '可见范围', dataIndex: 'visibilityType', key: 'visibilityType', width: '10%' },
  { title: '创建人', dataIndex: 'creatorName', key: 'creatorName', width: '10%' },
  { title: '阅读情况', dataIndex: 'readStats', key: 'readStats', width: '18%' },
  { title: '发布时间', dataIndex: 'publishTime', key: 'publishTime', width: '12%' },
  { title: '操作', dataIndex: 'action', key: 'action', width: '25%', fixed: 'right' as const },
];

const readColumns = [
  { title: '姓名', dataIndex: 'userName', key: 'userName' },
  { title: '阅读时间', dataIndex: 'readTime', key: 'readTime' },
];

const unreadColumns = [
  { title: '用户名', dataIndex: 'username', key: 'username' },
  { title: '昵称', dataIndex: 'nickname', key: 'nickname' },
];

const formatTime = (time?: string) => {
  if (!time) return '-';
  return dayjs(time).format('YYYY-MM-DD HH:mm');
};

const getStatusText = (status?: number) => {
  const map: Record<number, string> = {
    0: '草稿',
    1: '待发布',
    2: '已发布',
    3: '已归档',
  };
  return status !== undefined ? map[status] || '-' : '-';
};

const getStatusColor = (status?: number) => {
  const map: Record<number, string> = {
    0: 'default',
    1: 'processing',
    2: 'success',
    3: 'warning',
  };
  return status !== undefined ? map[status] || 'default' : 'default';
};

const getVisibilityText = (type?: number) => {
  const map: Record<number, string> = {
    1: '全员可见',
    2: '指定部门',
    3: '指定角色',
  };
  return type !== undefined ? map[type] || '-' : '-';
};

const fetchList = () => {
  announcementStore.fetchAdminList({
    title: searchKeyword.value || undefined,
    status: filterStatus.value,
    pageNum: pageNum.value,
    pageSize: pageSize.value,
  });
};

const handleSearch = () => {
  pageNum.value = 1;
  fetchList();
};

const handleSearchChange = () => {
  if (!searchKeyword.value) {
    pageNum.value = 1;
    fetchList();
  }
};

const handleFilterChange = () => {
  pageNum.value = 1;
  fetchList();
};

const handleTableChange = (pag: any) => {
  pageNum.value = pag.current;
  pageSize.value = pag.pageSize;
  fetchList();
};

const goCreate = () => {
  router.push('/admin/announcements/new');
};

const editAnnouncement = (record: Announcement) => {
  router.push(`/admin/announcements/edit/${record.id}`);
};

const viewDetail = async (record: Announcement) => {
  if (!record.id) return;
  try {
    await announcementStore.fetchReadStats(record.id);
    statsModalOpen.value = true;
  } catch (error) {
    message.error('获取阅读统计失败');
  }
};

const handlePublish = (record: Announcement) => {
  if (!record.id) return;
  Modal.confirm({
    title: '确认发布',
    content: record.effectiveTime
      ? `公告将按照设定的生效时间发布，确定要发布吗？`
      : '确定要立即发布这篇公告吗？',
    okText: '确定',
    cancelText: '取消',
    onOk: async () => {
      const result = await announcementStore.publish(record.id!);
      if (result) {
        message.success('发布成功');
        fetchList();
      }
    },
  });
};

const handleTogglePin = (record: Announcement) => {
  if (!record.id) return;
  announcementStore.togglePin(record.id, !record.isPinned).then(() => {
    message.success(record.isPinned ? '已取消置顶' : '已置顶');
    fetchList();
  });
};

const handleArchive = (record: Announcement) => {
  if (!record.id) return;
  Modal.confirm({
    title: '确认归档',
    content: '确定要归档这篇公告吗？归档后将不再在列表中显示。',
    okText: '确定',
    okType: 'warning' as const,
    cancelText: '取消',
    onOk: async () => {
      const result = await announcementStore.archive(record.id!);
      if (result) {
        message.success('归档成功');
        fetchList();
      }
    },
  });
};

const handleDelete = (record: Announcement) => {
  if (!record.id) return;
  Modal.confirm({
    title: '确认删除',
    content: '确定要删除这篇公告吗？此操作不可恢复。',
    okText: '确定',
    okType: 'danger' as const,
    cancelText: '取消',
    onOk: async () => {
      const result = await announcementStore.delete(record.id!);
      if (result) {
        message.success('删除成功');
        fetchList();
      }
    },
  });
};

onMounted(() => {
  fetchList();
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
  max-width: 1600px;
}

.main-card {
  border-radius: 14px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
  transition: all 0.3s ease;
  overflow: hidden;
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

.create-btn {
  height: 40px;
  border-radius: 20px;
  font-weight: 600;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  box-shadow: 0 4px 12px rgba(118, 75, 162, 0.3);
}

.filter-section {
  margin-bottom: 20px;
  padding: 16px;
  background: #fafafa;
  border-radius: 12px;
}

.title-cell {
  display: flex;
  align-items: center;
  gap: 6px;
}

.title-text {
  cursor: pointer;
  color: #1890ff;
}

.title-text:hover {
  text-decoration: underline;
}

.read-count-text {
  margin-left: 8px;
  font-size: 12px;
  color: #8c8c8c;
}

.stats-list-title {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 12px;
}

.modern-table :deep(.ant-table-thead > tr > th) {
  background: #fafafa;
  font-weight: 600;
}

@media (max-width: 768px) {
  .page-container {
    padding: 16px 12px;
  }

  .header-section {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }

  .header-actions {
    width: 100%;
  }

  .create-btn {
    width: 100%;
  }
}
</style>
