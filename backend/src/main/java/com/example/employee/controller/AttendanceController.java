package com.example.employee.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.employee.common.Result;
import com.example.employee.dto.*;
import com.example.employee.entity.AttendanceMakeUp;
import com.example.employee.entity.AttendanceMonthly;
import com.example.employee.entity.AttendanceRecord;
import com.example.employee.entity.AttendanceRule;
import com.example.employee.service.*;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/attendance")
@CrossOrigin(origins = "*")
public class AttendanceController {

    @Autowired
    private AttendanceRecordService attendanceRecordService;

    @Autowired
    private AttendanceRuleService attendanceRuleService;

    @Autowired
    private AttendanceMakeUpService attendanceMakeUpService;

    @Autowired
    private AttendanceExceptionService attendanceExceptionService;

    @Autowired
    private AttendanceMonthlyService attendanceMonthlyService;

    // ==================== 打卡相关 ====================

    @PostMapping("/clock-in")
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE')")
    public Result<ClockInResponseDTO> clockIn(@RequestBody @Valid ClockInRequestDTO request) {
        return Result.success(attendanceRecordService.checkIn(request));
    }

    @PostMapping("/clock-out")
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE')")
    public Result<ClockInResponseDTO> clockOut(@RequestBody @Valid ClockInRequestDTO request) {
        return Result.success(attendanceRecordService.checkOut(request));
    }

    @GetMapping("/record/today")
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE')")
    public Result<AttendanceRecord> getTodayRecord(@RequestParam Long employeeId) {
        AttendanceRecord record = attendanceRecordService.getEmployeeRecordForDate(employeeId, LocalDate.now());
        return Result.success(record);
    }

    @GetMapping("/record/{employeeId}/{date}")
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE')")
    public Result<AttendanceRecord> getRecordByDate(@PathVariable Long employeeId, @PathVariable String date) {
        LocalDate d = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        return Result.success(attendanceRecordService.getEmployeeRecordForDate(employeeId, d));
    }

    @GetMapping("/calendar")
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE')")
    public Result<List<AttendanceCalendarDTO>> getCalendarData(
            @RequestParam Long employeeId,
            @RequestParam Integer year,
            @RequestParam Integer month) {
        return Result.success(attendanceRecordService.getMonthlyCalendar(employeeId, year, month));
    }

    @GetMapping("/records")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public Result<Page<AttendanceRecord>> queryRecords(@ModelAttribute AttendanceRecordQueryDTO query) {
        return Result.success(attendanceRecordService.queryRecords(query));
    }

    @GetMapping("/records/my")
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE')")
    public Result<Page<AttendanceRecord>> queryMyRecords(
            @RequestParam Long employeeId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        AttendanceRecordQueryDTO query = new AttendanceRecordQueryDTO();
        query.setEmployeeId(employeeId);
        query.setPageNum(pageNum);
        query.setPageSize(pageSize);
        if (startDate != null) query.setStartDate(LocalDate.parse(startDate));
        if (endDate != null) query.setEndDate(LocalDate.parse(endDate));
        if (status != null) query.setStatus(status);
        return Result.success(attendanceRecordService.queryRecords(query));
    }

    @GetMapping("/records/export")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public void exportRecords(@ModelAttribute AttendanceRecordQueryDTO query, HttpServletResponse response) throws IOException {
        query.setPageNum(1);
        query.setPageSize(10000);
        Page<AttendanceRecord> page = attendanceRecordService.queryRecords(query);
        List<AttendanceRecord> records = page.getRecords();

        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=attendance_records_" + LocalDate.now() + ".csv");
        OutputStreamWriter writer = new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8);
        writer.write('\uFEFF');
        writer.write("员工姓名,部门,考勤日期,上班时间,下班时间,工作时长(分钟),迟到(分钟),早退(分钟),状态\n");

        String[] statusText = {"正常", "迟到", "早退", "缺卡", "迟到早退", "请假", "出差"};
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        for (AttendanceRecord r : records) {
            writer.write((r.getEmployeeName() != null ? r.getEmployeeName() : "") + ",");
            writer.write((r.getDepartmentName() != null ? r.getDepartmentName() : "") + ",");
            writer.write(r.getAttendanceDate() + ",");
            writer.write((r.getCheckInTime() != null ? r.getCheckInTime().format(dtf) : "") + ",");
            writer.write((r.getCheckOutTime() != null ? r.getCheckOutTime().format(dtf) : "") + ",");
            writer.write((r.getWorkMinutes() != null ? r.getWorkMinutes() : 0) + ",");
            writer.write((r.getLateMinutes() != null ? r.getLateMinutes() : 0) + ",");
            writer.write((r.getEarlyMinutes() != null ? r.getEarlyMinutes() : 0) + ",");
            int s = r.getStatus() != null ? r.getStatus() : 0;
            writer.write(statusText[s >= 0 && s < statusText.length ? s : 0] + "\n");
        }
        writer.flush();
        writer.close();
    }

    // ==================== 考勤规则 ====================

    @GetMapping("/rules")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public Result<List<AttendanceRule>> getRules() {
        return Result.success(attendanceRuleService.listEnabledRules());
    }

    @GetMapping("/rules/all")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Page<AttendanceRule>> queryRules(@ModelAttribute AttendanceRuleQueryDTO query) {
        return Result.success(attendanceRuleService.queryRules(query));
    }

    @GetMapping("/rules/default")
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE')")
    public Result<AttendanceRule> getDefaultRule() {
        return Result.success(attendanceRuleService.getDefaultRule());
    }

    @GetMapping("/rules/employee/{employeeId}")
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE')")
    public Result<AttendanceRule> getEmployeeRule(@PathVariable Long employeeId) {
        return Result.success(attendanceRuleService.getEmployeeTodayRule(employeeId));
    }

    @PostMapping("/rules")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Boolean> createRule(@RequestBody @Valid AttendanceRule rule) {
        return Result.success(attendanceRuleService.createRule(rule));
    }

    @PutMapping("/rules")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Boolean> updateRule(@RequestBody @Valid AttendanceRule rule) {
        return Result.success(attendanceRuleService.updateRule(rule));
    }

    @DeleteMapping("/rules/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Boolean> deleteRule(@PathVariable Long id) {
        return Result.success(attendanceRuleService.deleteRule(id));
    }

    // ==================== 补卡申请 ====================

    @PostMapping("/makeup")
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE')")
    public Result<AttendanceMakeUp> submitMakeUp(@RequestBody @Valid AttendanceMakeUpRequestDTO request) {
        return Result.success(attendanceMakeUpService.submitMakeUp(request));
    }

    @GetMapping("/makeup/my")
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE')")
    public Result<Page<AttendanceMakeUp>> getMyMakeUpList(
            @RequestParam Long employeeId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(attendanceMakeUpService.getPersonalHistory(employeeId, pageNum, pageSize));
    }

    @GetMapping("/makeup/pending")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public Result<Page<AttendanceMakeUp>> getPendingApprovals(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        if (departmentId != null) {
            return Result.success(attendanceMakeUpService.getDepartmentPendingList(departmentId, pageNum, pageSize));
        }
        return Result.success(attendanceMakeUpService.getPendingList(pageNum, pageSize));
    }

    @PostMapping("/makeup/approve")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public Result<AttendanceMakeUp> approveMakeUp(@RequestBody @Valid AttendanceMakeUpApprovalDTO approval) {
        return Result.success(attendanceMakeUpService.approveMakeUp(approval));
    }

    // ==================== 月度报表 ====================

    @PostMapping("/monthly/generate")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public Result<AttendanceMonthly> generateMonthly(
            @RequestParam Long employeeId,
            @RequestParam Integer year,
            @RequestParam Integer month) {
        return Result.success(attendanceMonthlyService.generateMonthlySummary(employeeId, year, month));
    }

    @GetMapping("/monthly/reports")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public Result<Page<AttendanceMonthlyReportDTO>> getMonthlyReports(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(attendanceMonthlyService.queryMonthlyReports(employeeId, departmentId, year, month, pageNum, pageSize));
    }

    @GetMapping("/monthly/employee/{employeeId}")
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE')")
    public Result<AttendanceMonthlyReportDTO> getEmployeeMonthlyReport(
            @PathVariable Long employeeId,
            @RequestParam Integer year,
            @RequestParam Integer month) {
        return Result.success(attendanceMonthlyService.getEmployeeMonthlyReport(employeeId, year, month));
    }

    @GetMapping("/monthly/department/stats")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public Result<Map<String, Object>> getDepartmentStats(
            @RequestParam(required = false) Long departmentId,
            @RequestParam Integer year,
            @RequestParam Integer month) {
        return Result.success(attendanceMonthlyService.getDepartmentAggregatedStats(departmentId, year, month));
    }

    @GetMapping("/monthly/export")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public void exportMonthlyReports(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            HttpServletResponse response) throws IOException {

        Page<AttendanceMonthlyReportDTO> page = attendanceMonthlyService.queryMonthlyReports(
                employeeId, departmentId, year, month, 1, 10000);
        List<AttendanceMonthlyReportDTO> reports = page.getRecords();

        response.setContentType("text/csv;charset=UTF-8");
        String fileName = "attendance_monthly_" + (year != null ? year : "") + (month != null ? String.format("%02d", month) : "") + "_" + LocalDate.now() + ".csv";
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
        OutputStreamWriter writer = new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8);
        writer.write('\uFEFF');
        writer.write("员工姓名,部门,年份,月份,应出勤天数,实际出勤天数,迟到次数,早退次数,缺卡次数,异常次数,异常率(%)\n");

        for (AttendanceMonthlyReportDTO r : reports) {
            writer.write((r.getEmployeeName() != null ? r.getEmployeeName() : "") + ",");
            writer.write((r.getDepartmentName() != null ? r.getDepartmentName() : "") + ",");
            writer.write(r.getStatYear() + ",");
            writer.write(r.getStatMonth() + ",");
            writer.write(r.getWorkDays() + ",");
            writer.write(r.getActualDays() + ",");
            writer.write(r.getLateCount() + ",");
            writer.write(r.getEarlyCount() + ",");
            writer.write(r.getAbsentCount() + ",");
            writer.write(r.getExceptionCount() + ",");
            BigDecimal rate = r.getExceptionRate();
            writer.write((rate != null ? rate : BigDecimal.ZERO) + "%\n");
        }
        writer.flush();
        writer.close();
    }
}
