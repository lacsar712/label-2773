package com.example.employee.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.employee.dto.AttendanceMakeUpApprovalDTO;
import com.example.employee.dto.AttendanceMakeUpRequestDTO;
import com.example.employee.entity.AttendanceMakeUp;
import com.example.employee.entity.AttendanceRecord;
import com.example.employee.entity.Employee;
import com.example.employee.mapper.AttendanceMakeUpMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AttendanceMakeUpService extends ServiceImpl<AttendanceMakeUpMapper, AttendanceMakeUp> {

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private AttendanceRecordService attendanceRecordService;

    @Transactional
    public AttendanceMakeUp submitMakeUp(AttendanceMakeUpRequestDTO request) {
        Employee employee = employeeService.getById(request.getEmployeeId());
        if (employee == null) {
            throw new RuntimeException("员工不存在");
        }

        AttendanceMakeUp makeup = new AttendanceMakeUp();
        makeup.setEmployeeId(request.getEmployeeId());
        makeup.setEmployeeName(employee.getName());
        makeup.setDepartmentId(employee.getDepartmentId());
        makeup.setAttendanceDate(request.getAttendanceDate());
        makeup.setMakeupType(request.getMakeupType());
        makeup.setMakeupTime(request.getMakeupTime());
        makeup.setReason(request.getReason());
        makeup.setLocation(request.getLocation());
        makeup.setIpAddress(request.getIpAddress());
        makeup.setLng(request.getLng());
        makeup.setLat(request.getLat());
        makeup.setStatus(0);
        save(makeup);
        return makeup;
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
