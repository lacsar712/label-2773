<template>
  <div class="page-container">
    <div class="content-wrapper">
      <a-card class="main-card" :bordered="false">
        <div class="header-section">
          <div class="title-group">
            <h1 class="page-title">入职清单管理</h1>
            <p class="subtitle">Onboarding Checklist Management</p>
          </div>
          <div class="header-actions">
            <a-space>
              <a-select
                v-model:value="statusFilter"
                style="width: 140px"
                placeholder="按状态筛选"
                allow-clear
              >
                <a-select-option :value="0">进行中</a-select-option>
                <a-select-option :value="1">已完成</a-select-option>
                <a-select-option :value="2">已归档</a-select-option>
              </a-select>
              <a-button type="primary" class="add-btn" @click="showGenerateModal()">
                <template #icon><UserAddOutlined /></template>
                生成立新员工清单
              </a-button>
            </a-space>
          </div>
        </div>

        <a-table
          :columns="columns"
          :data-source="filteredChecklists"
          :loading="checklistStore.loading"
          row-key="id"
          :pagination="{ pageSize: 10 }"
          class="modern-table"
          @row-click="(record: any) => goToDetail(record.id)"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'employee'">
              <div class="employee-cell">
                <a-avatar :style="{ backgroundColor: '#7265e6' }" size="small">
                  {{ record.employeeName?.charAt(0) }}
                </a-avatar>
                <div class="employee-info">
                  <div class="employee-name">{{ record.employeeName }}</div>
                  <div class="employee-dept">{{ record.departmentName }} · {{ record.position }}</div>
                </div>
              </div>
            </template>

            <template v-if="column.key === 'progress'">
              <div class="progress-cell">
                <a-progress
                  :percent="record.progress || 0"
                  :size="['100%', 10]"
                  :stroke-color="progressColor(record.progress)"
                  :show-info="false"
                />
                <span class="progress-text">{{ record.progress || 0 }}%</span>
              </div>
            </template>

            <template v-if="column.key === 'stages'">
              <div class="stages-row">
                <div
                  v-for="stage in [1, 2, 3, 4]"
                  :key="stage"
                  class="stage-mini"
                  :class="getStageProgressClass(record, stage)"
                  :title="getStageText(stage)"
                >
                  {{ getStageProgress(record, stage) }}%
                </div>
              </div>
            </template>

            <template v-if="column.key === 'status'">
              <a-tag :color="getStatusColor(record.status)">{{ getStatusText(record.status) }}</a-tag>
            </template>

            <template v-if="column.key === 'hireDate'">
              <span>{{ record.hireDate }}</span>
            </template>

            <template v-if="column.key === 'action'">
              <a-space size="small" @click.stop>
                <a-button type="link" class="action-btn view" @click="goToDetail(record.id)">查看</a-button>
                <a-divider type="vertical" />
                <a-popconfirm
                  v-if="record.status === 1"
                  title="确定要归档该入职清单吗?"
                  @confirm="handleArchive(record.id)"
                  ok-text="确定"
                  cancel-text="取消"
                >
                  <a-button type="link" class="action-btn archive">归档</a-button>
                </a-popconfirm>
              </a-space>
            </template>
          </template>
        </a-table>
      </a-card>

      <a-modal
        v-model:open="generateModalVisible"
        title="生成立新员工入职清单"
        @ok="handleGenerate"
        ok-text="生成"
        cancel-text="取消"
        centered
        class="modern-modal"
      >
        <a-form ref="generateFormRef" :model="generateFormState" layout="vertical" class="modern-form">
          <a-form-item label="选择员工" name="employeeId" :rules="[{ required: true, message: '请选择员工!' }]">
            <a-select
              v-model:value="generateFormState.employeeId"
              show-search
              placeholder="请选择要生成入职清单的员工"
              option-filter-prop="label"
              style="width: 100%"
              @change="handleEmployeeChange"
            >
              <a-select-option
                v-for="emp in availableEmployees"
                :key="emp.id"
                :value="emp.id"
                :label="`${emp.name} - ${emp.departmentName || ''}`"
              >
                {{ emp.name }}
                <span class="option-sub">{{ emp.departmentName }} · {{ emp.role }}</span>
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="入职日期" name="hireDate" :rules="[{ required: true, message: '请选择入职日期!' }]">
            <a-date-picker v-model:value="generateFormState.hireDate" style="width: 100%" format="YYYY-MM-DD" value-format="YYYY-MM-DD" />
          </a-form-item>
          <a-form-item label="使用模板" name="templateId">
            <a-select
              v-model:value="generateFormState.templateId"
              placeholder="不选则自动匹配最合适的模板"
              allow-clear
              style="width: 100%"
            >
              <a-select-option v-for="tpl in templateStore.enabledTemplates" :key="tpl.id" :value="tpl.id">
                {{ tpl.templateName }}
                <span class="option-sub" v-if="tpl.departmentName">（{{ tpl.departmentName }}{{ tpl.position ? ' - ' + tpl.position : '' }}）</span>
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="分配导师">
            <a-row :gutter="12">
              <a-col :span="12">
                <a-select
                  v-model:value="generateFormState.mentorId"
                  show-search
                  placeholder="选择导师（可选）"
                  option-filter-prop="label"
                  allow-clear
                  style="width: 100%"
                  @change="handleMentorChange"
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
              </a-col>
              <a-col :span="12">
                <a-input v-model:value="generateFormState.mentorName" placeholder="导师姓名" />
              </a-col>
            </a-row>
          </a-form-item>
        </a-form>
      </a-modal>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, reactive, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useOnboardingChecklistStore, type ChecklistGenerateDTO } from '../stores/onboardingChecklist';
import { useOnboardingTemplateStore } from '../stores/onboardingTemplate';
import { useEmployeeStore, type Employee } from '../stores/employee';
import { UserAddOutlined } from '@ant-design/icons-vue';

const router = useRouter();
const checklistStore = useOnboardingChecklistStore();
const templateStore = useOnboardingTemplateStore();
const employeeStore = useEmployeeStore();

const statusFilter = ref<number | undefined>(undefined);
const generateModalVisible = ref(false);
const generateFormRef = ref();

const generateFormState = reactive<ChecklistGenerateDTO>({
  employeeId: 0 as any,
  hireDate: '',
  templateId: undefined,
  mentorId: undefined,
  mentorName: undefined,
});

const columns = [
  { title: '员工', key: 'employee', width: '22%' },
  { title: '整体进度', key: 'progress', width: '18%' },
  { title: '阶段进度', key: 'stages', width: '20%' },
  { title: '状态', key: 'status', width: '10%' },
  { title: '入职日期', key: 'hireDate', dataIndex: 'hireDate', width: '12%' },
  { title: '操作', key: 'action', width: '18%', align: 'center' },
];

const filteredChecklists = computed(() => {
  if (statusFilter.value === undefined) {
    return checklistStore.checklists;
  }
  return checklistStore.checklists.filter((c) => c.status === statusFilter.value);
});

const availableEmployees = computed(() => {
  const checklistEmployeeIds = new Set(checklistStore.checklists.map((c) => c.employeeId));
  return employeeStore.employees.filter(
    (e) => !checklistEmployeeIds.has(e.id) && (e.status === 1 || e.status === 2 || e.status === 3)
  );
});

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

const getStageText = (stage?: number) => {
  switch (stage) {
    case 1: return '入职前';
    case 2: return '首日';
    case 3: return '首周';
    case 4: return '首月';
    default: return '-';
  }
};

const getStageProgress = (checklist: any, stage: number) => {
  switch (stage) {
    case 1: return checklist.preJoinProgress || 0;
    case 2: return checklist.firstDayProgress || 0;
    case 3: return checklist.firstWeekProgress || 0;
    case 4: return checklist.firstMonthProgress || 0;
    default: return 0;
  }
};

const getStageProgressClass = (checklist: any, stage: number) => {
  const progress = getStageProgress(checklist, stage);
  if (progress === 100) return 'stage-done';
  if (progress > 0) return 'stage-doing';
  return 'stage-pending';
};

const progressColor = (progress?: number) => {
  if (!progress) return '#d9d9d9';
  if (progress < 50) return '#ff4d4f';
  if (progress < 80) return '#faad14';
  return '#52c41a';
};

onMounted(async () => {
  await employeeStore.fetchEmployees();
  await templateStore.fetchEnabledTemplates();
  await checklistStore.fetchChecklists();
});

const showGenerateModal = () => {
  generateFormState.employeeId = 0 as any;
  generateFormState.hireDate = '';
  generateFormState.templateId = undefined;
  generateFormState.mentorId = undefined;
  generateFormState.mentorName = undefined;
  generateModalVisible.value = true;
};

const handleEmployeeChange = (empId: number) => {
  const emp = employeeStore.employees.find((e) => e.id === empId);
  if (emp) {
    generateFormState.hireDate = emp.hireDate || generateFormState.hireDate;
  }
};

const handleMentorChange = (mentorId: number | undefined) => {
  if (mentorId) {
    const mentor = employeeStore.employees.find((e) => e.id === mentorId);
    if (mentor) {
      generateFormState.mentorName = mentor.name;
    }
  }
};

const handleGenerate = async () => {
  try {
    await generateFormRef.value?.validate();
    const result = await checklistStore.generateChecklist(generateFormState);
    if (result) {
      generateModalVisible.value = false;
      if (result.id) {
        goToDetail(result.id);
      }
    }
  } catch (error) {
    // validation failed
  }
};

const goToDetail = (id: number) => {
  router.push(`/onboarding/checklist/${id}`);
};

const handleArchive = async (id: number) => {
  await checklistStore.archiveChecklist(id);
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

.employee-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.employee-info {
  display: flex;
  flex-direction: column;
}

.employee-name {
  font-weight: 600;
  color: #2c3e50;
}

.employee-dept {
  font-size: 12px;
  color: #8c8c8c;
}

.progress-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.progress-text {
  font-weight: 600;
  font-size: 13px;
  min-width: 36px;
}

.stages-row {
  display: flex;
  gap: 6px;
}

.stage-mini {
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 11px;
  font-weight: 500;
}

.stage-pending {
  background: #f5f5f5;
  color: #8c8c8c;
}

.stage-doing {
  background: #e6f7ff;
  color: #1890ff;
}

.stage-done {
  background: #f6ffed;
  color: #52c41a;
}

.option-sub {
  color: #8c8c8c;
  font-size: 12px;
  margin-left: 8px;
}

.action-btn {
  padding: 0;
  font-weight: 500;
}

.action-btn.view {
  color: #3498db;
}

.action-btn.archive {
  color: #722ed1;
}

.modern-table :deep(.ant-table-tbody > tr) {
  cursor: pointer;
  transition: background-color 0.2s;
}

.modern-table :deep(.ant-table-tbody > tr:hover) {
  background-color: #f5f7fa !important;
}
</style>
