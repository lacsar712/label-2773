package com.example.employee.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.employee.entity.AttendanceException;
import com.example.employee.entity.AttendanceRecord;
import com.example.employee.entity.Employee;
import com.example.employee.mapper.AttendanceExceptionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class AttendanceExceptionService extends ServiceImpl<AttendanceExceptionMapper, AttendanceException> {

    @Autowired
    private EmployeeService employeeService;

    @Transactional
    public AttendanceException generateException(AttendanceRecord record) {
        if (record.getExceptionFlag() == null || record.getExceptionFlag() == 0) {
            return null;
        }

        LambdaQueryWrapper<AttendanceException> existWrapper = new LambdaQueryWrapper<>();
        existWrapper.eq(AttendanceException::getRecordId, record.getId());
        AttendanceException exist = getOne(existWrapper);
        if (exist != null) {
            return exist;
        }

        Employee employee = employeeService.getById(record.getEmployeeId());

        AttendanceException exception = new AttendanceException();
        exception.setEmployeeId(record.getEmployeeId());
        if (employee != null) {
            exception.setEmployeeName(employee.getName());
            exception.setDepartmentId(employee.getDepartmentId());
        }
        exception.setAttendanceDate(record.getAttendanceDate());
        exception.setLateMinutes(record.getLateMinutes() != null ? record.getLateMinutes() : 0);
        exception.setEarlyMinutes(record.getEarlyMinutes() != null ? record.getEarlyMinutes() : 0);
        exception.setRecordId(record.getId());
        exception.setHandled(0);
        exception.setSalaryDeduction(BigDecimal.ZERO);

        int status = record.getStatus() != null ? record.getStatus() : 0;
        int exceptionType = status;
        if (status == 0) {
            exceptionType = 1;
        }
        exception.setExceptionType(exceptionType);

        save(exception);
        return exception;
    }

    public Page<AttendanceException> queryExceptionList(Long employeeId, Long departmentId,
                                                        Integer exceptionType, Integer handled,
                                                        Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<AttendanceException> wrapper = new LambdaQueryWrapper<>();
        if (employeeId != null) {
            wrapper.eq(AttendanceException::getEmployeeId, employeeId);
        }
        if (departmentId != null) {
            wrapper.eq(AttendanceException::getDepartmentId, departmentId);
        }
        if (exceptionType != null) {
            wrapper.eq(AttendanceException::getExceptionType, exceptionType);
        }
        if (handled != null) {
            wrapper.eq(AttendanceException::getHandled, handled);
        }
        wrapper.orderByDesc(AttendanceException::getAttendanceDate);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    public List<AttendanceException> getUnhandledExceptions() {
        return list(new LambdaQueryWrapper<AttendanceException>()
                .eq(AttendanceException::getHandled, 0)
                .orderByAsc(AttendanceException::getAttendanceDate));
    }

    @Transactional
    public boolean markHandled(Long id, String remark) {
        AttendanceException exception = getById(id);
        if (exception == null) {
            return false;
        }
        exception.setHandled(1);
        if (remark != null && !remark.isEmpty()) {
            exception.setRemark(remark);
        }
        return updateById(exception);
    }

    @Transactional
    public int batchMarkHandled(List<Long> ids, String remark) {
        int count = 0;
        for (Long id : ids) {
            if (markHandled(id, remark)) {
                count++;
            }
        }
        return count;
    }

    public Page<AttendanceException> getEmployeeExceptions(Long employeeId, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<AttendanceException> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AttendanceException::getEmployeeId, employeeId)
                .orderByDesc(AttendanceException::getAttendanceDate);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }
}
