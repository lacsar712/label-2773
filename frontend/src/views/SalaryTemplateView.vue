<template>
  <div class="page-container">
    <div class="content-wrapper">
      <a-card class="main-card" :bordered="false">
        <div class="header-section">
          <div class="title-group">
            <h1 class="page-title">薪资模板管理</h1>
            <p class="subtitle">Salary Templates</p>
          </div>
          <div class="header-actions">
            <a-space>
              <a-input-search v-model:value="keyword" placeholder="搜索模板名称/职级" style="width: 240px" @search="loadTemplates" />
              <a-button type="primary" @click="showCreateModal">
                <template #icon><PlusOutlined /></template>
                新建模板
              </a-button>
            </a-space>
          </div>
        </div>

        <a-table
          :columns="columns"
          :data-source="salaryStore.templates"
          :loading="salaryStore.loading"
          row-key="id"
          :pagination="{ pageSize: 10 }"
          class="modern-table"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'templateName'">
              <span class="tpl-name">{{ record.templateName }}</span>
            </template>
            <template v-if="column.key === 'jobLevel'">
              <a-tag color="purple">{{ record.jobLevel }}</a-tag>
            </template>
            <template v-if="column.key === 'baseSalary'">
              <span class="money">¥{{ formatMoney(record.baseSalary) }}</span>
            </template>
            <template v-if="column.key === 'performanceCoefficient'">
              <span>{{ record.performanceCoefficient?.toFixed(2) }}</span>
            </template>
            <template v-if="column.key === 'enabled'">
              <a-switch :checked="record.enabled === 1" :disabled="false" @change="handleToggle(record.id!)" />
            </template>
            <template v-if="column.key === 'action'">
              <a-space size="small">
                <a-button type="link" size="small" @click="showEditModal(record)">编辑</a-button>
                <a-button type="link" size="small" @click="showApplyModal(record)">套用</a-button>
              </a-space>
            </template>
          </template>
        </a-table>
      </a-card>
    </div>

    <a-modal
      v-model:open="formVisible"
      :title="isEdit ? '编辑薪资模板' : '新建薪资模板'"
      width="680px"
      @ok="handleSubmit"
      ok-text="保存"
      cancel-text="取消"
    >
      <a-form ref="formRef" :model="formState" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="模板名称" name="templateName" :rules="[{ required: true, message: '请输入模板名称' }]">
              <a-input v-model:value="formState.templateName" placeholder="例如：高级工程师模板" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="适用职级" name="jobLevel" :rules="[{ required: true, message: '请输入适用职级' }]">
              <a-input v-model:value="formState.jobLevel" placeholder="例如：P6" />
            </a-form-item>
          </a-col>
        </a-row>

        <a-divider orientation="left">薪资构成</a-divider>

        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="基本工资(元)" name="baseSalary" :rules="[{ required: true, message: '请输入基本工资' }]">
              <a-input-number v-model:value="formState.baseSalary" style="width: 100%" :min="0" :precision="2" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="岗位津贴(元)">
              <a-input-number v-model:value="formState.postAllowance" style="width: 100%" :min="0" :precision="2" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="绩效系数">
              <a-input-number v-model:value="formState.performanceCoefficient" style="width: 100%" :min="0" :max="3" :step="0.1" :precision="2" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="绩效奖金(元)">
              <a-input-number v-model:value="formState.performanceBonus" style="width: 100%" :min="0" :precision="2" />
            </a-form-item>
          </a-col>
        </a-row>

        <a-divider orientation="left">社保公积金比例</a-divider>

        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="社保个人比例">
              <a-input-number v-model:value="formState.socialInsurancePersonalRate" style="width: 100%" :min="0" :max="1" :step="0.0001" :precision="4" />
              <div class="form-tip">默认 0.1050 (10.5%)</div>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="社保公司比例">
              <a-input-number v-model:value="formState.socialInsuranceCompanyRate" style="width: 100%" :min="0" :max="1" :step="0.0001" :precision="4" />
              <div class="form-tip">默认 0.2716 (27.16%)</div>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="公积金个人比例">
              <a-input-number v-model:value="formState.housingFundPersonalRate" style="width: 100%" :min="0" :max="1" :step="0.0001" :precision="4" />
              <div class="form-tip">默认 0.0700 (7%)</div>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="公积金公司比例">
              <a-input-number v-model:value="formState.housingFundCompanyRate" style="width: 100%" :min="0" :max="1" :step="0.0001" :precision="4" />
              <div class="form-tip">默认 0.0700 (7%)</div>
            </a-form-item>
          </a-col>
        </a-row>

        <a-form-item label="社保公积金基数(元，留空则按基本工资计算)">
          <a-input-number v-model:value="formState.socialInsuranceBase" style="width: 100%" :min="0" :precision="2" :placeholder="'按基本工资计算'" />
        </a-form-item>

        <a-form-item label="备注说明">
          <a-textarea v-model:value="formState.description" :rows="2" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="applyVisible" title="套用模板" width="520px" @ok="handleApply" ok-text="确认套用" cancel-text="取消">
      <a-alert type="info" show-icon style="margin-bottom: 16px">
        <template #message>
          将模板 <b>{{ applyingTemplate?.templateName }}</b> 套用到指定员工，仅影响<strong>草稿状态</strong>的薪资单。
        </template>
      </a-alert>
      <a-form :model="applyForm" layout="vertical">
        <a-form-item label="薪资月份" :rules="[{ required: true, message: '请选择年份和月份' }]">
          <a-space>
            <a-select v-model:value="applyForm.salaryYear" style="width: 160px">
              <a-select-option v-for="y in yearOptions" :key="y" :value="y">{{ y }}年</a-select-option>
            </a-select>
            <a-select v-model:value="applyForm.salaryMonth" style="width: 120px">
              <a-select-option v-for="m in 12" :key="m" :value="m">{{ m }}月</a-select-option>
            </a-select>
          </a-space>
        </a-form-item>
        <a-form-item label="套用范围">
          <a-radio-group v-model:value="applyForm.scope">
            <a-radio value="dept">指定部门</a-radio>
            <a-radio value="emp">指定员工</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="选择部门" v-if="applyForm.scope === 'dept'">
          <a-tree-select
            v-model:value="applyForm.departmentId"
            :tree-data="deptFilterOptions"
            :field-names="{ label: 'name', value: 'id', children: 'children' }"
            placeholder="选择部门"
            allow-clear
            tree-default-expand-all
            style="width: 100%"
          />
        </a-form-item>
        <a-form-item label="选择员工" v-if="applyForm.scope === 'emp'">
          <a-select
            v-model:value="applyForm.employeeIds"
            mode="multiple"
            :options="employeeOptions"
            placeholder="选择员工（可多选）"
            style="width: 100%"
            :max-tag-count="3"
            allow-clear
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue';
import { useSalaryStore, type SalaryTemplate } from '../stores/salary';
import { useDepartmentStore } from '../stores/department';
import { useEmployeeStore } from '../stores/employee';
import { message, Modal } from 'ant-design-vue';
import { PlusOutlined } from '@ant-design/icons-vue';

const salaryStore = useSalaryStore();
const deptStore = useDepartmentStore();
const employeeStore = useEmployeeStore();

const keyword = ref('');
const formVisible = ref(false);
const applyVisible = ref(false);
const isEdit = ref(false);
const applyingTemplate = ref<SalaryTemplate | null>(null);

const currentYear = new Date().getFullYear();
const yearOptions = computed(() => [currentYear, currentYear - 1, currentYear - 2, currentYear + 1]);

const deptFilterOptions = computed(() => deptStore.departmentsTree);
const employeeOptions = computed(() =>
  employeeStore.employees.map((e) => ({ label: e.name, value: e.id }))
);

const formState = reactive<SalaryTemplate>({
  templateName: '',
  jobLevel: '',
  baseSalary: 0,
  postAllowance: 0,
  performanceCoefficient: 1.0,
  performanceBonus: 0,
  socialInsurancePersonalRate: 0.105,
  socialInsuranceCompanyRate: 0.2716,
  housingFundPersonalRate: 0.07,
  housingFundCompanyRate: 0.07,
  socialInsuranceBase: undefined,
  description: '',
  enabled: 1,
});

const applyForm = reactive({
  templateId: undefined as number | undefined,
  scope: 'dept' as 'dept' | 'emp',
  departmentId: undefined as number | undefined,
  employeeIds: [] as number[],
  salaryYear: currentYear,
  salaryMonth: new Date().getMonth() + 1,
});

const columns = [
  { title: '模板名称', dataIndex: 'templateName', key: 'templateName' },
  { title: '适用职级', dataIndex: 'jobLevel', key: 'jobLevel', width: 120 },
  { title: '基本工资', dataIndex: 'baseSalary', key: 'baseSalary', width: 140 },
  { title: '绩效系数', dataIndex: 'performanceCoefficient', key: 'performanceCoefficient', width: 100 },
  { title: '状态', dataIndex: 'enabled', key: 'enabled', width: 100 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 170 },
  { title: '操作', key: 'action', width: 160, fixed: 'right' as const },
];

const formatMoney = (v?: number) => (v ?? 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

function resetForm() {
  Object.assign(formState, {
    id: undefined,
    templateName: '',
    jobLevel: '',
    baseSalary: 0,
    postAllowance: 0,
    performanceCoefficient: 1.0,
    performanceBonus: 0,
    socialInsurancePersonalRate: 0.105,
    socialInsuranceCompanyRate: 0.2716,
    housingFundPersonalRate: 0.07,
    housingFundCompanyRate: 0.07,
    socialInsuranceBase: undefined,
    description: '',
    enabled: 1,
  });
}

async function loadTemplates() {
  await salaryStore.fetchTemplates(keyword.value);
}

function showCreateModal() {
  isEdit.value = false;
  resetForm();
  formVisible.value = true;
}

function showEditModal(record: SalaryTemplate) {
  isEdit.value = true;
  Object.assign(formState, { ...record });
  formVisible.value = true;
}

async function handleSubmit() {
  if (!formState.templateName || !formState.jobLevel || formState.baseSalary === undefined) {
    message.warning('请填写必填项');
    return;
  }
  let result;
  if (isEdit.value) {
    result = await salaryStore.updateTemplate({ ...formState });
  } else {
    result = await salaryStore.createTemplate({ ...formState });
  }
  if (result) {
    formVisible.value = false;
    loadTemplates();
  }
}

async function handleToggle(id: number) {
  await salaryStore.toggleTemplate(id);
  loadTemplates();
}

function showApplyModal(record: SalaryTemplate) {
  applyingTemplate.value = record;
  applyForm.templateId = record.id;
  applyForm.scope = 'dept';
  applyForm.departmentId = undefined;
  applyForm.employeeIds = [];
  applyVisible.value = true;
}

async function handleApply() {
  if (!applyForm.templateId) return;
  if (applyForm.scope === 'dept' && !applyForm.departmentId) {
    message.warning('请选择部门');
    return;
  }
  if (applyForm.scope === 'emp' && applyForm.employeeIds.length === 0) {
    message.warning('请选择员工');
    return;
  }
  const ok = await salaryStore.applyTemplate({
    templateId: applyForm.templateId,
    departmentId: applyForm.scope === 'dept' ? applyForm.departmentId : undefined,
    employeeIds: applyForm.scope === 'emp' ? applyForm.employeeIds : undefined,
    salaryYear: applyForm.salaryYear,
    salaryMonth: applyForm.salaryMonth,
  });
  if (ok) {
    applyVisible.value = false;
  }
}

onMounted(async () => {
  await deptStore.refreshAll();
  await employeeStore.fetchEmployees();
  loadTemplates();
});
</script>

<style scoped lang="scss">
.page-container { min-height: 100%; }
.content-wrapper { max-width: 1400px; margin: 0 auto; }
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
.modern-table {
  :deep(.ant-table-thead > tr > th) {
    background: #fafafa;
    font-weight: 600;
  }
}
.tpl-name { font-weight: 500; }
.money {
  font-family: 'SF Mono', Consolas, monospace;
  font-weight: 500;
  color: #52c41a;
}
.form-tip {
  font-size: 12px;
  color: #8c8c8c;
  margin-top: 4px;
}
</style>
