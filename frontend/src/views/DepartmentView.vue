<template>
  <div class="page-container">
    <div class="content-wrapper">
      <a-card class="main-card" :bordered="false">
        <div class="header-section">
          <div class="title-group">
            <h1 class="page-title">部门管理</h1>
            <p class="subtitle">Department Management</p>
          </div>
          <div class="header-actions">
            <a-space>
              <a-input-search
                v-model:value="searchKeyword"
                placeholder="按编码或名称搜索"
                style="width: 240px"
                @search="handleSearch"
                allow-clear
              />
              <a-button @click="showNotifications">
                <template #icon><bell-outlined /></template>
                通知
                <a-badge :count="store.notifications.length" :offset="[-4, 2]" />
              </a-button>
              <a-button type="primary" class="add-btn" @click="showModal()">
                <template #icon><plus-outlined /></template>
                添加部门
              </a-button>
            </a-space>
          </div>
        </div>

        <div class="view-toggle">
          <a-segmented v-model:value="viewMode" :options="viewOptions" />
        </div>

        <a-spin :spinning="store.loading">
          <div v-if="viewMode === 'table'" class="table-view">
            <a-table
              :columns="tableColumns"
              :data-source="filteredFlatList"
              :pagination="{ pageSize: 10, showSizeChanger: true }"
              row-key="id"
              class="modern-table"
              :scroll="{ x: 1200 }"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'name'">
                  <span class="dept-name" :class="{ disabled: !record.enabled }">
                    {{ record.name }}
                  </span>
                </template>
                <template v-if="column.key === 'status'">
                  <a-tag :color="record.enabled ? 'green' : 'default'">
                    {{ record.enabled ? '启用' : '停用' }}
                  </a-tag>
                </template>
                <template v-if="column.key === 'headcount'">
                  <div class="headcount-wrap">
                    <span>{{ record.employeeCount || 0 }} / {{ record.headcountLimit }}</span>
                    <a-progress
                      :percent="Math.min(100, ((record.employeeCount || 0) / record.headcountLimit) * 100)"
                      :status="record.overHeadcount ? 'exception' : 'normal'"
                      :show-info="false"
                      size="small"
                      style="margin-top: 4px"
                    />
                    <a-tag v-if="record.overHeadcount" color="red" style="margin-left: 8px">超员</a-tag>
                  </div>
                </template>
                <template v-if="column.key === 'action'">
                  <a-space size="small">
                    <a-button type="link" class="action-btn" @click="showModal(record)">编辑</a-button>
                    <a-divider type="vertical" />
                    <a-popconfirm
                      :title="record.enabled ? '确定要停用该部门吗？子部门将同步停用' : '确定要启用该部门吗？'"
                      @confirm="handleToggle(record)"
                      ok-text="确定"
                      cancel-text="取消"
                    >
                      <a-button type="link" :type="record.enabled ? 'danger' : 'primary'" class="action-btn">
                        {{ record.enabled ? '停用' : '启用' }}
                      </a-button>
                    </a-popconfirm>
                    <a-divider type="vertical" />
                    <a-popconfirm
                      title="确定要删除吗？存在下级部门或员工时无法删除"
                      @confirm="handleDelete(record.id)"
                      ok-text="确定"
                      cancel-text="取消"
                      placement="topRight"
                    >
                      <a-button type="link" danger class="action-btn delete">删除</a-button>
                    </a-popconfirm>
                  </a-space>
                </template>
              </template>
            </a-table>
          </div>

          <div v-else class="tree-view">
            <a-table
              :columns="treeColumns"
              :data-source="filteredTreeData"
              :pagination="false"
              row-key="id"
              class="modern-table"
              :scroll="{ x: 1200 }"
              :default-expand-all="true"
              :children-column-key="'children'"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'name'">
                  <span class="dept-name" :class="{ disabled: !record.enabled }">
                    {{ record.name }}
                  </span>
                </template>
                <template v-if="column.key === 'status'">
                  <a-tag :color="record.enabled ? 'green' : 'default'">
                    {{ record.enabled ? '启用' : '停用' }}
                  </a-tag>
                </template>
                <template v-if="column.key === 'headcount'">
                  <div class="headcount-wrap">
                    <span>{{ record.employeeCount || 0 }} / {{ record.headcountLimit }}</span>
                    <a-progress
                      :percent="Math.min(100, ((record.employeeCount || 0) / record.headcountLimit) * 100)"
                      :status="record.overHeadcount ? 'exception' : 'normal'"
                      :show-info="false"
                      size="small"
                      style="margin-top: 4px"
                    />
                    <a-tag v-if="record.overHeadcount" color="red" style="margin-left: 8px">超员</a-tag>
                  </div>
                </template>
                <template v-if="column.key === 'action'">
                  <a-space size="small">
                    <a-button type="link" class="action-btn" @click="showModal(record)">编辑</a-button>
                    <a-divider type="vertical" />
                    <a-popconfirm
                      :title="record.enabled ? '确定要停用该部门吗？子部门将同步停用' : '确定要启用该部门吗？'"
                      @confirm="handleToggle(record)"
                      ok-text="确定"
                      cancel-text="取消"
                    >
                      <a-button type="link" :type="record.enabled ? 'danger' : 'primary'" class="action-btn">
                        {{ record.enabled ? '停用' : '启用' }}
                      </a-button>
                    </a-popconfirm>
                    <a-divider type="vertical" />
                    <a-popconfirm
                      title="确定要删除吗？存在下级部门或员工时无法删除"
                      @confirm="handleDelete(record.id)"
                      ok-text="确定"
                      cancel-text="取消"
                      placement="topRight"
                    >
                      <a-button type="link" danger class="action-btn delete">删除</a-button>
                    </a-popconfirm>
                  </a-space>
                </template>
              </template>
            </a-table>
          </div>
        </a-spin>
      </a-card>

      <a-modal
        v-model:open="modalVisible"
        :title="isEdit ? '编辑部门' : '添加部门'"
        @ok="handleOk"
        ok-text="保存"
        cancel-text="取消"
        :confirm-loading="submitting"
        centered
        :mask-style="{ backdropFilter: 'blur(4px)' }"
        :width="600"
      >
        <a-form ref="formRef" :model="formState" layout="vertical" class="modern-form">
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item
                label="部门名称"
                name="name"
                :rules="[{ required: true, message: '请输入部门名称!' }]"
              >
                <a-input v-model:value="formState.name" placeholder="请输入部门名称" />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item
                label="部门编码"
                name="code"
                :rules="[
                  { required: true, message: '请输入部门编码!' },
                  { validator: validateCodeUnique },
                ]"
              >
                <a-input v-model:value="formState.code" placeholder="请输入部门编码，全局唯一" />
              </a-form-item>
            </a-col>
          </a-row>
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="上级部门" name="parentId">
              <a-tree-select
                v-model:value="formState.parentId"
                :tree-data="parentTreeOptions"
                :field-names="{ label: 'name', value: 'id', children: 'children' }"
                placeholder="请选择上级部门（不选则为顶级部门）"
                allow-clear
                tree-default-expand-all
                :disabled="isEdit && hasChildren"
              />
            </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item
                label="负责人"
                name="leader"
              >
                <a-input v-model:value="formState.leader" placeholder="请输入负责人姓名" />
              </a-form-item>
            </a-col>
          </a-row>
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item
                label="编制人数上限"
                name="headcountLimit"
                :rules="[{ required: true, message: '请输入编制人数上限!' }]"
              >
                <a-input-number
                  v-model:value="formState.headcountLimit"
                  :min="1"
                  placeholder="编制人数上限"
                  style="width: 100%"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="启用状态" name="enabled">
                <a-switch v-model:checked="formState.enabled" checked-children="启用" un-checked-children="停用" />
              </a-form-item>
            </a-col>
          </a-row>
          <a-form-item label="部门描述" name="description">
            <a-textarea
              v-model:value="formState.description" placeholder="请输入部门描述" :rows="3" />
          </a-form-item>
        </a-form>
      </a-modal>

      <a-drawer
        v-model:open="notificationVisible"
        title="负责人变更通知"
        placement="right"
        :width="480"
        @close="notificationVisible = false"
      >
        <a-timeline v-if="store.notifications.length" style="padding-top: 16px">
          <a-timeline-item v-for="item in store.notifications" :key="item.id">
          <template #dot><bell-outlined style="color: #1890ff" />
          </template>
          <div class="notification-item">
          <div class="notification-time">{{ formatTime(item.createTime) }}</div>
            <div class="notification-content">{{ item.content }}</div>
            <a-tag v-if="item.oldLeader" color="blue">原负责人: {{ item.oldLeader }}</a-tag>
            <a-tag v-if="item.newLeader" color="green">新负责人: {{ item.newLeader }}</a-tag>
          </div>
          </a-timeline-item>
        </a-timeline>
        <a-empty v-else description="暂无通知" />
      </a-drawer>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { onMounted, ref, reactive, computed } from 'vue';
import { useDepartmentStore, type Department } from '../stores/department';
import { PlusOutlined, BellOutlined } from '@ant-design/icons-vue';
import type { Rule } from 'ant-design-vue/es/form';
import { message } from 'ant-design-vue';

const store = useDepartmentStore();
const modalVisible = ref(false);
const notificationVisible = ref(false);
const isEdit = ref(false);
const submitting = ref(false);
const formRef = ref();
const searchKeyword = ref('');
const viewMode = ref<'table' | 'tree'>('table');

const viewOptions = [
  { label: '列表视图', value: 'table' },
  { label: '树形视图', value: 'tree' },
];

const formState = reactive<Department>({
  name: '',
  code: '',
  description: '',
  leader: '',
  parentId: null,
  headcountLimit: 10,
  enabled: true,
});

const hasChildren = computed(() => {
  if (!formState.id) return false;
  const flat = store.flattenDepartments(store.departmentsTree);
  return flat.some((d) => d.parentId === formState.id);
});

const parentTreeOptions = computed(() => {
  const excludeIds = new Set<number>();
  if (isEdit.value && formState.id) {
    const collect = (items: Department[], parentId: number) => {
      items.forEach((item) => {
        if (item.parentId === parentId || excludeIds.has(item.parentId as number)) {
          excludeIds.add(item.id as number);
        }
        if (item.children) {
          collect(item.children, parentId);
        }
      });
    };
    excludeIds.add(formState.id);
    collect(store.departmentsTree, formState.id);
  }
  const filter = (items: Department[]): Department[] => {
    return items
      .filter((d) => !excludeIds.has(d.id as number) && d.enabled)
      .map((d) => ({
        ...d,
        children: d.children ? filter(d.children) : undefined,
      }));
  });
  return filter(store.departmentsTree);
});

const tableColumns = [
  { title: '部门名称', dataIndex: 'name', key: 'name', width: '18%' },
  { title: '部门编码', dataIndex: 'code', key: 'code', width: '12%' },
  { title: '负责人', dataIndex: 'leader', key: 'leader', width: '10%' },
  { title: '上级部门', dataIndex: 'parentId', key: 'parentId', width: '15%',
    customRender: ({ record }: { record: Department }) => {
      if (!record.parentId) return '-';
      return store.getDepartmentName(record.parentId);
    },
  },
  { title: '人员编制', key: 'headcount', width: '20%' },
  { title: '状态', key: 'status', width: '8%' },
  { title: '描述', dataIndex: 'description', key: 'description', width: '12%' },
  { title: '操作', key: 'action', width: '17%', align: 'center' },
];

const treeColumns = [
  { title: '部门名称', dataIndex: 'name', key: 'name', width: '25%' },
  { title: '部门编码', dataIndex: 'code', key: 'code', width: '12%' },
  { title: '负责人', dataIndex: 'leader', key: 'leader', width: '10%' },
  { title: '人员编制', key: 'headcount', width: '20%' },
  { title: '状态', key: 'status', width: '8%' },
  { title: '描述', dataIndex: 'description', key: 'description', width: '10%' },
  { title: '操作', key: 'action', width: '15%', align: 'center' },
];

const filteredFlatList = computed(() => {
  if (!searchKeyword.value) return store.departmentsFlat;
  const kw = searchKeyword.value.toLowerCase();
  return store.departmentsFlat.filter(
    (d) =>
      d.code.toLowerCase().includes(kw) ||
      d.name.toLowerCase().includes(kw)
  );
});

const filteredTreeData = computed(() => {
  if (!searchKeyword.value) return store.departmentsTree;
  const kw = searchKeyword.value.toLowerCase();
  const flatMatches = store.departmentsFlat.filter(
    (d) =>
      d.code.toLowerCase().includes(kw) ||
      d.name.toLowerCase().includes(kw)
  );
  const matchedIds = new Set(flatMatches.map((d) => d.id));
  const addParents = (items: Department[]): Department[] => {
    return items
      .filter((item) => {
        const children = item.children ? addParents(item.children) : [];
        const selfMatch = matchedIds.has(item.id as number);
        return selfMatch || (children && children.length > 0);
      })
      .map((item) => ({
        ...item,
        children: item.children ? addParents(item.children) : undefined,
      }));
  };
  return addParents(store.departmentsTree);
});

onMounted(async () => {
  await store.refreshAll();
  await store.fetchNotifications();
});

const validateCodeUnique: Rule['validator'] = async (_rule: any, value: string) => {
  if (!value) return Promise.resolve();
  const isUnique = await store.checkCodeUnique(value, formState.id);
  if (!isUnique) {
    return Promise.reject('部门编码已存在');
  }
  return Promise.resolve();
};

const showModal = (record?: Department) => {
  if (record) {
    isEdit.value = true;
    Object.assign(formState, {
      id: record.id,
      name: record.name,
      code: record.code,
      description: record.description || '',
      leader: record.leader || '',
      parentId: record.parentId || null,
      headcountLimit: record.headcountLimit,
      enabled: record.enabled,
    });
  } else {
    isEdit.value = false;
    formState.id = undefined;
    formState.name = '';
    formState.code = '';
    formState.description = '';
    formState.leader = '';
    formState.parentId = null;
    formState.headcountLimit = 10;
    formState.enabled = true;
  }
  modalVisible.value = true;
};

const showNotifications = () => {
  store.fetchNotifications();
  notificationVisible.value = true;
};

const handleOk = async () => {
  try {
    submitting.value = true;
    await formRef.value.validate();
    if (isEdit.value) {
      await store.updateDepartment({ ...formState });
    } else {
      await store.createDepartment({ ...formState });
    }
    modalVisible.value = false;
  } catch (error) {
    // validation failed
  } finally {
    submitting.value = false;
  }
};

const handleToggle = async (record: Department) => {
  await store.toggleEnabled(record.id as number, !record.enabled);
};

const handleDelete = async (id: number) => {
  try {
    await store.deleteDepartment(id);
  } catch (error) {
    // error handled by interceptor
  }
};

const handleSearch = () => {
  message.success('搜索完成');
};

const formatTime = (t: string) => {
  if (!t) return '';
  const date = new Date(t);
  return date.toLocaleString('zh-CN');
};
</script>

<style scoped>
.page-container {
  min-height: 100vh;
  padding: 40px 24px;
  display: flex;
  justify-content: center;
  align-items: flex-start;
}

.content-wrapper {
  width: 100%;
  max-width: 1400px;
}

.main-card {
  border-radius: 16px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
  overflow: hidden;
}

.main-card:hover {
  box-shadow: 0 15px 35px rgba(0, 0, 0, 0.1);
}

.header-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding: 0 8px;
}

.page-title {
  font-size: 28px;
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
  font-size: 14px;
}

.add-btn {
  height: 40px;
  padding: 0 24px;
  border-radius: 20px;
  font-weight: 600;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  box-shadow: 0 4px 15px rgba(118, 75, 162, 0.3);
  transition: all 0.3s ease;
}

.add-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(118, 75, 162, 0.4);
}

.view-toggle {
  margin-bottom: 16px 0;
}

.dept-name {
  font-weight: 500;
  color: #2c3e50;
}

.dept-name.disabled {
  color: #bfbfbf;
  text-decoration: line-through;
}

.headcount-wrap {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.action-btn {
  padding: 0;
  font-weight: 500;
}

.action-btn.delete {
  color: #ff4d4f;
}

.notification-item {
  padding: 8px 0;
}

.notification-time {
  font-size: 12px;
  color: #8c8c8c;
  margin-bottom: 4px;
}

.notification-content {
  margin-bottom: 8px;
  color: #2c3e50;
}

@media (max-width: 768px) {
  .page-container {
    padding: 20px 16px;
  }

  .header-section {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }

  .header-actions {
    width: 100%;
  }

  .header-actions .ant-space {
    width: 100%;
    flex-wrap: wrap;
  }

  .add-btn {
    width: 100%;
  }
}
</style>
