package com.example.employee.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.employee.context.UserContext;
import com.example.employee.dto.AttendanceCalendarDTO;
import com.example.employee.dto.AttendanceRecordQueryDTO;
import com.example.employee.dto.ClockInRequestDTO;
import com.example.employee.dto.ClockInResponseDTO;
import com.example.employee.entity.AttendanceRecord;
import com.example.employee.entity.AttendanceRule;
import com.example.employee.entity.Employee;
import com.example.employee.exception.BusinessException;
import com.example.employee.mapper.AttendanceRecordMapper;
import com.example.employee.utils.IpUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AttendanceRecordService extends ServiceImpl<AttendanceRecordMapper, AttendanceRecord> {

    @Autowired
    private AttendanceRuleService attendanceRuleService;

    @Autowired
    private AttendanceExceptionService attendanceExceptionService;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private HttpServletRequest request;

    @Transactional
    public ClockInResponseDTO checkIn(ClockInRequestDTO dto) {
        validateClockPermission(dto.getEmployeeId());

        LocalDate today = LocalDate.now();
        AttendanceRecord record = getOrCreateRecord(dto.getEmployeeId(), today);

        if (record.getCheckInTime() != null) {
            return new ClockInResponseDTO(false, "今日已上班打卡", record);
        }

        AttendanceRule rule = attendanceRuleService.getEmployeeTodayRule(dto.getEmployeeId());
        record.setRuleId(rule.getId());

        LocalDateTime now = LocalDateTime.now();
        record.setCheckInTime(now);
        String clientIp = dto.getIpAddress() != null ? dto.getIpAddress() : IpUtil.getIpAddr(request);
        record.setCheckInIp(clientIp);
        record.setCheckInLocation(dto.getLocation());
        record.setCheckInLng(dto.getLng());
        record.setCheckInLat(dto.getLat());
        record.setCheckInType(1);

        calculateStatus(record, rule);
        updateById(record);

        if (record.getExceptionFlag() == 1) {
            attendanceExceptionService.generateException(record);
        }

        String msg = record.getLateMinutes() > 0 ? "上班打卡成功，迟到" + record.getLateMinutes() + "分钟" : "上班打卡成功";
        return new ClockInResponseDTO(true, msg, record);
    }

    @Transactional
    public ClockInResponseDTO checkOut(ClockInRequestDTO dto) {
        validateClockPermission(dto.getEmployeeId());

        LocalDate today = LocalDate.now();
        AttendanceRecord record = getById(getRecordId(dto.getEmployeeId(), today));

        if (record == null) {
            return new ClockInResponseDTO(false, "未找到今日上班打卡记录", null);
        }
        if (record.getCheckOutTime() != null) {
            return new ClockInResponseDTO(false, "今日已下班打卡", record);
        }

        AttendanceRule rule = attendanceRuleService.getById(record.getRuleId());
        if (rule == null) {
            rule = attendanceRuleService.getEmployeeTodayRule(dto.getEmployeeId());
        }

        LocalDateTime now = LocalDateTime.now();
        record.setCheckOutTime(now);
        String clientIp = dto.getIpAddress() != null ? dto.getIpAddress() : IpUtil.getIpAddr(request);
        record.setCheckOutIp(clientIp);
        record.setCheckOutLocation(dto.getLocation());
        record.setCheckOutLng(dto.getLng());
        record.setCheckOutLat(dto.getLat());
        record.setCheckOutType(1);

        calculateWorkMinutes(record);
        calculateStatus(record, rule);
        updateById(record);

        if (record.getExceptionFlag() == 1) {
            attendanceExceptionService.generateException(record);
        }

        String msg = record.getEarlyMinutes() > 0 ? "下班打卡成功，早退" + record.getEarlyMinutes() + "分钟" : "下班打卡成功";
        return new ClockInResponseDTO(true, msg, record);
    }

    private void validateClockPermission(Long targetEmployeeId) {
        var currentUser = UserContext.getCurrentUser();
        if (currentUser == null) {
            throw new BusinessException(401, "未登录");
        }
        String roleCode = currentUser.getRoleCode();
        // 管理员和 HR 可以代打卡，普通员工只能给自己打卡
        if (!"ADMIN".equals(roleCode) && !"HR".equals(roleCode)) {
            Long myEmployeeId = currentUser.getEmployeeId();
            if (myEmployeeId == null) {
                throw new BusinessException(403, "当前用户未关联员工信息，无法打卡");
            }
            if (!myEmployeeId.equals(targetEmployeeId)) {
                throw new BusinessException(403, "无权为其他员工打卡");
            }
        }
    }

    public AttendanceRecord getEmployeeRecordForDate(Long employeeId, LocalDate date) {
        return getOne(new LambdaQueryWrapper<AttendanceRecord>()
                .eq(AttendanceRecord::getEmployeeId, employeeId)
                .eq(AttendanceRecord::getAttendanceDate, date));
    }

    public List<AttendanceCalendarDTO> getMonthlyCalendar(Long employeeId, int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.plusMonths(1).minusDays(1);

        List<AttendanceRecord> records = list(new LambdaQueryWrapper<AttendanceRecord>()
                .eq(AttendanceRecord::getEmployeeId, employeeId)
                .between(AttendanceRecord::getAttendanceDate, start, end));

        Map<LocalDate, AttendanceRecord> recordMap = records.stream()
                .collect(Collectors.toMap(AttendanceRecord::getAttendanceDate, r -> r));

        List<AttendanceCalendarDTO> calendar = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            AttendanceCalendarDTO dto = new AttendanceCalendarDTO();
            dto.setAttendanceDate(d);
            AttendanceRecord r = recordMap.get(d);
            if (r != null) {
                dto.setStatus(r.getStatus());
                dto.setLateMinutes(r.getLateMinutes());
                dto.setEarlyMinutes(r.getEarlyMinutes());
            } else {
                dto.setStatus(null);
                dto.setLateMinutes(0);
                dto.setEarlyMinutes(0);
            }
            calendar.add(dto);
        }
        return calendar;
    }

    public Page<AttendanceRecord> queryRecords(AttendanceRecordQueryDTO query) {
        LambdaQueryWrapper<AttendanceRecord> wrapper = new LambdaQueryWrapper<>();
        if (query.getEmployeeId() != null) {
            wrapper.eq(AttendanceRecord::getEmployeeId, query.getEmployeeId());
        }
        if (query.getStartDate() != null) {
            wrapper.ge(AttendanceRecord::getAttendanceDate, query.getStartDate());
        }
        if (query.getEndDate() != null) {
            wrapper.le(AttendanceRecord::getAttendanceDate, query.getEndDate());
        }
        if (query.getStatus() != null) {
            wrapper.eq(AttendanceRecord::getStatus, query.getStatus());
        }
        if (query.getExceptionFlag() != null) {
            wrapper.eq(AttendanceRecord::getExceptionFlag, query.getExceptionFlag());
        }

        if (query.getDepartmentId() != null) {
            List<Employee> employees = employeeService.list(
                    new LambdaQueryWrapper<Employee>().eq(Employee::getDepartmentId, query.getDepartmentId()));
            if (employees.isEmpty()) {
                return new Page<>(query.getPageNum(), query.getPageSize());
            }
            List<Long> empIds = employees.stream().map(Employee::getId).collect(Collectors.toList());
            wrapper.in(AttendanceRecord::getEmployeeId, empIds);
        }

        wrapper.orderByDesc(AttendanceRecord::getAttendanceDate);
        Page<AttendanceRecord> page = page(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);

        enrichRecordsWithEmployeeInfo(page.getRecords());
        return page;
    }

    public int calculateWorkMinutes(AttendanceRecord record) {
        if (record.getCheckInTime() == null || record.getCheckOutTime() == null) {
            record.setWorkMinutes(0);
            return 0;
        }
        int minutes = (int) Duration.between(record.getCheckInTime(), record.getCheckOutTime()).toMinutes();
        record.setWorkMinutes(Math.max(0, minutes));
        return record.getWorkMinutes();
    }

    public void calculateStatus(AttendanceRecord record, AttendanceRule rule) {
        boolean hasIn = record.getCheckInTime() != null;
        boolean hasOut = record.getCheckOutTime() != null;
        int lateMinutes = 0;
        int earlyMinutes = 0;
        int status = 0;
        boolean exception = false;

        if (rule != null) {
            if (hasIn && rule.getWorkStartTime() != null) {
                LocalTime inTime = record.getCheckInTime().toLocalTime();
                LocalTime workStart = rule.getWorkStartTime();
                int grace = rule.getLateGraceMinutes() != null ? rule.getLateGraceMinutes() : 0;
                int diff = (int) Duration.between(workStart, inTime).toMinutes();
                if (diff > grace) {
                    lateMinutes = diff - grace;
                    exception = true;
                }
            }
            if (hasOut && rule.getWorkEndTime() != null) {
                LocalTime outTime = record.getCheckOutTime().toLocalTime();
                LocalTime workEnd = rule.getWorkEndTime();
                int grace = rule.getEarlyGraceMinutes() != null ? rule.getEarlyGraceMinutes() : 0;
                int diff = (int) Duration.between(outTime, workEnd).toMinutes();
                if (diff > grace) {
                    earlyMinutes = diff - grace;
                    exception = true;
                }
            }
        }

        record.setLateMinutes(lateMinutes);
        record.setEarlyMinutes(earlyMinutes);

        if (!hasIn && !hasOut) {
            status = 3;
            exception = true;
        } else if (!hasIn) {
            status = 3;
            exception = true;
        } else if (!hasOut) {
            if (lateMinutes > 0) {
                status = 1;
            } else {
                status = 0;
            }
        } else {
            if (lateMinutes > 0 && earlyMinutes > 0) {
                status = 4;
            } else if (lateMinutes > 0) {
                status = 1;
            } else if (earlyMinutes > 0) {
                status = 2;
            } else {
                status = 0;
            }
        }

        record.setStatus(status);
        record.setExceptionFlag(exception ? 1 : 0);
    }

    private AttendanceRecord getOrCreateRecord(Long employeeId, LocalDate date) {
        AttendanceRecord record = getEmployeeRecordForDate(employeeId, date);
        if (record == null) {
            record = new AttendanceRecord();
            record.setEmployeeId(employeeId);
            record.setAttendanceDate(date);
            record.setStatus(0);
            record.setExceptionFlag(0);
            record.setWorkMinutes(0);
            record.setLateMinutes(0);
            record.setEarlyMinutes(0);
            save(record);
        }
        return record;
    }

    private Long getRecordId(Long employeeId, LocalDate date) {
        AttendanceRecord record = getEmployeeRecordForDate(employeeId, date);
        return record != null ? record.getId() : null;
    }

    private void enrichRecordsWithEmployeeInfo(List<AttendanceRecord> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        List<Long> empIds = records.stream()
                .map(AttendanceRecord::getEmployeeId)
                .distinct()
                .collect(Collectors.toList());
        List<Employee> employees = employeeService.listByIds(empIds);
        Map<Long, Employee> empMap = employees.stream()
                .collect(Collectors.toMap(Employee::getId, e -> e));
        List<Long> deptIds = employees.stream()
                .map(Employee::getDepartmentId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> deptMap = departmentService.listByIds(deptIds).stream()
                .collect(Collectors.toMap(d -> d.getId(), d -> d.getName()));

        for (AttendanceRecord r : records) {
            Employee emp = empMap.get(r.getEmployeeId());
            if (emp != null) {
                r.setEmployeeName(emp.getName());
                r.setDepartmentId(emp.getDepartmentId());
                r.setDepartmentName(deptMap.get(emp.getDepartmentId()));
            }
        }
    }
}
