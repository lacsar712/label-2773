<template>
  <div class="dashboard-container">
    <div class="dashboard-wrapper">
      <div class="page-header">
        <div class="title-area">
          <h1 class="page-title">数据概览</h1>
          <p class="page-subtitle">Dashboard Overview</p>
        </div>
        <div class="filter-area">
          <a-space size="middle">
            <a-range-picker
              v-model:value="dateRange"
              format="YYYY-MM-DD"
              :ranges="dateRanges"
              placeholder="开始日期~结束日期"
              style="width: 280px"
              @change="handleDateRangeChange"
            />
            <a-tree-select
              v-model:value="dashboardStore.query.departmentId"
              :tree-data="deptFilterOptions"
              :field-names="{ label: 'name', value: 'id', children: 'children' }"
              placeholder="按部门筛选"
              allow-clear
              tree-default-expand-all
              style="width: 200px"
              @change="handleDeptFilterChange"
            />
            <a-button type="primary" :loading="dashboardStore.loading" @click="refreshData">
              <template #icon><ReloadOutlined /></template>
              刷新数据
            </a-button>
          </a-space>
        </div>
      </div>

      <a-spin :spinning="dashboardStore.loading" tip="数据加载中...">
        <div class="stats-grid">
          <div class="stat-card total-card">
            <div class="stat-icon">
              <TeamOutlined />
            </div>
            <div class="stat-content">
              <div class="stat-label">员工总数</div>
              <div class="stat-value">{{ dashboardStore.overview?.totalEmployees ?? 0 }}</div>
              <div class="stat-footnote">
                <span>编制 {{ dashboardStore.overview?.totalHeadcount ?? 0 }} 人</span>
                <span class="stat-mom" :class="momClass(dashboardStore.overview?.totalEmployeesMoM)">
                  <template v-if="dashboardStore.overview?.totalEmployeesMoM !== undefined">
                    <component :is="getTrendIcon(dashboardStore.overview.totalEmployeesMoM)" />
                    {{ Math.abs(dashboardStore.overview.totalEmployeesMoM).toFixed(1) }}%
                  </template>
                </span>
              </div>
            </div>
            <a-progress
              class="stat-progress"
              type="circle"
              :percent="Math.round(dashboardStore.overview?.headcountUsageRate ?? 0)"
              :size="64"
              :stroke-color="progressColor(dashboardStore.overview?.headcountUsageRate)"
            />
          </div>

          <div class="stat-card">
            <div class="stat-icon" style="background: linear-gradient(135deg, #52c41a 0%, #73d13d 100%);">
              <UserAddOutlined />
            </div>
            <div class="stat-content">
              <div class="stat-label">本月新入职</div>
              <div class="stat-value" style="color: #52c41a;">{{ dashboardStore.overview?.newHiresThisMonth ?? 0 }}</div>
              <div class="stat-footnote">
                <span>近一年趋势</span>
                <span class="stat-mom" :class="momClass(dashboardStore.overview?.newHiresMoM)">
                  <template v-if="dashboardStore.overview?.newHiresMoM !== undefined">
                    <component :is="getTrendIcon(dashboardStore.overview.newHiresMoM)" />
                    {{ Math.abs(dashboardStore.overview.newHiresMoM).toFixed(1) }}%
                  </template>
                </span>
              </div>
            </div>
          </div>

          <div class="stat-card">
            <div class="stat-icon" style="background: linear-gradient(135deg, #faad14 0%, #ffc53d 100%);">
              <ClockCircleOutlined />
            </div>
            <div class="stat-content">
              <div class="stat-label">待转正</div>
              <div class="stat-value" style="color: #faad14;">{{ dashboardStore.overview?.pendingProbation ?? 0 }}</div>
              <div class="stat-footnote">
                <span>试用期员工</span>
              </div>
            </div>
          </div>

          <div class="stat-card">
            <div class="stat-icon" style="background: linear-gradient(135deg, #722ed1 0%, #9254de 100%);">
              <FileTextOutlined />
            </div>
            <div class="stat-content">
              <div class="stat-label">合同即将到期</div>
              <div class="stat-value" style="color: #722ed1;">{{ dashboardStore.overview?.contractExpiringSoon ?? 0 }}</div>
              <div class="stat-footnote">
                <span>未来三个月内</span>
              </div>
            </div>
          </div>

          <div class="stat-card">
            <div class="stat-icon" style="background: linear-gradient(135deg, #f5222d 0%, #ff4d4f 100%);">
              <UserDeleteOutlined />
            </div>
            <div class="stat-content">
              <div class="stat-label">本月离职率</div>
              <div class="stat-value" style="color: #f5222d;">{{ (dashboardStore.overview?.attritionRate ?? 0).toFixed(1) }}%</div>
              <div class="stat-footnote">
                <span>月度流失率</span>
                <span class="stat-mom" :class="momClass(dashboardStore.overview?.attritionRateMoM, true)">
                  <template v-if="dashboardStore.overview?.attritionRateMoM !== undefined">
                    <component :is="getTrendIcon(dashboardStore.overview.attritionRateMoM, true)" />
                    {{ Math.abs(dashboardStore.overview.attritionRateMoM).toFixed(1) }}%
                  </template>
                </span>
              </div>
            </div>
          </div>

          <div class="stat-card">
            <div class="stat-icon" style="background: linear-gradient(135deg, #13c2c2 0%, #36cfc9 100%);">
              <CalendarOutlined />
            </div>
            <div class="stat-content">
              <div class="stat-label">待审批请假</div>
              <div class="stat-value" style="color: #13c2c2;">{{ dashboardStore.overview?.pendingLeaveApproval ?? 0 }}</div>
              <div class="stat-footnote">
                <span>等待处理</span>
              </div>
            </div>
          </div>

          <div class="stat-card">
            <div class="stat-icon" style="background: linear-gradient(135deg, #eb2f96 0%, #f759ab 100%);">
              <FormOutlined />
            </div>
            <div class="stat-content">
              <div class="stat-label">待处理入职</div>
              <div class="stat-value" style="color: #eb2f96;">{{ dashboardStore.overview?.pendingOnboarding ?? 0 }}</div>
              <div class="stat-footnote">
                <span>入职流程中</span>
              </div>
            </div>
          </div>
        </div>

        <div class="charts-grid">
          <div class="chart-card">
            <div class="chart-header">
              <h3 class="chart-title">各部门人数占比</h3>
              <span class="chart-subtitle">Department Distribution</span>
            </div>
            <div ref="pieChartRef" class="chart-body"></div>
          </div>

          <div class="chart-card">
            <div class="chart-header">
              <h3 class="chart-title">编制使用率</h3>
              <span class="chart-subtitle">Headcount Utilization</span>
            </div>
            <div ref="barChartRef" class="chart-body"></div>
          </div>
        </div>

        <div class="chart-card full-width">
          <div class="chart-header">
            <h3 class="chart-title">近一年入离职趋势</h3>
            <span class="chart-subtitle">Turnover Trend</span>
          </div>
          <div ref="lineChartRef" class="chart-body tall"></div>
        </div>
      </a-spin>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, onMounted, onBeforeUnmount, computed, nextTick, watch } from 'vue';
import * as echarts from 'echarts';
import dayjs, { type Dayjs } from 'dayjs';
import { useDashboardStore } from '../stores/dashboard';
import { useDepartmentStore } from '../stores/department';
import {
  TeamOutlined,
  UserAddOutlined,
  UserDeleteOutlined,
  ClockCircleOutlined,
  FileTextOutlined,
  CalendarOutlined,
  FormOutlined,
  ReloadOutlined,
  ArrowUpOutlined,
  ArrowDownOutlined,
} from '@ant-design/icons-vue';

const dashboardStore = useDashboardStore();
const deptStore = useDepartmentStore();

const pieChartRef = ref<HTMLDivElement>();
const barChartRef = ref<HTMLDivElement>();
const lineChartRef = ref<HTMLDivElement>();

let pieChart: echarts.ECharts | null = null;
let barChart: echarts.ECharts | null = null;
let lineChart: echarts.ECharts | null = null;

const dateRange = ref<[Dayjs, Dayjs] | null>(null);

const dateRanges = computed(() => ({
  近三个月: [dayjs().subtract(2, 'month').startOf('month'), dayjs().endOf('month')],
  近半年: [dayjs().subtract(5, 'month').startOf('month'), dayjs().endOf('month')],
  近一年: [dayjs().subtract(11, 'month').startOf('month'), dayjs().endOf('month')],
}));

const deptFilterOptions = computed(() => {
  return [{ id: null as any, name: '全部部门', children: deptStore.departmentsTree }];
});

const CHART_COLORS = ['#667eea', '#764ba2', '#f093fb', '#4facfe', '#00f2fe', '#43e97b', '#38f9d7', '#fa709a', '#fee140', '#ff9a9e'];

const momClass = (val?: number, inverse = false) => {
  if (val === undefined || val === 0) return 'neutral';
  if (inverse) return val > 0 ? 'negative' : 'positive';
  return val > 0 ? 'positive' : 'negative';
};

const getTrendIcon = (val?: number, inverse = false) => {
  if (val === undefined || val === 0) return null;
  if (inverse) return val > 0 ? ArrowUpOutlined : ArrowDownOutlined;
  return val > 0 ? ArrowUpOutlined : ArrowDownOutlined;
};

const progressColor = (rate?: number) => {
  if (rate === undefined) return '#667eea';
  if (rate >= 100) return '#f5222d';
  if (rate >= 90) return '#faad14';
  return '#667eea';
};

const handleDateRangeChange = (dates: any) => {
  if (dates && dates.length === 2) {
    dashboardStore.setQuery({
      startDate: dates[0].format('YYYY-MM-DD'),
      endDate: dates[1].format('YYYY-MM-DD'),
    });
  } else {
    dashboardStore.setQuery({ startDate: undefined, endDate: undefined });
  }
  dashboardStore.fetchOverview();
};

const handleDeptFilterChange = (value: number | null) => {
  dashboardStore.setQuery({ departmentId: value ?? null });
  dashboardStore.fetchOverview();
};

const refreshData = () => {
  dashboardStore.fetchOverview();
};

const initPieChart = () => {
  if (!pieChartRef.value) return;
  pieChart = echarts.init(pieChartRef.value);
  const data = dashboardStore.overview?.departmentStats || [];
  const option: echarts.EChartsOption = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}<br/>人数: {c} ({d}%)',
    },
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
        name: '部门人数',
        type: 'pie',
        radius: ['45%', '72%'],
        center: ['35%', '50%'],
        avoidLabelOverlap: true,
        itemStyle: {
          borderRadius: 8,
          borderColor: '#fff',
          borderWidth: 2,
        },
        label: {
          show: false,
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 14,
            fontWeight: 'bold',
            formatter: '{b}\n{d}%',
          },
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.2)',
          },
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
};

const initBarChart = () => {
  if (!barChartRef.value) return;
  barChart = echarts.init(barChartRef.value);
  const data = dashboardStore.overview?.departmentStats || [];
  const option: echarts.EChartsOption = {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params: any) => {
        const item = data[params[0]?.dataIndex];
        if (!item) return '';
        return `${item.departmentName}<br/>编制使用率: ${item.headcountUsageRate.toFixed(1)}%<br/>在职: ${item.employeeCount} / ${item.headcountLimit}`;
      },
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '10%',
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      data: data.map((d) => d.departmentName),
      axisLabel: {
        color: '#595959',
        interval: 0,
        rotate: data.length > 5 ? 30 : 0,
        fontSize: 11,
      },
      axisLine: { lineStyle: { color: '#e8e8e8' } },
      axisTick: { show: false },
    },
    yAxis: {
      type: 'value',
      max: 100,
      axisLabel: { color: '#8c8c8c', formatter: '{value}%' },
      splitLine: { lineStyle: { color: '#f0f0f0', type: 'dashed' } },
    },
    series: [
      {
        type: 'bar',
        data: data.map((d) => ({
          value: Math.round(d.headcountUsageRate * 10) / 10,
          itemStyle: {
            borderRadius: [6, 6, 0, 0],
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: d.headcountUsageRate >= 100 ? '#f5222d' : d.headcountUsageRate >= 90 ? '#faad14' : '#667eea' },
              { offset: 1, color: d.headcountUsageRate >= 100 ? '#ff7875' : d.headcountUsageRate >= 90 ? '#ffc53d' : '#764ba2' },
            ]),
          },
        })),
        barWidth: '45%',
        label: {
          show: true,
          position: 'top',
          color: '#595959',
          fontSize: 11,
          formatter: '{c}%',
        },
        markLine: {
          silent: true,
          symbol: 'none',
          lineStyle: { color: '#f5222d', type: 'dashed' },
          data: [{ yAxis: 100 }],
        },
      },
    ],
  };
  barChart.setOption(option);
};

const initLineChart = () => {
  if (!lineChartRef.value) return;
  lineChart = echarts.init(lineChartRef.value);
  const trend = dashboardStore.overview?.turnoverTrend || [];
  const months = trend.map((t) => t.month);
  const hires = trend.map((t) => t.hires);
  const departures = trend.map((t) => t.departures);
  const attrition = trend.map((t) => Math.round(t.attritionRate * 10) / 10);

  const option: echarts.EChartsOption = {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross' },
    },
    legend: {
      data: ['入职人数', '离职人数', '离职率(%)'],
      top: 0,
      textStyle: { color: '#595959' },
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '12%',
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: months,
      axisLabel: { color: '#595959' },
      axisLine: { lineStyle: { color: '#e8e8e8' } },
    },
    yAxis: [
      {
        type: 'value',
        name: '人数',
        axisLabel: { color: '#8c8c8c' },
        splitLine: { lineStyle: { color: '#f0f0f0', type: 'dashed' } },
      },
      {
        type: 'value',
        name: '离职率(%)',
        axisLabel: { color: '#8c8c8c', formatter: '{value}%' },
        splitLine: { show: false },
      },
    ],
    series: [
      {
        name: '入职人数',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 8,
        data: hires,
        lineStyle: { width: 3, color: '#52c41a' },
        itemStyle: { color: '#52c41a' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(82, 196, 26, 0.25)' },
            { offset: 1, color: 'rgba(82, 196, 26, 0.02)' },
          ]),
        },
      },
      {
        name: '离职人数',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 8,
        data: departures,
        lineStyle: { width: 3, color: '#f5222d' },
        itemStyle: { color: '#f5222d' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(245, 34, 45, 0.2)' },
            { offset: 1, color: 'rgba(245, 34, 45, 0.02)' },
          ]),
        },
      },
      {
        name: '离职率(%)',
        type: 'line',
        yAxisIndex: 1,
        smooth: true,
        symbol: 'diamond',
        symbolSize: 8,
        data: attrition,
        lineStyle: { width: 3, color: '#722ed1', type: 'dashed' },
        itemStyle: { color: '#722ed1' },
      },
    ],
  };
  lineChart.setOption(option);
};

const renderCharts = async () => {
  await nextTick();
  initPieChart();
  initBarChart();
  initLineChart();
};

const handleResize = () => {
  pieChart?.resize();
  barChart?.resize();
  lineChart?.resize();
};

watch(
  () => dashboardStore.overview,
  () => {
    if (dashboardStore.overview) {
      renderCharts();
    }
  },
  { deep: true }
);

onMounted(async () => {
  await deptStore.refreshAll();
  await dashboardStore.refresh();

  if (dashboardStore.query.startDate && dashboardStore.query.endDate) {
    dateRange.value = [
      dayjs(dashboardStore.query.startDate) as unknown as Dayjs,
      dayjs(dashboardStore.query.endDate) as unknown as Dayjs,
    ];
  }

  window.addEventListener('resize', handleResize);
});

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize);
  pieChart?.dispose();
  barChart?.dispose();
  lineChart?.dispose();
});
</script>

<style scoped lang="scss">
.dashboard-container {
  min-height: 100vh;
  padding: 24px;
  background: linear-gradient(135deg, #f5f7fa 0%, #e8ecf3 100%);
}

.dashboard-wrapper {
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 28px;
  flex-wrap: wrap;
  gap: 16px;

  .title-area {
    .page-title {
      font-size: 28px;
      font-weight: 700;
      margin: 0;
      background: linear-gradient(135deg, #2c3e50 0%, #667eea 100%);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
    }
    .page-subtitle {
      color: #8c8c8c;
      margin: 4px 0 0 0;
      font-size: 13px;
      letter-spacing: 1px;
    }
  }
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  background: #fff;
  border-radius: 14px;
  padding: 20px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
  display: flex;
  align-items: center;
  gap: 16px;
  position: relative;
  transition: all 0.3s ease;

  &:hover {
    transform: translateY(-3px);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
  }

  &.total-card {
    grid-column: span 1;
  }

  .stat-icon {
    width: 52px;
    height: 52px;
    border-radius: 14px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    display: flex;
    align-items: center;
    justify-content: center;
    color: #fff;
    font-size: 24px;
    flex-shrink: 0;
    box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
  }

  .stat-content {
    flex: 1;
    min-width: 0;

    .stat-label {
      font-size: 13px;
      color: #8c8c8c;
      margin-bottom: 6px;
    }

    .stat-value {
      font-size: 28px;
      font-weight: 700;
      color: #262626;
      line-height: 1.2;
    }

    .stat-footnote {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-top: 8px;
      font-size: 12px;
      color: #8c8c8c;

      .stat-mom {
        display: inline-flex;
        align-items: center;
        gap: 2px;
        font-weight: 600;
        padding: 2px 8px;
        border-radius: 10px;
        font-size: 11px;

        &.positive {
          color: #52c41a;
          background: rgba(82, 196, 26, 0.08);
        }
        &.negative {
          color: #f5222d;
          background: rgba(245, 34, 45, 0.08);
        }
        &.neutral {
          color: #8c8c8c;
          background: rgba(140, 140, 140, 0.08);
        }
      }
    }
  }

  .stat-progress {
    flex-shrink: 0;
  }
}

.charts-grid {
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

  &.full-width {
    width: 100%;
  }

  .chart-header {
    margin-bottom: 16px;

    .chart-title {
      font-size: 16px;
      font-weight: 600;
      color: #262626;
      margin: 0;
      display: inline-block;
    }

    .chart-subtitle {
      font-size: 12px;
      color: #bfbfbf;
      margin-left: 8px;
    }
  }

  .chart-body {
    width: 100%;
    height: 300px;

    &.tall {
      height: 360px;
    }
  }
}

@media (max-width: 992px) {
  .charts-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .dashboard-container {
    padding: 16px;
  }

  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .stat-card .stat-content .stat-value {
    font-size: 22px;
  }

  .page-header {
    flex-direction: column;
    align-items: flex-start;
  }
}

@media (max-width: 480px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
}
</style>
