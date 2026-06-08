<template>
  <div class="page-container">
    <div class="content-wrapper">
      <div v-if="checklistStore.currentChecklist" class="detail-layout">
        <a-card class="sidebar-card" :bordered="false">
          <div class="employee-header">
            <a-avatar :size="64" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);">
              {{ checklistStore.currentChecklist.employeeName?.charAt(0) }}
            </a-avatar>
            <div class="employee-info">
              <h2 class="employee-name">{{ checklistStore.currentChecklist.employeeName }}</h2>
              <p class="employee-meta">
                {{ checklistStore.currentChecklist.departmentName }} · {{ checklistStore.currentChecklist.position }}
              </p>
              <a-tag :color="getStatusColor(checklistStore.currentChecklist.status)">
                {{ getStatusText(checklistStore.currentChecklist.status) }}
              </a-tag>
            </div>
          </div>

          <a-divider />

          <div class="info-section">
            <div class="info-row">
              <span class="info-label">入职日期</span>
              <span class="info-value">{{ checklistStore.currentChecklist.hireDate }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">使用模板</span>
              <span class="info-value">{{ checklistStore.currentChecklist.templateName || '-' }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">导师</span>
              <span class="info-value">{{ checklistStore.currentChecklist.mentorName || '-' }}</span>
            </div>
          </div>

          <a-divider />

          <div class="overall-progress">
            <div class="progress-header">
              <span class="progress-label">整体进度</span>
              <span class="progress-value">{{ checklistStore.currentChecklist.progress || 0 }}%</span>
            </div>
            <a-progress
              :percent="checklistStore.currentChecklist.progress || 0"
              :size="['100%', 14]"
              :stroke-color="progressColor(checklistStore.currentChecklist.progress)"
              :show-info="false"
            />
          </div>

          <div class="stages-progress">
            <div
              v-for="stage in [1, 2, 3, 4]"
              :key="stage"
              class="stage-item"
              :class="{ 'stage-active': currentStage === stage }"
              @click="currentStage = stage"
            >
              <div class="stage-header">
                <div class="stage-icon" :style="{ background: stageColor(stage) }">
                  {{ stageEmoji(stage) }}
                </div>
                <div class="stage-info">
                  <span class="stage-name">{{ getStageText(stage) }}</span>
                  <span class="stage-count">{{ getStageItemCount(stage) }}项</span>
                </div>
              </div>
              <a-progress
                :percent="getStageProgress(stage)"
                :size="['100%', 6]"
                :stroke-color="stageColor(stage)"
                :show-info="false"
              />
            </div>
          </div>

          <a-divider v-if="canAddTemporary" />

          <div v-if="canAddTemporary" class="sidebar-actions">
            <a-button type="dashed" block @click="showTemporaryModal()">
              <template #icon><PlusOutlined /></template>
              追加临时待办
            </a-button>
          </div>
        </a-card>

        <a-card class="content-card" :bordered="false">
          <div class="content-header">
            <div class="stage-tabs">
              <a-radio-group v-model:value="currentStage" option-type="button" size="large">
                <a-radio-button :value="0">全部</a-radio-button>
                <a-radio-button :value="1">
                  <span class="tab-emoji">{{ stageEmoji(1) }}</span>
                  入职前
                  <a-tag color="orange" class="tab-count">{{ getStageItemCount(1) }}</a-tag>
                </a-radio-button>
                <a-radio-button :value="2">
                  <span class="tab-emoji">{{ stageEmoji(2) }}</span>
                  首日
                  <a-tag color="blue" class="tab-count">{{ getStageItemCount(2) }}</a-tag>
                </a-radio-button>
                <a-radio-button :value="3">
                  <span class="tab-emoji">{{ stageEmoji(3) }}</span>
                  首周
                  <a-tag color="cyan" class="tab-count">{{ getStageItemCount(3) }}</a-tag>
                </a-radio-button>
                <a-radio-button :value="4">
                  <span class="tab-emoji">{{ stageEmoji(4) }}</span>
                  首月
                  <a-tag color="green" class="tab-count">{{ getStageItemCount(4) }}</a-tag>
                </a-radio-button>
              </a-radio-group>
            </div>
            <div class="content-actions">
              <a-button @click="refreshDetail">
                <template #icon><ReloadOutlined /></template>
                刷新
              </a-button>
            </div>
          </div>

          <div class="items-container">
            <template v-for="stage in (currentStage === 0 ? [1, 2, 3, 4] : [currentStage])" :key="stage">
              <div v-if="getStageItems(stage).length > 0" class="stage-section">
                <div class="stage-section-header" :style="{ borderLeftColor: stageColor(stage) }">
                  <span class="stage-section-emoji">{{ stageEmoji(stage) }}</span>
                  <span class="stage-section-title">{{ getStageText(stage) }}</span>
                  <a-tag :color="getStageProgress(stage) === 100 ? 'green' : 'blue'">
                    {{ getStageCompletedCount(stage) }}/{{ getStageItemCount(stage) }}
                  </a-tag>
                </div>

                <div class="items-grid">
                  <div
                    v-for="item in getStageItems(stage)"
                    :key="item.id"
                    class="check-item-card"
                    :class="{
                      'item-completed': item.status === 2,
                      'item-overdue': item.status === 3,
                      'item-temporary': item.isTemporary,
                    }"
                  >
                    <div class="item-check-col">
                      <a-checkbox
                        :checked="item.status === 2"
                        :disabled="!canCompleteItem(item)"
                        @change="(e: any) => handleCompleteItem(item, e.target.checked)"
                      >
                      </a-checkbox>
                    </div>
                    <div class="item-main-col">
                      <div class="item-title-row">
                        <span class="item-name" :class="{ 'name-done': item.status === 2 }">
                          {{ item.itemName }}
                        </span>
                        <a-tag v-if="item.isTemporary" color="purple" class="temp-tag">临时</a-tag>
                        <a-tag v-if="item.status === 3" color="red" class="overdue-tag">已逾期</a-tag>
                      </div>
                      <div v-if="item.itemDescription" class="item-description">
                        {{ item.itemDescription }}
                      </div>
                      <div class="item-meta-row">
                        <a-tooltip title="责任人">
                          <span class="meta-item">
                            <UserOutlined /> {{ item.responsibleUserName || '-' }}
                          </span>
                        </a-tooltip>
                        <a-tooltip title="截止日期">
                          <span class="meta-item" :class="{ 'meta-overdue': item.status === 3 }">
                            <CalendarOutlined /> {{ item.dueDate }}
                          </span>
                        </a-tooltip>
                        <template v-if="item.status === 2">
                          <a-tooltip title="完成人">
                            <span class="meta-item">
                              <CheckCircleOutlined /> {{ item.completedUserName }}
                            </span>
                          </a-tooltip>
                          <a-tooltip title="完成时间">
                            <span class="meta-item">
                              <ClockCircleOutlined /> {{ formatDateTime(item.completedTime) }}
                            </span>
                          </a-tooltip>
                        </template>
                      </div>
                      <div v-if="item.remark && item.status === 2" class="item-remark">
                        <span class="remark-label">备注：</span>{{ item.remark }}
                      </div>
                    </div>
                    <div class="item-action-col">
                      <a-dropdown v-if="canManage" :trigger="['click']">
                        <template #overlay>
                          <a-menu>
                            <a-menu-item @click="showEditDueDateModal(item)" :disabled="item.status === 2">
                              <EditOutlined /> 调整截止日期
                            </a-menu-item>
                            <a-menu-item
                              v-if="item.isTemporary && item.status !== 2"
                              @click="handleDeleteItem(item)"
                              danger
                            >
                              <DeleteOutlined /> 删除
                            </a-menu-item>
                          </a-menu>
                        </template>
                        <a-button type="text" size="small">
                          <MoreOutlined />
                        </a-button>
                      </a-dropdown>
                    </div>
                  </div>
                </div>
              </div>
            </template>
          </div>
        </a-card>
      </div>

      <a-spin v-else :spinning="checklistStore.loading" tip="加载中...">
        <div style="height: 400px"></div>
      </a-spin>

      <a-modal
        v-model:open="completeModalVisible"
        title="确认完成待办"
        @ok="confirmComplete"
        ok-text="确认完成"
        cancel-text="取消"
        centered
      >
        <p>请确认是否已完成：<b>{{ completingItem?.itemName }}</b></p>
        <a-form layout="vertical">
          <a-form-item label="完成备注（可选）">
            <a-textarea v-model:value="completeRemark" :rows="3" placeholder="请输入完成备注..." />
          </a-form-item>
        </a-form>
      </a-modal>

      <a-modal
        v-model:open="temporaryModalVisible"
        title="追加临时待办"
        @ok="handleAddTemporary"
        ok-text="添加"
        cancel-text="取消"
        centered
      >
        <a-form ref="tempFormRef" :model="tempItemForm" layout="vertical">
          <a-form-item label="事项名称" name="itemName" :rules="[{ required: true, message: '请输入事项名称!' }]">
            <a-input v-model:value="tempItemForm.itemName" placeholder="请输入待办事项名称" />
          </a-form-item>
          <a-form-item label="事项描述">
            <a-textarea v-model:value="tempItemForm.itemDescription" :rows="2" placeholder="请输入事项描述（可选）" />
          </a-form-item>
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="所属阶段" name="stage" :rules="[{ required: true, message: '请选择阶段!' }]">
                <a-select v-model:value="tempItemForm.stage" style="width: 100%">
                  <a-select-option :value="1">入职前</a-select-option>
                  <a-select-option :value="2">首日</a-select-option>
                  <a-select-option :value="3">首周</a-select-option>
                  <a-select-option :value="4">首月</a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="截止日期" name="dueDate" :rules="[{ required: true, message: '请选择截止日期!' }]">
                <a-date-picker v-model:value="tempItemForm.dueDate" style="width: 100%" format="YYYY-MM-DD" value-format="YYYY-MM-DD" />
              </a-form-item>
            </a-col>
          </a-row>
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="责任人">
                <a-select
                  v-model:value="tempItemForm.responsibleUserId"
                  show-search
                  placeholder="选择责任人（可选）"
                  option-filter-prop="label"
                  allow-clear
                  style="width: 100%"
                  @change="handleTempResponsibleChange"
                >
                  <a-select-option
                    v-for="emp in employeeStore.employees"
                    :key="emp.id"
                    :value="emp.id"
                    :label="emp.name"
                  >
                    {{ emp.name }}
                    <span class="option-sub">{{ emp.departmentName }} · {{ emp.role }}</span>
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="责任人姓名">
                <a-input v-model:value="tempItemForm.responsibleUserName" placeholder="请输入责任人姓名" />
              </a-form-item>
            </a-col>
          </a-row>
        </a-form>
      </a-modal>

      <a-modal
        v-model:open="dueDateModalVisible"
        title="调整截止日期"
        @ok="confirmEditDueDate"
        ok-text="保存"
        cancel-text="取消"
        centered
      >
        <p>调整 <b>{{ editingDueDateItem?.itemName }}</b> 的截止日期：</p>
        <a-date-picker v-model:value="newDueDate" style="width: 100%" format="YYYY-MM-DD" value-format="YYYY-MM-DD" />
      </a-modal>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, reactive, computed, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import { useOnboardingChecklistStore, type OnboardingChecklistItem, type TemporaryItemDTO, type ItemDueDateDTO, type ItemCompleteDTO } from '../stores/onboardingChecklist';
import { useEmployeeStore } from '../stores/employee';
import { useAuthStore } from '../stores/auth';
import { message } from 'ant-design-vue';
import {
  PlusOutlined,
  ReloadOutlined,
  UserOutlined,
  CalendarOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  EditOutlined,
  DeleteOutlined,
  MoreOutlined,
} from '@ant-design/icons-vue';
import dayjs from 'dayjs';

const route = useRoute();
const checklistStore = useOnboardingChecklistStore();
const employeeStore = useEmployeeStore();
const authStore = useAuthStore();

const currentStage = ref(0);
const completeModalVisible = ref(false);
const temporaryModalVisible = ref(false);
const dueDateModalVisible = ref(false);
const completingItem = ref<OnboardingChecklistItem | null>(null);
const editingDueDateItem = ref<OnboardingChecklistItem | null>(null);
const completeRemark = ref('');
const newDueDate = ref('');
const tempFormRef = ref();

const canManage = computed(() => authStore.hasRole(['ADMIN', 'HR']));
const canAddTemporary = computed(() => authStore.hasRole(['ADMIN', 'HR']) && checklistStore.currentChecklist?.status !== 2);

const tempItemForm = reactive<TemporaryItemDTO>({
  checklistId: 0,
  itemName: '',
  itemDescription: '',
  stage: 3,
  dueDate: '',
  responsibleUserId: undefined,
  responsibleUserName: undefined,
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

const getStatusText = (status?: number) => {
  switch (status) {
    case 0: return '进行中';
    case 1: return '已完成';
    case 2: return '已归档';
    default: return '-';
  }
};

const getStatusColor = (status?: number) => {
  switch (status) {
    case 0: return 'blue';
    case 1: return 'green';
    case 2: return 'default';
    default: return 'default';
  }
};

const progressColor = (progress?: number) => {
  if (!progress) return '#d9d9d9';
  if (progress < 50) return '#ff4d4f';
  if (progress < 80) return '#faad14';
  return '#52c41a';
};

const formatDateTime = (dt?: string) => {
  if (!dt) return '-';
  return dayjs(dt).format('YYYY-MM-DD HH:mm');
};

const getStageItems = (stage: number): OnboardingChecklistItem[] => {
  if (!checklistStore.currentChecklist?.items) return [];
  return checklistStore.currentChecklist.items
    .filter((i) => i.stage === stage)
    .sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0));
};

const getStageItemCount = (stage: number) => {
  return getStageItems(stage).length;
};

const getStageCompletedCount = (stage: number) => {
  return getStageItems(stage).filter((i) => i.status === 2).length;
};

const getStageProgress = (stage: number): number => {
  if (!checklistStore.currentChecklist) return 0;
  switch (stage) {
    case 1: return checklistStore.currentChecklist.preJoinProgress || 0;
    case 2: return checklistStore.currentChecklist.firstDayProgress || 0;
    case 3: return checklistStore.currentChecklist.firstWeekProgress || 0;
    case 4: return checklistStore.currentChecklist.firstMonthProgress || 0;
    default: return 0;
  }
};

const canCompleteItem = (item: OnboardingChecklistItem) => {
  if (item.status === 2) return false;
  if (checklistStore.currentChecklist?.status === 2) return false;
  if (authStore.hasRole(['ADMIN', 'HR'])) return true;
  const currentUserId = authStore.userInfo?.userId;
  const currentEmployeeId = authStore.userInfo?.employeeId;
  if (item.responsibleUserId && currentUserId && Number(item.responsibleUserId) === Number(currentUserId)) return true;
  if (item.responsibleUserId && currentEmployeeId && Number(item.responsibleUserId) === Number(currentEmployeeId)) return true;
  if (item.responsibleRole === 'NEW_EMPLOYEE' && authStore.hasRole('EMPLOYEE')) {
    if (item.employeeId && currentEmployeeId && Number(item.employeeId) === Number(currentEmployeeId)) return true;
  }
  if (item.responsibleRole === 'MENTOR' && checklistStore.currentChecklist?.mentorId) {
    if (currentEmployeeId && Number(checklistStore.currentChecklist.mentorId) === Number(currentEmployeeId)) return true;
    if (currentUserId && Number(checklistStore.currentChecklist.mentorId) === Number(currentUserId)) return true;
  }
  return false;
};

onMounted(async () => {
  await employeeStore.fetchEmployees();
  const id = Number(route.params.id);
  if (id) {
    await checklistStore.fetchChecklist(id);
  }
});

const refreshDetail = async () => {
  if (checklistStore.currentChecklist?.id) {
    await checklistStore.fetchChecklist(checklistStore.currentChecklist.id);
  }
};

const handleCompleteItem = (item: OnboardingChecklistItem, checked: boolean) => {
  if (checked) {
    completingItem.value = item;
    completeRemark.value = '';
    completeModalVisible.value = true;
  }
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
  }
};

const showTemporaryModal = () => {
  if (!checklistStore.currentChecklist?.id) return;
  tempItemForm.checklistId = checklistStore.currentChecklist.id;
  tempItemForm.itemName = '';
  tempItemForm.itemDescription = '';
  tempItemForm.stage = currentStage.value === 0 ? 3 : currentStage.value;
  tempItemForm.dueDate = '';
  tempItemForm.responsibleUserId = undefined;
  tempItemForm.responsibleUserName = undefined;
  temporaryModalVisible.value = true;
};

const handleTempResponsibleChange = (userId: number | undefined) => {
  if (userId) {
    const emp = employeeStore.employees.find((e) => e.id === userId);
    if (emp) {
      tempItemForm.responsibleUserName = emp.name;
    }
  }
};

const handleAddTemporary = async () => {
  try {
    await tempFormRef.value?.validate();
    const success = await checklistStore.addTemporaryItem(tempItemForm);
    if (success) {
      temporaryModalVisible.value = false;
    }
  } catch (error) {
    // validation failed
  }
};

const showEditDueDateModal = (item: OnboardingChecklistItem) => {
  editingDueDateItem.value = item;
  newDueDate.value = item.dueDate;
  dueDateModalVisible.value = true;
};

const confirmEditDueDate = async () => {
  if (!editingDueDateItem.value?.id || !newDueDate.value) return;
  const dto: ItemDueDateDTO = {
    itemId: editingDueDateItem.value.id,
    dueDate: newDueDate.value,
  };
  const success = await checklistStore.updateDueDate(dto);
  if (success) {
    dueDateModalVisible.value = false;
  }
};

const handleDeleteItem = async (item: OnboardingChecklistItem) => {
  if (!item.id) return;
  const success = await checklistStore.deleteItem(item.id);
  if (success) {
    message.success('删除成功');
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
  max-width: 1400px;
}

.detail-layout {
  display: grid;
  grid-template-columns: 300px 1fr;
  gap: 20px;
}

.sidebar-card {
  border-radius: 14px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
  position: sticky;
  top: 24px;
  max-height: calc(100vh - 48px);
  overflow-y: auto;
}

.content-card {
  border-radius: 14px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
}

.employee-header {
  display: flex;
  gap: 16px;
  align-items: center;
}

.employee-info {
  flex: 1;
}

.employee-name {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: #2c3e50;
}

.employee-meta {
  margin: 4px 0 8px 0;
  font-size: 13px;
  color: #8c8c8c;
}

.info-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.info-label {
  font-size: 13px;
  color: #8c8c8c;
}

.info-value {
  font-size: 13px;
  color: #2c3e50;
  font-weight: 500;
}

.overall-progress {
  margin-bottom: 20px;
}

.progress-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}

.progress-label {
  font-size: 13px;
  color: #8c8c8c;
}

.progress-value {
  font-size: 16px;
  font-weight: 700;
  color: #2c3e50;
}

.stages-progress {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.stage-item {
  padding: 12px;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
  background: #fafafa;
}

.stage-item:hover,
.stage-item.stage-active {
  background: #f0f5ff;
}

.stage-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.stage-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
}

.stage-info {
  display: flex;
  flex-direction: column;
  flex: 1;
}

.stage-name {
  font-size: 14px;
  font-weight: 600;
  color: #2c3e50;
}

.stage-count {
  font-size: 12px;
  color: #8c8c8c;
}

.sidebar-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.content-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.stage-tabs {
  overflow-x: auto;
}

.tab-emoji {
  margin-right: 4px;
}

.tab-count {
  margin-left: 4px;
}

.items-container {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.stage-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.stage-section-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-left: 12px;
  border-left: 4px solid;
  font-size: 16px;
  font-weight: 600;
  color: #2c3e50;
}

.stage-section-emoji {
  font-size: 18px;
}

.stage-section-title {
  flex: 1;
}

.items-grid {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.check-item-card {
  display: grid;
  grid-template-columns: 40px 1fr auto;
  gap: 12px;
  padding: 16px;
  background: #fafafa;
  border-radius: 12px;
  border: 1px solid transparent;
  transition: all 0.2s;
  align-items: start;
}

.check-item-card:hover {
  background: #f5f7fa;
  border-color: #e8e8e8;
}

.check-item-card.item-completed {
  background: #f6ffed;
  border-color: #b7eb8f;
  opacity: 0.85;
}

.check-item-card.item-overdue {
  background: #fff1f0;
  border-color: #ffa39e;
}

.check-item-card.item-temporary {
  border-left: 4px solid #9254de;
}

.item-check-col {
  padding-top: 2px;
}

.item-main-col {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.item-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.item-name {
  font-size: 15px;
  font-weight: 600;
  color: #2c3e50;
}

.item-name.name-done {
  text-decoration: line-through;
  color: #8c8c8c;
}

.temp-tag {
  font-size: 11px;
}

.overdue-tag {
  font-size: 11px;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.6; }
}

.item-description {
  font-size: 13px;
  color: #595959;
}

.item-meta-row {
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

.meta-overdue {
  color: #ff4d4f;
  font-weight: 600;
}

.item-remark {
  font-size: 12px;
  color: #595959;
  padding: 8px 12px;
  background: white;
  border-radius: 8px;
  margin-top: 4px;
}

.remark-label {
  color: #8c8c8c;
}

.item-action-col {
  padding-top: 2px;
}

.option-sub {
  color: #8c8c8c;
  font-size: 12px;
  margin-left: 8px;
}

@media (max-width: 1024px) {
  .detail-layout {
    grid-template-columns: 1fr;
  }

  .sidebar-card {
    position: static;
    max-height: none;
  }
}
</style>
