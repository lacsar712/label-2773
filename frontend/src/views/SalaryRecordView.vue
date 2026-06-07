<template>
  <div class="page-container">
    <div class="content-wrapper">
      <a-card class="main-card" :bordered="false">
        <div class="header-section">
          <div class="title-group">
            <h1 class="page-title">薪资管理</h1>
            <p class="subtitle">Salary Management</p>
          </div>
          <div class="header-actions">
            <a-space>
              <a-button @click="showTemplatePage">
                <template #icon><FileTextOutlined /></template>
                薪资模板
              </a-button>
              <a-button type="primary" @click="showGenerateModal">
                <template #icon><PlusOutlined /></template>
                批量生成薪资
              </a-button>
            </a-space>
          </div>
        </div>

        <div class="filter-section">
          <a-form layout="inline" :model="queryForm">
            <a-form-item label="年份">
              <a-select v-model:value="queryForm.salaryYear" style="width: 120px">
                <a-select-option v-for="y in yearOptions" :key="y" :value="y">{{ y }}年</a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item label="月份">
              <a-select v-model:value="queryForm.salaryMonth" style="width: 120px">
                <a-select-option v-for="m in 12" :key="m" :value="m">{{ m }}月</a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item label="部门">
              <a-tree-select
                v-model:value="queryForm.departmentId"
                :tree-data="deptFilterOptions"
                :field-names="{ label: 'name', value: 'id', children: 'children' }"
                placeholder="全部部门"
                allow-clear
                tree-default-expand-all
                style="width: 200px"
              />
            </a-form-item>
            <a-form-item label="状态">
              <a-select v-model:value="queryForm.status" allow-clear placeholder="全部状态" style="width: 140px">
                <a-select-option :value="0">草稿</a-select-option>
                <a-select-option :value="1">已确认</a-select-option>
                <a-select-option :value="2">已发放</a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item label="关键字">
              <a-input v-model:value="queryForm.keyword" placeholder="姓名/薪资单号" allow-clear style="width: 180px" />
            </a-form-item>
            <a-form-item>
              <a-space>
                <a-button type="primary" @click="handleSearch">
                  <template #icon><SearchOutlined /></template>
                  查询
                </a-button>
                <a-button @click="handleReset">重置</a-button>
              </a-space>
            </a-form-item>
          </a-form>
        </div>

        <div class="batch-actions" v-if="selectedRowKeys.length > 0">
          <a-space>
            <span>已选择 {{ selectedRowKeys.length }} 条</span>
            <a-button type="primary" size="small" @click="handleBatchConfirm">
              <template #icon><CheckCircleOutlined /></template>
              批量确认
            </a-button>
            <a-button type="primary" size="small" danger @click="handleBatchIssue">
              <template #icon><SendOutlined /></template>
              批量发放
            </a-button>
          </a-space>
        </div>

        <a-table
          :columns="columns"
          :data-source="salaryStore.recordList"
          :loading="salaryStore.loading"
          row-key="id"
          :pagination="{
            current: queryForm.pageNum,
            pageSize: queryForm.pageSize,
            total: salaryStore.recordListTotal,
            showSizeChanger: true,
            showQuickJumper: true,
            onChange: handlePageChange,
          }"
          class="modern-table"
          :scroll="{ x: 1500 }"
          :row-selection="{ selectedRowKeys, onChange: onSelectionChange, getCheckboxProps: (record: any) => ({ disabled: record.status === 2 }) }"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'employee'">
              <div class="user-info">
                <a-avatar :style="{ backgroundColor: '#7265e6', verticalAlign: 'middle' }" size="small">
                  {{ (record.employeeName || '?').charAt(0) }}
                </a-avatar>
                <span class="user-name">{{ record.employeeName }}</span>
              </div>
            </template>
            <template v-if="column.key === 'period'">
              <span>{{ record.salaryYear }}年{{ record.salaryMonth }}月</span>
            </template>
            <template v-if="column.key === 'department'">
              <a-tag color="blue">{{ record.departmentName || '-' }}</a-tag>
            </template>
            <template v-if="column.key === 'grossSalary'">
              <span class="money">¥{{ formatMoney(record.grossSalary) }}</span>
            </template>
            <template v-if="column.key === 'totalDeduction'">
              <span class="money deduction">-¥{{ formatMoney(record.totalDeduction) }}</span>
            </template>
            <template v-if="column.key === 'netSalary'">
              <span class="money highlight">¥{{ formatMoney(record.netSalary) }}</span>
            </template>
            <template v-if="column.key === 'status'">
              <a-tag :color="getStatusColor(record.status)">{{ record.statusName }}</a-tag>
            </template>
            <template v-if="column.key === 'action'">
              <a-space size="small">
                <a-button type="link" size="small" @click="showDetail(record)">详情</a-button>
                <a-button type="link" size="small" v-if="record.status !== 2" @click="showAdjustModal(record)">
                  调整
                </a-button>
                <a-divider type="vertical" v-if="record.status !== 2" />
                <a-button type="link" size="small" v-if="record.status === 0" @click="handleConfirm(record.id)">
                  确认
                </a-button>
                <a-button type="link" size="small" v-if="record.status === 1" @click="handleIssue(record.id)">
                  发放
                </a-button>
              </a-space>
            </template>
          </template>
        </a-table>
      </a-card>
    </div>

    <a-modal v-model:open="generateVisible" title="批量生成薪资" width="560px" @ok="handleGenerate" ok-text="生成" cancel-text="取消">
      <a-form :model="generateForm" layout="vertical">
        <a-form-item label="薪资月份" :rules="[{ required: true, message: '请选择年份和月份' }]">
          <a-space>
            <a-select v-model:value="generateForm.salaryYear" style="width: 160px">
              <a-select-option v-for="y in yearOptions" :key="y" :value="y">{{ y }}年</a-select-option>
            </a-select>
            <a-select v-model:value="generateForm.salaryMonth" style="width: 120px">
              <a-select-option v-for="m in 12" :key="m" :value="m">{{ m }}月</a-select-option>
            </a-select>
          </a-space>
        </a-form-item>
        <a-form-item label="生成范围">
          <a-radio-group v-model:value="generateForm.scope">
            <a-radio :value="all">全部在职员工</a-radio>
            <a-radio :value="dept">指定部门</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="选择部门" v-if="generateForm.scope === 'dept'">
          <a-tree-select
            v-model:value="generateForm.departmentId"
            :tree-data="deptFilterOptions"
            :field-names="{ label: 'name', value: 'id', children: 'children' }"
            placeholder="选择部门"
            allow-clear
            tree-default-expand-all
            style="width: 100%"
          />
        </a-form-item>
        <a-form-item label="默认模板">
          <a-select v-model:value="generateForm.defaultTemplateId" allow-clear placeholder="选择薪资模板（可选）" style="width: 100%">
            <a-select-option v-for="t in salaryStore.enabledTemplates" :key="t.id" :value="t.id">
              {{ t.templateName }} ({{ t.jobLevel }})
            </a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>

    <a-drawer v-model:open="detailVisible" title="薪资详情" width="640px">
      <div v-if="salaryStore.currentRecord" class="salary-detail">
        <div class="detail-header">
          <div>
            <h3 class="detail-title">{{ salaryStore.currentRecord.employeeName }} - {{ salaryStore.currentRecord.salaryYear }}年{{ salaryStore.currentRecord.salaryMonth }}月工资单</h3>
            <p class="detail-sub">薪资单号：{{ salaryStore.currentRecord.recordNo }}</p>
          </div>
          <a-tag :color="getStatusColor(salaryStore.currentRecord.status)" style="font-size: 14px; padding: 4px 12px">
            {{ salaryStore.currentRecord.statusName }}
          </a-tag>
        </div>

        <a-descriptions title="收入明细" bordered size="small" :column="2">
          <a-descriptions-item label="基本工资">¥{{ formatMoney(salaryStore.currentRecord.baseSalary) }}</a-descriptions-item>
          <a-descriptions-item label="岗位津贴">¥{{ formatMoney(salaryStore.currentRecord.postAllowance) }}</a-descriptions-item>
          <a-descriptions-item label="绩效系数">{{ salaryStore.currentRecord.performanceCoefficient?.toFixed(2) }}</a-descriptions-item>
          <a-descriptions-item label="绩效奖金">¥{{ formatMoney(salaryStore.currentRecord.performanceBonus) }}</a-descriptions-item>
          <a-descriptions-item label="加班费">¥{{ formatMoney(salaryStore.currentRecord.overtimePay) }}</a-descriptions-item>
          <a-descriptions-item label="其他补贴">¥{{ formatMoney(salaryStore.currentRecord.otherAllowance) }}</a-descriptions-item>
          <a-descriptions-item label="应发合计" :span="2">
            <span class="money highlight large">¥{{ formatMoney(salaryStore.currentRecord.grossSalary) }}</span>
          </a-descriptions-item>
        </a-descriptions>

        <a-descriptions title="扣款明细" bordered size="small" :column="2" style="margin-top: 16px">
          <a-descriptions-item label="社保(个人)">¥{{ formatMoney(salaryStore.currentRecord.socialInsurancePersonal) }}</a-descriptions-item>
          <a-descriptions-item label="公积金(个人)">¥{{ formatMoney(salaryStore.currentRecord.housingFundPersonal) }}</a-descriptions-item>
          <a-descriptions-item label="个人所得税">¥{{ formatMoney(salaryStore.currentRecord.incomeTax) }}</a-descriptions-item>
          <a-descriptions-item label="其他扣款">¥{{ formatMoney(salaryStore.currentRecord.otherDeduction) }}</a-descriptions-item>
          <a-descriptions-item label="扣款合计" :span="2">
            <span class="money deduction large">-¥{{ formatMoney(salaryStore.currentRecord.totalDeduction) }}</span>
          </a-descriptions-item>
        </a-descriptions>

        <a-descriptions title="公司成本" bordered size="small" :column="2" style="margin-top: 16px">
          <a-descriptions-item label="社保(公司)">¥{{ formatMoney(salaryStore.currentRecord.socialInsuranceCompany) }}</a-descriptions-item>
          <a-descriptions-item label="公积金(公司)">¥{{ formatMoney(salaryStore.currentRecord.housingFundCompany) }}</a-descriptions-item>
          <a-descriptions-item label="公司总成本" :span="2">
            <span class="money large">¥{{ formatMoney(salaryStore.currentRecord.totalCompanyCost) }}</span>
          </a-descriptions-item>
        </a-descriptions>

        <div class="net-salary-box">
          <span class="net-label">实发工资</span>
          <span class="net-value">¥{{ formatMoney(salaryStore.currentRecord.netSalary) }}</span>
        </div>

        <div v-if="salaryStore.currentRecord.adjustLogs && salaryStore.currentRecord.adjustLogs.length > 0" class="adjust-log-section">
          <h4>调整记录</h4>
          <a-table :columns="adjustLogColumns" :data-source="salaryStore.currentRecord.adjustLogs" :pagination="false" size="small" row-key="id">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'change'">
                <span>¥{{ formatMoney(record.oldValue) }} → <span class="money highlight">¥{{ formatMoney(record.newValue) }}</span></span>
              </template>
            </template>
          </a-table>
        </div>
      </div>
    </a-drawer>

    <a-modal v-model:open="adjustVisible" title="调整薪资项" width="500px" @ok="handleAdjust" ok-text="确认调整" cancel-text="取消">
      <a-form :model="adjustForm" layout="vertical" v-if="adjustingRecord">
        <a-alert type="info" show-icon style="margin-bottom: 16px">
          <template #message>
            调整员工：{{ adjustingRecord.employeeName }}（{{ adjustingRecord.salaryYear }}年{{ adjustingRecord.salaryMonth }}月）
          </template>
        </a-alert>
        <a-form-item label="调整项目" :rules="[{ required: true, message: '请选择调整项目' }]">
          <a-select v-model:value="adjustForm.fieldName" placeholder="选择调整项目" style="width: 100%" @change="onFieldChange">
            <a-select-option v-for="f in salaryStore.fieldLabels" :key="f.field" :value="f.field">
              {{ f.label }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="调整前值">
          <a-input-number v-model:value="adjustForm.oldValue" style="width: 100%" :precision="2" disabled />
        </a-form-item>
        <a-form-item label="调整后值" :rules="[{ required: true, message: '请输入调整后值' }]">
          <a-input-number v-model:value="adjustForm.newValue" style="width: 100%" :precision="2" :min="0" />
        </a-form-item>
        <a-form-item label="调整原因">
          <a-textarea v-model:value="adjustForm.adjustReason" :rows="3" placeholder="请输入调整原因（建议留痕）" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue';
import { useRouter } from 'vue-router';
import { useSalaryStore, type SalaryRecord } from '../stores/salary';
import { useDepartmentStore } from '../stores/department';
import { message, Modal } from 'ant-design-vue';
import {
  PlusOutlined,
  SearchOutlined,
  FileTextOutlined,
  CheckCircleOutlined,
  SendOutlined,
} from '@ant-design/icons-vue';

const router = useRouter();
const salaryStore = useSalaryStore();
const deptStore = useDepartmentStore();

const currentYear = new Date().getFullYear();
const yearOptions = computed(() => [currentYear, currentYear - 1, currentYear - 2, currentYear + 1]);

const queryForm = reactive({
  salaryYear: currentYear,
  salaryMonth: new Date().getMonth() + 1,
  departmentId: undefined as number | undefined,
  status: undefined as number | undefined,
  keyword: '',
  pageNum: 1,
  pageSize: 10,
});

const generateForm = reactive({
  salaryYear: currentYear,
  salaryMonth: new Date().getMonth() + 1,
  scope: 'all' as 'all' | 'dept',
  departmentId: undefined as number | undefined,
  defaultTemplateId: undefined as number | undefined,
});

const adjustForm = reactive({
  salaryRecordId: undefined as number | undefined,
  fieldName: '',
  fieldLabel: '',
  oldValue: undefined as number | undefined,
  newValue: undefined as number | undefined,
  adjustReason: '',
});

const adjustingRecord = ref<SalaryRecord | null>(null);
const generateVisible = ref(false);
const detailVisible = ref(false);
const adjustVisible = ref(false);
const selectedRowKeys = ref<number[]>([]);

const deptFilterOptions = computed(() => deptStore.departmentsTree);

const columns = [
  { title: '薪资单号', dataIndex: 'recordNo', key: 'recordNo', width: 160 },
  { title: '员工', dataIndex: 'employeeName', key: 'employee', width: 140 },
  { title: '部门', dataIndex: 'departmentName', key: 'department', width: 140 },
  { title: '薪资周期', key: 'period', width: 110 },
  { title: '应发', dataIndex: 'grossSalary', key: 'grossSalary', width: 120 },
  { title: '扣款', dataIndex: 'totalDeduction', key: 'totalDeduction', width: 120 },
  { title: '实发', dataIndex: 'netSalary', key: 'netSalary', width: 130 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '操作', key: 'action', width: 200, fixed: 'right' as const },
];

const adjustLogColumns = [
  { title: '调整时间', dataIndex: 'createTime', key: 'createTime', width: 170 },
  { title: '调整项目', dataIndex: 'fieldLabel', key: 'fieldLabel' },
  { title: '变更', key: 'change' },
  { title: '原因', dataIndex: 'adjustReason', key: 'adjustReason' },
  { title: '操作人', dataIndex: 'operatorName', key: 'operatorName', width: 100 },
];

const formatMoney = (v?: number) => (v ?? 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

const getStatusColor = (s?: number) => {
  if (s === 0) return 'default';
  if (s === 1) return 'processing';
  if (s === 2) return 'success';
  return 'default';
};

function handleSearch() {
  queryForm.pageNum = 1;
  loadRecords();
}

function handleReset() {
  queryForm.salaryYear = currentYear;
  queryForm.salaryMonth = new Date().getMonth() + 1;
  queryForm.departmentId = undefined;
  queryForm.status = undefined;
  queryForm.keyword = '';
  queryForm.pageNum = 1;
  handleSearch();
}

function handlePageChange(page: number, pageSize: number) {
  queryForm.pageNum = page;
  queryForm.pageSize = pageSize;
  loadRecords();
}

function onSelectionChange(keys: number[]) {
  selectedRowKeys.value = keys;
}

async function loadRecords() {
  await salaryStore.fetchRecords({ ...queryForm });
}

function showGenerateModal() {
  generateVisible.value = true;
}

async function handleGenerate() {
  const result = await salaryStore.batchGenerate({
    salaryYear: generateForm.salaryYear,
    salaryMonth: generateForm.salaryMonth,
    departmentId: generateForm.scope === 'dept' ? generateForm.departmentId : undefined,
    defaultTemplateId: generateForm.defaultTemplateId,
  });
  if (result && result.length > 0) {
    generateVisible.value = false;
    loadRecords();
  }
}

async function showDetail(record: SalaryRecord) {
  await salaryStore.fetchRecordDetail(record.id!);
  detailVisible.value = true;
}

async function showAdjustModal(record: SalaryRecord) {
  adjustingRecord.value = record;
  adjustForm.salaryRecordId = record.id;
  adjustForm.fieldName = '';
  adjustForm.fieldLabel = '';
  adjustForm.oldValue = undefined;
  adjustForm.newValue = undefined;
  adjustForm.adjustReason = '';
  await salaryStore.fetchFieldLabels();
  adjustVisible.value = true;
}

function onFieldChange(field: string) {
  const label = salaryStore.fieldLabels.find((f) => f.field === field);
  adjustForm.fieldLabel = label?.label || field;
  const oldVal = getRecordFieldValue(adjustingRecord.value!, field);
  adjustForm.oldValue = oldVal;
  adjustForm.newValue = oldVal;
}

function getRecordFieldValue(record: SalaryRecord, field: string): number | undefined {
  return (record as any)[field];
}

async function handleAdjust() {
  if (!adjustForm.fieldName || adjustForm.newValue === undefined) {
    message.warning('请填写完整信息');
    return;
  }
  const result = await salaryStore.adjustField({ ...adjustForm });
  if (result) {
    adjustVisible.value = false;
    loadRecords();
  }
}

async function handleConfirm(id: number) {
  Modal.confirm({
    title: '确认薪资单',
    content: '确认后将进入"已确认"状态，可以批量发放。是否确认？',
    okText: '确认',
    cancelText: '取消',
    onOk: async () => {
      const r = await salaryStore.confirmRecord(id);
      if (r) loadRecords();
    },
  });
}

async function handleBatchConfirm() {
  Modal.confirm({
    title: '批量确认',
    content: `将确认选中的 ${selectedRowKeys.value.length} 条薪资单，是否继续？`,
    okText: '确认',
    cancelText: '取消',
    onOk: async () => {
      await salaryStore.batchConfirm(selectedRowKeys.value);
      selectedRowKeys.value = [];
      loadRecords();
    },
  });
}

async function handleIssue(id: number) {
  Modal.confirm({
    title: '发放薪资单',
    content: '发放后薪资单将锁定不可编辑，员工可查看。是否发放？',
    okText: '确认发放',
    okType: 'danger' as const,
    cancelText: '取消',
    onOk: async () => {
      const r = await salaryStore.issueRecord(id);
      if (r) loadRecords();
    },
  });
}

async function handleBatchIssue() {
  Modal.confirm({
    title: '批量发放',
    content: `将发放选中的 ${selectedRowKeys.value.length} 条薪资单，发放后将锁定不可编辑。是否继续？`,
    okText: '确认发放',
    okType: 'danger' as const,
    cancelText: '取消',
    onOk: async () => {
      await salaryStore.batchIssue(selectedRowKeys.value);
      selectedRowKeys.value = [];
      loadRecords();
    },
  });
}

function showTemplatePage() {
  router.push('/salary/templates');
}

onMounted(async () => {
  await deptStore.refreshAll();
  await salaryStore.fetchEnabledTemplates();
  await salaryStore.fetchFieldLabels();
  loadRecords();
});
</script>

<style scoped lang="scss">
.page-container {
  min-height: 100%;
}
.content-wrapper {
  max-width: 1600px;
  margin: 0 auto;
}
.main-card {
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}
.header-section {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 20px;
  .title-group {
    .page-title {
      margin: 0;
      font-size: 22px;
      font-weight: 700;
      color: #2c3e50;
      background: linear-gradient(135deg, #2c3e50 0%, #667eea 100%);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
    }
    .subtitle {
      margin: 4px 0 0 0;
      color: #8c8c8c;
      font-size: 12px;
      letter-spacing: 1px;
    }
  }
}
.filter-section {
  margin-bottom: 16px;
  padding: 16px;
  background: #fafafa;
  border-radius: 8px;
}
.batch-actions {
  margin-bottom: 12px;
  padding: 8px 16px;
  background: #e6f4ff;
  border-radius: 6px;
}
.modern-table {
  :deep(.ant-table-thead > tr > th) {
    background: #fafafa;
    font-weight: 600;
  }
}
.user-info {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  .user-name {
    font-weight: 500;
  }
}
.money {
  font-family: 'SF Mono', Consolas, monospace;
  font-weight: 500;
  &.deduction { color: #f5222d; }
  &.highlight { color: #52c41a; font-weight: 600; }
  &.large { font-size: 18px; }
}
.salary-detail {
  .detail-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 20px;
    padding-bottom: 16px;
    border-bottom: 1px solid #f0f0f0;
    .detail-title { margin: 0; font-size: 18px; }
    .detail-sub { margin: 4px 0 0; color: #8c8c8c; font-size: 13px; }
  }
  .net-salary-box {
    margin-top: 20px;
    padding: 20px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border-radius: 10px;
    color: white;
    display: flex;
    justify-content: space-between;
    align-items: center;
    .net-label { font-size: 14px; opacity: 0.9; }
    .net-value { font-size: 28px; font-weight: 700; }
  }
  .adjust-log-section {
    margin-top: 20px;
    h4 { margin-bottom: 12px; }
  }
}
</style>
