<template>
  <div class="page-container">
    <div class="content-wrapper">
      <a-card class="main-card" :bordered="false" v-if="announcementStore.currentAnnouncement">
        <div class="back-section">
          <a-button type="link" @click="goBack">
            <ArrowLeftOutlined /> 返回列表
          </a-button>
        </div>

        <div class="header-section">
          <div class="item-tags">
            <a-tag v-if="announcement.isPinned" color="red">
              <PushpinOutlined /> 置顶
            </a-tag>
            <a-tag v-if="announcement.isImportant" color="orange">
              <ExclamationCircleOutlined /> 重要公告
            </a-tag>
            <a-tag v-if="!announcement.hasRead" color="blue">未读</a-tag>
          </div>
        </div>

        <h1 class="announcement-title">{{ announcement.title }}</h1>

        <div class="meta-section">
          <span class="meta-item">
            <UserOutlined /> 发布人：{{ announcement.creatorName }}
          </span>
          <span class="meta-item">
            <ClockCircleOutlined /> 发布时间：{{ formatTime(announcement.publishTime || announcement.createTime) }}
          </span>
          <span class="meta-item" v-if="announcement.effectiveTime">
            <CalendarOutlined /> 生效时间：{{ formatTime(announcement.effectiveTime) }}
          </span>
          <span class="meta-item" v-if="announcement.expireTime">
            <StopwatchOutlined /> 过期时间：{{ formatTime(announcement.expireTime) }}
          </span>
          <span class="meta-item" v-if="announcement.totalTargetCount">
            <EyeOutlined /> 阅读：{{ announcement.readCount || 0 }}/{{ announcement.totalTargetCount }}
          </span>
        </div>

        <a-divider />

        <div v-if="announcement.coverImage" class="cover-image-section">
          <img :src="announcement.coverImage" class="cover-image" alt="封面图" />
        </div>

        <div
          v-if="announcement.content"
          class="content-section"
          v-html="announcement.content"
        ></div>
        <a-empty v-else description="暂无正文内容" :image="null" />
      </a-card>

      <a-spin v-else size="large" class="loading-spin" />
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, computed, onMounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
import {
  ArrowLeftOutlined,
  PushpinOutlined,
  ExclamationCircleOutlined,
  UserOutlined,
  ClockCircleOutlined,
  CalendarOutlined,
  StopwatchOutlined,
  EyeOutlined,
} from '@ant-design/icons-vue';
import dayjs from 'dayjs';
import { useAnnouncementStore } from '../stores/announcement';

const route = useRoute();
const router = useRouter();
const announcementStore = useAnnouncementStore();

const announcement = computed(() => announcementStore.currentAnnouncement);

const formatTime = (time?: string) => {
  if (!time) return '-';
  return dayjs(time).format('YYYY-MM-DD HH:mm');
};

const goBack = () => {
  router.back();
};

const loadDetail = async () => {
  const id = Number(route.params.id);
  if (!id) return;
  try {
    await announcementStore.fetchMyDetail(id);
    if (!announcement.value?.hasRead) {
      await announcementStore.markAsRead(id);
    }
  } catch (error) {
    message.error('加载公告详情失败');
  }
};

watch(
  () => route.params.id,
  () => {
    loadDetail();
  }
);

onMounted(() => {
  loadDetail();
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
  max-width: 1000px;
}

.main-card {
  border-radius: 14px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
  transition: all 0.3s ease;
  overflow: hidden;
}

.loading-spin {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
}

.back-section {
  margin-bottom: 16px;
}

.header-section {
  margin-bottom: 16px;
}

.item-tags {
  display: flex;
  gap: 8px;
}

.announcement-title {
  font-size: 28px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0 0 20px 0;
  line-height: 1.4;
}

.meta-section {
  display: flex;
  flex-wrap: wrap;
  gap: 24px;
  color: #8c8c8c;
  font-size: 14px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
}

.cover-image-section {
  margin-bottom: 24px;
  text-align: center;
}

.cover-image {
  max-width: 100%;
  max-height: 400px;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.content-section {
  font-size: 15px;
  line-height: 1.8;
  color: #333;
}

.content-section :deep(img) {
  max-width: 100%;
  border-radius: 8px;
}

.content-section :deep(p) {
  margin-bottom: 16px;
}

.content-section :deep(h1),
.content-section :deep(h2),
.content-section :deep(h3) {
  margin-top: 24px;
  margin-bottom: 16px;
  font-weight: 600;
}

.content-section :deep(ul),
.content-section :deep(ol) {
  padding-left: 24px;
  margin-bottom: 16px;
}

.content-section :deep(li) {
  margin-bottom: 8px;
}

@media (max-width: 768px) {
  .page-container {
    padding: 16px 12px;
  }

  .announcement-title {
    font-size: 22px;
  }

  .meta-section {
    flex-direction: column;
    gap: 12px;
  }
}
</style>
