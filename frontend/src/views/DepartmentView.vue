<template>
  <div class="org-page">
    <div class="org-header">
      <div class="title-group">
        <h1 class="page-title">组织架构</h1>
        <p class="subtitle">Organization Structure Management</p>
      </div>
      <div class="header-actions">
        <a-space>
          <a-input-search
            v-model:value="searchKeyword"
            placeholder="搜索部门名称或编码"
            style="width: 240px"
            @search="handleSearch"
            allow-clear
          />
          <a-button @click="showSnapshotModal">
            <template #icon><camera-outlined /></template>
            版本快照
          </a-button>
          <a-button v-permission="['ADMIN']" type="primary" class="add-btn" @click="showModal()">
            <template #icon><plus-outlined /></template>
            新建部门
          </a-button>
        </a-space>
      </div>
    </div>

    <div class="org-content">
      <div class="tree-panel">
        <a-card class="tree-card" :bordered="false">
          <a-spin :spinning="store.loading">
            <a-tree
              :tree-data="treeData"
              :field-names="{ title: 'name', key: 'id', children: 'children' }"
              :expanded-keys="expandedKeys"
              :selected-keys="selectedKeys"
              :draggable="hasPermission"
              block-node
              @expand="onExpand"
              @select="onSelect"
              @drop="onDrop"
              @dragenter="onDragEnter"
            >
              <template #title="{ record }">
                <div class="tree-node" @click.stop="selectNode(record)">
                  <span class="node-name" :class="{ disabled: !record.enabled }">
                    <component :is="getLevelIcon(record.levelType)" class="level-icon" />
                    {{ record.name }}
                  </span>
                  <span class="node-badge">
                    <a-tag v-if="record.overHeadcount" color="red" size="small">超员</a-tag>
                    <a-tag v-else color="blue" size="small">
                      {{ record.activeEmployeeCount || 0 }}/{{ record.headcountLimit }}
                    </a-tag>
                  </span>
                </div>
              </template>
            </a-tree>
            <a-empty v-if="!store.loading && treeData.length === 0" description="暂无部门数据" />
          </a-spin>
        </a-card>
      </div>

      <div class="detail-panel">
        <a-spin :spinning="detailLoading">
          <div v-if="store.currentDetail" class="detail-content">
            <div class="detail-header">
              <div class="dept-title-row">
                <h2 class="dept-name">
                  <component :is="getLevelIcon(store.currentDetail.department.levelType)" class="level-icon-lg" />
                  {{ store.currentDetail.department.name }}
                </h2>
                <a-tag :color="store.currentDetail.department.enabled ? 'green' : 'default'">
                  {{ store.currentDetail.department.enabled ? '启用' : '停用' }}
                </a-tag>
                <span class="dept-code">{{ store.currentDetail.department.code }}</span>
              </div>
              <p class="dept-desc">{{ store.currentDetail.department.description || '暂无描述' }}</p>
              <div class="dept-info-cards">
                <div class="info-card">
                  <div class="info-label">负责人</div>
                  <div class="info-value">
                    <user-outlined class="info-icon" />
                    {{ store.currentDetail.department.leader || '未设置' }}
                  </div>
                </div>
                <div class="info-card">
                  <div class="info-label">副负责人</div>
                  <div class="info-value">
                    <team-outlined class="info-icon" />
                    {{ store.currentDetail.department.deputyLeader || '未设置' }}
                  </div>
                </div>
                <div class="info-card">
                  <div class="info-label">人员编制</div>
                  <div class="info-value">
                    <team-outlined class="info-icon" />
                    <span :class="{ danger: store.currentDetail.department.overHeadcount }">
                      {{ store.currentDetail.department.activeEmployeeCount || 0 }}
                    </span>
                    <span class="divider">/</span>
                    <span>{{ store.currentDetail.department.headcountLimit }}</span>
                    <a-tag v-if="store.currentDetail.department.overHeadcount" color="red" style="margin-left: 8px">超员</a-tag>
                  </div>
                </div>
                <div class="info-card">
                  <div class="info-label">所属层级</div>
                  <div class="info-value">
                    <apartment-outlined class="info-icon" />
                    {{ getLevelTypeName(store.currentDetail.department.levelType) }}
                  </div>
                </div>
              </div>
              <div class="detail-actions">
                <a-space>
                  <a-button v-permission="['ADMIN']" @click="showModal(store.currentDetail!.department)">
                    <template #icon><edit-outlined /></template>
                    编辑
                  </a-button>
                  <a-button v-permission="['ADMIN']" @click="showModal(undefined, store.currentDetail!.department.id)">
                    <template #icon><folder-add-outlined /></template>
                    添加子部门
                  </a-button>
                  <a-button
                    v-permission="['ADMIN']"
                    :type="store.currentDetail.department.enabled ? 'default' : 'primary'"
                    @click="handleToggle(store.currentDetail!.department)"
                  >
                    {{ store.currentDetail.department.enabled ? '停用' : '启用' }}
                  </a-button>
                  <a-popconfirm
                    v-permission="['ADMIN']"
                    :title="getDeleteConfirmText(store.currentDetail!.department)"
                    @confirm="handleDelete(store.currentDetail!.department.id as number)"
                    ok-text="确定"
                    cancel-text="取消"
                  >
                    <a-button danger>
                      <template #icon><delete-outlined /></template>
                      删除
                    </a-button>
                  </a-popconfirm>
                </a-space>
              </div>
            </div>

            <a-tabs v-model:activeKey="activeTab" class="detail-tabs">
              <a-tab-pane key="employees" tab="在职员工">
                <template #tab>
                  <span><user-outlined /> 在职员工 ({{ store.currentDetail.activeEmployees.length }})</span>
                </template>
                <a-table
                  :columns="employeeColumns"
                  :data-source="store.currentDetail.activeEmployees"
                  :pagination="{ pageSize: 8, showSizeChanger: false }"
                  row-key="id"
                  size="middle"
                >
                  <template #bodyCell="{ column, record }">
                    <template v-if="column.key === 'status'">
                      <a-tag :color="getStatusColor(record.status)">
                        {{ getStatusName(record.status) }}
                      </a-tag>
                    </template>
                  </template>
                </a-table>
                <a-empty v-if="store.currentDetail.activeEmployees.length === 0" description="暂无在职员工" />
              </a-tab-pane>

              <a-tab-pane key="subDepts" tab="下级部门">
                <template #tab>
                  <span><apartment-outlined /> 下级部门 ({{ store.currentDetail.subDepartments.length }})</span>
                </template>
                <div v-if="store.currentDetail.subDepartments.length > 0" class="sub-dept-grid">
                  <div
                    v-for="sub in store.currentDetail.subDepartments"
                    :key="sub.id"
                    class="sub-dept-card"
                    @click="jumpToDept(sub.id as number)"
                  >
                    <div class="sub-dept-header">
                      <component :is="getLevelIcon(sub.levelType)" class="sub-dept-icon" />
                      <span class="sub-dept-name">{{ sub.name }}</span>
                      <a-tag v-if="sub.overHeadcount" color="red" size="small">超员</a-tag>
                    </div>
                    <div class="sub-dept-info">
                      <span>负责人：{{ sub.leader || '未设置' }}</span>
                      <span>{{ sub.activeEmployeeCount || 0 }}/{{ sub.headcountLimit }}人</span>
                    </div>
                    <div class="sub-dept-code">{{ sub.code }}</div>
                  </div>
                </div>
                <a-empty v-else description="暂无下级部门" />
              </a-tab-pane>

              <a-tab-pane key="history" tab="负责人变更历史">
                <template #tab>
                  <span><history-outlined /> 变更历史</span>
                </template>
                <a-timeline v-if="store.currentDetail.leaderChangeHistory.length > 0">
                  <a-timeline-item v-for="item in store.currentDetail.leaderChangeHistory" :key="item.id">
                    <template #dot>
                      <component :is="item.changeType === 1 ? 'user-outlined' : 'team-outlined'" 
                        :style="{ color: item.changeType === 1 ? '#1890ff' : '#52c41a', fontSize: '16px' }" />
                    </template>
                    <div class="history-item">
                      <div class="history-header">
                        <span class="history-type">
                          {{ item.changeType === 1 ? '负责人变更' : '副负责人变更' }}
                        </span>
                        <span class="history-time">{{ formatTime(item.createTime) }}</span>
                      </div>
                      <div class="history-content">
                        <a-tag v-if="item.oldLeader" color="default">原：{{ item.oldLeader }}</a-tag>
                        <arrow-right-outlined class="arrow-icon" />
                        <a-tag v-if="item.newLeader" color="green">新：{{ item.newLeader }}</a-tag>
                        <span v-if="!item.oldLeader && !item.newLeader" style="color: #8c8c8c">无变更记录</span>
                      </div>
                      <div class="history-operator" v-if="item.operatorName">
                        操作人：{{ item.operatorName }}
                      </div>
                    </div>
                  </a-timeline-item>
                </a-timeline>
                <a-empty v-else description="暂无变更历史" />
              </a-tab-pane>
            </a-tabs>
          </div>

          <a-empty v-else description="请在左侧选择部门查看详情" />
        </a-spin>
      </div>
    </div>

    <a-modal
      v-model:open="modalVisible"
      :title="modalTitle"
      @ok="handleOk"
      ok-text="保存"
      cancel-text="取消"
      :confirm-loading="submitting"
      centered
      :mask-style="{ backdropFilter: 'blur(4px)' }"
      :width="640"
      :destroy-on-close="true"
    >
      <a-form ref="formRef" :model="formState" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="部门名称" name="name" :rules="[{ required: true, message: '请输入部门名称' }]">
              <a-input v-model:value="formState.name" placeholder="请输入部门名称" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="部门编码" name="code" :rules="[
              { required: true, message: '请输入部门编码' },
              { validator: validateCodeUnique },
            ]">
              <a-input v-model:value="formState.code" placeholder="全局唯一编码" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="层级类型" name="levelType">
              <a-select v-model:value="formState.levelType" placeholder="请选择层级类型" allow-clear>
                <a-select-option :value="1">公司</a-select-option>
                <a-select-option :value="2">事业部</a-select-option>
                <a-select-option :value="3">小组</a-select-option>
                <a-select-option :value="4">项目组</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="上级部门" name="parentId">
              <a-tree-select
                v-model:value="formState.parentId"
                :tree-data="parentTreeOptions"
                :field-names="{ label: 'name', value: 'id', children: 'children' }"
                placeholder="不选则为顶级部门"
                allow-clear
                tree-default-expand-all
                :disabled="isEdit && hasChildren"
              />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="负责人" name="leader">
              <a-input v-model:value="formState.leader" placeholder="请输入负责人姓名" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="副负责人" name="deputyLeader">
              <a-input v-model:value="formState.deputyLeader" placeholder="请输入副负责人姓名" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="编制人数上限" name="headcountLimit" :rules="[{ required: true, message: '请输入编制上限' }]">
              <a-input-number v-model:value="formState.headcountLimit" :min="1" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="启用状态" name="enabled">
              <a-switch v-model:checked="formState.enabled" checked-children="启用" un-checked-children="停用" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="部门描述" name="description">
          <a-textarea v-model:value="formState.description" :rows="3" placeholder="请输入部门描述" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="snapshotModalVisible"
      title="组织架构版本快照"
      :footer="null"
      :width="800"
      :destroy-on-close="true"
    >
      <div class="snapshot-toolbar">
        <a-input
          v-model:value="newSnapshotName"
          placeholder="快照名称"
          style="width: 200px; margin-right: 8px"
        />
        <a-input
          v-model:value="newSnapshotDesc"
          placeholder="描述（可选）"
          style="width: 200px; margin-right: 8px"
        />
        <a-button v-permission="['ADMIN']" type="primary" @click="handleCreateSnapshot">
          <template #icon><plus-outlined /></template>
          创建快照
        </a-button>
      </div>
      <a-table
        :columns="snapshotColumns"
        :data-source="store.snapshots"
        :pagination="{ pageSize: 8 }"
        row-key="id"
        size="middle"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'type'">
            <a-tag :color="record.snapshotType === 1 ? 'blue' : 'orange'">
              {{ record.snapshotType === 1 ? '手动快照' : '自动快照' }}
            </a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a-space size="small">
              <a-button type="link" @click="handlePreviewSnapshot(record)">预览</a-button>
              <a-popconfirm
                v-permission="['ADMIN']"
                title="确定删除该快照？"
                @confirm="store.deleteSnapshot(record.id)"
                ok-text="确定"
                cancel-text="取消"
              >
                <a-button type="link" danger>删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-modal>

    <a-modal
      v-model:open="previewModalVisible"
      title="快照预览"
      :footer="null"
      :width="600"
      :destroy-on-close="true"
    >
      <div v-if="previewSnapshotData" class="preview-header">
        <div class="preview-title">{{ currentPreviewSnapshot?.snapshotName }}</div>
        <div class="preview-meta">
          <span>创建人：{{ currentPreviewSnapshot?.operatorName || '系统' }}</span>
          <span>时间：{{ formatTime(currentPreviewSnapshot?.createTime) }}</span>
        </div>
      </div>
      <a-tree
        v-if="previewSnapshotData"
        :tree-data="previewSnapshotData"
        :field-names="{ title: 'name', key: 'id', children: 'children' }"
        default-expand-all
        block-node
      >
        <template #title="{ record }">
          <span>
            {{ record.name }}
            <a-tag color="blue" size="small" style="margin-left: 8px">
              {{ record.activeEmployeeCount || 0 }}/{{ record.headcountLimit }}
            </a-tag>
          </span>
        </template>
      </a-tree>
    </a-modal>
  </div>
</template>

<script lang="ts" setup>
import { ref, reactive, computed, onMounted, watch } from 'vue';
import { useDepartmentStore, type Department } from '../stores/department';
import {
  PlusOutlined, EditOutlined, DeleteOutlined, CameraOutlined,
  UserOutlined, TeamOutlined, ApartmentOutlined, HistoryOutlined,
  ArrowRightOutlined, FolderAddOutlined,
  BankOutlined, BuildOutlined, BulbOutlined,
} from '@ant-design/icons-vue';
import { message } from 'ant-design-vue';
import type { Rule } from 'ant-design-vue/es/form';
import { useAuthStore } from '../stores/auth';

const store = useDepartmentStore();
const authStore = useAuthStore();

const hasPermission = computed(() => authStore.hasRole('ADMIN'));

const modalVisible = ref(false);
const snapshotModalVisible = ref(false);
const previewModalVisible = ref(false);
const isEdit = ref(false);
const isAddChild = ref(false);
const submitting = ref(false);
const formRef = ref();
const searchKeyword = ref('');
const detailLoading = ref(false);
const activeTab = ref('employees');
const newSnapshotName = ref('');
const newSnapshotDesc = ref('');
const previewSnapshotData = ref<Department[]>([]);
const currentPreviewSnapshot = ref<any>(null);

const expandedKeys = ref<number[]>([]);
const selectedKeys = ref<number[]>([]);
let expandedKeysDirty = false;

const formState = reactive<Department>({
  name: '',
  code: '',
  description: '',
  leader: '',
  deputyLeader: '',
  parentId: null,
  headcountLimit: 10,
  levelType: undefined,
  enabled: true,
});

const modalTitle = computed(() => {
  if (isAddChild.value) return '添加子部门';
  return isEdit.value ? '编辑部门' : '新建部门';
});

const treeData = computed(() => {
  if (!searchKeyword.value) return store.departmentsTree;
  const kw = searchKeyword.value.toLowerCase();
  const flatMatches = store.departmentsFlat.filter(
    (d) => d.code.toLowerCase().includes(kw) || d.name.toLowerCase().includes(kw)
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

const hasChildren = computed(() => {
  if (!formState.id) return false;
  return store.departmentsFlat.some((d) => d.parentId === formState.id);
});

const parentTreeOptions = computed(() => {
  const excludeIds = new Set<number>();
  if (isEdit.value && formState.id) {
    excludeIds.add(formState.id as number);
    const collect = (items: Department[], parentId: number) => {
      items.forEach((item) => {
        if (item.parentId === parentId || excludeIds.has(item.parentId as number)) {
          excludeIds.add(item.id as number);
        }
        if (item.children) collect(item.children, parentId);
      });
    };
    collect(store.departmentsTree, formState.id as number);
  }
  const filter = (items: Department[]): Department[] => {
    return items
      .filter((d) => !excludeIds.has(d.id as number))
      .map((d) => ({
        ...d,
        children: d.children ? filter(d.children) : undefined,
      }));
  };
  return filter(store.departmentsTree);
});

const employeeColumns = [
  { title: '姓名', dataIndex: 'name', key: 'name', width: '20%' },
  { title: '邮箱', dataIndex: 'email', key: 'email', width: '30%' },
  { title: '职位', dataIndex: 'role', key: 'role', width: '25%' },
  { title: '入职日期', dataIndex: 'hireDate', key: 'hireDate', width: '15%' },
  { title: '状态', key: 'status', width: '10%' },
];

const snapshotColumns = [
  { title: '快照名称', dataIndex: 'snapshotName', key: 'name', width: '25%' },
  { title: '类型', key: 'type', width: '12%' },
  { title: '描述', dataIndex: 'description', key: 'desc', width: '25%' },
  { title: '操作人', dataIndex: 'operatorName', key: 'op', width: '12%' },
  { title: '创建时间', dataIndex: 'createTime', key: 'time', width: '16%',
    customRender: ({ record }: any) => formatTime(record.createTime) },
  { title: '操作', key: 'action', width: '10%' },
];

const getLevelIcon = (levelType?: number) => {
  switch (levelType) {
    case 1: return BankOutlined;
    case 2: return BuildOutlined;
    case 3: return TeamOutlined;
    case 4: return BulbOutlined;
    default: return ApartmentOutlined;
  }
};

const getLevelTypeName = (levelType?: number) => {
  switch (levelType) {
    case 1: return '公司';
    case 2: return '事业部';
    case 3: return '小组';
    case 4: return '项目组';
    default: return '未设置';
  }
};

const getStatusName = (status?: number) => {
  switch (status) {
    case 1: return '在职';
    case 2: return '试用期';
    case 3: return '待转正';
    case 4: return '已离职';
    default: return '未知';
  }
};

const getStatusColor = (status?: number) => {
  switch (status) {
    case 1: return 'green';
    case 2: return 'blue';
    case 3: return 'orange';
    case 4: return 'default';
    default: return 'default';
  }
};

const getDeleteConfirmText = (dept: Department) => {
  if ((dept.activeEmployeeCount || 0) > 0) {
    return `该部门下有 ${dept.activeEmployeeCount} 名在职员工，无法删除`;
  }
  return '确定删除该部门？删除后不可恢复';
};

const formatTime = (t?: string) => {
  if (!t) return '';
  return new Date(t).toLocaleString('zh-CN');
};

onMounted(async () => {
  await store.refreshAll();
  await store.fetchNotifications();
  await store.fetchTreeState('department');
  await store.fetchSnapshots();

  if (store.treeState.expandedIds.length > 0) {
    expandedKeys.value = store.treeState.expandedIds;
  } else {
    expandedKeys.value = store.departmentsTree.map((d) => d.id as number);
  }
  if (store.treeState.selectedId) {
    selectedKeys.value = [store.treeState.selectedId];
    await loadDetail(store.treeState.selectedId);
  }
});

let saveTimer: ReturnType<typeof setTimeout> | null = null;
watch([expandedKeys, selectedKeys], () => {
  expandedKeysDirty = true;
  if (saveTimer) clearTimeout(saveTimer);
  saveTimer = setTimeout(() => {
    if (expandedKeysDirty) {
      store.saveTreeState(
        expandedKeys.value,
        selectedKeys.value.length > 0 ? selectedKeys.value[0] : undefined,
        'department'
      );
      expandedKeysDirty = false;
    }
  }, 1000);
}, { deep: true });

const onExpand = (keys: number[]) => {
  expandedKeys.value = keys;
};

const onSelect = (keys: number[]) => {
  selectedKeys.value = keys;
};

const selectNode = async (record: Department) => {
  selectedKeys.value = [record.id as number];
  await loadDetail(record.id as number);
};

const loadDetail = async (id: number) => {
  detailLoading.value = true;
  try {
    await store.getDepartmentDetail(id);
  } finally {
    detailLoading.value = false;
  }
};

const jumpToDept = async (id: number) => {
  selectedKeys.value = [id];
  const ensureExpanded = (items: Department[], targetId: number, parents: number[] = []): number[] | null => {
    for (const item of items) {
      if (item.id === targetId) return parents;
      if (item.children) {
        const result = ensureExpanded(item.children, targetId, [...parents, item.id as number]);
        if (result) return result;
      }
    }
    return null;
  };
  const parents = ensureExpanded(store.departmentsTree, id);
  if (parents) {
    expandedKeys.value = Array.from(new Set([...expandedKeys.value, ...parents]));
  }
  await loadDetail(id);
};

const onDragEnter = (_info: any) => {
  // placeholder for drag visual feedback
};

const onDrop = async (info: any) => {
  const dropKey = info.node.eventKey;
  const dragKey = info.dragNode.eventKey;
  const dropToGap = info.dropToGap;

  let targetParentId: number | null;
  if (dropToGap) {
    const node = info.node;
    targetParentId = node.dataRef.parentId ?? null;
  } else {
    targetParentId = dropKey;
  }

  try {
    await store.moveDepartment(dragKey, targetParentId);
  } catch (e) {
    // handled
  }
};

const validateCodeUnique: Rule['validator'] = async (_rule: any, value: string) => {
  if (!value) return Promise.resolve();
  const isUnique = await store.checkCodeUnique(value, formState.id as number);
  if (!isUnique) return Promise.reject('部门编码已存在');
  return Promise.resolve();
};

const showModal = (record?: Department, parentId?: number) => {
  if (record) {
    isEdit.value = true;
    isAddChild.value = false;
    Object.assign(formState, {
      id: record.id,
      name: record.name,
      code: record.code,
      description: record.description || '',
      leader: record.leader || '',
      deputyLeader: record.deputyLeader || '',
      parentId: record.parentId || null,
      headcountLimit: record.headcountLimit,
      levelType: record.levelType,
      enabled: record.enabled,
    });
  } else {
    isEdit.value = false;
    isAddChild.value = parentId !== undefined;
    formState.id = undefined;
    formState.name = '';
    formState.code = '';
    formState.description = '';
    formState.leader = '';
    formState.deputyLeader = '';
    formState.parentId = parentId !== undefined ? parentId : null;
    formState.headcountLimit = 10;
    formState.levelType = undefined;
    formState.enabled = true;
  }
  modalVisible.value = true;
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
  } catch (e) {
    // handled
  } finally {
    submitting.value = false;
  }
};

const handleToggle = async (record: Department) => {
  await store.toggleEnabled(record.id as number, !record.enabled);
  if (selectedKeys.value.includes(record.id as number)) {
    await loadDetail(record.id as number);
  }
};

const handleDelete = async (id: number) => {
  try {
    await store.deleteDepartment(id);
  } catch (e) {
    // handled
  }
};

const handleSearch = () => {
  if (searchKeyword.value) {
    message.success('搜索完成');
  }
};

const showSnapshotModal = () => {
  store.fetchSnapshots();
  snapshotModalVisible.value = true;
};

const handleCreateSnapshot = async () => {
  if (!newSnapshotName.value.trim()) {
    message.warning('请输入快照名称');
    return;
  }
  await store.createSnapshot(newSnapshotName.value.trim(), newSnapshotDesc.value.trim() || undefined);
  newSnapshotName.value = '';
  newSnapshotDesc.value = '';
};

const handlePreviewSnapshot = async (record: any) => {
  currentPreviewSnapshot.value = record;
  previewSnapshotData.value = await store.restoreSnapshot(record.id);
  previewModalVisible.value = true;
};
</script>

<style scoped>
.org-page {
  min-height: 100vh;
  padding: 32px 24px;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4ecf7 100%);
}

.org-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding: 0 8px;
}

.title-group .page-title {
  font-size: 28px;
  font-weight: 700;
  margin: 0;
  background: linear-gradient(135deg, #2c3e50 0%, #3498db 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.title-group .subtitle {
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

.org-content {
  display: flex;
  gap: 20px;
  height: calc(100vh - 160px);
  min-height: 600px;
}

.tree-panel {
  width: 340px;
  flex-shrink: 0;
}

.tree-card {
  height: 100%;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
  overflow: hidden;
}

.tree-card :deep(.ant-card-body) {
  padding: 16px 8px;
  height: 100%;
  overflow-y: auto;
}

.tree-node {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  padding: 4px 8px;
  border-radius: 6px;
  transition: background-color 0.2s;
}

.tree-node:hover {
  background-color: #f0f5ff;
}

.node-name {
  font-size: 14px;
  font-weight: 500;
  color: #2c3e50;
  display: flex;
  align-items: center;
  gap: 6px;
}

.node-name.disabled {
  color: #bfbfbf;
  text-decoration: line-through;
}

.level-icon {
  color: #1890ff;
  font-size: 14px;
}

.detail-panel {
  flex: 1;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
  padding: 28px;
  overflow-y: auto;
}

.detail-header {
  padding-bottom: 24px;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 20px;
}

.dept-title-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.dept-name {
  font-size: 24px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 10px;
}

.level-icon-lg {
  color: #1890ff;
  font-size: 24px;
}

.dept-code {
  color: #8c8c8c;
  font-size: 13px;
  background: #f5f5f5;
  padding: 2px 10px;
  border-radius: 10px;
}

.dept-desc {
  color: #595959;
  margin: 8px 0 20px 0;
  font-size: 14px;
}

.dept-info-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.info-card {
  background: linear-gradient(135deg, #f8faff 0%, #f0f5ff 100%);
  border-radius: 12px;
  padding: 16px;
  border: 1px solid #e6efff;
}

.info-label {
  font-size: 12px;
  color: #8c8c8c;
  margin-bottom: 8px;
}

.info-value {
  font-size: 15px;
  font-weight: 600;
  color: #2c3e50;
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.info-value .danger {
  color: #ff4d4f;
  font-weight: 700;
}

.info-value .divider {
  color: #bfbfbf;
  margin: 0 2px;
}

.info-icon {
  color: #1890ff;
}

.detail-actions {
  margin-top: 16px;
}

.detail-tabs {
  margin-top: 8px;
}

.history-item {
  margin-bottom: 4px;
}

.history-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 6px;
}

.history-type {
  font-weight: 600;
  color: #2c3e50;
}

.history-time {
  font-size: 12px;
  color: #8c8c8c;
}

.history-content {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.arrow-icon {
  color: #1890ff;
}

.history-operator {
  font-size: 12px;
  color: #8c8c8c;
}

.sub-dept-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 16px;
}

.sub-dept-card {
  background: linear-gradient(135deg, #fafbfc 0%, #f5f7fa 100%);
  border: 1px solid #e8e8e8;
  border-radius: 12px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.sub-dept-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.08);
  border-color: #1890ff;
}

.sub-dept-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.sub-dept-icon {
  color: #1890ff;
  font-size: 16px;
}

.sub-dept-name {
  font-weight: 600;
  color: #2c3e50;
  font-size: 15px;
  flex: 1;
}

.sub-dept-info {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: #595959;
  margin-bottom: 6px;
}

.sub-dept-code {
  font-size: 12px;
  color: #8c8c8c;
  background: #fff;
  padding: 2px 8px;
  border-radius: 8px;
  display: inline-block;
}

.snapshot-toolbar {
  margin-bottom: 16px;
  display: flex;
  align-items: center;
}

.preview-header {
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.preview-title {
  font-size: 16px;
  font-weight: 600;
  color: #2c3e50;
  margin-bottom: 8px;
}

.preview-meta {
  display: flex;
  gap: 20px;
  font-size: 13px;
  color: #8c8c8c;
}

@media (max-width: 1200px) {
  .dept-info-cards {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 992px) {
  .org-content {
    flex-direction: column;
    height: auto;
  }
  .tree-panel {
    width: 100%;
  }
  .tree-card {
    max-height: 400px;
  }
}
</style>
