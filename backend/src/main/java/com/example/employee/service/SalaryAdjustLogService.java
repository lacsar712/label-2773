package com.example.employee.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.employee.entity.SalaryAdjustLog;
import com.example.employee.mapper.SalaryAdjustLogMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SalaryAdjustLogService extends ServiceImpl<SalaryAdjustLogMapper, SalaryAdjustLog> {

    public List<SalaryAdjustLog> listBySalaryRecordId(Long salaryRecordId) {
        LambdaQueryWrapper<SalaryAdjustLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SalaryAdjustLog::getSalaryRecordId, salaryRecordId)
                .orderByDesc(SalaryAdjustLog::getCreateTime);
        return list(wrapper);
    }

    public List<SalaryAdjustLog> listByEmployeeId(Long employeeId) {
        LambdaQueryWrapper<SalaryAdjustLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SalaryAdjustLog::getEmployeeId, employeeId)
                .orderByDesc(SalaryAdjustLog::getCreateTime);
        return list(wrapper);
    }
}
