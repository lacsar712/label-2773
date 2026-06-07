<template>
  <div class="page-container">
    <div class="content-wrapper">
      <a-card class="main-card" :bordered="false">
        <div class="header-section">
          <div class="back-section">
            <a-button type="link" @click="goBack">
              <ArrowLeftOutlined /> 返回列表
            </a-button>
          </div>
          <div class="title-group">
            <h1 class="page-title">{{ isEdit ? '编辑公告' : '新建公告' }}</h1>
            <p class="subtitle">{{ isEdit ? 'Edit Announcement' : 'Create Announcement' }}</p>
          </div>
        </div>

        <a-form ref="formRef" :model="formState" layout="vertical" class="modern-form" @finish="handleFinish">
          <a-form-item
            label="公告标题"
            name="title"
            :rules="[{ required: true, message: '请输入公告标题!' }]"
          >
            <a-input
              v-model:value="formState.title"
              placeholder="请输入公告标题"
              size="large"
              :max-length="200"
              show-count
            />
          </a-form-item>

          <a-row :gutter="24">
            <a-col :xs="24" :md="12">
              <a-form-item label="可见范围" name="visibilityType" :rules="[{ required: true, message: '请选择可见范围!' }]">
                <a-radio-group v-model:value="formState.visibilityType" size="large">
                  <a-radio :value="1">全员可见</a-radio>
                  <a-radio :value="2">指定部门</a-radio>
                  <a-radio :value="3">指定角色</a-radio>
                </a-radio-group>
              </a-form-item>
            </a-col>
            <a-col :xs="24" :md="12">
              <div class="options-row">
                <a-form-item name="isPinned">
                  <a-checkbox v-model:checked="formState.isPinned">
                    <PushpinOutlined /> 置顶公告
                  </a-checkbox>
                </a-form-item>
                <a-form-item name="isImportant">
                  <a-checkbox v-model:checked="formState.isImportant">
                    <ExclamationCircleOutlined /> 重要公告（发布时推送消息通知）
                  </a-checkbox>
                </a-form-item>
              </div>
            </a-col>
          </a-row>

          <a-row :gutter="24" v-if="formState.visibilityType === 2">
            <a-col :span="24">
              <a-form-item label="选择部门" name="visibilityTargets" :rules="[{ required: true, message: '请选择至少一个部门!' }]">
                <a-tree-select
                  v-model:value="selectedDeptIds"
                  :tree-data="deptTreeData"
                  tree-checkable
                  :show-checked-strategy="SHOW_PARENT"
                  placeholder="请选择可见部门"
                  style="width: 100%"
                  :max-tag-count="5"
                  search-placeholder="搜索部门"
                  tree-default-expand-all
                  :field-names="{ label: 'name', value: 'id', children: 'children' }"
                  @change="handleDeptChange"
                />
              </a-form-item>
            </a-col>
          </a-row>

          <a-row :gutter="24" v-if="formState.visibilityType === 3">
            <a-col :span="24">
              <a-form-item label="选择角色" name="visibilityTargets" :rules="[{ required: true, message: '请选择至少一个角色!' }]">
                <a-checkbox-group v-model:value="selectedRoleIds" @change="handleRoleChange">
                  <a-checkbox v-for="role in roleList" :key="role.id" :value="role.id">
                    {{ role.roleName }}
                  </a-checkbox>
                </a-checkbox-group>
              </a-form-item>
            </a-col>
          </a-row>

          <a-row :gutter="24">
            <a-col :xs="24" :md="12">
              <a-form-item label="生效时间">
                <a-date-picker
                  v-model:value="formState.effectiveTime"
                  show-time
                  style="width: 100%"
                  format="YYYY-MM-DD HH:mm:ss"
                  value-format="YYYY-MM-DD HH:mm:ss"
                  placeholder="不设置则立即生效"
                />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :md="12">
              <a-form-item label="过期时间">
                <a-date-picker
                  v-model:value="formState.expireTime"
                  show-time
                  style="width: 100%"
                  format="YYYY-MM-DD HH:mm:ss"
                  value-format="YYYY-MM-DD HH:mm:ss"
                  placeholder="不设置则永不过期"
                />
              </a-form-item>
            </a-col>
          </a-row>

          <a-form-item label="封面图URL">
            <a-input
              v-model:value="formState.coverImage"
              placeholder="请输入封面图URL（可选）"
            />
            <div v-if="formState.coverImage" class="cover-preview">
              <img :src="formState.coverImage" alt="封面预览" />
            </div>
          </a-form-item>

          <a-form-item label="公告正文">
            <div class="editor-wrapper">
              <div class="editor-toolbar">
                <a-button size="small" @click="insertFormat('**', '**')">加粗</a-button>
                <a-button size="small" @click="insertFormat('*', '*')">斜体</a-button>
                <a-button size="small" @click="insertFormat('## ', '')">标题</a-button>
                <a-button size="small" @click="insertFormat('\n- ', '')">列表</a-button>
                <a-button size="small" @click="insertFormat('[', '](url)')">链接</a-button>
              </div>
              <a-textarea
                v-model:value="formState.content"
                :rows="15"
                placeholder="请输入公告正文，支持Markdown格式"
                show-count
                :max-length="20000"
              />
            </div>
          </a-form-item>

          <a-form-item>
            <a-space size="large" style="width: 100%">
              <a-button
                type="primary"
                class="save-draft-btn"
                size="large"
                @click="handleSaveDraft"
              >
                <template #icon><SaveOutlined /></template>
                保存草稿
              </a-button>
              <a-button
                type="primary"
                class="preview-btn"
                size="large"
                @click="openPreview"
              >
                <template #icon><EyeOutlined /></template>
                预览
              </a-button>
              <a-button
                type="primary"
                class="publish-btn"
                size="large"
                @click="handlePublishClick"
                danger
              >
                <template #icon><SendOutlined /></template>
                发布公告
              </a-button>
            </a-space>
          </a-form-item>
        </a-form>
      </a-card>

      <a-modal
        v-model:open="previewModalOpen"
        title="公告预览"
        :width="800"
        :footer="null"
      >
        <div v-if="formState.title" class="preview-content">
          <div class="preview-tags">
            <a-tag v-if="formState.isPinned" color="red">
              <PushpinOutlined /> 置顶
            </a-tag>
            <a-tag v-if="formState.isImportant" color="orange">
              <ExclamationCircleOutlined /> 重要公告
            </a-tag>
            <a-tag>{{ getVisibilityText(formState.visibilityType) }}</a-tag>
          </div>
          <h2 class="preview-title">{{ formState.title }}</h2>
          <div class="preview-meta">
            <span v-if="formState.effectiveTime">生效时间：{{ formState.effectiveTime }}</span>
            <span v-if="formState.expireTime">过期时间：{{ formState.expireTime }}</span>
          </div>
          <a-divider />
          <div v-if="formState.coverImage" class="preview-cover">
            <img :src="formState.coverImage" alt="封面图" />
          </div>
          <div class="preview-body" v-html="renderMarkdown(formState.content || '')"></div>
        </div>
      </a-modal>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, reactive, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { Modal, message } from 'ant-design-vue';
import {
  ArrowLeftOutlined,
  PushpinOutlined,
  ExclamationCircleOutlined,
  SaveOutlined,
  EyeOutlined,
  SendOutlined,
} from '@ant-design/icons-vue';
import { useAnnouncementStore, type Announcement, type VisibilityTarget } from '../stores/announcement';
import { useDepartmentStore } from '../stores/department';

const SHOW_PARENT = 'SHOW_PARENT';

const route = useRoute();
const router = useRouter();
const announcementStore = useAnnouncementStore();
const departmentStore = useDepartmentStore();

const formRef = ref();
const isEdit = computed(() => !!route.params.id);
const editId = computed(() => Number(route.params.id));
const previewModalOpen = ref(false);

const formState = reactive<Announcement>({
  title: '',
  content: '',
  coverImage: '',
  visibilityType: 1,
  effectiveTime: '',
  expireTime: '',
  isPinned: false,
  isImportant: false,
  visibilityTargets: [],
});

const selectedDeptIds = ref<number[]>([]);
const selectedRoleIds = ref<number[]>([]);

const roleList = ref([
  { id: 1, roleCode: 'ADMIN', roleName: '系统管理员' },
  { id: 2, roleCode: 'HR', roleName: '人力资源' },
  { id: 3, roleCode: 'EMPLOYEE', roleName: '普通员工' },
]);

const deptTreeData = computed(() => departmentStore.departmentsTree);

const goBack = () => {
  router.push('/admin/announcements');
};

const getVisibilityText = (type: number) => {
  const map: Record<number, string> = {
    1: '全员可见',
    2: '指定部门',
    3: '指定角色',
  };
  return map[type] || '-';
};

const insertFormat = (before: string, after: string) => {
  const textarea = document.querySelector('.editor-wrapper textarea') as HTMLTextAreaElement;
  if (!textarea) return;
  const start = textarea.selectionStart;
  const end = textarea.selectionEnd;
  const text = formState.content || '';
  const selected = text.substring(start, end);
  const newText = text.substring(0, start) + before + selected + after + text.substring(end);
  formState.content = newText;
  setTimeout(() => {
    textarea.focus();
    textarea.setSelectionRange(start + before.length, start + before.length + selected.length);
  }, 0);
};

const renderMarkdown = (text: string) => {
  let html = text
    .replace(/^### (.*$)/gm, '<h3>$1</h3>')
    .replace(/^## (.*$)/gm, '<h2>$1</h2>')
    .replace(/^# (.*$)/gm, '<h1>$1</h1>')
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.*?)\*/g, '<em>$1</em>')
    .replace(/`(.*?)`/g, '<code>$1</code>')
    .replace(/^\- (.*$)/gm, '<li>$1</li>')
    .replace(/\n\n/g, '</p><p>')
    .replace(/\n/g, '<br/>');
  return '<p>' + html + '</p>';
};

const handleDeptChange = () => {
  updateVisibilityTargets();
};

const handleRoleChange = () => {
  updateVisibilityTargets();
};

const updateVisibilityTargets = () => {
  const targets: VisibilityTarget[] = [];
  if (formState.visibilityType === 2) {
    const flatDepts = departmentStore.flattenDepartments(departmentStore.departmentsTree);
    selectedDeptIds.value.forEach((id) => {
      const dept = flatDepts.find((d: any) => d.id === id);
      if (dept) {
        targets.push({ targetType: 1, targetId: id, targetName: dept.name });
      }
    });
  } else if (formState.visibilityType === 3) {
    selectedRoleIds.value.forEach((id) => {
      const role = roleList.value.find((r) => r.id === id);
      if (role) {
        targets.push({ targetType: 2, targetId: id, targetName: role.roleName });
      }
    });
  }
  formState.visibilityTargets = targets;
};

const loadEditData = async () => {
  if (!editId.value) return;
  try {
    const data = await announcementStore.fetchAdminDetail(editId.value);
    if (data) {
      formState.title = data.title || '';
      formState.content = data.content || '';
      formState.coverImage = data.coverImage || '';
      formState.visibilityType = data.visibilityType || 1;
      formState.effectiveTime = data.effectiveTime || '';
      formState.expireTime = data.expireTime || '';
      formState.isPinned = !!data.isPinned;
      formState.isImportant = !!data.isImportant;
      formState.visibilityTargets = data.visibilityList || [];
      if (data.visibilityType === 2) {
        selectedDeptIds.value = (data.visibilityList || [])
          .filter((v: any) => v.targetType === 1)
          .map((v: any) => v.targetId);
      } else if (data.visibilityType === 3) {
        selectedRoleIds.value = (data.visibilityList || [])
          .filter((v: any) => v.targetType === 2)
          .map((v: any) => v.targetId);
      }
    }
  } catch (error) {
    message.error('加载公告数据失败');
  }
};

const validateForm = async () => {
  try {
    await formRef.value.validate();
    if (formState.visibilityType === 2 && selectedDeptIds.value.length === 0) {
      message.warning('请选择至少一个可见部门');
      return false;
    }
    if (formState.visibilityType === 3 && selectedRoleIds.value.length === 0) {
      message.warning('请选择至少一个可见角色');
      return false;
    }
    updateVisibilityTargets();
    return true;
  } catch {
    return false;
  }
};

const handleSaveDraft = async () => {
  if (!formState.title) {
    message.warning('请输入公告标题');
    return;
  }
  updateVisibilityTargets();
  try {
    if (isEdit.value) {
      await announcementStore.update(editId.value, formState);
    } else {
      await announcementStore.createDraft(formState);
    }
    message.success('草稿保存成功');
    router.push('/admin/announcements');
  } catch (error) {
    message.error('保存失败');
  }
};

const openPreview = async () => {
  if (!formState.title) {
    message.warning('请先输入公告标题');
    return;
  }
  previewModalOpen.value = true;
};

const handlePublishClick = async () => {
  if (!(await validateForm())) return;
  const timeDesc = formState.effectiveTime
    ? `公告将在 ${formState.effectiveTime} 生效发布`
    : '公告将立即发布';
  const importantDesc = formState.isImportant
    ? '，并向所有目标用户推送消息通知'
    : '';
  Modal.confirm({
    title: '发布确认',
    content: `确定要发布这篇公告吗？${timeDesc}${importantDesc}。`,
    okText: '确认发布',
    okType: 'danger' as const,
    cancelText: '取消',
    onOk: async () => {
      try {
        let savedId: number;
        if (isEdit.value) {
          await announcementStore.update(editId.value, formState);
          savedId = editId.value;
        } else {
          const draft = await announcementStore.createDraft(formState);
          savedId = draft.id as number;
        }
        const result = await announcementStore.publish(savedId);
        if (result) {
          message.success('公告发布成功');
          router.push('/admin/announcements');
        }
      } catch (error) {
        message.error('发布失败');
      }
    },
  });
};

const handleFinish = () => {
  handleSaveDraft();
};

onMounted(async () => {
  await departmentStore.fetchDepartmentsTree();
  if (isEdit.value) {
    loadEditData();
  }
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

.back-section {
  margin-bottom: 8px;
}

.header-section {
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

.options-row {
  display: flex;
  gap: 24px;
  padding-top: 6px;
}

.editor-wrapper {
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  overflow: hidden;
}

.editor-toolbar {
  display: flex;
  gap: 8px;
  padding: 8px 12px;
  background: #fafafa;
  border-bottom: 1px solid #f0f0f0;
}

.editor-wrapper :deep(textarea) {
  border: none;
  border-radius: 0;
}

.cover-preview {
  margin-top: 12px;
}

.cover-preview img {
  max-height: 200px;
  border-radius: 8px;
}

.save-draft-btn,
.preview-btn {
  height: 44px;
  border-radius: 22px;
  font-weight: 600;
  font-size: 15px;
}

.save-draft-btn {
  background: #52c41a;
  border-color: #52c41a;
}

.preview-btn {
  background: #1890ff;
  border-color: #1890ff;
}

.publish-btn {
  height: 44px;
  border-radius: 22px;
  font-weight: 600;
  font-size: 15px;
  background: linear-gradient(135deg, #f5222d 0%, #cf1322 100%);
  border: none;
  box-shadow: 0 4px 12px rgba(245, 34, 45, 0.3);
}

.publish-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 18px rgba(245, 34, 45, 0.4);
}

.preview-content {
  padding: 8px 0;
}

.preview-tags {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.preview-title {
  font-size: 24px;
  font-weight: 700;
  margin: 0 0 16px 0;
}

.preview-meta {
  color: #8c8c8c;
  font-size: 14px;
  display: flex;
  gap: 24px;
  flex-wrap: wrap;
}

.preview-cover {
  text-align: center;
  margin-bottom: 24px;
}

.preview-cover img {
  max-width: 100%;
  max-height: 300px;
  border-radius: 12px;
}

.preview-body {
  font-size: 15px;
  line-height: 1.8;
  color: #333;
}

.preview-body :deep(h1),
.preview-body :deep(h2),
.preview-body :deep(h3) {
  margin-top: 20px;
  margin-bottom: 12px;
  font-weight: 600;
}

.preview-body :deep(p) {
  margin-bottom: 16px;
}

.preview-body :deep(li) {
  margin-bottom: 8px;
}

.modern-form :deep(.ant-form-item-label > label) {
  font-weight: 600;
  color: #2c3e50;
  font-size: 15px;
}

@media (max-width: 768px) {
  .page-container {
    padding: 16px 12px;
  }

  .options-row {
    flex-direction: column;
    gap: 8px;
  }
}
</style>
