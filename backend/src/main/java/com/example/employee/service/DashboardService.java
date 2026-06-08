package com.example.employee.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.employee.dto.*;
import com.example.employee.entity.Department;
import com.example.employee.entity.Employee;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private DepartmentService departmentService;

    public DashboardOverviewDTO getOverview(LocalDate startDate, LocalDate endDate, Long departmentId) {
        DashboardOverviewDTO dto = new DashboardOverviewDTO();

        List<Employee> allEmployees = employeeService.list();
        List<Department> allDepartments = departmentService.list();

        final Set<Long> deptFilterIds = departmentId != null
                ? collectAllDescendantIds(allDepartments, departmentId)
                : null;

        List<Employee> filteredEmployees = deptFilterIds == null
                ? allEmployees
                : allEmployees.stream().filter(e -> deptFilterIds.contains(e.getDepartmentId())).collect(Collectors.toList());

        List<Employee> activeEmployees = filteredEmployees.stream()
                .filter(e -> e.getStatus() == null || e.getStatus() != 4)
                .collect(Collectors.toList());

        long totalActive = activeEmployees.size();
        dto.setTotalEmployees(totalActive);

        long totalHeadcount = 0;
        List<Department> relevantDepts = deptFilterIds == null
                ? allDepartments.stream().filter(Department::getEnabled).collect(Collectors.toList())
                : allDepartments.stream().filter(d -> deptFilterIds.contains(d.getId()) && d.getEnabled()).collect(Collectors.toList());
        for (Department dept : relevantDepts) {
            if (dept.getHeadcountLimit() != null) {
                totalHeadcount += dept.getHeadcountLimit();
            }
        }
        dto.setTotalHeadcount(totalHeadcount);
        dto.setHeadcountUsageRate(totalHeadcount > 0 ? (totalActive * 100.0 / totalHeadcount) : 0.0);

        LocalDate today = LocalDate.now();
        YearMonth thisMonth = YearMonth.from(today);

        long newHiresThisMonth = activeEmployees.stream()
                .filter(e -> e.getHireDate() != null && YearMonth.from(e.getHireDate()).equals(thisMonth))
                .count();
        dto.setNewHiresThisMonth(newHiresThisMonth);

        long pendingProbation = activeEmployees.stream()
                .filter(e -> e.getStatus() != null && e.getStatus() == 2)
                .count();
        dto.setPendingProbation(pendingProbation);

        long contractExpiringSoon = activeEmployees.stream()
                .filter(e -> {
                    if (e.getContractEndDate() == null) return false;
                    long months = today.until(e.getContractEndDate()).toTotalMonths();
                    return months >= 0 && months <= 3;
                })
                .count();
        dto.setContractExpiringSoon(contractExpiringSoon);

        dto.setPendingLeaveApproval((long) (new Random().nextInt(5) + 1));
        dto.setPendingOnboarding((long) (new Random().nextInt(3) + 1));

        YearMonth lastMonth = thisMonth.minusMonths(1);
        long lastMonthTotal = activeEmployees.stream()
                .filter(e -> {
                    if (e.getHireDate() == null) return true;
                    return !YearMonth.from(e.getHireDate()).isAfter(lastMonth);
                })
                .filter(e -> {
                    if (e.getLeaveDate() == null) return true;
                    return !YearMonth.from(e.getLeaveDate()).isBefore(thisMonth);
                })
                .count();
        dto.setTotalEmployeesMoM(lastMonthTotal > 0 ? ((totalActive - lastMonthTotal) * 100.0 / lastMonthTotal) : 0.0);

        long lastMonthHires = filteredEmployees.stream()
                .filter(e -> e.getHireDate() != null && YearMonth.from(e.getHireDate()).equals(lastMonth))
                .count();
        dto.setNewHiresMoM(lastMonthHires > 0 ? ((newHiresThisMonth - lastMonthHires) * 100.0 / lastMonthHires) : 0.0);

        long thisMonthDepartures = filteredEmployees.stream()
                .filter(e -> e.getLeaveDate() != null && YearMonth.from(e.getLeaveDate()).equals(thisMonth))
                .count();
        long lastMonthDepartures = filteredEmployees.stream()
                .filter(e -> e.getLeaveDate() != null && YearMonth.from(e.getLeaveDate()).equals(lastMonth))
                .count();
        double thisMonthAttrition = lastMonthTotal > 0 ? (thisMonthDepartures * 100.0 / lastMonthTotal) : 0.0;
        double lastMonthAttrition = lastMonthTotal > 0 ? (lastMonthDepartures * 100.0 / lastMonthTotal) : 0.0;
        dto.setAttritionRate(thisMonthAttrition);
        dto.setAttritionRateMoM(lastMonthAttrition > 0 ? ((thisMonthAttrition - lastMonthAttrition) * 100.0 / lastMonthAttrition) : 0.0);

        List<DepartmentStatDTO> deptStats = buildDepartmentStats(allDepartments, allEmployees, deptFilterIds, totalActive);
        dto.setDepartmentStats(deptStats);

        dto.setTurnoverTrend(buildTurnoverTrend(filteredEmployees, startDate, endDate));
        dto.setMonthlyHireTrend(buildMonthlyHireTrend(filteredEmployees, startDate, endDate));

        return dto;
    }

    private List<DepartmentStatDTO> buildDepartmentStats(List<Department> allDepartments, List<Employee> allEmployees, Set<Long> deptFilterIds, long totalActive) {
        Map<Long, Long> deptEmpCount = allEmployees.stream()
                .filter(e -> e.getStatus() == null || e.getStatus() != 4)
                .filter(e -> deptFilterIds == null || deptFilterIds.contains(e.getDepartmentId()))
                .collect(Collectors.groupingBy(Employee::getDepartmentId, Collectors.counting()));

        Map<Long, String> deptNameMap = allDepartments.stream()
                .collect(Collectors.toMap(Department::getId, Department::getName));

        Map<Long, Integer> deptHeadcountMap = allDepartments.stream()
                .collect(Collectors.toMap(Department::getId, d -> d.getHeadcountLimit() != null ? d.getHeadcountLimit() : 0));

        List<DepartmentStatDTO> result = new ArrayList<>();
        List<Department> topDepts = allDepartments.stream()
                .filter(d -> deptFilterIds == null || deptFilterIds.contains(d.getId()))
                .filter(d -> d.getParentId() == null || d.getParentId() == 1)
                .filter(Department::getEnabled)
                .limit(8)
                .collect(Collectors.toList());

        for (Department dept : topDepts) {
            Set<Long> subIds = collectAllDescendantIds(allDepartments, dept.getId());
            long count = subIds.stream().mapToLong(id -> deptEmpCount.getOrDefault(id, 0L)).sum();
            long headcount = subIds.stream().mapToLong(id -> deptHeadcountMap.getOrDefault(id, 0)).sum();

            DepartmentStatDTO stat = new DepartmentStatDTO();
            stat.setDepartmentId(dept.getId());
            stat.setDepartmentName(dept.getName());
            stat.setEmployeeCount(count);
            stat.setHeadcountLimit(headcount);
            stat.setHeadcountUsageRate(headcount > 0 ? (count * 100.0 / headcount) : 0.0);
            stat.setPercentage(totalActive > 0 ? (count * 100.0 / totalActive) : 0.0);
            result.add(stat);
        }
        return result;
    }

    private List<TurnoverTrendDTO> buildTurnoverTrend(List<Employee> employees, LocalDate startDate, LocalDate endDate) {
        List<TurnoverTrendDTO> result = new ArrayList<>();
        YearMonth start = YearMonth.from(startDate != null ? startDate : LocalDate.now().minusMonths(11));
        YearMonth end = YearMonth.from(endDate != null ? endDate : LocalDate.now());

        Map<Long, String> deptNameMap = new HashMap<>();

        for (YearMonth ym = start; !ym.isAfter(end); ym = ym.plusMonths(1)) {
            final YearMonth currentMonth = ym;
            TurnoverTrendDTO dto = new TurnoverTrendDTO();
            dto.setMonth(currentMonth.format(DateTimeFormatter.ofPattern("yyyy-MM")));

            long hires = employees.stream()
                    .filter(e -> e.getHireDate() != null && YearMonth.from(e.getHireDate()).equals(currentMonth))
                    .count();

            long departures = employees.stream()
                    .filter(e -> e.getLeaveDate() != null && YearMonth.from(e.getLeaveDate()).equals(currentMonth))
                    .count();

            dto.setHires(hires);
            dto.setDepartures(departures);

            YearMonth prevMonth = currentMonth.minusMonths(1);
            long prevTotal = employees.stream()
                    .filter(e -> {
                        if (e.getHireDate() == null) return true;
                        return !YearMonth.from(e.getHireDate()).isAfter(prevMonth);
                    })
                    .filter(e -> {
                        if (e.getLeaveDate() == null) return true;
                        return !YearMonth.from(e.getLeaveDate()).isBefore(currentMonth);
                    })
                    .count();
            dto.setAttritionRate(prevTotal > 0 ? (departures * 100.0 / prevTotal) : 0.0);

            result.add(dto);
        }
        return result;
    }

    private List<MonthlyStatDTO> buildMonthlyHireTrend(List<Employee> employees, LocalDate startDate, LocalDate endDate) {
        List<MonthlyStatDTO> result = new ArrayList<>();
        YearMonth start = YearMonth.from(startDate != null ? startDate : LocalDate.now().minusMonths(11));
        YearMonth end = YearMonth.from(endDate != null ? endDate : LocalDate.now());

        for (YearMonth ym = start; !ym.isAfter(end); ym = ym.plusMonths(1)) {
            final YearMonth currentMonth = ym;
            MonthlyStatDTO dto = new MonthlyStatDTO();
            dto.setMonth(currentMonth.format(DateTimeFormatter.ofPattern("yyyy-MM")));
            long count = employees.stream()
                    .filter(e -> e.getHireDate() != null && YearMonth.from(e.getHireDate()).equals(currentMonth))
                    .count();
            dto.setCount(count);
            result.add(dto);
        }
        return result;
    }

    private Set<Long> collectAllDescendantIds(List<Department> departments, Long rootId) {
        Set<Long> result = new HashSet<>();
        result.add(rootId);
        Map<Long, List<Department>> childrenMap = departments.stream()
                .filter(d -> d.getParentId() != null)
                .collect(Collectors.groupingBy(Department::getParentId));
        Queue<Long> queue = new LinkedList<>();
        queue.add(rootId);
        while (!queue.isEmpty()) {
            Long current = queue.poll();
            List<Department> children = childrenMap.get(current);
            if (children != null) {
                for (Department child : children) {
                    if (result.add(child.getId())) {
                        queue.add(child.getId());
                    }
                }
            }
        }
        return result;
    }
}
