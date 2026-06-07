<template>
  <div class="page-container">
    <div class="content-wrapper">
      <a-card class="main-card" :bordered="false">
        <div class="header-section">
          <div class="title-group">
            <h1 class="page-title">我的薪资条</h1>
            <p class="subtitle">My Payslip</p>
          </div>
        </div>

        <a-alert type="info" show-icon style="margin-bottom: 20px">
          <template #message>
            仅展示已发放的薪资单，敏感字段已做脱敏处理。如对薪资有疑问，请联系 HR。
          </template>
        </a-alert>

        <a-table
          :columns="columns"
          :data-source="salaryStore.myRecordList"
          :loading="salaryStore.loading"
          row-key="id"
          :pagination="{
            current: pageNum,
            pageSize,
            total: salaryStore.myRecordListTotal,
            showSizeChanger: true,
            onChange: handlePageChange,
          }"
          class="modern-table"
          @row-click="showDetail"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'period'">
              <span class="period-tag">{{ record.salaryYear }}年{{ record.salaryMonth }}月</span>
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
            <template v-if="column.key === 'issueTime'">
              <span>{{ record.issueTime?.slice(0, 10) || '-' }}</span>
            </template>
            <template v-if="column.key === 'action'">
              <a-button type="link" size="small" @click.stop="showDetail(record)">查看详情</a-button>
            </template>
          </template>
        </a-table>
      </a-card>
    </div>

    <a-drawer v-model:open="detailVisible" title="薪资条详情" width="560px" placement="right">
      <div v-if="salaryStore.myCurrentRecord" class="payslip-detail">
        <div class="payslip-header">
          <div class="company-info">
            <div class="company-name">员工管理系统</div>
            <div class="payslip-title">工资单</div>
          </div>
          <div class="period-info">
            <div class="period">{{ salaryStore.myCurrentRecord.salaryYear }}年{{ salaryStore.myCurrentRecord.salaryMonth }}月</div>
            <div class="pay-date">发放日期：{{ salaryStore.myCurrentRecord.issueTime?.slice(0, 10) }}</div>
          </div>
        </div>

        <div class="employee-info">
          <div class="info-item"><span class="label">员工姓名：</span><span class="value">{{ salaryStore.myCurrentRecord.employeeName }}</span></div>
          <div class="info-item"><span class="label">所属部门：</span><span class="value">{{ salaryStore.myCurrentRecord.departmentName || '-' }}</span></div>
          <div class="info-item"><span class="label">薪资单号：</span><span class="value mono">{{ salaryStore.myCurrentRecord.recordNo }}</span></div>
        </div>

        <div class="section">
          <div class="section-title">
            <DollarOutlined />
            <span>收入明细</span>
          </div>
          <a-table :pagination="false" size="small" :data-source="incomeRows" class="plain-table">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'value'">
                <span class="money">¥{{ formatMoney(record.value) }}</span>
              </template>
            </template>
          </a-table>
          <div class="section-total">
            <span>应发合计</span>
            <span class="money highlight large">¥{{ formatMoney(salaryStore.myCurrentRecord.grossSalary) }}</span>
          </div>
        </div>

        <div class="section">
          <div class="section-title">
            <MinusCircleOutlined />
            <span>扣款明细</span>
          </div>
          <a-table :pagination="false" size="small" :data-source="deductionRows" class="plain-table">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'value'">
                <span class="money deduction">¥{{ formatMoney(record.value) }}</span>
              </template>
            </template>
          </a-table>
          <div class="section-total">
            <span>扣款合计</span>
            <span class="money deduction large">-¥{{ formatMoney(salaryStore.myCurrentRecord.totalDeduction) }}</span>
          </div>
        </div>

        <div class="final-section">
          <div class="final-row">
            <span class="final-label">实发工资</span>
            <span class="final-value">¥{{ formatMoney(salaryStore.myCurrentRecord.netSalary) }}</span>
          </div>
          <div class="final-sub">
            <span>大写：{{ toChineseAmount(salaryStore.myCurrentRecord.netSalary) }}</span>
          </div>
        </div>

        <div class="footer-note">
          <p>※ 本工资单为系统自动生成，如有疑问请在收到后5个工作日内与人力资源部联系。</p>
          <p>※ 薪资信息属于个人敏感信息，请勿泄露给他人。</p>
        </div>
      </div>
    </a-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useSalaryStore, type SalaryRecord } from '../stores/salary';
import { DollarOutlined, MinusCircleOutlined } from '@ant-design/icons-vue';

const salaryStore = useSalaryStore();

const pageNum = ref(1);
const pageSize = ref(10);
const detailVisible = ref(false);

const columns = [
  { title: '薪资周期', key: 'period', dataIndex: 'period' },
  { title: '应发工资', key: 'grossSalary', dataIndex: 'grossSalary' },
  { title: '扣款合计', key: 'totalDeduction', dataIndex: 'totalDeduction' },
  { title: '实发工资', key: 'netSalary', dataIndex: 'netSalary' },
  { title: '发放时间', key: 'issueTime', dataIndex: 'issueTime', width: 140 },
  { title: '操作', key: 'action', width: 100 },
];

const incomeRows = computed(() => {
  const r = salaryStore.myCurrentRecord;
  if (!r) return [];
  return [
    { label: '基本工资', value: r.baseSalary || 0 },
    { label: '岗位津贴', value: r.postAllowance || 0 },
    { label: '绩效奖金', value: r.performanceBonus || 0 },
    { label: '加班费', value: r.overtimePay || 0 },
    { label: '其他补贴', value: r.otherAllowance || 0 },
  ];
});

const deductionRows = computed(() => {
  const r = salaryStore.myCurrentRecord;
  if (!r) return [];
  return [
    { label: '社会保险(个人)', value: r.socialInsurancePersonal || 0 },
    { label: '住房公积金(个人)', value: r.housingFundPersonal || 0 },
    { label: '个人所得税', value: r.incomeTax || 0 },
    { label: '其他扣款', value: r.otherDeduction || 0 },
  ];
});

const formatMoney = (v?: number) => (v ?? 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

function handlePageChange(p: number, s: number) {
  pageNum.value = p;
  pageSize.value = s;
  salaryStore.fetchMyRecords(p, s);
}

async function showDetail(record: SalaryRecord) {
  await salaryStore.fetchMyRecordDetail(record.id!);
  detailVisible.value = true;
}

function toChineseAmount(num?: number): string {
  if (num === undefined || num === null) return '';
  const digits = ['零', '壹', '贰', '叁', '肆', '伍', '陆', '柒', '捌', '玖'];
  const units = ['', '拾', '佰', '仟'];
  const bigUnits = ['', '万', '亿'];
  const n = Math.round(num * 100);
  const intPart = Math.floor(n / 100);
  const decPart = n % 100;
  let result = '';
  if (intPart === 0) result = '零';
  else {
    let intStr = intPart.toString();
    let groupCount = Math.ceil(intStr.length / 4);
    for (let g = 0; g < groupCount; g++) {
      const start = Math.max(0, intStr.length - (g + 1) * 4);
      const group = intStr.substring(start, intStr.length - g * 4);
      let groupStr = '';
      for (let i = 0; i < group.length; i++) {
        const d = parseInt(group[i]);
        const u = group.length - 1 - i;
        if (d !== 0) {
          groupStr += digits[d] + units[u];
        } else if (groupStr && !groupStr.endsWith('零')) {
          groupStr += '零';
        }
      }
      groupStr = groupStr.replace(/零+$/, '');
      if (groupStr) groupStr += bigUnits[g];
      result = groupStr + result;
    }
  }
  result += '元';
  const jiao = Math.floor(decPart / 10);
  const fen = decPart % 10;
  if (jiao === 0 && fen === 0) result += '整';
  else {
    if (jiao > 0) result += digits[jiao] + '角';
    else if (intPart > 0) result += '零';
    if (fen > 0) result += digits[fen] + '分';
  }
  return result;
}

onMounted(() => {
  salaryStore.fetchMyRecords(pageNum.value, pageSize.value);
});
</script>

<style scoped lang="scss">
.page-container { min-height: 100%; }
.content-wrapper { max-width: 1200px; margin: 0 auto; }
.main-card {
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}
.header-section {
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
  :deep(.ant-table-tbody > tr) {
    cursor: pointer;
    &:hover > td { background: #e6f4ff; }
  }
}
.period-tag {
  display: inline-block;
  padding: 2px 10px;
  background: #e6f4ff;
  color: #1677ff;
  border-radius: 4px;
  font-weight: 500;
}
.money {
  font-family: 'SF Mono', Consolas, monospace;
  font-weight: 500;
  &.deduction { color: #f5222d; }
  &.highlight { color: #52c41a; font-weight: 600; }
  &.large { font-size: 16px; }
}
.mono { font-family: 'SF Mono', Consolas, monospace; }

.payslip-detail {
  .payslip-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-end;
    padding-bottom: 16px;
    border-bottom: 2px solid #667eea;
    margin-bottom: 20px;
    .company-info {
      .company-name { font-size: 14px; color: #595959; }
      .payslip-title {
        font-size: 26px;
        font-weight: 700;
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        -webkit-background-clip: text;
        -webkit-text-fill-color: transparent;
        margin-top: 4px;
      }
    }
    .period-info {
      text-align: right;
      .period { font-size: 18px; font-weight: 600; color: #262626; }
      .pay-date { font-size: 12px; color: #8c8c8c; margin-top: 4px; }
    }
  }
  .employee-info {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 8px;
    padding: 12px 16px;
    background: #fafafa;
    border-radius: 8px;
    margin-bottom: 20px;
    .info-item {
      font-size: 13px;
      .label { color: #8c8c8c; }
      .value { color: #262626; font-weight: 500; }
    }
  }
  .section {
    margin-bottom: 20px;
    .section-title {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 14px;
      font-weight: 600;
      color: #262626;
      margin-bottom: 8px;
      padding-bottom: 8px;
      border-bottom: 1px solid #f0f0f0;
    }
    .section-total {
      display: flex;
      justify-content: space-between;
      padding: 10px 12px;
      background: #fafafa;
      border-radius: 6px;
      margin-top: 4px;
      font-weight: 600;
    }
  }
  .plain-table {
    :deep(.ant-table-thead > tr > th) { background: transparent; padding: 6px 12px; }
    :deep(.ant-table-tbody > tr > td) { padding: 6px 12px; border: none; }
    :deep(.ant-table) { border: none; }
  }
  .final-section {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border-radius: 12px;
    padding: 24px;
    color: white;
    text-align: center;
    margin-bottom: 20px;
    .final-row {
      display: flex;
      justify-content: space-between;
      align-items: baseline;
      .final-label { font-size: 16px; opacity: 0.95; }
      .final-value {
        font-size: 36px;
        font-weight: 700;
        font-family: 'SF Mono', Consolas, monospace;
      }
    }
    .final-sub {
      margin-top: 8px;
      font-size: 13px;
      opacity: 0.9;
      text-align: right;
    }
  }
  .footer-note {
    font-size: 12px;
    color: #8c8c8c;
    line-height: 1.8;
    p { margin: 0; }
  }
}
</style>
