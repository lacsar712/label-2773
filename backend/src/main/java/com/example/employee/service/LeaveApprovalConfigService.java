package com.example.employee.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.employee.entity.Department;
import com.example.employee.entity.Employee;
import com.example.employee.entity.LeaveApprovalConfig;
import com.example.employee.mapper.LeaveApprovalConfigMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class LeaveApprovalConfigService extends ServiceImpl<LeaveApprovalConfigMapper, LeaveApprovalConfig> {

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private EmployeeService employeeService;

    public List<LeaveApprovalConfig> getConfigsForApplication(Integer leaveType, Long departmentId, BigDecimal days) {
        LambdaQueryWrapper<LeaveApprovalConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(LeaveApprovalConfig::getLeaveType, leaveType).or().eq(LeaveApprovalConfig::getLeaveType, 0));
        wrapper.and(w -> w.eq(LeaveApprovalConfig::getDepartmentId, departmentId).or().isNull(LeaveApprovalConfig::getDepartmentId));
        wrapper.le(LeaveApprovalConfig::getMinDays, days);
        wrapper.ge(LeaveApprovalConfig::getMaxDays, days);
        wrapper.eq(LeaveApprovalConfig::getEnabled, 1);

        List<LeaveApprovalConfig> configs = list(wrapper);

        configs.sort(Comparator.comparing(LeaveApprovalConfig::getNodeIndex));

        return dedupeConfigs(configs);
    }

    private List<LeaveApprovalConfig> dedupeConfigs(List<LeaveApprovalConfig> configs) {
        List<LeaveApprovalConfig> result = new ArrayList<>();
        for (LeaveApprovalConfig config : configs) {
            boolean exists = result.stream()
                    .anyMatch(c -> c.getNodeIndex().equals(config.getNodeIndex())
                            && c.getDepartmentId() != null);
            if (!exists) {
                boolean betterMatch = result.stream()
                        .anyMatch(c -> c.getNodeIndex().equals(config.getNodeIndex())
                                && c.getLeaveType().equals(config.getLeaveType())
                                && c.getDepartmentId() == null
                                && config.getDepartmentId() != null);
                if (betterMatch) {
                    result.removeIf(c -> c.getNodeIndex().equals(config.getNodeIndex())
                            && c.getLeaveType().equals(config.getLeaveType()));
                    result.add(config);
                } else {
                    result.add(config);
                }
            }
        }
        result.sort(Comparator.comparing(LeaveApprovalConfig::getNodeIndex));
        return result;
    }

    public Employee resolveApprover(LeaveApprovalConfig config, Long employeeId, Long departmentId) {
        String role = config.getApproverRole();
        switch (role) {
            case "DIRECT_MANAGER":
                return findDirectManager(employeeId);
            case "DEPARTMENT_HEAD":
                return findDepartmentHead(departmentId);
            case "HR":
                return findHR();
            case "SPECIFIC":
                return employeeService.getById(config.getApproverId());
            default:
                return null;
        }
    }

    private Employee findDirectManager(Long employeeId) {
        Employee employee = employeeService.getById(employeeId);
        if (employee == null) return null;

        Department dept = departmentService.getById(employee.getDepartmentId());
        if (dept != null && dept.getLeader() != null) {
            LambdaQueryWrapper<Employee> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Employee::getName, dept.getLeader());
            Employee manager = employeeService.getOne(wrapper);
            if (manager != null && !manager.getId().equals(employeeId)) {
                return manager;
            }
        }

        if (dept != null && dept.getParentId() != null) {
            Department parentDept = departmentService.getById(dept.getParentId());
            if (parentDept != null && parentDept.getLeader() != null) {
                LambdaQueryWrapper<Employee> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(Employee::getName, parentDept.getLeader());
                return employeeService.getOne(wrapper);
            }
        }
        return findHR();
    }

    private Employee findDepartmentHead(Long departmentId) {
        Department dept = departmentService.getById(departmentId);
        if (dept != null && dept.getLeader() != null) {
            LambdaQueryWrapper<Employee> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Employee::getName, dept.getLeader());
            return employeeService.getOne(wrapper);
        }
        return findHR();
    }

    private Employee findHR() {
        List<Employee> all = employeeService.list();
        return all.stream()
                .filter(e -> e.getRole() != null && (e.getRole().contains("HR") || e.getRole().contains("人事") || e.getRole().contains("人力资源")))
                .findFirst()
                .orElse(all.stream().findFirst().orElse(null));
    }
}
