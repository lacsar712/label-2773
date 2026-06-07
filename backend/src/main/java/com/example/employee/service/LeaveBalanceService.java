package com.example.employee.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.employee.entity.Employee;
import com.example.employee.entity.LeaveBalance;
import com.example.employee.mapper.LeaveBalanceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class LeaveBalanceService extends ServiceImpl<LeaveBalanceMapper, LeaveBalance> {

    @Autowired
    private EmployeeService employeeService;

    public List<LeaveBalance> getEmployeeBalances(Long employeeId, Integer year) {
        LambdaQueryWrapper<LeaveBalance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LeaveBalance::getEmployeeId, employeeId);
        if (year != null) {
            wrapper.eq(LeaveBalance::getYear, year);
        }
        return list(wrapper);
    }

    public LeaveBalance getBalance(Long employeeId, Integer leaveType, Integer year) {
        LambdaQueryWrapper<LeaveBalance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LeaveBalance::getEmployeeId, employeeId)
                .eq(LeaveBalance::getLeaveType, leaveType)
                .eq(LeaveBalance::getYear, year);
        LeaveBalance balance = getOne(wrapper);
        if (balance == null) {
            balance = initBalance(employeeId, leaveType, year);
        }
        return balance;
    }

    @Transactional
    public LeaveBalance initBalance(Long employeeId, Integer leaveType, Integer year) {
        Employee employee = employeeService.getById(employeeId);
        if (employee == null) {
            throw new RuntimeException("员工不存在");
        }

        LeaveBalance balance = new LeaveBalance();
        balance.setEmployeeId(employeeId);
        balance.setEmployeeName(employee.getName());
        balance.setLeaveType(leaveType);
        balance.setYear(year);
        balance.setUsedDays(BigDecimal.ZERO);

        BigDecimal totalDays = calculateTotalDays(employee, leaveType, year);
        balance.setTotalDays(totalDays);
        balance.setRemainingDays(totalDays);
        balance.setRemark("系统自动初始化");
        save(balance);
        return balance;
    }

    private BigDecimal calculateTotalDays(Employee employee, Integer leaveType, Integer year) {
        if (leaveType == 1) {
            LocalDate hireDate = employee.getHireDate();
            if (hireDate == null) {
                return BigDecimal.ZERO;
            }
            long yearsWorked = ChronoUnit.YEARS.between(hireDate, LocalDate.of(year, 12, 31));
            if (yearsWorked >= 10) {
                return BigDecimal.valueOf(15);
            } else if (yearsWorked >= 1) {
                return BigDecimal.valueOf(10);
            } else if (yearsWorked >= 0 && hireDate.getYear() == year) {
                long monthsWorked = ChronoUnit.MONTHS.between(hireDate, LocalDate.of(year, 12, 31));
                return BigDecimal.valueOf(Math.max(0, monthsWorked * 5.0 / 12));
            }
            return BigDecimal.valueOf(5);
        }
        return BigDecimal.valueOf(9999);
    }

    @Transactional
    public void deductBalance(Long employeeId, Integer leaveType, Integer year, BigDecimal days) {
        LeaveBalance balance = getBalance(employeeId, leaveType, year);
        if (balance.getRemainingDays().compareTo(days) < 0) {
            throw new RuntimeException("假期余额不足");
        }
        balance.setUsedDays(balance.getUsedDays().add(days));
        balance.setRemainingDays(balance.getRemainingDays().subtract(days));
        updateById(balance);
    }

    @Transactional
    public void refundBalance(Long employeeId, Integer leaveType, Integer year, BigDecimal days) {
        LeaveBalance balance = getBalance(employeeId, leaveType, year);
        balance.setUsedDays(balance.getUsedDays().subtract(days));
        balance.setRemainingDays(balance.getRemainingDays().add(days));
        if (balance.getUsedDays().compareTo(BigDecimal.ZERO) < 0) {
            balance.setUsedDays(BigDecimal.ZERO);
        }
        updateById(balance);
    }

    public boolean checkBalance(Long employeeId, Integer leaveType, Integer year, BigDecimal days) {
        if (leaveType == 2 || leaveType == 3) {
            return true;
        }
        LeaveBalance balance = getBalance(employeeId, leaveType, year);
        return balance.getRemainingDays().compareTo(days) >= 0;
    }
}
