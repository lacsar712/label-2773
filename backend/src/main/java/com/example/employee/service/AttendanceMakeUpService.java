package com.example.employee.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.employee.context.UserContext;
import com.example.employee.dto.AttendanceMakeUpApprovalDTO;
import com.example.employee.dto.AttendanceMakeUpRequestDTO;
import com.example.employee.entity.AttendanceMakeUp;
import com.example.employee.entity.AttendanceRecord;
import com.example.employee.entity.Department;
import com.example.employee.entity.Employee;
import com.example.employee.exception.BusinessException;
import com.example.employee.mapper.AttendanceMakeUpMapper;
import com.example.employee.utils.IpUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AttendanceMakeUpService extends ServiceImpl<AttendanceMakeUpMapper, AttendanceMakeUp> {

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private AttendanceRecordService attendanceRecordService;

    @Autowired
    private HttpServletRequest request;

    @Transactional
    public AttendanceMakeUp submitMakeUp(AttendanceMakeUpRequestDTO dto) {
        validateMakeUpPermission(dto.getEmployeeId());

        Employee employee = employeeService.getById(dto.getEmployeeId());
        if (employee == null) {
            throw new RuntimeException("员工不存在");
        }

        Department dept = departmentService.getById(employee.getDepartmentId());

        AttendanceMakeUp makeup = new AttendanceMakeUp();
        makeup.setEmployeeId(dto.getEmployeeId());
        makeup.setEmployeeName(employee.getName());
        makeup.setDepartmentId(employee.getDepartmentId());
        makeup.setDepartmentName(dept != null ? dept.getName() : null);
        makeup.setAttendanceDate(dto.getAttendanceDate());
        makeup.setMakeupType(dto.getMakeupType());
        makeup.setMakeupTime(dto.getMakeupTime());
        makeup.setReason(dto.getReason());
        makeup.setLocation(dto.getLocation());
        String clientIp = dto.getIpAddress() != null ? dto.getIpAddress() : IpUtil.getIpAddr(request);
        makeup.setIpAddress(clientIp);
        makeup.setLng(dto.getLng());
        makeup.setLat(dto.getLat());
        makeup.setStatus(0);
        save(makeup);
        return makeup;
    }

    private void validateMakeUpPermission(Long targetEmployeeId) {
        var currentUser = UserContext.getCurrentUser();
        if (currentUser == null) {
            throw new BusinessException(401, "未登录");
        }
        String roleCode = currentUser.getRoleCode();
        if (!"ADMIN".equals(roleCode) && !"HR".equals(roleCode)) {
            Long myEmployeeId = currentUser.getEmployeeId();
            if (myEmployeeId == null) {
                throw new BusinessException(403, "当前用户未关联员工信息");
            }
            if (!myEmployeeId.equals(targetEmployeeId)) {
                throw new BusinessException(403, "无权为其他员工提交补卡申请");
            }
        }
    }

    @Transactional
    public AttendanceMakeUp approveMakeUp(AttendanceMakeUpApprovalDTO approval) {
        AttendanceMakeUp makeup = getById(approval.getId());
        if (makeup == null) {
            throw new RuntimeException("补卡申请不存在");
        }
        if (makeup.getStatus() != 0) {
            throw new RuntimeException("补卡申请已审批");
        }

        makeup.setStatus(approval.getStatus());
        makeup.setApprovalRemark(approval.getApprovalRemark());
        makeup.setApprovalTime(LocalDateTime.now());
        updateById(makeup);

        if (approval.getStatus() == 1) {
            applyMakeUpToRecord(makeup);
        }
        return makeup;
    }

    public Page<AttendanceMakeUp> getPendingList(Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<AttendanceMakeUp> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AttendanceMakeUp::getStatus, 0)
                .orderByAsc(AttendanceMakeUp::getCreateTime);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    public Page<AttendanceMakeUp> getPersonalHistory(Long employeeId, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<AttendanceMakeUp> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AttendanceMakeUp::getEmployeeId, employeeId)
                .orderByDesc(AttendanceMakeUp::getCreateTime);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    public Page<AttendanceMakeUp> getDepartmentPendingList(Long departmentId, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<AttendanceMakeUp> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AttendanceMakeUp::getStatus, 0);
        if (departmentId != null) {
            wrapper.eq(AttendanceMakeUp::getDepartmentId, departmentId);
        }
        wrapper.orderByAsc(AttendanceMakeUp::getCreateTime);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    private void applyMakeUpToRecord(AttendanceMakeUp makeup) {
        AttendanceRecord record = attendanceRecordService.getEmployeeRecordForDate(
                makeup.getEmployeeId(), makeup.getAttendanceDate());

        if (record == null) {
            record = new AttendanceRecord();
            record.setEmployeeId(makeup.getEmployeeId());
            record.setAttendanceDate(makeup.getAttendanceDate());
            attendanceRecordService.save(record);
        }

        if (makeup.getMakeupType() == 1) {
            record.setCheckInTime(makeup.getMakeupTime());
            record.setCheckInLocation(makeup.getLocation());
            record.setCheckInIp(makeup.getIpAddress());
            record.setCheckInLng(makeup.getLng());
            record.setCheckInLat(makeup.getLat());
            record.setCheckInType(2);
            record.setMakeupInId(makeup.getId());
        } else if (makeup.getMakeupType() == 2) {
            record.setCheckOutTime(makeup.getMakeupTime());
            record.setCheckOutLocation(makeup.getLocation());
            record.setCheckOutIp(makeup.getIpAddress());
            record.setCheckOutLng(makeup.getLng());
            record.setCheckOutLat(makeup.getLat());
            record.setCheckOutType(2);
            record.setMakeupOutId(makeup.getId());
        }

        attendanceRecordService.calculateWorkMinutes(record);
        attendanceRecordService.updateById(record);
    }
}
