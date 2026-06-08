<template>
  <div class="page-container">
    <div class="content-wrapper">
      <a-card class="main-card" :bordered="false">
        <div class="header-section">
          <div class="title-group">
            <h1 class="page-title">操作审计日志</h1>
            <p class="subtitle">Audit Logs</p>
          </div>
        </div>

        <a-form layout="inline" class="filter-form" @finish="handleSearch">
          <a-form-item label="目标模块">
            <a-select
              v-model:value="filterModule"
              allow-clear
              placeholder="全部模块"
              style="width: 160px"
            >
              <a-select-option value="EMPLOYEE">员工管理</a-select-option>
              <a-select-option value="DEPARTMENT">部门管理</a-select-option>
              <a-select-option value="SALARY">薪资管理</a-select-option>
              <a-select-option value="ATTENDANCE">考勤管理</a-select-option>
              <a-select-option value="LEAVE">请假管理</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="操作类型">
            <a-select
              v-model:value="filterOperationType"
              allow-clear
              placeholder="全部类型"
              style="width: 140px"
            >
              <a-select-option :value="1">新增</a-select-option>
              <a-select-option :value="2">修改</a-select-option>
              <a-select-option :value="3">删除</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="操作人">
            <a-input
              v-model:value="filterOperatorName"
              placeholder="请输入操作人姓名"
              style="width: 160px"
              allow-clear
            />
          </a-form-item>
          <a-form-item label="关键字">
            <a-input
              v-model:value="filterKeyword"
              placeholder="搜索记录名称/内容"
              style="width: 180px"
              allow-clear
            />
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
          :scroll="{ x: 1600 }"
          @change="handleTableChange"
          :expandable="{ expandedRowRender, expandedRowKeys }"
          @expand="handleExpand"
        >
          <template #bodyCell="{ column, record, index }">
            <template v-if="column.key === 'index'">
              <span>{{ (pageNum - 1) * pageSize + index + 1 }}</span>
            </template>
            <template v-if="column.key === 'operationType'">
              <a-tag :color="getOperationTypeColor(record.operationType)">
                {{ getOperationTypeText(record.operationType) }}
              </a-tag>
            </template>
            <template v-if="column.key === 'targetModule'">
              <a-tag color="blue">
                {{ record.targetModuleName }}
              </a-tag>
            </template>
            <template v-if="column.key === 'operatorName'">
              <a class="operator-link" @click="viewOperatorTrail(record)">
                {{ record.operatorName || record.operatorUsername || '-' }}
              </a>
              <div class="operator-role" v-if="record.operatorRoleName">
                {{ record.operatorRoleName }}
              </div>
            </template>
            <template v-if="column.key === 'targetRecordName'">
              <span class="record-name">{{ record.targetRecordName || '-' }}</span>
              <span class="record-id" v-if="record.targetRecordId">
                ID: {{ record.targetRecordId }}
              </span>
            </template>
            <template v-if="column.key === 'requestInfo'">
              <div>{{ record.requestIp }}</div>
              <div class="ua-info">
                {{ record.browser }} / {{ record.os }}
              </div>
            </template>
            <template v-if="column.key === 'operationTime'">
              <span>{{ record.operationTime }}</span>
            </template>
            <template v-if="column.key === 'action'">
              <a-button type="link" size="small" @click="showDetail(record)">
                查看变更
              </a-button>
              <a-button type="link" size="small" @click="viewOperatorTrail(record)">
                追溯操作
              </a-button>
            </template>
          </template>
          <template #emptyText>
            <a-empty description="暂无操作审计日志数据" />
          </template>
        </a-table>
      </a-card>
    </div>

    <a-modal
      v-model:open="detailModalVisible"
      title="操作详情 - 字段变更对比"
      :width="900"
      :footer="null"
      destroy-on-close
    >
      <div v-if="currentDetail" class="detail-container">
        <a-descriptions bordered size="small" :column="2" class="basic-info">
          <a-descriptions-item label="操作人">
            {{ currentDetail.log.operatorName || currentDetail.log.operatorUsername || '-' }}
            <a-tag v-if="currentDetail.log.operatorRoleName" color="blue" style="margin-left: 8px">
              {{ currentDetail.log.operatorRoleName }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="操作时间">
            {{ currentDetail.log.operationTime }}
          </a-descriptions-item>
          <a-descriptions-item label="操作类型">
            <a-tag :color="getOperationTypeColor(currentDetail.log.operationType)">
              {{ getOperationTypeText(currentDetail.log.operationType) }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="目标模块">
            {{ currentDetail.log.targetModuleName }}
          </a-descriptions-item>
          <a-descriptions-item label="目标记录">
            {{ currentDetail.log.targetRecordName || '-' }}
            <span class="record-id-inline" v-if="currentDetail.log.targetRecordId">
              (ID: {{ currentDetail.log.targetRecordId }})
            </span>
          </a-descriptions-item>
          <a-descriptions-item label="请求来源">
            {{ currentDetail.log.requestIp }} / {{ currentDetail.log.browser }}
          </a-descriptions-item>
        </a-descriptions>

        <div class="diff-section-title">
          <template #icon><DiffOutlined /></template>
          字段变更对比
          <span class="diff-count" v-if="currentDetail.diffs?.length">
            （共 {{ currentDetail.diffs.length }} 处变更）
          </span>
        </div>

        <div v-if="!currentDetail.diffs || currentDetail.diffs.length === 0" class="no-diff">
          <a-empty description="无字段变更数据" />
        </div>

        <div v-else class="diff-table-wrapper">
          <a-table
            :columns="diffColumns"
            :data-source="currentDetail.diffs"
            :pagination="false"
            row-key="fieldName"
            size="small"
            class="diff-table"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'fieldLabel'">
                <span class="diff-field-label">{{ record.fieldLabel }}</span>
                <span class="diff-field-name">({{ record.fieldName }})</span>
              </template>
              <template v-if="column.key === 'diffType'">
                <a-tag :color="getDiffTypeColor(record.diffType)">
                  {{ getDiffTypeText(record.diffType) }}
                </a-tag>
              </template>
              <template v-if="column.key === 'oldValue'">
                <div v-if="record.diffType === 'ADD'" class="diff-empty">—</div>
                <div v-else class="diff-old-value">
                  <pre class="diff-pre">{{ formatValue(record.oldValue) }}</pre>
                </div>
              </template>
              <template v-if="column.key === 'newValue'">
                <div v-if="record.diffType === 'REMOVE'" class="diff-empty">—</div>
                <div v-else class="diff-new-value">
                  <pre class="diff-pre">{{ formatValue(record.newValue) }}</pre>
                </div>
              </template>
            </template>
          </a-table>
        </div>
      </div>
    </a-modal>

    <a-modal
      v-model:open="trailModalVisible"
      :title="trailModalTitle"
      :width="1000"
      :footer="null"
      destroy-on-close
    >
      <div v-if="trailRecords.length > 0" class="trail-container">
        <div class="timeline">
          <a-timeline mode="left">
            <a-timeline-item
              v-for="(record, idx) in trailRecords"
              :key="record.id"
              :color="getTimelineColor(record.operationType)"
            >
              <template #label>
                <span class="timeline-time">{{ record.operationTime }}</span>
              </template>
              <div class="timeline-card" @click="showDetailFromTrail(record)">
                <div class="timeline-header">
                  <a-tag :color="getOperationTypeColor(record.operationType)">
                    {{ getOperationTypeText(record.operationType) }}
                  </a-tag>
                  <span class="timeline-module">{{ record.targetModuleName }}</span>
                </div>
                <div class="timeline-title">
                  {{ record.targetRecordName || '记录ID: ' + record.targetRecordId }}
                </div>
                <div class="timeline-ip">
                  IP: {{ record.requestIp }} | {{ record.browser }} / {{ record.os }}
                </div>
              </div>
            </a-timeline-item>
          </a-timeline>
        </div>
      </div>
      <a-empty v-else description="暂无操作记录" />
    </a-modal>
  </div>
</template>

<script lang="ts" setup>
import { ref, computed, onMounted } from 'vue';
import request from '../utils/request';
import { SearchOutlined, DiffOutlined } from '@ant-design/icons-vue';

interface AuditLogRecord {
  id: number;
  operatorId: number;
  operatorName: string;
  operatorUsername: string;
  operatorRoleId: number;
  operatorRoleCode: string;
  operatorRoleName: string;
  operationType: number;
  targetModule: string;
  targetModuleName: string;
  targetRecordId: string;
  targetRecordName: string;
  beforeSnapshot: string;
  afterSnapshot: string;
  requestIp: string;
  userAgent: string;
  browser: string;
  os: string;
  device: string;
  operationTime: string;
  archived: boolean;
}

interface FieldDiffDTO {
  fieldName: string;
  fieldLabel: string;
  oldValue: any;
  newValue: any;
  diffType: string;
}

interface AuditLogDetailDTO {
  log: AuditLogRecord;
  diffs: FieldDiffDTO[];
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

const filterModule = ref<string | undefined>();
const filterOperationType = ref<number | undefined>();
const filterOperatorName = ref<string>('');
const filterKeyword = ref<string>('');
const dateRange = ref<string[]>([]);
const pageNum = ref(1);
const pageSize = ref(10);
const loading = ref(false);
const records = ref<AuditLogRecord[]>([]);
const total = ref(0);
const expandedRowKeys = ref<number[]>([]);

const detailModalVisible = ref(false);
const currentDetail = ref<AuditLogDetailDTO | null>(null);

const trailModalVisible = ref(false);
const trailModalTitle = ref('');
const trailRecords = ref<AuditLogRecord[]>([]);

const paginationConfig = computed(() => ({
  current: pageNum.value,
  pageSize: pageSize.value,
  total: total.value,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (t: number) => `共 ${t} 条记录`,
}));

const columns = [
  { title: '序号', key: 'index', width: '6%' },
  { title: '操作人', dataIndex: 'operatorName', key: 'operatorName', width: '12%' },
  { title: '操作类型', dataIndex: 'operationType', key: 'operationType', width: '8%' },
  { title: '目标模块', dataIndex: 'targetModule', key: 'targetModule', width: '10%' },
  { title: '目标记录', dataIndex: 'targetRecordName', key: 'targetRecordName', width: '18%' },
  { title: '请求信息', key: 'requestInfo', width: '18%' },
  { title: '操作时间', dataIndex: 'operationTime', key: 'operationTime', width: '16%' },
  { title: '操作', key: 'action', width: '12%', fixed: 'right' as const },
];

const diffColumns = [
  { title: '字段', dataIndex: 'fieldLabel', key: 'fieldLabel', width: '22%' },
  { title: '变更类型', dataIndex: 'diffType', key: 'diffType', width: '10%' },
  { title: '变更前', dataIndex: 'oldValue', key: 'oldValue', width: '30%' },
  { title: '变更后', dataIndex: 'newValue', key: 'newValue', width: '28%' },
];

const getOperationTypeText = (type?: number) => {
  switch (type) {
    case 1: return '新增';
    case 2: return '修改';
    case 3: return '删除';
    default: return type || '-';
  }
};

const getOperationTypeColor = (type?: number) => {
  switch (type) {
    case 1: return 'green';
    case 2: return 'orange';
    case 3: return 'red';
    default: return 'default';
  }
};

const getTimelineColor = (type?: number) => {
  switch (type) {
    case 1: return 'green';
    case 2: return 'orange';
    case 3: return 'red';
    default: return 'blue';
  }
};

const getDiffTypeText = (type?: string) => {
  switch (type) {
    case 'ADD': return '新增';
    case 'MODIFY': return '修改';
    case 'REMOVE': return '删除';
    default: return type || '-';
  }
};

const getDiffTypeColor = (type?: string) => {
  switch (type) {
    case 'ADD': return 'green';
    case 'MODIFY': return 'orange';
    case 'REMOVE': return 'red';
    default: return 'default';
  }
};

const formatValue = (val: any) => {
  if (val === null || val === undefined) return 'null';
  if (typeof val === 'object') return JSON.stringify(val, null, 2);
  return String(val);
};

const getQueryParams = () => {
  const body: Record<string, any> = {
    pageNum: pageNum.value,
    pageSize: pageSize.value,
  };
  if (filterModule.value) {
    body.targetModule = filterModule.value;
  }
  if (filterOperationType.value !== undefined) {
    body.operationType = filterOperationType.value;
  }
  if (filterOperatorName.value) {
    body.operatorName = filterOperatorName.value;
  }
  if (filterKeyword.value) {
    body.keyword = filterKeyword.value;
  }
  if (dateRange.value?.[0]) {
    body.startTime = dateRange.value[0];
  }
  if (dateRange.value?.[1]) {
    body.endTime = dateRange.value[1];
  }
  return body;
};

const fetchData = async () => {
  loading.value = true;
  try {
    const res = await request.post<any, Result<PageResult<AuditLogRecord>>>('/audit-logs/page', getQueryParams());
    records.value = res.data.records || [];
    total.value = res.data.total || 0;
  } catch (e) {
    console.error('获取操作审计日志失败', e);
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
  filterModule.value = undefined;
  filterOperationType.value = undefined;
  filterOperatorName.value = '';
  filterKeyword.value = '';
  dateRange.value = [];
  pageNum.value = 1;
  fetchData();
};

const showDetail = async (record: AuditLogRecord) => {
  try {
    const res = await request.get<any, Result<AuditLogDetailDTO>>(`/audit-logs/${record.id}`);
    currentDetail.value = res.data;
    detailModalVisible.value = true;
  } catch (e) {
    console.error('获取日志详情失败', e);
  }
};

const showDetailFromTrail = async (record: AuditLogRecord) => {
  try {
    const res = await request.get<any, Result<AuditLogDetailDTO>>(`/audit-logs/${record.id}`);
    currentDetail.value = res.data;
    detailModalVisible.value = true;
  } catch (e) {
    console.error('获取日志详情失败', e);
  }
};

const viewOperatorTrail = async (record: AuditLogRecord) => {
  if (!record.operatorId) return;
  trailModalTitle.value = `${record.operatorName || record.operatorUsername} 的操作历史`;
  try {
    const res = await request.get<any, Result<AuditLogRecord[]>>(`/audit-logs/operator/${record.operatorId}`);
    trailRecords.value = res.data || [];
    trailModalVisible.value = true;
  } catch (e) {
    console.error('获取操作人操作历史失败', e);
  }
};

const handleExpand = (expandedKeys: number[]) => {
  expandedRowKeys.value = expandedKeys;
};

const expandedRowRender = (record: AuditLogRecord) => {
  return null;
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
  max-width: 1600px;
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

.operator-link {
  color: #1890ff;
  cursor: pointer;
  font-weight: 500;
}

.operator-link:hover {
  color: #40a9ff;
  text-decoration: underline;
}

.operator-role {
  font-size: 12px;
  color: #8c8c8c;
  margin-top: 2px;
}

.record-name {
  font-weight: 500;
  color: #2c3e50;
}

.record-id {
  display: block;
  font-size: 12px;
  color: #8c8c8c;
  margin-top: 2px;
}

.ua-info {
  font-size: 12px;
  color: #8c8c8c;
  margin-top: 2px;
}

.detail-container {
  padding: 8px 0;
}

.basic-info {
  margin-bottom: 24px;
}

.record-id-inline {
  color: #8c8c8c;
  font-size: 12px;
}

.diff-section-title {
  font-size: 16px;
  font-weight: 600;
  margin: 20px 0 12px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding-left: 12px;
  border-left: 3px solid #1890ff;
}

.diff-count {
  font-size: 13px;
  font-weight: 400;
  color: #8c8c8c;
}

.no-diff {
  padding: 40px 0;
}

.diff-table-wrapper {
  border-radius: 8px;
  overflow: hidden;
}

.diff-field-label {
  font-weight: 500;
  color: #2c3e50;
}

.diff-field-name {
  font-size: 12px;
  color: #8c8c8c;
  margin-left: 4px;
}

.diff-old-value {
  background-color: #fff1f0;
  border: 1px solid #ffa39e;
  border-radius: 4px;
  padding: 8px;
  max-height: 120px;
  overflow: auto;
}

.diff-new-value {
  background-color: #f6ffed;
  border: 1px solid #b7eb8f;
  border-radius: 4px;
  padding: 8px;
  max-height: 120px;
  overflow: auto;
}

.diff-empty {
  color: #bfbfbf;
  text-align: center;
  padding: 8px;
}

.diff-pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
  font-family: 'Courier New', Courier, monospace;
  font-size: 12px;
  line-height: 1.5;
}

.trail-container {
  padding: 8px 0;
}

.timeline-time {
  font-size: 13px;
  color: #595959;
  font-weight: 500;
}

.timeline-card {
  background: #fafafa;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  padding: 12px 16px;
  cursor: pointer;
  transition: all 0.2s;
}

.timeline-card:hover {
  background: #e6f7ff;
  border-color: #91d5ff;
  transform: translateX(4px);
}

.timeline-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.timeline-module {
  font-size: 13px;
  color: #595959;
}

.timeline-title {
  font-size: 14px;
  font-weight: 500;
  color: #2c3e50;
  margin-bottom: 4px;
}

.timeline-ip {
  font-size: 12px;
  color: #8c8c8c;
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
