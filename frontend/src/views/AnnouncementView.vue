<template>
  <div class="page-container">
    <div class="content-wrapper">
      <a-card class="main-card" :bordered="false">
        <div class="header-section">
          <div class="title-group">
            <h1 class="page-title">公告通知</h1>
            <p class="subtitle">Announcements</p>
          </div>
          <div class="search-bar">
            <a-input-search
              v-model:value="searchKeyword"
              placeholder="搜索公告标题"
              style="width: 300px"
              allow-clear
              @search="handleSearch"
              @change="handleSearchChange"
            />
          </div>
        </div>

        <div v-if="announcementStore.loading" class="loading-container">
          <a-spin size="large" />
        </div>

        <div v-else class="announcement-list">
          <a-empty
            v-if="announcementStore.myAnnouncementList.length === 0"
            description="暂无公告"
            :image="null"
          />

          <div
            v-for="item in announcementStore.myAnnouncementList"
            :key="item.id"
            class="announcement-item"
            :class="{ pinned: item.isPinned, unread: !item.hasRead }"
            @click="viewDetail(item)"
          >
            <div class="item-header">
              <div class="item-tags">
                <a-tag v-if="item.isPinned" color="red" class="pin-tag">
                  <PushpinOutlined /> 置顶
                </a-tag>
                <a-tag v-if="item.isImportant" color="orange">
                  <ExclamationCircleOutlined /> 重要
                </a-tag>
                <a-tag v-if="!item.hasRead" color="blue">未读</a-tag>
              </div>
              <span class="item-time">
                <ClockCircleOutlined /> {{ formatTime(item.publishTime || item.createTime) }}
              </span>
            </div>
            <div class="item-title">{{ item.title }}</div>
            <div class="item-meta">
              <span class="creator">
                <UserOutlined /> {{ item.creatorName }}
              </span>
              <span class="read-count" v-if="item.totalTargetCount">
                阅读 {{ item.readCount || 0 }}/{{ item.totalTargetCount }}
              </span>
            </div>
          </div>
        </div>

        <div v-if="announcementStore.myAnnouncementListTotal > 0" class="pagination-section">
          <a-pagination
            v-model:current="pageNum"
            v-model:pageSize="pageSize"
            :total="announcementStore.myAnnouncementListTotal"
            :show-size-changer="true"
            :show-quick-jumper="true"
            :show-total="(total) => `共 ${total} 条公告`"
            @change="handlePageChange"
          />
        </div>
      </a-card>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import {
  PushpinOutlined,
  ClockCircleOutlined,
  UserOutlined,
  ExclamationCircleOutlined,
} from '@ant-design/icons-vue';
import dayjs from 'dayjs';
import { useAnnouncementStore, type Announcement } from '../stores/announcement';

const router = useRouter();
const announcementStore = useAnnouncementStore();

const searchKeyword = ref('');
const pageNum = ref(1);
const pageSize = ref(10);

const formatTime = (time?: string) => {
  if (!time) return '-';
  return dayjs(time).format('YYYY-MM-DD HH:mm');
};

const fetchList = async () => {
  await announcementStore.fetchMyList({
    title: searchKeyword.value || undefined,
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

const handlePageChange = (page: number, size: number) => {
  pageNum.value = page;
  pageSize.value = size;
  fetchList();
};

const viewDetail = (item: Announcement) => {
  router.push(`/announcements/${item.id}`);
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
  max-width: 1200px;
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

.loading-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 300px;
}

.announcement-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.announcement-item {
  padding: 20px 24px;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  background: white;
}

.announcement-item:hover {
  border-color: #1890ff;
  box-shadow: 0 4px 16px rgba(24, 144, 255, 0.12);
  transform: translateY(-2px);
}

.announcement-item.unread {
  background: linear-gradient(135deg, #e6f7ff 0%, #ffffff 100%);
  border-left: 4px solid #1890ff;
}

.announcement-item.pinned {
  background: linear-gradient(135deg, #fff1f0 0%, #ffffff 100%);
  border-left: 4px solid #f5222d;
}

.announcement-item.pinned.unread {
  background: linear-gradient(135deg, #fff1f0 0%, #e6f7ff 100%);
}

.item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.item-tags {
  display: flex;
  gap: 8px;
}

.pin-tag {
  font-weight: 600;
}

.item-time {
  color: #8c8c8c;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.item-title {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 12px;
  line-height: 1.5;
}

.item-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #8c8c8c;
  font-size: 13px;
}

.creator {
  display: flex;
  align-items: center;
  gap: 4px;
}

.pagination-section {
  margin-top: 24px;
  display: flex;
  justify-content: center;
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

  .search-bar {
    width: 100%;
  }

  .search-bar .ant-input-search {
    width: 100% !important;
  }
}
</style>
