<template>
  <div class="page-container">
    <div class="content-wrapper">
      <a-card class="main-card" :bordered="false">
        <div class="header-section">
          <div class="title-group">
            <h1 class="page-title">入职清单模板</h1>
            <p class="subtitle">Onboarding Template Management</p>
          </div>
          <div class="header-actions">
            <a-button type="primary" class="add-btn" @click="showTemplateModal()">
              <template #icon><PlusOutlined /></template>
              新建模板
            </a-button>
          </div>
        </div>

        <a-table
          :columns="columns"
          :data-source="templateStore.templates"
          :loading="templateStore.loading"
          row-key="id"
          :pagination="{ pageSize: 10 }"
          class="modern-table"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'templateName'">
              <div class="template-name-cell">
                <FileTextOutlined class="template-icon" />
                <span class="name-text">{{ record.templateName }}</span>
                <a-tag v-if="record.isDefault" color="gold" class="default-tag">默认</a-tag>
              </div>
            </template>

            <template v-if="column.key === 'scope'">
              <div>
                <a-tag v-if="record.departmentName" color="blue">{{ record.departmentName }}</a-tag>
                <a-tag v-if="record.position" color="purple">{{ record.position }}</a-tag>
                <a-tag v-if="!record.departmentName && !record.position" color="default">通用</a-tag>
              </div>
            </template>

            <template v-if="column.key === 'itemCount'">
              <span>{{ record.items?.length || 0 }} 项待办</span>
            </template>

            <template v-if="column.key === 'enabled'">
              <a-switch
                :checked="record.enabled"
                :disabled="!canEdit"
                @change="(checked: boolean) => toggleEnabled(record, checked)"
              />
            </template>

            <template v-if="column.key === 'action'">
              <a-space size="small">
                <a-button type="link" class="action-btn view" @click="viewTemplate(record)">查看</a-button>
                <a-divider type="vertical" />
                <a-button type="link" class="action-btn edit" @click="showTemplateModal(record)">编辑</a-button>
                <a-divider type="vertical" />
                <a-popconfirm
                  title="确定要删除该模板吗?"
                  @confirm="handleDelete(record.id)"
                  ok-text="确定"
                  cancel-text="取消"
                >
                  <a-button type="link" danger class="action-btn delete">删除</a-button>
                </a-popconfirm>
              </a-space>
            </template>
          </template>
        </a-table>
      </a-card>

      <a-modal
        v-model:open="templateModalVisible"
        :title="isEditTemplate ? '编辑模板' : '新建模板'"
        @ok="handleTemplateOk"
        ok-text="保存"
        cancel-text="取消"
        :width="900"
        centered
        class="modern-modal"
      >
        <a-form ref="templateFormRef" :model="templateFormState" layout="vertical" class="modern-form">
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="模板名称" name="templateName" :rules="[{ required: true, message: '请输入模板名称!' }]">
                <a-input v-model:value="templateFormState.templateName" placeholder="请输入模板名称" />
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item label="适用部门">
                <a-tree-select
                  v-model:value="templateFormState.departmentId"
                  :tree-data="deptSelectOptions"
                  :field-names="{ label: 'name', value: 'id', children: 'children' }"
                  placeholder="通用（不选）"
                  allow-clear
                  tree-default-expand-all
                  style="width: 100%"
                  @change="handleDeptChange"
                />
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item label="适用岗位">
                <a-input v-model:value="templateFormState.position" placeholder="通用（不填）" />
              </a-form-item>
            </a-col>
          </a-row>
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="设为默认">
                <a-switch v-model:checked="templateFormState.isDefault" />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="启用状态">
                <a-switch v-model:checked="templateFormState.enabled" />
              </a-form-item>
            </a-col>
          </a-row>
          <a-form-item label="模板描述">
            <a-textarea v-model:value="templateFormState.description" :rows="2" placeholder="请输入模板描述" />
          </a-form-item>

          <a-divider orientation="left">待办事项配置</a-divider>

          <div class="items-toolbar">
            <a-radio-group v-model:value="currentStageFilter" option-type="button">
              <a-radio-button :value="0">全部</a-radio-button>
              <a-radio-button :value="1">入职前</a-radio-button>
              <a-radio-button :value="2">首日</a-radio-button>
              <a-radio-button :value="3">首周</a-radio-button>
              <a-radio-button :value="4">首月</a-radio-button>
            </a-radio-group>
            <a-button type="primary" size="small" @click="addTemplateItem">
              <template #icon><PlusOutlined /></template>
              添加待办项
            </a-button>
          </div>

          <div class="items-list">
            <div
              v-for="(item, index) in filteredTemplateItems"
              :key="item._key || index"
              class="item-card"
              :class="getStageClass(item.stage)"
            >
              <div class="item-header">
                <a-tag :color="getStageColor(item.stage)" class="stage-tag">{{ getStageText(item.stage) }}</a-tag>
                <div class="item-actions">
                  <a-button type="text" size="small" @click="moveItemUp(index)">
                    <template #icon><UpOutlined /></template>
                  </a-button>
                  <a-button type="text" size="small" @click="moveItemDown(index)">
                    <template #icon><DownOutlined /></template>
                  </a-button>
                  <a-button type="text" size="small" danger @click="removeTemplateItem(index)">
                    <template #icon><DeleteOutlined /></template>
                  </a-button>
                </div>
              </div>
              <div class="item-body">
                <a-row :gutter="12">
                  <a-col :span="10">
                    <a-input v-model:value="item.itemName" placeholder="待办事项名称" size="small" />
                  </a-col>
                  <a-col :span="4">
                    <a-input-number v-model:value="item.dueDays" :min="-30" :max="60" size="small" style="width: 100%" addon-before="入职" addon-after="天" />
                  </a-col>
                  <a-col :span="5">
                    <a-select v-model:value="item.responsibleRole" size="small" style="width: 100%">
                      <a-select-option value="NEW_EMPLOYEE">新员工</a-select-option>
                      <a-select-option value="MENTOR">导师</a-select-option>
                      <a-select-option value="HR">HR</a-select-option>
                      <a-select-option value="DEPARTMENT_HEAD">部门负责人</a-select-option>
                      <a-select-option value="SPECIFIC">指定人员</a-select-option>
                    </a-select>
                  </a-col>
                  <a-col :span="5">
                    <a-input v-model:value="item.responsibleUserName" placeholder="指定人员姓名" size="small" :disabled="item.responsibleRole !== 'SPECIFIC'" />
                  </a-col>
                </a-row>
                <a-input v-model:value="item.itemDescription" placeholder="事项描述（可选）" size="small" style="margin-top: 8px" />
              </div>
            </div>
            <a-empty v-if="filteredTemplateItems.length === 0" description="暂无待办项" />
          </div>
        </a-form>
      </a-modal>

      <a-modal
        v-model:open="viewModalVisible"
        title="模板详情"
        :footer="null"
        :width="800"
        centered
      >
        <div v-if="viewingTemplate" class="template-detail">
          <div class="detail-header">
            <h3>{{ viewingTemplate.templateName }}</h3>
            <a-tag v-if="viewingTemplate.isDefault" color="gold">默认模板</a-tag>
          </div>
          <div class="detail-meta">
            <span><b>适用范围：</b>
              <a-tag v-if="viewingTemplate.departmentName" color="blue">{{ viewingTemplate.departmentName }}</a-tag>
              <a-tag v-if="viewingTemplate.position" color="purple">{{ viewingTemplate.position }}</a-tag>
              <a-tag v-if="!viewingTemplate.departmentName && !viewingTemplate.position" color="default">通用</a-tag>
            </span>
            <span v-if="viewingTemplate.description"><b>描述：</b>{{ viewingTemplate.description }}</span>
          </div>
          <a-timeline class="template-timeline">
            <template v-for="stage in [1, 2, 3, 4]" :key="stage">
              <a-timeline-item :color="getStageColor(stage)">
                <template #dot>
                  <div class="timeline-dot">{{ getStageText(stage) }}</div>
                </template>
                <div class="timeline-stage">
                  <div
                    v-for="item in (viewingTemplate.items || []).filter(i => i.stage === stage)"
                    :key="item.id"
                    class="timeline-item"
                  >
                    <div class="timeline-item-name">{{ item.itemName }}</div>
                    <div class="timeline-item-meta">
                      <span>入职 {{ item.dueDays >= 0 ? '+' : '' }}{{ item.dueDays }} 天</span>
                      <a-tag color="blue">{{ getRoleText(item.responsibleRole) }}</a-tag>
                    </div>
                    <div v-if="item.itemDescription" class="timeline-item-desc">{{ item.itemDescription }}</div>
                  </div>
                </div>
              </a-timeline-item>
            </template>
          </a-timeline>
        </div>
      </a-modal>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, reactive, computed, onMounted } from 'vue';
import { useOnboardingTemplateStore, type OnboardingTemplate, type OnboardingTemplateItem } from '../stores/onboardingTemplate';
import { useDepartmentStore } from '../stores/department';
import { useAuthStore } from '../stores/auth';
import { message } from 'ant-design-vue';
import { PlusOutlined, UpOutlined, DownOutlined, DeleteOutlined, FileTextOutlined } from '@ant-design/icons-vue';

const templateStore = useOnboardingTemplateStore();
const deptStore = useDepartmentStore();
const authStore = useAuthStore();

const canEdit = computed(() => authStore.hasRole(['ADMIN', 'HR']));

const columns = [
  { title: '模板名称', dataIndex: 'templateName', key: 'templateName', width: '25%' },
  { title: '适用范围', key: 'scope', width: '25%' },
  { title: '待办项数', key: 'itemCount', width: '12%' },
  { title: '状态', dataIndex: 'enabled', key: 'enabled', width: '10%' },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: '15%' },
  { title: '操作', key: 'action', width: '18%', align: 'center' },
];

const templateModalVisible = ref(false);
const viewModalVisible = ref(false);
const isEditTemplate = ref(false);
const templateFormRef = ref();
const currentStageFilter = ref(0);
const viewingTemplate = ref<OnboardingTemplate | null>(null);

interface TemplateItemForm extends OnboardingTemplateItem {
  _key?: string;
}

const templateFormState = reactive<OnboardingTemplate>({
  templateName: '',
  departmentId: undefined,
  departmentName: undefined,
  position: undefined,
  description: undefined,
  enabled: true,
  isDefault: false,
  items: [],
});

const deptSelectOptions = computed(() => {
  const filterEnabled = (items: any[]): any[] => {
    return items
      .filter((d) => d.enabled)
      .map((d) => ({
        ...d,
        children: d.children ? filterEnabled(d.children) : undefined,
      }));
  };
  return filterEnabled(deptStore.departmentsTree);
});

const filteredTemplateItems = computed(() => {
  if (!templateFormState.items) return [];
  if (currentStageFilter.value === 0) return templateFormState.items;
  return templateFormState.items.filter((i) => i.stage === currentStageFilter.value);
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

const getStageColor = (stage?: number) => {
  switch (stage) {
    case 1: return 'orange';
    case 2: return 'blue';
    case 3: return 'cyan';
    case 4: return 'green';
    default: return 'default';
  }
};

const getStageClass = (stage?: number) => {
  switch (stage) {
    case 1: return 'stage-prejoin';
    case 2: return 'stage-firstday';
    case 3: return 'stage-firstweek';
    case 4: return 'stage-firstmonth';
    default: return '';
  }
};

const getRoleText = (role?: string) => {
  switch (role) {
    case 'NEW_EMPLOYEE': return '新员工';
    case 'MENTOR': return '导师';
    case 'HR': return 'HR';
    case 'DEPARTMENT_HEAD': return '部门负责人';
    case 'SPECIFIC': return '指定人员';
    default: return '-';
  }
};

onMounted(async () => {
  await deptStore.refreshAll();
  await templateStore.fetchTemplates();
});

const handleDeptChange = (deptId: number | null) => {
  if (deptId) {
    const flat = deptStore.flattenDepartments(deptStore.departmentsTree);
    const dept = flat.find((d: any) => d.id === deptId);
    templateFormState.departmentName = dept?.name;
  } else {
    templateFormState.departmentName = undefined;
  }
};

const showTemplateModal = (record?: OnboardingTemplate) => {
  if (record) {
    isEditTemplate.value = true;
    Object.assign(templateFormState, JSON.parse(JSON.stringify(record)));
    if (!templateFormState.items) templateFormState.items = [];
  } else {
    isEditTemplate.value = false;
    templateFormState.id = undefined;
    templateFormState.templateName = '';
    templateFormState.departmentId = undefined;
    templateFormState.departmentName = undefined;
    templateFormState.position = undefined;
    templateFormState.description = undefined;
    templateFormState.enabled = true;
    templateFormState.isDefault = false;
    templateFormState.items = [];
  }
  currentStageFilter.value = 0;
  templateModalVisible.value = true;
};

const viewTemplate = async (record: OnboardingTemplate) => {
  const detail = await templateStore.fetchTemplate(record.id!);
  viewingTemplate.value = detail;
  viewModalVisible.value = true;
};

const addTemplateItem = () => {
  const stage = currentStageFilter.value === 0 ? 1 : currentStageFilter.value;
  if (!templateFormState.items) templateFormState.items = [];
  const newItem: TemplateItemForm = {
    _key: Date.now().toString(),
    itemName: '',
    itemDescription: '',
    stage,
    sortOrder: templateFormState.items.length,
    dueDays: 0,
    responsibleRole: 'HR',
  };
  templateFormState.items.push(newItem);
};

const removeTemplateItem = (index: number) => {
  const actualIndex = templateFormState.items!.findIndex(
    (item) => item === filteredTemplateItems.value[index]
  );
  if (actualIndex !== -1) {
    templateFormState.items!.splice(actualIndex, 1);
  }
};

const moveItemUp = (index: number) => {
  if (index <= 0) return;
  const actualIndex = templateFormState.items!.findIndex(
    (item) => item === filteredTemplateItems.value[index]
  );
  const prevActualIndex = templateFormState.items!.findIndex(
    (item) => item === filteredTemplateItems.value[index - 1]
  );
  if (actualIndex !== -1 && prevActualIndex !== -1) {
    const temp = templateFormState.items![actualIndex];
    templateFormState.items![actualIndex] = templateFormState.items![prevActualIndex];
    templateFormState.items![prevActualIndex] = temp;
  }
};

const moveItemDown = (index: number) => {
  if (index >= filteredTemplateItems.value.length - 1) return;
  const actualIndex = templateFormState.items!.findIndex(
    (item) => item === filteredTemplateItems.value[index]
  );
  const nextActualIndex = templateFormState.items!.findIndex(
    (item) => item === filteredTemplateItems.value[index + 1]
  );
  if (actualIndex !== -1 && nextActualIndex !== -1) {
    const temp = templateFormState.items![actualIndex];
    templateFormState.items![actualIndex] = templateFormState.items![nextActualIndex];
    templateFormState.items![nextActualIndex] = temp;
  }
};

const handleTemplateOk = async () => {
  try {
    await templateFormRef.value?.validate();
    if (!templateFormState.items || templateFormState.items.length === 0) {
      message.warning('请至少添加一个待办项');
      return;
    }
    const invalidItems = templateFormState.items.filter((i) => !i.itemName);
    if (invalidItems.length > 0) {
      message.warning('请填写所有待办项的名称');
      return;
    }
    let success;
    if (isEditTemplate.value) {
      success = await templateStore.updateTemplate(templateFormState);
    } else {
      success = await templateStore.createTemplate(templateFormState);
    }
    if (success) {
      templateModalVisible.value = false;
    }
  } catch (error) {
    // validation failed
  }
};

const handleDelete = async (id: number) => {
  await templateStore.deleteTemplate(id);
};

const toggleEnabled = async (record: OnboardingTemplate, checked: boolean) => {
  record.enabled = checked;
  await templateStore.updateTemplate(record);
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

.main-card {
  border-radius: 14px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
}

.header-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding: 0 8px;
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

.add-btn {
  height: 36px;
  padding: 0 20px;
  border-radius: 18px;
  font-weight: 600;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  box-shadow: 0 4px 12px rgba(118, 75, 162, 0.3);
}

.template-name-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.template-icon {
  color: #667eea;
  font-size: 18px;
}

.name-text {
  font-weight: 500;
}

.default-tag {
  margin-left: 4px;
}

.action-btn {
  padding: 0;
  font-weight: 500;
}

.action-btn.view {
  color: #13c2c2;
}

.action-btn.edit {
  color: #3498db;
}

.action-btn.delete {
  color: #ff4d4f;
}

.items-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.items-list {
  max-height: 400px;
  overflow-y: auto;
  padding-right: 8px;
}

.item-card {
  border-radius: 10px;
  padding: 12px 16px;
  margin-bottom: 12px;
  border-left: 4px solid;
  background: #fafafa;
  transition: all 0.2s;
}

.item-card:hover {
  background: #f5f5f5;
}

.stage-prejoin {
  border-left-color: #fa8c16;
}

.stage-firstday {
  border-left-color: #1890ff;
}

.stage-firstweek {
  border-left-color: #13c2c2;
}

.stage-firstmonth {
  border-left-color: #52c41a;
}

.item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.stage-tag {
  font-weight: 500;
}

.item-actions {
  display: flex;
  gap: 4px;
}

.item-body {
  padding: 4px 0;
}

.template-detail .detail-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.template-detail .detail-header h3 {
  margin: 0;
  font-size: 20px;
}

.detail-meta {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 24px;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 10px;
}

.template-timeline {
  padding-left: 8px;
}

.timeline-dot {
  padding: 4px 10px;
  background: white;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
}

.timeline-stage {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.timeline-item {
  padding: 12px 16px;
  background: #fafafa;
  border-radius: 8px;
}

.timeline-item-name {
  font-weight: 600;
  color: #2c3e50;
  margin-bottom: 4px;
}

.timeline-item-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 13px;
  color: #8c8c8c;
}

.timeline-item-desc {
  margin-top: 6px;
  font-size: 13px;
  color: #595959;
}
</style>
