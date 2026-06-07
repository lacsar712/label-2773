<template>
  <div class="page-container">
    <div class="content-wrapper">
      <div v-if="myChecklist" class="my-checklist-layout">
        <a-card class="welcome-card" :bordered="false">
          <div class="welcome-content">
            <div class="welcome-left">
              <h1 class="welcome-title">
                欢迎加入，{{ myChecklist.employeeName }}！ 👋
              </h1>
              <p class="welcome-subtitle">
                以下是您的入职清单，请按步骤完成各项待办事项
              </p>
              <div class="welcome-meta">
                <a-tag color="blue">入职日期：{{ myChecklist.hireDate }}</a-tag>
                <a-tag color="purple">{{ myChecklist.departmentName }}</a-tag>
                <a-tag color="cyan">{{ myChecklist.position }}</a-tag>
                <a-tag v-if="myChecklist.mentorName" color="gold">导师：{{ myChecklist.mentorName }}</a-tag>
              </div>
            </div>
            <div class="welcome-right">
              <div class="progress-ring">
                <a-progress
                  type="circle"
                  :percent="myChecklist.progress || 0"
                  :size="120"
                  :stroke-width="10"
                  :stroke-color="progressColor(myChecklist.progress)"
                />
              </div>
            </div>
          </div>
        </a-card>

        <div class="stages-overview">
          <div
            v-for="stage in [1, 2, 3, 4]"
            :key="stage"
            class="stage-card"
            :class="{ 'stage-current': isCurrentStage(stage) }"
            :style="{ borderTopColor: stageColor(stage) }"
          >
            <div class="stage-header">
              <span class="stage-emoji">{{ stageEmoji(stage) }}</span>
              <span class="stage-name">{{ getStageText(stage) }}</span>
            </div>
            <div class="stage-stats">
              <span class="stage-completed">{{ getStageCompletedCount(stage) }}</span>
              <span class="stage-divider">/</span>
              <span class="stage-total">{{ getStageTotalCount(stage) }}</span>
            </div>
            <a-progress
              :percent="getStageProgress(stage)"
              :size="['100%', 6]"
              :stroke-color="stageColor(stage)"
              :show-info="false"
            />
          </div>
        </div>

        <a-card class="tasks-card" :bordered="false">
          <a-tabs v-model:activeKey="activeStage" size="large" class="tasks-tabs">
            <a-tab-pane key="0" tab="全部待办">
              <div class="tasks-list">
                <TaskItem
                  v-for="item in allItems"
                  :key="item.id"
                  :item="item"
                  :can-complete="canCompleteItem(item)"
                  @complete="handleComplete"
                />
                <a-empty v-if="allItems.length === 0" description="暂无待办事项" />
              </div>
            </a-tab-pane>
            <a-tab-pane
              v-for="stage in [1, 2, 3, 4]"
              :key="String(stage)"
              :tab="getStageTabTitle(stage)"
            >
              <div class="tasks-list">
                <TaskItem
                  v-for="item in getStageItems(stage)"
                  :key="item.id"
                  :item="item"
                  :can-complete="canCompleteItem(item)"
                  @complete="handleComplete"
                />
                <a-empty v-if="getStageItems(stage).length === 0" description="该阶段暂无待办事项" />
              </div>
            </a-tab-pane>
          </a-tabs>
        </a-card>
      </div>

      <a-card v-else class="no-checklist-card" :bordered="false">
        <a-empty description="您还没有入职清单，请联系HR">
          <template #image>
            <div class="empty-icon">📋</div>
          </template>
        </a-empty>
      </a-card>

      <a-modal
        v-model:open="completeModalVisible"
        title="确认完成"
        @ok="confirmComplete"
        ok-text="确认完成"
        cancel-text="取消"
        centered
      >
        <p>请确认是否已完成：<b>{{ completingItem?.itemName }}</b></p>
        <a-form layout="vertical">
          <a-form-item label="备注（可选）">
            <a-textarea v-model:value="completeRemark" :rows="3" placeholder="请输入备注..." />
          </a-form-item>
        </a-form>
      </a-modal>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, computed, onMounted, defineComponent, h } from 'vue';
import { useOnboardingChecklistStore, type OnboardingChecklistItem, type ItemCompleteDTO } from '../stores/onboardingChecklist';
import { useAuthStore } from '../stores/auth';
import { message } from 'ant-design-vue';
import dayjs from 'dayjs';

const checklistStore = useOnboardingChecklistStore();
const authStore = useAuthStore();

const activeStage = ref('0');
const completeModalVisible = ref(false);
const completingItem = ref<OnboardingChecklistItem | null>(null);
const completeRemark = ref('');

const myChecklist = computed(() => checklistStore.currentChecklist);

const TaskItem = defineComponent({
  name: 'TaskItem',
  props: {
    item: { type: Object as () => OnboardingChecklistItem, required: true },
    canComplete: { type: Boolean, default: false },
  },
  emits: ['complete'],
  setup(props, { emit }) {
    const getStatusText = (status?: number) => {
      switch (status) {
        case 0: return '未开始';
        case 1: return '进行中';
        case 2: return '已完成';
        case 3: return '已逾期';
        default: return '-';
      }
    };
    const getStatusColor = (status?: number) => {
      switch (status) {
        case 0: return 'default';
        case 1: return 'blue';
        case 2: return 'green';
        case 3: return 'red';
        default: return 'default';
      }
    };
    const formatDateTime = (dt?: string) => {
      if (!dt) return '-';
      return dayjs(dt).format('YYYY-MM-DD HH:mm');
    };
    const isOverdue = (item: OnboardingChecklistItem) => {
      return item.status === 3 || (item.status !== 2 && dayjs(item.dueDate).isBefore(dayjs(), 'day'));
    };
    return () => h('div', {
      class: [
        'task-item',
        {
          'task-completed': props.item.status === 2,
          'task-overdue': isOverdue(props.item),
        }
      ]
    }, [
      h('div', { class: 'task-checkbox' }, [
        h('a-checkbox', {
          checked: props.item.status === 2,
          disabled: !props.canComplete,
          onChange: (e: any) => {
            if (e.target.checked) emit('complete', props.item);
          }
        })
      ]),
      h('div', { class: 'task-content' }, [
        h('div', { class: 'task-header' }, [
          h('span', {
            class: ['task-name', { 'task-name-done': props.item.status === 2 }]
          }, props.item.itemName),
          h('a-tag', { color: getStatusColor(props.item.status), class: 'task-status' }, getStatusText(props.item.status)),
          props.item.isTemporary ? h('a-tag', { color: 'purple', class: 'task-status' }, '临时') : null,
        ]),
        props.item.itemDescription ? h('div', { class: 'task-desc' }, props.item.itemDescription) : null,
        h('div', { class: 'task-meta' }, [
          h('span', { class: 'meta-item' }, [
            h('span', { class: 'meta-icon' }, '👤'),
            `责任人：${props.item.responsibleUserName || '-'}`
          ]),
          h('span', { class: ['meta-item', { 'meta-overdue': isOverdue(props.item) }] }, [
            h('span', { class: 'meta-icon' }, '📅'),
            `截止：${props.item.dueDate}`
          ]),
          props.item.status === 2 ? h('span', { class: 'meta-item' }, [
            h('span', { class: 'meta-icon' }, '✅'),
            `完成人：${props.item.completedUserName || '-'}`
          ]) : null,
          props.item.status === 2 ? h('span', { class: 'meta-item' }, [
            h('span', { class: 'meta-icon' }, '⏰'),
            `完成时间：${formatDateTime(props.item.completedTime)}`
          ]) : null,
        ]),
        props.item.remark && props.item.status === 2
          ? h('div', { class: 'task-remark' }, `备注：${props.item.remark}`)
          : null,
      ])
    ]);
  }
});

const getStageText = (stage?: number) => {
  switch (stage) {
    case 1: return '入职前';
    case 2: return '首日';
    case 3: return '首周';
    case 4: return '首月';
    default: return '-';
  }
};

const stageEmoji = (stage?: number) => {
  switch (stage) {
    case 1: return '📋';
    case 2: return '🎉';
    case 3: return '🚀';
    case 4: return '💪';
    default: return '📌';
  }
};

const stageColor = (stage?: number) => {
  switch (stage) {
    case 1: return '#fa8c16';
    case 2: return '#1890ff';
    case 3: return '#13c2c2';
    case 4: return '#52c41a';
    default: return '#8c8c8c';
  }
};

const progressColor = (progress?: number) => {
  if (!progress) return '#d9d9d9';
  if (progress < 50) return '#ff4d4f';
  if (progress < 80) return '#faad14';
  return '#52c41a';
};

const getStageTabTitle = (stage: number) => {
  const items = getStageItems(stage);
  const completed = items.filter((i) => i.status === 2).length;
  return `${stageEmoji(stage)} ${getStageText(stage)} (${completed}/${items.length})`;
};

const allItems = computed(() => {
  if (!myChecklist.value?.items) return [];
  return [...myChecklist.value.items].sort((a, b) => {
    const stageDiff = (a.stage || 0) - (b.stage || 0);
    if (stageDiff !== 0) return stageDiff;
    return (a.sortOrder || 0) - (b.sortOrder || 0);
  });
});

const getStageItems = (stage: number): OnboardingChecklistItem[] => {
  if (!myChecklist.value?.items) return [];
  return myChecklist.value.items
    .filter((i) => i.stage === stage)
    .sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0));
};

const getStageTotalCount = (stage: number) => getStageItems(stage).length;
const getStageCompletedCount = (stage: number) => getStageItems(stage).filter((i) => i.status === 2).length;

const getStageProgress = (stage: number): number => {
  if (!myChecklist.value) return 0;
  switch (stage) {
    case 1: return myChecklist.value.preJoinProgress || 0;
    case 2: return myChecklist.value.firstDayProgress || 0;
    case 3: return myChecklist.value.firstWeekProgress || 0;
    case 4: return myChecklist.value.firstMonthProgress || 0;
    default: return 0;
  }
};

const isCurrentStage = (stage: number): boolean => {
  if (!myChecklist.value) return false;
  const progress = getStageProgress(stage);
  if (progress === 100) return false;
  for (let s = 1; s < stage; s++) {
    if (getStageProgress(s) < 100) return false;
  }
  return true;
};

const canCompleteItem = (item: OnboardingChecklistItem) => {
  if (item.status === 2) return false;
  if (myChecklist.value?.status === 2) return false;
  if (authStore.hasRole(['ADMIN', 'HR'])) return true;
  const currentUserId = authStore.userInfo?.userId;
  const currentEmployeeId = authStore.userInfo?.employeeId;
  if (item.responsibleUserId && currentUserId && item.responsibleUserId === Number(currentUserId)) return true;
  if (item.responsibleUserId && currentEmployeeId && item.responsibleUserId === Number(currentEmployeeId)) return true;
  return false;
};

onMounted(async () => {
  const employeeId = authStore.userInfo?.employeeId;
  if (employeeId) {
    await checklistStore.fetchByEmployeeId(Number(employeeId));
  }
});

const handleComplete = (item: OnboardingChecklistItem) => {
  completingItem.value = item;
  completeRemark.value = '';
  completeModalVisible.value = true;
};

const confirmComplete = async () => {
  if (!completingItem.value?.id) return;
  const dto: ItemCompleteDTO = {
    itemId: completingItem.value.id,
    remark: completeRemark.value || undefined,
  };
  const success = await checklistStore.completeItem(dto);
  if (success) {
    completeModalVisible.value = false;
    message.success('完成成功');
  }
};
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

.my-checklist-layout {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.welcome-card {
  border-radius: 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  box-shadow: 0 8px 24px rgba(102, 126, 234, 0.3);
  overflow: hidden;
}

.welcome-card :deep(.ant-card-body) {
  padding: 0;
}

.welcome-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 32px;
  color: white;
}

.welcome-title {
  margin: 0;
  font-size: 28px;
  font-weight: 700;
  color: white;
}

.welcome-subtitle {
  margin: 8px 0 16px 0;
  font-size: 14px;
  opacity: 0.9;
}

.welcome-meta {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.welcome-meta :deep(.ant-tag) {
  background: rgba(255, 255, 255, 0.2);
  border: none;
  color: white;
}

.progress-ring {
  background: rgba(255, 255, 255, 0.15);
  border-radius: 50%;
  padding: 8px;
}

.progress-ring :deep(.ant-progress-text) {
  color: white;
  font-size: 20px;
  font-weight: 700;
}

.stages-overview {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.stage-card {
  background: white;
  border-radius: 14px;
  padding: 20px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
  border-top: 4px solid;
  transition: all 0.3s;
}

.stage-card.stage-current {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.stage-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.stage-emoji {
  font-size: 24px;
}

.stage-name {
  font-size: 15px;
  font-weight: 600;
  color: #2c3e50;
}

.stage-stats {
  display: flex;
  align-items: baseline;
  gap: 4px;
  margin-bottom: 8px;
}

.stage-completed {
  font-size: 28px;
  font-weight: 700;
  color: #2c3e50;
}

.stage-divider {
  font-size: 18px;
  color: #8c8c8c;
}

.stage-total {
  font-size: 16px;
  color: #8c8c8c;
}

.tasks-card {
  border-radius: 14px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
}

.tasks-tabs {
  margin-bottom: 0;
}

.tasks-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding-top: 8px;
}

.task-item {
  display: grid;
  grid-template-columns: 40px 1fr;
  gap: 16px;
  padding: 16px 20px;
  background: #fafafa;
  border-radius: 12px;
  border: 1px solid transparent;
  transition: all 0.2s;
}

.task-item:hover {
  background: #f5f7fa;
  border-color: #e8e8e8;
}

.task-item.task-completed {
  background: #f6ffed;
  border-color: #b7eb8f;
  opacity: 0.85;
}

.task-item.task-overdue {
  background: #fff1f0;
  border-color: #ffa39e;
}

.task-checkbox {
  padding-top: 4px;
}

.task-content {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.task-header {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.task-name {
  font-size: 15px;
  font-weight: 600;
  color: #2c3e50;
}

.task-name.task-name-done {
  text-decoration: line-through;
  color: #8c8c8c;
}

.task-status {
  font-size: 11px;
}

.task-desc {
  font-size: 13px;
  color: #595959;
}

.task-meta {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  font-size: 12px;
  color: #8c8c8c;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.meta-icon {
  font-size: 14px;
}

.meta-overdue {
  color: #ff4d4f;
  font-weight: 600;
}

.task-remark {
  font-size: 12px;
  color: #595959;
  padding: 8px 12px;
  background: white;
  border-radius: 8px;
}

.no-checklist-card {
  border-radius: 14px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
}

.empty-icon {
  font-size: 80px;
  opacity: 0.5;
}

@media (max-width: 768px) {
  .stages-overview {
    grid-template-columns: repeat(2, 1fr);
  }

  .welcome-content {
    flex-direction: column;
    gap: 20px;
    text-align: center;
  }

  .welcome-meta {
    justify-content: center;
  }
}
</style>
