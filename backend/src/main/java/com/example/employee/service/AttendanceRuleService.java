package com.example.employee.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.employee.dto.AttendanceRuleQueryDTO;
import com.example.employee.entity.AttendanceRule;
import com.example.employee.entity.EmployeeAttendanceRule;
import com.example.employee.mapper.AttendanceRuleMapper;
import com.example.employee.mapper.EmployeeAttendanceRuleMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class AttendanceRuleService extends ServiceImpl<AttendanceRuleMapper, AttendanceRule> {

    @Autowired
    private EmployeeAttendanceRuleMapper employeeAttendanceRuleMapper;

    public AttendanceRule getDefaultRule() {
        return getOne(new LambdaQueryWrapper<AttendanceRule>()
                .eq(AttendanceRule::getIsDefault, 1)
                .eq(AttendanceRule::getEnabled, 1)
                .last("LIMIT 1"));
    }

    public AttendanceRule getEmployeeRuleForDate(Long employeeId, LocalDate date) {
        EmployeeAttendanceRule ear = employeeAttendanceRuleMapper.selectOne(
                new LambdaQueryWrapper<EmployeeAttendanceRule>()
                        .eq(EmployeeAttendanceRule::getEmployeeId, employeeId)
                        .le(EmployeeAttendanceRule::getEffectiveDate, date)
                        .orderByDesc(EmployeeAttendanceRule::getEffectiveDate)
                        .last("LIMIT 1")
        );

        if (ear != null) {
            AttendanceRule rule = getById(ear.getRuleId());
            if (rule != null && rule.getEnabled() == 1) {
                return rule;
            }
        }
        return getDefaultRule();
    }

    public AttendanceRule getEmployeeTodayRule(Long employeeId) {
        return getEmployeeRuleForDate(employeeId, LocalDate.now());
    }

    public Page<AttendanceRule> queryRules(AttendanceRuleQueryDTO query) {
        LambdaQueryWrapper<AttendanceRule> wrapper = new LambdaQueryWrapper<>();
        if (query.getRuleName() != null && !query.getRuleName().isEmpty()) {
            wrapper.like(AttendanceRule::getRuleName, query.getRuleName());
        }
        if (query.getRuleType() != null) {
            wrapper.eq(AttendanceRule::getRuleType, query.getRuleType());
        }
        if (query.getEnabled() != null) {
            wrapper.eq(AttendanceRule::getEnabled, query.getEnabled());
        }
        if (query.getIsDefault() != null) {
            wrapper.eq(AttendanceRule::getIsDefault, query.getIsDefault());
        }
        wrapper.orderByDesc(AttendanceRule::getCreateTime);
        return page(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
    }

    public List<AttendanceRule> listEnabledRules() {
        return list(new LambdaQueryWrapper<AttendanceRule>()
                .eq(AttendanceRule::getEnabled, 1)
                .orderByDesc(AttendanceRule::getIsDefault)
                .orderByDesc(AttendanceRule::getCreateTime));
    }

    public boolean createRule(AttendanceRule rule) {
        if (rule.getIsDefault() != null && rule.getIsDefault() == 1) {
            clearDefaultFlag();
        }
        return save(rule);
    }

    public boolean updateRule(AttendanceRule rule) {
        if (rule.getIsDefault() != null && rule.getIsDefault() == 1) {
            clearDefaultFlag();
        }
        return updateById(rule);
    }

    public boolean deleteRule(Long id) {
        return removeById(id);
    }

    private void clearDefaultFlag() {
        List<AttendanceRule> defaults = list(new LambdaQueryWrapper<AttendanceRule>()
                .eq(AttendanceRule::getIsDefault, 1));
        for (AttendanceRule r : defaults) {
            r.setIsDefault(0);
            updateById(r);
        }
    }
}
