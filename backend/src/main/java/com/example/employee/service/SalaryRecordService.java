package com.example.employee.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.employee.context.UserContext;
import com.example.employee.dto.*;
import com.example.employee.entity.*;
import com.example.employee.mapper.SalaryAdjustLogMapper;
import com.example.employee.mapper.SalaryRecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SalaryRecordService extends ServiceImpl<SalaryRecordMapper, SalaryRecord> {

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private SalaryTemplateService salaryTemplateService;

    @Autowired
    private SalaryAdjustLogService salaryAdjustLogService;

    @Autowired
    private SalaryAdjustLogMapper salaryAdjustLogMapper;

    private static final String[] STATUS_NAMES = {"草稿", "已确认", "已发放"};

    private static final Map<String, String> FIELD_LABELS = new LinkedHashMap<>();
    static {
        FIELD_LABELS.put("baseSalary", "基本工资");
        FIELD_LABELS.put("postAllowance", "岗位津贴");
        FIELD_LABELS.put("performanceCoefficient", "绩效系数");
        FIELD_LABELS.put("performanceBonus", "绩效奖金");
        FIELD_LABELS.put("overtimePay", "加班费");
        FIELD_LABELS.put("otherAllowance", "其他补贴");
        FIELD_LABELS.put("socialInsurancePersonal", "社保(个人)");
        FIELD_LABELS.put("socialInsuranceCompany", "社保(公司)");
        FIELD_LABELS.put("housingFundPersonal", "公积金(个人)");
        FIELD_LABELS.put("housingFundCompany", "公积金(公司)");
        FIELD_LABELS.put("incomeTax", "个人所得税");
        FIELD_LABELS.put("otherDeduction", "其他扣款");
    }

    private static final BigDecimal TAX_THRESHOLD = new BigDecimal("5000");
    private static final BigDecimal[][] TAX_BRACKETS = {
            {new BigDecimal("3000"), new BigDecimal("0.03"), BigDecimal.ZERO},
            {new BigDecimal("12000"), new BigDecimal("0.10"), new BigDecimal("210")},
            {new BigDecimal("25000"), new BigDecimal("0.20"), new BigDecimal("1410")},
            {new BigDecimal("35000"), new BigDecimal("0.25"), new BigDecimal("2660")},
            {new BigDecimal("55000"), new BigDecimal("0.30"), new BigDecimal("4410")},
            {new BigDecimal("80000"), new BigDecimal("0.35"), new BigDecimal("7160")},
            {new BigDecimal("9999999"), new BigDecimal("0.45"), new BigDecimal("15160")}
    };

    private void calculateSalary(SalaryRecord record, SalaryTemplate template) {
        if (template != null) {
            if (record.getBaseSalary() == null) record.setBaseSalary(template.getBaseSalary());
            if (record.getPostAllowance() == null) record.setPostAllowance(template.getPostAllowance());
            if (record.getPerformanceCoefficient() == null) record.setPerformanceCoefficient(template.getPerformanceCoefficient());
            if (record.getPerformanceBonus() == null) record.setPerformanceBonus(template.getPerformanceBonus());
        }

        if (record.getBaseSalary() == null) record.setBaseSalary(BigDecimal.ZERO);
        if (record.getPostAllowance() == null) record.setPostAllowance(BigDecimal.ZERO);
        if (record.getPerformanceCoefficient() == null) record.setPerformanceCoefficient(BigDecimal.ONE);
        if (record.getPerformanceBonus() == null) record.setPerformanceBonus(BigDecimal.ZERO);
        if (record.getOvertimePay() == null) record.setOvertimePay(BigDecimal.ZERO);
        if (record.getOtherAllowance() == null) record.setOtherAllowance(BigDecimal.ZERO);
        if (record.getOtherDeduction() == null) record.setOtherDeduction(BigDecimal.ZERO);

        BigDecimal siBase = template != null && template.getSocialInsuranceBase() != null
                ? template.getSocialInsuranceBase() : record.getBaseSalary();

        BigDecimal siPersonalRate = template != null && template.getSocialInsurancePersonalRate() != null
                ? template.getSocialInsurancePersonalRate() : new BigDecimal("0.1050");
        BigDecimal siCompanyRate = template != null && template.getSocialInsuranceCompanyRate() != null
                ? template.getSocialInsuranceCompanyRate() : new BigDecimal("0.2716");
        BigDecimal hfPersonalRate = template != null && template.getHousingFundPersonalRate() != null
                ? template.getHousingFundPersonalRate() : new BigDecimal("0.0700");
        BigDecimal hfCompanyRate = template != null && template.getHousingFundCompanyRate() != null
                ? template.getHousingFundCompanyRate() : new BigDecimal("0.0700");

        BigDecimal siPersonal = siBase.multiply(siPersonalRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal siCompany = siBase.multiply(siCompanyRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal hfPersonal = siBase.multiply(hfPersonalRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal hfCompany = siBase.multiply(hfCompanyRate).setScale(2, RoundingMode.HALF_UP);

        record.setSocialInsurancePersonal(siPersonal);
        record.setSocialInsuranceCompany(siCompany);
        record.setHousingFundPersonal(hfPersonal);
        record.setHousingFundCompany(hfCompany);

        BigDecimal actualBonus = record.getPerformanceBonus().multiply(record.getPerformanceCoefficient())
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal gross = record.getBaseSalary()
                .add(record.getPostAllowance())
                .add(actualBonus)
                .add(record.getOvertimePay())
                .add(record.getOtherAllowance());
        record.setPerformanceBonus(actualBonus);
        record.setGrossSalary(gross);

        BigDecimal taxableIncome = gross.subtract(siPersonal).subtract(hfPersonal).subtract(TAX_THRESHOLD);
        if (taxableIncome.compareTo(BigDecimal.ZERO) < 0) taxableIncome = BigDecimal.ZERO;
        BigDecimal incomeTax = calculateIncomeTax(taxableIncome);
        record.setIncomeTax(incomeTax);

        BigDecimal totalDeduction = siPersonal.add(hfPersonal).add(incomeTax).add(record.getOtherDeduction());
        record.setTotalDeduction(totalDeduction);

        BigDecimal netSalary = gross.subtract(totalDeduction);
        if (netSalary.compareTo(BigDecimal.ZERO) < 0) netSalary = BigDecimal.ZERO;
        record.setNetSalary(netSalary);

        BigDecimal totalCompanyCost = gross.add(siCompany).add(hfCompany);
        record.setTotalCompanyCost(totalCompanyCost);
    }

    private BigDecimal calculateIncomeTax(BigDecimal taxableIncome) {
        for (BigDecimal[] bracket : TAX_BRACKETS) {
            if (taxableIncome.compareTo(bracket[0]) <= 0) {
                return taxableIncome.multiply(bracket[1]).subtract(bracket[2])
                        .setScale(2, RoundingMode.HALF_UP);
            }
        }
        return BigDecimal.ZERO;
    }

    private String generateRecordNo() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "SA" + dateStr + uuid;
    }

    @Transactional
    public List<SalaryRecord> batchGenerate(SalaryBatchGenerateDTO dto) {
        List<Employee> employees;
        if (dto.getEmployeeIds() != null && !dto.getEmployeeIds().isEmpty()) {
            employees = employeeService.listByIds(dto.getEmployeeIds());
        } else if (dto.getDepartmentId() != null) {
            LambdaQueryWrapper<Employee> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Employee::getDepartmentId, dto.getDepartmentId())
                    .in(Employee::getStatus, 1, 2, 3);
            employees = employeeService.list(wrapper);
        } else {
            LambdaQueryWrapper<Employee> wrapper = new LambdaQueryWrapper<>();
            wrapper.in(Employee::getStatus, 1, 2, 3);
            employees = employeeService.list(wrapper);
        }

        SalaryTemplate forceTemplate = dto.getDefaultTemplateId() != null
                ? salaryTemplateService.getById(dto.getDefaultTemplateId()) : null;

        Map<String, SalaryTemplate> templateByJobLevel = new HashMap<>();
        SalaryTemplate fallbackTemplate = null;
        if (forceTemplate == null) {
            LambdaQueryWrapper<SalaryTemplate> tplWrapper = new LambdaQueryWrapper<>();
            tplWrapper.eq(SalaryTemplate::getEnabled, 1)
                    .orderByDesc(SalaryTemplate::getCreateTime);
            List<SalaryTemplate> allTpls = salaryTemplateService.list(tplWrapper);
            for (SalaryTemplate t : allTpls) {
                if (t.getJobLevel() != null) {
                    templateByJobLevel.putIfAbsent(t.getJobLevel().trim().toUpperCase(), t);
                }
                if (fallbackTemplate == null) {
                    fallbackTemplate = t;
                }
            }
        }

        List<SalaryRecord> results = new ArrayList<>();
        for (Employee emp : employees) {
            LambdaQueryWrapper<SalaryRecord> existWrapper = new LambdaQueryWrapper<>();
            existWrapper.eq(SalaryRecord::getEmployeeId, emp.getId())
                    .eq(SalaryRecord::getSalaryYear, dto.getSalaryYear())
                    .eq(SalaryRecord::getSalaryMonth, dto.getSalaryMonth());
            SalaryRecord existing = getOne(existWrapper);
            if (existing != null) {
                results.add(existing);
                continue;
            }

            Department dept = departmentService.getById(emp.getDepartmentId());

            SalaryTemplate template;
            if (forceTemplate != null) {
                template = forceTemplate;
            } else {
                String jobLevel = emp.getJobLevel() != null ? emp.getJobLevel().trim().toUpperCase() : null;
                template = (jobLevel != null) ? templateByJobLevel.get(jobLevel) : null;
                if (template == null) {
                    template = fallbackTemplate;
                }
            }

            SalaryRecord record = new SalaryRecord();
            record.setRecordNo(generateRecordNo());
            record.setEmployeeId(emp.getId());
            record.setEmployeeName(emp.getName());
            record.setDepartmentId(emp.getDepartmentId());
            record.setDepartmentName(dept != null ? dept.getName() : null);
            record.setJobLevel(emp.getJobLevel());
            record.setTemplateId(template != null ? template.getId() : null);
            record.setSalaryYear(dto.getSalaryYear());
            record.setSalaryMonth(dto.getSalaryMonth());
            record.setStatus(0);

            if (template != null) {
                record.setBaseSalary(template.getBaseSalary());
                record.setPostAllowance(template.getPostAllowance());
                record.setPerformanceCoefficient(template.getPerformanceCoefficient());
                record.setPerformanceBonus(template.getPerformanceBonus());
            }

            calculateSalary(record, template);
            save(record);
            results.add(record);
        }

        for (SalaryRecord r : results) {
            r.setStatusName(STATUS_NAMES[r.getStatus()]);
        }
        return results;
    }

    @Transactional
    public SalaryRecord applyTemplate(SalaryTemplateApplyDTO dto) {
        SalaryTemplate template = salaryTemplateService.getById(dto.getTemplateId());
        if (template == null) {
            throw new RuntimeException("薪资模板不存在");
        }

        List<Employee> employees;
        if (dto.getEmployeeIds() != null && !dto.getEmployeeIds().isEmpty()) {
            employees = employeeService.listByIds(dto.getEmployeeIds());
        } else if (dto.getDepartmentId() != null) {
            LambdaQueryWrapper<Employee> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Employee::getDepartmentId, dto.getDepartmentId())
                    .in(Employee::getStatus, 1, 2, 3);
            employees = employeeService.list(wrapper);
        } else {
            throw new RuntimeException("请指定套用员工或部门");
        }

        for (Employee emp : employees) {
            LambdaQueryWrapper<SalaryRecord> existWrapper = new LambdaQueryWrapper<>();
            existWrapper.eq(SalaryRecord::getEmployeeId, emp.getId())
                    .eq(SalaryRecord::getSalaryYear, dto.getSalaryYear())
                    .eq(SalaryRecord::getSalaryMonth, dto.getSalaryMonth());
            SalaryRecord record = getOne(existWrapper);
            if (record == null || record.getStatus() == 2) {
                continue;
            }

            record.setTemplateId(template.getId());
            record.setBaseSalary(template.getBaseSalary());
            record.setPostAllowance(template.getPostAllowance());
            record.setPerformanceCoefficient(template.getPerformanceCoefficient());
            record.setPerformanceBonus(template.getPerformanceBonus());

            calculateSalary(record, template);
            updateById(record);
        }

        return null;
    }

    public SalaryRecord getDetail(Long id, boolean desensitize) {
        SalaryRecord record = getById(id);
        if (record == null) return null;

        record.setStatusName(STATUS_NAMES[record.getStatus()]);
        record.setAdjustLogs(salaryAdjustLogService.listBySalaryRecordId(id));

        if (desensitize) {
            desensitizeRecord(record);
        }
        return record;
    }

    public Page<SalaryRecord> queryRecords(SalaryRecordQueryDTO query, boolean desensitize) {
        LambdaQueryWrapper<SalaryRecord> wrapper = new LambdaQueryWrapper<>();
        if (query.getEmployeeId() != null) {
            wrapper.eq(SalaryRecord::getEmployeeId, query.getEmployeeId());
        }
        if (query.getDepartmentId() != null) {
            wrapper.eq(SalaryRecord::getDepartmentId, query.getDepartmentId());
        }
        if (query.getSalaryYear() != null) {
            wrapper.eq(SalaryRecord::getSalaryYear, query.getSalaryYear());
        }
        if (query.getSalaryMonth() != null) {
            wrapper.eq(SalaryRecord::getSalaryMonth, query.getSalaryMonth());
        }
        if (query.getStatus() != null) {
            wrapper.eq(SalaryRecord::getStatus, query.getStatus());
        }
        if (query.getKeyword() != null && !query.getKeyword().isEmpty()) {
            wrapper.and(w -> w.like(SalaryRecord::getEmployeeName, query.getKeyword())
                    .or().like(SalaryRecord::getRecordNo, query.getKeyword()));
        }
        wrapper.orderByDesc(SalaryRecord::getSalaryYear, SalaryRecord::getSalaryMonth, SalaryRecord::getCreateTime);

        Page<SalaryRecord> page = page(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);

        for (SalaryRecord r : page.getRecords()) {
            r.setStatusName(STATUS_NAMES[r.getStatus()]);
            if (desensitize) {
                desensitizeRecord(r);
            }
        }
        return page;
    }

    public Page<SalaryRecord> getMyRecords(Long employeeId, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<SalaryRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SalaryRecord::getEmployeeId, employeeId)
                .eq(SalaryRecord::getStatus, 2)
                .orderByDesc(SalaryRecord::getSalaryYear, SalaryRecord::getSalaryMonth);

        Page<SalaryRecord> page = page(new Page<>(pageNum, pageSize), wrapper);
        for (SalaryRecord r : page.getRecords()) {
            r.setStatusName(STATUS_NAMES[r.getStatus()]);
            desensitizeRecord(r);
        }
        return page;
    }

    public SalaryRecord getMyRecordDetail(Long employeeId, Long id) {
        SalaryRecord record = getById(id);
        if (record == null || !record.getEmployeeId().equals(employeeId) || record.getStatus() != 2) {
            throw new RuntimeException("薪资单不存在或未发放");
        }
        record.setStatusName(STATUS_NAMES[record.getStatus()]);
        record.setAdjustLogs(null);
        desensitizeRecord(record);
        return record;
    }

    private void desensitizeRecord(SalaryRecord record) {
        if (record.getEmployeeName() != null && record.getEmployeeName().length() > 1) {
            String name = record.getEmployeeName();
            record.setEmployeeName(name.charAt(0) + "*".repeat(name.length() - 1));
        }
    }

    @Transactional
    public SalaryRecord adjustField(SalaryAdjustDTO dto, Long operatorId, String operatorName) {
        SalaryRecord record = getById(dto.getSalaryRecordId());
        if (record == null) {
            throw new RuntimeException("薪资记录不存在");
        }
        if (record.getStatus() == 2) {
            throw new RuntimeException("已发放的薪资单不可修改");
        }

        String fieldName = dto.getFieldName();
        BigDecimal oldValue = getFieldValue(record, fieldName);
        BigDecimal newValue = dto.getNewValue();

        if (oldValue == null && newValue == null) return record;
        if (oldValue != null && oldValue.compareTo(newValue) == 0) return record;

        setFieldValue(record, fieldName, newValue);

        SalaryTemplate template = record.getTemplateId() != null
                ? salaryTemplateService.getById(record.getTemplateId()) : null;
        calculateSalary(record, template);
        updateById(record);

        SalaryAdjustLog log = new SalaryAdjustLog();
        log.setSalaryRecordId(record.getId());
        log.setEmployeeId(record.getEmployeeId());
        log.setEmployeeName(record.getEmployeeName());
        log.setFieldName(fieldName);
        log.setFieldLabel(dto.getFieldLabel() != null ? dto.getFieldLabel() : FIELD_LABELS.getOrDefault(fieldName, fieldName));
        log.setOldValue(oldValue);
        log.setNewValue(newValue);
        log.setAdjustReason(dto.getAdjustReason());
        log.setOperatorId(operatorId);
        log.setOperatorName(operatorName);
        salaryAdjustLogMapper.insert(log);

        return getDetail(record.getId(), false);
    }

    private BigDecimal getFieldValue(SalaryRecord record, String fieldName) {
        return switch (fieldName) {
            case "baseSalary" -> record.getBaseSalary();
            case "postAllowance" -> record.getPostAllowance();
            case "performanceCoefficient" -> record.getPerformanceCoefficient();
            case "performanceBonus" -> record.getPerformanceBonus();
            case "overtimePay" -> record.getOvertimePay();
            case "otherAllowance" -> record.getOtherAllowance();
            case "socialInsurancePersonal" -> record.getSocialInsurancePersonal();
            case "socialInsuranceCompany" -> record.getSocialInsuranceCompany();
            case "housingFundPersonal" -> record.getHousingFundPersonal();
            case "housingFundCompany" -> record.getHousingFundCompany();
            case "incomeTax" -> record.getIncomeTax();
            case "otherDeduction" -> record.getOtherDeduction();
            default -> null;
        };
    }

    private void setFieldValue(SalaryRecord record, String fieldName, BigDecimal value) {
        switch (fieldName) {
            case "baseSalary" -> record.setBaseSalary(value);
            case "postAllowance" -> record.setPostAllowance(value);
            case "performanceCoefficient" -> record.setPerformanceCoefficient(value);
            case "performanceBonus" -> record.setPerformanceBonus(value);
            case "overtimePay" -> record.setOvertimePay(value);
            case "otherAllowance" -> record.setOtherAllowance(value);
            case "socialInsurancePersonal" -> record.setSocialInsurancePersonal(value);
            case "socialInsuranceCompany" -> record.setSocialInsuranceCompany(value);
            case "housingFundPersonal" -> record.setHousingFundPersonal(value);
            case "housingFundCompany" -> record.setHousingFundCompany(value);
            case "incomeTax" -> record.setIncomeTax(value);
            case "otherDeduction" -> record.setOtherDeduction(value);
        }
    }

    @Transactional
    public SalaryRecord confirmRecord(Long id) {
        SalaryRecord record = getById(id);
        if (record == null) {
            throw new RuntimeException("薪资记录不存在");
        }
        if (record.getStatus() != 0) {
            throw new RuntimeException("只有草稿状态可以确认");
        }
        record.setStatus(1);
        updateById(record);
        return getDetail(id, false);
    }

    @Transactional
    public List<SalaryRecord> batchConfirm(List<Long> ids) {
        List<SalaryRecord> results = new ArrayList<>();
        for (Long id : ids) {
            try {
                results.add(confirmRecord(id));
            } catch (Exception ignored) {
            }
        }
        return results;
    }

    @Transactional
    public SalaryRecord issueRecord(Long id) {
        SalaryRecord record = getById(id);
        if (record == null) {
            throw new RuntimeException("薪资记录不存在");
        }
        if (record.getStatus() != 1) {
            throw new RuntimeException("只有已确认状态可以发放");
        }
        record.setStatus(2);
        record.setIssueTime(LocalDateTime.now());
        updateById(record);
        return getDetail(id, false);
    }

    @Transactional
    public List<SalaryRecord> batchIssue(List<Long> ids) {
        List<SalaryRecord> results = new ArrayList<>();
        for (Long id : ids) {
            try {
                results.add(issueRecord(id));
            } catch (Exception ignored) {
            }
        }
        return results;
    }

    public SalaryCostReportDTO getCostReport(Integer year, Integer month, Long departmentId) {
        SalaryCostReportDTO report = new SalaryCostReportDTO();
        report.setSalaryYear(year);
        report.setSalaryMonth(month);

        LambdaQueryWrapper<SalaryRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SalaryRecord::getSalaryYear, year)
                .eq(SalaryRecord::getSalaryMonth, month)
                .eq(SalaryRecord::getStatus, 2);
        if (departmentId != null) {
            wrapper.eq(SalaryRecord::getDepartmentId, departmentId);
        }
        List<SalaryRecord> records = list(wrapper);

        BigDecimal currentTotalCost = sumField(records, SalaryRecord::getTotalCompanyCost);
        BigDecimal currentNetSalary = sumField(records, SalaryRecord::getNetSalary);
        int currentCount = records.size();

        report.setTotalCompanyCost(currentTotalCost);
        report.setTotalNetSalary(currentNetSalary);
        report.setTotalEmployeeCount(currentCount);
        report.setAvgNetSalary(currentCount > 0
                ? currentNetSalary.divide(BigDecimal.valueOf(currentCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);

        BigDecimal prevMonthTotal = getMonthlyTotalCost(year, month - 1 > 0 ? month - 1 : 12,
                month - 1 > 0 ? year : year - 1, departmentId);
        BigDecimal prevYearTotal = getMonthlyTotalCost(year - 1, month, year - 1, departmentId);

        report.setMomRate(calcGrowthRate(currentTotalCost, prevMonthTotal));
        report.setYoyRate(calcGrowthRate(currentTotalCost, prevYearTotal));

        Map<Long, List<SalaryRecord>> deptMap = records.stream()
                .filter(r -> r.getDepartmentId() != null)
                .collect(Collectors.groupingBy(SalaryRecord::getDepartmentId));

        List<SalaryCostSummaryDTO> deptSummaries = new ArrayList<>();
        for (Map.Entry<Long, List<SalaryRecord>> entry : deptMap.entrySet()) {
            SalaryCostSummaryDTO sum = buildSummary(entry.getValue(), year, month, entry.getKey(), departmentId);
            deptSummaries.add(sum);
        }
        deptSummaries.sort((a, b) -> b.getTotalCompanyCost().compareTo(a.getTotalCompanyCost()));
        report.setDepartmentSummaries(deptSummaries);

        List<SalaryCostSummaryDTO> trend = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            LambdaQueryWrapper<SalaryRecord> tw = new LambdaQueryWrapper<>();
            tw.eq(SalaryRecord::getSalaryYear, year)
                    .eq(SalaryRecord::getSalaryMonth, m)
                    .eq(SalaryRecord::getStatus, 2);
            if (departmentId != null) {
                tw.eq(SalaryRecord::getDepartmentId, departmentId);
            }
            List<SalaryRecord> monthRecords = list(tw);
            SalaryCostSummaryDTO ms = new SalaryCostSummaryDTO();
            ms.setSalaryYear(year);
            ms.setSalaryMonth(m);
            ms.setEmployeeCount(monthRecords.size());
            ms.setTotalGrossSalary(sumField(monthRecords, SalaryRecord::getGrossSalary));
            ms.setTotalNetSalary(sumField(monthRecords, SalaryRecord::getNetSalary));
            ms.setTotalCompanyCost(sumField(monthRecords, SalaryRecord::getTotalCompanyCost));
            ms.setTotalSocialInsuranceCompany(sumField(monthRecords, SalaryRecord::getSocialInsuranceCompany));
            ms.setTotalHousingFundCompany(sumField(monthRecords, SalaryRecord::getHousingFundCompany));
            trend.add(ms);
        }
        report.setMonthlyTrend(trend);

        return report;
    }

    private SalaryCostSummaryDTO buildSummary(List<SalaryRecord> records, Integer year, Integer month,
                                              Long deptId, Long filterDeptId) {
        SalaryCostSummaryDTO sum = new SalaryCostSummaryDTO();
        sum.setSalaryYear(year);
        sum.setSalaryMonth(month);
        sum.setDepartmentId(deptId);
        if (!records.isEmpty()) {
            sum.setDepartmentName(records.get(0).getDepartmentName());
        }
        sum.setEmployeeCount(records.size());
        sum.setTotalBaseSalary(sumField(records, SalaryRecord::getBaseSalary));
        sum.setTotalPostAllowance(sumField(records, SalaryRecord::getPostAllowance));
        sum.setTotalPerformanceBonus(sumField(records, SalaryRecord::getPerformanceBonus));
        sum.setTotalOvertimePay(sumField(records, SalaryRecord::getOvertimePay));
        sum.setTotalGrossSalary(sumField(records, SalaryRecord::getGrossSalary));
        sum.setTotalSocialInsurancePersonal(sumField(records, SalaryRecord::getSocialInsurancePersonal));
        sum.setTotalSocialInsuranceCompany(sumField(records, SalaryRecord::getSocialInsuranceCompany));
        sum.setTotalHousingFundPersonal(sumField(records, SalaryRecord::getHousingFundPersonal));
        sum.setTotalHousingFundCompany(sumField(records, SalaryRecord::getHousingFundCompany));
        sum.setTotalIncomeTax(sumField(records, SalaryRecord::getIncomeTax));
        sum.setTotalNetSalary(sumField(records, SalaryRecord::getNetSalary));
        sum.setTotalCompanyCost(sumField(records, SalaryRecord::getTotalCompanyCost));
        int cnt = records.size();
        sum.setAvgNetSalary(cnt > 0
                ? sum.getTotalNetSalary().divide(BigDecimal.valueOf(cnt), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);

        BigDecimal current = sum.getTotalCompanyCost();
        int pm = month - 1 > 0 ? month - 1 : 12;
        int py = month - 1 > 0 ? year : year - 1;
        sum.setMomRate(calcGrowthRate(current, getMonthlyTotalCost(pm > 0 ? pm : 12, pm > 0 ? pm : 12, py, deptId)));
        sum.setYoyRate(calcGrowthRate(current, getMonthlyTotalCost(year - 1, month, year - 1, deptId)));

        return sum;
    }

    private BigDecimal getMonthlyTotalCost(Integer year, Integer month, Integer actualYear, Long departmentId) {
        LambdaQueryWrapper<SalaryRecord> w = new LambdaQueryWrapper<>();
        w.eq(SalaryRecord::getSalaryYear, actualYear)
                .eq(SalaryRecord::getSalaryMonth, month)
                .eq(SalaryRecord::getStatus, 2);
        if (departmentId != null) {
            w.eq(SalaryRecord::getDepartmentId, departmentId);
        }
        return sumField(list(w), SalaryRecord::getTotalCompanyCost);
    }

    private BigDecimal calcGrowthRate(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return current.subtract(previous)
                .divide(previous, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }

    private BigDecimal sumField(List<SalaryRecord> records, java.util.function.Function<SalaryRecord, BigDecimal> getter) {
        return records.stream()
                .map(getter)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public List<Map<String, Object>> getFieldLabels() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, String> entry : FIELD_LABELS.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("field", entry.getKey());
            item.put("label", entry.getValue());
            result.add(item);
        }
        return result;
    }
}
