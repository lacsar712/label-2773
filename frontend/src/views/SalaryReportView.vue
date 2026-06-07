<template>
  <div class="page-container">
    <div class="content-wrapper">
      <a-card class="main-card" :bordered="false">
        <div class="header-section">
          <div class="title-group">
            <h1 class="page-title">人力成本报表</h1>
            <p class="subtitle">Labor Cost Report</p>
          </div>
          <div class="header-actions">
            <a-space>
              <a-select v-model:value="queryYear" style="width: 120px" @change="loadReport">
                <a-select-option v-for="y in yearOptions" :key="y" :value="y">{{ y }}年</a-select-option>
              </a-select>
              <a-select v-model:value="queryMonth" style="width: 100px" @change="loadReport">
                <a-select-option v-for="m in 12" :key="m" :value="m">{{ m }}月</a-select-option>
              </a-select>
              <a-tree-select
                v-model:value="deptFilterId"
                :tree-data="deptFilterOptions"
                :field-names="{ label: 'name', value: 'id', children: 'children' }"
                placeholder="按部门筛选"
                allow-clear
                tree-default-expand-all
                style="width: 200px"
                @change="loadReport"
              />
              <a-button type="primary" @click="loadReport">
                <template #icon><ReloadOutlined /></template>
                刷新
              </a-button>
            </a-space>
          </div>
        </div>

        <a-spin :spinning="salaryStore.loading" tip="加载中...">
          <div class="overview-cards" v-if="salaryStore.costReport">
            <div class="stat-card primary">
              <div class="stat-icon"><WalletOutlined /></div>
              <div class="stat-content">
                <div class="stat-label">公司总成本</div>
                <div class="stat-value">¥{{ formatMoney(salaryStore.costReport.totalCompanyCost) }}</div>
                <div class="stat-foot">
                  <span>已发放 {{ salaryStore.costReport.totalEmployeeCount }} 人</span>
                </div>
              </div>
            </div>
            <div class="stat-card success">
              <div class="stat-icon"><DollarOutlined /></div>
              <div class="stat-content">
                <div class="stat-label">实发工资总额</div>
                <div class="stat-value">¥{{ formatMoney(salaryStore.costReport.totalNetSalary) }}</div>
                <div class="stat-foot">
                  <template v-if="salaryStore.costReport.momRate !== undefined && salaryStore.costReport.momRate !== null">
                    <span :class="trendClass(salaryStore.costReport.momRate, true)">
                      <component :is="trendIcon(salaryStore.costReport.momRate, true)" />
                      环比 {{ Math.abs(salaryStore.costReport.momRate).toFixed(1) }}%
                    </span>
                  </template>
                </div>
              </div>
            </div>
            <div class="stat-card warning">
              <div class="stat-icon"><TeamOutlined /></div>
              <div class="stat-content">
                <div class="stat-label">人均实发</div>
                <div class="stat-value">¥{{ formatMoney(salaryStore.costReport.avgNetSalary) }}</div>
                <div class="stat-foot">
                  <template v-if="salaryStore.costReport.yoyRate !== undefined && salaryStore.costReport.yoyRate !== null">
                    <span :class="trendClass(salaryStore.costReport.yoyRate, true)">
                      <component :is="trendIcon(salaryStore.costReport.yoyRate, true)" />
                      同比 {{ Math.abs(salaryStore.costReport.yoyRate).toFixed(1) }}%
                    </span>
                  </template>
                </div>
              </div>
            </div>
          </div>

          <div class="charts-section" v-if="salaryStore.costReport">
            <div class="chart-card">
              <div class="chart-header">
                <h3 class="chart-title">各部门人力成本</h3>
                <span class="chart-sub">Department Cost Distribution</span>
              </div>
              <div ref="barChartRef" class="chart-body"></div>
            </div>
            <div class="chart-card">
              <div class="chart-header">
                <h3 class="chart-title">各部门人数占比</h3>
                <span class="chart-sub">Headcount Distribution</span>
              </div>
              <div ref="pieChartRef" class="chart-body"></div>
            </div>
          </div>

          <div class="chart-card full-width" v-if="salaryStore.costReport">
            <div class="chart-header">
              <h3 class="chart-title">{{ queryYear }}年月度人力成本趋势</h3>
              <span class="chart-sub">Monthly Cost Trend</span>
            </div>
            <div ref="lineChartRef" class="chart-body tall"></div>
          </div>

          <div class="table-section" v-if="salaryStore.costReport">
            <div class="table-header">
              <h3 class="chart-title">各部门成本明细</h3>
            </div>
            <a-table
              :columns="deptColumns"
              :data-source="salaryStore.costReport.departmentSummaries || []"
              :pagination="false"
              row-key="departmentId"
              class="modern-table"
              :scroll="{ x: 1200 }"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'departmentName'">
                  <a-tag color="blue">{{ record.departmentName }}</a-tag>
                </template>
                <template v-if="column.key === 'employeeCount'">
                  <span class="strong">{{ record.employeeCount }} 人</span>
                </template>
                <template v-if="['totalGrossSalary', 'totalNetSalary', 'totalCompanyCost'].includes(column.key || '')">
                  <span class="money">¥{{ formatMoney((record as any)[column.key!]) }}</span>
                </template>
                <template v-if="column.key === 'avgNetSalary'">
                  <span class="money">¥{{ formatMoney(record.avgNetSalary) }}</span>
                </template>
                <template v-if="column.key === 'momRate'">
                  <span v-if="record.momRate === undefined || record.momRate === null" class="muted">-</span>
                  <span v-else :class="trendClass(record.momRate, true)">
                    <component :is="trendIcon(record.momRate, true)" />
                    {{ Math.abs(record.momRate).toFixed(1) }}%
                  </span>
                </template>
                <template v-if="column.key === 'yoyRate'">
                  <span v-if="record.yoyRate === undefined || record.yoyRate === null" class="muted">-</span>
                  <span v-else :class="trendClass(record.yoyRate, true)">
                    <component :is="trendIcon(record.yoyRate, true)" />
                    {{ Math.abs(record.yoyRate).toFixed(1) }}%
                  </span>
                </template>
              </template>
            </a-table>
          </div>
        </a-spin>
      </a-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch, nextTick, onBeforeUnmount } from 'vue';
import * as echarts from 'echarts';
import { useSalaryStore } from '../stores/salary';
import { useDepartmentStore } from '../stores/department';
import {
  ReloadOutlined,
  WalletOutlined,
  DollarOutlined,
  TeamOutlined,
  ArrowUpOutlined,
  ArrowDownOutlined,
} from '@ant-design/icons-vue';

const salaryStore = useSalaryStore();
const deptStore = useDepartmentStore();

const currentYear = new Date().getFullYear();
const currentMonth = new Date().getMonth() + 1;

const queryYear = ref(currentYear);
const queryMonth = ref(currentMonth);
const deptFilterId = ref<number | undefined>(undefined);

const yearOptions = computed(() => [currentYear, currentYear - 1, currentYear - 2]);
const deptFilterOptions = computed(() => deptStore.departmentsTree);

const barChartRef = ref<HTMLDivElement>();
const pieChartRef = ref<HTMLDivElement>();
const lineChartRef = ref<HTMLDivElement>();

let barChart: echarts.ECharts | null = null;
let pieChart: echarts.ECharts | null = null;
let lineChart: echarts.ECharts | null = null;

const CHART_COLORS = ['#667eea', '#764ba2', '#f093fb', '#4facfe', '#00f2fe', '#43e97b', '#38f9d7', '#fa709a', '#fee140', '#ff9a9e'];

const deptColumns = [
  { title: '部门', key: 'departmentName', width: 140, fixed: 'left' as const },
  { title: '人数', key: 'employeeCount', width: 90 },
  { title: '应发合计', key: 'totalGrossSalary', width: 130 },
  { title: '实发合计', key: 'totalNetSalary', width: 130 },
  { title: '人均实发', key: 'avgNetSalary', width: 130 },
  { title: '公司总成本', key: 'totalCompanyCost', width: 140 },
  { title: '环比', key: 'momRate', width: 100 },
  { title: '同比', key: 'yoyRate', width: 100 },
];

const formatMoney = (v?: number) => {
  if (v === undefined || v === null) return '0.00';
  if (v >= 10000) {
    return (v / 10000).toFixed(2) + '万';
  }
  return v.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
};

const trendClass = (val?: number, inverse = false) => {
  if (val === undefined || val === null || val === 0) return 'muted';
  if (inverse) return val > 0 ? 'trend-up' : 'trend-down';
  return val > 0 ? 'trend-up' : 'trend-down';
};

const trendIcon = (val?: number, inverse = false) => {
  if (val === undefined || val === null || val === 0) return null;
  if (inverse) return val > 0 ? ArrowUpOutlined : ArrowDownOutlined;
  return val > 0 ? ArrowUpOutlined : ArrowDownOutlined;
};

async function loadReport() {
  await salaryStore.fetchCostReport(queryYear.value, queryMonth.value, deptFilterId.value);
  await nextTick();
  renderCharts();
}

function renderCharts() {
  renderBarChart();
  renderPieChart();
  renderLineChart();
}

function renderBarChart() {
  if (!barChartRef.value || !salaryStore.costReport) return;
  if (!barChart) barChart = echarts.init(barChartRef.value);
  const data = salaryStore.costReport.departmentSummaries || [];
  const option: echarts.EChartsOption = {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (p: any) => {
        const item = data[p[0].dataIndex];
        return `${item.departmentName}<br/>总成本: ¥${(item.totalCompanyCost || 0).toLocaleString()}<br/>实发: ¥${(item.totalNetSalary || 0).toLocaleString()}<br/>人均: ¥${(item.avgNetSalary || 0).toLocaleString()}`;
      },
    },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '10%', containLabel: true },
    xAxis: {
      type: 'category',
      data: data.map((d) => d.departmentName),
      axisLabel: { color: '#595959', interval: 0, rotate: data.length > 5 ? 25 : 0, fontSize: 11 },
      axisLine: { lineStyle: { color: '#e8e8e8' } },
      axisTick: { show: false },
    },
    yAxis: {
      type: 'value',
      axisLabel: {
        color: '#8c8c8c',
        formatter: (v: any) => (v >= 10000 ? (v / 10000).toFixed(0) + '万' : v),
      },
      splitLine: { lineStyle: { color: '#f0f0f0', type: 'dashed' } },
    },
    series: [
      {
        type: 'bar',
        data: data.map((d, i) => ({
          value: d.totalCompanyCost,
          itemStyle: {
            borderRadius: [6, 6, 0, 0],
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: CHART_COLORS[i % CHART_COLORS.length] },
              { offset: 1, color: CHART_COLORS[(i + 2) % CHART_COLORS.length] },
            ]),
          },
        })),
        barWidth: '50%',
        label: {
          show: true,
          position: 'top',
          color: '#595959',
          fontSize: 11,
          formatter: (p: any) => (p.value >= 10000 ? (p.value / 10000).toFixed(1) + '万' : p.value),
        },
      },
    ],
  };
  barChart.setOption(option);
}

function renderPieChart() {
  if (!pieChartRef.value || !salaryStore.costReport) return;
  if (!pieChart) pieChart = echarts.init(pieChartRef.value);
  const data = salaryStore.costReport.departmentSummaries || [];
  const option: echarts.EChartsOption = {
    tooltip: { trigger: 'item', formatter: '{b}<br/>{c}人 ({d}%)' },
    legend: {
      type: 'scroll',
      orient: 'vertical',
      right: 10,
      top: 20,
      bottom: 20,
      textStyle: { color: '#595959', fontSize: 12 },
    },
    color: CHART_COLORS,
    series: [
      {
        type: 'pie',
        radius: ['45%', '72%'],
        center: ['38%', '50%'],
        avoidLabelOverlap: true,
        itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 },
        label: { show: false },
        emphasis: {
          label: { show: true, fontSize: 14, fontWeight: 'bold', formatter: '{b}\n{d}%' },
          itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0,0,0,0.2)' },
        },
        data: data.map((d, i) => ({
          value: d.employeeCount,
          name: d.departmentName,
          itemStyle: { color: CHART_COLORS[i % CHART_COLORS.length] },
        })),
      },
    ],
  };
  pieChart.setOption(option);
}

function renderLineChart() {
  if (!lineChartRef.value || !salaryStore.costReport) return;
  if (!lineChart) lineChart = echarts.init(lineChartRef.value);
  const trend = salaryStore.costReport.monthlyTrend || [];
  const months = trend.map((t) => `${t.salaryMonth}月`);
  const costs = trend.map((t) => t.totalCompanyCost || 0);
  const nets = trend.map((t) => t.totalNetSalary || 0);
  const option: echarts.EChartsOption = {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross' },
      formatter: (params: any) => {
        let html = `${params[0].axisValue}<br/>`;
        params.forEach((p: any) => {
          html += `${p.marker}${p.seriesName}: ¥${(p.value || 0).toLocaleString()}<br/>`;
        });
        return html;
      },
    },
    legend: { data: ['公司总成本', '实发工资总额'], top: 0, textStyle: { color: '#595959' } },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '12%', containLabel: true },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: months,
      axisLabel: { color: '#595959' },
      axisLine: { lineStyle: { color: '#e8e8e8' } },
    },
    yAxis: {
      type: 'value',
      axisLabel: {
        color: '#8c8c8c',
        formatter: (v: any) => (v >= 10000 ? (v / 10000).toFixed(0) + '万' : v),
      },
      splitLine: { lineStyle: { color: '#f0f0f0', type: 'dashed' } },
    },
    series: [
      {
        name: '公司总成本',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 8,
        data: costs,
        lineStyle: { width: 3, color: '#667eea' },
        itemStyle: { color: '#667eea' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(102, 126, 234, 0.3)' },
            { offset: 1, color: 'rgba(102, 126, 234, 0.02)' },
          ]),
        },
      },
      {
        name: '实发工资总额',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 8,
        data: nets,
        lineStyle: { width: 3, color: '#52c41a' },
        itemStyle: { color: '#52c41a' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(82, 196, 26, 0.25)' },
            { offset: 1, color: 'rgba(82, 196, 26, 0.02)' },
          ]),
        },
      },
    ],
  };
  lineChart.setOption(option);
}

function handleResize() {
  barChart?.resize();
  pieChart?.resize();
  lineChart?.resize();
}

watch(
  () => salaryStore.costReport,
  () => {
    if (salaryStore.costReport) nextTick(renderCharts);
  },
  { deep: true }
);

onMounted(async () => {
  await deptStore.refreshAll();
  loadReport();
  window.addEventListener('resize', handleResize);
});

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize);
  barChart?.dispose();
  pieChart?.dispose();
  lineChart?.dispose();
});
</script>

<style scoped lang="scss">
.page-container { min-height: 100%; }
.content-wrapper { max-width: 1500px; margin: 0 auto; }
.main-card {
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}
.header-section {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 24px;
  flex-wrap: wrap;
  gap: 12px;
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
.overview-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}
.stat-card {
  background: #fff;
  border-radius: 14px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
  transition: transform 0.3s;
  &:hover { transform: translateY(-3px); }
  &.primary {
    .stat-icon { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); }
    .stat-value { color: #667eea; }
  }
  &.success {
    .stat-icon { background: linear-gradient(135deg, #52c41a 0%, #73d13d 100%); }
    .stat-value { color: #52c41a; }
  }
  &.warning {
    .stat-icon { background: linear-gradient(135deg, #faad14 0%, #ffc53d 100%); }
    .stat-value { color: #faad14; }
  }
  .stat-icon {
    width: 52px; height: 52px;
    border-radius: 14px;
    display: flex; align-items: center; justify-content: center;
    color: #fff; font-size: 24px;
    flex-shrink: 0;
    box-shadow: 0 4px 12px rgba(102,126,234,0.3);
  }
  .stat-content {
    flex: 1;
    .stat-label { font-size: 13px; color: #8c8c8c; margin-bottom: 6px; }
    .stat-value {
      font-size: 24px;
      font-weight: 700;
      line-height: 1.2;
      font-family: 'SF Mono', Consolas, monospace;
    }
    .stat-foot {
      margin-top: 8px;
      font-size: 12px;
      color: #8c8c8c;
    }
  }
}
.charts-section {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-bottom: 20px;
}
.chart-card {
  background: #fff;
  border-radius: 14px;
  padding: 20px 24px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
  &.full-width { width: 100%; margin-bottom: 20px; }
  .chart-header {
    margin-bottom: 16px;
    .chart-title {
      font-size: 16px;
      font-weight: 600;
      color: #262626;
      margin: 0;
      display: inline-block;
    }
    .chart-sub {
      font-size: 12px;
      color: #bfbfbf;
      margin-left: 8px;
    }
  }
  .chart-body { width: 100%; height: 300px; &.tall { height: 360px; } }
}
.table-section { margin-top: 20px; .table-header { margin-bottom: 12px; } }
.modern-table {
  :deep(.ant-table-thead > tr > th) {
    background: #fafafa;
    font-weight: 600;
  }
}
.money {
  font-family: 'SF Mono', Consolas, monospace;
  font-weight: 500;
}
.strong { font-weight: 600; color: #262626; }
.muted { color: #bfbfbf; }
.trend-up { color: #f5222d; font-weight: 600; }
.trend-down { color: #52c41a; font-weight: 600; }

@media (max-width: 992px) {
  .charts-section { grid-template-columns: 1fr; }
}
</style>
