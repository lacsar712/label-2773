package com.example.employee.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.employee.dto.AttendanceMonthlyReportDTO;
import com.example.employee.entity.AttendanceMonthly;
import com.example.employee.entity.AttendanceRecord;
import com.example.employee.entity.Employee;
import com.example.employee.mapper.AttendanceMonthlyMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AttendanceMonthlyService extends ServiceImpl<AttendanceMonthlyMapper, AttendanceMonthly> {

    @Autowired
    private AttendanceRecordService attendanceRecordService;

    @Autowired
    private EmployeeService employeeService;

    @Transactional
    public AttendanceMonthly generateMonthlySummary(Long employeeId, int year, int month) {
        Employee employee = employeeService.getById(employeeId);
        if (employee == null) {
            throw new RuntimeException("员工不存在");
        }

        YearMonth ym = YearMonth.of(year, month);
        LocalDate startDate = ym.atDay(1);
        LocalDate endDate = ym.atEndOfMonth();

        LambdaQueryWrapper<AttendanceRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AttendanceRecord::getEmployeeId, employeeId)
                .between(AttendanceRecord::getAttendanceDate, startDate, endDate);
        List<AttendanceRecord> records = attendanceRecordService.list(wrapper);

        int workDays = calculateWorkDays(startDate, endDate);
        int actualDays = 0;
        int lateCount = 0;
        int earlyCount = 0;
        int absentCount = 0;
        int exceptionCount = 0;
        long totalWorkMinutes = 0;
        int totalLateMinutes = 0;
        int totalEarlyMinutes = 0;
        int leaveDays = 0;
        int businessTripDays = 0;

        for (AttendanceRecord r : records) {
            if (r.getStatus() == null) {
                continue;
            }
            int status = r.getStatus();
            if (status == 5) {
                leaveDays++;
                continue;
            }
            if (status == 6) {
                businessTripDays++;
                actualDays++;
                continue;
            }

            actualDays++;
            if (r.getWorkMinutes() != null) {
                totalWorkMinutes += r.getWorkMinutes();
            }
            if (status == 1 || status == 4) {
                lateCount++;
                totalLateMinutes += r.getLateMinutes() != null ? r.getLateMinutes() : 0;
            }
            if (status == 2 || status == 4) {
                earlyCount++;
                totalEarlyMinutes += r.getEarlyMinutes() != null ? r.getEarlyMinutes() : 0;
            }
            if (status == 3) {
                absentCount++;
            }
            if (r.getExceptionFlag() != null && r.getExceptionFlag() == 1) {
                exceptionCount++;
            }
        }

        BigDecimal exceptionRate = BigDecimal.ZERO;
        if (workDays > 0) {
            exceptionRate = BigDecimal.valueOf(exceptionCount)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(workDays), 2, RoundingMode.HALF_UP);
        }

        LambdaQueryWrapper<AttendanceMonthly> existWrapper = new LambdaQueryWrapper<>();
        existWrapper.eq(AttendanceMonthly::getEmployeeId, employeeId)
                .eq(AttendanceMonthly::getStatYear, year)
                .eq(AttendanceMonthly::getStatMonth, month);
        AttendanceMonthly summary = getOne(existWrapper);

        if (summary == null) {
            summary = new AttendanceMonthly();
            summary.setEmployeeId(employeeId);
            summary.setStatYear(year);
            summary.setStatMonth(month);
        }

        summary.setEmployeeName(employee.getName());
        summary.setDepartmentId(employee.getDepartmentId());
        if (employee.getDepartmentName() != null) {
            summary.setDepartmentName(employee.getDepartmentName());
        }
        summary.setWorkDays(workDays);
        summary.setActualDays(actualDays);
        summary.setLateCount(lateCount);
        summary.setEarlyCount(earlyCount);
        summary.setAbsentCount(absentCount);
        summary.setExceptionCount(exceptionCount);
        summary.setTotalWorkMinutes(totalWorkMinutes);
        summary.setTotalLateMinutes(totalLateMinutes);
        summary.setTotalEarlyMinutes(totalEarlyMinutes);
        summary.setExceptionRate(exceptionRate);
        summary.setSalaryDeduction(BigDecimal.ZERO);
        summary.setLeaveDays(leaveDays);
        summary.setBusinessTripDays(businessTripDays);

        saveOrUpdate(summary);
        return summary;
    }

    public Page<AttendanceMonthlyReportDTO> queryMonthlyReports(Long employeeId, Long departmentId,
                                                                 Integer year, Integer month,
                                                                 Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<AttendanceMonthly> wrapper = new LambdaQueryWrapper<>();
        if (employeeId != null) {
            wrapper.eq(AttendanceMonthly::getEmployeeId, employeeId);
        }
        if (departmentId != null) {
            wrapper.eq(AttendanceMonthly::getDepartmentId, departmentId);
        }
        if (year != null) {
            wrapper.eq(AttendanceMonthly::getStatYear, year);
        }
        if (month != null) {
            wrapper.eq(AttendanceMonthly::getStatMonth, month);
        }
        wrapper.orderByDesc(AttendanceMonthly::getStatYear)
                .orderByDesc(AttendanceMonthly::getStatMonth);

        Page<AttendanceMonthly> page = page(new Page<>(pageNum, pageSize), wrapper);
        Page<AttendanceMonthlyReportDTO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream()
                .map(this::convertToReportDTO)
                .collect(Collectors.toList()));
        return result;
    }

    public AttendanceMonthlyReportDTO getEmployeeMonthlyReport(Long employeeId, int year, int month) {
        LambdaQueryWrapper<AttendanceMonthly> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AttendanceMonthly::getEmployeeId, employeeId)
                .eq(AttendanceMonthly::getStatYear, year)
                .eq(AttendanceMonthly::getStatMonth, month);
        AttendanceMonthly monthly = getOne(wrapper);
        if (monthly == null) {
            monthly = generateMonthlySummary(employeeId, year, month);
        }
        return convertToReportDTO(monthly);
    }

    public List<AttendanceMonthlyReportDTO> getDepartmentSummary(Long departmentId, int year, int month) {
        List<Employee> employees;
        if (departmentId != null) {
            employees = employeeService.list(new LambdaQueryWrapper<Employee>()
                    .eq(Employee::getDepartmentId, departmentId));
        } else {
            employees = employeeService.list();
        }

        List<AttendanceMonthlyReportDTO> result = new ArrayList<>();
        for (Employee emp : employees) {
            try {
                result.add(getEmployeeMonthlyReport(emp.getId(), year, month));
            } catch (Exception ignored) {
            }
        }
        return result;
    }

    public Map<String, Object> getDepartmentAggregatedStats(Long departmentId, int year, int month) {
        List<AttendanceMonthlyReportDTO> reports = getDepartmentSummary(departmentId, year, month);
        int totalEmployees = reports.size();
        int totalLate = reports.stream().mapToInt(r -> r.getLateCount() != null ? r.getLateCount() : 0).sum();
        int totalEarly = reports.stream().mapToInt(r -> r.getEarlyCount() != null ? r.getEarlyCount() : 0).sum();
        int totalAbsent = reports.stream().mapToInt(r -> r.getAbsentCount() != null ? r.getAbsentCount() : 0).sum();
        int totalException = reports.stream().mapToInt(r -> r.getExceptionCount() != null ? r.getExceptionCount() : 0).sum();

        BigDecimal avgExceptionRate = BigDecimal.ZERO;
        if (totalEmployees > 0) {
            BigDecimal sum = reports.stream()
                    .map(r -> r.getExceptionRate() != null ? r.getExceptionRate() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            avgExceptionRate = sum.divide(BigDecimal.valueOf(totalEmployees), 2, RoundingMode.HALF_UP);
        }

        Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("totalEmployees", totalEmployees);
        stats.put("totalLateCount", totalLate);
        stats.put("totalEarlyCount", totalEarly);
        stats.put("totalAbsentCount", totalAbsent);
        stats.put("totalExceptionCount", totalException);
        stats.put("avgExceptionRate", avgExceptionRate);
        stats.put("year", year);
        stats.put("month", month);
        return stats;
    }

    private AttendanceMonthlyReportDTO convertToReportDTO(AttendanceMonthly m) {
        AttendanceMonthlyReportDTO dto = new AttendanceMonthlyReportDTO();
        dto.setEmployeeId(m.getEmployeeId());
        dto.setEmployeeName(m.getEmployeeName());
        dto.setDepartmentId(m.getDepartmentId());
        dto.setDepartmentName(m.getDepartmentName());
        dto.setStatYear(m.getStatYear());
        dto.setStatMonth(m.getStatMonth());
        dto.setWorkDays(m.getWorkDays());
        dto.setActualDays(m.getActualDays());
        dto.setLateCount(m.getLateCount());
        dto.setEarlyCount(m.getEarlyCount());
        dto.setAbsentCount(m.getAbsentCount());
        dto.setExceptionCount(m.getExceptionCount());
        dto.setExceptionRate(m.getExceptionRate());
        return dto;
    }

    private int calculateWorkDays(LocalDate start, LocalDate end) {
        int count = 0;
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            int dow = d.getDayOfWeek().getValue();
            if (dow <= 5) {
                count++;
            }
        }
        return count;
    }
}
